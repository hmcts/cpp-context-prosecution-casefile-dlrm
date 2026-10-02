package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.organisationParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_ORG_POSTCODE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_POSTCODE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.pgAddressKey;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.pgOrgAddressKey;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation.parentGuardianInformation;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LibraParentGuardianAddressValidationRuleTest {

    private static final String CHARS_35 = "x".repeat(35);
    private static final String CHARS_36 = "x".repeat(36);
    private static final String ADDRESS1_INVALID = "PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID";
    private static final String ORG_ADDRESS1_INVALID = "PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID";

    static Stream<Arguments> individualAddresses() {
        return Stream.of(
                arguments("valid address", individual(validGuardianAddress().build()), List.of()),
                arguments("no personalInformation", parentGuardianInformation().withGender("MALE").withDateOfBirth(LocalDate.of(1980, 6, 1)).build(),
                        List.of(ADDRESS1_INVALID + " " + pgAddressKey(1))),
                arguments("personalInformation without address (AC-S3-004)", individual(null), List.of(ADDRESS1_INVALID + " " + pgAddressKey(1))),
                arguments("address1 empty", individual(validGuardianAddress().withAddress1("").build()), List.of(ADDRESS1_INVALID + " " + pgAddressKey(1))),
                arguments("address1 whitespace only (AC-S3-006)", individual(validGuardianAddress().withAddress1("   ").build()), List.of(ADDRESS1_INVALID + " " + pgAddressKey(1))),
                arguments("address1 35 characters", individual(validGuardianAddress().withAddress1(CHARS_35).build()), List.of()),
                arguments("address1 36 characters", individual(validGuardianAddress().withAddress1(CHARS_36).build()), List.of(ADDRESS1_INVALID + " " + pgAddressKey(1))),
                arguments("address1 35 characters plus padding", individual(validGuardianAddress().withAddress1(" " + CHARS_35 + " ").build()), List.of()),
                arguments("address1 NFA (AC-S3-005)", individual(validGuardianAddress().withAddress1("NFA").build()), List.of()),
                arguments("address1 nfa, case-insensitive", individual(validGuardianAddress().withAddress1("nfa").build()), List.of()),
                arguments("address2 35 characters", individual(validGuardianAddress().withAddress2(CHARS_35).build()), List.of()),
                arguments("address3 36 characters (AC-S3-007)", individual(validGuardianAddress().withAddress3(CHARS_36).build()),
                        List.of("PARENT_GUARDIAN_ADDRESS_LINE_INVALID " + pgAddressKey(3))),
                arguments("address2 and address5 36 characters", individual(validGuardianAddress().withAddress2(CHARS_36).withAddress5(CHARS_36).build()),
                        List.of("PARENT_GUARDIAN_ADDRESS_LINE_INVALID " + pgAddressKey(2), "PARENT_GUARDIAN_ADDRESS_LINE_INVALID " + pgAddressKey(5))),
                arguments("postcode absent (Q1)", individual(validGuardianAddress().withPostcode(null).build()), List.of()),
                arguments("postcode blank (Q1)", individual(validGuardianAddress().withPostcode(" ").build()), List.of()),
                arguments("postcode SW1A 1AA", individual(validGuardianAddress().withPostcode("SW1A 1AA").build()), List.of()),
                arguments("postcode SW1A1AA", individual(validGuardianAddress().withPostcode("SW1A1AA").build()), List.of()),
                arguments("postcode with surrounding spaces", individual(validGuardianAddress().withPostcode(" SW1A 1AA ").build()), List.of()),
                arguments("postcode ZZ99 9ZZ, no fixed abode (Q14)", individual(validGuardianAddress().withPostcode("ZZ99 9ZZ").build()), List.of()),
                arguments("postcode zz999zz, no fixed abode", individual(validGuardianAddress().withPostcode("zz999zz").build()), List.of()),
                arguments("postcode NOT A POSTCODE", individual(validGuardianAddress().withPostcode("NOT A POSTCODE").build()),
                        List.of("INVALID_GUARDIAN_POST_CODE " + PG_POSTCODE)),
                arguments("postcode over 8 characters (Q7)", individual(validGuardianAddress().withPostcode("SW1A   1AA").build()),
                        List.of("INVALID_GUARDIAN_POST_CODE " + PG_POSTCODE)),
                arguments("address1 blank and postcode invalid — address1 first (AC-S3-009 order)",
                        individual(validGuardianAddress().withAddress1(" ").withPostcode("NOT A POSTCODE").build()),
                        List.of(ADDRESS1_INVALID + " " + pgAddressKey(1), "INVALID_GUARDIAN_POST_CODE " + PG_POSTCODE))
        );
    }

    static Stream<Arguments> organisationAddresses() {
        return Stream.of(
                arguments("valid address", organisationParentGuardian(validGuardianAddress().build()).build(), List.of()),
                arguments("no address — the real LIBRA feed shape, F-2/Q18 (AC-S4-001, AC-S4-004)", organisationParentGuardian(null).build(),
                        List.of(ORG_ADDRESS1_INVALID + " " + pgOrgAddressKey(1))),
                arguments("address1 blank (AC-S4-002)", organisationParentGuardian(validGuardianAddress().withAddress1(" ").build()).build(),
                        List.of(ORG_ADDRESS1_INVALID + " " + pgOrgAddressKey(1))),
                arguments("address1 36 characters", organisationParentGuardian(validGuardianAddress().withAddress1(CHARS_36).build()).build(),
                        List.of(ORG_ADDRESS1_INVALID + " " + pgOrgAddressKey(1))),
                arguments("address1 35 characters", organisationParentGuardian(validGuardianAddress().withAddress1(CHARS_35).build()).build(), List.of()),
                arguments("address5 36 characters (AC-S2-004)", organisationParentGuardian(validGuardianAddress().withAddress5(CHARS_36).build()).build(),
                        List.of("PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID " + pgOrgAddressKey(5))),
                arguments("postcode NOT A POSTCODE (AC-S2-005)", organisationParentGuardian(validGuardianAddress().withPostcode("NOT A POSTCODE").build()).build(),
                        List.of("INVALID_GUARDIAN_ORGANISATION_POST_CODE " + PG_ORG_POSTCODE)),
                arguments("postcode ZZ99 9ZZ", organisationParentGuardian(validGuardianAddress().withPostcode("ZZ99 9ZZ").build()).build(), List.of()),
                arguments("postcode absent", organisationParentGuardian(validGuardianAddress().withPostcode(null).build()).build(), List.of())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("individualAddresses")
    void shouldValidateIndividualGuardianAddress(final String description, final ParentGuardianInformation guardian, final List<String> expected) {
        assertProblems(new LibraIndividualParentGuardianAddressValidationRule(), guardian, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("organisationAddresses")
    void shouldValidateOrganisationGuardianAddress(final String description, final ParentGuardianInformation guardian, final List<String> expected) {
        assertProblems(new LibraOrganisationParentGuardianAddressValidationRule(), guardian, expected);
    }

    private static void assertProblems(final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> rule,
                                       final ParentGuardianInformation guardian, final List<String> expectedCodeAndKey) {
        final List<String> actual = rule
                .validate(new DefendantWithReferenceData(defendantWithParentGuardian(DEFENDANT_ID, guardian), new ReferenceDataVO(), CaseDetails.caseDetails().build()), null)
                .problems().stream()
                .map(p -> p.getCode() + " " + p.getValues().get(0).getKey() + " " + p.getValues().get(0).getValue())
                .toList();

        assertThat(actual, is(expectedCodeAndKey.stream().map(codeAndKey -> codeAndKey + " " + codeAndKey.substring(codeAndKey.indexOf(' ') + 1)).toList()));
    }

    private static ParentGuardianInformation individual(final Address address) {
        return individualParentGuardian(validGuardianContactDetails().build(), address).build();
    }
}
