package uk.gov.moj.cpp.pcfdlrm.event.processor.utils;


import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


public class MetadataHelper {

    private static final String IDPC_ID = "idpcId";
    private static final String CC_METADATA = "ccMetadata";
    private static final String CC_FILE_STORE_ID = "fileStoreId";

    private MetadataHelper() {
    }

    public static Metadata metadataWithIdpcProcessId(final Metadata metadata, final String processId) {
        return JsonEnvelope.metadataFrom(
                        createObjectBuilder(metadata.asJsonObject())
                                .add(IDPC_ID, processId)
                                .build())
                .build();
    }

    public JsonEnvelope envelopeWithCustomMetadata(final Metadata originalMetadata, final JsonObject ccMetadata, final JsonObject payload, final String fileServiceId) {
        final Metadata enrichedMetadata = enrichMetadata(originalMetadata, ccMetadata, fileServiceId);
        final JsonObject enrichedPayload = payloadWithMetadata(payload, enrichedMetadata);
        return envelopeFrom(enrichedMetadata, enrichedPayload);
    }

    private Metadata enrichMetadata(final Metadata metadata, final JsonObject ccMetadata, final String fileServiceId) {
        final JsonObjectBuilder builder = createObjectBuilder(metadata.asJsonObject())
                .add(CC_METADATA, ccMetadata);
        ofNullable(fileServiceId).ifPresent(s -> builder.add(CC_FILE_STORE_ID, fileServiceId));
        return JsonEnvelope.metadataFrom(builder.build())
                .build();
    }

    private static JsonObject payloadWithMetadata(final JsonObject payload, final Metadata metadata) {

        final JsonObjectBuilder updatedMetadataBuilder = JsonObjects.createObjectBuilderWithFilter(metadata.asJsonObject(), key -> !JsonMetadata.CAUSATION.equals(key));

        return createObjectBuilder(payload)
                .add(JsonEnvelope.METADATA, updatedMetadataBuilder)
                .build();
    }

    public static String metadataToString(final Metadata metadata) {
        return metadata.asJsonObject().toString();
    }

    public Optional<JsonObject> getCCMetadata(final JsonEnvelope envelope) {
        return ofNullable(envelope.metadata().asJsonObject().getJsonObject(CC_METADATA));
    }

    public String getFileStoreId(final JsonEnvelope envelope) {
        return envelope.metadata().asJsonObject().getString(CC_FILE_STORE_ID, null);
    }

}
