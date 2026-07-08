package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_HEARING_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.util.Optional;

class CourtHearingLocationOuCodeValidationRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    private static final int COURT_HEARING_OU_CODE_LENGTH = 7;

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String courtHearingLocation = migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation();
        final ReferenceDataVO referenceDataVO = migratedHearingWithReferenceData.getReferenceDataVO();
        referenceDataVO.setCourtHearingLocation(courtHearingLocation);

        if (referenceDataVO.getOrganisationUnitWithCourtroomsReferenceData().isPresent()) {
            return VALID;
        }

        if (isValidOuCode(courtHearingLocation)) {
            final Optional<OrganisationUnitWithCourtroomsReferenceData> resolved = referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(courtHearingLocation);
            if (resolved.isPresent()) {
                referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(resolved.get());
                return VALID;
            }
        }

        return newValidationResult(of(newProblem(COURT_HEARING_LOCATION_OUCODE_INVALID, new ProblemValue(null, COURT_HEARING_LOCATION.getValue(), courtHearingLocation))));
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGTH == ouCode.length();
    }
}
