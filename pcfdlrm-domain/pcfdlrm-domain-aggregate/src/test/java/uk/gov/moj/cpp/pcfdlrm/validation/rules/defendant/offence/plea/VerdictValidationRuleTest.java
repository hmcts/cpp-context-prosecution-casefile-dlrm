package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_VERDICT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.VERDICT_DATE_ABSENT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.VERDICT_DATE_CANNOT_BE_FUTURE_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class VerdictValidationRuleTest {

    private static final UUID VERDICT_REF_ID = UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e");

    @InjectMocks
    private VerdictValidationRule verdictValidationRule;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DefendantWithReferenceData defendantWithReferenceData;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private MigratedDefendant migratedDefendant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        verdictValidationRule = new VerdictValidationRule();
    }

    @Test
    void shouldReturnsValidWhenNoOffences() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getOffences()).thenReturn(null);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnsValidWhenNoOffencesWhenEmptyOffencesList() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getOffences()).thenReturn(Collections.emptyList());

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnsValidWhenNoOffencesWhenOffencesExistButNoVerdicts() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(null)
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnsValidWhenNoOffencesWhenValidVerdictWithMatchingReferenceData() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(now())
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(defendantId, Map.of(offenceId, verdictReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldRaiseInvalidCodeProblemWhenInvalidVerdictCode() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(UUID.randomUUID())
                        .withVerdictDate(now())
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(defendantId, Map.of(UUID.randomUUID(), verdictReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(INVALID_VERDICT.name(), result.problems().get(0).getCode());
    }

    @Test
    void shouldRaiseVerdictDateAbsentProblemWhenVerdictDateIsAbsent() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(null)
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(defendantId, Map.of(offenceId, verdictReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(VERDICT_DATE_ABSENT.name(), result.problems().get(0).getCode());
    }

    @Test
    void shouldRaiseVerdictDateCannotBeFutureDateWhenVerdictDateIsInFuture() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(now().plusDays(1))
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(defendantId, Map.of(offenceId, verdictReferenceData));

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(1, result.problems().size());
        assertEquals(VERDICT_DATE_CANNOT_BE_FUTURE_DATE.name(), result.problems().get(0).getCode());
        assertEquals(offence.getVerdict().getVerdictDate().toString(), result.problems().get(0).getValues().get(0).getValue());
    }


    @Test
    void shouldReturnsValidWhenMultipleOffencesWithValidVerdicts() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId1 = UUID.randomUUID();
        UUID offenceId2 = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence1 = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId1)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(now())
                        .build())
                .build();

        MigratedOffence offence2 = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId2)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(now().minusDays(1))
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(asList(offence1, offence2));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        Map<UUID, VerdictReferenceData> defendantVerdictData = new HashMap<>();
        defendantVerdictData.put(offenceId1, verdictReferenceData);
        defendantVerdictData.put(offenceId2, verdictReferenceData);
        verdictReferenceDataMap.put(defendantId, defendantVerdictData);

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    @Test
    void shouldRaiseProblemsWhenMultipleOffencesWithMixedResults() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId1 = UUID.randomUUID();
        UUID offenceId2 = UUID.randomUUID();
        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        MigratedOffence offence1 = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId1)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(UUID.randomUUID())
                        .withVerdictDate(now())
                        .build())
                .build();

        MigratedOffence offence2 = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId2)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(VERDICT_REF_ID)
                        .withVerdictDate(now().plusDays(1))
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(asList(offence1, offence2));

        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        Map<UUID, VerdictReferenceData> defendantVerdictData = new HashMap<>();
        defendantVerdictData.put(offenceId2, verdictReferenceData);
        verdictReferenceDataMap.put(defendantId, defendantVerdictData);

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(verdictReferenceDataMap);

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(2, result.problems().size());
        assertEquals(INVALID_VERDICT.name(), result.problems().get(0).getCode());
        assertEquals(VERDICT_DATE_CANNOT_BE_FUTURE_DATE.name(), result.problems().get(1).getCode());
        assertEquals(offence2.getVerdict().getVerdictDate().toString(), result.problems().get(1).getValues().get(0).getValue());
    }

    @Test
    void shouldReturnsValidWhenVerdictExistsButNoVerdictId() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withVerdictDate(now())
                        .build())
                .build();

        when(defendantWithReferenceData.getDefendant()).thenReturn(migratedDefendant);
        when(migratedDefendant.getId()).thenReturn(defendantId);
        when(migratedDefendant.getOffences()).thenReturn(Collections.singletonList(offence));

        when(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).thenReturn(new HashMap<>());

        ValidationResult result = verdictValidationRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertEquals(VALID, result);
    }

    private VerdictReferenceData getVerdictReferenceData() {
        return VerdictReferenceData.verdictReferenceData()
                .withId(UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e"))
                .withDescription("Found not guilty but guilty by Judge alone (under DVC&V Act 2004) of alternative offence not charged namely")
                .withCategory("Not Guilty but Guilty of alternative offence")
                .withCategoryType("NOT_GUILTY_BUT_GUILTY_OF_ALTERNATIVE_OFFENCE_BY_JURY_CONVICTED")
                .withSequence(165)
                .withJurisdiction(Jurisdiction.CROWN)
                .withVerdictCode("NGJAA")
                .withJurySplitAvailable("No")
                .withCjsVerdictCode("A")
                .build();
    }
}
