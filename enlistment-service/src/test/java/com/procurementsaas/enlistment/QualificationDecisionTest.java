package com.procurementsaas.enlistment;

import com.procurementsaas.enlistment.domain.CriterionAssessment;
import com.procurementsaas.enlistment.domain.EnlistmentCriterion;
import com.procurementsaas.enlistment.domain.EnlistmentSchedule;
import com.procurementsaas.enlistment.service.QualificationDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the qualification rule — no Spring, no database.
 *
 * <p>This is the judgement that decides who may compete for public money, so it is worth
 * being able to read the rule and its tests in one sitting.
 */
class QualificationDecisionTest {

    private static final EnlistmentSchedule SCHEDULE = new EnlistmentSchedule(
        "S-1", "Round", null, "IT-HARDWARE", Instant.now().plusSeconds(3600),
        new BigDecimal("60"), 12);

    private static CriterionAssessment assessment(String code, int weight, boolean mandatory,
                                                  String score, boolean met) {
        EnlistmentCriterion criterion =
            new EnlistmentCriterion(SCHEDULE, code, code + " name", weight, mandatory);
        return new CriterionAssessment(null, criterion, new BigDecimal(score), met, null);
    }

    @Test
    void aStrongApplicantQualifies() {
        // 80@60 + 90@40 = 84.00, comfortably over the 60 pass mark
        var result = QualificationDecision.decide(List.of(
            assessment("QUALITY", 60, false, "80", true),
            assessment("CAPACITY", 40, false, "90", true)), new BigDecimal("60"));

        assertThat(result.qualified()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("84.00");
        assertThat(result.reason()).isNull();
    }

    @Test
    void scoringBelowThePassMarkIsRefusedWithTheNumbers() {
        // 40@60 + 50@40 = 44.00
        var result = QualificationDecision.decide(List.of(
            assessment("QUALITY", 60, false, "40", true),
            assessment("CAPACITY", 40, false, "50", true)), new BigDecimal("60"));

        assertThat(result.qualified()).isFalse();
        assertThat(result.score()).isEqualByComparingTo("44.00");
        assertThat(result.reason()).contains("below the pass mark");
    }

    /** The point of a mandatory criterion: it cannot be outscored. */
    @Test
    void failingAMandatoryCriterionDisqualifiesDespiteAnExcellentScore() {
        // 100@90 + 100@10 = 100.00 — a perfect score, and still refused.
        var result = QualificationDecision.decide(List.of(
            assessment("QUALITY", 90, false, "100", true),
            assessment("LICENCE", 10, true, "100", false)), new BigDecimal("60"));

        assertThat(result.score()).isEqualByComparingTo("100.00");
        assertThat(result.qualified()).isFalse();
        assertThat(result.reason()).contains("Mandatory requirement not met")
            .contains("LICENCE name");
    }

    @Test
    void everyFailedMandatoryRequirementIsNamed() {
        var result = QualificationDecision.decide(List.of(
            assessment("LICENCE", 50, true, "0", false),
            assessment("INSURANCE", 50, true, "0", false)), new BigDecimal("60"));

        assertThat(result.qualified()).isFalse();
        assertThat(result.reason()).contains("LICENCE name").contains("INSURANCE name");
    }

    @Test
    void meetingAMandatoryCriterionWithALowScoreStillCountsTowardTheTotal() {
        // Met, but only 20@20 + 100@80 = 84.00 -> passes on score.
        var result = QualificationDecision.decide(List.of(
            assessment("LICENCE", 20, true, "20", true),
            assessment("QUALITY", 80, false, "100", true)), new BigDecimal("60"));

        assertThat(result.qualified()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("84.00");
    }

    @Test
    void aScoreExactlyOnThePassMarkQualifies() {
        var result = QualificationDecision.decide(List.of(
            assessment("QUALITY", 100, false, "60", true)), new BigDecimal("60"));

        assertThat(result.qualified()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("60.00");
    }

    @Test
    void aMandatoryFailureIsReportedAheadOfALowScore() {
        // Both wrong; the mandatory gate is the reason that matters.
        var result = QualificationDecision.decide(List.of(
            assessment("LICENCE", 50, true, "0", false),
            assessment("QUALITY", 50, false, "10", true)), new BigDecimal("60"));

        assertThat(result.reason()).contains("Mandatory requirement not met");
    }

    @Test
    void theWeightedScoreRoundsToTwoPlaces() {
        // 33.333@50 + 66.667@50 = 50.00
        var result = QualificationDecision.decide(List.of(
            assessment("A", 50, false, "33.33", true),
            assessment("B", 50, false, "66.67", true)), new BigDecimal("60"));

        assertThat(result.score()).isEqualByComparingTo("50.00");
    }
}
