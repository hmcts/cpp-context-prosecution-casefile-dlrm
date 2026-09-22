package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VerdictDataRefDataEnricherTest {

    @InjectMocks
    private VerdictDataRefDataEnricher verdictDataRefDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldEnrich() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID verdictId = UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e");

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(verdictId)
                        .withVerdictDate(LocalDate.now())
                        .build())
                .build();

        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withId(defendantId)
                .withOffences(Collections.singletonList(offence))
                .build();

        DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setDefendants(Collections.singletonList(defendant));
        defendantsWithReferenceData.setReferenceDataVO(new ReferenceDataVO());
        defendantsWithReferenceData.setMigrationSourceSystemName("XHIBIT");

        VerdictReferenceData verdictReferenceData = getVerdictReferenceData();

        when(referenceDataQueryService.getVerdictTypeById(verdictId)).thenReturn(Optional.of(verdictReferenceData));

        List<DefendantsWithReferenceData> defendantsWithReferenceDataList = Collections.singletonList(defendantsWithReferenceData);

        verdictDataRefDataEnricher.enrich(defendantsWithReferenceDataList);

        // Assertions
        Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = defendantsWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap();
        assertNotNull(verdictReferenceDataMap);
        assertTrue(verdictReferenceDataMap.containsKey(defendantId));
        assertTrue(verdictReferenceDataMap.get(defendantId).containsKey(offenceId));
        assertEquals("NGJAA", verdictReferenceDataMap.get(defendantId).get(offenceId).getVerdictCode());
    }

    @ParameterizedTest
    @EnumSource(value = Jurisdiction.class, names = {"EITHER", "MAGISTRATES"})
    public void shouldAdmitVerdictReferenceDataForLibraJurisdiction(final Jurisdiction verdictJurisdiction) {
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap =
                enrichForLibraJurisdiction(verdictJurisdiction, defendantId, offenceId);

        assertNotNull(verdictReferenceDataMap);
        assertTrue(verdictReferenceDataMap.containsKey(defendantId));
        assertTrue(verdictReferenceDataMap.get(defendantId).containsKey(offenceId));
    }

    @Test
    public void shouldFilterOutVerdictReferenceDataForLibraCrownJurisdiction() {
        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap =
                enrichForLibraJurisdiction(Jurisdiction.CROWN, UUID.randomUUID(), UUID.randomUUID());

        assertNull(verdictReferenceDataMap);
    }

    private Map<UUID, Map<UUID, VerdictReferenceData>> enrichForLibraJurisdiction(final Jurisdiction verdictJurisdiction, final UUID defendantId, final UUID offenceId) {
        final UUID verdictId = UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e");

        final MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(verdictId)
                        .withVerdictDate(LocalDate.now())
                        .build())
                .build();

        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withId(defendantId)
                .withOffences(Collections.singletonList(offence))
                .build();

        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setDefendants(Collections.singletonList(defendant));
        defendantsWithReferenceData.setReferenceDataVO(new ReferenceDataVO());
        defendantsWithReferenceData.setMigrationSourceSystemName("LIBRA");

        final VerdictReferenceData verdictReferenceData = getVerdictReferenceData(verdictJurisdiction);

        when(referenceDataQueryService.getVerdictTypeById(verdictId)).thenReturn(Optional.of(verdictReferenceData));

        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = Collections.singletonList(defendantsWithReferenceData);

        verdictDataRefDataEnricher.enrich(defendantsWithReferenceDataList);

        return defendantsWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap();
    }

    VerdictReferenceData getVerdictReferenceData(final Jurisdiction jurisdiction) {
        return VerdictReferenceData.verdictReferenceData()
                .withId(UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e"))
                .withDescription("Guilty")
                .withCategory("Guilty")
                .withCategoryType("GUILTY")
                .withSequence(10)
                .withJurisdiction(jurisdiction)
                .withVerdictCode("G")
                .withJurySplitAvailable("No")
                .withCjsVerdictCode("G")
                .build();
    }

    VerdictReferenceData getVerdictReferenceData(){
        return  VerdictReferenceData.verdictReferenceData()
                .withId(UUID.fromString("3be1b0c3-dc72-3a96-9474-07cb9b37a43e"))
                .withDescription("Found not guilty but guilty by Judge alone (under DVC&V Act 2004) of alternative offence not charged namely")
                .withCategory("Not Guilty but Guilty of alternative offence")
                .withCategoryType("NOT_GUILTY_BUT_GUILTY_OF_ALTERNATIVE_OFFENCE_BY_JURY_CONVICTED")
                .withSequence(165)
                .withJurisdiction(Jurisdiction.CROWN)  // Ensure `Jurisdiction` enum exists
                .withVerdictCode("NGJAA")
                .withJurySplitAvailable("No") // Ensure `JurySplitAvailable` enum exists
                .withCjsVerdictCode("A")
                .build();
    }
}
