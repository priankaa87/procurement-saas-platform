package com.procurementsaas.enlistment.service;

import com.procurementsaas.enlistment.domain.CriterionAssessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Decides whether an applicant qualifies, and says why when they do not.
 *
 * <p>Kept free of Spring and JPA so the rule can be read and tested on its own — this is
 * the judgement that decides who gets to compete for public money, and it should not be
 * buried in a service method.
 *
 * <p>Two hurdles, in order:
 * <ol>
 *   <li>every <em>mandatory</em> criterion must be met — a gate, not a score;</li>
 *   <li>the weighted score must reach the pass mark.</li>
 * </ol>
 */
public final class QualificationDecision {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private QualificationDecision() {
    }

    /** The outcome, with the reason spelled out when it is a refusal. */
    public record Result(boolean qualified, BigDecimal score, String reason) {
    }

    public static Result decide(List<CriterionAssessment> assessments, BigDecimal passMark) {
        BigDecimal score = weightedScore(assessments);

        // A mandatory failure is decisive: no amount of scoring elsewhere makes an
        // unlicensed supplier licensed.
        List<String> failedMandatory = assessments.stream()
            .filter(a -> a.getCriterion().isMandatory() && !a.isMet())
            .map(a -> a.getCriterion().getName())
            .toList();

        if (!failedMandatory.isEmpty()) {
            return new Result(false, score,
                "Mandatory requirement not met: " + String.join(", ", failedMandatory));
        }
        if (score.compareTo(passMark) < 0) {
            return new Result(false, score,
                "Score " + score + " is below the pass mark of " + passMark);
        }
        return new Result(true, score, null);
    }

    /** {@code Σ(score × weight) / 100}. */
    public static BigDecimal weightedScore(List<CriterionAssessment> assessments) {
        BigDecimal total = BigDecimal.ZERO;
        for (CriterionAssessment assessment : assessments) {
            total = total.add(assessment.getScore()
                .multiply(BigDecimal.valueOf(assessment.getCriterion().getWeight())));
        }
        return total.divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }
}
