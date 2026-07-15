package com.procurementsaas.events.autoconfigure;

import com.procurementsaas.events.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Exposes {@link DomainEventPublisher} to any service that has Kafka configured.
 * Services without a {@link KafkaTemplate} simply don't get one.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class PlatformEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    public DomainEventPublisher domainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DomainEventPublisher(kafkaTemplate);
    }
}
