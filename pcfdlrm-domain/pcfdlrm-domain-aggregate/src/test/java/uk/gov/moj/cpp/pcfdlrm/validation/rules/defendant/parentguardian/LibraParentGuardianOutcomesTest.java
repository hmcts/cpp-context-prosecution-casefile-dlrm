package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.organisationParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validIndividualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_COMPANY_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_GENDER;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_HOME;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_MOBILE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_ORG_POSTCODE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_SECONDARY_EMAIL;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_WORK;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.pgAddressKey;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.pgOrgAddressKey;

import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * DD-43501 C4 — {@link LibraParentGuardianOutcomes}, the single code → outcome table, and
 * {@link LibraParentGuardianSanitiser}, which applies it. Closes the FR-021 / S5 audit: every code this
 * story raises is classified exactly once, every NULL code has a working nulling function for each field
 * key it can carry, and REJECT fields are never touched (AC-S5-002).
 */
class LibraParentGuardianOutcomesTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID,              REJECT",
            "INVALID_GUARDIAN_POST_CODE,                               REJECT",
            "PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID, REJECT",
            "PARENT_GUARDIAN_WORK_TELEPHONE_INVALID,                   NULL",
            "PARENT_GUARDIAN_HOME_TELEPHONE_INVALID,                   NULL",
            "PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID,                 NULL",
            "PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID,                NULL",
            "DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID,  NULL",
            "DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID, NULL",
            "DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE,        NULL",
            "DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID,     NULL",
            "DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID, NULL",
            "PARENT_GUARDIAN_ADDRESS_LINE_INVALID,                     NULL",
            "PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID,        NULL",
            "INVALID_GUARDIAN_ORGANISATION_POST_CODE,                  NULL",
            "PARENT_GUARDIAN_GENDER_INVALID,                           DEFAULT"
    })
    void shouldClassifyEveryLibraParentGuardianProblemCode(final ProblemCode code, final LibraParentGuardianOutcomes.Outcome expected) {
        assertThat(LibraParentGuardianOutcomes.outcomeOf(code), is(Optional.of(expected)));
        assertThat(LibraParentGuardianOutcomes.isReject(code.name()), is(expected == LibraParentGuardianOutcomes.Outcome.REJECT));
    }

    // Scope guard: defendant-level codes outside the guardian block are not in the table, so LIBRA can
    // never reject or null on them through this path (FR-020).
    @ParameterizedTest
    @CsvSource({"INVALID_DEFENDANT_POST_CODE", "INVALID_DEFENDANT_INDIVIDUAL_POST_CODE", "DEFENDANT_GENDER_INVALID", "OFFENCE_CODE_IS_INVALID"})
    void shouldNotClassifyNonGuardianCodes(final ProblemCode code) {
        assertThat(LibraParentGuardianOutcomes.outcomeOf(code), is(Optional.empty()));
        assertThat(LibraParentGuardianOutcomes.isReject(code.name()), is(false));
    }

    /** One row per (NULL code, field key): sanitising the valid guardian must remove exactly that field. */
    static Stream<Arguments> nullings() {
        final ParentGuardianInformation individual = validIndividualParentGuardian().build();
        final ParentGuardianInformation organisation = organisationParentGuardian(validGuardianAddress().withAddress3("Line 3").withAddress4("Line 4").withAddress5("Line 5").build()).build();
        return Stream.of(
                arguments("PARENT_GUARDIAN_WORK_TELEPHONE_INVALID", PG_WORK, individual,
                        individualParentGuardian(validGuardianContactDetails().withWork(null).build(), validGuardianAddress().build()).build()),
                arguments("PARENT_GUARDIAN_HOME_TELEPHONE_INVALID", PG_HOME, individual,
                        individualParentGuardian(validGuardianContactDetails().withHome(null).build(), validGuardianAddress().build()).build()),
                arguments("PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID", PG_MOBILE, individual,
                        individualParentGuardian(validGuardianContactDetails().withMobile(null).build(), validGuardianAddress().build()).build()),
                arguments("DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID", PG_PRIMARY_EMAIL, individual,
                        individualParentGuardian(validGuardianContactDetails().withPrimaryEmail(null).build(), validGuardianAddress().build()).build()),
                arguments("DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID", PG_SECONDARY_EMAIL, individual,
                        individualParentGuardian(validGuardianContactDetails().withSecondaryEmail(null).build(), validGuardianAddress().build()).build()),
                arguments("DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE", PG_DATE_OF_BIRTH, individual,
                        validIndividualParentGuardian().withDateOfBirth(null).build()),
                arguments("DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID", PG_OBSERVED_ETHNICITY, validIndividualParentGuardian().withObservedEthnicity("99").build(),
                        validIndividualParentGuardian().build()),
                arguments("DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID", PG_SELF_DEFINED_ETHNICITY, validIndividualParentGuardian().withSelfDefinedEthnicity("Z9").build(),
                        validIndividualParentGuardian().build()),
                arguments("PARENT_GUARDIAN_ADDRESS_LINE_INVALID", pgAddressKey(2), individual,
                        individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress2(null).build()).build()),
                arguments("PARENT_GUARDIAN_ADDRESS_LINE_INVALID", pgAddressKey(3), individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress3("Line 3").build()).build(),
                        individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().build()).build()),
                arguments("PARENT_GUARDIAN_ADDRESS_LINE_INVALID", pgAddressKey(4), individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress4("Line 4").build()).build(),
                        individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().build()).build()),
                arguments("PARENT_GUARDIAN_ADDRESS_LINE_INVALID", pgAddressKey(5), individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress5("Line 5").build()).build(),
                        individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().build()).build()),
                arguments("PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID", PG_COMPANY_TELEPHONE, organisation,
                        ParentGuardianInformation.parentGuardianInformation().withValuesFrom(organisation).withCompanyTelephoneNumber(null).build()),
                arguments("PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID", pgOrgAddressKey(2), organisation,
                        organisationParentGuardian(validGuardianAddress().withAddress2(null).withAddress3("Line 3").withAddress4("Line 4").withAddress5("Line 5").build()).build()),
                arguments("PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID", pgOrgAddressKey(3), organisation,
                        organisationParentGuardian(validGuardianAddress().withAddress4("Line 4").withAddress5("Line 5").build()).build()),
                arguments("PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID", pgOrgAddressKey(4), organisation,
                        organisationParentGuardian(validGuardianAddress().withAddress3("Line 3").withAddress5("Line 5").build()).build()),
                arguments("PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID", pgOrgAddressKey(5), organisation,
                        organisationParentGuardian(validGuardianAddress().withAddress3("Line 3").withAddress4("Line 4").build()).build()),
                arguments("INVALID_GUARDIAN_ORGANISATION_POST_CODE", PG_ORG_POSTCODE, organisation,
                        organisationParentGuardian(validGuardianAddress().withPostcode(null).withAddress3("Line 3").withAddress4("Line 4").withAddress5("Line 5").build()).build())
        );
    }

    @ParameterizedTest(name = "{0} @ {1}")
    @MethodSource("nullings")
    void shouldNullExactlyTheFieldNamedByTheProblemKey(final String code, final String key, final ParentGuardianInformation input, final ParentGuardianInformation expected) {
        assertThat(sanitise(input, List.of(problem(code, key))), is(expected));
    }

    // REJECT outcomes are never sanitised: the case is rejected, so the defendant never leaves the aggregate.
    @ParameterizedTest
    @CsvSource({
            "PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID, individual_parentGuardianInformation_personalInformation_address_address1",
            "INVALID_GUARDIAN_POST_CODE,                  individual_parentGuardianInformation_personalInformation_address_postcode"
    })
    void shouldLeaveRejectFieldsUntouched(final String code, final String key) {
        final ParentGuardianInformation guardian = validIndividualParentGuardian().build();

        assertThat(sanitise(guardian, List.of(problem(code, key))), is(guardian));
    }

    // AC-S1-005 sanitiser half: numeric codes are mapped with no problem present (AC-017); an unmappable
    // or absent gender defaults to NOT_KNOWN; a valid CP name stays byte-for-byte (AC-043).
    @ParameterizedTest(name = "individual gender \"{0}\" -> {1}")
    @CsvSource(nullValues = "NONE", value = {
            "0,    NOT_KNOWN",
            "1,    MALE",
            "2,    FEMALE",
            "9,    NOT_SPECIFIED",
            "MALE, MALE",
            "X,    NOT_KNOWN",
            "NONE, NOT_KNOWN"
    })
    void shouldNormaliseIndividualGuardianGender(final String gender, final String expected) {
        final List<Problem> problems = LibraGenderCode.normalise(gender).isPresent() ? List.of() : List.of(problem("PARENT_GUARDIAN_GENDER_INVALID", PG_GENDER));

        assertThat(sanitise(validIndividualParentGuardian().withGender(gender).build(), problems).getGender(), is(expected));
    }

    // AC-S1-006 / Q13: gender is never read or written for an organisation guardian.
    @Test
    void shouldNeverTouchGenderOfOrganisationGuardian() {
        final ParentGuardianInformation organisation = organisationParentGuardian(validGuardianAddress().build()).build();

        assertThat(sanitise(organisation, List.of()), is(organisation));
    }

    @Test
    void shouldLeaveDefendantWithoutGuardianUnchanged() {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID, null);
        final MigratedDefendant.Builder builder = MigratedDefendant.migratedDefendant().withValuesFrom(defendant);

        LibraParentGuardianSanitiser.sanitise(builder, List.of());

        assertThat(builder.build(), is(defendant));
    }

    static Stream<Arguments> reviewedRegexes() {
        return Stream.of(
                arguments("CP_TELEPHONE", "^[0-9+ \\-]{10,}$", "0".repeat(50_000) + "x"),
                arguments("LIBRA_GUARDIAN_EMAIL", "^[0-9A-Za-z'._-]{1,127}@[0-9A-Za-z'._-]{1,127}$", "a".repeat(127) + "@" + "a".repeat(50_000)),
                arguments("NO_FIXED_ABODE_POST_CODE", "^[zZ][zZ]99 ?[0-9][a-zA-Z]{2}$", "ZZ99" + " ".repeat(50_000)));
    }

    // AC-S5-003 (NFR-004): the three new Constants are exactly the reviewed single-character-class, bounded
    // patterns (02-design.md C8), and an adversarial input fails fast rather than backtracking.
    @ParameterizedTest(name = "{0}")
    @MethodSource("reviewedRegexes")
    void shouldShipOnlyTheReviewedBoundedRegexes(final String constant, final String reviewedRegex, final String adversarialInput) {
        final String regex = Constants.valueOf(constant).getValue();

        assertThat(regex, is(reviewedRegex));
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertThat(Pattern.compile(regex).matcher(adversarialInput).matches(), is(false)));
    }

    private static ParentGuardianInformation sanitise(final ParentGuardianInformation guardian, final List<Problem> problems) {
        final MigratedDefendant.Builder builder = MigratedDefendant.migratedDefendant().withValuesFrom(defendantWithParentGuardian(DEFENDANT_ID, guardian));
        LibraParentGuardianSanitiser.sanitise(builder, problems);
        return builder.build().getIndividual().getParentGuardianInformation();
    }

    private static Problem problem(final String code, final String key) {
        return new Problem(code, List.of(new ProblemValue(null, key, key)));
    }
}
