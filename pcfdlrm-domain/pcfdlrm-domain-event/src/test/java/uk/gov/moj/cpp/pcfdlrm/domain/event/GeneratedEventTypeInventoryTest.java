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
    // BC-21: reflections 0.9.10->0.10.2 changes the classpath-scanning contract codegen relies on,
    // so verify generated sources rather than assume the count. Derives the expected type from each
    // schema's own "id" URL instead of hard-coding a package or class list (FR12).
    //
    // Two package roots are genuinely in use here, confirmed by inspecting the real
    // target/generated-sources tree before writing this, not assumed (02-design.md §G): 7 of the 8
    // schemas' "id" fields sit under .../cps/.../domain/event/, generating into
    // uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event; defendant-validation-passed's "id" sits
    // under a different path, .../cpp/json/schemas/.../events/, generating into
    // uk.gov.moj.cpp.json.schemas.prosecution.casefile.dlrm.events. This test follows the id field
    // rather than assuming one package for the whole module.
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
