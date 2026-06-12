package uk.gov.moj.cpp.pcfdlrm.helper;

import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.pcfdlrm.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.pcfdlrm.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.pcfdlrm.helper.StubUtil.setupUserAsSystemUser;
import static uk.gov.moj.cpp.pcfdlrm.stub.MaterialStub.stubForUploadFileCommand;
import static uk.gov.moj.cpp.pcfdlrm.stub.ProgressionStub.stubForAddCourtDocument;
import static uk.gov.moj.cpp.pcfdlrm.stub.ProgressionStub.stubForInitiateCourtProceedings;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCode;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeList;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubApplicationTypes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubFirstHearingApplicationType;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetAlcoholLevelMethods;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetAllCountryNationalities;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetAllCourtLocations;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetAllParentBundleSections;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetBailStatuses;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetCustodyStatuses;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetEnforcementArea;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetHearingTypes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetInitiationTypes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetObservedEthnicities;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetOffenceDateCodes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetOffenderCodes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetParentBundleSections;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetPoliceForces;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetProsecutor;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetSelfDefinedEthnicities;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetSummonsCodes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubGetVehicleCodes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubPleaTypes;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubPoliceRanks;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubProsecutors;
import static uk.gov.moj.cpp.pcfdlrm.stub.ReferenceDataStub.stubVerdictTypes;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import javax.ws.rs.core.Response.Status;

/**
 * Provides helper methods for tests to interact with Wiremock instance
 */
public class WiremockTestHelper {

    private WiremockTestHelper() {
    }

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void createCommonMockEndpoints() {
        resetStubs();
        setupUserAsSystemUser(USER_ID.toString());
        stubGetInitiationTypes();
        stubGetAllParentBundleSections();
        stubForAddCourtDocument();
        stubGetAllCourtLocations();
        stubGetAllCountryNationalities();
        stubGetOrganisationUnitWithOneCourtroom();
        stubOffencesForOffenceCode();
        stubOffencesForOffenceCodeList();
        stubGetProsecutor();
        stubGetSummonsCodes();
        stubGetAlcoholLevelMethods();
        stubGetOffenceDateCodes();
        stubGetOffenderCodes();
        stubProsecutors();
        stubPoliceRanks();
        stubGetBailStatuses();
        stubGetVehicleCodes();
        stubGetParentBundleSections();
        stubModeOfTrialReasons();
        stubGetPoliceForces();
        stubApplicationTypes();
        stubFirstHearingApplicationType();
        stubForUploadFileCommand();
        stubGetSelfDefinedEthnicities();
        stubGetObservedEthnicities();
        stubGetHearingTypes();
        stubGetCustodyStatuses();
        stubGetOrganisationUnits();
        stubPleaTypes();
        stubVerdictTypes();
        stubGetEnforcementArea();
        stubForInitiateCourtProceedings();
    }

    public static void waitForStubToBeReady(String resource, String mediaType) {
        waitForStubToBeReady(resource, mediaType, OK);
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .until(
                        status().is(expectedStatus)
                );
    }
}
