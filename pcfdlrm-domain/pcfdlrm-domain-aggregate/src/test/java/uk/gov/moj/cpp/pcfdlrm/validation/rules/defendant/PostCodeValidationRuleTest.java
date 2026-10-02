package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_DEFENDANT_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_GUARDIAN_POST_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostCodeValidationRuleTest {


    public static final String INVALID_POST_CODE = "CRO 2QX";
    public static final String ADDRESS_POSTCODE_FIELD = "address_postcode";
    public static final String ADDRESS_1 = "ASHBY WALK";
    private static final String VALID_POST_CODE = "CR0 2QX";
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock()
    private DefendantWithReferenceData defendantWithReferenceData;

    @Test
    void shouldMatchWhenNoPostCodeAvailable() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(MigratedDefendant.migratedDefendant().build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat("Empty post code invalidated", validationResult.problems(), is(empty()));
    }

    @Test
    public void shouldPassWhenPostCodeIsValid() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(MigratedDefendant.migratedDefendant()
                .withAddress(address()
                        .withAddress1(ADDRESS_1)
                        .withPostcode(VALID_POST_CODE)
                        .build())
                .build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat(validationResult.problems(), is(empty()));
    }

    @Test
    public void shouldFailWhenPostCodeIsInvalid() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(MigratedDefendant.migratedDefendant()
                .withAddress(address()
                        .withAddress1(ADDRESS_1)
                        .withPostcode(INVALID_POST_CODE)
                        .build())
                .build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        final Problem problem = validationResult.problems().get(0);
        assertThat(problem, is(notNullValue()));
        assertThat(problem.getCode(), is(INVALID_DEFENDANT_POST_CODE.name()));
        assertThat(problem.getValues().get(0).getKey(), is(ADDRESS_POSTCODE_FIELD));
        assertThat(problem.getValues().get(0).getValue(), is(INVALID_POST_CODE));
    }

    // DD-43501 AC-S3-001 / AC-S1-009: the no-arg rule (XHIBIT, SPI, MCC, LIBRA J/R/O) still warns on an
    // invalid guardian postcode, value kept. PostCodeValidationRule(false) is the LIBRA S/C/Q variant:
    // the guardian postcode is owned by LibraIndividualParentGuardianAddressValidationRule there, while
    // the defendant postcode check is unchanged.
    @ParameterizedTest(name = "validateParentGuardianPostCode={0}")
    @CsvSource({
            "true,  INVALID_DEFENDANT_POST_CODE|INVALID_GUARDIAN_POST_CODE",
            "false, INVALID_DEFENDANT_POST_CODE"
    })
    void shouldValidateGuardianPostCodeOnlyWhenEnabled(final boolean validateParentGuardianPostCode, final String expectedCodes) {
        when(defendantWithReferenceData.getDefendant()).thenReturn(defendantWithInvalidDefendantAndGuardianPostCode());

        final ValidationResult validationResult = new PostCodeValidationRule(validateParentGuardianPostCode).validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(codesOf(validationResult), is(List.of(expectedCodes.split("\\|"))));
    }

    @Test
    void shouldKeepWarningOnInvalidGuardianPostCodeForNoArgConstructor() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(defendantWithInvalidDefendantAndGuardianPostCode());

        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);

        final Problem guardianProblem = validationResult.problems().get(1);
        assertThat(guardianProblem.getCode(), is(INVALID_GUARDIAN_POST_CODE.name()));
        assertThat(guardianProblem.getValues().get(0).getValue(), is(INVALID_POST_CODE));
    }

    private static MigratedDefendant defendantWithInvalidDefendantAndGuardianPostCode() {
        return MigratedDefendant.migratedDefendant()
                .withAddress(address().withAddress1(ADDRESS_1).withPostcode(INVALID_POST_CODE).build())
                .withIndividual(Individual.individual()
                        .withParentGuardianInformation(individualParentGuardian(validGuardianContactDetails().build(),
                                validGuardianAddress().withPostcode(INVALID_POST_CODE).build()).build())
                        .build())
                .build();
    }

    private static List<String> codesOf(final ValidationResult validationResult) {
        return validationResult.problems().stream().map(Problem::getCode).toList();
    }

}
