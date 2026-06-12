package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BailConditionsValidationAndEnricherRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnProblemWhenCustodyStatusIsBAndBailConditionNotProvided() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withBailConditions(null)
                .withCustodyStatus("B")
                .build());

        final Optional<Problem> optionalProblem = new BailConditionsValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }


    @Test
    public void shouldReturnEmptyProblemListWhenCustodyStatusIsBAndBailConditionIsProvided() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withBailConditions("BailConditions")
                .build());

        final Optional<Problem> optionalProblem = new BailConditionsValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

    }

    @Test
    public void shouldReturnEmptyProblemListWhenCustodyStatusIsNullAndBailConditionIsProvided() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withBailConditions("BailConditions")
                .build());

        final Optional<Problem> optionalProblem = new BailConditionsValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

    }

}