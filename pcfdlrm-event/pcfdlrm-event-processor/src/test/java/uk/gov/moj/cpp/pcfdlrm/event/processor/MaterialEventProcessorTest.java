package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.MaterialEventProcessor.IS_CPS_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Material.material;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded.materialAdded;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMaterialToUploadCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMaterialUploadedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.MetadataHelper;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Material;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaterialEventProcessorTest {
    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;

    @Mock
    private MetadataHelper metadataHelper;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Metadata> metadataArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Mock
    private Sender sender;

    @Mock
    private PcfMaterialToUploadCounter pcfMaterialToUploadCounter;

    @Mock
    private PcfMaterialUploadedCounter pcfMaterialUploadedCounter;

    private static final String UPLOAD_FILE_TO_MATERIAL_CONTEXT = "material.command.upload-file";
    private static final String MATERIAL_ADDED_IN_MATERIAL_CONTEXT = "material.material-added";
    private static final String FILE_NAME = "File name";
    final String fileCloudLocation = "FileCloudLocation";

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventForNonSJPFileCloudLocation() {
        final boolean isUnbundledDocument = true;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEvent(null, isUnbundledDocument, null, randomAlphanumeric(20));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, false, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventForCpsCase() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEvent(true, isUnbundledDocument, null, randomAlphanumeric(20));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, true, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventForDefendantLevelDocument() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEvent(null, isUnbundledDocument, null, randomAlphanumeric(20));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, false, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventWithReceivedDateTime() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventWithReceivedDateTime(null, isUnbundledDocument, null, randomAlphanumeric(20));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, false, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventWithSectionCode() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventWithSectionCode(null, isUnbundledDocument, null, randomAlphanumeric(20));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, false, isUnbundledDocument);
    }

    @Test
    void shouldHandleMaterialAddedPublicEvent() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, fileCloudLocation);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectCaseLevel(null, fileCloudLocation).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(fileCloudLocation);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(null);

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleAddDocumentCommand(envelope, ccMetadataJsonObject, null, null, fileCloudLocation);
        verify(pcfMaterialUploadedCounter).increment();
    }

    @Test
    void shouldNotHandleMaterialAddedPublicEventWhenFileStoreIdExists() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, fileCloudLocation);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectCaseLevel(null, fileCloudLocation).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(fileCloudLocation);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn("fileStoreId");

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verify(sender, Mockito.never()).sendAsAdmin(any());
        verify(pcfMaterialUploadedCounter, never()).increment();
    }

    @Test
    void shouldNotHandleMaterialAddedPublicEventWhenFileCloudLocationIsNull() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, null);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectCaseLevel(null, null).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(null);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(null);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verify(sender, Mockito.never()).sendAsAdmin(any());
        verify(pcfMaterialUploadedCounter, never()).increment();
    }

    @Test
    void shouldNotHandleMaterialAddedPublicEventWhenCCMetadataIsEmpty() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, "fileCloudLocation");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.empty());

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verify(sender, Mockito.never()).sendAsAdmin(any());
        verify(pcfMaterialUploadedCounter, never()).increment();
    }

    @Test
    void shouldHandleMaterialAddedPublicEventForDefendantLevelDocument() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, fileCloudLocation);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectDefendantLevel(null).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(fileCloudLocation);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(null);

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleAddDocumentCommandForDefendantLevel(envelope, ccMetadataJsonObject, null, null, fileCloudLocation);
        verify(pcfMaterialUploadedCounter).increment();
    }

    @Test
    void shouldHandleMaterialAddedPublicEventForApplicationDocument() {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, fileCloudLocation);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectApplicationLevel(null, null).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(fileCloudLocation);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(null);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        // Application documents with applicationId are not processed by the current implementation
        verify(sender, Mockito.never()).sendAsAdmin(any());
        verify(pcfMaterialUploadedCounter).increment();
    }

    @Test
    void shouldHandleMaterialAddedPublicEventWithReceivedDateTime() {
        final String receivedDateTime = now().toString();
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, fileCloudLocation);
        final JsonObject ccMetadataJsonObject = Mockito.spy(getCCMetadataJsonObjectWithReceivedDateTime(null,  receivedDateTime).getJsonObject("ccMetadata"));

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(ccMetadataJsonObject.getString("fileCloudLocation", null)).thenReturn(fileCloudLocation);
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(null);

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleAddDocumentCommandWithReceivedDateTime(envelope, ccMetadataJsonObject, null, null, fileCloudLocation, receivedDateTime);
        verify(pcfMaterialUploadedCounter).increment();
    }


    private static Envelope<MaterialAdded> createMaterialAddedEvent(final Boolean isCpsCase, final boolean isUnbundledDocument, UUID fileStoreId, String fileCloudLocation) {
        MaterialAdded.Builder builder = materialAdded()
                .withCaseId(randomUUID())
                .withMaterial(randomMaterial(null, isUnbundledDocument, fileStoreId, fileCloudLocation))
                .withCaseType("CC")
                .withDocumentCategory("Defendant level")
                .withDocumentTypeId(randomUUID().toString())
                .withDocumentType("DocumentType")
                .withProsecutorDefendantId(randomUUID().toString())
                .withDefendantId(randomUUID())
                .withReceivedDateTime(now())
                .withSectionCode("IPDC");

        if (fileCloudLocation != null) {
            builder.withDocumentTypeId("5a945033-f9dd-43d2-9e9e-3042349940da");
        }

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("pcfdlrm.events.material-added"),
                builder.build());
    }

    private static Envelope<MaterialAdded> createMaterialAddedEventWithReceivedDateTime(final Boolean isCpsCase, final boolean isUnbundledDocument, UUID fileStoreId, String fileCloudLocation) {
        MaterialAdded.Builder builder = materialAdded()
                .withCaseId(randomUUID())
                .withMaterial(randomMaterial(null, isUnbundledDocument, fileStoreId, fileCloudLocation))
                .withCaseType("CC")
                .withDocumentCategory("Case level")
                .withDocumentTypeId(randomUUID().toString())
                .withDocumentType("DocumentType")
                .withProsecutorDefendantId(randomUUID().toString())
                .withDefendantId(randomUUID())
                .withReceivedDateTime(now())
                .withSectionCode("IPDC");

        if (fileCloudLocation != null) {
            builder.withDocumentTypeId("5a945033-f9dd-43d2-9e9e-3042349940da");
        }

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("pcfdlrm.events.material-added"),
                builder.build());
    }

    private static Envelope<MaterialAdded> createMaterialAddedEventWithSectionCode(final Boolean isCpsCase, final boolean isUnbundledDocument, UUID fileStoreId, String fileCloudLocation) {
        MaterialAdded.Builder builder = materialAdded()
                .withCaseId(randomUUID())
                .withMaterial(randomMaterial(null, isUnbundledDocument, fileStoreId, fileCloudLocation))
                .withCaseType("CC")
                .withDocumentCategory("Case level")
                .withDocumentTypeId(randomUUID().toString())
                .withDocumentType("DocumentType")
                .withProsecutorDefendantId(randomUUID().toString())
                .withDefendantId(randomUUID())
                .withReceivedDateTime(now())
                .withSectionCode("IPDC");

        if (fileCloudLocation != null) {
            builder.withDocumentTypeId("5a945033-f9dd-43d2-9e9e-3042349940da");
        }

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("pcfdlrm.events.material-added"),
                builder.build());
    }

    private static Material randomMaterial(final String documentType, final boolean isUnbundledDocument, UUID fileStoreId, String fileCloudLocation) {

        Material.Builder materialBuilder = material()
                .withDocumentType(nonNull(documentType) ? documentType : randomAlphanumeric(5))
                .withFileType(randomAlphanumeric(5))
                .withIsUnbundledDocument(isUnbundledDocument);

        if (fileStoreId != null) {
            materialBuilder.withFileStoreId(randomUUID());
        } else if (fileCloudLocation != null) {
            materialBuilder
                    .withFileCloudLocation(fileCloudLocation)
                    .withDocumentType(nonNull(documentType) ? documentType : "5a945033-f9dd-43d2-9e9e-3042349940da");

        }

        return materialBuilder.build();
    }

    private void verifyUploadMaterialWhenMaterialAddedEventForNonSJP(Envelope<MaterialAdded> materialAddedEvent, JsonEnvelope jsonEnvelope, boolean withIsCpsCase, final boolean isUnbundledDocument) {

        verify(metadataHelper).envelopeWithCustomMetadata(
                metadataArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                eq(null));

        final String commandName = metadataArgumentCaptor.getValue().name();
        final JsonObject ccMetadata = jsonObjectArgumentCaptor.getAllValues().get(0);
        final JsonObject materialPayload = jsonObjectArgumentCaptor.getAllValues().get(1);

        assertEquals(UPLOAD_FILE_TO_MATERIAL_CONTEXT, commandName);

        assertTrue(materialPayload.containsKey("materialId"));

        if (null != materialAddedEvent.payload().getMaterial().getFileStoreId()) {
            assertEquals(materialPayload.getString("fileServiceId"), materialAddedEvent.payload().getMaterial().getFileStoreId().toString());
        }

        if (null != materialAddedEvent.payload().getMaterial().getFileCloudLocation()) {
            assertEquals(materialPayload.getString("fileCloudLocation"), materialAddedEvent.payload().getMaterial().getFileCloudLocation());
        }

        if (isUnbundledDocument) {
            assertTrue(materialPayload.getBoolean("isUnbundledDocument"));
        } else {
            assertNull(materialPayload.get("isUnbundledDocument"));
        }

        assertAll(
                () -> assertEquals(ccMetadata.getString("caseId"), materialAddedEvent.payload().getCaseId().toString()),
                () -> assertEquals(ccMetadata.getString("documentTypeId"), materialAddedEvent.payload().getDocumentTypeId()),
                () -> assertEquals(ccMetadata.getString("documentTypeDescription"), materialAddedEvent.payload().getDocumentType()),
                () -> assertEquals(ccMetadata.getString("documentCategory"), materialAddedEvent.payload().getDocumentCategory())
        );


        // Only check defendantId if document category is defendant level
        if ("Defendant level".equalsIgnoreCase(materialAddedEvent.payload().getDocumentCategory())) {
            assertEquals(ccMetadata.getString("defendantId"), materialAddedEvent.payload().getDefendantId().toString());
        }

        if (withIsCpsCase) {
            assertEquals(ccMetadata.getBoolean("isCpsCase"), materialAddedEvent.payload().getIsCpsCase());
        }
        verify(sender).send(jsonEnvelope);
        verify(pcfMaterialToUploadCounter).increment();
    }

    private void verifyHandleAddDocumentCommand(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase, String fileStoreId, String fileCloudLocation) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }

        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName("pcfdlrm.command.add-case-court-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadataJsonObject.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.fileCloudLocation", equalTo(fileCloudLocation)),
                        isCpsCaseMatcher
                )))));
    }

    private void verifyHandleAddDocumentCommandForDefendantLevel(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase, String fileStoreId, String fileCloudLocation) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }

        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName("pcfdlrm.command.add-case-court-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadataJsonObject.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadataJsonObject.getString("defendantId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.fileCloudLocation", equalTo(fileCloudLocation)),
                        isCpsCaseMatcher
                )))));
    }



    private void verifyHandleAddDocumentCommandWithReceivedDateTime(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase, String fileStoreId, String fileCloudLocation, String receivedDateTime) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }

        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName("pcfdlrm.command.add-case-court-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadataJsonObject.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.materials[0].receivedDateTime", equalTo(receivedDateTime)),
                        withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.fileCloudLocation", equalTo(fileCloudLocation)),
                        isCpsCaseMatcher
                )))));
    }

    private static JsonEnvelope createDocumentAddedV2PayloadForNonSJP(Boolean isCpsCase, String fileCloudLocation) {
        final Metadata metadata;

        metadata = JsonEnvelope.metadataFrom(
                        getCCMetadataJsonObjectCaseLevel(isCpsCase, fileCloudLocation))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", randomUUID().toString())
                .add("fileDetails", createObjectBuilder().add("fileName", FILE_NAME).add("mimeType", "application/pdf"))
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonObject getCCMetadataJsonObjectCaseLevel(Boolean isCpsCase, String fileStoreId) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", now().toString())
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        JsonObjectBuilder resultBuilder = JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder);

        if (fileStoreId != null) {
            resultBuilder.add("fileStoreId", fileStoreId);
        }

        return resultBuilder.build();
    }

    private static JsonObject getCCMetadataJsonObjectDefendantLevel(Boolean isCpsCase) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Defendant level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", now().toString())
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder)
                .build();
    }

    private static JsonObject getCCMetadataJsonObjectApplicationLevel(Boolean isCpsCase,String applicationId) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Application level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", now().toString())
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        JsonObjectBuilder resultBuilder = JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder);

        if (applicationId != null) {
            resultBuilder.add("applicationId", applicationId);
        }

        return resultBuilder.build();
    }

    private static JsonObject getCCMetadataJsonObjectWithReceivedDateTime(Boolean isCpsCase,String receivedDateTime) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", receivedDateTime)
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder)
                .build();
    }

}