package uk.gov.moj.cpp.pcfdlrm.validation.rules;


import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.CASE_REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;


import java.util.Optional;

public class CourtReferralCreatedValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {
    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData caseDocumentWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {

       return newValidationResult(Optional.of(caseDocumentWithReferenceData)
                .filter(CaseDocumentWithReferenceData::isCaseReferredToCourt)
                .map(prosecutionAlreadyAccepted ->newProblem(CASE_REFERRED_TO_OPEN_COURT,
                        new ProblemValue(null,"referralReasonId", caseDocumentWithReferenceData.getReferralReasonId().toString()))));
    }
}