package uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;

import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentDefendantLevelValidationRuleTest {

    private static final String VALID_DOCUMENT_TYPE = "Valid document type";
    private static final String DEFENDANT_LEVEL = "Defendant level";
    private static final String CASE_LEVEL = "Case level";
    private static final String VALID_PROSECUTOR_DEFENDANT_ID = "ABC";
    private static final String VALID_PROSECUTOR_DEFENDANT_ID_LOWER_CASE = "abc";
    private static final String INVALID_PROSECUTOR_DEFENDANT_ID = "BCD";

    private CCDocumentDefendantLevelValidationRule ccDocumentDefendantLevelValidationRule = new CCDocumentDefendantLevelValidationRule();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldReturnProblemWhenProsecutorDefendantIdMissingForDefendantLevelDocument() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), null, getDefendants(UUID.randomUUID()), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.DEFENDANT_ID_REQUIRED, "prosecutorDefendantId", "");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldReturnProblemWhenProsecutorDefendantIdNotValidForDefendantLevelDocument() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), INVALID_PROSECUTOR_DEFENDANT_ID, getDefendants(UUID.randomUUID()), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.DEFENDANT_ID_INVALID, "prosecutorDefendantId", "BCD");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldReturnNoProblemWhenProsecutorDefendantIdValid() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final UUID defendantId = randomUUID();
        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), defendantId.toString(), getDefendants(defendantId), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoProblemWhenCaseLevelDocumentAndMissingProsecutorDefendantID() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockCaseLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), null, getDefendants(UUID.randomUUID()), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoProblemWhenDocumentTypeAccessReferenceDataListIsNotEmpty() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = new CaseDocumentWithReferenceData(randomUUID(), false, material().build(),
                VALID_PROSECUTOR_DEFENDANT_ID, getDefendants(UUID.randomUUID()), VALID_DOCUMENT_TYPE, false, false);

        final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList = List.of(
                documentTypeAccessReferenceData()
                        .withSection("ABC")
                        .build());

        caseDocumentWithReferenceData.setDocumentTypeAccessReferenceDataList(documentTypeAccessReferenceDataList);

        final ValidationResult validationResult = ccDocumentDefendantLevelValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService);


        final Optional<Problem> actualProblem = validationResult.problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));

        verify(referenceDataQueryService, never()).retrieveDocumentsTypeAccess();
    }

    @Test
    public void shouldReturnNoProblemWhenCaseLevelDocumentAndValidProsecutorDefendantID() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockCaseLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), VALID_PROSECUTOR_DEFENDANT_ID, getDefendants(UUID.randomUUID()), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    private List<MigratedDefendant> getDefendants(final UUID defendantId) {
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(migratedDefendant()
                .withProsecutorDefendantReference(VALID_PROSECUTOR_DEFENDANT_ID)
                .withId(defendantId)
                .build());
        return defendants;
    }

    private List<DocumentTypeAccessReferenceData> getMockCaseLevelDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE)
                .withDocumentCategory(CASE_LEVEL)
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }

    private List<DocumentTypeAccessReferenceData> getMockDefendantLevelDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE)
                .withDocumentCategory(DEFENDANT_LEVEL)
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }
}
