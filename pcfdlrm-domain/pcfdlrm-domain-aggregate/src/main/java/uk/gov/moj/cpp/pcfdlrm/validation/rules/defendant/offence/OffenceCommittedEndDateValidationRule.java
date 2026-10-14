package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_COMMITTED_END_DATE;
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
 * DD-43502 (LIBRA only): offence date code 4 ("between") needs an end date that is after the
 * committed date and not in the future.
 */
class OffenceCommittedEndDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final int OFFENCE_DATE_CODE_BETWEEN = 4;

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> Objects.equals(OFFENCE_DATE_CODE_BETWEEN, offence.getOffenceDateCode()))
                .filter(offence -> isInvalidEndDate(offence, today))
                .map(offence -> new ProblemValue(offence.getOffenceId().toString(), OFFENCE_COMMITTED_END_DATE.getValue(),
                        Objects.toString(offence.getOffenceCommittedEndDate(), null)))
                .toList();

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.OFFENCE_COMMITTED_END_DATE_INVALID, problemValues.toArray(new ProblemValue[0]))));
    }

    private static boolean isInvalidEndDate(final MigratedOffence offence, final LocalDate today) {
        final LocalDate endDate = offence.getOffenceCommittedEndDate();
        return endDate == null
                || (offence.getOffenceCommittedDate() != null && !endDate.isAfter(offence.getOffenceCommittedDate()))
                || endDate.isAfter(today);
    }
}
