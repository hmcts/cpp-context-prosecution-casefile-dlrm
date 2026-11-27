package uk.gov.moj.cpp.pcfdlrm.event.processor;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

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
    void shouldSendMaterialReadyForCourtDocumentToProgressionUnchanged() {
        final UUID eventId = randomUUID();
        final String materialId = randomUUID().toString();
        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("courtDocument", createObjectBuilder()
                        .add("courtDocumentId", randomUUID().toString())
                        .add("documentTypeId", randomUUID().toString())
                        .add("documentCategory", createObjectBuilder()
                                .add("caseDocument", createObjectBuilder()
                                        .add("prosecutionCaseId", randomUUID().toString())))
                        .add("materials", createArrayBuilder()
                                .add(createObjectBuilder().add("id", materialId))))
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder().withId(eventId)
                .withName("pcfdlrm.events.material-ready-for-court-document")
                .build(), payload);

        materialReadyForCourtDocumentProcessor.handleMaterialReadyForCourtDocument(jsonEnvelope);

        verify(sender).sendAsAdmin(senderCaptor.capture());

        final JsonEnvelope command = senderCaptor.getValue();

        assertThat(command.metadata().name(), is("progression.add-court-document"));
        assertThat(command.metadata().id(), is(eventId));
        assertThat(command.payloadAsJsonObject(), is(payload));
    }
}

