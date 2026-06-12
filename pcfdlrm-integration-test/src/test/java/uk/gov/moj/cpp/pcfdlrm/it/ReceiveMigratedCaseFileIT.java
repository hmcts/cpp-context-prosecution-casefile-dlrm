package uk.gov.moj.cpp.pcfdlrm.it;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.openejb.config.QuickJarsTxtParser.FILE_NAME;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ReceiveMigratedCaseFileIT {

    private static final String NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING = "No matching defendants with hearings found for the hearing";

    private final ReceiveMigratedCaseFileHelper receiveMigratedCaseFileHelper = new ReceiveMigratedCaseFileHelper();
    private final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

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

    private static void stubWireMocks() {
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    @ParameterizedTest
    @CsvSource({"true, 15", "false, 16"})
    void shouldReceiveMigratedCaseFileWithRetrialIndicator(boolean retrialIndicator, String index) {
        final String submissionId = UUID.randomUUID().toString();

        final String caseId = UUID.randomUUID().toString();

        final String defendantId = UUID.randomUUID().toString();

        final String materialId = randomUUID().toString();

        final String caseUrn = randomAlphanumeric(10);

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

        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, retrialIndicator,0);
    }

    @ParameterizedTest
    @CsvSource({"command-json/pcfdlrm.command.receive-migrated-case-file-with-no-allocation-decision.json, 20, b8c37e33-defd-351c-b91e-1e03e51657da",
                "command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json, 21, b8c37e33-defd-351c-b91e-1e03e51657da",
                "command-json/pcfdlrm.command.receive-migrated-case-file-with-indictable-allocation-decision.json, 19, 5aaecac5-222b-402d-9047-84803679edac"
    })
    void shouldSetAllocationDecisionForDifferentScenario(String fileName, String index, String motReasonId) {

        final String submissionId = UUID.randomUUID().toString();

        final String caseId = UUID.randomUUID().toString();

        final String defendantId = UUID.randomUUID().toString();

        final String materialId = randomUUID().toString();

        final String caseUrn = randomAlphanumeric(10);

        final String staticPayLoad = getStringFromResource(fileName)
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("DEFENDANT_ID", defendantId)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());

        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);

        receiveMigratedCaseFileHelper.verifyReceiveMigratedCaseFileForSingleMaterial(addMaterialHelper, submissionId, new String[]{ index }, "99");

        final JsonEnvelope materialAddedToMaterialContextPayload = createMaterialAddedPayload(materialId, caseId, defendantId);

        addMaterialHelper.sendMessage(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiatedWithAllocationDecision(caseUrn, motReasonId);
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

    @Test
    void receiveMigratedCaseFileWithoutMaterial() {
        final String submissionId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String caseUrn = randomAlphanumeric(10);
        final String staticPayLoad = getStringFromResource("command-json/pcfdlrm.command.receive-migrated-case-file-xhibit-no-material.json")
                .replace("SUBMISSION_ID", submissionId)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("HEARING_DATE", LocalDate.now().plusDays(1).toString());
        receiveMigratedCaseFileHelper.receiveMigratedCaseFile(staticPayLoad);
        receiveMigratedCaseFileHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, false, 3);
    }


    private JsonEnvelope createMaterialAddedPayload(final String materialId, final String caseId, final String defendantId) {

        final Metadata metadata = JsonEnvelope
                .metadataFrom(getCCMetadataJsonObjectCaseLevel(caseId, defendantId))
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

    private static JsonObject getCCMetadataJsonObjectCaseLevel(final String caseId, final String defendantId) {
        return JsonObjects
                .createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName("material.material-added")
                        .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("caseId", caseId)
                        .add("defendantId", defendantId)
                        .add("documentCategory", "Case level")
                        .add("documentTypeDescription", "SJPN")
                        .add("documentTypeId", randomUUID().toString())
                        .add("fileCloudLocation", "azure.net")
                        .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString())
                        .add("isCpsCase", false))
                .build();
    }
}
