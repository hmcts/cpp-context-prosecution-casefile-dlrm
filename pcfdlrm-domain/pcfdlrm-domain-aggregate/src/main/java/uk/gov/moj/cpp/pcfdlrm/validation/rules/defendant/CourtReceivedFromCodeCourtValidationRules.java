package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;


import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.COURT_RECEIVED_FROM;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;

public class CourtReceivedFromCodeCourtValidationRules extends CourtValidationRules {

    @Override
    public ValidationResult validate(DefendantWithReferenceData defendantWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {
        return validateResult(referenceDataQueryService, COURT_RECEIVED_FROM, defendantWithReferenceData.getCaseDetails().getCourtReceivedFromCode());
    }
}
