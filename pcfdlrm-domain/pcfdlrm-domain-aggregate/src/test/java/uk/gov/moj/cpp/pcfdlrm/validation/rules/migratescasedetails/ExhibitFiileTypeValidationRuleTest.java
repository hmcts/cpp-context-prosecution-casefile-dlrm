package uk.gov.moj.cpp.pcfdlrm.validation.rules.migratescasedetails;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial.migratedMaterial;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedMaterialsWithOriginatingSystem;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ExhibitFiileTypeValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private ExhibitFiileTypeValidationRule validationRule;

    @BeforeEach
    void setUp() {
        validationRule = new ExhibitFiileTypeValidationRule();
    }

    @Test
    void shouldReturnValidForXhibitSystemWithSinglePdfFile() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInput("abc.pdf", "XHIBIT");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertThat(result, is(VALID));
    }

    @Test
    void shouldReturnValidForLibraSystemRegardlessOfMaterials() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInput("def.pdf", "LIBRA");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertThat(result, is(VALID));
    }

    @Test
    void shouldReturnValidForXhibitSystemWithMultiplePdfFiles() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInputWithMultipleFiles("XHIBIT");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertThat(result, is(VALID));
    }

    @Test
    void shouldReturnValidationErrorForXhibitSystemWhenSecondMaterialHasNonPdfFile() {
        // Given
        MigratedMaterial validMaterial = migratedMaterial().withFileName("first.pdf").withFileType("99").build();
        MigratedMaterial invalidMaterial = migratedMaterial().withFileName("second.docx").withFileType("99").build();
        MigratedMaterialsWithOriginatingSystem input = new MigratedMaterialsWithOriginatingSystem(
                Arrays.asList(validMaterial, invalidMaterial), "XHIBIT", getSections());

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertFalse(result.problems().isEmpty());
        assertThat(result.problems().get(0).getCode(), is(INVALID_FILE_TYPE_FOR_XHIBIT.name()));
    }

    @Test
    void shouldReturnValidationErrorForXhibitSystemWhenSecondMaterialHasWrongFileType() {
        // Given
        MigratedMaterial validMaterial = migratedMaterial().withFileName("first.pdf").withFileType("99").build();
        MigratedMaterial invalidMaterial = migratedMaterial().withFileName("second.pdf").withFileType("1").build();
        MigratedMaterialsWithOriginatingSystem input = new MigratedMaterialsWithOriginatingSystem(
                Arrays.asList(validMaterial, invalidMaterial), "XHIBIT", getSections());

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertFalse(result.problems().isEmpty());
        assertThat(result.problems().get(0).getCode(), is(INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION.name()));
    }

    @Test
    void shouldReturnValidationErrorForXhibitSystemWithNonPdfFile() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInput("document.docx", "XHIBIT");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertFalse(result.problems().isEmpty());
        assertThat(result.problems().get(0).getCode(), is(INVALID_FILE_TYPE_FOR_XHIBIT.name()));
    }

    @Test
    void shouldReturnValidationErrorForXhibitSystemWithFileTypeNot99() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInputWithFileType("test.pdf", "XHIBIT", "1");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertFalse(result.problems().isEmpty());
        assertThat(result.problems().get(0).getCode(), is(INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION.name()));
    }

    @Test
    void shouldReturnValidForXhibitSystemWithFileType99() {
        // Given
        MigratedMaterialsWithOriginatingSystem input = createInputWithFileType("test.pdf", "XHIBIT", "99");

        // When
        ValidationResult result = validationRule.validate(input, referenceDataQueryService);

        // Then
        assertThat(result, is(VALID));
    }

    // Simple helper method using Builder pattern
    private MigratedMaterialsWithOriginatingSystem createInput(String fileName, String systemName) {
        MigratedMaterial material = migratedMaterial()
                .withFileName(fileName)
                .withFileType("99")
                .build();
        return new MigratedMaterialsWithOriginatingSystem(Collections.singletonList(material), systemName, getSections());
    }

    private MigratedMaterialsWithOriginatingSystem createInputWithMultipleFiles(String systemName) {
        MigratedMaterial material1 = migratedMaterial().withFileName("abc.pdf").withFileType("99").build();
        MigratedMaterial material2 = migratedMaterial().withFileName("def.pdf").withFileType("99").build();
        return new MigratedMaterialsWithOriginatingSystem(Arrays.asList(material1, material2), systemName, getSections());
    }

    private MigratedMaterialsWithOriginatingSystem createInputWithFileType(String fileName, String systemName, String fileType) {
        MigratedMaterial material = migratedMaterial()
                .withFileName(fileName)
                .withFileType(fileType)
                .build();
        return new MigratedMaterialsWithOriginatingSystem(Collections.singletonList(material), systemName, getSections());
    }

    private static Map<String, ImmutablePair<String, String>> getSections() {
        return Map.of(
                "1", ImmutablePair.of("IDPC", "IDPC Bundle"),
                "2", ImmutablePair.of("MCEB", "Magistrates' court evidence bundle"),
                "3", ImmutablePair.of("WS", "Witness Statements"),
                "6", ImmutablePair.of("UM", "Unused material"),
                "8", ImmutablePair.of("WS", "Witness Statements"),
                "9", ImmutablePair.of("EX", "Exhibits"),
                "99",ImmutablePair.of("PSJH", "Private section - Judges & HMCTS")
        );

    }}
