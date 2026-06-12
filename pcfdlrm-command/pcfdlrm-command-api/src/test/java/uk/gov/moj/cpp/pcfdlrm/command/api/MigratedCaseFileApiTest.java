package uk.gov.moj.cpp.pcfdlrm.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedCaseFileApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private MigratedCaseFileApi migratedCaseFileApi;

    @Mock
    private JsonEnvelope envelope;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    void shouldHandleReceiveCase() {

        final MetadataBuilder metadataBuilder = DefaultJsonMetadata.metadataBuilder();
        metadataBuilder.withName("receive-migrated-case-file").withId(randomUUID());
        when(envelope.metadata()).thenReturn(metadataBuilder.build());

        migratedCaseFileApi.receiveMigratedCaseFile(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> resultEnvelope = envelopeCaptor.getValue();
        assertThat(resultEnvelope.metadata().name(), is("pcfdlrm.command.receive-migrated-case-file"));
    }

}