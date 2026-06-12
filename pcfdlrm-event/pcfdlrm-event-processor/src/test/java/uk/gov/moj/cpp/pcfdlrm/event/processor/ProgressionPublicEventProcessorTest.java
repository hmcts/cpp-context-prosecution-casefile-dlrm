package uk.gov.moj.cpp.pcfdlrm.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressionPublicEventProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private ProgressionPublicEventProcessor progressionPublicEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionCaseCreated>> envelopeCaptor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final String LEGACY_SYSTEM = "XHIBIT";
    private static final String PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED="public.progression.prosecution-case-created";

    @Test
    void shouldHandleProgressionPublicEvent() {
        assertThat(ProgressionPublicEventProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleProsecutionCaseCreated").thatHandles(PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED))
        );
    }

    @Test
    void shouldSendAcceptMigratedCaseCommandWhenProsecutionCaseHasMigrationSourceSystem() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID defendantId1 = UUID.randomUUID();
        UUID defendantId2 = UUID.randomUUID();

        JsonObject defendant1 = createObjectBuilder()
                .add("id", defendantId1.toString())
                .build();

        JsonObject defendant2 = createObjectBuilder()
                .add("id", defendantId2.toString())
                .build();

        JsonArray defendants = JsonObjects.createArrayBuilder()
                .add(defendant1)
                .add(defendant2)
                .build();

        JsonObject migrationSourceSystem = createObjectBuilder()
                .add("sourceSystem", LEGACY_SYSTEM)
                .build();

        JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", defendants)
                .add("migrationSourceSystem", migrationSourceSystem)
                .build();

        JsonObject payload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase)
                .build();

        JsonObject metadataJson = createObjectBuilder()
                .add("name", PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED)
                .add("id", UUID.randomUUID().toString())
                .build();
        Metadata metadata = mock(Metadata.class);
        JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metadataJson);

        // When
        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        // Then
        verify(sender).send(envelopeCaptor.capture());

        Envelope<ProsecutionCaseCreated> capturedEnvelope = envelopeCaptor.getValue();
        assertNotNull(capturedEnvelope);
        assertNotNull(capturedEnvelope.payload());
    }

    @Test
    void shouldNotSendAcceptMigratedCaseCommandWhenProsecutionCaseHasNoMigrationSourceSystem() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .build();

        JsonArray defendants = JsonObjects.createArrayBuilder()
                .add(defendant)
                .build();

        JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", defendants)
                .build();

        JsonObject payload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase)
                .build();

        JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        // When
        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        // Then
        verify(sender, never()).send(any());
    }

    @Test
    void shouldHandleProsecutionCaseWithSingleDefendant() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        JsonObject defendant = createObjectBuilder()
                .add("id", defendantId.toString())
                .build();

        JsonArray defendants = JsonObjects.createArrayBuilder()
                .add(defendant)
                .build();

        JsonObject migrationSourceSystem = createObjectBuilder()
                .add("sourceSystem", LEGACY_SYSTEM)
                .build();

        JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", defendants)
                .add("migrationSourceSystem", migrationSourceSystem)
                .build();

        JsonObject payload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase)
                .build();

        JsonObject metadataJson = createObjectBuilder()
                .add("name", PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED)
                .add("id", UUID.randomUUID().toString())
                .build();
        Metadata metadata = mock(Metadata.class);
        JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metadataJson);

        // When
        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        // Then
        verify(sender).send(envelopeCaptor.capture());

        Envelope capturedEnvelope = envelopeCaptor.getValue();
        assertNotNull(capturedEnvelope);
        assertNotNull(capturedEnvelope.payload());
    }

    @Test
    void shouldHandleProsecutionCaseWithEmptyDefendantsArray() {
        // Given
        UUID caseId = UUID.randomUUID();

        JsonArray defendants = JsonObjects.createArrayBuilder().build();

        JsonObject migrationSourceSystem = createObjectBuilder()
                .add("sourceSystem", LEGACY_SYSTEM)
                .build();

        JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", defendants)
                .add("migrationSourceSystem", migrationSourceSystem)
                .build();

        JsonObject payload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase)
                .build();

        JsonObject metadataJson = createObjectBuilder()
                .add("name", PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED)
                .add("id", UUID.randomUUID().toString())
                .build();
        Metadata metadata = mock(Metadata.class);
        JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metadataJson);

        // When
        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        // Then
        verify(sender).send(envelopeCaptor.capture());

        Envelope<ProsecutionCaseCreated> capturedEnvelope = envelopeCaptor.getValue();
        assertNotNull(capturedEnvelope);
        assertNotNull(capturedEnvelope.payload());
    }

    @Test
    void shouldHandleProsecutionCaseWithMultipleDefendants() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID defendantId1 = UUID.randomUUID();
        UUID defendantId2 = UUID.randomUUID();
        UUID defendantId3 = UUID.randomUUID();

        JsonObject defendant1 = createObjectBuilder()
                .add("id", defendantId1.toString())
                .build();

        JsonObject defendant2 = createObjectBuilder()
                .add("id", defendantId2.toString())
                .build();

        JsonObject defendant3 = createObjectBuilder()
                .add("id", defendantId3.toString())
                .build();

        JsonArray defendants = JsonObjects.createArrayBuilder()
                .add(defendant1)
                .add(defendant2)
                .add(defendant3)
                .build();

        JsonObject migrationSourceSystem = createObjectBuilder()
                .add("sourceSystem", LEGACY_SYSTEM)
                .build();

        JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", defendants)
                .add("migrationSourceSystem", migrationSourceSystem)
                .build();

        JsonObject payload = createObjectBuilder()
                .add("prosecutionCase", prosecutionCase)
                .build();

        JsonObject metadataJson =createObjectBuilder()
                .add("name", PUBLIC_PROGRESSION_PROSECUTION_CASE_CREATED)
                .add("id", UUID.randomUUID().toString())
                .build();
        Metadata metadata = mock(Metadata.class);
        JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.asJsonObject()).thenReturn(metadataJson);

        // When
        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        // Then
        verify(sender).send(envelopeCaptor.capture());

        Envelope<ProsecutionCaseCreated> capturedEnvelope = envelopeCaptor.getValue();
        assertNotNull(capturedEnvelope);
        assertNotNull(capturedEnvelope.payload());
    }
}


