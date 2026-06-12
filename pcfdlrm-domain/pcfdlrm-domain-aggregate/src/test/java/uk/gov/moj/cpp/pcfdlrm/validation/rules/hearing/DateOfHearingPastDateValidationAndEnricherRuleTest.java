package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DATE_OF_HEARING_IN_THE_PAST;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_DATE_OF_HEARING;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateOfHearingPastDateValidationAndEnricherRuleTest {


    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String PAST_DATE_OF_HEARING = "2006-11-03";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    MigratedDefendant defendant;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;


    @Test
    public void shouldReturnEmptyListWhenDateOfHearingIsInFuture() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(DATE_OF_HEARING);

        final Optional<Problem> optionalProblem = new DateOfHearingPastDateValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public  void shouldBevalidWhenNoDOHButWC(){
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(null);

        final Optional<Problem> optionalProblem = new DateOfHearingPastDateValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

    }

    @Test
    public  void shouldBevalidWhenDOHEmpty(){
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn("");

        final Optional<Problem> optionalProblem = new DateOfHearingPastDateValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

    }


    @Test
    public void shouldReturnProblemWhenDefendantDateOfBirthIsInFuture() {

        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(PAST_DATE_OF_HEARING);

        final Optional<Problem> optionalProblem = new DateOfHearingPastDateValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));

        assertThat(optionalProblem.get().getCode(), is(DATE_OF_HEARING_IN_THE_PAST.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_DATE_OF_HEARING.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(PAST_DATE_OF_HEARING));

    }

}