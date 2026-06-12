package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_DOB_IN_FUTURE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_DOB;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.time.ZoneId;

public class DefendantDateOfBirthValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null || defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation().getDateOfBirth() == null) {
            return VALID;
        }

        final LocalDate dateOfBirth = defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation().getDateOfBirth();

        if (dateOfBirth.isAfter(now(ZoneId.of("Europe/London")))) {
            return newValidationResult(of(newProblem(DEFENDANT_DOB_IN_FUTURE, new ProblemValue(null, DEFENDANT_DOB.getValue(), dateOfBirth.toString()))));
        } else {
            return VALID;
        }
    }
}
