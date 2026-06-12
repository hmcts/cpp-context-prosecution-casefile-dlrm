package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_PNC_ID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PNC_ID;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PncIdValidationRuleTest {

    private static final String TEST_INVALID_PNC_ID = "0001/1234567M";
    private static final String TEST_VALID_PNC_ID_ONE = "2099/1234567L";
    private static final String TEST_VALID_PNC_ID_TWO = "1000/1111111k";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenPNCIDNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(null));

        final Optional<Problem> optionalProblem = new PncIdValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyErrorListWhenValidPNCIDOne() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_VALID_PNC_ID_ONE));

        final Optional<Problem> optionalProblemOne = new PncIdValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblemOne.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyErrorListWhenValidPNCIDTwo() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_VALID_PNC_ID_TWO));

        final Optional<Problem> optionalProblemTwo = new PncIdValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblemTwo.isPresent(), is(false));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void shouldReturnProblemWhenInvalidPNCID() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_INVALID_PNC_ID));

        final Optional<Problem> optionalProblem = new PncIdValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(INVALID_PNC_ID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PNC_ID.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(TEST_INVALID_PNC_ID));
    }

    private MigratedDefendant getDefendant(String pncId) {
        if (Objects.nonNull(pncId)) {
            return migratedDefendant().withPncIdentifier(pncId).build();
        } else {
            return migratedDefendant().build();
        }
    }
}
