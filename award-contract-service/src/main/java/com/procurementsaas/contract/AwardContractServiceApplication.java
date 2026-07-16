package com.procurementsaas.contract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Award &amp; Contract Service — what happens after a tender is won.
 *
 * <p>Issues the notice of award, waits for the supplier to accept it, then turns an
 * accepted award into a work order with a delivery schedule, and records goods as they
 * arrive.
 *
 * <p>Awards arrive here as {@code TenderAwardedEvent} from the Tender service rather than
 * by a call: the tender is finished either way, and a contract failing to raise should not
 * be able to fail the award that caused it.
 */
@SpringBootApplication
public class AwardContractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AwardContractServiceApplication.class, args);
    }
}
