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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OffenceCommittedDateValidationRuleTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/London"));

    private final OffenceCommittedDateValidationRule rule = new OffenceCommittedDateValidationRule();

    @Test
    void shouldRaiseProblemForFutureCommittedDate() {
        final ValidationResult result = rule.validate(defendantWith(TODAY.plusDays(1)), null);

        assertThat(result.problems().size(), is(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(ProblemCode.OFFENCE_COMMITTED_DATE_IN_FUTURE.name()));
        assertThat(problem.getValues().stream().map(ProblemValue::getId).toList(), contains(OFFENCE_ID.toString()));
    }

    @Test
    void shouldAcceptCommittedDateOfTodayOrEarlier() {
        assertThat(rule.validate(defendantWith(TODAY), null).problems(), is(empty()));
        assertThat(rule.validate(defendantWith(TODAY.minusYears(1)), null).problems(), is(empty()));
    }

    @Test
    void shouldIgnoreMissingCommittedDate() {
        assertThat(rule.validate(defendantWith(null), null).problems(), is(empty()));
    }

    private static DefendantWithReferenceData defendantWith(final LocalDate committedDate) {
        return new DefendantWithReferenceData(
                migratedDefendant().withId(UUID.randomUUID())
                        .withOffences(List.of(migratedOffence().withOffenceId(OFFENCE_ID).withOffenceCommittedDate(committedDate).build()))
                        .build(),
                new ReferenceDataVO(), CaseDetails.caseDetails().build());
    }
}
