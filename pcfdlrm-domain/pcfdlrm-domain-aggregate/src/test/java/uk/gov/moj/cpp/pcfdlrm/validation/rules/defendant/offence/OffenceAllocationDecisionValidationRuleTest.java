package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.ALLOCATION_DECISION_DATE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.ALLOCATION_DECISION_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedAllocationDecision.migratedAllocationDecision;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OffenceAllocationDecisionValidationRuleTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/London"));
    private static final UUID KNOWN_MOT_REASON_ID = UUID.randomUUID();

    private final OffenceAllocationDecisionValidationRule rule = new OffenceAllocationDecisionValidationRule();

    @Test
    void shouldReportOnlyAllocationDecisionProblemWhenMotReasonIsUnknown() {
        // Missing date would also fail the date check if it ran.
        final List<Problem> problems = rule.validate(defendantWith(UUID.randomUUID(), null), null).problems();

        assertThat(problems.stream().map(Problem::getCode).toList(), contains(ALLOCATION_DECISION_INVALID.name()));
    }

    @Test
    void shouldCheckDateWhenAllocationDecisionIsValid() {
        final List<Problem> problems = rule.validate(defendantWith(KNOWN_MOT_REASON_ID, null), null).problems();

        assertThat(problems.stream().map(Problem::getCode).toList(), contains(ALLOCATION_DECISION_DATE_INVALID.name()));
    }

    @Test
    void shouldBeValidWhenAllocationDecisionAndDateAreValid() {
        assertThat(rule.validate(defendantWith(KNOWN_MOT_REASON_ID, TODAY.minusMonths(1)), null).problems(), is(empty()));
    }

    private static DefendantWithReferenceData defendantWith(final UUID motReasonId, final LocalDate allocationDecisionDate) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.getModeOfTrialReasonsReferenceData().add(modeOfTrialReasonsReferenceData().withId(KNOWN_MOT_REASON_ID.toString()).build());
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID())
                        .withOffences(List.of(migratedOffence()
                                .withOffenceId(UUID.randomUUID())
                                .withAllocationDecision(migratedAllocationDecision()
                                        .withMotReasonId(motReasonId)
                                        .withAllocationDecisionDate(allocationDecisionDate)
                                        .build())
                                .build()))
                        .build(),
                referenceDataVO, CaseDetails.caseDetails().build());
    }
}
