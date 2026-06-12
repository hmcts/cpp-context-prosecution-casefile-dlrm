package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

public class ProsecutorReferenceDataValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecution, final ReferenceDataQueryService context) {
        if (prosecution.getProsecution().getCaseDetails().getProsecutor() == null) {
            return VALID;
        }

        if (nonNull(prosecution.getReferenceDataVO().getProsecutorsReferenceData()) &&
                nonNull(prosecution.getReferenceDataVO().getProsecutorsReferenceData().getStandard()) &&
                prosecution.getReferenceDataVO().getProsecutorsReferenceData().getStandard()) {
            return VALID;
        }

        return ofNullable(prosecution.getReferenceDataVO().getProsecutorsReferenceData())
                .map(prosecutorReferenceData -> VALID)
                .orElse(newValidationResult(
                        singletonList(Problems.newProblem(ProblemCode.PROSECUTOR_OUCODE_NOT_RECOGNISED,
                                problemValue()
                                        .withKey(FieldName.PROSECUTING_AUTHORITY.getValue())
                                        .withValue(prosecution.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority())
                                        .build()))
                ));
    }
}
