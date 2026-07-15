package com.procurementsaas.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Evaluation Service.
 *
 * <p>Scores the bids of an opened tender in two stages, mirroring standard procurement
 * practice:
 *
 * <ol>
 *   <li><strong>Technical</strong> — each participant is scored against weighted criteria;
 *       those below the pass mark are disqualified and take no further part.</li>
 *   <li><strong>Financial</strong> — only technically qualified participants are priced,
 *       the lowest bid scoring 100 and others scored proportionally.</li>
 * </ol>
 *
 * <p>The two are combined into a weighted total and ranked to produce the comparative
 * statement. Sequencing is enforced: financial scores cannot be computed — or even seen —
 * before the technical stage is closed, so price cannot influence technical judgement.
 */
@SpringBootApplication
public class EvaluationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
