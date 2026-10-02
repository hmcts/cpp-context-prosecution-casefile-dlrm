package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.parentGuardianOf;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.RedactingValidationRule.redactedProblem;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class LibraParentGuardianEmailValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern LIBRA_GUARDIAN_EMAIL = Pattern.compile(Constants.LIBRA_GUARDIAN_EMAIL.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final Optional<ContactDetails> contactDetails = Optional.ofNullable(parentGuardianOf(defendantWithReferenceData.getDefendant()))
                .map(ParentGuardianInformation::getPersonalInformation)
                .map(PersonalInformation::getContactDetails);
        if (contactDetails.isEmpty()) {
            return ValidationResult.VALID;
        }

        final List<Problem> problems = new ArrayList<>();
        check(contactDetails.get().getPrimaryEmail(), DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID, PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS, problems);
        check(contactDetails.get().getSecondaryEmail(), DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID, PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS, problems);
        return newValidationResult(problems);
    }

    private static void check(final String email, final ProblemCode code, final FieldName fieldName, final List<Problem> problems) {
        if (email != null && !LIBRA_GUARDIAN_EMAIL.matcher(email).matches()) {
            problems.add(redactedProblem(code, fieldName));
        }
    }
}
