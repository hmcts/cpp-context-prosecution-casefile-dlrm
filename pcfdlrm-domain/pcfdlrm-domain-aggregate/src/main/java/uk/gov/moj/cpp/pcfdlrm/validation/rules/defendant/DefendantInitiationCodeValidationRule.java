package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;

public class DefendantInitiationCodeValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    public static final String INITIATION_CODE_J = "J";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String defendantInitiationCode = defendantWithReferenceData.getDefendant().getInitiationCode();

        if (nonNull(defendantInitiationCode)) {

            final String caseInitiationCode = defendantWithReferenceData.getCaseDetails().getInitiationCode();
            if (!caseInitiationCode.equals(INITIATION_CODE_J) && defendantInitiationCode.equals(INITIATION_CODE_J)) {
                return ValidationResult.newValidationResult(of(Problems.newProblem(ProblemCode.DEFENDANT_INITIATION_CODE_INVALID, new ProblemValue(null, FieldName.DEFENDANT_INITIATION_CODE.getValue(), defendantInitiationCode))));
            }
            if (caseInitiationCode.equals(INITIATION_CODE_J) && !defendantInitiationCode.equals(INITIATION_CODE_J)) {
                return ValidationResult.newValidationResult(of(Problems.newProblem(ProblemCode.DEFENDANT_INITIATION_CODE_INVALID, new ProblemValue(null, FieldName.DEFENDANT_INITIATION_CODE.getValue(), defendantInitiationCode))));
            }
        }
        return ValidationResult.VALID;
    }
}