package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_HOME_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_WORK_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_HOME_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_MOBILE_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_WORK_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.INDIVIDUAL;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.ORGANISATION;
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

public class LibraParentGuardianTelephoneValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern CP_TELEPHONE = Pattern.compile(Constants.CP_TELEPHONE.getValue());

    private final ParentGuardianShape mode;

    public LibraParentGuardianTelephoneValidationRule(final ParentGuardianShape mode) {
        if (mode != INDIVIDUAL && mode != ORGANISATION) {
            throw new IllegalArgumentException("Telephone rule mode must be INDIVIDUAL or ORGANISATION, was " + mode);
        }
        this.mode = mode;
    }

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ParentGuardianInformation parentGuardianInformation = parentGuardianOf(defendantWithReferenceData.getDefendant());
        if (parentGuardianInformation == null) {
            return ValidationResult.VALID;
        }

        final List<Problem> problems = new ArrayList<>();
        if (mode == ORGANISATION) {
            check(parentGuardianInformation.getCompanyTelephoneNumber(), PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID, PARENT_GUARDIAN_COMPANY_TELEPHONE_NUMBER, problems);
        } else {
            Optional.ofNullable(parentGuardianInformation.getPersonalInformation())
                    .map(PersonalInformation::getContactDetails)
                    .ifPresent(contactDetails -> checkIndividual(contactDetails, problems));
        }
        return newValidationResult(problems);
    }

    private static void checkIndividual(final ContactDetails contactDetails, final List<Problem> problems) {
        check(contactDetails.getWork(), PARENT_GUARDIAN_WORK_TELEPHONE_INVALID, PARENT_GUARDIAN_WORK_TELEPHONE, problems);
        check(contactDetails.getHome(), PARENT_GUARDIAN_HOME_TELEPHONE_INVALID, PARENT_GUARDIAN_HOME_TELEPHONE, problems);
        check(contactDetails.getMobile(), PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID, PARENT_GUARDIAN_MOBILE_TELEPHONE, problems);
    }

    private static void check(final String telephoneNumber, final ProblemCode code, final FieldName fieldName, final List<Problem> problems) {
        if (telephoneNumber != null && !CP_TELEPHONE.matcher(telephoneNumber).matches()) {
            problems.add(redactedProblem(code, fieldName));
        }
    }
}
