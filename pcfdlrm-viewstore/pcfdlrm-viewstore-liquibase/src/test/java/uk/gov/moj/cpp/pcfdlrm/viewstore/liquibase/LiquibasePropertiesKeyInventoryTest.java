package uk.gov.moj.cpp.pcfdlrm.viewstore.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LiquibasePropertiesKeyInventoryTest {

    @Test
    // BC-07: Liquibase 4->5 rejects removed properties (hub.mode, searchPath), failing the
    // migration job at deploy time rather than in mvn test. This pins today's key set — including
    // liquibase.hub.mode, the exact property java-25-parity.pdf flags as rejected — so an unsupported
    // key fails fast here. Removing the stale key is the upgrade story's job (01-requirements.md
    // FR16), not this one.
    void shouldExposeExactlyTheCurrentSupportedKeySet() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("liquibase.properties")) {
            properties.load(in);
        }

        assertEquals(Set.of("changelogFile", "liquibase.hub.mode", "liquibase.headless"),
                properties.stringPropertyNames());
    }
}
