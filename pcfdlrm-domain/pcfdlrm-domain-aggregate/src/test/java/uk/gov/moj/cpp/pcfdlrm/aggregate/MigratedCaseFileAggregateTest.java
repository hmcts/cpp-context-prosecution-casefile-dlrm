package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.createMigratedMaterials;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.noMaterialsInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.FUTURE_DATE_OF_HEARING_EXCLUSIONS;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.FUTURE_HEARING_DATE_GMT;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.FUTURE_WEEK_COMMENCING_START_DATE;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.FUTURE_WEEK_COMMENCING_START_DATE_EXCLUSIONS;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.HEARING_DEFENDANT_VALIDATION_NOISE;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.warning;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate.HEARING_VALIDATION;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildMigratedCaseDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildProsecution;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildReceiveMigratedCaseFile;
import static uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem.sourceSystem;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT_IDENDIFIER;
import static uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader.fixture;
import static uk.gov.moj.cpp.pcfdlrm.test.WholePayloadMatcher.matchesWholePayload;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURTROOM_ID_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.W;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.provider.CcProsecutionValidationRuleProvider;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedWeekCommencingDate;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAddedPendingProcess;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseNotFoundInAutomation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedCaseFileAggregateTest {

    private static final String SHOULD_RAISE_MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION = "shouldRaiseMigratedCaseNotFoundInAutomation()";

    private static final String SHOULD_RAISE_EVENT_ON_MATERIAL_ADDED_POST_PROCESSING = "shouldRaiseEventOnMaterialAddedPostProcessing()";

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

        receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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
                receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData)
        );
        assertEquals("File type matching cps bundle code is not found in map", exception.getMessage());
    }

    // Call-argument check, not output — isXhibit guards discard case problems for LIBRA either way.
    @Test
    void shouldPassLibraSourceSystemNameToCaseValidationRules() {
        final CaseFileInput input = noMaterialsInput(sourceSystem("LIBRA", "LIBRA-123"));
        prosecutionWithReferenceData = input.prosecutionWithReferenceData();

        try (MockedStatic<CcProsecutionValidationRuleProvider> mockedProvider =
                     mockStatic(CcProsecutionValidationRuleProvider.class, CALLS_REAL_METHODS)) {

            receiveMigratedCaseFile(input.receiveMigratedCaseFile(), prosecutionWithReferenceData);

            mockedProvider.verify(() -> CcProsecutionValidationRuleProvider.getCaseValidationRules(any(), eq("LIBRA")));
        }
    }

    // AC-T3-3 — migrationSourceSystemName is not required by schema, so a null one is a real input.
    @Test
    void shouldNotThrowWhenMigrationSourceSystemNameIsAbsent() {
        final CaseFileInput input = noMaterialsInput(sourceSystem(null, "UNKNOWN-1"));
        prosecutionWithReferenceData = input.prosecutionWithReferenceData();

        assertDoesNotThrow(() -> receiveMigratedCaseFile(input.receiveMigratedCaseFile(), prosecutionWithReferenceData));
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

        final List<Object> eventStream = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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

        final List<Object> eventStream = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

        final boolean hasCourtRoomIdWarning = eventStream.stream()
                .filter(e -> e instanceof MigratedCaseValidatedWithWarnings)
                .map(e -> (MigratedCaseValidatedWithWarnings) e)
                .filter(e -> HEARING_VALIDATION.equals(e.getType()))
                .anyMatch(e -> e.getMessage().contains(COURTROOM_ID_INVALID.name()));

        assertThat(hasCourtRoomIdWarning, is(true));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios#fixedHearingTimeDefaultingScenarios")
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

        final List<Object> actual = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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

        final List<Object> actual = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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

        final List<Object> actual = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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

        final List<Object> actual = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

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

        final List<Object> actual = receiveMigratedCaseFile(receiveMigratedCase, prosecutionWithReferenceData);

        final List<ExpectedEvent> expected = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        expected.add(warning("Hearing validation", "COURT_HEARING_LOCATION_OUCODE_INVALID : [C50EX00]"));
        expected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-unscheduled.json"));
        assertEventsMatchExpected(actual, expected);
    }

    @ParameterizedTest
    @MethodSource("uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios#aggregateScenarios")
    void shouldEmitExpectedEventsForScenario(final AggregateScenario scenario) {
        final List<Object> actual = receiveMigratedCaseFile(scenario.input().receiveMigratedCaseFile(), scenario.input().prosecutionWithReferenceData());

        assertEventsMatchExpected(actual, scenario.expected());
    }

    /**
     * Collapses the 8-argument {@link CaseProcessingArgs} construction shared by every test above
     * except {@code shouldThrowNotYetImplementedWhenMaterialValidationFails}, which supplies its
     * own {@code documentMetadataReferenceDataList} and so builds its own.
     */
    private List<Object> receiveMigratedCaseFile(final ReceiveMigratedCaseFile command, final ProsecutionWithReferenceData data) {
        return migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(
                command, data,
                List.of(caseRefDataEnricher), List.of(defendantRefDataEnricher),
                referenceDataQueryService, getSections(),
                getDocumentMetadataReferenceDataList(), List.of(migratedHearingRefDataEnricher))).toList();
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

    /**
     * Shared by {@link #shouldEmitExpectedEventsForScenario} and the standalone hearing-time
     * tests above — the latter can't join the {@code aggregateScenarios()} row harness because
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
     * Fixed ID, not {@code randomUUID()} — a {@code MaterialAdded} event carries whichever entry
     * matched, so a random ID here made every fixture built on it non-deterministic. Every material
     * in this test class uses {@code fileType "99"}, which {@link #getSections()} maps to
     * {@code ("PSJH", "Private section - Judges & HMCTS")} — {@code CCDocumentTypeValidationRule}
     * matches on the {@code section} string, so this is the only entry any test can ever resolve to.
     */
    private static List<DocumentTypeAccessReferenceData> getDocumentMetadataReferenceDataList() {
        return List.of(
                new DocumentTypeAccessReferenceData(false, null, "Defendant level",
                        UUID.fromString("d2d2d2d2-2222-4222-8222-222222222222"), "Private section - Judges & HMCTS", "PSJH", null, null, null));
    }
}
