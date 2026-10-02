package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.organisationParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validIndividualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.ABSENT;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.INDIVIDUAL;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.ORGANISATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation.personalInformation;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ParentGuardianShapeTest {

    private static final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> ALWAYS_FAILS =
            (input, context) -> ValidationResult.newValidationResult(List.of(new Problem("DELEGATE_RAN", List.of(new ProblemValue(null, "k", "k")))));

    static Stream<Arguments> shapes() {
        return Stream.of(
                arguments("no guardian block", null, ABSENT),
                arguments("empty guardian block", parentGuardianInformation().build(), ABSENT),
                arguments("only blank values (Q4)", parentGuardianInformation().withGender(" ").withOrganisationName("")
                        .withPersonalInformation(personalInformation().withLastName(" ").build())
                        .withAddress(address().withAddress1(" ").build()).build(), ABSENT),
                arguments("gender only", parentGuardianInformation().withGender("1").build(), INDIVIDUAL),
                arguments("dateOfBirth only", parentGuardianInformation().withDateOfBirth(LocalDate.of(1980, 6, 1)).build(), INDIVIDUAL),
                arguments("personalInformation only", parentGuardianInformation().withPersonalInformation(personalInformation().withLastName("Parent").build()).build(), INDIVIDUAL),
                arguments("observedEthnicity only", parentGuardianInformation().withObservedEthnicity("1").build(), INDIVIDUAL),
                arguments("selfDefinedEthnicity only", parentGuardianInformation().withSelfDefinedEthnicity("W1").build(), INDIVIDUAL),
                arguments("full individual", validIndividualParentGuardian().build(), INDIVIDUAL),
                arguments("organisationName only", parentGuardianInformation().withOrganisationName("Example Care Ltd").build(), ORGANISATION),
                arguments("companyTelephoneNumber only", parentGuardianInformation().withCompanyTelephoneNumber("0161 496 0000").build(), ORGANISATION),
                arguments("address only", parentGuardianInformation().withAddress(validGuardianAddress().build()).build(), ORGANISATION),
                arguments("organisation as sent by LIBRA (no address, F-2)", organisationParentGuardian(null).build(), ORGANISATION),
                arguments("malformed mix — INDIVIDUAL wins", validIndividualParentGuardian().withOrganisationName("Example Care Ltd").build(), INDIVIDUAL)
        );
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("shapes")
    void shouldClassifyParentGuardianShape(final String description, final ParentGuardianInformation parentGuardianInformation, final ParentGuardianShape expected) {
        assertThat(ParentGuardianShape.of(parentGuardianInformation), is(expected));
    }

    @ParameterizedTest(name = "gate {0}, guardian {1} -> delegate runs: {2}")
    @CsvSource({
            "INDIVIDUAL,   INDIVIDUAL,   true",
            "INDIVIDUAL,   ORGANISATION, false",
            "INDIVIDUAL,   ABSENT,       false",
            "ORGANISATION, ORGANISATION, true",
            "ORGANISATION, INDIVIDUAL,   false",
            "ORGANISATION, ABSENT,       false"
    })
    void shouldRunDelegateOnlyForItsShape(final ParentGuardianShape gateShape, final ParentGuardianShape guardianShape, final boolean delegateRuns) {
        final ParentGuardianShapeGate gate = ParentGuardianShapeGate.onlyFor(gateShape, ALWAYS_FAILS);

        final ValidationResult result = gate.validate(input(defendantWithParentGuardian(DEFENDANT_ID, guardianOf(guardianShape))), null);

        assertThat(result.problems(), hasSize(delegateRuns ? 1 : 0));
        assertThat(gate.shape(), is(gateShape));
        assertThat(gate.delegate(), is(ALWAYS_FAILS));
    }

    @Test
    void shouldNotRunDelegateForCorporateDefendantWithNoIndividual() {
        final MigratedDefendant corporate = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).build();

        assertThat(ParentGuardianShapeGate.onlyFor(INDIVIDUAL, ALWAYS_FAILS).validate(input(corporate), null).problems(), hasSize(0));
        assertThat(ParentGuardianShapeGate.onlyFor(ORGANISATION, ALWAYS_FAILS).validate(input(corporate), null).problems(), hasSize(0));
    }

    private static ParentGuardianInformation guardianOf(final ParentGuardianShape shape) {
        return switch (shape) {
            case INDIVIDUAL -> validIndividualParentGuardian().build();
            case ORGANISATION -> organisationParentGuardian(validGuardianAddress().build()).build();
            case ABSENT -> null;
        };
    }

    private static DefendantWithReferenceData input(final MigratedDefendant defendant) {
        return new DefendantWithReferenceData(defendant, new ReferenceDataVO(), CaseDetails.caseDetails().build());
    }
}
