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

public class CroNumberValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern CRO_NUMBER_FORMAT_ONE = Pattern.compile(Constants.CRO_NUMBER_REGEX_ONE.getValue());
    private static final Pattern CRO_NUMBER_FORMAT_TWO = Pattern.compile(Constants.CRO_NUMBER_REGEX_TWO.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getCroNumber() == null) {
            return ValidationResult.VALID;
        }

        final String croNumber = defendantWithReferenceData.getDefendant().getCroNumber();

        return (CRO_NUMBER_FORMAT_ONE.matcher(croNumber).matches() || CRO_NUMBER_FORMAT_TWO.matcher(croNumber).matches()) ?
                ValidationResult.VALID :
                ValidationResult.newValidationResult(of(Problems.newProblem(ProblemCode.INVALID_CRO_NUMBER, new ProblemValue(null, FieldName.CRO_NUMBER.getValue(), croNumber))));
    }
}
