package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
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

class OffenceCommittedEndDateValidationRuleTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/London"));
    private static final LocalDate COMMITTED_DATE = TODAY.minusYears(1);

    private final OffenceCommittedEndDateValidationRule rule = new OffenceCommittedEndDateValidationRule();

    static Stream<Arguments> invalidEndDates() {
        return Stream.of(
                Arguments.of("missing", null),
                Arguments.of("same as committed date", COMMITTED_DATE),
                Arguments.of("before committed date", COMMITTED_DATE.minusDays(1)),
                Arguments.of("in the future", TODAY.plusDays(1)));
    }

    @ParameterizedTest(name = "date code 4, end date {0} → problem")
    @MethodSource("invalidEndDates")
    void shouldRaiseProblemForDateCodeBetweenWithInvalidEndDate(final String description, final LocalDate endDate) {
        final ValidationResult result = rule.validate(defendantWith(offence(4, endDate)), null);

        assertThat(result.problems().size(), is(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(ProblemCode.OFFENCE_COMMITTED_END_DATE_INVALID.name()));
        assertThat(problem.getValues().stream().map(ProblemValue::getId).toList(), contains(OFFENCE_ID.toString()));
    }

    @Test
    void shouldAcceptDateCodeBetweenWithValidEndDate() {
        assertThat(rule.validate(defendantWith(offence(4, COMMITTED_DATE.plusMonths(1))), null).problems(), is(empty()));
    }

    @Test
    void shouldAcceptEndDateOfTodayForDateCodeBetween() {
        assertThat(rule.validate(defendantWith(offence(4, TODAY)), null).problems(), is(empty()));
    }

    @Test
    void shouldIgnoreOtherDateCodes() {
        assertThat(rule.validate(defendantWith(offence(1, null)), null).problems(), is(empty()));
        assertThat(rule.validate(defendantWith(offence(1, TODAY.plusDays(1))), null).problems(), is(empty()));
    }

    @Test
    void shouldIgnoreDefendantWithoutOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).build(), new ReferenceDataVO(), CaseDetails.caseDetails().build());

        assertThat(rule.validate(defendantWithReferenceData, null).problems(), is(empty()));
    }

    private static MigratedOffence offence(final Integer offenceDateCode, final LocalDate endDate) {
        return migratedOffence()
                .withOffenceId(OFFENCE_ID)
                .withOffenceDateCode(offenceDateCode)
                .withOffenceCommittedDate(COMMITTED_DATE)
                .withOffenceCommittedEndDate(endDate)
                .build();
    }

    private static DefendantWithReferenceData defendantWith(final MigratedOffence offence) {
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID()).withOffences(List.of(offence)).build(),
                new ReferenceDataVO(), CaseDetails.caseDetails().build());
    }
}
