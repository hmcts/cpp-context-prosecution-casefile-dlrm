package uk.gov.moj.cpp.pcfdlrm.domain.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

class GeneratedEventTypeInventoryTest {

    private static final String SCHEMA_CLASSPATH_DIR = "json/schema";

    @Test
    // BC-21 codegen type-inventory guard — see docs/j25-parity-checklist.md, 01-requirements.md
    // FR12, 02-design.md §G.
    void shouldGenerateExactlyOneTypePerOwnSchemaFile() throws Exception {
        final URL schemaDirUrl = getClass().getClassLoader().getResource(SCHEMA_CLASSPATH_DIR);
        assertFalse(schemaDirUrl == null, "Expected " + SCHEMA_CLASSPATH_DIR + " to be on the test classpath");

        final File schemaDir = new File(schemaDirUrl.toURI());
        final File[] schemaFiles = schemaDir.listFiles((dir, name) -> name.endsWith(".json"));
        assertFalse(schemaFiles == null || schemaFiles.length == 0, "Expected schema files under " + schemaDir);

        for (final File schemaFile : schemaFiles) {
            final String fullyQualifiedClassName = expectedTypeNameFor(schemaFile);
            assertDoesNotThrow(() -> Class.forName(fullyQualifiedClassName),
                    () -> "Expected generated type " + fullyQualifiedClassName + " for schema " + schemaFile.getName());
        }
    }

    private static String expectedTypeNameFor(final File schemaFile) throws IOException {
        try (InputStream in = schemaFile.toURI().toURL().openStream()) {
            final JsonObject schema = Json.createReader(in).readObject();
            final URI id = URI.create(schema.getString("id"));

            final List<String> pathSegments = Arrays.stream(id.getPath().split("/"))
                    .filter(segment -> !segment.isEmpty())
                    .toList();

            final String lastSegment = pathSegments.get(pathSegments.size() - 1).replace(".json", "");
            final String simpleClassName = toPascalCase(lastSegment);
            final String packageName = "uk.gov.moj." + String.join(".", pathSegments.subList(0, pathSegments.size() - 1));

            return packageName + "." + simpleClassName;
        }
    }

    private static String toPascalCase(final String kebabCase) {
        final StringBuilder pascalCase = new StringBuilder();
        for (final String word : kebabCase.split("-")) {
            pascalCase.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return pascalCase.toString();
    }
}
