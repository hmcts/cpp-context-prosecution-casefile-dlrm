package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.processor.convertor.MigratedCaseToProsecutionCaseConverter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.counter.PcfMigratedCaseReceivedCounter;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.EnvelopeHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class MigratedCaseReceivedProcessorTest {

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Mock
    private Sender sender;

    @Mock
    private MigratedCaseToProsecutionCaseConverter migratedCaseToProsecutionCaseConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private MigratedCaseFileReceived migratedCaseFileReceived;


    @Mock
    private JsonEnvelope envelope1;

    @Mock
    private EnvelopeHelper envelopeHelper;


    @Mock
    private PcfMigratedCaseReceivedCounter pcfMigratedCaseReceivedCounter;

    @InjectMocks
    private MigratedCaseReceivedProcessor migratedCaseReceivedProcessor;

    @Test
    public void shouldHandleSjpProsecutionEvents() {
        assertThat(MigratedCaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleMigratedCaseReceived")
                        .thatHandles("pcfdlrm.events.migrated-case-file-received"))
        );
    }

    @Test
    public void testHandleMigratedCaseReceived() {
        final Metadata metadata = metadataBuilder()
                .withName("pcfdlrm.events.migrated-case-file-received")
                .withId(randomUUID())
                .build();
        final Envelope envelope = Envelope.envelopeFrom(metadata, NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any(JsonEnvelope.class))).thenReturn(envelope);

        migratedCaseReceivedProcessor.handleMigratedCaseReceived(envelopeFrom(metadata, migratedCaseFileReceived));
        verify(sender).sendAsAdmin(envelope);
        verify(pcfMigratedCaseReceivedCounter).increment();
    }

}