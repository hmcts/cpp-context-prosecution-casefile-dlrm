package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialReadyForCourtDocumentProcessor {

    @Inject
    private Sender sender;

    @Handles("pcfdlrm.events.material-ready-for-court-document")
    public void handleMaterialReadyForCourtDocument(final JsonEnvelope materialReadyForCourtDocumentEnvelope) {
        final Metadata metadata = metadataFrom(materialReadyForCourtDocumentEnvelope.metadata())
                .withName("progression.add-court-document")
                .build();

        sender.sendAsAdmin(envelopeFrom(metadata, materialReadyForCourtDocumentEnvelope.payload()));
    }

}
