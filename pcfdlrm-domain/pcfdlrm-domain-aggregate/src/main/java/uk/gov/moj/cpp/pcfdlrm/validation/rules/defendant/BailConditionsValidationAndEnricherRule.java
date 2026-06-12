package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;


public class BailConditionsValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {


    public static final String CONDITIONAL_BAIL_STATUS = "B";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final String custodyStatus = defendantWithReferenceData.getDefendant().getIndividual().getCustodyStatus();

        if (custodyStatus != null && CONDITIONAL_BAIL_STATUS.equals(custodyStatus) && isBailConditionsEmpty(defendantWithReferenceData)) {
            return newValidationResult(of(Problems.newProblem(ProblemCode.BAIL_CONDITIONS_REQUIRED, new ProblemValue(null, FieldName.DEFENDANT_BAIL_CONDITIONS.getValue(), ""))));

        }

        return VALID;
    }

    private boolean isBailConditionsEmpty(final DefendantWithReferenceData defendantWithReferenceData) {
        return (defendantWithReferenceData.getDefendant().getIndividual() != null) && (defendantWithReferenceData.getDefendant().getIndividual().getBailConditions() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getBailConditions().trim().isEmpty());
    }
}
