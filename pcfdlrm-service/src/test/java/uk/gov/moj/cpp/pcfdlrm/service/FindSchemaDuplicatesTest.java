package uk.gov.moj.cpp.pcfdlrm.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.jupiter.api.Test;

class FindSchemaDuplicatesTest {

    @Test
    void testSchemaDuplicates() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
