package uk.gov.moj.cpp.pcfdlrm.it;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.openejb.config.QuickJarsTxtParser.FILE_NAME;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.moj.cpp.pcfdlrm.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.pcfdlrm.helper.FileUtil.getStringFromResource;
import static uk.gov.moj.cpp.pcfdlrm.helper.WiremockTestHelper.createCommonMockEndpoints;
import static uk.gov.moj.cpp.pcfdlrm.stub.ProgressionStub.stubForAddCourtDocument;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.helper.AddMaterialHelper;
import uk.gov.moj.cpp.pcfdlrm.helper.ReceiveMigratedCaseFileHelper;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ReceiveMigratedCaseFileIT {
    private static final String NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING = "No matching defendants with hearings found for the hearing";

    private static final String COURT_RECORD_SHEET_NOT_PDF = "Court Record Sheet must be a PDF file";

    private final ReceiveMigratedCaseFileHelper receiveMigratedCaseFileHelper = new ReceiveMigratedCaseFileHelper();

    private final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

    // DD-43463: accepted LIBRA bundle codes and their STE reference-data level.
    private static final Map<String, String> LIBRA_BUNDLE_LEVELS = Map.of(
            "1", "Defendant level",
            "2", "Defendant level",
            "3", "Case level",
            "6", "Defendant level",
            "8", "Case level",
            "99", "Defendant level");

    @BeforeAll
    static void setup() {
        createCommonMockEndpoints();
        stubWireMocks();
        stubForAddCourtDocument();
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("command-json/pcfdlrm.command.receive-migrated-case-file.json", new String[]{"1"}, "99"),
                Arguments.of("command-json/pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json", new String[]{"3", "4"}, "9"),
                Arguments.of("command-json/pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json", new String[]{"5", "6"},"9"),
                Arguments.of("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit.json", new String[]{"7"},"99"),
                Arguments.of("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-custody.json", new String[]{"9"}, "99"),
                Arguments.of("command-json/pcfdlrm.command.receive-multiple-hearing-migrated-complex-case-file.json", new String[]{"17"},"99"),
                Arguments.of("command-json/pcfdlrm.command.receive-migrated-case-file-unrecognised-hearingLanguage.json", new String[]{"18"},"99"),
                Arguments.of("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-sending-court.json", new String[]{"22"},"99")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void receiveMigratedCaseFile(final String path, final String[] filePathIndex, String fileType) {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource(path)
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());


        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForMultipleMaterial(addMaterialHelper, submissionId, filePathIndex, fileType);
    }

    @Test
    void receiveMigratedCaseFileWhenXhibitMaterialIsNotPdf() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-invalid-file-type.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyCaseProcessed(addMaterialHelper, submissionId, COURT_RECORD_SHEET_NOT_PDF);
    }

    @Test
    void receiveMigratedCaseFileWhenNoVerdictDate() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-verdict-date.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyCaseProcessed(addMaterialHelper, submissionId, "Missing or Invalid verdict date");
    }

    @Test
    void receiveMigratedCaseFileWhenSourceSystemIsXHIBITWhenGenderNotInCP() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-gender.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId);

        final String[] filepathIndexOne = {"11"};
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForMultipleMaterial(addMaterialHelper, submissionId, filepathIndexOne, "99");
    }

    @Test
    void receiveMigratedCaseFileWithNotGuiltyPleaAndMissingPleaDate() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-with-not-guilty-plea.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());
        final String[] filepathIndexOne = {"13"};
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForMultipleMaterial(addMaterialHelper, submissionId, filepathIndexOne, "99");

    }

    @ParameterizedTest
    @CsvSource({"true, 15", "false, 16"})
    void shouldReceiveMigratedCaseFileWithRetrialIndicator(boolean retrialIndicator, String index) {
        final String submissionId = UUID.randomUUID().toString();

        final String caseId = UUID.randomUUID().toString();

        final String defendantId = UUID.randomUUID().toString();

        final String materialId = randomUUID().toString();

        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-retrail-indicator-"+retrialIndicator+".json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);

        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForSingleMaterial(addMaterialHelper, submissionId, new String[]{index}, "99");

        final JsonEnvelope materialAddedToMaterialContextPayload = createMaterialAddedPayload(materialId, caseId, defendantId);

        addMaterialHelper.sendMessage(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, retrialIndicator ? "retrial-true" : "retrial-false");
    }

    // DD-43888: the LIBRA row reuses the XHIBIT payload with the source system swapped and its own material
    // location, as each upload location must be unique across the run.
    @ParameterizedTest
    @CsvSource({"command-json/pcfdlrm.command.receive-migrated-case-file-with-no-allocation-decision.json, 20, allocation-no-decision, XHIBIT",
                "command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json, 21, allocation-with-decision, XHIBIT",
                "command-json/pcfdlrm.command.receive-migrated-case-file-with-indictable-allocation-decision.json, 19, allocation-with-indictable-decision, XHIBIT",
                "command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json, 24, libra-allocation-with-decision, LIBRA"
    })
    void shouldSetAllocationDecisionForDifferentScenario(String fileName, String index, String initiateCourtProceedingsFixture, String sourceSystemName) {

        final String submissionId = UUID.randomUUID().toString();

        final String caseId = UUID.randomUUID().toString();

        final String defendantId = UUID.randomUUID().toString();

        final String materialId = randomUUID().toString();

        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String staticPayLoad = getStringFromResource(fileName)
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString())
                .replace("\"migrationSourceSystemName\": \"XHIBIT\"", "\"migrationSourceSystemName\": \"" + sourceSystemName + "\"")
                .replaceAll("http://inazure/secrets/\\d+", "http://inazure/secrets/" + index);

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);

        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForSingleMaterial(addMaterialHelper, submissionId, new String[]{ index }, "99");

        final JsonEnvelope materialAddedToMaterialContextPayload = createMaterialAddedPayload(materialId, caseId, defendantId);

        addMaterialHelper.sendMessage(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiatedWithAllocationDecision(caseUrn, initiateCourtProceedingsFixture);
    }

    // DD-43463: every accepted LIBRA bundle reaches material-ready-for-court-document on accept at its
    // reference-data level. Reuses the XHIBIT allocation-decision payload with the source system and
    // materials swapped; upload locations 25-30 are unique across the run.
    @Test
    void shouldSendEveryAcceptedLibraBundleForCourtDocumentOnAccept() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString())
                .replace("\"migrationSourceSystemName\": \"XHIBIT\"", "\"migrationSourceSystemName\": \"LIBRA\"");

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(withLibraMaterials(staticPayLoad));

        // pcfdlrm sets each material's level from reference data (MaterialAdded.documentCategory).
        final Map<String, String> materialIdToBundle = new HashMap<>();
        final Map<String, String> bundleToLevel = new HashMap<>();
        for (int i = 0; i < LIBRA_BUNDLE_LEVELS.size(); i++) {
            final JsonObject materialAdded = receiveMigratedCaseFileHelper.nextMaterialAdded(addMaterialHelper).payloadAsJsonObject();
            final String bundle = materialAdded.getJsonObject("material").getString("fileType");
            final String level = materialAdded.getString("documentCategory");
            bundleToLevel.put(bundle, level);

            // Material echoes back the category pcfdlrm sent it.
            final String materialId = randomUUID().toString();
            materialIdToBundle.put(materialId, bundle);
            sendMaterialAdded(createMaterialAddedPayload(materialId, caseId, defendantId, level));
        }
        assertThat(bundleToLevel, is(LIBRA_BUNDLE_LEVELS));

        receiveMigratedCaseFileHelper.verifyMigratedCaseFileReceived(addMaterialHelper, submissionId);
        receiveMigratedCaseFileHelper.triggerAcceptMigratedCase(caseId);

        for (int i = 0; i < LIBRA_BUNDLE_LEVELS.size(); i++) {
            final JsonObject event = receiveMigratedCaseFileHelper.nextMaterialReadyForCourtDocument(addMaterialHelper).payloadAsJsonObject();
            final String bundle = materialIdToBundle.remove(event.getString("materialId"));
            final JsonObject category = event.getJsonObject("courtDocument").getJsonObject("documentCategory");

            if ("Case level".equals(LIBRA_BUNDLE_LEVELS.get(bundle))) {
                assertThat("bundle " + bundle, category.getJsonObject("caseDocument").getString("prosecutionCaseId"), is(caseId));
                assertThat("bundle " + bundle, category.containsKey("defendantDocument"), is(false));
            } else {
                assertThat("bundle " + bundle, category.getJsonObject("defendantDocument").getJsonArray("defendants").getString(0), is(defendantId));
                assertThat("bundle " + bundle, category.getJsonObject("defendantDocument").getJsonArray("defendants").size(), is(1));
            }
        }
        assertThat(materialIdToBundle.isEmpty(), is(true));

        receiveMigratedCaseFileHelper.verifyMigratedCaseAccepted(addMaterialHelper, submissionId, caseId, caseUrn);
    }

    // The LIBRA upload date arrives as the migrated material's receivedDateTime and reaches the court
    // document as materials[0].uploadDateTime. Upload location 31 is unique across the run.
    @Test
    void shouldSendLibraMaterialUploadDateAsCourtDocumentUploadDateTimeOnAccept() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);
        final ZonedDateTime libraUploadDateTime = ZonedDateTime.parse("2019-03-14T10:22:31.107Z");

        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString())
                .replace("\"migrationSourceSystemName\": \"XHIBIT\"", "\"migrationSourceSystemName\": \"LIBRA\"");

        final JsonObject receiveMigratedCaseFile = JsonObjects.createReader(new StringReader(staticPayLoad)).readObject();
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(JsonObjects.createObjectBuilder(receiveMigratedCaseFile)
                .add("materials", JsonObjects.createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("azureLocation", "http://inazure/secrets/31")
                        .add("fileName", "bundle-3.pdf")
                        .add("fileType", "3")
                        .add("documentType", 2)
                        .add("receivedDateTime", libraUploadDateTime.toOffsetDateTime().toString())))
                .build()
                .toString());

        final JsonObject materialAdded = receiveMigratedCaseFileHelper.nextMaterialAdded(addMaterialHelper).payloadAsJsonObject();
        final String uploadDateTime = materialAdded.getString("uploadDateTime");
        assertThat(ZonedDateTime.parse(uploadDateTime).toInstant(), is(libraUploadDateTime.toInstant()));

        // Material echoes back the ccMetadata pcfdlrm sent it, including uploadDateTime.
        final String materialId = randomUUID().toString();
        sendMaterialAdded(createMaterialAddedPayload(materialId, caseId, defendantId, materialAdded.getString("documentCategory"), uploadDateTime));

        receiveMigratedCaseFileHelper.verifyMigratedCaseFileReceived(addMaterialHelper, submissionId);
        receiveMigratedCaseFileHelper.triggerAcceptMigratedCase(caseId);

        final JsonObject event = receiveMigratedCaseFileHelper.nextMaterialReadyForCourtDocument(addMaterialHelper).payloadAsJsonObject();
        assertThat(event.getString("materialId"), is(materialId));
        final JsonObject material = event.getJsonObject("courtDocument").getJsonArray("materials").getJsonObject(0);
        assertThat(ZonedDateTime.parse(material.getString("uploadDateTime")).toInstant(), is(libraUploadDateTime.toInstant()));

        receiveMigratedCaseFileHelper.verifyMigratedCaseAccepted(addMaterialHelper, submissionId, caseId, caseUrn);
    }

    private static String withLibraMaterials(final String payload) {
        final JsonObject receiveMigratedCaseFile = JsonObjects.createReader(new StringReader(payload)).readObject();
        final JsonArrayBuilder materials = JsonObjects.createArrayBuilder();
        int uploadIndex = 25;
        for (final String bundle : LIBRA_BUNDLE_LEVELS.keySet().stream().sorted().toList()) {
            materials.add(createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("azureLocation", "http://inazure/secrets/" + uploadIndex++)
                    .add("fileName", "bundle-" + bundle + ".pdf")
                    .add("fileType", bundle)
                    .add("documentType", 2));
        }
        return JsonObjects.createObjectBuilder(receiveMigratedCaseFile)
                .add("materials", materials)
                .build()
                .toString();
    }

    private void sendMaterialAdded(final JsonEnvelope materialAdded) {
        addMaterialHelper.sendMessage(materialAdded.metadata().name(), materialAdded);
    }

    @Test
    void shouldNotSetOffenceCustodyTimeLimitWhenCustodyStatusIsNotCustody() {

        final String submissionId = UUID.randomUUID().toString();

        final String caseId = UUID.randomUUID().toString();

        final String defendantId = UUID.randomUUID().toString();

        final String materialId = randomUUID().toString();

        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String offenceId = "550e8400-e29b-41d4-a716-446655440206";

        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-custody-status-not-c-with-ctl.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);

        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForSingleMaterial(addMaterialHelper, submissionId, new String[]{"23"}, "99");

        final JsonEnvelope materialAddedToMaterialContextPayload = createMaterialAddedPayload(materialId, caseId, defendantId);

        addMaterialHelper.sendMessage(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        receiveMigratedCaseFileHelper.verifyCourtProceedingsInitiatedWithoutOffenceCustodyTimeLimit(caseUrn, offenceId);
    }

    @Test
    void receiveMigratedCaseFileWhenListedDefendantsDoNotMatchHearing() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-matching-defendants.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyCaseProcessed(addMaterialHelper, submissionId, NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING);
    }

    // A future defendant date of birth is cleared, so the request sent to progression matches the same fixture.
    @ParameterizedTest
    @CsvSource({
            "command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-material.json, no-material, false",
            "command-json/pcfdlrm.command.receive-migrated-case-file-libra-journey.json, libra-journey, false",
            "command-json/pcfdlrm.command.receive-migrated-case-file-libra-indicated-plea.json, libra-indicated-plea, false",
            "command-json/pcfdlrm.command.receive-migrated-case-file-libra-indicated-guilty-plea.json, libra-indicated-guilty-plea, false",
            "command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-material.json, no-material, true",
            "command-json/pcfdlrm.command.receive-migrated-case-file-libra-journey.json, libra-journey, true"
    })
    void receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings(final String commandJson, final String expectedFixture,
                                                                        final boolean futureDefendantDateOfBirth) {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);
        final String staticPayLoad = getStringFromResource(commandJson)
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());
        final String payload = futureDefendantDateOfBirth ? withDefendantDateOfBirth(staticPayLoad, LocalDate.now().plusYears(1)) : staticPayLoad;
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(payload);
        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, expectedFixture);
    }

    private static String withDefendantDateOfBirth(final String payload, final LocalDate dateOfBirth) {
        final String dateOfBirthField = "\"dateOfBirth\": \"" + dateOfBirth + "\"";
        final String updated = payload.replace("\"selfDefinedInformation\": {", "\"selfDefinedInformation\": {" + dateOfBirthField + ",");
        assertThat(updated, containsString(dateOfBirthField));
        return updated;
    }

    @Test
    void receiveMigratedCaseFileWithAllHearingsSkipped() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String payload = getStringFromResource(
                "command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-all-hearings-skipped.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(payload);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(findAll(postRequestedFor(urlMatching(
                                "/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(), is(1)));

        assertThat(ReceiveMigratedCaseFileHelper.getLastLoggedRequest(caseUrn),
                not(containsString("listHearingRequests")));
    }

    @Test
    void receiveMigratedCaseFileWithXhibitFdHearingAndNoTimeDefaultsToTenAmLondon() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final LocalDate hearingDate = LocalDate.now().plusDays(1);

        final String payload = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-fd-no-time.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", RandomStringUtils.secure().nextAlphanumeric(10))
                .replace("HEARING_DATE", hearingDate.toString());

        final String expectedUtcTime = LocalDateTime.of(hearingDate, LocalTime.of(10, 0))
                .atZone(ZoneId.of("Europe/London"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(payload);

        receiveMigratedCaseFileHelper.verifyMigratedCaseFileReceivedWithDefaultedHearingTime(addMaterialHelper, expectedUtcTime);
    }

    @Test
    void shouldMarkMigratedCaseAsProcessedWhenAcceptingAPreviouslyReceivedCase() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String caseUrn = RandomStringUtils.secure().nextAlphanumeric(10);

        final String receivePayload = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-material.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(receivePayload);
        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, "no-material");

        receiveMigratedCaseFileHelper.triggerAcceptMigratedCase(caseId);

        receiveMigratedCaseFileHelper.verifyMigratedCaseAccepted(addMaterialHelper, submissionId, caseId, caseUrn);
    }

    @Test
    void shouldRaiseMigratedCaseNotFoundInAutomationWhenAcceptingAnUnknownCase() {
        final String caseId = UUID.randomUUID().toString();

        receiveMigratedCaseFileHelper.triggerAcceptMigratedCase(caseId);

        receiveMigratedCaseFileHelper.verifyMigratedCaseNotFoundInAutomation(addMaterialHelper);
    }

    private static void stubWireMocks() {
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    private JsonEnvelope createMaterialAddedPayload(final String materialId, final String caseId, final String defendantId) {
        return createMaterialAddedPayload(materialId, caseId, defendantId, "Case level");
    }

    private JsonEnvelope createMaterialAddedPayload(final String materialId, final String caseId, final String defendantId, final String documentCategory) {
        return createMaterialAddedPayload(materialId, caseId, defendantId, documentCategory, null);
    }

    private JsonEnvelope createMaterialAddedPayload(final String materialId, final String caseId, final String defendantId,
                                                    final String documentCategory, final String uploadDateTime) {

        final Metadata metadata = JsonEnvelope
                .metadataFrom(getCCMetadataJsonObject(caseId, defendantId, documentCategory, uploadDateTime))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("fileDetails", createObjectBuilder()
                        .add("fileName", FILE_NAME)
                        .add("alfrescoAssetId", randomUUID().toString())
                        .add("mimeType", PDF_MIME_TYPE)
                        .build())
                .add("materialAddedDate", "2019-09-17T07:54:37.539Z")
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonObject getCCMetadataJsonObject(final String caseId, final String defendantId, final String documentCategory,
                                                      final String uploadDateTime) {
        final JsonObjectBuilder ccMetadata = createObjectBuilder()
                .add("caseId", caseId)
                .add("defendantId", defendantId)
                .add("documentCategory", documentCategory)
                .add("documentTypeDescription", "SJPN")
                .add("documentTypeId", randomUUID().toString())
                .add("fileCloudLocation", "azure.net")
                .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString())
                .add("isCpsCase", false);
        if (uploadDateTime != null) {
            ccMetadata.add("uploadDateTime", uploadDateTime);
        }
        return JsonObjects
                .createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName("material.material-added")
                        .build().asJsonObject())
                .add("ccMetadata", ccMetadata)
                .build();
    }

}
