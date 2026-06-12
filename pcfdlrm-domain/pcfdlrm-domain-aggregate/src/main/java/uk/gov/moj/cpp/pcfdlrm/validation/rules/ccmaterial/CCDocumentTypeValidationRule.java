package uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.APPLICATIONS;

import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;

public class CCDocumentTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
            return VALID;
        }

        List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList = caseDocumentWithReferenceData.getDocumentTypeAccessReferenceDataList();

        if (CollectionUtils.isEmpty(documentTypeAccessReferenceDataList)) {
            documentTypeAccessReferenceDataList = referenceDataQueryService.retrieveDocumentsTypeAccess();
        }

        final Optional<DocumentTypeAccessReferenceData> matchedDocumentMetadata = documentTypeAccessReferenceDataList
                .stream()
                .filter(documentMetadataReferenceData -> documentMetadataReferenceData.getSection().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentType()))
                .findFirst();

        matchedDocumentMetadata.ifPresent(documentMetadata -> {
            if(caseDocumentWithReferenceData.getCourtApplicationSubject() != null){
                caseDocumentWithReferenceData.setDocumentTypeAccessReferenceData(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                        .withDocumentCategory(APPLICATIONS.toString())
                        .withId(documentMetadata.getId())
                        .withSection(documentMetadata.getSection())
                        .withActionRequired(documentMetadata.getActionRequired())
                        .withCourtDocumentTypeRBAC(documentMetadata.getCourtDocumentTypeRBAC())
                        .withValidFrom(documentMetadata.getValidFrom())
                        .withValidTo(documentMetadata.getValidTo())
                        .withSectionCode(documentMetadata.getSectionCode())
                        .build());
                caseDocumentWithReferenceData.setDocumentCategory(APPLICATIONS.toString());
            }else{
                caseDocumentWithReferenceData.setDocumentTypeAccessReferenceData(documentMetadata);
                caseDocumentWithReferenceData.setDocumentCategory(documentMetadata.getDocumentCategory());
            }
            caseDocumentWithReferenceData.setDocumentType(documentMetadata.getSection());

        });

        if(matchedDocumentMetadata.isEmpty()) {
            return newValidationResult(of(newProblem(INVALID_DOCUMENT_TYPE, DOCUMENT_TYPE.getValue(), Optional.ofNullable(caseDocumentWithReferenceData.getDocumentType()).orElse(""))));
        }

        return VALID;
    }
}
