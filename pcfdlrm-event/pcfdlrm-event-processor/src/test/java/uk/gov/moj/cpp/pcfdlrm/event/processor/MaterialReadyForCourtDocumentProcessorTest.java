package uk.gov.moj.cpp.pcfdlrm.event.processor;


import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaterialReadyForCourtDocumentProcessorTest {

    @InjectMocks
    private MaterialReadyForCourtDocumentProcessor materialReadyForCourtDocumentProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderCaptor;

    @Test
    void shouldHandleMaterialReadyForCourtDocument() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("pcfdlrm.events.material-ready-for-court-document")
                .build(), NULL);
        materialReadyForCourtDocumentProcessor.handleMaterialReadyForCourtDocument(jsonEnvelope);

        verify(sender).sendAsAdmin(senderCaptor.capture());

        final JsonEnvelope event = senderCaptor.getValue();

        assertThat(event.metadata().name(), is("progression.add-court-document"));

    }
}

