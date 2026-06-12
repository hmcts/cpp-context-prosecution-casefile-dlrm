package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.APPLICATIONS;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.CASE_LEVEL;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.DEFENDANT_LEVEL;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMaterialToUploadCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMaterialUploadedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.MetadataHelper;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialEventProcessor.class);

    private static final String MATERIAL_COMMAND_UPLOAD_FILE = "material.command.upload-file";
    private static final String CASE_ID_FIELD = "caseId";
    private static final String DEFENDANT_ID_FIELD = "defendantId";
    private static final String DOCUMENT_TYPE_ID_FIELD = "documentTypeId";
    private static final String DOCUMENT_TYPE_DESCRIPTION_FIELD = "documentTypeDescription";
    private static final String DOCUMENT_CATEGORY_FIELD = "documentCategory";
    private static final String MATERIAL_ID_FIELD = "materialId";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String FILE_CLOUD_LOCATION = "fileCloudLocation";
    public static final String RECEIVED_DATE_TIME = "receivedDateTime";
    public static final String SECTION_CODE = "sectionCode";
    public static final String IS_CPS_CASE = "isCpsCase";
    private static final String APPLICATION_ID_FIELD = "applicationId";


    @Inject
    private Sender sender;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private MetadataHelper metadataHelper;


    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private PcfMaterialToUploadCounter pcfMaterialToUploadCounter;

    @Inject
    private PcfMaterialUploadedCounter pcfMaterialUploadedCounter;

    @Handles("pcfdlrm.events.material-added")
    public void handleMaterialAdded(final Envelope<MaterialAdded> materialAddedEvent) {
        uploadToMaterialContext(materialAddedEvent);
        pcfMaterialToUploadCounter.increment();
    }

    @Handles("material.material-added")
    public void handleMaterialAddedFromMaterialContext(final JsonEnvelope materialAddedEvent){
        LOGGER.info("material.material-added received and payload {} ", materialAddedEvent.payload());
        final Optional<JsonObject> ccMetadata = metadataHelper.getCCMetadata(materialAddedEvent);
        final String fileStoreId = metadataHelper.getFileStoreId(materialAddedEvent);

        final String fileCloudLocationId = ccMetadata.map(c -> c.getString(FILE_CLOUD_LOCATION, null)).orElse(null);

        final UUID materialId = fromString(materialAddedEvent.payloadAsJsonObject().getString(MATERIAL_ID_FIELD));

        if (ccMetadata.isPresent() && isNull(fileStoreId) && nonNull(fileCloudLocationId)) {
            LOGGER.info("ccMetadata {} ", ccMetadata);
            handleDocumentUploadedEvent(materialAddedEvent, ccMetadata.get(), materialId.toString(), fileCloudLocationId);
            pcfMaterialUploadedCounter.increment();
        }

    }

    private void uploadToMaterialContext(final Envelope<MaterialAdded> materialAddedEvent) {
        final MaterialAdded materialAdded = materialAddedEvent.payload();
        final JsonObjectBuilder ccMetadataBuilder = createObjectBuilder();

        ccMetadataBuilder.add(CASE_ID_FIELD, materialAdded.getCaseId().toString())
                .add(DOCUMENT_TYPE_ID_FIELD, Objects.requireNonNullElse(materialAdded.getDocumentTypeId(), "460f8154-c002-11e8-a355-529269fb1459"))
                .add(DOCUMENT_CATEGORY_FIELD, Objects.requireNonNullElse(materialAdded.getDocumentCategory(), "Case level"))
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, materialAdded.getDocumentType());

        ccMetadataBuilder.add(FILE_CLOUD_LOCATION, materialAddedEvent.payload().getMaterial().getFileCloudLocation());

        if (nonNull(materialAdded.getIsCpsCase())) {
            ccMetadataBuilder.add(IS_CPS_CASE, materialAdded.getIsCpsCase());
        }

        if (nonNull(materialAdded.getReceivedDateTime())) {
            ccMetadataBuilder.add(RECEIVED_DATE_TIME, materialAdded.getReceivedDateTime().toOffsetDateTime().toString());
        }

        if (DEFENDANT_LEVEL.toString().equalsIgnoreCase(materialAdded.getDocumentCategory())) {
            ccMetadataBuilder.add(DEFENDANT_ID_FIELD, materialAdded.getDefendantId().toString());
        }

        if (nonNull(materialAdded.getSectionCode())) {
            ccMetadataBuilder.add(SECTION_CODE, materialAdded.getSectionCode());
        }

        final JsonObject ccMetadata = ccMetadataBuilder.build();

        final JsonObjectBuilder uploadFilePayloadBuilder = buildUploadFilePayload(materialAdded);

        if (nonNull(materialAdded.getMaterial().getIsUnbundledDocument()) && materialAdded.getMaterial().getIsUnbundledDocument()) {
            uploadFilePayloadBuilder.add(IS_UNBUNDLED_DOCUMENT, true);
        }

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(materialAddedEvent.metadata()).withName(MATERIAL_COMMAND_UPLOAD_FILE).build(),
                ccMetadata,
                uploadFilePayloadBuilder.build(), null));
    }

    private JsonObjectBuilder buildUploadFilePayload(MaterialAdded materialAdded) {
        JsonObjectBuilder uploadFilePayloadBuilder = createObjectBuilder()
                .add(MATERIAL_ID_FIELD, UUID.randomUUID().toString());

        String fileCloudLocation = materialAdded.getMaterial().getFileCloudLocation();
        if (fileCloudLocation != null && !fileCloudLocation.isEmpty()) {
            uploadFilePayloadBuilder.add(FILE_CLOUD_LOCATION, fileCloudLocation);
        }

        return uploadFilePayloadBuilder;
    }

    private void handleDocumentUploadedEvent(final JsonEnvelope materialAddedEvent, final JsonObject ccMetadata, final String materialId, String fileCloudLocationId) {
        final String caseId = ccMetadata.getString(CASE_ID_FIELD, null);
        final String applicationId = ccMetadata.getString(APPLICATION_ID_FIELD, null);
        final String documentTypeId = ccMetadata.getString(DOCUMENT_TYPE_ID_FIELD);
        final String documentTypeDescription = ccMetadata.getString(DOCUMENT_TYPE_DESCRIPTION_FIELD);
        final String documentCategory = ccMetadata.getString(DOCUMENT_CATEGORY_FIELD);
        final String receivedDateTime = ccMetadata.containsKey(RECEIVED_DATE_TIME) ? ccMetadata.getString(RECEIVED_DATE_TIME) : null;

        LOGGER.info("handleDocumentUploadedEvent fileCloudLocationId {} ", fileCloudLocationId);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("courtDocumentId", randomUUID().toString())
                .add("name", materialAddedEvent.payloadAsJsonObject().getJsonObject("fileDetails").getString("fileName"))
                .add("mimeType", materialAddedEvent.payloadAsJsonObject().getJsonObject("fileDetails").getString("mimeType"))
                .add(DOCUMENT_TYPE_ID_FIELD, documentTypeId)
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, documentTypeDescription)
                .add("materials", buildMaterials(materialId, receivedDateTime))
                .add("containsFinancialMeans", false);


        createDocumentCategoryField(ccMetadata, caseId, applicationId, documentCategory, payloadBuilder);

        final JsonObjectBuilder progressionPayloadBuilder = createObjectBuilder().
                add("courtDocument", payloadBuilder.build())
                .add(MATERIAL_ID_FIELD, materialId);

        callAddCourtDocument(materialAddedEvent, fileCloudLocationId, progressionPayloadBuilder, applicationId);
    }

    private void createDocumentCategoryField(final JsonObject ccMetadata, final String caseId, final String applicationId, final String documentCategory, final JsonObjectBuilder payloadBuilder) {
        if (DEFENDANT_LEVEL.toString().equalsIgnoreCase(documentCategory)) {
            final String defendantId = ccMetadata.getString(DEFENDANT_ID_FIELD);
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("defendantDocument", createObjectBuilder()
                            .add("prosecutionCaseId", caseId)
                            .add("defendants", createArrayBuilder()
                                    .add(defendantId)
                                    .build())
                            .build())
                    .build());
        } else if (CASE_LEVEL.toString().equalsIgnoreCase(documentCategory)) {
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("caseDocument", createObjectBuilder()
                            .add("prosecutionCaseId", caseId)
                            .build())
                    .build());
        } else if (APPLICATIONS.toString().equalsIgnoreCase(documentCategory)) {
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("applicationDocument", createObjectBuilder()
                            .add(APPLICATION_ID_FIELD, applicationId)
                            .build())
                    .build());
        }
    }

    private void callAddCourtDocument(final JsonEnvelope materialAddedEvent, String fileCloudLocationId, final JsonObjectBuilder progressionPayloadBuilder, final String applicationId) {
        LOGGER.info("callAddCourtDocument  fileCloudLocationId is {} ", fileCloudLocationId);
        progressionPayloadBuilder.add(FILE_CLOUD_LOCATION, fileCloudLocationId);

        if (isNull(applicationId)) {
            final Metadata metadata = Envelope.metadataFrom(materialAddedEvent.metadata())
                    .withName("pcfdlrm.command.add-case-court-document")
                    .build();
            final JsonObject progressionPayload = progressionPayloadBuilder.build();
            LOGGER.info("callAddCourtDocument progressionPayload {} ", progressionPayload);

            sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelopeFrom(metadata, progressionPayload)));
        }
    }

    private JsonArray buildMaterials(final String materialId, final String receivedDateTime) {
        JsonObjectBuilder materialBuilder = createObjectBuilder().add("id", materialId);
        if (receivedDateTime != null) {
            materialBuilder = materialBuilder.add(RECEIVED_DATE_TIME, receivedDateTime);
        }
        return createArrayBuilder().add(materialBuilder.build())
                .build();
    }

}

