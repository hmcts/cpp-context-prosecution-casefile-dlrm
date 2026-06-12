package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.CASE_IS_IN_SESSION;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;

public class SjpCaseInSessionValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData input, ReferenceDataQueryService context) {
        return newValidationResult(of(input).filter(CaseDocumentWithReferenceData::isCaseAssigned).map(prosecutionCase -> newProblem(CASE_IS_IN_SESSION,
                new ProblemValue(null, "caseInSession", "true"))));
    }
}
