package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader.fixture;
import static uk.gov.moj.cpp.pcfdlrm.test.ReflectionFieldInjector.writeField;
import static uk.gov.moj.cpp.pcfdlrm.test.WholePayloadMatcher.matchesWholePayload;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.MigratedCaseToProsecutionCaseConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.ProsecutionCaseFileMigratedDefendantToCCDefendantConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.ProsecutionMigrationCaseToCCPersonDefendantConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseReceivedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedAllocationDecision;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCourtIndicatedSentence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class MigratedCaseReceivedProcessorTest {
    private static final UUID PERSON_DEFENDANT_ID = fromString("9824e40f-0289-4221-8854-346eb28c8f27");

    private static final UUID LEGAL_ENTITY_DEFENDANT_ID = fromString("9924e40f-0289-4221-8854-346eb28c8f27");

    private static final UUID GUILTY_OFFENCE_ID = fromString("e1e1e1e1-1111-4111-8111-111111111111");

    private static final UUID NOT_GUILTY_OFFENCE_ID = fromString("e2e2e2e2-2222-4222-8222-222222222222");

    private static final List<String> DEFENDANT_0_EXCLUSIONS = List.of(
            "initiateCourtProceedings.prosecutionCases[0].defendants[0].courtProceedingsInitiated");

    private static final List<String> MAXIMAL_EXCLUSIONS = List.of(
            "initiateCourtProceedings.prosecutionCases[0].defendants[0].courtProceedingsInitiated",
            "initiateCourtProceedings.prosecutionCases[0].defendants[1].courtProceedingsInitiated",
            "initiateCourtProceedings.prosecutionCases[0].caseMarkers[0].id");

    /**
     * Maximal input: 2 defendants (person + legal entity), the person with 2 offences (one
     * guilty — {@code PLEA_DATE_CANNOT_BE_FUTURE_DATE}-style rule nulls its verdict — one not
     * guilty with a verdict present), and a case marker. No hearing here — hearing present/absent is
     * its own isolated row instead of folded into the maximal case.
     */
    private static final UUID MOT_REASON_ID = fromString("d1d1d1d1-1111-4111-8111-111111111111");

    private record ConverterScenario(String name, MigratedCaseFileReceived input, String expectedFixture,
                                      List<String> exclusions, Map<String, String> fixtureParameters) {
        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    void shouldHandleSjpProsecutionEvents() {
        assertThat(MigratedCaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleMigratedCaseReceived")
                        .thatHandles("pcfdlrm.events.migrated-case-file-received"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterScenarios")
    void shouldConvertAndSendInitiateCourtProceedings(final ConverterScenario scenario) {
        final Sender sender = mock(Sender.class);
        final EnvelopeHelper envelopeHelper = mock(EnvelopeHelper.class);
        final PcfMigratedCaseReceivedCounter counter = mock(PcfMigratedCaseReceivedCounter.class);
        final ReferenceDataQueryService referenceDataQueryService = mock(ReferenceDataQueryService.class);

        final Metadata inboundMetadata = metadataBuilder()
                .withName("pcfdlrm.events.migrated-case-file-received")
                .withId(randomUUID())
                .build();
        final JsonEnvelope dummyOutboundEnvelope = JsonEnvelope.envelopeFrom(inboundMetadata, NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(dummyOutboundEnvelope);

        final MigratedCaseReceivedProcessor processor = buildProcessor(sender, envelopeHelper, counter, referenceDataQueryService);

        processor.handleMigratedCaseReceived(envelopeFrom(inboundMetadata, scenario.input()));

        final ArgumentCaptor<JsonEnvelope> converted = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(converted.capture());

        assertThat(converted.getValue().metadata().name(), is("progression.initiate-court-proceedings"));
        assertThat(converted.getValue().payload().toString(),
                matchesWholePayload(fixture(scenario.expectedFixture(), scenario.fixtureParameters()), scenario.exclusions()));

        verify(sender).sendAsAdmin((Envelope<?>) dummyOutboundEnvelope);
        verify(counter).increment();
    }

    /**
     * AC-T3-3 — tier 2's minimal input has no case markers and no hearings; pins that an absent
     * input field produces an OMITTED output field, not an explicit JSON {@code null}, as its own
     * named assertion. {@link #shouldConvertAndSendInitiateCourtProceedings} already enforces this
     * for the minimal-tier row via {@code WholePayloadMatcher}'s STRICT comparison (a fixture with no
     * {@code caseMarkers}/{@code listHearingRequests} key would fail against real output that had
     * either as an explicit {@code null}), but only as a side effect of matching the whole payload —
     * this test names the distinction directly.
     */
    @Test
    void shouldOmitRatherThanEmitNullForFieldsAbsentFromMinimalInput() throws com.fasterxml.jackson.core.JsonProcessingException {
        final Sender sender = mock(Sender.class);
        final EnvelopeHelper envelopeHelper = mock(EnvelopeHelper.class);
        final PcfMigratedCaseReceivedCounter counter = mock(PcfMigratedCaseReceivedCounter.class);
        final ReferenceDataQueryService referenceDataQueryService = mock(ReferenceDataQueryService.class);

        final Metadata inboundMetadata = metadataBuilder()
                .withName("pcfdlrm.events.migrated-case-file-received")
                .withId(randomUUID())
                .build();
        final JsonEnvelope dummyOutboundEnvelope = JsonEnvelope.envelopeFrom(inboundMetadata, NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(dummyOutboundEnvelope);

        final MigratedCaseReceivedProcessor processor = buildProcessor(sender, envelopeHelper, counter, referenceDataQueryService);
        processor.handleMigratedCaseReceived(envelopeFrom(inboundMetadata, minimalInput()));

        final ArgumentCaptor<JsonEnvelope> converted = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(converted.capture());

        final JsonNode payload = new ObjectMapper().readTree(converted.getValue().payload().toString());
        final JsonNode initiateCourtProceedings = payload.get("initiateCourtProceedings");
        final JsonNode prosecutionCase = initiateCourtProceedings.get("prosecutionCases").get(0);

        assertThat(prosecutionCase.has("caseMarkers"), is(false));
        assertThat(initiateCourtProceedings.has("listHearingRequests"), is(false));
    }

    /**
     * AC-T3-2 — pins the current NPE behaviour when the hearing list is null (see
     * {@link #withNullHearingListInput()}). Not folded into {@link #converterScenarios()} — that
     * harness's {@code ConverterScenario} assumes a successful whole-payload comparison, which
     * doesn't apply to an exception-throwing branch. Unlike {@code defendants} (mandatory,
     * {@code minItems: 1}, in stagingdlrm's outbound schema — the only real production caller of this
     * endpoint), {@code hearings} is genuinely optional there too, so this branch stays reachable in
     * production and is worth pinning; the equivalent null-defendants test was removed as unreachable.
     */
    @Test
    void shouldThrowNpeWhenHearingListIsNull() {
        final Sender sender = mock(Sender.class);
        final EnvelopeHelper envelopeHelper = mock(EnvelopeHelper.class);
        final PcfMigratedCaseReceivedCounter counter = mock(PcfMigratedCaseReceivedCounter.class);
        final ReferenceDataQueryService referenceDataQueryService = mock(ReferenceDataQueryService.class);
        final MigratedCaseReceivedProcessor processor = buildProcessor(sender, envelopeHelper, counter, referenceDataQueryService);

        final Metadata inboundMetadata = metadataBuilder()
                .withName("pcfdlrm.events.migrated-case-file-received")
                .withId(randomUUID())
                .build();

        final NullPointerException npe = assertThrows(NullPointerException.class, () ->
                processor.handleMigratedCaseReceived(envelopeFrom(inboundMetadata, withNullHearingListInput())));
        assertThat(npe.getStackTrace()[0].getClassName(), is(MigratedCaseToProsecutionCaseConverter.class.getName()));
    }

    /**
     * AC-T3-5 — the {@code MigratedCaseFileReceived} fixture used as processor input round-trips
     * JSON → POJO → JSON byte-for-byte. Proves the generated POJO doesn't silently drop a field
     * before the downstream STRICT comparison ever gets a chance to see it. Deserialises the three
     * sub-trees separately, not the {@code MigratedCaseFileReceived} wrapper itself — that wrapper is
     * immutable/builder-only with no {@code @JsonCreator}, so Jackson can't construct it directly;
     * the round-trip risk the AC cares about (a field dropped by a generated POJO) lives entirely in
     * {@code ReceiveMigratedCaseFile}'s nested schema tree, which this does exercise.
     */
    @Test
    void shouldRoundTripReceiveMigratedCaseFileFixtureWithoutLosingFields() throws com.fasterxml.jackson.core.JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

        final ReceiveMigratedCaseFile original = maximalInput().getReceiveMigratedCaseFile();
        final String firstPass = objectToJsonObjectConverter.convert(original).toString();

        final ReceiveMigratedCaseFile roundTripped = objectMapper.readValue(firstPass, ReceiveMigratedCaseFile.class);
        final String secondPass = objectToJsonObjectConverter.convert(roundTripped).toString();

        assertThat(objectMapper.readTree(secondPass), is(objectMapper.readTree(firstPass)));
    }

    /**
     * All six converters use private {@code @Inject} fields, and {@code @InjectMocks} only reaches
     * one level of this three-level hierarchy — so the tree is assembled by hand via reflection,
     * using the generic {@code ReflectionFieldInjector.writeField} helper from {@code pcfdlrm-test-support}.
     * The concrete tree topology below stays local to this class rather than moving into that shared
     * helper: it names six {@code pcfdlrm-event-processor} classes directly, and this module already
     * depends on {@code pcfdlrm-test-support} at test scope — a dependency back the other way would
     * be a Maven reactor cycle. Field names below are read directly off each converter's source, not
     * guessed. Only {@code ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter} needs a
     * {@code ReferenceDataQueryService} — the design doc's claim that the root converter also needs
     * one does not match the current code.
     */
    private static MigratedCaseToProsecutionCaseConverter buildConverterTree(final ReferenceDataQueryService referenceDataQueryService) {
        final ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter offenceConverter =
                new ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter();
        writeField(offenceConverter, "referenceDataQueryService", referenceDataQueryService);

        final ProsecutionCaseFileMigratedDefendantToCCDefendantConverter defendantConverter =
                new ProsecutionCaseFileMigratedDefendantToCCDefendantConverter();
        writeField(defendantConverter, "prosecutionMigrationCaseToCCPersonDefendantConverter",
                new ProsecutionMigrationCaseToCCPersonDefendantConverter());
        writeField(defendantConverter, "prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter",
                new ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter());
        writeField(defendantConverter, "prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter",
                offenceConverter);

        final MigratedCaseToProsecutionCaseConverter rootConverter = new MigratedCaseToProsecutionCaseConverter();
        writeField(rootConverter, "prosecutionCaseFileMigratedDefendantToCCDefendantConverter",
                defendantConverter);
        writeField(rootConverter, "prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter",
                new ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter());

        return rootConverter;
    }

    /**
     * Wires a real {@link MigratedCaseReceivedProcessor} with a real converter tree, keeping only
     * {@code sender}, {@code envelopeHelper} and {@code pcfMigratedCaseReceivedCounter} as mocks —
     * matching the design doc's "capture at envelopeHelper, not sender" decision.
     */
    private static MigratedCaseReceivedProcessor buildProcessor(final Sender sender,
                                                                 final EnvelopeHelper envelopeHelper,
                                                                 final PcfMigratedCaseReceivedCounter counter,
                                                                 final ReferenceDataQueryService referenceDataQueryService) {
        final MigratedCaseReceivedProcessor processor = new MigratedCaseReceivedProcessor();
        writeField(processor, "sender", sender);
        writeField(processor, "migratedCaseToProsecutionCaseConverter", buildConverterTree(referenceDataQueryService));
        writeField(processor, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));
        writeField(processor, "envelopeHelper", envelopeHelper);
        writeField(processor, "pcfMigratedCaseReceivedCounter", counter);
        return processor;
    }

    /** Minimal input: only the fields the converter tree cannot run without. No defendants, no hearings. */
    private static MigratedCaseFileReceived minimalInput() {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withProsecutorCaseReference("URN001")
                .withReceiptType("Either way case")
                .build();

        final MigrationSourceSystem migrationSourceSystem = MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("XHIBIT")
                .withMigrationSourceSystemCaseIdentifier("XHIBIT-123")
                .build();

        final MigratedCaseDetails migratedCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(caseDetails)
                .withMigrationSourceSystem(migrationSourceSystem)
                .withDefendants(List.of())
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withChannel(Channel.DLRM_MIGRATION)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(fromString("b1b1b1b1-1111-4111-8111-111111111111"))
                .withShortName("CPS")
                .build());

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(List.of())
                .build();
    }

    /** A person defendant, isolated (no plea/verdict/hearing). */
    private static MigratedCaseFileReceived withOnePersonDefendantInput() {
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation()
                .withGender("MALE")
                .build();

        final PersonalInformation personalInformation = PersonalInformation.personalInformation()
                .withFirstName("John")
                .withLastName("Smith")
                .withAddress(Address.address()
                        .withAddress1("1 Test Street")
                        .withPostcode("SW1A 1AA")
                        .build())
                .build();

        final Individual individual = Individual.individual()
                .withSelfDefinedInformation(selfDefinedInformation)
                .withPersonalInformation(personalInformation)
                .build();

        final MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(GUILTY_OFFENCE_ID)
                .withOffenceCode("998A")
                .withOffenceSequenceNumber(1)
                .withOffenceCommittedDate(LocalDate.of(2024, 1, 15))
                .build();

        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withId(PERSON_DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-001")
                .withDocumentationLanguage("W")
                .withHearingLanguage("W")
                .withIndividual(individual)
                .withOffences(List.of(offence))
                .build();

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withProsecutorCaseReference("URN001")
                .withReceiptType("Either way case")
                .build();

        final MigrationSourceSystem migrationSourceSystem = MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("XHIBIT")
                .withMigrationSourceSystemCaseIdentifier("XHIBIT-123")
                .build();

        final MigratedCaseDetails migratedCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(caseDetails)
                .withMigrationSourceSystem(migrationSourceSystem)
                .withDefendants(List.of(defendant))
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withChannel(Channel.DLRM_MIGRATION)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(fromString("b1b1b1b1-1111-4111-8111-111111111111"))
                .withShortName("CPS")
                .build());

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(List.of())
                .build();
    }

    /** A legal-entity defendant, isolated (no person defendant). */
    private static MigratedCaseFileReceived withLegalEntityDefendantInput() {
        final MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(GUILTY_OFFENCE_ID)
                .withOffenceCode("998A")
                .withOffenceSequenceNumber(1)
                .withOffenceCommittedDate(LocalDate.of(2024, 1, 15))
                .build();

        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withId(LEGAL_ENTITY_DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-002")
                .withOrganisationName("Acme Ltd")
                .withOffences(List.of(offence))
                .build();

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withProsecutorCaseReference("URN001")
                .withReceiptType("Either way case")
                .build();

        final MigrationSourceSystem migrationSourceSystem = MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("XHIBIT")
                .withMigrationSourceSystemCaseIdentifier("XHIBIT-123")
                .build();

        final MigratedCaseDetails migratedCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(caseDetails)
                .withMigrationSourceSystem(migrationSourceSystem)
                .withDefendants(List.of(defendant))
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withChannel(Channel.DLRM_MIGRATION)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(fromString("b1b1b1b1-1111-4111-8111-111111111111"))
                .withShortName("CPS")
                // No informantEmailAddress — exercises buildContact()'s contactEmailAddress fallback branch
                // (jacoco: MigratedCaseToProsecutionCaseConverter.lambda$buildContact$6), the maximal
                // scenario's prosecutorsReferenceData covers the informantEmailAddress branch instead.
                .withContactEmailAddress("contact@example.com")
                .build());

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(List.of())
                .build();
    }

    /**
     * Next future occurrence of {@code month}/{@code day}, strictly after today — same time-bomb
     * shape as DD-43099 T2's GMT/BST hearing-date fix
     * (see {@code MigratedCaseFileAggregateTest.nextFutureDate}): the hearing converter's
     * {@code isValidHearing} excludes any {@code dateOfHearing} that is {@code isBefore(LocalDate.now())}.
     */
    private static String nextFutureDate(final int month, final int day) {
        final LocalDate today = LocalDate.now();
        LocalDate candidate = LocalDate.of(today.getYear(), month, day);
        if (!candidate.isAfter(today)) {
            candidate = candidate.plusYears(1);
        }
        return candidate.toString();
    }

    /** A valid, future-dated hearing present; {@code listHearingRequests} is populated. */
    private static MigratedCaseFileReceived withHearingInput() {
        final MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(GUILTY_OFFENCE_ID)
                .withOffenceCode("998A")
                .withOffenceSequenceNumber(1)
                .withOffenceCommittedDate(LocalDate.of(2024, 1, 15))
                .build();

        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withId(PERSON_DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-001")
                .withDocumentationLanguage("W")
                .withHearingLanguage("W")
                .withOffences(List.of(offence))
                .build();

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withProsecutorCaseReference("URN001")
                .withReceiptType("Either way case")
                .build();

        final MigrationSourceSystem migrationSourceSystem = MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("XHIBIT")
                .withMigrationSourceSystemCaseIdentifier("XHIBIT-123")
                .build();

        final MigratedCaseDetails migratedCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(caseDetails)
                .withMigrationSourceSystem(migrationSourceSystem)
                .withDefendants(List.of(defendant))
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withChannel(Channel.DLRM_MIGRATION)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();

        final MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C50EX00")
                .withCourtRoomId(235)
                .withDateOfHearing(nextFutureDate(1, 15))
                .withTimeOfHearing("10:00:00")
                .withDurationMinutes(30)
                .build();

        final ReferenceDataVO hearingReferenceDataVO = new ReferenceDataVO();
        hearingReferenceDataVO.setHearingType(HearingType.hearingType()
                .withId(fromString("1a1a1a1a-1111-4111-8111-111111111111"))
                .withHearingDescription("First hearing")
                .build());
        hearingReferenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                .withId("a1a1a1a1-1111-4111-8111-111111111111")
                .withOucodeL1Code("B")
                .withOucodeL3Name("Test Court")
                .withCourtrooms(List.of(CourtRoom.courtRoom()
                        .withId("1c1c1c1c-1111-4111-8111-111111111111")
                        .withCourtroomId(235)
                        .withCourtroomName("Courtroom 1")
                        .build()))
                .build());

        final MigratedHearingWithReferenceData migratedHearingWithReferenceData = new MigratedHearingWithReferenceData();
        migratedHearingWithReferenceData.setCaseDetails(caseDetails);
        migratedHearingWithReferenceData.setReferenceDataVO(hearingReferenceDataVO);
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(
                new MigratedDefendantWithOffences(defendant, List.of(GUILTY_OFFENCE_ID))));

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(fromString("b1b1b1b1-1111-4111-8111-111111111111"))
                .withShortName("CPS")
                .build());

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(List.of(migratedHearingWithReferenceData))
                .build();
    }

    /**
     * Same hearing as {@link #withHearingInput()} but dated in the past: {@code isValidHearing}
     * excludes it, so {@code listHearingRequests} is absent from the payload even though a hearing
     * was supplied. Demonstrates the validity gate, not just its absence.
     */
    private static MigratedCaseFileReceived withPastHearingInput() {
        final MigratedCaseFileReceived future = withHearingInput();
        final MigratedHearingWithReferenceData original = future.getMigratedHearingWithReferenceDataList().get(0);
        final MigratedHearingWithReferenceData past = new MigratedHearingWithReferenceData();
        past.setCaseDetails(original.getCaseDetails());
        past.setReferenceDataVO(original.getReferenceDataVO());
        past.setMigratedHearing(MigratedHearing.migratedHearing()
                .withValuesFrom(original.getMigratedHearing())
                .withDateOfHearing("2020-01-15")
                .build());
        past.setMigratedDefendantWithOffences(original.getMigratedDefendantWithOffences());

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withValuesFrom(future)
                .withMigratedHearingWithReferenceData(List.of(past))
                .build();
    }

    private static MigratedCaseFileReceived maximalInput() {
        final MigratedOffence guiltyOffence = MigratedOffence.migratedOffence()
                .withOffenceId(GUILTY_OFFENCE_ID)
                .withOffenceCode("998A")
                .withOffenceSequenceNumber(1)
                .withOffenceCommittedDate(LocalDate.of(2024, 1, 15))
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(fromString("f1f1f1f1-1111-4111-8111-111111111111"))
                        .withPleaDate(LocalDate.of(2024, 1, 20))
                        .build())
                .withAllocationDecision(MigratedAllocationDecision.migratedAllocationDecision()
                        .withMotReasonId(MOT_REASON_ID)
                        .withCourtIndicatedSentence(MigratedCourtIndicatedSentence.migratedCourtIndicatedSentence()
                                .withCourtIndicatedSentenceTypeId(fromString("d2d2d2d2-2222-4222-8222-222222222222"))
                                .withCourtIndicatedSentenceDescription("Indicated custodial sentence")
                                .build())
                        .build())
                .build();

        final MigratedOffence notGuiltyOffence = MigratedOffence.migratedOffence()
                .withOffenceId(NOT_GUILTY_OFFENCE_ID)
                .withOffenceCode("998B")
                .withOffenceSequenceNumber(2)
                .withOffenceCommittedDate(LocalDate.of(2024, 1, 16))
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(fromString("f2f2f2f2-2222-4222-8222-222222222222"))
                        .withPleaDate(LocalDate.of(2024, 1, 21))
                        .build())
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(fromString("f3f3f3f3-3333-4333-8333-333333333333"))
                        .withVerdictDate(LocalDate.of(2024, 1, 30))
                        .build())
                .build();

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation()
                .withGender("MALE")
                .build();

        final PersonalInformation personalInformation = PersonalInformation.personalInformation()
                .withFirstName("John")
                .withLastName("Smith")
                .withAddress(Address.address()
                        .withAddress1("1 Test Street")
                        .withPostcode("SW1A 1AA")
                        .build())
                .build();

        final Individual individual = Individual.individual()
                .withSelfDefinedInformation(selfDefinedInformation)
                .withPersonalInformation(personalInformation)
                .build();

        final MigratedDefendant personDefendant = MigratedDefendant.migratedDefendant()
                .withId(PERSON_DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-001")
                .withDocumentationLanguage("W")
                .withHearingLanguage("W")
                .withIndividual(individual)
                .withOffences(List.of(guiltyOffence, notGuiltyOffence))
                .build();

        final MigratedDefendant legalEntityDefendant = MigratedDefendant.migratedDefendant()
                .withId(LEGAL_ENTITY_DEFENDANT_ID)
                .withProsecutorDefendantId("DEF-002")
                .withOrganisationName("Acme Ltd")
                .withOffences(List.of())
                .build();

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withProsecutorCaseReference("URN001")
                .withReceiptType("Either way case")
                .withCaseMarkers(List.of(CaseMarker.caseMarker()
                        .withMarkerTypeCode("ABC001")
                        .withMarkerTypeDescription("Marker One")
                        .build()))
                .build();

        final MigrationSourceSystem migrationSourceSystem = MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("XHIBIT")
                .withMigrationSourceSystemCaseIdentifier("XHIBIT-123")
                .build();

        final MigratedCaseDetails migratedCaseDetails = MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(caseDetails)
                .withMigrationSourceSystem(migrationSourceSystem)
                .withDefendants(List.of(personDefendant, legalEntityDefendant))
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withChannel(Channel.DLRM_MIGRATION)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(fromString("b1b1b1b1-1111-4111-8111-111111111111"))
                .withShortName("CPS")
                // Exercises buildContact()'s informantEmailAddress branch (jacoco: lambda$buildContact$5);
                // the legal-entity scenario's prosecutorsReferenceData covers the contactEmailAddress
                // fallback branch instead.
                .withInformantEmailAddress("informant@example.com")
                .build());
        referenceDataVO.setCaseMarkers(List.of(CaseMarker.caseMarker()
                .withMarkerTypeCode("ABC001")
                .withMarkerTypeDescription("Marker One")
                .build()));
        // Exercises convert()'s organisationUnitWithCourtroomReferenceData.ifPresent(...) branch
        // (jacoco: lambda$convert$3) — a singular-"Courtroom" reference distinct from the plural
        // "Courtrooms" one used by the hearing converter.
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData()
                .withId("e1e1e1e1-1111-4111-8111-111111111111")
                .withOucodeL1Code("B")
                .build());
        referenceDataVO.setModeOfTrialReferenceData(List.of(ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData()
                .withId(MOT_REASON_ID.toString())
                .withCode("MOT01")
                .withDescription("Either way — allocated to Crown Court")
                .withSeqNum("1")
                .build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(
                PERSON_DEFENDANT_ID, Map.of(
                        GUILTY_OFFENCE_ID, PleaReferenceData.pleaReferenceData()
                                .withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").withPleaValue("GUILTY").build(),
                        NOT_GUILTY_OFFENCE_ID, PleaReferenceData.pleaReferenceData()
                                .withPleaTypeCode("NG").withPleaTypeGuiltyFlag("No").withPleaValue("NOT GUILTY").build())));
        referenceDataVO.setVerdictReferenceDataMap(Map.of(
                PERSON_DEFENDANT_ID, Map.of(
                        NOT_GUILTY_OFFENCE_ID, VerdictReferenceData.verdictReferenceData()
                                .withId(fromString("c1c1c1c1-1111-4111-8111-111111111111"))
                                .withCategory("Guilty")
                                .withCjsVerdictCode("1001")
                                .withDescription("Convicted after trial")
                                .withVerdictCode("GAT")
                                .build())));

        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(List.of())
                .build();
    }

    /**
     * AC-T3-2 — tier-3 branch row covering the "null collections" dimension for the hearing list:
     * {@code MigratedCaseToProsecutionCaseConverter.convert()} calls
     * {@code source.getMigratedHearingWithReferenceDataList().stream()} with no null-guard. Real
     * production robustness gap, not a test artifact — {@code hearings} is optional in stagingdlrm's
     * outbound schema (the only real production caller of this endpoint), so a null/absent hearing
     * list is reachable in production; confirmed by real execution. Out of scope for this test-only
     * story to fix in {@code src/main}; used by {@link #shouldThrowNpeWhenHearingListIsNull()} to pin
     * the current behaviour rather than leaving this branch uncovered.
     */
    private static MigratedCaseFileReceived withNullHearingListInput() {
        return MigratedCaseFileReceived.migratedCaseFileReceived()
                .withValuesFrom(minimalInput())
                .withMigratedHearingWithReferenceData(null)
                .build();
    }

    /**
     * {@code dateOfHearing} floats via {@link #nextFutureDate}, so {@code listedStartDateTime} in
     * the hearing-present fixture floats too — read the year back off the already-built input rather
     * than recomputing {@link #nextFutureDate} a second time, so this can never drift from the value
     * the scenario actually used.
     */
    private static String hearingYearOf(final MigratedCaseFileReceived input) {
        return input.getMigratedHearingWithReferenceDataList().get(0).getMigratedHearing().getDateOfHearing().substring(0, 4);
    }

    private static Stream<ConverterScenario> converterScenarios() {
        final MigratedCaseFileReceived hearingPresentInput = withHearingInput();
        return Stream.of(
                new ConverterScenario("Minimal input — no defendants, no hearings",
                        minimalInput(), "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-minimal.json", List.of(), Map.of()),
                new ConverterScenario("Person defendant, isolated",
                        withOnePersonDefendantInput(), "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-person-defendant.json", DEFENDANT_0_EXCLUSIONS, Map.of()),
                new ConverterScenario("Legal entity defendant, isolated",
                        withLegalEntityDefendantInput(), "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-legal-entity-defendant.json", DEFENDANT_0_EXCLUSIONS, Map.of()),
                new ConverterScenario("Valid future hearing present",
                        hearingPresentInput, "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-hearing-present.json", DEFENDANT_0_EXCLUSIONS,
                        Map.of("hearingYear", hearingYearOf(hearingPresentInput))),
                new ConverterScenario("Past hearing excluded by isValidHearing()",
                        withPastHearingInput(), "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-past-hearing-excluded.json", DEFENDANT_0_EXCLUSIONS, Map.of()),
                new ConverterScenario("Maximal input — 2 defendants, plea+verdict, case marker",
                        maximalInput(), "json/xhibit/migrated-case-received-processor/initiate-court-proceedings-maximal.json", MAXIMAL_EXCLUSIONS, Map.of())
        );
    }

}
