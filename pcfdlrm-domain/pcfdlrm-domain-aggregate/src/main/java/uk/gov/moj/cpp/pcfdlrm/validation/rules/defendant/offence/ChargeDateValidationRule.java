package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChargeDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        if (isNull(defendantWithReferenceData.getCaseDetails().getFeeStatus())) {
            final List<MigratedOffence> offenceList = defendantWithReferenceData.getDefendant().getOffences().stream().filter(Objects::nonNull).filter(offence -> offence.getChargeDate() == null).collect(Collectors.toList());
            offenceList.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_CHARGE_DATE.getValue(), "Charge date not provided")));
        }

        final List<MigratedOffence> chargeDateIsGreaterThanCurrentDate = defendantWithReferenceData.getDefendant().getOffences().stream().filter(offence -> offence.getChargeDate() != null && offence.getChargeDate().isAfter(LocalDate.now(ZoneId.of("Europe/London")))).collect(Collectors.toList());

        chargeDateIsGreaterThanCurrentDate.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_CHARGE_DATE.getValue(),
                offence.getChargeDate().toString())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.CHARGE_DATE_IN_FUTURE, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
