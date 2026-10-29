package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

/**
 * DD-43888 (LIBRA only): the allocation decision date is only checked for a valid allocation
 * decision (fail fast).
 */
public class OffenceAllocationDecisionValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private final OffenceAllocationDecisionMotReasonValidationRule motReasonValidationRule = new OffenceAllocationDecisionMotReasonValidationRule();
    private final OffenceAllocationDecisionDateValidationRule dateValidationRule = new OffenceAllocationDecisionDateValidationRule();

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ValidationResult motReasonResult = motReasonValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        if (!motReasonResult.equals(VALID)) {
            return motReasonResult;
        }

        return dateValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);
    }
}
