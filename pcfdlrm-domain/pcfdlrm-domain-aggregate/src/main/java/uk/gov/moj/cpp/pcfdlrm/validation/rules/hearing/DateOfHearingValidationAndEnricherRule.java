package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_DATE_OF_HEARING;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateOfHearingValidationAndEnricherRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        Optional<String> dateOfHearingOpt = Optional.ofNullable( migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing());
        if(dateOfHearingOpt.isEmpty()){
            return  VALID;
        }

        final String dateOfHearing = dateOfHearingOpt.get().trim();

        if(dateOfHearing.isEmpty()){
            return  VALID;
        }

        final LocalDate dateOfHearingDateFormat = convertToLocalDate(dateOfHearing);

        final boolean isBeforeOffenceStartDate = migratedHearingWithReferenceData.getDefendants().stream()
                .flatMap(e -> e.getOffences().stream())
                .anyMatch(offence -> dateOfHearingDateFormat.isBefore(offence.getOffenceCommittedDate()));

        if (isBeforeOffenceStartDate) {
            return newValidationResult(of(newProblem(DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE, new ProblemValue(null, DEFENDANT_DATE_OF_HEARING.getValue(), dateOfHearing))));
        } else {
            return VALID;
        }
    }

    private LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }


}
