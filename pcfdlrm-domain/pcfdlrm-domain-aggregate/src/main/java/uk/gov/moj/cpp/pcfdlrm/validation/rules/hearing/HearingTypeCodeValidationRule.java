package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.HEARING_TYPE_CODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.HEARING_TYPE_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;


public class HearingTypeCodeValidationRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final MigratedHearing migratedHearing = migratedHearingWithReferenceData.getMigratedHearing();

        if (isNull(migratedHearing)) {
            return VALID;
        }

        if (isNull(migratedHearing.getHearingType())) {
            return newValidationResult(of(newProblem(HEARING_TYPE_CODE_INVALID, new ProblemValue(null, HEARING_TYPE_CODE.getValue(), null))));
        }

        final String hearingTypeCode = migratedHearing.getHearingType();

        if (referenceDataQueryService.retrieveHearingTypes().getHearingtypes().stream().anyMatch(x -> x.getHearingCode().equals(hearingTypeCode))) {
            return VALID;
        } else {
            return newValidationResult(of(newProblem(HEARING_TYPE_CODE_INVALID, new ProblemValue(null, HEARING_TYPE_CODE.getValue(), hearingTypeCode))));
        }
    }
}