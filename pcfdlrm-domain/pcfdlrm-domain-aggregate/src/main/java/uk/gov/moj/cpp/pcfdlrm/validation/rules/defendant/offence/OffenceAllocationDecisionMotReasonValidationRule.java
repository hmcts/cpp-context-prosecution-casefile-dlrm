package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_ALLOCATION_DECISION_MOT_REASON_ID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DD-43888 (LIBRA only): an allocation decision's motReasonId must be a CP mode of trial reason.
 */
class OffenceAllocationDecisionMotReasonValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final Set<String> modeOfTrialReasonIds = defendantWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().stream()
                .map(ModeOfTrialReasonsReferenceData::getId)
                .collect(toSet());

        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> offence.getAllocationDecision() != null)
                .filter(offence -> !modeOfTrialReasonIds.contains(Objects.toString(offence.getAllocationDecision().getMotReasonId(), null)))
                .map(offence -> new ProblemValue(offence.getOffenceId().toString(), OFFENCE_ALLOCATION_DECISION_MOT_REASON_ID.getValue(),
                        Objects.toString(offence.getAllocationDecision().getMotReasonId(), null)))
                .toList();

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.ALLOCATION_DECISION_INVALID, problemValues.toArray(new ProblemValue[0]))));
    }
}
