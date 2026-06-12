package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cps.pcfdlrm.command.handler.AcceptMigratedCase.acceptMigratedCase;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cps.pcfdlrm.command.handler.AcceptMigratedCase;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionPublicEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionPublicEventProcessor.class);
    private static final String FIELD_ID = "id";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("public.progression.prosecution-case-created")
    public void handleProsecutionCaseCreated(final JsonEnvelope prosecutionCaseCreated) {
        LOGGER.info("Received public.progression.prosecution-case-created- {} ", prosecutionCaseCreated.payload());

        final JsonObject prosecutionCase = prosecutionCaseCreated.payloadAsJsonObject().getJsonObject("prosecutionCase");
        final UUID caseId = fromString(prosecutionCase.getString(FIELD_ID));
        LOGGER.info("Received caseId from public.progression.prosecution-case-created- {} ", caseId);

        final List<UUID> defendantIds = prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .map(defendant -> fromString(defendant.getString(FIELD_ID))).toList();


        final boolean hasMigrationSourceSystem = prosecutionCase.getJsonObject("migrationSourceSystem") != null;

        if (hasMigrationSourceSystem) {
            final AcceptMigratedCase acceptMigratedCase = acceptMigratedCase()
                    .withCaseId(caseId)
                    .withDefendantIds(defendantIds)
                    .build();

            final Metadata metadata = metadataFrom(prosecutionCaseCreated.metadata())
                    .withName("pcfdlrm.command.accept-migrated-case")
                    .build();

            sender.send(envelopeFrom(metadata, acceptMigratedCase));

        }
    }
}
