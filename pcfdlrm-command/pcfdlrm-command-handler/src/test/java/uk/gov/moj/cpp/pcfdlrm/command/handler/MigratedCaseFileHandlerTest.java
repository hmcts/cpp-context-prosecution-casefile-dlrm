package uk.gov.moj.cpp.pcfdlrm.command.handler;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.builder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.pcfdlrm.command.handler.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData.parentBundleSectionReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cps.pcfdlrm.command.handler.AcceptMigratedCase.acceptMigratedCase;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.aggregate.CaseProcessingArgs;
import uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BundleSections;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.pcfdlrm.command.handler.AcceptMigratedCase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.inject.Instance;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedCaseFileHandlerTest {

    private static final String PROSECUTION_CASE_RECEIVE_MIGRATED_CASE_FILE_COMMAND = "pcfdlrm.command.receive-migrated-case-file";
    private static final String PROSECUTION_ACCEPT_MIGRATED_CASE_COMMAND = "pcfdlrm.command.accept-migrated-case";

    private static final UUID CASE_ID_VALUE = fromString("8c74f505-d062-49bd-b2be-1f64e8da3233");
    private static final UUID OFFENCE_ID = fromString("5f66994c-c8f2-458d-9828-d2923308a0ad");
    private static final String OFFENCE_CODE = "FOO";
    private static final LocalDate OFFENCE_COMMITTED_DATE = now();
    private static final LocalDate ARREST_DATE = now().minusMonths(3);
    private static final LocalDate OFFENCE_CHARGE_DATE = now().minusMonths(4);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private MigratedCaseDetails migratedCaseDetails;

    @Mock
    private CaseDetails caseDetails;

    private final List<MigratedMaterial> materialList = generateMaterials();

    private final List<MigratedDefendant> defendantList = getDefendantList();

    @Mock
    Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private Instance<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers;

    @InjectMocks
    private MigratedCaseFileHandler migratedCaseFileHandler;

    @Mock
    MigratedCaseFileAggregate aggregate;

    @Captor
    private ArgumentCaptor<Map<String, ImmutablePair<String, String>>> mapArgumentCaptor;

    @Captor
    private ArgumentCaptor<CaseProcessingArgs> argsCaptor;


    @Test
    void shouldHandleReceiveMigratedCaseFileCommand() {
        assertThat(migratedCaseFileHandler, isHandler(COMMAND_HANDLER)
                .with(method("receiveMigratedCaseFile").thatHandles(PROSECUTION_CASE_RECEIVE_MIGRATED_CASE_FILE_COMMAND)));
    }

    @Test
    void shouldHandleAcceptMigratedCaseCommand() {
        assertThat(migratedCaseFileHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAcceptMigratedCase").thatHandles(PROSECUTION_ACCEPT_MIGRATED_CASE_COMMAND)));
    }

    @Test
    void shouldHandleReceiveMigratedCaseFileEvent() throws EventStreamException {

        ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withMaterials(materialList).withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(UUID.randomUUID()).build();


        final Envelope<ReceiveMigratedCaseFile> receiveMigratedCaseFileCommand =
                envelopeFrom(
                        metadataFor(PROSECUTION_CASE_RECEIVE_MIGRATED_CASE_FILE_COMMAND), receiveMigratedCaseFile);
        when(eventSource.getStreamById(CASE_ID_VALUE)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MigratedCaseFileAggregate.class)).thenReturn(aggregate);
        when(eventSource.getStreamById(CASE_ID_VALUE)).thenReturn(eventStream);
        when(migratedCaseDetails.getCaseDetails()).thenReturn(caseDetails);
        when(migratedCaseDetails.getDefendants()).thenReturn(defendantList);
        when(caseDetails.getCaseId()).thenReturn(CASE_ID_VALUE);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(migratedHearingRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());

        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getDocumentTypeAccessReferenceData());

        when(referenceDataQueryService.getAllParentBundleSection(any(Metadata.class))).thenReturn(getParentBundleSectionReferenceData());

        migratedCaseFileHandler.receiveMigratedCaseFile(receiveMigratedCaseFileCommand);

        verify(eventSource).getStreamById(CASE_ID_VALUE);
        verify(aggregateService).get(eventStream, MigratedCaseFileAggregate.class);

        verify(aggregate).receiveMigratedCaseFile(argsCaptor.capture());

        CaseProcessingArgs captured = argsCaptor.getValue();

        Map<String, ImmutablePair<String, String>> actualSections = captured.getSections();
        assertThat(actualSections, is(Map.of(
                "8", new ImmutablePair<>("WS", "Witness Statements"),
                "9", new ImmutablePair<>("EX", "Exhibits")
        )));

        assertThat(captured.getDocumentMetadataReferenceDataList(), is(getDocumentTypeAccessReferenceData()));


        verify(referenceDataQueryService).getAllParentBundleSection(any(Metadata.class));


    }


    @Test
    void shouldHandleAcceptMigratedCase() throws EventStreamException {

        final List<UUID> defendantIds = of(UUID.randomUUID());
        AcceptMigratedCase acceptMigratedCase = acceptMigratedCase().withCaseId(CASE_ID_VALUE).withDefendantIds(defendantIds).build();
        final Envelope<AcceptMigratedCase> acceptMigratedCaseCommand = envelopeFrom(metadataFor(PROSECUTION_ACCEPT_MIGRATED_CASE_COMMAND), acceptMigratedCase);
        when(eventSource.getStreamById(CASE_ID_VALUE)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MigratedCaseFileAggregate.class)).thenReturn(aggregate);
        when(aggregate.acceptMigratedCase()).thenReturn(builder().build());

        migratedCaseFileHandler.handleAcceptMigratedCase(acceptMigratedCaseCommand);

        verify(aggregateService).get(eventStream, MigratedCaseFileAggregate.class);
        verify(aggregate).acceptMigratedCase();

    }

    private List<MigratedMaterial> generateMaterials() {
        List<MigratedMaterial> list = new ArrayList<>();
        list.add(randomMaterial());
        list.add(randomMaterial());
        return list;
    }

    private List<MigratedDefendant> getDefendantList() {

        return Arrays.asList(buildDefendant("John", "Smith"), buildDefendant("Roger", "Wesley"));

    }

    private MigratedDefendant buildDefendant(final String firstName, final String lastName) {
        return MigratedDefendant.migratedDefendant()
                .withId(randomUUID())
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(null)
                                .build())
                        .build())
                .withOffences(singletonList(MigratedOffence.migratedOffence()
                        .withArrestDate(ARREST_DATE)
                        .withOffenceId(randomUUID())
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                        .withChargeDate(OFFENCE_CHARGE_DATE)
                        .withOffenceId(OFFENCE_ID)
                        .withOffenceSequenceNumber(1)
                        .build()))
                .withInitiationCode("C")
                .build();
    }


    private static MigratedMaterial randomMaterial() {
        return MigratedMaterial.migratedMaterial().withCaseId(CASE_ID_VALUE)
                .withDefendantId("defendantId")
                .withDocumentType(8)
                .withFileType("8")
                .build();

    }

    private List<DocumentTypeAccessReferenceData> getDocumentTypeAccessReferenceData() {

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData1 = documentTypeAccessReferenceData()
                .withActionRequired(false)
                .withDocumentCategory("Case level")
                .withId(fromString("460f8974-c002-11e8-a355-529269fb1459"))
                .withSection("Witness Statements")
                .withSectionCode("WS")
                .build();

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData2 = documentTypeAccessReferenceData()
                .withActionRequired(false)
                .withDocumentCategory("Defendant level")
                .withId(fromString("460f7ec0-c002-11e8-a355-529269fb1459"))
                .withSection("Charges")
                .withSectionCode("CH")
                .build();

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData3 = documentTypeAccessReferenceData()
                .withActionRequired(false)
                .withDocumentCategory("Case level")
                .withId(fromString("460f8154-c002-11e8-a355-529269fb1459"))
                .withSection("Case Summary")
                .withSectionCode("CS")
                .build();

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData4 = documentTypeAccessReferenceData()
                .withActionRequired(false)
                .withDocumentCategory("Case level")
                .withId(fromString("460f851e-c002-11e8-a355-529269fb1459"))
                .withSection("Key witness Statements")
                .withSectionCode("KWS")
                .build();

        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData5 = documentTypeAccessReferenceData()
                .withActionRequired(false)
                .withDocumentCategory("Case level")
                .withId(fromString("460f851e-c002-11e8-a355-529269fb1459"))
                .withSection("Exhibits")
                .withSectionCode("EX")
                .build();

        return of(documentTypeAccessReferenceData1, documentTypeAccessReferenceData2,
                documentTypeAccessReferenceData3, documentTypeAccessReferenceData4, documentTypeAccessReferenceData5);
    }

    private List<ParentBundleSectionReferenceData> getParentBundleSectionReferenceData() {
        List<ParentBundleSectionReferenceData> parentBundleSectionReferenceDataList = new ArrayList<>();

        ParentBundleSectionReferenceData parentBundleSectionReferenceData1 = parentBundleSectionReferenceData()
                .withId(UUID.fromString("21438016-ed0f-3efc-8126-c0a62e8cb623"))
                .withSeqNum(230)
                .withCpsBundleCode("0")
                .withParentBundleDescription("Other non-bundle Document")
                .withTargetSectionCode("ONBD")
                .withBundleAcceptanceFlag(false)
                .withBundleAcceptanceFlag(false)
                .withBundleSections(new ArrayList<>())
                .build();

        List<BundleSections> bundleSectionList = new ArrayList<>();
        bundleSectionList.add(BundleSections.bundleSections()
                .withSeqNum(80)
                .withBundleSectionCode("WS")
                .build());

        ParentBundleSectionReferenceData parentBundleSectionReferenceData2 = parentBundleSectionReferenceData()
                .withId(UUID.fromString("2e6d1ccd-1ff5-33ae-a7f5-abb4d240c044"))
                .withSeqNum(60)
                .withCpsBundleCode("8")
                .withParentBundleDescription("Witness Statements")
                .withTargetSectionCode("WS")
                .withBundleAcceptanceFlag(false)
                .withBundleAcceptanceFlag(true)
                .withBundleSections(bundleSectionList)
                .build();

        ParentBundleSectionReferenceData parentBundleSectionReferenceData3 = parentBundleSectionReferenceData()
                .withId(UUID.fromString("2e6d1ccd-1ff5-33ae-a7f5-abb4d240c044"))
                .withSeqNum(40)
                .withCpsBundleCode("6")
                .withParentBundleDescription("Other non-bundle Document")
                .withTargetSectionCode("UM").withBundleAcceptanceFlag(false)
                .withBundleAcceptanceFlag(false)
                .withBundleSections(new ArrayList<>())
                .build();

        ParentBundleSectionReferenceData parentBundleSectionReferenceData4 = parentBundleSectionReferenceData()
                .withId(UUID.fromString("2c87029f-1ef5-31cb-ba94-ff9c149b3532"))
                .withSeqNum(50)
                .withCpsBundleCode("9")
                .withParentBundleDescription("Exhibits")
                .withTargetSectionCode("EX").withBundleAcceptanceFlag(false)
                .withBundleAcceptanceFlag(false)
                .withBundleSections(new ArrayList<>())
                .build();

        parentBundleSectionReferenceDataList.add(parentBundleSectionReferenceData1);
        parentBundleSectionReferenceDataList.add(parentBundleSectionReferenceData2);
        parentBundleSectionReferenceDataList.add(parentBundleSectionReferenceData3);
        parentBundleSectionReferenceDataList.add(parentBundleSectionReferenceData4);

        return parentBundleSectionReferenceDataList;
    }
}