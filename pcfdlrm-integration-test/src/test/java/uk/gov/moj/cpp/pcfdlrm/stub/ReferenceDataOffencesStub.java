package uk.gov.moj.cpp.pcfdlrm.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.pcfdlrm.helper.FileUtil.resourceToString;
import static uk.gov.moj.cpp.pcfdlrm.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.pcfdlrm.helper.StubUtil;

import java.util.UUID;

public class ReferenceDataOffencesStub extends StubUtil {

    private static final String REFERENCE_DATA_ACTION_QUERY_OFFENCES_URL = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences/all-versions";
    private static final String REFERENCE_DATA_ACTION_QUERY_OFFENCES_ALL_VERSIONS_MEDIA_TYPE = "application/vnd.referencedataoffences.query.offences-all-versions+json";
    private static final String REFERENCE_DATA_ACTION_QUERY_OFFENCES_LIST_URL = "/referencedataoffences-service/query/api/rest/referencedataoffences/offences";
    private static final String REFERENCE_DATA_ACTION_QUERY_OFFENCES_MEDIA_TYPE = "application/vnd.referencedataoffences.offences-list+json";


    public static void stubOffencesForOffenceCode() {
        stubOffencesForOffenceCode("stub-data/referencedataoffences.offences.json");
    }

    public static void stubOffencesForOffenceCodeList() {
        stubOffencesForOffenceCodeList("stub-data/referencedataoffences.offences-list.json");
    }

    private static void stubOffencesForOffenceCode(final String referenceDataOffencesStubFile) {
        InternalEndpointMockUtils.stubPingFor("referencedataoffences-service");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_QUERY_OFFENCES_URL))
                .withQueryParam("cjsoffencecodes", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_QUERY_OFFENCES_ALL_VERSIONS_MEDIA_TYPE)
                        .withBody(resourceToString(referenceDataOffencesStubFile))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_QUERY_OFFENCES_URL + "?cjsoffencecodes=CA03013&date=2018-09-10", REFERENCE_DATA_ACTION_QUERY_OFFENCES_ALL_VERSIONS_MEDIA_TYPE);
    }

    private static void stubOffencesForOffenceCodeList(final String referenceDataOffencesStubFile) {
        InternalEndpointMockUtils.stubPingFor("referencedataoffences-service");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_QUERY_OFFENCES_LIST_URL))
                .withQueryParam("cjsoffencecode", matching(".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_QUERY_OFFENCES_MEDIA_TYPE)
                        .withBody(resourceToString(referenceDataOffencesStubFile))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_QUERY_OFFENCES_LIST_URL + "?cjsoffencecode=CA03013", REFERENCE_DATA_ACTION_QUERY_OFFENCES_MEDIA_TYPE);
    }

}
