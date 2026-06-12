package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.CORPORATE_DEFENDANT_SECONDARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CorporateDefendantSecondaryEmailAddressValidationRuleTest {

    private static final String INVALID_EMAIL = "invalid@email";
    private static final String VALID_EMAIL = "valid@email.com";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenSecondaryEmailAddressNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithNoSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenValidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithValidSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenInvalidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithInvalidSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CORPORATE_DEFENDANT_SECONDARY_EMAIL_ADDRESS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_EMAIL));
    }

    private MigratedDefendant getCorporateDefendantWithNoSecondaryEmail() {
        return migratedDefendant().build();
    }

    private MigratedDefendant getCorporateDefendantWithValidSecondaryEmail() {
        return migratedDefendant().withEmailAddress2(VALID_EMAIL).build();
    }

    private MigratedDefendant getCorporateDefendantWithInvalidSecondaryEmail() {
        return migratedDefendant().withEmailAddress2(INVALID_EMAIL).build();
    }

}
