package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.DateValidation.validationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

public class DateOfHearingPastDateValidationAndEnricherRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String dateOfHearing = migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing();
        return validationResult(dateOfHearing);
    }
}
