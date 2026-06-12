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


public class PoliceForceCodeValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String policeForceCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceForceCode();

        if(policeForceCode == null) {
            return VALID;
        }

        if(referenceDataQueryService.retrievePoliceForceCode().stream().anyMatch( x -> x.getPoliceForceCode().equals(policeForceCode))) {
            return VALID;
        }else {
            return newValidationResult(of(Problems.newProblem(ProblemCode.POLICE_FORCE_CODE_INVALID, new ProblemValue(null, FieldName.POLICE_FORCE_CODE.getValue(), policeForceCode))));
        }
    }
}
