package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static java.util.List.of;
import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader.fixture;
import static uk.gov.moj.cpp.pcfdlrm.test.WholePayloadMatcher.matchesWholePayload;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseFailedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseReceivedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseSuccessfullyProcessedCounter;
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

    @Mock
    private PcfMigratedCaseReceivedCounter pcfMigratedCaseReceivedCounter;

    @Captor
    private ArgumentCaptor<Envelope<?>> privateEventCaptor;

    @Captor
    private ArgumentCaptor<Envelope<MigratedCaseFileProcessed>> publicEventCaptor;

    @InjectMocks
    private MigratedCaseFileProcessedProcessor processor;

    /**
     * AC-T3-4 — {@code MigratedCaseFileProcessedProcessor} does no conversion at all: it re-sends
     * the same {@code MigratedCaseFileProcessed} payload under the renamed
     * {@code public.pcfdlrm.migrated-case-file-processed} envelope. Asserts both the renamed
     * metadata and the payload whole, rather than a partial field dig.
     */
    @Test
    void shouldSendPublicEventWithRenamedMetadataAndWholePayload() {
        final MigratedCaseFileProcessed event = MigratedCaseFileProcessed.migratedCaseFileProcessed()
                .withCaseId(fromString("a4391799-f828-4515-a355-61f1d5d9690c"))
                .withCaseUrn("URN001")
                .withDescription("Either Sending or Receiving Court not found")
                .withProcessingIsSuccessful(false)
                .withSubmissionId(fromString("e3e3e3e3-3333-4333-8333-333333333333"))
                .build();
        final Envelope<MigratedCaseFileProcessed> envelope = getEnvelope(event, "pcfdlrm.events.migrated-case-file-processed");

        processor.handleMigratedCaseFileProcessed(envelope);

        verify(sender).send(publicEventCaptor.capture());
        final Envelope<MigratedCaseFileProcessed> sent = publicEventCaptor.getValue();

        assertThat(sent.metadata().name(), is("public.pcfdlrm.migrated-case-file-processed"));

        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        assertThat(objectToJsonObjectConverter.convert(sent.payload()).toString(),
                matchesWholePayload(fixture("json/migrated-case-file-processed-processor/public-migrated-case-file-processed.json"), of()));
    }

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
        verify(pcfMigratedCaseReceivedCounter).increment();
        verify(pcfMigratedCaseSuccessfullyProcessedCounter, never()).increment();
    }

    static <T> Envelope<T> getEnvelope(final T payload, final String eventName) {
        return Envelope.envelopeFrom(
                metadataWithRandomUUID(eventName),
                payload
        );
    }
}
