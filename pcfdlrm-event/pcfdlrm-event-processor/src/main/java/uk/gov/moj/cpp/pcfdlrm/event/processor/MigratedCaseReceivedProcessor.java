package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.MigratedCaseToProsecutionCaseConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseReceivedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.EnvelopeHelper;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class MigratedCaseReceivedProcessor {

    private static final String PROGRESSION_INITIATE_COURT_PROCEEDINGS = "progression.initiate-court-proceedings";

    private static final Logger LOGGER = LoggerFactory.getLogger(MigratedCaseReceivedProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private MigratedCaseToProsecutionCaseConverter migratedCaseToProsecutionCaseConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private PcfMigratedCaseReceivedCounter pcfMigratedCaseReceivedCounter;

    @Handles("pcfdlrm.events.migrated-case-file-received")
    public void handleMigratedCaseReceived(final Envelope<MigratedCaseFileReceived> envelope) {

        LOGGER.info("Received MigratedCaseFileReceived- {} ", envelope.payload());

        final InitiateCourtProceedings initiateCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(envelope.payload());

        final Envelope<JsonValue> sendingEnvelope = envelopeHelper.withMetadataInPayloadForEnvelope(
                envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_INITIATE_COURT_PROCEEDINGS),
                        objectToJsonObjectConverter.convert(initiateCourtProceedings)));

        LOGGER.info("Sending progression.initiate-court-proceedings sending envelope- {} ", sendingEnvelope.payload());

        sender.sendAsAdmin(sendingEnvelope);
        pcfMigratedCaseReceivedCounter.increment();

    }
}