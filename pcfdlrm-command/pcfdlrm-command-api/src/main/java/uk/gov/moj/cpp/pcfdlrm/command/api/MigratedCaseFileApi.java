package uk.gov.moj.cpp.pcfdlrm.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class MigratedCaseFileApi {

    @Inject
    private Sender sender;

    @Handles("pcfdlrm.receive-migrated-case-file")
    public void receiveMigratedCaseFile(final JsonEnvelope envelope) {

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("pcfdlrm.command.receive-migrated-case-file")
                .build();
        sender.send(envelopeFrom(metadata, envelope.payload()));
    }
}
