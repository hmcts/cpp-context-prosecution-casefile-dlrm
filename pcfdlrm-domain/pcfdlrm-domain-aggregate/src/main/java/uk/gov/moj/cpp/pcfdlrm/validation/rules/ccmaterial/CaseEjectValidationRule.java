package uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial;


import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.CASE_ALREADY_EJECTED;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.Optional;

public class CaseEjectValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData input, ReferenceDataQueryService context) {
        return newValidationResult(Optional.of(input).filter(CaseDocumentWithReferenceData::isCaseEjected)
                .map(e -> newProblem(CASE_ALREADY_EJECTED, "documentType", Optional.ofNullable(input.getDocumentType()).orElse(""))));
    }
}
