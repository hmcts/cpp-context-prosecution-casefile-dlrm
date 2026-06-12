package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseSuccessfullyProcessedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseFailedCounter;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedCaseFileProcessedProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private PcfMigratedCaseSuccessfullyProcessedCounter pcfMigratedCaseSuccessfullyProcessedCounter;

    @Mock
    private PcfMigratedCaseFailedCounter pcfMigratedCaseFailedCounter;

    @Captor
    private ArgumentCaptor<Envelope<?>> privateEventCaptor;

    @InjectMocks
    private MigratedCaseFileProcessedProcessor processor;


    @Test
    void shouldIncrementSuccessfullyProcessedCounterWhenProcessingIsSuccessfulTrue() {
        // Given
        final MigratedCaseFileProcessed event = MigratedCaseFileProcessed.migratedCaseFileProcessed()
                .withProcessingIsSuccessful(true)
                .build();
        final Envelope<MigratedCaseFileProcessed> envelope = getEnvelope(event, "pcfdlrm.events.migrated-case-file-processed");

        // When
        processor.handleMigratedCaseFileProcessed(envelope);

        // Then
        verify(pcfMigratedCaseSuccessfullyProcessedCounter).increment();
        verify(pcfMigratedCaseFailedCounter, never()).increment();
    }

    @Test
    void shouldIncrementUnsuccessfullyProcessedCounterWhenProcessingIsSuccessfulFalse() {
        // Given
        final MigratedCaseFileProcessed event = MigratedCaseFileProcessed.migratedCaseFileProcessed()
                .withProcessingIsSuccessful(false)
                .build();
        final Envelope<MigratedCaseFileProcessed> envelope = getEnvelope(event, "pcfdlrm.events.migrated-case-file-processed");

        // When
        processor.handleMigratedCaseFileProcessed(envelope);

        // Then
        verify(pcfMigratedCaseFailedCounter).increment();
        verify(pcfMigratedCaseSuccessfullyProcessedCounter, never()).increment();
    }

    static <T> Envelope<T> getEnvelope(final T payload, final String eventName) {
        return Envelope.envelopeFrom(
                metadataWithRandomUUID(eventName),
                payload
        );
    }
}
