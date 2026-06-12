package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;


import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;

import java.util.regex.Pattern;

public class PncIdValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern PNC_ID_FORMAT = Pattern.compile(Constants.PNC_ID_REGEX.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getPncIdentifier() == null) {
            return ValidationResult.VALID;
        }

        final String pncId = defendantWithReferenceData.getDefendant().getPncIdentifier();

        return PNC_ID_FORMAT.matcher(pncId).matches() ?
                ValidationResult.VALID :
                ValidationResult.newValidationResult(of(Problems.newProblem(ProblemCode.INVALID_PNC_ID, new ProblemValue(null, FieldName.PNC_ID.getValue(), pncId))));
    }
}
