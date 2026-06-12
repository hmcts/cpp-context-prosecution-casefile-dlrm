package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PERCEIVED_BIRTH_YEAR_IN_FUTURE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_PERCEIVED_BIRTH_YEAR;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantPerceivedBirthYearValidationRuleTest {
    @InjectMocks
    private DefendantPerceivedBirthYearValidationRule defendantPerceivedBirthYearValidationRule;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO ;

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    @Test
    public void shouldReturnProblemWhenDefendantDateOfBirthIsInFuture() {
        final String defendantYearOfBirth = LocalDate.now().plusYears(1).getYear()+"";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(defendantYearOfBirth);

        final Optional<Problem> problem = defendantPerceivedBirthYearValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem, equalTo(Optional.of(newProblem(DEFENDANT_PERCEIVED_BIRTH_YEAR_IN_FUTURE, DEFENDANT_PERCEIVED_BIRTH_YEAR.getValue(), defendantYearOfBirth))));

    }

    @Test
    public void shouldNotReturnProblemWhenDefendantDateOfBirthIsNotInFuture() {
        final String defendantYearOfBirth = LocalDate.now().minusYears(1).getYear()+"";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(defendantYearOfBirth);

        final Optional<Problem> actualProblem = defendantPerceivedBirthYearValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }

    @Test
    public void shouldNotReturnProblemWhenDefendantDateOfBirthIsNotPresent() {

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);

        final Optional<Problem> actualProblem = defendantPerceivedBirthYearValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }


    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String defendantYearOfBirth) {

     final Individual individual = Individual.individual().withPerceivedBirthYear(defendantYearOfBirth).build();
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withIndividual(individual ).build();

        return new DefendantWithReferenceData(defendant,referenceDataVO, null);
    }
}
