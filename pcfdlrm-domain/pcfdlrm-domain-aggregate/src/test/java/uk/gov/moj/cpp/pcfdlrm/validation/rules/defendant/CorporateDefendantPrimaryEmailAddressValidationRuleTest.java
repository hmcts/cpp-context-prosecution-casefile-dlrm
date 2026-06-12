package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.CORPORATE_DEFENDANT_PRIMARY_EMAIL_ADDRESS;

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
public class CorporateDefendantPrimaryEmailAddressValidationRuleTest {

    private static final String INVALID_EMAIL = "invalid@email";
    private static final String VALID_EMAIL = "valid@email.com";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenPrimaryEmailAddressNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithNoPrimaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenValidPrimaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithValidPrimaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenInvalidPrimaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithInvalidPrimaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CORPORATE_DEFENDANT_PRIMARY_EMAIL_ADDRESS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_EMAIL));
    }

    private MigratedDefendant getCorporateDefendantWithNoPrimaryEmail() {
        return MigratedDefendant.migratedDefendant().build();
    }

    private MigratedDefendant getCorporateDefendantWithValidPrimaryEmail() {
        return MigratedDefendant.migratedDefendant().withEmailAddress1(VALID_EMAIL).build();
    }

    private MigratedDefendant getCorporateDefendantWithInvalidPrimaryEmail() {
        return MigratedDefendant.migratedDefendant().withEmailAddress1(INVALID_EMAIL).build();
    }

}
