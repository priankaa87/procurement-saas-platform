package com.procurementsaas.evaluation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * The evaluation arithmetic, deliberately free of Spring and JPA so the rules can be
 * verified in isolation.
 *
 * <p>All results are rounded to 2 decimal places, half-up.
 */
public final class ScoreCalculator {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ScoreCalculator() {
    }

    /**
     * Weighted technical score: {@code Σ(score × weight) / 100}.
     *
     * @param scoresByCriterionWeight raw 0–100 scores keyed by the criterion's weight-bearing id
     * @param weightsByCriterionId    criterion weights, which must total 100
     */
    public static BigDecimal technicalScore(Map<Long, BigDecimal> scoresByCriterionWeight,
                                            Map<Long, Integer> weightsByCriterionId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : scoresByCriterionWeight.entrySet()) {
            Integer weight = weightsByCriterionId.get(entry.getKey());
            if (weight == null) {
                continue;
            }
            total = total.add(entry.getValue().multiply(BigDecimal.valueOf(weight)));
        }
        return total.divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Financial score relative to the cheapest qualified bid: {@code lowest / bid × 100}.
     * The lowest bid scores 100; a bid twice the lowest scores 50.
     */
    public static BigDecimal financialScore(BigDecimal bidAmount, BigDecimal lowestBid) {
        if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than zero");
        }
        return lowestBid.multiply(HUNDRED).divide(bidAmount, SCALE, RoundingMode.HALF_UP);
    }

    /** Combined score: {@code technical × techWeight/100 + financial × finWeight/100}. */
    public static BigDecimal combinedScore(BigDecimal technicalScore, int technicalWeight,
                                           BigDecimal financialScore, int financialWeight) {
        BigDecimal technicalPart = technicalScore.multiply(BigDecimal.valueOf(technicalWeight));
        BigDecimal financialPart = financialScore.multiply(BigDecimal.valueOf(financialWeight));
        return technicalPart.add(financialPart).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }
}
