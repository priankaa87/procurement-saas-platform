package com.procurementsaas.notification;

import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.TenderPublishedEvent;
import com.procurementsaas.events.Topics;
import com.procurementsaas.notification.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the event backbone end to end: an event published to Kafka results in rendered,
 * delivered notifications — with no synchronous call between the services.
 */
// This service only consumes in production, so it configures no producer. The test plays
// the part of tender-service, and needs the same JSON serializer that service publishes with.
@SpringBootTest(properties = {
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@AutoConfigureMockMvc
@Testcontainers
class NotificationServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    NotificationRepository notificationRepository;

    private static RequestPostProcessor view() {
        return jwt().authorities(new SimpleGrantedAuthority("FEATURE_NOTIFICATION_VIEW"));
    }

    private static RequestPostProcessor manage() {
        return jwt().authorities(new SimpleGrantedAuthority("FEATURE_NOTIFICATION_MANAGE"));
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void seededTemplatesAreAvailable() throws Exception {
        mvc.perform(get("/templates").with(view()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void publishingATenderNotifiesEveryInvitedSupplier() {
        TenderPublishedEvent event = new TenderPublishedEvent("public", "T-NOTIFY-1",
            "Laptops", Instant.parse("2027-01-01T10:00:00Z"),
            List.of("SUP-A", "SUP-B", "SUP-C"), Instant.now());

        kafkaTemplate.send(Topics.TENDER_PUBLISHED, event.tenderCode(), event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var sup = notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-A");
            assertThat(sup).hasSize(1);
            assertThat(sup.get(0).getTemplateCode()).isEqualTo("TENDER_PUBLISHED");
            // The template variables were actually substituted.
            assertThat(sup.get(0).getSubject()).isEqualTo("Invitation to bid: Laptops");
            assertThat(sup.get(0).getBody()).contains("T-NOTIFY-1").contains("1 Jan 2027");
            assertThat(sup.get(0).getStatus().name()).isEqualTo("SENT");

            assertThat(notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-B")).hasSize(1);
            assertThat(notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-C")).hasSize(1);
        });
    }

    @Test
    void awardingATenderTellsTheWinnerAndTheLosersDifferentThings() {
        TenderAwardedEvent event = new TenderAwardedEvent("public", "T-NOTIFY-2", "Servers",
            "SUP-WIN", List.of("SUP-LOSE-1", "SUP-LOSE-2"), Instant.now());

        kafkaTemplate.send(Topics.TENDER_AWARDED, event.tenderCode(), event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var winner = notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-WIN");
            assertThat(winner).hasSize(1);
            assertThat(winner.get(0).getTemplateCode()).isEqualTo("TENDER_AWARDED_WINNER");
            assertThat(winner.get(0).getSubject()).contains("You have been awarded");

            var loser = notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-LOSE-1");
            assertThat(loser).hasSize(1);
            assertThat(loser.get(0).getTemplateCode()).isEqualTo("TENDER_AWARDED_UNSUCCESSFUL");
            assertThat(loser.get(0).getBody()).contains("awarded to another supplier");

            assertThat(notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-LOSE-2"))
                .hasSize(1);
        });
    }

    /**
     * Kafka redelivers; the same event arriving twice must not double-notify anyone.
     */
    @Test
    void aRedeliveredEventDoesNotNotifyAnyoneTwice() {
        TenderPublishedEvent event = new TenderPublishedEvent("public", "T-DUPE",
            "Chairs", Instant.parse("2027-02-01T10:00:00Z"), List.of("SUP-DUPE"), Instant.now());

        kafkaTemplate.send(Topics.TENDER_PUBLISHED, event.tenderCode(), event);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-DUPE"))
                .hasSize(1));

        // Exactly the same event again, as a broker replay would deliver it.
        kafkaTemplate.send(Topics.TENDER_PUBLISHED, event.tenderCode(), event);

        // Give the consumer time to process it, then confirm nothing new was created.
        await().during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(notificationRepository.findByRecipientOrderByCreatedAtDesc("SUP-DUPE"))
                .hasSize(1));
    }

    @Test
    void templatesCanBeEditedWithoutARedeploy() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/templates/SUPPLIER_DEBARRED").with(manage())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Debarment notice\",\"body\":\"{{supplierCode}} is out: {{reason}}\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("Debarment notice"));

        mvc.perform(get("/templates").with(view()))
            .andExpect(jsonPath("$[?(@.code=='SUPPLIER_DEBARRED')].subject")
                .value("Debarment notice"));
    }

    @Test
    void editingTemplatesRequiresTheManagePrivilege() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/templates/TENDER_PUBLISHED").with(view())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"hacked\",\"body\":\"nope\"}"))
            .andExpect(status().isForbidden());
    }
}
