package uk.gov.moj.cpp.pcfdlrm.command.handler;


import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DefendantDocument;
import uk.gov.moj.cps.pcfdlrm.command.handler.AddCaseCourtDocument;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(COMMAND_HANDLER)
public class AddMaterialHandler extends BaseProsecutionCaseFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddMaterialHandler.class);

    @Handles("pcfdlrm.command.add-case-court-document")
    public void addCaseCourtDocument(final Envelope<AddCaseCourtDocument> command) throws EventStreamException {
        LOGGER.info("addCaseCourtDocument {}", command.payload());
        final DefendantDocument defendantDocument = command.payload().getCourtDocument().getDocumentCategory().getDefendantDocument();
        final CaseDocument caseDocument = command.payload().getCourtDocument().getDocumentCategory().getCaseDocument();

        final UUID caseId = ofNullable(defendantDocument)
                .map(DefendantDocument::getProsecutionCaseId)
                .orElseGet(() -> caseDocument.getProsecutionCaseId());

       if (nonNull(command.payload().getFileCloudLocation())) {
            appendEventsToStreamMigratedCaseFileAggregate(caseId, command,
                    migratedCaseFileAggregate ->
                            migratedCaseFileAggregate.materialAddedPostProcessing(command.payload().getCourtDocument(), command.payload().getMaterialId()));
       }
    }

}
