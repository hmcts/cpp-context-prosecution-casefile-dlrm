package uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_ID_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_ID_REQUIRED;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.DEFENDANT_LEVEL;

import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

public class CCDocumentDefendantLevelValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
            return getProsecutorDefendantProblem(caseDocumentWithReferenceData, caseDocumentWithReferenceData.getProsecutorDefendantId());
        }

        List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList = caseDocumentWithReferenceData.getDocumentTypeAccessReferenceDataList();

        if (CollectionUtils.isEmpty(documentTypeAccessReferenceDataList)) {
            documentTypeAccessReferenceDataList = referenceDataQueryService.retrieveDocumentsTypeAccess();
        }

        final Optional<DocumentTypeAccessReferenceData> matchedDocumentMetadata = documentTypeAccessReferenceDataList
                .stream()
                .filter(documentMetadataReferenceData ->
                        documentMetadataReferenceData.getSection()
                                .equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentType())).findFirst();

        matchedDocumentMetadata.ifPresent(caseDocumentWithReferenceData::setDocumentTypeAccessReferenceData);

        if (matchedDocumentMetadata.isEmpty()) {
            return VALID;
        }

        return getProsecutorDefendantProblem(caseDocumentWithReferenceData, caseDocumentWithReferenceData.getProsecutorDefendantId());
    }

    private ValidationResult getProsecutorDefendantProblem(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final String prosecutorDefendantId) {
        if (!DEFENDANT_LEVEL.toString().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory())) {
            return VALID;
        }
        final ValidationResult missingIdProblem = getProsecutorDefendantIdMissingProblem(prosecutorDefendantId);
        if (!missingIdProblem.isValid()) {
            return missingIdProblem;
        } else {
            Optional<MigratedDefendant> matchedDefendant = getDefendantFromProsecutorDefendantId(caseDocumentWithReferenceData.getDefendants(), caseDocumentWithReferenceData.getProsecutorDefendantId());
            if (matchedDefendant.isEmpty()) {
                matchedDefendant = getDefendantFromASN(caseDocumentWithReferenceData.getDefendants(), caseDocumentWithReferenceData.getProsecutorDefendantId());
            }
            matchedDefendant.ifPresent(defendant -> caseDocumentWithReferenceData.setDefendantId(fromString(defendant.getId().toString())));
            return matchedDefendant.isPresent() ? VALID : newValidationResult(of(newProblem(DEFENDANT_ID_INVALID, PROSECUTOR_DEFENDANT_ID.getValue(), caseDocumentWithReferenceData.getProsecutorDefendantId())));
        }
    }

    private ValidationResult getProsecutorDefendantIdMissingProblem(final String prosecutorDefendantId) {
        return isEmpty(prosecutorDefendantId) ?
                newValidationResult(of(newProblem(DEFENDANT_ID_REQUIRED, PROSECUTOR_DEFENDANT_ID.getValue(), ""))) :
                VALID;
    }


    private Optional<MigratedDefendant> getDefendantFromProsecutorDefendantId(final List<MigratedDefendant> defendants, final String prosecutorDefendantId) {
        return defendants.stream()
                .filter(defendant -> prosecutorDefendantId.equalsIgnoreCase(Optional.ofNullable(defendant.getId()).map(UUID::toString).orElse(null)))
                .findFirst();
    }

    private Optional<MigratedDefendant> getDefendantFromASN(final List<MigratedDefendant> defendants, final String prosecutorDefendantId) {
        return defendants.stream()
                .filter(defendant -> prosecutorDefendantId.equalsIgnoreCase(defendant.getAsn()))
                .findFirst();
    }
}
