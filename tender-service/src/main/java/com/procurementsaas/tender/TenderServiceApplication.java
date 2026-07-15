package com.procurementsaas.tender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tender Service — the core procurement engine.
 *
 * <p>Owns the tender lifecycle: drafting with line items, inviting suppliers, publishing,
 * sealed bid submission, opening after the deadline, and award.
 *
 * <p>The central integrity rule is <strong>bid sealing</strong>: no one can read submitted
 * bids until the tender is formally opened, and a tender cannot be opened before its
 * deadline. Items and suppliers are referenced by code (owned by Master Data and Vendor
 * Management respectively), never by cross-database foreign key.
 */
@SpringBootApplication
public class TenderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenderServiceApplication.class, args);
    }
}
