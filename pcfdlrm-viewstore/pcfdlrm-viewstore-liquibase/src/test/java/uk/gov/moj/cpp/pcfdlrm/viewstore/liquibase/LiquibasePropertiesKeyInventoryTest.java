package uk.gov.moj.cpp.pcfdlrm.viewstore.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LiquibasePropertiesKeyInventoryTest {

    @Test
    // BC-07 liquibase.properties key-inventory pin — see docs/j25-parity-checklist.md,
    // 01-requirements.md FR12/FR16, 02-design.md §D.
    void shouldExposeExactlyTheCurrentSupportedKeySet() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("liquibase.properties")) {
            properties.load(in);
        }

        assertEquals(Set.of("changelogFile", "liquibase.hub.mode", "liquibase.headless"),
                properties.stringPropertyNames());
    }
}
