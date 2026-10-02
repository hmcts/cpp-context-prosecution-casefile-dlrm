package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validIndividualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG_GENDER;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LibraParentGuardianGenderValidationRuleTest {

    @ParameterizedTest(name = "normalise(\"{0}\") -> {1}")
    @CsvSource(nullValues = "NONE", value = {
            "0,             NOT_KNOWN",
            "1,             MALE",
            "2,             FEMALE",
            "9,             NOT_SPECIFIED",
            "' 1 ',         MALE",            // trimmed
            "MALE,          MALE",
            "NOT_SPECIFIED, NOT_SPECIFIED",
            "male,          male",            // CP enum name, case-insensitive, returned unchanged
            "' MALE ',      MALE",            // CP enum name, trimmed
            "3,             NONE",
            "X,             NONE",
            "'',            NONE",
            "'  ',          NONE",
            "NONE,          NONE"
    })
    void shouldNormaliseLibraGenderCode(final String gender, final String expected) {
        assertThat(LibraGenderCode.normalise(gender), is(Optional.ofNullable(expected)));
    }

    @ParameterizedTest(name = "gender \"{0}\" -> warning: {1}")
    @CsvSource(nullValues = "NONE", value = {
            "0,    false",
            "1,    false",
            "2,    false",
            "9,    false",
            "MALE, false",
            "3,    true",
            "X,    true",
            "'',   true",
            "NONE, true"
    })
    void shouldRaiseRedactedGenderProblemOnlyWhenUnmappable(final String gender, final boolean expectProblem) {
        final List<String> problems = new LibraParentGuardianGenderValidationRule()
                .validate(new DefendantWithReferenceData(defendantWithParentGuardian(DEFENDANT_ID, validIndividualParentGuardian().withGender(gender).build()),
                        new ReferenceDataVO(), CaseDetails.caseDetails().build()), null)
                .problems().stream()
                .map(p -> p.getCode() + " " + p.getValues().get(0).getKey() + " " + p.getValues().get(0).getValue())
                .toList();

        assertThat(problems, is(expectProblem ? List.of("PARENT_GUARDIAN_GENDER_INVALID " + PG_GENDER + " " + PG_GENDER) : List.of()));
    }
}
