package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.CORPORATE_DEFENDANT_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.regex.Pattern;

public class CorporateDefendantPrimaryEmailAddressValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern EMAIL_REGEX = Pattern.compile(Constants.EMAIL.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getEmailAddress1() == null) {
            return VALID;
        }

        final String primaryEmailAddress = defendantWithReferenceData.getDefendant().getEmailAddress1();
        if (primaryEmailAddress == null) {
            return VALID;
        }
        return EMAIL_REGEX.matcher(primaryEmailAddress).matches() ?
                VALID :
                newValidationResult(of(newProblem(DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID, new ProblemValue(null, CORPORATE_DEFENDANT_PRIMARY_EMAIL_ADDRESS.getValue(), primaryEmailAddress))));
    }
}
