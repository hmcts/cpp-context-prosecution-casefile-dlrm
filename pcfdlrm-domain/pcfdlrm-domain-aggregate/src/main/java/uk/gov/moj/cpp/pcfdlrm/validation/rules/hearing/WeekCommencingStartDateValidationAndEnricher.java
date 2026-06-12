package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.DateValidation.validationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedWeekCommencingDate;

public class WeekCommencingStartDateValidationAndEnricher implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {
    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String startDate = ofNullable(migratedHearingWithReferenceData.getMigratedHearing().getWeekCommencingDate())
                .map(MigratedWeekCommencingDate::getStartDate)
                .orElse(null);

        return validationResult(startDate);
    }

}
