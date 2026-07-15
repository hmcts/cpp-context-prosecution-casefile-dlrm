package uk.gov.moj.cpp.pcfdlrm.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.pcfdlrm.helper.EventSelector.MATERIAL_ADDED_EVENT;
import static uk.gov.moj.cpp.pcfdlrm.helper.EventSelector.MIGRATED_CASE_FILE_PROCESSED;
import static uk.gov.moj.cpp.pcfdlrm.helper.EventSelector.MIGRATED_CASE_FILE_RECEIVED;
import static uk.gov.moj.cpp.pcfdlrm.helper.EventSelector.MIGRATED_CASE_VALIDATED_CREATION_PENDING;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class ReceiveMigratedCaseFileHelper extends AbstractTestHelper {

    private static final String PROGRESSION_INITIATE_COURT_PROCEEDINGS = "progression.initiate-court-proceedings";
    private final JmsMessageConsumerClient materialAddedEventProcessConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(MATERIAL_ADDED_EVENT).getMessageConsumerClient();
    private final JmsMessageConsumerClient migratedCaseValidatedCreationPendingConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(MIGRATED_CASE_VALIDATED_CREATION_PENDING).getMessageConsumerClient();
    private final JmsMessageConsumerClient migratedCaseFileProcessedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(MIGRATED_CASE_FILE_PROCESSED).getMessageConsumerClient();
    private final JmsMessageConsumerClient migratedCaseFileReceivedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(MIGRATED_CASE_FILE_RECEIVED).getMessageConsumerClient();

    public void receiveMigratedCaseFile(final String payload) {
        makePostCall(getWriteUrl("/receive-migrated-case-file"),
                "application/vnd.pcfdlrm.receive-migrated-case-file+json",
                payload);
    }

    public void verifyCaseProcessed(AddMaterialHelper addMaterialHelper, String submissionId, String description) {
        await()
                .untilAsserted(() -> {
                    final JsonEnvelope materialAddedEnvelop = addMaterialHelper.verifyInMessagingQueue(migratedCaseFileProcessedConsumer);

                    assertThat(materialAddedEnvelop, jsonEnvelope(metadata().withName(MIGRATED_CASE_FILE_PROCESSED), payload().isJson(allOf(
                            withJsonPath("description", is(description)),
                            withJsonPath("submissionId", is(submissionId))
                    ))));
                });
    }

    public void verifyReceiveMigratedCaseFileForSingleMaterial(AddMaterialHelper addMaterialHelper, String submissionId, String[] filepathIndexOne, final String fileType) {

        await()
                .untilAsserted(() -> {
                    final JsonEnvelope materialAddedEnvelop = addMaterialHelper.verifyInMessagingQueue(materialAddedEventProcessConsumer);

                    assertThat(materialAddedEnvelop, jsonEnvelope(metadata().withName(MATERIAL_ADDED_EVENT), payload().isJson(allOf(
                            withJsonPath("material.fileCloudLocation", is("http://inazure/secrets/" + filepathIndexOne[0])),
                            withJsonPath("material.fileType", is(fileType))
                    ))));
                });

        addMaterialHelper.verifyUploadMaterialCalled("http://inazure/secrets/" + filepathIndexOne[0]);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final JsonEnvelope migratedCaseValidatedCreationPendingEvent = addMaterialHelper.verifyInMessagingQueue(migratedCaseValidatedCreationPendingConsumer);
                    assertThat(migratedCaseValidatedCreationPendingEvent, jsonEnvelope(metadata().withName(MIGRATED_CASE_VALIDATED_CREATION_PENDING), payload().isJson(allOf(
                            withJsonPath("receiveMigratedCaseFile.submissionId", is(submissionId))
                    ))));
                });
    }

    public void verifyReceiveMigratedCaseFileForMultipleMaterial(AddMaterialHelper addMaterialHelper, String submissionId, String[] filepathIndexOne, final String fileType) {
        verifyReceiveMigratedCaseFileForMultipleMaterial(addMaterialHelper, submissionId, filepathIndexOne, fileType, null);
    }

    public void verifyReceiveMigratedCaseFileForMultipleMaterial(AddMaterialHelper addMaterialHelper, String submissionId, String[] filepathIndexOne, final String fileType, final String expectedDefaultedHearingTime) {

        await()
                .untilAsserted(() -> {
                    final JsonEnvelope materialAddedEnvelop = addMaterialHelper.verifyInMessagingQueue(materialAddedEventProcessConsumer);

                    assertThat(materialAddedEnvelop, jsonEnvelope(metadata().withName(MATERIAL_ADDED_EVENT), payload().isJson(allOf(
                            withJsonPath("material.fileCloudLocation", is("http://inazure/secrets/" + filepathIndexOne[0])),
                            withJsonPath("material.fileType", is(fileType))
                    ))));
                });

        if(filepathIndexOne.length > 1 ){
            addMaterialHelper.verifyUploadMaterialCalled("http://inazure/secrets/" + filepathIndexOne[1]);
        }

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final JsonEnvelope migratedCaseValidatedCreationPendingEvent = addMaterialHelper.verifyInMessagingQueue(migratedCaseValidatedCreationPendingConsumer);
                    if (expectedDefaultedHearingTime != null) {
                        assertThat(migratedCaseValidatedCreationPendingEvent, jsonEnvelope(metadata().withName(MIGRATED_CASE_VALIDATED_CREATION_PENDING), payload().isJson(allOf(
                                withJsonPath("receiveMigratedCaseFile.submissionId", is(submissionId)),
                                withJsonPath("migratedHearingWithReferenceDataList[0].migratedHearing.timeOfHearing", is(expectedDefaultedHearingTime))
                        ))));
                    } else {
                        assertThat(migratedCaseValidatedCreationPendingEvent, jsonEnvelope(metadata().withName(MIGRATED_CASE_VALIDATED_CREATION_PENDING), payload().isJson(allOf(
                                withJsonPath("receiveMigratedCaseFile.submissionId", is(submissionId))
                        ))));
                    }
                });
    }

    public void verifyCourtProceedingsForCaseCreationHasBeenInitiated(final String containedString, boolean retrialIndicator, Integer offencCount) {

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(containedString))).size(), is(1)));

        try (final JsonReader jsonReader = createReader(new StringReader(getLastLoggedRequest(containedString)))) {

            final JsonObject jsonObject = jsonReader.readObject();

            final DefaultJsonObjectEnvelopeConverter defaultJsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();

            final JsonEnvelope envelope = defaultJsonObjectEnvelopeConverter.asEnvelope(jsonObject);

            assertThat(envelope,
                    jsonEnvelope(metadata()
                                    .withName(PROGRESSION_INITIATE_COURT_PROCEEDINGS),
                            payload().isJson(allOf(withJsonPath("initiateCourtProceedings.prosecutionCases[0].retrialIndicator", is(retrialIndicator)),
                                    withJsonPath("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].count", is(offencCount))
                            ))));
        }
    }

    public void verifyCourtProceedingsForCaseCreationHasBeenInitiatedWithAllocationDecision(final String containedString, final String motReasonId) {

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(containedString))).size(), is(1)));

        try (final JsonReader jsonReader = createReader(new StringReader(getLastLoggedRequest(containedString)))) {

            final JsonObject jsonObject = jsonReader.readObject();

            final DefaultJsonObjectEnvelopeConverter defaultJsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();

            final JsonEnvelope envelope = defaultJsonObjectEnvelopeConverter.asEnvelope(jsonObject);

            assertThat(envelope,
                    jsonEnvelope(metadata()
                                    .withName(PROGRESSION_INITIATE_COURT_PROCEEDINGS),
                            payload().isJson(allOf(withJsonPath("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].allocationDecision.motReasonId", is(motReasonId))))));
        }
    }

    public void verifyMigratedCaseFileReceivedWithDefaultedHearingTime(final AddMaterialHelper addMaterialHelper, final String expectedDefaultedUtcTime) {
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    final JsonEnvelope event = addMaterialHelper.verifyInMessagingQueue(migratedCaseFileReceivedConsumer);
                    assertThat(event, jsonEnvelope(metadata().withName(MIGRATED_CASE_FILE_RECEIVED), payload().isJson(allOf(
                            withJsonPath("receiveMigratedCaseFile.migratedCaseDetails.hearings", hasSize(4)),
                            withJsonPath("receiveMigratedCaseFile.migratedCaseDetails.hearings[0].timeOfHearing", is(expectedDefaultedUtcTime)),
                            withJsonPath("receiveMigratedCaseFile.migratedCaseDetails.hearings[1].timeOfHearing", is(expectedDefaultedUtcTime)),
                            withJsonPath("receiveMigratedCaseFile.migratedCaseDetails.hearings[2].timeOfHearing", is("10:05:00")),
                            withJsonPath("receiveMigratedCaseFile.migratedCaseDetails.hearings[3].timeOfHearing", is("08:30:00"))
                    ))));
                });
    }

    public void verifyCourtProceedingsInitiatedWithoutOffenceCustodyTimeLimit(final String caseUrn, final String offenceId) {

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(), is(1)));

        final String requestBody = getLastLoggedRequest(caseUrn);

        assertThat(requestBody, containsString(offenceId));
        assertThat(requestBody, not(containsString("custodyTimeLimit")));
    }

    public static String getLastLoggedRequest(final String caseUrn) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                .withRequestBody(containing(caseUrn)));

        return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
    }

}
