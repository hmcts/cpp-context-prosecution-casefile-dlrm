package uk.gov.moj.cpp.pcfdlrm.validation.rules;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.SENDING_COURT;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

public class SendingCourtValidationRules implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    private static final int COURT_HEARING_OU_CODE_LENGTH = 7;

    @Override
    public ValidationResult validate(ProsecutionWithReferenceData prosecutionWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {

        final String sendingCourt = prosecutionWithReferenceData.getProsecution().getCaseDetails().getSendingCourt();

        if (isNull(sendingCourt)) return VALID;

        if (isValidOuCode(sendingCourt) &&
                prosecutionWithReferenceData.getReferenceDataVO().getSendingCourtOrganisationUnit().isPresent()) {
            return ValidationResult.VALID;
        }

        return newValidationResult(of(newProblem(COURT_LOCATION_OUCODE_INVALID, new ProblemValue(null, SENDING_COURT.getValue(), sendingCourt))));
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGTH == ouCode.length();
    }
}
