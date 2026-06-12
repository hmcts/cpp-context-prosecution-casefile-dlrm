package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_PLEA_ID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PLEA_DATE_CANNOT_BE_FUTURE_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PleaValidationRuleTest {

    @InjectMocks
    private PleaValidationRule pleaValidationRule;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DefendantWithReferenceData defendantWithReferenceData;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private MigratedDefendant migratedDefendant;

    @BeforeEach
    void setUp() {
        pleaValidationRule = new PleaValidationRule();
    }

    @Test
    void testValidate_ReturnsValid_WhenNoOffences() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getOffences()).thenReturn(null);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void testValidate_ReturnsValid_WhenNoPleas() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getOffences()).thenReturn(Collections.emptyList());

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldValidateAndPass() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("G")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence().withOffenceId(offenceId).withPlea(MigratedPlea.migratedPlea().withId(randomUUID()).withPleaDate(now()).build()).build();


        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(0, result.problems().size());
    }

    @Test
    void shouldNotRaiseProblemWhenPleaCodeIsSetAsIndicatedPleaAndPleaDate() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withJurisdiction(Jurisdiction.EITHER)
                .withPleaTypeCode("IG")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Indicated Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now())
                        .build()).build();


        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(0, result.problems().size());

    }

    @Test
    void shouldRaiseProblemWhenPleaCodeIsSetAsIndicatedPleaButNotPleaDate() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withJurisdiction(Jurisdiction.EITHER)
                .withPleaTypeCode("IG")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Indicated Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .build()).build();


        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());

    }

    @Test
    void shouldValidateAndFail() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("VALID_CODE")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence().withOffenceId(offenceId).withPlea(MigratedPlea.migratedPlea().withId(randomUUID()).withPleaDate(now()).build()).build();


        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(randomUUID(), pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());

    }

    @Test
    void shouldRaiseProblemWhenPleaDateIsInFuture() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("G")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now().plusDays(1))
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(PLEA_DATE_CANNOT_BE_FUTURE_DATE.name(), result.problems().get(0).getCode());
        assertEquals(offence.getPlea().getPleaDate().toString(), result.problems().get(0).getValues().get(0).getValue());
    }

    @Test
    void shouldPassWhenPleaTypeGuiltyFlagIsNoAndPleaDateIsMissing() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("NG")
                .withPleaTypeGuiltyFlag("No")
                .withPleaValue("Not Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(0, result.problems().size());
    }

    @Test
    void shouldFailWhenPleaTypeGuiltyFlagIsNoButPleaDateIsInFuture() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("NG")
                .withPleaTypeGuiltyFlag("No")
                .withPleaValue("Not Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now().plusDays(1))
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(PLEA_DATE_CANNOT_BE_FUTURE_DATE.name(), result.problems().get(0).getCode());
        assertEquals(offence.getPlea().getPleaDate().toString(), result.problems().get(0).getValues().get(0).getValue());
    }

    @Test
    void shouldHandleNullPleaReferenceDataMap() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now())
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(null);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(INVALID_PLEA_ID.name(), result.problems().get(0).getCode());
    }

    @Test
    void shouldHandleEmptyPleaReferenceDataMap() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        UUID pleaId = randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(pleaId)
                        .withPleaDate(now())
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(new HashMap<>());

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(INVALID_PLEA_ID.name(), result.problems().get(0).getCode());
    }

    @Test
    void shouldHandleMultipleOffencesWithMixedValidationResults() {
        UUID defendantId = randomUUID();
        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();
        UUID offenceId3 = randomUUID();

        PleaReferenceData validPleaData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("G")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Guilty")
                .build();
        PleaReferenceData invalidPleaData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("NG")
                .withPleaTypeGuiltyFlag("No")
                .withPleaValue("Not Guilty")
                .build();

        MigratedOffence validOffence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId1)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now())
                        .build()).build();

        MigratedOffence invalidPleaCode = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId2)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .build()).build();

        MigratedOffence futureDatePlea = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId3)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now().plusDays(1))
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Arrays.asList(validOffence, invalidPleaCode, futureDatePlea));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        Map<UUID, PleaReferenceData> defendantPleaData = new HashMap<>();
        defendantPleaData.put(offenceId1, validPleaData);
        defendantPleaData.put(offenceId3, invalidPleaData);
        pleaReferenceDataMap.put(defendantId, defendantPleaData);

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(2, result.problems().size());
        assertEquals(futureDatePlea.getPlea().getPleaDate().toString(), result.problems().get(1).getValues().get(0).getValue());
    }

    @Test
    void shouldHandleOffenceWithPleaButNoPleaId() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withPleaDate(now())
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(new HashMap<>());

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(0, result.problems().size());
    }

    @Test
    void shouldHandleMultipleProblemsOnSameOffence() {
        UUID defendantId = randomUUID();
        UUID offenceId = randomUUID();
        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withPleaTypeCode("G")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Guilty")
                .build();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(now().plusDays(1))
                        .build()).build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(defendantId, Map.of(offenceId, pleaReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).thenReturn(pleaReferenceDataMap);

        ValidationResult result = pleaValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(PLEA_DATE_CANNOT_BE_FUTURE_DATE.name(), result.problems().get(0).getCode());
    }

}
