package uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors;


import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;


/**
 * \subsection Validation Rules
 * <p>
 * ProsecutorSJPValidationRule  check SJP is active via prosecution reference data
 */

public class ProsecutorSJPValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(ProsecutionWithReferenceData prosecution, ReferenceDataQueryService query) {

        final Prosecutor prosecutor = prosecution.getProsecution().getCaseDetails().getProsecutor();
        if (prosecutor.getReferenceData() != null && prosecutor.getReferenceData().getSjpFlag()) {
            return ValidationResult.VALID;
        } else {
            return ValidationResult.newValidationResult(of(Problems.newProblem(ProblemCode.PROSECUTOR_NOT_RECOGNISED_AS_AN_AUTHORISED_SJP_PROSECUTOR,
                    new ProblemValue(null,
                            FieldName.PROSECUTING_AUTHORITY.getValue(),
                            prosecutor.getProsecutingAuthority()))));
        }
    }
}



