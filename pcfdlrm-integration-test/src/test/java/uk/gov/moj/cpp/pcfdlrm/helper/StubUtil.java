package uk.gov.moj.cpp.pcfdlrm.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.pcfdlrm.helper.FileUtil.resourceToString;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import javax.ws.rs.core.MediaType;

public class StubUtil {

    protected static final String DEFAULT_JSON_CONTENT_TYPE = MediaType.APPLICATION_JSON;
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void resetStubs() {
        reset();
        stubPingFor("usersgroups-service");
    }

    public static void setupUserAsSystemUser(String userId) {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/" + userId + "/groups"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)
                        .withBody(resourceToString("stub-data/usersgroups.get-groups-by-user.json"))));
    }
}


