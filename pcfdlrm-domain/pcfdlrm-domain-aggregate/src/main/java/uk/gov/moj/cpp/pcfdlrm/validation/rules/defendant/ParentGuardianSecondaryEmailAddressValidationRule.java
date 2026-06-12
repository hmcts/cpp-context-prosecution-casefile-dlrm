package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.regex.Pattern;

public class ParentGuardianSecondaryEmailAddressValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern EMAIL_REGEX = Pattern.compile(Constants.EMAIL.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null || !doesParentGuardianSecondaryEmailExist(defendantWithReferenceData.getDefendant().getIndividual())) {
            return VALID;
        }

        final String secondaryEmailAddress = defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation().getContactDetails().getSecondaryEmail();
        return EMAIL_REGEX.matcher(secondaryEmailAddress).matches() ?
                VALID :
                newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID, new ProblemValue(null, PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS.getValue(), secondaryEmailAddress))));
    }

    private boolean doesParentGuardianSecondaryEmailExist(final Individual individual) {
        return  individual.getParentGuardianInformation() != null &&
                individual.getParentGuardianInformation().getPersonalInformation() != null &&
                individual.getParentGuardianInformation().getPersonalInformation().getContactDetails() != null &&
                individual.getParentGuardianInformation().getPersonalInformation().getContactDetails().getSecondaryEmail() != null;
    }
}
