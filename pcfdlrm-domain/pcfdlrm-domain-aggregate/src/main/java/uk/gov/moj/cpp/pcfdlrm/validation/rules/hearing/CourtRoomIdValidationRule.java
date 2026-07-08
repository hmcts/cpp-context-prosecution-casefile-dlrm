package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURTROOM_ID_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.COURT_ROOM_ID;
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

class CourtRoomIdValidationRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final Integer courtRoomId = migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId();

        if (nonNull(courtRoomId) && courtRoomId > 0) {
            final Optional<OrganisationUnitWithCourtroomsReferenceData> organisationUnit =
                    migratedHearingWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData();

            if (organisationUnit.isPresent()) {
                final boolean matched = organisationUnit.get().getCourtrooms().stream()
                        .anyMatch(courtRoom -> courtRoomId.equals(courtRoom.getCourtroomId()));
                if (!matched) {
                    return newValidationResult(of(newProblem(COURTROOM_ID_INVALID, new ProblemValue(null, COURT_ROOM_ID.getValue(), String.valueOf(courtRoomId)))));
                }
            }
        }

        return VALID;
    }
}
