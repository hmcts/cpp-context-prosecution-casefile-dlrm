package uk.gov.moj.cpp.pcfdlrm.event.processor.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

class JsonObjectsNullGuardParityTest {

    @Test
    // BC-11 null-guard parity pin — see docs/j25-parity-checklist.md, 01-requirements.md FR9,
    // 02-design.md §B.
    void shouldThrowNpeWhenNullValueAddedToObjectBuilder() {
        final JsonObjectBuilder builder = createObjectBuilder();

        assertThrows(NullPointerException.class, () -> builder.add("aKey", (String) null));
    }
}
