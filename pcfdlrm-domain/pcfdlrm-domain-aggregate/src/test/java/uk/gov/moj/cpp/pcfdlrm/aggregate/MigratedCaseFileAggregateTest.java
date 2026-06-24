package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate.COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANTS;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate.HEARING_VALIDATION;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate.NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildMigratedCaseDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildProsecution;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildReceiveMigratedCaseFile;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.CASE_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID2;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURTROOM_ID_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.E;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.W;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedCreationPending;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.DefendantValidationFailed;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MigratedCaseFileAggregateTest {

    public static final String XHIBIT = "XHIBIT";
    private static final String SHOULD_RAISE_MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION = "shouldRaiseMigratedCaseNotFoundInAutomation()";
    private static final String SHOULD_RAISE_MATERIAL_READY_FOR_COURT_DOCUMENT = "shouldRaiseMaterialReadyForCourtDocument()";
    private static final String SHOULD_RAISE_EVENT_ON_MATERIAL_ADDED_POST_PROCESSING = "shouldRaiseEventOnMaterialAddedPostProcessing()";
    public static final String INVALID_PLEA_ID = "INVALID_PLEA_ID";
    @InjectMocks
    private MigratedCaseFileAggregate migratedCaseFileAggregate;


    @Mock(answer = RETURNS_DEEP_STUBS)
    private CaseDetails caseDetails;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Prosecution prosecution;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DocumentTypeAccessReferenceData documentTypeAccessReferenceData;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private CourtDocumentTypeRBAC courtDocumentTypeRBAC;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MigratedHearing migratedHearing;

    @Mock
    private Prosecutor prosecutor;

    @Mock
    private CaseRefDataEnricher caseRefDataEnricher;
    @Mock
    private DefendantRefDataEnricher defendantRefDataEnricher;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private CourtDocument courtDocument;

    @Mock
    private MigratedHearingRefDataEnricher migratedHearingRefDataEnricher;


    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        if (testInfo.getDisplayName().equalsIgnoreCase(SHOULD_RAISE_MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION) || testInfo.getDisplayName().equalsIgnoreCase(SHOULD_RAISE_MATERIAL_READY_FOR_COURT_DOCUMENT) ||
                testInfo.getDisplayName().equalsIgnoreCase(SHOULD_RAISE_EVENT_ON_MATERIAL_ADDED_POST_PROCESSING)
        )
            return;

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(prosecution);
        when(documentTypeAccessReferenceData.getCourtDocumentTypeRBAC()).thenReturn(courtDocumentTypeRBAC);
        when(migratedHearingWithReferenceData.getMigratedHearing()).thenReturn(migratedHearing);
        when(caseDetails.getCaseId()).thenReturn(CASE_ID);
        Mockito.lenient().when(prosecutor.getProsecutingAuthority()).thenReturn("prosAut");
        Mockito.lenient().when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn("Either way case");
    }

    @Test
    void shouldRaiseMigratedCaseFileReceived() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", W.name(), W.name(), null, null, null);
        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        MigratedCaseFileAggregate migratedCaseFileAggregate = new MigratedCaseFileAggregate();

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher)));

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);
    }

    private static void assertsForMaterialsAdded(final MigratedCaseFileAggregate migratedCaseSubmissionAggregate, final int materialCount) {
        assertThat("Materials added size should match", migratedCaseSubmissionAggregate.getMaterialsAdded().size() == materialCount);
        assertThat("Materials data should match", "99".equals(migratedCaseSubmissionAggregate.getMaterialsAdded().get(0).getMaterial().getFileType()));
    }

    @Test
    void shouldRaiseEventOnMaterialAddedPostProcessing() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        MigratedCaseFileAggregate migratedCaseFileAggregate = new MigratedCaseFileAggregate();

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher), referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)));


        migratedCaseFileAggregate.materialAddedPostProcessing(courtDocument, UUID.randomUUID());
        assertThat("Should be null migratedCaseFile ", null == migratedCaseFileAggregate.getReceiveMigratedCaseFile());
        assertThat("Size of materialsAddedPostProcessing should be  1 ", migratedCaseFileAggregate.getMaterialsAddedPostProcessing().size() == 1);

        migratedCaseFileAggregate.materialAddedPostProcessing(courtDocument, UUID.randomUUID());
        assertThat("Size of materialsAddedPostProcessing should be  2 ", migratedCaseFileAggregate.getMaterialsAddedPostProcessing().size() == 2);

    }

    @Test
    void shouldRaiseMigratedCaseNotFoundInAutomation() {
        Mockito.reset(prosecutionWithReferenceData);
        migratedCaseFileAggregate.acceptMigratedCase();

        assertThat("Size of materialReadyForCourtDocument should be empty ", migratedCaseFileAggregate.getMaterailsReadyForCourtDocuments().isEmpty());

    }

    private List<DocumentTypeAccessReferenceData> getDocumentMetadataReferenceDataList() {
        return List.of(
                new DocumentTypeAccessReferenceData(false, null, "Case level",
                        UUID.randomUUID(), "Witness Statements", "WS", null, null, null),
                new DocumentTypeAccessReferenceData(false, null, "Defendant level",
                        UUID.randomUUID(), "Private section - Judges & HMCTS", "PSJH", null, null, null),
                new DocumentTypeAccessReferenceData(false, null, "Case level",
                        UUID.randomUUID(), "IDPC Bundle", "IDPC", null, null, null));
    }


    @Test
    void shouldRaiseMigratedCaseFileReceivedForDefendantLevel() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher), referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)));

        assertThat("Materials added size should match", migratedCaseFileAggregate.getMaterialsAdded().size() == 1);

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedNonPdfWithoutMaterial() {

        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "doc");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", " MALE", W.name(), W.name(), null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher), referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)));

        assertThat("Materials added size should match", migratedCaseFileAggregate.getMaterialsAdded().isEmpty());

    }

    @Test
    void shouldThrowNotYetImplementedWhenDocumentIsNotPdf() {

        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "doc");
        MigratedMaterial migratedMaterial = MigratedMaterial.migratedMaterial()
                .withValuesFrom(migratedMaterials.get(0))
                .withFileType("18")
                .build();

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "YYYY", W.name(), null, null, null, null);

        MigratedCaseDetails amendedMigCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemCaseIdentifier("LIBRA123")
                        .withMigrationSourceSystemName("LIBRA")
                        .build())
                .build();

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigCaseDetails, singletonList(migratedMaterial));

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        MigratedCaseFileAggregate.NotYetImplementedException exception = assertThrows(MigratedCaseFileAggregate.NotYetImplementedException.class, () ->
                migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                        receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        getDocumentMetadataReferenceDataList(),
                        List.of(migratedHearingRefDataEnricher)
                ))
        );
        assertEquals("File type matching cps bundle code is not found in map", exception.getMessage());
    }


    @Test
    void shouldThrowNotYetImplementedWhenMaterialValidationFails() {

        List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = List.of(
                new DocumentTypeAccessReferenceData(false, null, "Case level",
                        UUID.randomUUID(), "Witness Statements1", "WS1", null, null, null)
        );

        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        MigratedCaseFileAggregate.NotYetImplementedException exception = assertThrows(MigratedCaseFileAggregate.NotYetImplementedException.class, () ->
                migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                        receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        documentMetadataReferenceDataList,
                        List.of(migratedHearingRefDataEnricher)
                ))
        );
        assertEquals("Only happy path implemented now", exception.getMessage());
    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantProblemsHappened() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, null, null, null, null, null, null, null);
        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getDocumentationLanguage(), is(E.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getHearingLanguage(), is(E.name()));
        assertNull(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getEthnicity());
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getGender(), is(NOT_KNOWN.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getParentGuardianInformation().getGender(), is(NOT_KNOWN.name()));
        assertNull(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getPersonalInformation().getObservedEthnicity());

    }

    @Test
    void shouldNotRaiseMigratedCaseFileReceivedWhenDefendantProblemsHappenedForBadOffenceCode() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, null, null, null, null, "BadOffenceCode", null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);


        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();


        assertThat(eventStream.size(), is(2));

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenGuiltyPleaHasPleaCodeAndPleaDate() {
        List<MigratedMaterial> migratedMaterials = List.of(createMigratedMaterials(2, "pdf").get(0));

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails,
                null, null, null, null, "998A", "G", LocalDate.now());

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final List<MigratedDefendant> defendants = amendedprosecution.getDefendants();
        final MigratedOffence migratedOffence = defendants.get(0).getOffences().get(0);
        final UUID offenceId = migratedOffence.getOffenceId();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceId,
                PleaReferenceData.pleaReferenceData()
                        .withPleaValue("Guilty")
                        .withPleaTypeCode("G")
                        .withPleaTypeGuiltyFlag("Yes")
                        .build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        getDocumentMetadataReferenceDataList(),
                        List.of(migratedHearingRefDataEnricher)))
                .toList();

        assertThat(eventStream.size(), is(3));

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenNotGuiltyPleaHasPleaCodeAndPleaDate() {

        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails,
                null, null, null, null, "998A", "NG", LocalDate.now());

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final List<MigratedDefendant> defendants = amendedprosecution.getDefendants();
        final MigratedOffence migratedOffence = defendants.get(0).getOffences().get(0);
        final UUID offenceId = migratedOffence.getOffenceId();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceId,
                PleaReferenceData.pleaReferenceData()
                        .withPleaValue("Not Guilty")
                        .withPleaTypeCode("NG")
                        .withPleaTypeGuiltyFlag("No")
                        .build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        getDocumentMetadataReferenceDataList(),
                        List.of(migratedHearingRefDataEnricher)))
                .toList();

        assertThat(eventStream.size(), is(3));

    }

    @Test
    void shouldNotRaiseMigratedCaseFileReceivedWhenGuiltyPleaHasMissingPleaDate() {

        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails,
                null, null, null, null, "998A", "G", null);

        prepPleaDateTests(migCaseDetails, migratedMaterials);

    }

    private void prepPleaDateTests(MigratedCaseDetails migCaseDetails, List<MigratedMaterial> migratedMaterials) {
        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final List<MigratedDefendant> defendants = amendedprosecution.getDefendants();
        final MigratedOffence migratedOffence = defendants.get(0).getOffences().get(0);
        final UUID offenceId = migratedOffence.getOffenceId();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceId,
                PleaReferenceData.pleaReferenceData()
                        .withPleaTypeCode("G")
                        .withPleaTypeGuiltyFlag("Yes")
                        .withPleaValue("Guilty")
                        .build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        getDocumentMetadataReferenceDataList(),
                        List.of(migratedHearingRefDataEnricher)))
                .toList();

        assertThat(eventStream.size(), is(2));
    }

    @Test
    void shouldNotRaiseMigratedCaseFileReceivedWhenGuiltyPleaHasFuturePleaDate() {

        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails,
                null, null, null, null, "998A", "G", LocalDate.now().plusDays(1));

        prepPleaDateTests(migCaseDetails, migratedMaterials);

    }

    @Test
    void shouldNotRaiseMigratedCaseFileReceivedWhenNotGuiltyPleaHasMissingPleaDate() {

        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails,
                null, null, null, null, "998A", "NG", null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final List<MigratedDefendant> defendants = amendedprosecution.getDefendants();
        final MigratedOffence migratedOffence = defendants.get(0).getOffences().get(0);
        final UUID offenceId = migratedOffence.getOffenceId();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceId,
                PleaReferenceData.pleaReferenceData()
                        .withPleaTypeCode("NG")
                        .withPleaTypeGuiltyFlag("No")
                        .withPleaValue("Not Guilty")
                        .build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                        prosecutionWithReferenceData,
                        List.of(caseRefDataEnricher),
                        List.of(defendantRefDataEnricher),
                        referenceDataQueryService,
                        getSections(),
                        getDocumentMetadataReferenceDataList(),
                        List.of(migratedHearingRefDataEnricher)))
                .toList();

        assertThat(eventStream.size(), is(3));

    }

    @Test
    void shouldNotRaiseMigratedCaseFileReceivedWhenDefendantProblemsHappenedForBadPleaCode() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, null, null, null, null, "998A", "badPlea", LocalDate.now());

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();


        assertThat(eventStream.size(), is(4));
        MigratedCaseValidatedWithWarnings migratedCaseValidatedWithWarnings = (MigratedCaseValidatedWithWarnings) eventStream.get(1);
        assertThat(migratedCaseValidatedWithWarnings.getType(), is("Offence validation"));
        assertThat(migratedCaseValidatedWithWarnings.getMessage(), containsStringIgnoringCase(INVALID_PLEA_ID));

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantAndParenGuardianGenderProvided() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, MALE.name(), FEMALE.name(), W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getDocumentationLanguage(), is(W.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getHearingLanguage(), is(E.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getGender(), is(MALE.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getParentGuardianInformation().getGender(), is(FEMALE.name()));

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantAndParenGuardianGenderProvidedNotMatchInCP() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getDocumentationLanguage(), is(E.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getHearingLanguage(), is(W.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getGender(), is(NOT_KNOWN.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getParentGuardianInformation().getGender(), is(NOT_KNOWN.name()));

    }


    @Test
    void shouldRaiseSendingCourtProblemWhenInvalidCodeFound() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSendingCourt()).thenReturn("AB00001");

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(1));
        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is("Either Sending or Receiving Court not found"));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @Test
    void shouldRaiseReceivingCourtProblemWhenInvalidCodeFound() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn("AB00001");

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(1));
        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is("Either Sending or Receiving Court not found"));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @Test
    void shouldNotRaiseProblemWhenSendingAndReceivingCourtIsValid() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", E.name(), W.name(), null, null, null);
        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSendingCourt()).thenReturn("AB00001");
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn("AB00001");
        when(prosecutionWithReferenceData.getReferenceDataVO().getSendingCourtOrganisationUnit())
                .thenReturn(Optional.of(OrganisationUnitReferenceData.organisationUnitReferenceData().build()));
        when(prosecutionWithReferenceData.getReferenceDataVO().getReceivingCourtOrganisationUnit())
                .thenReturn(Optional.of(OrganisationUnitReferenceData.organisationUnitReferenceData().build()));

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRaiseProblemWhenReceiptTypesIsNullOrEmpty(String receiptType) {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "FEMALE", "FEMALE", W.name(), W.name(), null, null, null);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn(receiptType);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(1));
        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is("Invalid receipt types"));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @Test
    void shouldRaiseProblemWhenReceiptTypeIsUnrecognised() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "FEMALE", "FEMALE", W.name(), W.name(), null, null, null);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn("Bring back");

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertThat(eventStream.size(), is(1));
        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is("Invalid receipt types"));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @Test
    void shouldFailFastWhenHearingHasNoMatchingDefendants() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", W.name(), W.name(), null, null, null);
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withListedDefendants(List.of())
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(prosecution, migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedProsecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher)
        )).toList();

        assertThat(eventStream.size(), is(1));

        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is(NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @Test
    void shouldFailFastWhenHearingDefendantMatchesButNoOffencesMatch() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", W.name(), W.name(), null, null, null);

        MigratedDefendant defendantWithNonMatchingOffences = migratedDefendant()
                .withId(DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of(
                        migratedOffence()
                                .withOffenceId(UUID.randomUUID())
                                .withProsecutorOffenceId("OFF-001")
                                .withOffenceSequenceNumber(1)
                                .build()))
                .build();

        MigratedDefendant secondDefendantWithNonMatchingOffences = migratedDefendant()
                .withId(DEFENDANT_ID2)
                .withProsecutorDefendantId("DEF-002")
                .withOffences(List.of(
                        migratedOffence()
                                .withOffenceId(UUID.randomUUID())
                                .withProsecutorOffenceId("OFF-002")
                                .withOffenceSequenceNumber(1)
                                .build()))
                .build();

        final MigratedCaseDetails migCaseDetailsWithMismatch = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendantWithNonMatchingOffences, secondDefendantWithNonMatchingOffences))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of("OFF-999"))
                                                .build(),
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-002")
                                                .withListedOffences(List.of("OFF-888", "OFF-001"))
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(prosecution, migCaseDetailsWithMismatch);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithMismatch, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedProsecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher)
        )).toList();

        assertThat(eventStream.size(), is(1));

        MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is(NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }


    @ParameterizedTest
    @CsvSource({"Either way case", "Transfer", "Voluntary bill", "Indictable"})
    void shouldNotRaiseProblemWhenReceiptTypesIsValid(String receiptType) {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "XXX", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn(receiptType);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
    }

    @Test
    void shouldNotRaiseProblemWhenCaseMarkerIsInvalid() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(List.of(CaseMarker.caseMarker()
                .withMarkerTypeId(UUID.randomUUID())
                .withMarkerTypeCode("ABC001")
                .withMarkerTypeDescription("Test Code")
                .build()));

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertThat(eventStream.size(), is(4));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(3);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));

        MigratedCaseValidatedWithWarnings migratedCaseValidatedWithWarnings = (MigratedCaseValidatedWithWarnings) eventStream.get(1);

        assertNotNull(migratedCaseValidatedWithWarnings);

        assertThat(migratedCaseValidatedWithWarnings.getType(), is("Case validation"));
    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantAndParenGuardianGenderProvidedButNotInCP() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "XXX", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getDocumentationLanguage(), is(W.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getHearingLanguage(), is(E.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getGender(), is(NOT_KNOWN.name()));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getParentGuardianInformation().getGender(), is(NOT_KNOWN.name()));

    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotRaiseProblemWhenCaseMarkerIsNullOrEmpty(String markerTypeCode) {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "XXX", "YYYY", W.name(), null, null, null, null);

        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(List.of(CaseMarker.caseMarker()
                .withMarkerTypeId(UUID.randomUUID())
                .withMarkerTypeCode(markerTypeCode)
                .withMarkerTypeDescription("Test Code")
                .build()));

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();
        ;

        assertThat(eventStream.size(), is(4));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(3);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));

        MigratedCaseValidatedWithWarnings migratedCaseValidatedWithWarnings = (MigratedCaseValidatedWithWarnings) eventStream.get(1);

        assertNotNull(migratedCaseValidatedWithWarnings);

        assertThat(migratedCaseValidatedWithWarnings.getType(), is("Case validation"));
    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantAndParenGuardianIsNull() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "XXX", "YYYY", W.name(), null, null, null, null);

        MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(singletonList(MigratedDefendant.migratedDefendant()
                        .withValuesFrom(migCaseDetails.getDefendants().get(0))
                        .withIndividual(Individual.individual()
                                .withValuesFrom(migCaseDetails.getDefendants().get(0).getIndividual())
                                .withParentGuardianInformation(null)
                                .build())
                        .build()))
                .build();

        final Prosecution amendedprosecution = buildProsecution(prosecution, amendedMigratedDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getSelfDefinedInformation().getGender(), is(NOT_KNOWN.name()));
        assertNull(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getParentGuardianInformation());

    }

    @Test
    void shouldRaiseMigratedCaseFileReceivedWhenDefendantCustodyStatusIsCandCTLIsNull() {
        List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", W.name(), null, null, null, null);

        MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(singletonList(MigratedDefendant.migratedDefendant()
                        .withValuesFrom(migCaseDetails.getDefendants().get(0))
                        .withIndividual(Individual.individual()
                                .withValuesFrom(migCaseDetails.getDefendants().get(0).getIndividual())
                                .withCustodyStatus("C")
                                .withParentGuardianInformation(null)
                                .build())
                        .build()))
                .build();

        final Prosecution amendedprosecution = buildProsecution(prosecution, amendedMigratedDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase,
                prosecutionWithReferenceData,
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertsForMaterialsAdded(migratedCaseFileAggregate, 1);

        assertThat(eventStream.size(), is(3));

        DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventStream.get(0);
        MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending = (MigratedCaseValidatedCreationPending) eventStream.get(2);
        List<Problem> problems = defendantValidationFailed.getProblems();

        assertNotNull(migratedCaseValidatedCreationPending);

        ReceiveMigratedCaseFile resultReceiveMigratedCaseFile = migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile();
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(defendantValidationFailed.getDefendant().getIndividual().getCustodyStatus(), is("C"));
        assertThat(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getHearingLanguage(), is(E.name()));
        assertNull(resultReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getIndividual().getCustodyTimeLimit());
        assertTrue(problems.stream().anyMatch(problem -> problem.getCode().equals(DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING.name())));

    }

    @ParameterizedTest
    @MethodSource("provideMigratedMaterials")
    void shouldRaiseMigratedCaseFileReceivedWhenNoMaterialsPresent(List<MigratedMaterial> migratedMaterials) {

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "MALE", E.name(), W.name(), null, null, null);
        final Prosecution amendedprosecution = buildProsecution(prosecution, migCaseDetails);

        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, null);

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedprosecution);

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(), getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)));

        assertThat("Materials added size should match", migratedCaseFileAggregate.getMaterialsAdded().isEmpty());
        assertNull(migratedCaseFileAggregate.getMigratedCaseValidatedCreationPending());
        assertNotNull(migratedCaseFileAggregate.getReceiveMigratedCaseFile());

    }

    private static Stream<List<MigratedMaterial>> provideMigratedMaterials() {
        return Stream.of(
                List.of(),  // First value: an empty list
                null        // Second value: null
        );
    }

    private List<MigratedMaterial> createMigratedMaterials(final int fileCount, final String fileType) {
        List<MigratedMaterial> migratedMaterials = new ArrayList<>();
        MigratedMaterial migratedMaterial1 = MigratedMaterial.migratedMaterial()
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID.toString())
                .withAzureLocation("azure/abc.pdf")
                .withDocumentType(3)
                .withFileName("abc." + fileType)
                .withFileType("99").build();
        MigratedMaterial migratedMaterial2 = MigratedMaterial.migratedMaterial()
                .withDefendantId(DEFENDANT_ID2.toString())
                .withCaseId(CASE_ID)
                .withAzureLocation("azure/def.pdf")
                .withDocumentType(3)
                .withFileName("def." + fileType)
                .withFileType("99").build();

        if (fileCount != 1) {
            migratedMaterials.add(migratedMaterial2);
        }

        migratedMaterials.add(migratedMaterial1);

        return migratedMaterials;
    }

    @Test
    void shouldFailFastWhenNumberOfFilesExceedsNumberOfDefendants() {
        final MigratedMaterial material1 = MigratedMaterial.migratedMaterial()
                .withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString())
                .withAzureLocation("azure/abc.pdf").withDocumentType(3)
                .withFileName("abc.pdf").withFileType("99").build();
        final MigratedMaterial material2 = MigratedMaterial.migratedMaterial()
                .withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID2.toString())
                .withAzureLocation("azure/def.pdf").withDocumentType(3)
                .withFileName("def.pdf").withFileType("99").build();
        final MigratedMaterial material3 = MigratedMaterial.migratedMaterial()
                .withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString())
                .withAzureLocation("azure/ghi.pdf").withDocumentType(3)
                .withFileName("ghi.pdf").withFileType("99").build();

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "FEMALE", "FEMALE", W.name(), W.name(), null, null, null);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of(material1, material2, material3));

        final List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        assertThat(eventStream.size(), is(1));
        final MigratedCaseFileProcessed migratedCaseFileProcessed = (MigratedCaseFileProcessed) eventStream.get(0);
        assertNotNull(migratedCaseFileProcessed);
        assertThat(migratedCaseFileProcessed.getDescription(), is(COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANTS));
        assertThat(migratedCaseFileProcessed.getProcessingIsSuccessful(), is(false));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void shouldNotRaiseCourtRoomIdWarningWhenHearingCourtRoomIdIsNotPositive(final Integer courtRoomId) {
        final MigratedCaseDetails migCaseDetailsWithHearing = buildCaseDetailsWithHearing(courtRoomId);
        final Prosecution amendedProsecution = buildProsecution(prosecution, migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedProsecution);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms("C50EX00"))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(1).build()))
                        .build()));

        final List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final boolean hasCourtRoomIdWarning = eventStream.stream()
                .filter(e -> e instanceof MigratedCaseValidatedWithWarnings)
                .map(e -> (MigratedCaseValidatedWithWarnings) e)
                .filter(e -> HEARING_VALIDATION.equals(e.getType()))
                .anyMatch(e -> e.getMessage().contains(COURTROOM_ID_INVALID.name()));

        assertThat(hasCourtRoomIdWarning, is(false));
    }

    @Test
    void shouldRaiseHearingWarningWhenCourtRoomIdIsPositiveAndDoesNotMatchCourtroom() {
        final MigratedCaseDetails migCaseDetailsWithHearing = buildCaseDetailsWithHearing(999);
        final Prosecution amendedProsecution = buildProsecution(prosecution, migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));

        when(prosecutionWithReferenceData.getProsecution()).thenReturn(amendedProsecution);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms("C50EX00"))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(1).build()))
                        .build()));

        final List<Object> eventStream = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final boolean hasCourtRoomIdWarning = eventStream.stream()
                .filter(e -> e instanceof MigratedCaseValidatedWithWarnings)
                .map(e -> (MigratedCaseValidatedWithWarnings) e)
                .filter(e -> HEARING_VALIDATION.equals(e.getType()))
                .anyMatch(e -> e.getMessage().contains(COURTROOM_ID_INVALID.name()));

        assertThat(hasCourtRoomIdWarning, is(true));
    }

    private MigratedCaseDetails buildCaseDetailsWithHearing(final Integer courtRoomId) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(caseDetails, "MALE", "FEMALE", W.name(), W.name(), null, null, null);

        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();

        return MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withCourtRoomId(courtRoomId)
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();
    }

    private static Map<String, ImmutablePair<String, String>> getSections() {
        return Map.of(
                "1", ImmutablePair.of("IDPC", "IDPC Bundle"),
                "2", ImmutablePair.of("MCEB", "Magistrates' court evidence bundle"),
                "3", ImmutablePair.of("WS", "Witness Statements"),
                "6", ImmutablePair.of("UM", "Unused material"),
                "8", ImmutablePair.of("WS", "Witness Statements"),
                "9", ImmutablePair.of("EX", "Exhibits"),
                "99", ImmutablePair.of("PSJH", "Private section - Judges & HMCTS")
        );

    }
}