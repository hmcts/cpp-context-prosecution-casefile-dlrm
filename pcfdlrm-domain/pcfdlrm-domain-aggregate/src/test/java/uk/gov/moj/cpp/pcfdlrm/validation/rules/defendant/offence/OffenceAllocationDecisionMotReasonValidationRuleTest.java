package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedAllocationDecision.migratedAllocationDecision;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OffenceAllocationDecisionMotReasonValidationRuleTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID KNOWN_MOT_REASON_ID = UUID.randomUUID();

    private final OffenceAllocationDecisionMotReasonValidationRule rule = new OffenceAllocationDecisionMotReasonValidationRule();

    @Test
    void shouldAcceptKnownMotReasonId() {
        assertThat(rule.validate(defendantWith(offenceWithMotReason(KNOWN_MOT_REASON_ID)), null).problems(), is(empty()));
    }

    @Test
    void shouldRaiseProblemForUnknownMotReasonId() {
        final UUID unknownMotReasonId = UUID.randomUUID();

        final ValidationResult result = rule.validate(defendantWith(offenceWithMotReason(unknownMotReasonId)), null);

        assertThat(result.problems().size(), is(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(ProblemCode.ALLOCATION_DECISION_INVALID.name()));
        assertThat(problem.getValues().stream().map(ProblemValue::getId).toList(), contains(OFFENCE_ID.toString()));
        assertThat(problem.getValues().stream().map(ProblemValue::getValue).toList(), contains(unknownMotReasonId.toString()));
    }

    @Test
    void shouldIgnoreOffenceWithoutAllocationDecision() {
        assertThat(rule.validate(defendantWith(migratedOffence().withOffenceId(OFFENCE_ID).build()), null).problems(), is(empty()));
    }

    @Test
    void shouldIgnoreDefendantWithoutOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).build(), referenceDataWithKnownMotReason(), CaseDetails.caseDetails().build());

        assertThat(rule.validate(defendantWithReferenceData, null).problems(), is(empty()));
    }

    private static MigratedOffence offenceWithMotReason(final UUID motReasonId) {
        return migratedOffence()
                .withOffenceId(OFFENCE_ID)
                .withAllocationDecision(migratedAllocationDecision().withMotReasonId(motReasonId).build())
                .build();
    }

    // As ModeOfTrialRefDataEnricher leaves it before validation.
    private static ReferenceDataVO referenceDataWithKnownMotReason() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.getModeOfTrialReasonsReferenceData().add(modeOfTrialReasonsReferenceData().withId(KNOWN_MOT_REASON_ID.toString()).build());
        return referenceDataVO;
    }

    private static DefendantWithReferenceData defendantWith(final MigratedOffence offence) {
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).withOffences(List.of(offence)).build(),
                referenceDataWithKnownMotReason(), CaseDetails.caseDetails().build());
    }
}
