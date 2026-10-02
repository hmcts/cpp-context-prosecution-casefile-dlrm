package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.organisationParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_COMPANY_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_HOME;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_MOBILE;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_WORK;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.INDIVIDUAL;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.ORGANISATION;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * DD-43501 FR-004 (individual work/home/mobile, AC-S1-002) and FR-014 (organisation
 * companyTelephoneNumber, AC-S2-003) against {@code Constants.CP_TELEPHONE} {@code ^[0-9+ \-]{10,}$}.
 * Every problem is redacted: {@code ProblemValue.value} is the field key (AC-S1-008, AC-S2-006).
 */
class LibraParentGuardianTelephoneValidationRuleTest {

    private static final Map<String, String> KEYS = Map.of("work", PG_WORK, "home", PG_HOME, "mobile", PG_MOBILE);

    @ParameterizedTest(name = "{0} = \"{1}\" -> {2}")
    @CsvSource(nullValues = "NONE", value = {
            "work,   +44 20 7946 0000,  NONE",
            "work,   0123456789,        NONE",                                      // exactly 10 characters
            "home,   020-7946-0000,     NONE",
            "work,   NONE,              NONE",                                      // absent
            "work,   012345678,         PARENT_GUARDIAN_WORK_TELEPHONE_INVALID",    // 9 characters
            "home,   012345678,         PARENT_GUARDIAN_HOME_TELEPHONE_INVALID",
            "mobile, 07700-ABC-123,     PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID",
            "mobile, (020) 7946 0000,   PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID",
            "home,   020.7946.0000,     PARENT_GUARDIAN_HOME_TELEPHONE_INVALID",
            "home,   '020\t79460000',   PARENT_GUARDIAN_HOME_TELEPHONE_INVALID",    // tab
            "home,   '',                PARENT_GUARDIAN_HOME_TELEPHONE_INVALID"     // blank but present (D-1)
    })
    void shouldValidateIndividualGuardianTelephoneNumbers(final String field, final String value, final String expectedCode) {
        final ParentGuardianInformation guardian = individualParentGuardian(contactDetailsWith(field, value), validGuardianAddress().build()).build();

        final List<Problem> problems = validate(INDIVIDUAL, guardian);

        assertThat(describe(problems), is(expectedCode == null ? List.of() : List.of(expectedCode + " " + KEYS.get(field))));
        assertRedacted(problems);
    }

    // AC-S1-002 exactly: work kept, home and mobile each raise their own problem, in field order.
    @Test
    void shouldRaiseOneProblemPerInvalidNumber() {
        final ContactDetails contactDetails = validGuardianContactDetails().withWork("+44 20 7946 0000").withHome("012345678").withMobile("07700-ABC-123").build();

        final List<Problem> problems = validate(INDIVIDUAL, individualParentGuardian(contactDetails, validGuardianAddress().build()).build());

        assertThat(describe(problems), is(List.of(
                "PARENT_GUARDIAN_HOME_TELEPHONE_INVALID " + PG_HOME,
                "PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID " + PG_MOBILE)));
        assertRedacted(problems);
    }

    @ParameterizedTest(name = "companyTelephoneNumber = \"{0}\" -> {1}")
    @CsvSource(nullValues = "NONE", value = {
            "0161 496 0000, NONE",
            "NONE,          NONE",
            "ext 22,        PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID",
            "016149600,     PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID",
            "'',            PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID"
    })
    void shouldValidateOrganisationGuardianCompanyTelephoneNumber(final String value, final String expectedCode) {
        final ParentGuardianInformation guardian = organisationParentGuardian(validGuardianAddress().build()).withCompanyTelephoneNumber(value).build();

        final List<Problem> problems = validate(ORGANISATION, guardian);

        assertThat(describe(problems), is(expectedCode == null ? List.of() : List.of(expectedCode + " " + PG_COMPANY_TELEPHONE)));
        assertRedacted(problems);
    }

    private static List<Problem> validate(final ParentGuardianShape mode, final ParentGuardianInformation guardian) {
        return new LibraParentGuardianTelephoneValidationRule(mode)
                .validate(new DefendantWithReferenceData(defendantWithParentGuardian(DEFENDANT_ID, guardian), new ReferenceDataVO(), CaseDetails.caseDetails().build()), null)
                .problems();
    }

    private static ContactDetails contactDetailsWith(final String field, final String value) {
        final ContactDetails.Builder builder = validGuardianContactDetails();
        switch (field) {
            case "work" -> builder.withWork(value);
            case "home" -> builder.withHome(value);
            case "mobile" -> builder.withMobile(value);
            default -> throw new IllegalArgumentException(field);
        }
        return builder.build();
    }

    private static List<String> describe(final List<Problem> problems) {
        return problems.stream().map(p -> p.getCode() + " " + p.getValues().get(0).getKey()).toList();
    }

    private static void assertRedacted(final List<Problem> problems) {
        assertThat(problems.stream().map(p -> p.getValues().get(0).getValue().equals(p.getValues().get(0).getKey())).toList(), everyItem(is(true)));
    }
}
