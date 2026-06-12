package uk.gov.moj.cpp.pcfdlrm.command.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.pcfdlrm.command.handler.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.pcfdlrm.command.handler.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate;
import uk.gov.moj.cps.pcfdlrm.command.handler.AddCaseCourtDocument;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialRejected;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddMaterialHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    MaterialAdded.class,
                    MaterialRejected.class);
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AddMaterialHandler addMaterialHandler;

    @Spy
    private MigratedCaseFileAggregate migratedCaseFileAggregate = new MigratedCaseFileAggregate();


    @Test
    void shouldAddCaseCourtDocument() throws EventStreamException {

        final AddCaseCourtDocument addCaseCourtDocument = readJson("json/add-cloud-case-court-document.json", AddCaseCourtDocument.class);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (MigratedCaseFileAggregate.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return migratedCaseFileAggregate;
            else
                return null;
        });

        final Envelope<AddCaseCourtDocument> envelope =
                envelopeFrom(metadataFor("pcfdlrm.command.add-case-court-document"), addCaseCourtDocument);
        addMaterialHandler.addCaseCourtDocument(envelope);
        verify(migratedCaseFileAggregate).materialAddedPostProcessing(addCaseCourtDocument.getCourtDocument(),addCaseCourtDocument.getMaterialId());

    }

}