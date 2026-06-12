package uk.gov.moj.cpp.pcfdlrm.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;

public class ProgressionStub {

    public static final String INITIATE_COURT_PROCEEDINGS_COMMAND = "/progression-service/command/api/rest/progression/initiatecourtproceedings";
    public static final String ADD_COURT_DOCUMENT_COMMAND = "/progression-service/command/api/rest/progression/courtdocument/";

    public static void stubForAddCourtDocument() {
        stubPingFor("progression-service");
        stubFor(post(urlPathMatching(ADD_COURT_DOCUMENT_COMMAND + "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON))
        );
    }

    public static void stubForInitiateCourtProceedings() {
        stubPingFor("progression-service");

        stubFor(post(urlPathEqualTo(INITIATE_COURT_PROCEEDINGS_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON))
        );
    }
}