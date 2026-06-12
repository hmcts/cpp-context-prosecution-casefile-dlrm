package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PERCEIVED_BIRTH_YEAR_IN_FUTURE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_PERCEIVED_BIRTH_YEAR;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

public class DefendantPerceivedBirthYearValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null || defendantWithReferenceData.getDefendant().getIndividual().getPerceivedBirthYear() == null) {
            return VALID;
        }

        final String yearOfBirth = defendantWithReferenceData.getDefendant().getIndividual().getPerceivedBirthYear();

        if (Integer.parseInt(yearOfBirth) >= now().getYear()) {
            return  newValidationResult(of(newProblem(DEFENDANT_PERCEIVED_BIRTH_YEAR_IN_FUTURE, new ProblemValue(null, DEFENDANT_PERCEIVED_BIRTH_YEAR.getValue(), yearOfBirth))));
        } else {
            return VALID;
        }
    }
}
