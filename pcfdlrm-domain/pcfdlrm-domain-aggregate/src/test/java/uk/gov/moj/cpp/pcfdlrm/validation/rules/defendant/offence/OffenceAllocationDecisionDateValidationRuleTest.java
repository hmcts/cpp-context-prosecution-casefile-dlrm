package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OffenceAllocationDecisionDateValidationRuleTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID OTHER_OFFENCE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/London"));

    private final OffenceAllocationDecisionDateValidationRule rule = new OffenceAllocationDecisionDateValidationRule();

    static Stream<Arguments> invalidAllocationDecisionDates() {
        return Stream.of(
                Arguments.of("missing", null),
                Arguments.of("in the future", TODAY.plusDays(1)));
    }

    @ParameterizedTest(name = "allocation decision date {0} → problem")
    @MethodSource("invalidAllocationDecisionDates")
    void shouldRaiseProblemForAllocationDecisionWithInvalidDate(final String description, final LocalDate allocationDecisionDate) {
        final ValidationResult result = rule.validate(defendantWith(offenceWithAllocationDecision(OFFENCE_ID, allocationDecisionDate)), null);

        assertThat(result.problems().size(), is(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(ProblemCode.ALLOCATION_DECISION_DATE_INVALID.name()));
        assertThat(problem.getValues().stream().map(ProblemValue::getId).toList(), contains(OFFENCE_ID.toString()));
    }

    @Test
    void shouldAcceptAllocationDecisionWithPastDate() {
        assertThat(rule.validate(defendantWith(offenceWithAllocationDecision(OFFENCE_ID, TODAY.minusMonths(1))), null).problems(), is(empty()));
    }

    @Test
    void shouldAcceptAllocationDecisionDateOfToday() {
        assertThat(rule.validate(defendantWith(offenceWithAllocationDecision(OFFENCE_ID, TODAY)), null).problems(), is(empty()));
    }

    @Test
    void shouldIgnoreOffenceWithoutAllocationDecision() {
        assertThat(rule.validate(defendantWith(migratedOffence().withOffenceId(OFFENCE_ID).build()), null).problems(), is(empty()));
    }

    @Test
    void shouldReportOneValuePerInvalidOffence() {
        final ValidationResult result = rule.validate(defendantWith(
                offenceWithAllocationDecision(OFFENCE_ID, null),
                offenceWithAllocationDecision(OTHER_OFFENCE_ID, TODAY.plusDays(1))), null);

        assertThat(result.problems().size(), is(1));
        assertThat(result.problems().get(0).getValues().stream().map(ProblemValue::getId).toList(),
                contains(OFFENCE_ID.toString(), OTHER_OFFENCE_ID.toString()));
    }

    @Test
    void shouldIgnoreDefendantWithoutOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).build(), new ReferenceDataVO(), CaseDetails.caseDetails().build());

        assertThat(rule.validate(defendantWithReferenceData, null).problems(), is(empty()));
    }

    private static MigratedOffence offenceWithAllocationDecision(final UUID offenceId, final LocalDate allocationDecisionDate) {
        return migratedOffence()
                .withOffenceId(offenceId)
                .withAllocationDecision(migratedAllocationDecision()
                        .withMotReasonId(UUID.randomUUID())
                        .withAllocationDecisionDate(allocationDecisionDate)
                        .build())
                .build();
    }

    private static DefendantWithReferenceData defendantWith(final MigratedOffence... offences) {
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).withOffences(List.of(offences)).build(),
                new ReferenceDataVO(), CaseDetails.caseDetails().build());
    }
}
