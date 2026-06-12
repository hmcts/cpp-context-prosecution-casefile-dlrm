package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseSuccessfullyProcessedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseFailedCounter;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class MigratedCaseFileProcessedProcessor {

    @Inject
    private Sender sender;
    @Inject
    private PcfMigratedCaseSuccessfullyProcessedCounter pcfMigratedCaseSuccessfullyProcessedCounter;
    @Inject
    private PcfMigratedCaseFailedCounter pcfMigratedCaseFailedCounter;


    public static final String PUBLIC_PCFDLRM_MIGRATED_CASE_FILE_PROCESSED = "public.pcfdlrm.migrated-case-file-processed";

    @Handles("pcfdlrm.events.migrated-case-file-processed")
    public void handleMigratedCaseFileProcessed(final Envelope<MigratedCaseFileProcessed> migratedCaseFileProcessed) {

        final MigratedCaseFileProcessed event = migratedCaseFileProcessed.payload();

        final Metadata metadata = Envelope.metadataFrom(migratedCaseFileProcessed.metadata())
                .withName(PUBLIC_PCFDLRM_MIGRATED_CASE_FILE_PROCESSED)
                .build();

        sender.send(envelopeFrom(
                metadata,
                event
        ));
        if (event.getProcessingIsSuccessful()) {
            pcfMigratedCaseSuccessfullyProcessedCounter.increment();
        } else {
            pcfMigratedCaseFailedCounter.increment();
        }
    }

}
