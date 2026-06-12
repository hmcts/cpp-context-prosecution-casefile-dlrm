package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation.personalInformation;
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
public class ParentGuardianPrimaryEmailAddressValidationRuleTest {

    private static final String INVALID_EMAIL = "invalid@email";
    private static final String VALID_EMAIL = "valid@email.com";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenPrimaryEmailAddressNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithNoPrimaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenValidPrimaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithValidPrimaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenInvalidPrimaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithInvalidPrimaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianPrimaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_EMAIL));
    }

    private MigratedDefendant getIndividualWithParentGuardianWithNoPrimaryEmail() {
        return migratedDefendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private MigratedDefendant getIndividualWithParentGuardianWithValidPrimaryEmail() {
        return migratedDefendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().withPrimaryEmail(VALID_EMAIL).build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private MigratedDefendant getIndividualWithParentGuardianWithInvalidPrimaryEmail() {
        return migratedDefendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().withPrimaryEmail(INVALID_EMAIL).build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

}
