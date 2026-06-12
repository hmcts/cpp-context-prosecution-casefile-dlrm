package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Arrays.asList;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;

import java.util.List;
import java.util.Optional;

public class SjpDocumentTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    private static final List<String> VALID_SJP_DOCUMENT_TYPES = asList("SJPN", "CITN", "PLEA", "FINANCIAL_MEANS", "DISQUALIFICATION_REPLY_SLIP");
    private static final String OTHER_DOCUMENT_TYPES = "OTHER-";

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        return newValidationResult(Optional.of(caseDocumentWithReferenceData)
                .filter(m -> !VALID_SJP_DOCUMENT_TYPES.contains(caseDocumentWithReferenceData.getMaterial().getDocumentType()))
                .filter(m -> !caseDocumentWithReferenceData.getMaterial().getDocumentType().startsWith(OTHER_DOCUMENT_TYPES))
                .map(m -> newProblem(INVALID_DOCUMENT_TYPE, "documentType", Optional.ofNullable(caseDocumentWithReferenceData.getMaterial().getDocumentType()).orElse(""))));
    }
}
