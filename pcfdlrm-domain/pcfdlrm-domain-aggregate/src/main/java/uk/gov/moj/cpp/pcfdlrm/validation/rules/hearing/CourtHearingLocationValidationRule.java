package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

public class CourtHearingLocationValidationRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    private final CourtHearingLocationOuCodeValidationRule ouCodeValidationRule = new CourtHearingLocationOuCodeValidationRule();
    private final CourtRoomIdValidationRule courtRoomIdValidationRule = new CourtRoomIdValidationRule();

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (migratedHearingWithReferenceData.getMigratedHearing() == null) {
            return VALID;
        }

        final ValidationResult ouCodeResult = ouCodeValidationRule.validate(migratedHearingWithReferenceData, referenceDataQueryService);

        if (!ouCodeResult.equals(VALID)) {
            return ouCodeResult;
        }

        return courtRoomIdValidationRule.validate(migratedHearingWithReferenceData, referenceDataQueryService);
    }
}
