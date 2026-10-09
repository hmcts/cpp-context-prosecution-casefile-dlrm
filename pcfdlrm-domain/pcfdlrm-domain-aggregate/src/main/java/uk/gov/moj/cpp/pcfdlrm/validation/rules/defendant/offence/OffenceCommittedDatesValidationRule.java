package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

/**
 * DD-43502 (LIBRA only): the end date is only checked against a valid committed date, so a future
 * committed date does not also raise an end-date problem.
 */
public class OffenceCommittedDatesValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private final OffenceCommittedDateValidationRule committedDateValidationRule = new OffenceCommittedDateValidationRule();
    private final OffenceCommittedEndDateValidationRule committedEndDateValidationRule = new OffenceCommittedEndDateValidationRule();

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ValidationResult committedDateResult = committedDateValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        if (!committedDateResult.equals(VALID)) {
            return committedDateResult;
        }

        return committedEndDateValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);
    }
}
