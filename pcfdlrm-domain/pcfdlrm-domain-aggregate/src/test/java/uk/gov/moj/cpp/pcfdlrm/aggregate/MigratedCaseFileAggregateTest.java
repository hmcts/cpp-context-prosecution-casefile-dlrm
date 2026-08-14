package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate.HEARING_VALIDATION;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildMigratedCaseDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildProsecution;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildReceiveMigratedCaseFile;
import static uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem.sourceSystem;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.CASE_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID2;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT_IDENDIFIER;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SUBMISSION_ID;
import static uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader.fixture;
import static uk.gov.moj.cpp.pcfdlrm.test.WholePayloadMatcher.matchesWholePayload;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURTROOM_ID_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.E;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.W;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedCreationPending;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedWeekCommencingDate;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.DefendantValidationFailed;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAddedPendingProcess;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseNotFoundInAutomation;

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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedCaseFileAggregateTest {

    public static final String XHIBIT = "XHIBIT";

    private static final String SHOULD_RAISE_MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION = "shouldRaiseMigratedCaseNotFoundInAutomation()";

    private static final String SHOULD_RAISE_EVENT_ON_MATERIAL_ADDED_POST_PROCESSING = "shouldRaiseEventOnMaterialAddedPostProcessing()";

    private static final String FUTURE_HEARING_DATE_GMT = nextFutureDate(1, 15);

    private static final String FUTURE_HEARING_DATE_BST = nextFutureDate(6, 15);

    private static final String FUTURE_WEEK_COMMENCING_START_DATE = nextFutureDate(6, 14);

    private static final List<String> FUTURE_DATE_OF_HEARING_EXCLUSIONS = List.of(
            "receiveMigratedCaseFile.migratedCaseDetails.hearings[0].dateOfHearing",
            "migratedHearingWithReferenceDataList[0].migratedHearing.dateOfHearing");

    private static final List<String> FUTURE_WEEK_COMMENCING_START_DATE_EXCLUSIONS = List.of(
            "receiveMigratedCaseFile.migratedCaseDetails.hearings[0].weekCommencingDate.startDate",
            "migratedHearingWithReferenceDataList[0].migratedHearing.weekCommencingDate.startDate");

    private static final List<ExpectedEvent> HEARING_DEFENDANT_VALIDATION_NOISE = List.of(
            new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-hearing-defendant.json"),
            warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
            warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
            warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"));

    /**
     * {@code addMaterial}'s {@code receivedDateTime} is stamped from {@code ZonedDateTime.now()} in
     * {@code src/main} — out of scope to change here — so it is the one field every
     * {@code MaterialAdded} fixture must exclude; presence is still checked, only the value is
     * skipped (see {@code WholePayloadMatcher}).
     */
    private static final List<String> MATERIAL_ADDED_EXCLUSIONS = List.of("receivedDateTime");

    /** Fixed, not {@code LocalDate.now()} — a plea date baked into a static whole-payload fixture is non-deterministic otherwise. */
    private static final LocalDate PLEA_DATE_ANCHOR = LocalDate.of(2024, 1, 15);

    /**
     * For {@code guiltyPleaFutureDateInput()} only — that scenario needs a plea date genuinely in
     * the future relative to the real clock (see its comment), so its two date occurrences can't
     * be pinned to a fixed value and are excluded from the whole-payload comparison instead.
     */
    private static final List<String> PLEA_FUTURE_DATE_EXCLUSIONS = List.of("defendant.offences[0].plea.pleaDate", "problems[2].values[0].value");

    @InjectMocks
    private MigratedCaseFileAggregate migratedCaseFileAggregate;

    private ProsecutionWithReferenceData prosecutionWithReferenceData;

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
        if (testInfo.getDisplayName().equalsIgnoreCase(SHOULD_RAISE_MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION) ||
                testInfo.getDisplayName().equalsIgnoreCase(SHOULD_RAISE_EVENT_ON_MATERIAL_ADDED_POST_PROCESSING)
        )
            return;

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(
                Prosecution.prosecution()
                        .withCaseDetails(CaseDetails.caseDetails().withReceiptType("Either way case").build())
                        .build());
    }

    @Test
    void shouldRaiseEventOnMaterialAddedPostProcessing() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));

        final Prosecution amendedprosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails().build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedprosecution);

        migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher), referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)));

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

        final List<Object> firstCall = migratedCaseFileAggregate.materialAddedPostProcessing(courtDocument,
                UUID.fromString("c1c1c1c1-1111-4111-8111-111111111111")).toList();
        assertThat(firstCall, hasSize(1));
        assertThat(firstCall.get(0), instanceOf(MaterialAddedPendingProcess.class));
        assertThat(objectToJsonObjectConverter.convert(firstCall.get(0)).toString(),
                matchesWholePayload(fixture("json/aggregate/material-added-pending-process-call1.json"), List.of()));

        final List<Object> secondCall = migratedCaseFileAggregate.materialAddedPostProcessing(courtDocument,
                UUID.fromString("c2c2c2c2-2222-4222-8222-222222222222")).toList();
        assertThat(secondCall, hasSize(1));
        assertThat(secondCall.get(0), instanceOf(MaterialAddedPendingProcess.class));
        assertThat(objectToJsonObjectConverter.convert(secondCall.get(0)).toString(),
                matchesWholePayload(fixture("json/aggregate/material-added-pending-process-call2.json"), List.of()));
    }

    @Test
    void shouldRaiseMigratedCaseNotFoundInAutomation() {
        final List<Object> eventStream = migratedCaseFileAggregate.acceptMigratedCase().toList();

        assertThat(eventStream, hasSize(1));
        assertThat(eventStream.get(0), instanceOf(MigratedCaseNotFoundInAutomation.class));

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        assertThat(objectToJsonObjectConverter.convert(eventStream.get(0)).toString(),
                matchesWholePayload(fixture("json/aggregate/migrated-case-not-found-in-automation.json"), List.of()));
    }

    @Test
    void shouldThrowNotYetImplementedWhenDocumentIsNotPdf() {

        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "doc");
        MigratedMaterial migratedMaterial = MigratedMaterial.migratedMaterial()
                .withValuesFrom(migratedMaterials.get(0))
                .withFileType("18")
                .build();

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));

        MigratedCaseDetails amendedMigCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemCaseIdentifier("LIBRA123")
                        .withMigrationSourceSystemName("LIBRA")
                        .build())
                .build();

        final Prosecution amendedprosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigCaseDetails, singletonList(migratedMaterial));

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedprosecution);

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
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));

        final Prosecution amendedprosecution = buildProsecution(migCaseDetails);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedprosecution);

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

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void shouldNotRaiseCourtRoomIdWarningWhenHearingCourtRoomIdIsNotPositive(final Integer courtRoomId) {
        final MigratedCaseDetails migCaseDetailsWithHearing = buildCaseDetailsWithHearing(courtRoomId);
        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
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
        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
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

    @ParameterizedTest(name = "{2}")
    @MethodSource("fixedHearingTimeDefaultingScenarios")
    void shouldDefaultHearingTimeTo10AmOnlyForFixedHearingWithNoWarnings(
            final String dateOfHearing,
            final String timeOfHearing,
            final String scenarioName,
            final List<ExpectedEvent> expected) {

        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withCourtRoomId(235)
                                .withHearingType("SIT")
                                .withDateOfHearing(dateOfHearing)
                                .withTimeOfHearing(timeOfHearing)
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, null);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms("C50EX00"))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(235).build()))
                        .build()));
        when(referenceDataQueryService.retrieveHearingTypes())
                .thenReturn(HearingTypes.hearingTypes()
                        .withHearingtypes(List.of(HearingType.hearingType().withHearingCode("SIT").build()))
                        .build());

        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        assertEventsMatchExpected(actual, expected);
    }

    @Test
    void shouldDefaultHearingTimeTo10amForUnallocatedHearingWhenTimeNotProvided() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withHearingType("SIT")
                                .withDateOfHearing(FUTURE_HEARING_DATE_GMT)
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, null);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms("C50EX00"))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of())
                        .build()));
        when(referenceDataQueryService.retrieveHearingTypes())
                .thenReturn(HearingTypes.hearingTypes()
                        .withHearingtypes(List.of(HearingType.hearingType().withHearingCode("SIT").build()))
                        .build());

        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final List<ExpectedEvent> expected = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        expected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-unallocated.json", Map.of("timeOfHearing", "10:00:00"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));
        assertEventsMatchExpected(actual, expected);
    }

    @Test
    void shouldNotOverwriteHearingTimeForUnallocatedHearingWhenTimeProvided() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withHearingType("SIT")
                                .withDateOfHearing(FUTURE_HEARING_DATE_GMT)
                                .withTimeOfHearing("09:30")
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, null);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms("C50EX00"))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of())
                        .build()));
        when(referenceDataQueryService.retrieveHearingTypes())
                .thenReturn(HearingTypes.hearingTypes()
                        .withHearingtypes(List.of(HearingType.hearingType().withHearingCode("SIT").build()))
                        .build());

        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final List<ExpectedEvent> expected = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        expected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-unallocated.json", Map.of("timeOfHearing", "09:30"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));
        assertEventsMatchExpected(actual, expected);
    }

    @Test
    void shouldNotDefaultHearingTimeForWeekCommencingHearing() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withHearingType("SIT")
                                .withWeekCommencingDate(MigratedWeekCommencingDate.migratedWeekCommencingDate()
                                        .withStartDate(FUTURE_WEEK_COMMENCING_START_DATE)
                                        .withDuration(3)
                                        .build())
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, null);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
        when(referenceDataQueryService.retrieveHearingTypes())
                .thenReturn(HearingTypes.hearingTypes()
                        .withHearingtypes(List.of(HearingType.hearingType().withHearingCode("SIT").build()))
                        .build());

        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final List<ExpectedEvent> expected = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        expected.add(warning("Hearing validation", "COURT_HEARING_LOCATION_OUCODE_INVALID : [C50EX00]"));
        expected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-week-commencing.json", FUTURE_WEEK_COMMENCING_START_DATE_EXCLUSIONS));
        assertEventsMatchExpected(actual, expected);
    }

    @Test
    void shouldNotDefaultHearingTimeForUnscheduledHearing() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendant = migratedDefendant()
                .withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withProsecutorDefendantId("DEF-001")
                .withOffences(List.of())
                .build();
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendant))
                .withHearings(List.of(
                        MigratedHearing.migratedHearing()
                                .withCourtHearingLocation("C50EX00")
                                .withHearingType("SIT")
                                .withListedDefendants(List.of(
                                        ListedDefendant.listedDefendant()
                                                .withProsecutorDefendantId("DEF-001")
                                                .withListedOffences(List.of())
                                                .build()))
                                .build()))
                .build();

        final Prosecution amendedProsecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, null);

        prosecutionWithReferenceData = new ProsecutionWithReferenceData(amendedProsecution);
        when(referenceDataQueryService.retrieveHearingTypes())
                .thenReturn(HearingTypes.hearingTypes()
                        .withHearingtypes(List.of(HearingType.hearingType().withHearingCode("SIT").build()))
                        .build());

        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                receiveMigratedCase, prosecutionWithReferenceData,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher)
        )).toList();

        final List<ExpectedEvent> expected = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        expected.add(warning("Hearing validation", "COURT_HEARING_LOCATION_OUCODE_INVALID : [C50EX00]"));
        expected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-unscheduled.json"));
        assertEventsMatchExpected(actual, expected);
    }

    @ParameterizedTest
    @MethodSource("aggregateScenarios")
    void shouldEmitExpectedEventsForScenario(final AggregateScenario scenario) {
        final List<Object> actual = migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                scenario.input().receiveMigratedCaseFile(),
                scenario.input().prosecutionWithReferenceData(),
                List.of(caseRefDataEnricher),
                List.of(defendantRefDataEnricher),
                referenceDataQueryService,
                getSections(),
                getDocumentMetadataReferenceDataList(),
                List.of(migratedHearingRefDataEnricher))).toList();

        assertEventsMatchExpected(actual, scenario.expected());
    }

    private static Stream<Arguments> fixedHearingTimeDefaultingScenarios() {
        final List<ExpectedEvent> pastNoWarning = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        pastNoWarning.add(warning("Hearing validation", "DATE_OF_HEARING_IN_THE_PAST : [2026-03-05]"));
        pastNoWarning.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-past-no-warning.json"));

        final List<ExpectedEvent> futureWithTime = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureWithTime.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "09:30"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        final List<ExpectedEvent> futureNoTimeGmt = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureNoTimeGmt.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "10:00:00"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        final List<ExpectedEvent> futureNoTimeBst = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureNoTimeBst.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "09:00:00"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        return Stream.of(
                Arguments.of("2026-03-05", null, "past dateOfHearing raises warning — should not default timeOfHearing", pastNoWarning),
                Arguments.of(FUTURE_HEARING_DATE_GMT, "09:30", "future dateOfHearing with existing time — should not overwrite timeOfHearing", futureWithTime),
                Arguments.of(FUTURE_HEARING_DATE_GMT, null, "GMT: future dateOfHearing with no time — should default to 10:00:00 UTC", futureNoTimeGmt),
                Arguments.of(FUTURE_HEARING_DATE_BST, null, "BST: future dateOfHearing with no time — should default to 09:00:00 UTC", futureNoTimeBst)
        );
    }

    private static Stream<AggregateScenario> aggregateScenarios() {
        return Stream.of(xhibitGateScenarios(), failFastScenarios(), hasOffenceProblemsGateScenarios(), materialsMainPathScenarios(), defendantProblemsScenarios(), pleaScenarios(), genderCourtMarkerScenarios())
                .flatMap(s -> s);
    }

    private static Stream<AggregateScenario> xhibitGateScenarios() {
        final List<ExpectedEvent> defendantValidationNoise = List.of(
                new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-no-materials.json"),
                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"));

        final List<ExpectedEvent> xhibitExpected = new ArrayList<>(defendantValidationNoise);
        xhibitExpected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-no-materials.json"));

        final List<ExpectedEvent> xhibitExpectedNullMaterials = new ArrayList<>(defendantValidationNoise);
        xhibitExpectedNullMaterials.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-no-materials-null.json"));

        return Stream.of(
                new AggregateScenario("isXhibit() true for XHIBIT — MigratedCaseFileReceived reaches the stream",
                        noMaterialsInput(sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER)), xhibitExpected),
                new AggregateScenario("isXhibit() false for LIBRA — MigratedCaseFileReceived is withheld",
                        noMaterialsInput(sourceSystem("LIBRA", "LIBRA-123")), defendantValidationNoise),
                new AggregateScenario("No materials present — materials list is null rather than empty, same isXhibit() outcome",
                        nullMaterialsInput(), xhibitExpectedNullMaterials)
        );
    }

    /**
     * The nine cheapest, single-event fail-fast paths (03-stories.md PR3 background) — eight
     * converted from scenarios that already existed, one ("Invalid Prosecuting Authority",
     * {@code hasInvalidProsecutingAuthority()}) new: it did not exist before this story
     * (02-design.md, Coverage).
     */
    private static Stream<AggregateScenario> failFastScenarios() {
        return Stream.of(
                new AggregateScenario("Court record sheet count exceeds defendant count",
                        courtRecordSheetCountExceedsInput(),
                        List.of(processedFailure("Number of Court Record Sheets exceeds number of defendants"))),
                new AggregateScenario("Sending court code invalid — Either Sending or Receiving Court not found",
                        courtCodeInvalidInput(CaseDetails.caseDetails().withReceiptType("Either way case").withSendingCourt("AB00001").build()),
                        List.of(processedFailure("Either Sending or Receiving Court not found"))),
                new AggregateScenario("Receiving court code invalid — Either Sending or Receiving Court not found",
                        courtCodeInvalidInput(CaseDetails.caseDetails().withReceiptType("Either way case").withReceivingCourt("AB00001").build()),
                        List.of(processedFailure("Either Sending or Receiving Court not found"))),
                new AggregateScenario("Receipt type null — Invalid receipt types",
                        receiptTypeInput(null),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Receipt type empty — Invalid receipt types",
                        receiptTypeInput(""),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Receipt type unrecognised — Invalid receipt types",
                        receiptTypeInput("Bring back"),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Hearing has no listed defendants — No matching defendants with hearings found for the hearing",
                        noMatchingDefendantsForHearingInput(),
                        List.of(processedFailure("No matching defendants with hearings found for the hearing"))),
                new AggregateScenario("Hearing defendant matches but no offences match — No matching defendants with hearings found for the hearing",
                        hearingDefendantMatchesNoOffencesInput(),
                        List.of(processedFailure("No matching defendants with hearings found for the hearing"))),
                new AggregateScenario("Invalid Prosecuting Authority — hasInvalidProsecutingAuthority() (new scenario)",
                        invalidProsecutingAuthorityInput(),
                        List.of(processedFailure("Invalid Prosecuting Authority")))
        );
    }

    /**
     * The three remaining R3a scenarios behind the {@code hasOffenceProblems()} gate — none existed
     * before this story (02-design.md, Coverage). Each is a defendant-level offence problem the
     * aggregate turns into a single fail-fast {@link MigratedCaseFileProcessed}, but only after
     * {@code validateDefendantErrors} has already added one {@link DefendantValidationFailed} for
     * the same defendant — hence two events, not one.
     */
    private static Stream<AggregateScenario> hasOffenceProblemsGateScenarios() {
        return Stream.of(
                new AggregateScenario("Invalid offence code — hasOffenceProblems()/hasInvalidOffenceCode() (new scenario)",
                        invalidOffenceCodeInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-invalid-offence-code.json"),
                                processedFailure("Invalid offence code"))),
                new AggregateScenario("Guilty plea missing plea date — hasOffenceProblems()/hasInvalidPleaDate() (new scenario)",
                        missingPleaDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-missing-plea-date.json"),
                                processedFailure("Missing or Invalid plea date"))),
                new AggregateScenario("Verdict missing verdict date — hasOffenceProblems()/hasInvalidVerdictDate() (new scenario)",
                        missingVerdictDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-missing-verdict-date.json"),
                                processedFailure("Missing or Invalid verdict date")))
        );
    }

    private static Stream<AggregateScenario> materialsMainPathScenarios() {
        return Stream.of(
                new AggregateScenario("Receive migrated case file with one PDF material — happy path",
                        receivedWithMaterialInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-received-with-material.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-received-with-material.json"))),
                new AggregateScenario("Non-PDF material without a matching defendant — fails fast, not a PDF",
                        nonPdfWithoutMaterialInput(),
                        List.of(processedFailure("Court Record Sheet must be a PDF file"))),
                new AggregateScenario("Defendant-level material, no documentation language, invalid parent guardian gender",
                        defendantLevelInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-defendant-level.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-defendant-level.json")))
        );
    }

    private static Stream<AggregateScenario> defendantProblemsScenarios() {
        return Stream.of(
                new AggregateScenario("All defendant fields null — every defendant-level warning fires",
                        allNullDefendantInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-all-null.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [null]"),
                                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [null]"),
                                warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [null]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-all-null.json")))
        );
    }

    private static Stream<AggregateScenario> pleaScenarios() {
        final List<ExpectedEvent> arrestAndChargeDateNoise = List.of(
                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                warning("Defendant validation", "ARREST_DATE_IN_FUTURE : [null]"),
                warning("Defendant validation", "CHARGE_DATE_IN_FUTURE : [Charge date not provided]"),
                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [null]"),
                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [null]"),
                warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [null]"),
                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));

        final List<ExpectedEvent> guiltyPleaWithDateExpected = new ArrayList<>();
        guiltyPleaWithDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-plea-with-date.json"));
        guiltyPleaWithDateExpected.addAll(arrestAndChargeDateNoise);
        guiltyPleaWithDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-guilty-plea-with-date.json", MATERIAL_ADDED_EXCLUSIONS));
        guiltyPleaWithDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-guilty-plea-with-date.json"));

        final List<ExpectedEvent> notGuiltyPleaWithDateExpected = new ArrayList<>();
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-plea-with-date.json"));
        notGuiltyPleaWithDateExpected.addAll(arrestAndChargeDateNoise);
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-not-guilty-plea-with-date.json"));

        final List<ExpectedEvent> notGuiltyMissingDateExpected = new ArrayList<>();
        notGuiltyMissingDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-not-guilty-missing-plea-date.json"));
        notGuiltyMissingDateExpected.addAll(arrestAndChargeDateNoise);
        notGuiltyMissingDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        notGuiltyMissingDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-not-guilty-missing-plea-date.json"));

        final List<ExpectedEvent> badPleaCodeExpected = new ArrayList<>();
        badPleaCodeExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-bad-plea-code.json"));
        badPleaCodeExpected.addAll(arrestAndChargeDateNoise);
        badPleaCodeExpected.add(warning("Offence validation", "INVALID_PLEA : [e1e1e1e1-1111-4111-8111-111111111111]"));
        badPleaCodeExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        badPleaCodeExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-bad-plea-code.json"));

        return Stream.of(
                new AggregateScenario("Guilty plea with plea code and plea date — happy path",
                        guiltyPleaWithDateInput(), guiltyPleaWithDateExpected),
                new AggregateScenario("Not guilty plea with plea code and plea date — happy path",
                        notGuiltyPleaWithDateInput(), notGuiltyPleaWithDateExpected),
                new AggregateScenario("Guilty plea with a future plea date — fails fast",
                        guiltyPleaFutureDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-guilty-plea-future-date.json", PLEA_FUTURE_DATE_EXCLUSIONS),
                                processedFailure("Missing or Invalid plea date"))),
                new AggregateScenario("Not guilty plea with a missing plea date — plea date not required for NG",
                        notGuiltyMissingDateInput(), notGuiltyMissingDateExpected),
                new AggregateScenario("Unrecognised plea code — offence validation warning, still creation-pending",
                        badPleaCodeInput(), badPleaCodeExpected)
        );
    }

    private static Stream<AggregateScenario> genderCourtMarkerScenarios() {
        return Stream.of(
                new AggregateScenario("Defendant and parent/guardian gender provided and valid in CP",
                        genderProvidedInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-provided.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-gender-provided.json"))),
                new AggregateScenario("Defendant and parent/guardian gender provided but not recognised in CP",
                        genderNotMatchInCpInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-match-in-cp.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [NOTINCP]"),
                                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [NOTINCP]"),
                                warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [NOTINCP]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-gender-not-match-in-cp.json"))),
                new AggregateScenario("Sending and receiving court both valid",
                        courtValidInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-court-valid.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-court-valid.json"))),
                new AggregateScenario("Receipt type valid — Either way case",
                        validReceiptTypeInput("Either way case"), receiptTypeExpected("Either way case")),
                new AggregateScenario("Receipt type valid — Transfer",
                        validReceiptTypeInput("Transfer"), receiptTypeExpected("Transfer")),
                new AggregateScenario("Receipt type valid — Voluntary bill",
                        validReceiptTypeInput("Voluntary bill"), receiptTypeExpected("Voluntary bill")),
                new AggregateScenario("Receipt type valid — Indictable",
                        validReceiptTypeInput("Indictable"), receiptTypeExpected("Indictable")),
                new AggregateScenario("Case marker with an unrecognised marker type code",
                        caseMarkerInvalidInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-case-marker-invalid.json"),
                                warning("Case validation", "CASE_MARKER_IS_INVALID : [ABC001]"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-case-marker-invalid.json"))),
                new AggregateScenario("Defendant and parent/guardian gender provided but not recognised in CP (no case marker)",
                        genderNotInCpInput(), genderNotInCpExpected("Either way case")),
                new AggregateScenario("Case marker with a null marker type code",
                        caseMarkerNullOrEmptyInput(null),
                        caseMarkerNullOrEmptyExpected(warning("Case validation", "CASE_MARKER_IS_INVALID : [null]"), "json/aggregate/migrated-case-validated-creation-pending-case-marker-null.json")),
                new AggregateScenario("Case marker with an empty marker type code",
                        caseMarkerNullOrEmptyInput(""),
                        caseMarkerNullOrEmptyExpected(warning("Case validation", "CASE_MARKER_IS_INVALID : []"), "json/aggregate/migrated-case-validated-creation-pending-case-marker-empty.json")),
                new AggregateScenario("Parent/guardian information entirely null",
                        parentGuardianNullInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-parent-guardian-null.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-parent-guardian-null.json"))),
                new AggregateScenario("Custody status C with a missing custody time limit",
                        custodyCWithMissingCtlInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-custody-c-ctl-null.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : [C]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING : [DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-custody-c-ctl-null.json")))
        );
    }

    /**
     * Fixed ID, not {@code randomUUID()} — a {@code MaterialAdded} event carries whichever entry
     * matched, so a random ID here made every fixture built on it non-deterministic. Every material
     * in this test class uses {@code fileType "99"}, which {@code getSections()} maps to
     * {@code ("PSJH", "Private section - Judges & HMCTS")} — {@code CCDocumentTypeValidationRule}
     * matches on the {@code section} string, so this is the only entry any test can ever resolve to.
     */
    private static List<DocumentTypeAccessReferenceData> getDocumentMetadataReferenceDataList() {
        return List.of(
                new DocumentTypeAccessReferenceData(false, null, "Defendant level",
                        UUID.fromString("d2d2d2d2-2222-4222-8222-222222222222"), "Private section - Judges & HMCTS", "PSJH", null, null, null));
    }

    private static List<MigratedMaterial> createMigratedMaterials(final int fileCount, final String fileType) {
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

    /**
     * Next future occurrence of {@code month}/{@code day}, strictly after today. Keeps the GMT
     * (January) / BST (June) hearing-date scenarios below perpetually future — and perpetually in
     * the same DST season — rather than baking in a fixed year that lapses into the past once the
     * real clock catches up (2027-01-15/2027-06-15 would have started failing from 2027 onward).
     */
    private static String nextFutureDate(final int month, final int day) {
        final LocalDate today = LocalDate.now();
        LocalDate candidate = LocalDate.of(today.getYear(), month, day);
        if (!candidate.isAfter(today)) {
            candidate = candidate.plusYears(1);
        }
        return candidate.toString();
    }

    private MigratedCaseDetails buildCaseDetailsWithHearing(final Integer courtRoomId) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));

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

    /** A {@code MigratedCaseValidatedWithWarnings} event carrying a single warning {@code type}/{@code message} pair. */
    private static ExpectedEvent warning(final String type, final String message) {
        return new ExpectedEvent(MigratedCaseValidatedWithWarnings.class, String.format(
                "{\"caseId\": \"%s\", \"type\": \"%s\", \"message\": \"%s\"}", CASE_ID, type, message), true, Map.of(), List.of());
    }

    /** A failed {@code MigratedCaseFileProcessed} event carrying a single failure {@code description}. */
    private static ExpectedEvent processedFailure(final String description) {
        return new ExpectedEvent(MigratedCaseFileProcessed.class, String.format(
                "{\"caseId\": \"%s\", \"description\": \"%s\", \"processingIsSuccessful\": false, \"submissionId\": \"%s\"}",
                CASE_ID, description, SUBMISSION_ID), true, Map.of(), List.of());
    }

    private static CaseFileInput noMaterialsInput(final uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem sourceSystem) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "MALE", E.name(), W.name(), null, null, null, sourceSystem);
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput nullMaterialsInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "MALE", E.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, null);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput courtRecordSheetCountExceedsInput() {
        final MigratedMaterial material1 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString()).withAzureLocation("azure/abc.pdf").withDocumentType(3).withFileName("abc.pdf").withFileType("99").build();
        final MigratedMaterial material2 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID2.toString()).withAzureLocation("azure/def.pdf").withDocumentType(3).withFileName("def.pdf").withFileType("99").build();
        final MigratedMaterial material3 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString()).withAzureLocation("azure/ghi.pdf").withDocumentType(3).withFileName("ghi.pdf").withFileType("99").build();
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("FEMALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of(material1, material2, material3));
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(CaseDetails.caseDetails().withReceiptType("Either way case").build()).build();
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput courtCodeInvalidInput(final CaseDetails caseDetails) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, caseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput receiptTypeInput(final String receiptType) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("FEMALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails().withReceiptType(receiptType).build());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput noMatchingDefendantsForHearingInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withHearings(List.of(MigratedHearing.migratedHearing().withListedDefendants(List.of()).build())).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput hearingDefendantMatchesNoOffencesInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendantWithNonMatchingOffences = migratedDefendant().withId(DEFENDANT_ID).withProsecutorDefendantId("DEF-001")
                .withOffences(List.of(migratedOffence().withOffenceId(UUID.fromString("b1b1b1b1-1111-4111-8111-111111111111")).withProsecutorOffenceId("OFF-001").withOffenceSequenceNumber(1).build())).build();
        final MigratedDefendant secondDefendantWithNonMatchingOffences = migratedDefendant().withId(DEFENDANT_ID2).withProsecutorDefendantId("DEF-002")
                .withOffences(List.of(migratedOffence().withOffenceId(UUID.fromString("b2b2b2b2-2222-4222-8222-222222222222")).withProsecutorOffenceId("OFF-002").withOffenceSequenceNumber(1).build())).build();
        final MigratedCaseDetails migCaseDetailsWithMismatch = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendantWithNonMatchingOffences, secondDefendantWithNonMatchingOffences))
                .withHearings(List.of(MigratedHearing.migratedHearing().withListedDefendants(List.of(
                        ListedDefendant.listedDefendant().withProsecutorDefendantId("DEF-001").withListedOffences(List.of("OFF-999")).build(),
                        ListedDefendant.listedDefendant().withProsecutorDefendantId("DEF-002").withListedOffences(List.of("OFF-888", "OFF-001")).build()
                )).build())).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithMismatch);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithMismatch, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput invalidProsecutingAuthorityInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withProsecutor(Prosecutor.prosecutor().withProsecutingAuthority("NOTREG").build())
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput invalidOffenceCodeInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "BadOffenceCode", null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput missingPleaDateInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").withPleaValue("Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput missingVerdictDateInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedOffence baseOffence = migCaseDetails.getDefendants().get(0).getOffences().get(0);
        final MigratedOffence offenceWithVerdict = MigratedOffence.migratedOffence().withValuesFrom(baseOffence)
                .withVerdict(MigratedVerdict.migratedVerdict().withId(UUID.fromString("f1f1f1f1-1111-4111-8111-111111111111")).build())
                .build();
        final MigratedDefendant defendantWithVerdict = MigratedDefendant.migratedDefendant().withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withOffences(List.of(offenceWithVerdict)).build();
        final MigratedCaseDetails migCaseDetailsWithVerdict = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendantWithVerdict)).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithVerdict);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithVerdict, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setVerdictReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceWithVerdict.getOffenceId(),
                VerdictReferenceData.verdictReferenceData().build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput allNullDefendantInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput guiltyPleaWithDateInput() {
        final List<MigratedMaterial> migratedMaterials = List.of(createMigratedMaterials(2, "pdf").get(0));
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaValue("Guilty").withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput notGuiltyPleaWithDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "NG", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaValue("Not Guilty").withPleaTypeCode("NG").withPleaTypeGuiltyFlag("No").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput guiltyPleaFutureDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        // Genuinely LocalDate.now() here, not PLEA_DATE_ANCHOR — this scenario specifically exercises
        // the PLEA_DATE_CANNOT_BE_FUTURE_DATE rule, which compares against the real clock at
        // execution time. A fixed anchor date would eventually stop being "in the future" and this
        // scenario would silently stop testing what its name says. The resulting non-deterministic
        // date is excluded from the whole-payload comparison below (see PLEA_DATE_EXCLUSIONS).
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", LocalDate.now().plusDays(1), sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").withPleaValue("Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput notGuiltyMissingDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "NG", null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("NG").withPleaTypeGuiltyFlag("No").withPleaValue("Not Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput badPleaCodeInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "badPlea", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static List<ExpectedEvent> receiptTypeExpected(final String receiptType) {
        return List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-in-cp.json"),
                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"),
                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"),
                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-receipt-type.json", Map.of("receiptType", receiptType)));
    }

    private static List<ExpectedEvent> genderNotInCpExpected(final String receiptType) {
        return receiptTypeExpected(receiptType);
    }

    private static List<ExpectedEvent> caseMarkerNullOrEmptyExpected(final ExpectedEvent caseMarkerWarning, final String creationPendingFixture) {
        return List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-in-cp.json"),
                caseMarkerWarning,
                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"),
                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"),
                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, creationPendingFixture));
    }

    private static CaseFileInput genderProvidedInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(MALE.name(), FEMALE.name(), W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput genderNotMatchInCpInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput courtValidInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", E.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withSendingCourt("AB00001")
                .withReceivingCourt("AB00001")
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.getReferenceDataVO().setSendingCourtOrganisationUnit(OrganisationUnitReferenceData.organisationUnitReferenceData().build());
        prosecutionWithReferenceData.getReferenceDataVO().setReceivingCourtOrganisationUnit(OrganisationUnitReferenceData.organisationUnitReferenceData().build());
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    private static CaseFileInput validReceiptTypeInput(final String receiptType) {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails().withReceiptType(receiptType).build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput caseMarkerInvalidInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withCaseMarkers(List.of(CaseMarker.caseMarker()
                        .withMarkerTypeId(UUID.fromString("c1c1c1c1-1111-4111-8111-111111111111"))
                        .withMarkerTypeCode("ABC001")
                        .withMarkerTypeDescription("Test Code")
                        .build()))
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput genderNotInCpInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput caseMarkerNullOrEmptyInput(final String markerTypeCode) {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withCaseMarkers(List.of(CaseMarker.caseMarker()
                        .withMarkerTypeId(UUID.fromString("c2c2c2c2-2222-4222-8222-222222222222"))
                        .withMarkerTypeCode(markerTypeCode)
                        .withMarkerTypeDescription("Test Code")
                        .build()))
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput parentGuardianNullInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(singletonList(MigratedDefendant.migratedDefendant()
                        .withValuesFrom(migCaseDetails.getDefendants().get(0))
                        .withIndividual(Individual.individual()
                                .withValuesFrom(migCaseDetails.getDefendants().get(0).getIndividual())
                                .withParentGuardianInformation(null)
                                .build())
                        .build()))
                .build();
        final Prosecution prosecution = buildProsecution(amendedMigratedDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput custodyCWithMissingCtlInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
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
        final Prosecution prosecution = buildProsecution(amendedMigratedDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput defendantLevelInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput receivedWithMaterialInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    private static CaseFileInput nonPdfWithoutMaterialInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "doc");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", " MALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    /**
     * Shared by {@link #shouldEmitExpectedEventsForScenario} and the standalone hearing-time
     * tests below — the latter can't join the {@code aggregateScenarios()} row harness because
     * each stubs {@code referenceDataQueryService} with scenario-specific courtroom/hearing-type
     * data (no per-row stubbing hook in the shared harness), but they still owe the same
     * whole-payload proof, not a single-field getter dig.
     */
    private static void assertEventsMatchExpected(final List<Object> actual, final List<ExpectedEvent> expected) {
        // Count first: an extra or missing event fails here, naming the problem, rather than
        // surfacing as a confusing payload diff at position 0.
        assertThat(actual, hasSize(expected.size()));

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        for (int i = 0; i < actual.size(); i++) {
            final ExpectedEvent expectedEvent = expected.get(i);
            assertThat("event " + i, actual.get(i), instanceOf(expectedEvent.type()));
            assertThat("event " + i + " payload", objectToJsonObjectConverter.convert(actual.get(i)).toString(),
                    matchesWholePayload(expectedEvent.expectedJson(), expectedEvent.exclusions()));
        }
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

    /**
     * The row and shared assertion block for T2 (see
     * docs/pipeline/DD-43067-DD-43099-pcfdlrm-test-hardening/03-stories.md, Aggregate scenario
     * harness). Every scenario supplies its own fully-built {@link ReceiveMigratedCaseFile} and
     * {@link ProsecutionWithReferenceData} — the harness makes no assumption about what varies
     * between rows, so it serves the {@code isXhibit()} gate proof (PR2) and the fail-fast rows
     * (PR3) alike. Broadening to the main path is PR3's remaining job.
     *
     * <p>{@code fixture} is a classpath path unless {@code inline} is true, in which case it is
     * the expected JSON body itself — see {@link #warning}. {@code parameters} feeds
     * {@code {{token}}} substitution into a path fixture (see {@link FixtureLoader#fixture(String, Map)});
     * it is ignored when {@code inline} is true.
     */
    private record ExpectedEvent(Class<?> type, String fixture, boolean inline, Map<String, String> parameters, List<String> exclusions) {
        private ExpectedEvent(final Class<?> type, final String fixture) {
            this(type, fixture, false, Map.of(), List.of());
        }

        private ExpectedEvent(final Class<?> type, final String fixture, final List<String> exclusions) {
            this(type, fixture, false, Map.of(), exclusions);
        }

        private ExpectedEvent(final Class<?> type, final String fixture, final Map<String, String> parameters) {
            this(type, fixture, false, parameters, List.of());
        }

        private ExpectedEvent(final Class<?> type, final String fixture, final Map<String, String> parameters, final List<String> exclusions) {
            this(type, fixture, false, parameters, exclusions);
        }

        private String expectedJson() {
            return inline ? fixture : FixtureLoader.fixture(fixture, parameters);
        }
    }

    private record CaseFileInput(ReceiveMigratedCaseFile receiveMigratedCaseFile, ProsecutionWithReferenceData prosecutionWithReferenceData) {}

    private record AggregateScenario(String name, CaseFileInput input, List<ExpectedEvent> expected) {
        @Override
        public String toString() {
            return name;
        }
    }

}
