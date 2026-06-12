package uk.gov.moj.cpp.pcfdlrm.helper;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("WeakerAccess")
@ExtendWith(JmsResourceManagementExtension.class)
public abstract class AbstractTestHelper {

    public static final String CONTEXT_NAME = "pcfdlrm";

    protected static final UUID USER_ID = fromString("974cfe1d-64a5-4243-be8e-4ff8731ea4b1");
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String WRITE_BASE_URL = "/pcfdlrm-command-api/command/api/rest/pcfdlrm";
    private static final long RETRIEVE_TIMEOUT = 20000;
    private static final String READ_BASE_URL = "/pcfdlrm-service/query/api/rest/pcfdlrm";

    private final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider()
            .getMessageProducerClient();

    protected final RestClient restClient = new RestClient();
    private final Map<String, JmsMessageConsumerClient> messageConsumerClientMap = new HashMap<>();

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    static {
        doAllStubbing();
    }

    public static void doAllStubbing() {
    }

    protected void makePostCall(String url, String mediaType, String payload) {
        makePostCall(USER_ID, url, mediaType, payload);
    }

    protected void makePostCall(UUID userId, String url, String mediaType, String payload) {
        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(HeaderConstants.USER_ID, userId)
                .build();
        Response response = restClient.postCommand(requestParams.getUrl(), requestParams.getMediaType(), payload, requestParams.getHeaders());
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    public Optional<JsonEnvelope> retrieveEvent(final String eventName) {
        if (!messageConsumerClientMap.containsKey(eventName)) {
            return Optional.empty();
        }

        return messageConsumerClientMap.get(eventName).retrieveMessageAsJsonEnvelope(RETRIEVE_TIMEOUT);
    }

    public void createPrivateConsumerForMultipleSelectors(final String... eventNames) {
        Arrays.stream(eventNames).forEach(this::createPrivateConsumer);
    }

    public JmsMessageConsumerClient createPrivateConsumer(final String eventSelector) {
        if (!messageConsumerClientMap.containsKey(eventSelector)) {
            messageConsumerClientMap.put(eventSelector, newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                    .withEventNames(eventSelector)
                    .getMessageConsumerClient());
        }

        return messageConsumerClientMap.get(eventSelector);
    }

    public void createPublicConsumerForMultipleSelectors(final String... eventNames) {
        Arrays.stream(eventNames).forEach(this::createPublicConsumer);
    }

    public void createPublicConsumer(final String eventSelector) {
        if (!messageConsumerClientMap.containsKey(eventSelector)) {
            messageConsumerClientMap.put(eventSelector, newPublicJmsMessageConsumerClientProvider()
                    .withEventNames(eventSelector)
                    .getMessageConsumerClient());
        }

        messageConsumerClientMap.get(eventSelector);
    }

    public void sendMessage(final String name, final JsonEnvelope envelope) {
        publicMessageProducerClient.sendMessage(name, envelope);
    }
}

