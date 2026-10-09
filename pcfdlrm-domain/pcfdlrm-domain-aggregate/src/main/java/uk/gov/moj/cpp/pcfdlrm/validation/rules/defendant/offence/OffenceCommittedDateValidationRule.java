package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * DD-43502 (LIBRA only): the offence committed date must not be in the future. Missing is already
 * rejected by the LIBRA intake schema.
 */
class OffenceCommittedDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> offence.getOffenceCommittedDate() != null && offence.getOffenceCommittedDate().isAfter(today))
                .map(offence -> new ProblemValue(offence.getOffenceId().toString(), OFFENCE_COMMITTED_DATE.getValue(),
                        offence.getOffenceCommittedDate().toString()))
                .toList();

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.OFFENCE_COMMITTED_DATE_IN_FUTURE, problemValues.toArray(new ProblemValue[0]))));
    }
}
