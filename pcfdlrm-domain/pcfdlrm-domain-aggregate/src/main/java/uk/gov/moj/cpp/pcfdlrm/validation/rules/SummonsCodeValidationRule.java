package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

public class SummonsCodeValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    public static final String SUMMONS_CASE_TYPE = "S";

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String summonsCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode();

        if (!SUMMONS_CASE_TYPE.equals(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode())) {
            return VALID;
        }

        if(summonsCode == null) {
            return newValidationResult(of(Problems.newProblem(ProblemCode.SUMMONS_CODE_INVALID, new ProblemValue(null, FieldName.CASE_SUMMONS_CODE.getValue(), ""))));
        }

        if ((referenceDataQueryService.retrieveSummonsCodes().stream().anyMatch(s -> s.getSummonsCode().equals(summonsCode)))) {
            return VALID;
        } else {
            return newValidationResult(of(Problems.newProblem(ProblemCode.SUMMONS_CODE_INVALID, new ProblemValue(null, FieldName.CASE_SUMMONS_CODE.getValue(), summonsCode))));
        }

    }
}
