package com.procurementsaas.contract.messaging;

import com.procurementsaas.contract.service.AwardService;
import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Raises a notice of award when the Tender service reports a winner.
 *
 * <p>The award arrives as an event rather than a call: the tender is decided either way,
 * and this service being down should not be able to fail an award. The amount travels on
 * the event, so nothing has to be read back out of the Tender service.
 */
@Component
public class TenderAwardedListener {

    private static final Logger log = LoggerFactory.getLogger(TenderAwardedListener.class);

    private final AwardService awardService;

    public TenderAwardedListener(AwardService awardService) {
        this.awardService = awardService;
    }

    @KafkaListener(topics = Topics.TENDER_AWARDED, groupId = "award-contract-service")
    public void onTenderAwarded(TenderAwardedEvent event) {
        log.info("Raising award for tender {} won by {} at {} {}",
            event.tenderCode(), event.awardedSupplierCode(), event.awardedAmount(),
            event.currencyCode());

        awardService.createFromTenderIfAbsent(event.tenderCode(), event.title(),
            event.awardedSupplierCode(), event.awardedAmount(), event.currencyCode());
    }
}
