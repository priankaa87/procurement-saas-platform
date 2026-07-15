package com.procurementsaas.evaluation;

import com.procurementsaas.evaluation.service.ScoreCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the evaluation arithmetic — no Spring, no database. */
class ScoreCalculatorTest {

    @Test
    void technicalScoreIsTheWeightedAverageOfCriterionScores() {
        // 80 at weight 60, 50 at weight 40 -> (80*60 + 50*40) / 100 = 68.00
        BigDecimal result = ScoreCalculator.technicalScore(
            Map.of(1L, new BigDecimal("80"), 2L, new BigDecimal("50")),
            Map.of(1L, 60, 2L, 40));
        assertThat(result).isEqualByComparingTo("68.00");
    }

    @Test
    void aPerfectScoreOnEveryCriterionGivesOneHundred() {
        BigDecimal result = ScoreCalculator.technicalScore(
            Map.of(1L, new BigDecimal("100"), 2L, new BigDecimal("100")),
            Map.of(1L, 70, 2L, 30));
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void theLowestBidScoresOneHundred() {
        BigDecimal result = ScoreCalculator.financialScore(
            new BigDecimal("300"), new BigDecimal("300"));
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void aBidTwiceTheLowestScoresFifty() {
        BigDecimal result = ScoreCalculator.financialScore(
            new BigDecimal("600"), new BigDecimal("300"));
        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void financialScoreRoundsToTwoDecimalPlaces() {
        // 300 / 700 * 100 = 42.857... -> 42.86
        BigDecimal result = ScoreCalculator.financialScore(
            new BigDecimal("700"), new BigDecimal("300"));
        assertThat(result).isEqualByComparingTo("42.86");
    }

    @Test
    void aZeroBidIsRejectedRatherThanDividingByZero() {
        assertThatThrownBy(() -> ScoreCalculator.financialScore(BigDecimal.ZERO, new BigDecimal("100")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than zero");
    }

    @Test
    void combinedScoreAppliesTheStageWeights() {
        // technical 80 @70%, financial 50 @30% -> 56 + 15 = 71.00
        BigDecimal result = ScoreCalculator.combinedScore(
            new BigDecimal("80"), 70, new BigDecimal("50"), 30);
        assertThat(result).isEqualByComparingTo("71.00");
    }

    @Test
    void aHundredPercentFinancialWeightIgnoresTheTechnicalScore() {
        BigDecimal result = ScoreCalculator.combinedScore(
            new BigDecimal("10"), 0, new BigDecimal("90"), 100);
        assertThat(result).isEqualByComparingTo("90.00");
    }
}
