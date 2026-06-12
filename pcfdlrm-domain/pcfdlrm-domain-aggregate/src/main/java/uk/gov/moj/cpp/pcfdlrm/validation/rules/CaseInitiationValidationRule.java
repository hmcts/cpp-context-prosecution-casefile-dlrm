package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;

public class CaseInitiationValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {


    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String initiationCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode();

        if(isNull(initiationCode)) {
            return newValidationResult(of(Problems.newProblem(ProblemCode.CASE_INITIATION_CODE_INVALID, new ProblemValue(null, FieldName.CASE_INITIATION_CODE.getValue(), ""))));
        }

        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        if (referenceDataVO.getInitiationTypes().stream().anyMatch(x -> x.equals(initiationCode))) {
            return VALID;
        } else {
            return newValidationResult(of(Problems.newProblem(ProblemCode.CASE_INITIATION_CODE_INVALID, new ProblemValue(null, FieldName.CASE_INITIATION_CODE.getValue(), initiationCode))));
        }
    }
}
