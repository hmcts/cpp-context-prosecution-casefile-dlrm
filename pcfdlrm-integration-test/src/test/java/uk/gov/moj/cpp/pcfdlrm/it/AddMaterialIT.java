package uk.gov.moj.cpp.pcfdlrm.it;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.openejb.config.QuickJarsTxtParser.FILE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.pcfdlrm.helper.AbstractTestHelper.CONTEXT_NAME;
import static uk.gov.moj.cpp.pcfdlrm.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.pcfdlrm.helper.EventSelector.MATERIAL_ADDED_PENDING_PROCESS_EVENT;
import static uk.gov.moj.cpp.pcfdlrm.stub.ProgressionStub.stubForAddCourtDocument;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.helper.AddMaterialHelper;

import java.time.ZonedDateTime;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AddMaterialIT {

    private final static String SJPN_DOCUMENT_TYPE = "SJPN";
    private static final String MATERIAL_ADDED_IN_MATERIAL_CONTEXT = "material.material-added";
    private final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

    final JmsMessageConsumerClient materialAddedPendingProcessConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(MATERIAL_ADDED_PENDING_PROCESS_EVENT).getMessageConsumerClient();


    @BeforeAll
    static void setup() {
        stubForAddCourtDocument();
    }

    @Test
    void shouldConsumePublicMaterialAddedEvent() {
        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPCaseLevel(materialId, false);

        addMaterialHelper.sendMessage(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        final JsonEnvelope envelope = addMaterialHelper.verifyInMessagingQueue(materialAddedPendingProcessConsumer);

        assertThat(envelope, jsonEnvelope(metadata().withName(MATERIAL_ADDED_PENDING_PROCESS_EVENT), payload().isJson(allOf(
                withJsonPath("materialId", is(materialId)),
                withJsonPath("courtDocument.name", is("jars.txt")))
        )));
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJPCaseLevel(final String materialId, Boolean isCpsCase) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectCaseLevel(isCpsCase))
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

    private static JsonObject getCCMetadataJsonObjectCaseLevel(Boolean isCpsCase) {
        JsonObjectBuilder ccMetadataBuilder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeDescription", SJPN_DOCUMENT_TYPE)
                .add("documentTypeId", randomUUID().toString())
                .add("fileCloudLocation", "azure.net")
                .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString());
        if (isCpsCase != null) {
            ccMetadataBuilder.add("isCpsCase", isCpsCase);
        }
        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", ccMetadataBuilder)
                .build();
    }
}
