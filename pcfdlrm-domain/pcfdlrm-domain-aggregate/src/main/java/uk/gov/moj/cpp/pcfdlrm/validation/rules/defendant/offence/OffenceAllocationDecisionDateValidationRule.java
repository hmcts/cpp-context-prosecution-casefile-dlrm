package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_ALLOCATION_DECISION_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * DD-43888 (LIBRA only): an allocation decision needs an allocation decision date that is not in the
 * future.
 */
class OffenceAllocationDecisionDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> offence.getAllocationDecision() != null)
                .filter(offence -> isInvalidAllocationDecisionDate(offence, today))
                .map(offence -> new ProblemValue(offence.getOffenceId().toString(), OFFENCE_ALLOCATION_DECISION_DATE.getValue(),
                        Objects.toString(offence.getAllocationDecision().getAllocationDecisionDate(), null)))
                .toList();

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.ALLOCATION_DECISION_DATE_INVALID, problemValues.toArray(new ProblemValue[0]))));
    }

    private static boolean isInvalidAllocationDecisionDate(final MigratedOffence offence, final LocalDate today) {
        final LocalDate allocationDecisionDate = offence.getAllocationDecision().getAllocationDecisionDate();
        return allocationDecisionDate == null || allocationDecisionDate.isAfter(today);
    }
}
