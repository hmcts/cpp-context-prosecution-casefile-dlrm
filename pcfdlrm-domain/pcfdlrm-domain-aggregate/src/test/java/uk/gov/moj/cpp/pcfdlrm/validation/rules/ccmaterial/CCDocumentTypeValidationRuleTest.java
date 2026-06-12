package uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Material.material;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;


import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentTypeValidationRuleTest {

    private static final String VALID_DOCUMENT_TYPE = "ABC";
    private static final String VALID_DOCUMENT_TYPE_LOWER_CASE = "abc";
    private static final String INVALID_DOCUMENT_TYPE = "BCD";

    private final CCDocumentTypeValidationRule ccDocumentTypeValidationRule = new CCDocumentTypeValidationRule();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;


    @Test
    public void shouldReturnProblemWhenDocumentTypeIsNotInReferenceData() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDocumentMetadataReferenceDataList());
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(INVALID_DOCUMENT_TYPE);
        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.INVALID_DOCUMENT_TYPE, "documentType", INVALID_DOCUMENT_TYPE);

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsInReferenceData() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDocumentMetadataReferenceDataList());
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(VALID_DOCUMENT_TYPE);
        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldOverwriteDocumentTypeForApplication(){
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDocumentMetadataReferenceDataList());
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentForApplicationWithReferenceData(VALID_DOCUMENT_TYPE);

        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
        assertThat(caseDocumentWithReferenceData.getDocumentCategory(), is("Applications"));
        assertThat(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory(), is("Applications"));

        verify(referenceDataQueryService).retrieveDocumentsTypeAccess();

    }

    @Test
    public void shouldReturnNoProblemWhenCaseDocumentWithReferenceDataHasDocumentTypeAccessReferenceDataList() {
        final  CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(VALID_DOCUMENT_TYPE);
        caseDocumentWithReferenceData.setDocumentTypeAccessReferenceDataList(
                List.of(
                        documentTypeAccessReferenceData()
                                .withSection(VALID_DOCUMENT_TYPE)
                                .build()));


        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));

    }

    @Test
    public void shouldReturnNoProblemWhenDocumentTypeAccessReferenceDataIsNotEmpty() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(VALID_DOCUMENT_TYPE);
        caseDocumentWithReferenceData.setDocumentTypeAccessReferenceData(documentTypeAccessReferenceData().build());

        final ValidationResult validationResult = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService);


        assertThat(validationResult.problems().stream().findFirst().isPresent(), is(false));

        verify(referenceDataQueryService, never()).retrieveDocumentsTypeAccess();
    }


    private CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final String documentType) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), "Prosecutor defendant id",
                emptyList(), documentType, false, false);
    }

    private CaseDocumentWithReferenceData getCaseDocumentForApplicationWithReferenceData(final String documentType) {
        CaseDocumentWithReferenceData caseDocumentWithReferenceData =  new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), "Prosecutor defendant id",
                emptyList(), documentType, false, false);
        caseDocumentWithReferenceData.setHasApplication(true);
        caseDocumentWithReferenceData.setCourtApplicationSubject(CourtApplicationSubject.courtApplicationSubject().build());
        return caseDocumentWithReferenceData;
    }

    private List<DocumentTypeAccessReferenceData> getMockDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE_LOWER_CASE)
                .withDocumentCategory("Document category")
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }
}
