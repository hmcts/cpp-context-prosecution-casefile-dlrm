package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class DateValidation {
    private DateValidation() {
    }

    public static ValidationResult validationResult(String inputDate) {


        if (isNull(inputDate)) {
            return VALID;
        }

        final String dateOfHearing = inputDate.trim();
        if (dateOfHearing.isEmpty()) {
            return VALID;
        }
        final LocalDate hearingDate = convertToLocalDate(dateOfHearing);

        if (hearingDate.isBefore(LocalDate.now())) {
            return newValidationResult(of(Problems.newProblem(ProblemCode.DATE_OF_HEARING_IN_THE_PAST, new ProblemValue(null, FieldName.DEFENDANT_DATE_OF_HEARING.getValue(), dateOfHearing))));
        }
        return VALID;
    }

    private static LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
