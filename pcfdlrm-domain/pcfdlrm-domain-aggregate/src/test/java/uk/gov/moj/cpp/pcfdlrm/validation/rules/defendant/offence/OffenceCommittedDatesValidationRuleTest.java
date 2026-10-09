package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_COMMITTED_DATE_IN_FUTURE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_COMMITTED_END_DATE_INVALID;
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

class OffenceCommittedDatesValidationRuleTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/London"));

    private final OffenceCommittedDatesValidationRule rule = new OffenceCommittedDatesValidationRule();

    @Test
    void shouldReportOnlyCommittedDateProblemWhenCommittedDateIsInFuture() {
        // End date before the (future) committed date would also fail the end-date check if it ran.
        final List<Problem> problems = rule.validate(defendantWith(TODAY.plusDays(10), TODAY.plusDays(5)), null).problems();

        assertThat(problems.stream().map(Problem::getCode).toList(), contains(OFFENCE_COMMITTED_DATE_IN_FUTURE.name()));
    }

    @Test
    void shouldCheckEndDateWhenCommittedDateIsValid() {
        final List<Problem> problems = rule.validate(defendantWith(TODAY.minusYears(1), null), null).problems();

        assertThat(problems.stream().map(Problem::getCode).toList(), contains(OFFENCE_COMMITTED_END_DATE_INVALID.name()));
    }

    @Test
    void shouldBeValidWhenBothDatesAreValid() {
        assertThat(rule.validate(defendantWith(TODAY.minusYears(1), TODAY.minusMonths(1)), null).problems(), is(empty()));
    }

    private static DefendantWithReferenceData defendantWith(final LocalDate committedDate, final LocalDate endDate) {
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID())
                        .withOffences(List.of(migratedOffence()
                                .withOffenceId(UUID.randomUUID())
                                .withOffenceDateCode(4)
                                .withOffenceCommittedDate(committedDate)
                                .withOffenceCommittedEndDate(endDate)
                                .build()))
                        .build(),
                new ReferenceDataVO(), CaseDetails.caseDetails().build());
    }
}
