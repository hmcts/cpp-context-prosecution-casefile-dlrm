package uk.gov.moj.cpp.pcfdlrm.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.pcfdlrm.helper.QueueUtil.retrieveMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;

public class AddMaterialHelper extends AbstractTestHelper {

    public static final String UPLOAD_FILE_URL = "/material-service/command/api/rest/material/material";
    public static final String PDF_MIME_TYPE = "application/pdf";

    public void verifyUploadMaterialCalled(final String fileStoreId) {
        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .pollDelay(5, TimeUnit.SECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(UPLOAD_FILE_URL))
                                .withRequestBody(containing(fileStoreId))).size(),
                        CoreMatchers.is(1));
    }

    public JsonEnvelope verifyInMessagingQueue(JmsMessageConsumerClient jmsMessageConsumerClient) {
        final Optional<JsonEnvelope> message = retrieveMessage(jmsMessageConsumerClient);
        assertTrue(message.isPresent());
        return message.get();
    }

    public void verifyNotInMessagingQueue(JmsMessageConsumerClient jmsMessageConsumerClient) {
        final Optional<JsonEnvelope> message = retrieveMessage(jmsMessageConsumerClient);
        assertTrue(message.isEmpty());
    }



}

