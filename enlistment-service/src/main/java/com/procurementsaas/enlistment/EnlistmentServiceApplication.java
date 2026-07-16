package com.procurementsaas.enlistment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Enlistment (Pre-Qualification) Service.
 *
 * <p>Decides which suppliers are allowed to bid for a category of work <em>before</em> any
 * tender exists. A buyer opens an enlistment round, suppliers apply with evidence, a
 * committee assesses them against published criteria, and those who pass are enlisted for
 * a fixed period.
 *
 * <p>An enlistment is a standing judgement with an expiry date, not a permanent badge —
 * evidence goes stale, and a supplier qualified three years ago on accounts long since
 * changed is not qualified today.
 */
@SpringBootApplication
public class EnlistmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnlistmentServiceApplication.class, args);
    }
}
