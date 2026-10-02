package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_PRIMARY_EMAIL;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_SECONDARY_EMAIL;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * DD-43501 FR-005 (AC-S1-003) — guardian primary/secondary email against the LIBRA-guardian-only
 * {@code Constants.LIBRA_GUARDIAN_EMAIL} {@code ^[0-9A-Za-z'._-]{1,127}@[0-9A-Za-z'._-]{1,127}$} (Q3).
 * Each value is run against both fields; problems are redacted (AC-S1-008).
 */
class LibraParentGuardianEmailValidationRuleTest {

    private static final String LOCAL_127 = "a".repeat(127);
    private static final String LOCAL_128 = "a".repeat(128);

    static Stream<Arguments> emails() {
        return Stream.of(
                        arguments("taylor.parent@example.org", true),
                        arguments("O'Neil_x-y@example.org", true),
                        arguments(LOCAL_127 + "@example.org", true),           // boundary: 127-character local part kept
                        arguments("a@b", true),                                  // ticket regex does not require a dot in the domain
                        arguments(null, true),                                   // absent
                        arguments(LOCAL_128 + "@example.org", false),          // boundary: 128-character local part removed
                        arguments("a@" + "b".repeat(128), false),
                        arguments("first+tag@example.org", false),
                        arguments("no-at-sign.example.org", false),
                        arguments("two@@example.org", false),
                        arguments("", false))                                    // blank but present (D-1)
                .flatMap(row -> Stream.of(
                        arguments("primaryEmail", row.get()[0], row.get()[1]),
                        arguments("secondaryEmail", row.get()[0], row.get()[1])));
    }

    @ParameterizedTest(name = "{0} = \"{1}\" valid: {2}")
    @MethodSource("emails")
    void shouldValidateGuardianEmailAddresses(final String field, final String email, final boolean valid) {
        final ContactDetails.Builder contactDetails = validGuardianContactDetails();
        if ("primaryEmail".equals(field)) {
            contactDetails.withPrimaryEmail(email);
        } else {
            contactDetails.withSecondaryEmail(email);
        }

        final List<Problem> problems = new LibraParentGuardianEmailValidationRule()
                .validate(new DefendantWithReferenceData(defendantWithParentGuardian(DEFENDANT_ID,
                        individualParentGuardian(contactDetails.build(), validGuardianAddress().build()).build()),
                        new ReferenceDataVO(), CaseDetails.caseDetails().build()), null)
                .problems();

        final String key = "primaryEmail".equals(field) ? PG_PRIMARY_EMAIL : PG_SECONDARY_EMAIL;
        final String code = "primaryEmail".equals(field)
                ? "DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID"
                : "DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID";
        assertThat(problems.stream().map(p -> p.getCode() + " " + p.getValues().get(0).getKey() + " " + p.getValues().get(0).getValue()).toList(),
                is(valid ? List.of() : List.of(code + " " + key + " " + key)));
    }
}
