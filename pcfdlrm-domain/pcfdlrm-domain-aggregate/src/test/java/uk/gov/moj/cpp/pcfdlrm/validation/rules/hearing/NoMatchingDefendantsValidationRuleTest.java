package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.NO_MATCHING_DEFENDANTS_FOR_HEARING;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoMatchingDefendantsValidationRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Test
    void shouldReturnValidWhenHearingHasMatchingDefendants() {
        when(migratedHearingWithReferenceData.getMigratedDefendantWithOffences())
                .thenReturn(List.of(mock(MigratedDefendantWithOffences.class)));

        final ValidationResult result = new NoMatchingDefendantsValidationRule()
                .validate(migratedHearingWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    @Test
    void shouldReturnProblemWhenDefendantsWithOffencesIsNull() {
        when(migratedHearingWithReferenceData.getMigratedDefendantWithOffences())
                .thenReturn(null);

        final ValidationResult result = new NoMatchingDefendantsValidationRule()
                .validate(migratedHearingWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(false));

        final Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.get().getCode(), is(NO_MATCHING_DEFENDANTS_FOR_HEARING.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is("listedDefendants"));
        assertThat(problem.get().getValues().get(0).getValue(), is("No matching defendants with offences"));
    }

    @Test
    void shouldReturnProblemWhenHearingHasNoMatchingDefendants() {
        when(migratedHearingWithReferenceData.getMigratedDefendantWithOffences())
                .thenReturn(Collections.emptyList());

        final ValidationResult result = new NoMatchingDefendantsValidationRule()
                .validate(migratedHearingWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(false));

        final Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.get().getCode(), is(NO_MATCHING_DEFENDANTS_FOR_HEARING.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is("listedDefendants"));
        assertThat(problem.get().getValues().get(0).getValue(), is("No matching defendants with offences"));
    }
}
