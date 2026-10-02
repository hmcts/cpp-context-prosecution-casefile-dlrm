package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_GENDER_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_GENDER;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.parentGuardianOf;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.RedactingValidationRule.redactedProblem;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;

/**
 * DD-43501: an individual guardian's gender must be a LIBRA code or a CP gender name. Absent, blank
 * or unmappable raises a redacted {@code PARENT_GUARDIAN_GENDER_INVALID}; {@link LibraParentGuardianSanitiser}
 * applies the {@code NOT_KNOWN} default. Replaces the generic guardian-gender check in LIBRA scope.
 */
public class LibraParentGuardianGenderValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ParentGuardianInformation parentGuardianInformation = parentGuardianOf(defendantWithReferenceData.getDefendant());
        if (parentGuardianInformation == null || LibraGenderCode.normalise(parentGuardianInformation.getGender()).isPresent()) {
            return VALID;
        }
        return newValidationResult(of(redactedProblem(PARENT_GUARDIAN_GENDER_INVALID, PARENT_GUARDIAN_GENDER)));
    }
}
