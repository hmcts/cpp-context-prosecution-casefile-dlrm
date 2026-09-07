package uk.gov.moj.cpp.pcfdlrm.event.processor.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

class JsonObjectsNullGuardParityTest {

    @Test
    // BC-11 (corrected 2026-08-26, see java-25-parity.pdf and 01-requirements.md FR9): originally
    // hypothesised as a JSON-P ServiceLoader collision between glassfish (J17) and Parsson (J25).
    // Refuted by a real J17/J25 run on cpp-context-notification-notify — both runtimes throw
    // identically when a null value is added via JsonObjectBuilder.add(key, value). This pins that
    // observed J17 behaviour directly on the same uk.gov.justice.services.messaging.JsonObjects
    // helper this module's MetadataHelper/EnvelopeHelper/MaterialEventProcessor already call
    // (confirmed: this repo's only javax.json touchpoints in main code go through this exact helper —
    // see 02-design.md §B), rather than exercising any one caller's business logic. The null-guard
    // itself lives in whichever javax.json.spi.JsonProvider createObjectBuilder() resolves to, not in
    // JsonObjects — this test is deliberately provider-agnostic.
    void shouldThrowNpeWhenNullValueAddedToObjectBuilder() {
        final JsonObjectBuilder builder = createObjectBuilder();

        assertThrows(NullPointerException.class, () -> builder.add("aKey", (String) null));
    }
}
