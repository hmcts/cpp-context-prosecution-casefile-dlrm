package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaDataRefDataEnricherTest {

    @InjectMocks
    private PleaDataRefDataEnricher pleaDataRefDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void shouldEnrichPleaTypes() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID pleaId = UUID.randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(pleaId)
                        .withPleaDate(LocalDate.now())
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

        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withId(pleaId)
                .withJurisdiction(Jurisdiction.CROWN)
                .withPleaTypeCode("VALID_CODE")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Valid Plea Value")
                .build();

        when(referenceDataQueryService.getPleaTypeById(pleaId)).thenReturn(Optional.of(pleaReferenceData));

        List<DefendantsWithReferenceData> defendantsWithReferenceDataList = Collections.singletonList(defendantsWithReferenceData);

        pleaDataRefDataEnricher.enrich(defendantsWithReferenceDataList);

        // Assertions
        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = defendantsWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap();
        assertNotNull(pleaReferenceDataMap);
        assertTrue(pleaReferenceDataMap.containsKey(defendantId));
        assertTrue(pleaReferenceDataMap.get(defendantId).containsKey(offenceId));
        assertEquals("Valid Plea Value", pleaReferenceDataMap.get(defendantId).get(offenceId).getPleaValue());
    }

    @Test
    void shouldEnrichIndicatedPleaPleaTypes() {
        UUID defendantId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID pleaId = UUID.randomUUID();

        MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(offenceId)
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(pleaId)
                        .withPleaDate(LocalDate.now())
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

        PleaReferenceData pleaReferenceData = PleaReferenceData.pleaReferenceData()
                .withId(pleaId)
                .withJurisdiction(Jurisdiction.EITHER)
                .withPleaTypeCode("IG")
                .withPleaTypeGuiltyFlag("Yes")
                .withPleaValue("Indicated Plea")
                .build();

        when(referenceDataQueryService.getPleaTypeById(pleaId)).thenReturn(Optional.of(pleaReferenceData));

        List<DefendantsWithReferenceData> defendantsWithReferenceDataList = Collections.singletonList(defendantsWithReferenceData);

        pleaDataRefDataEnricher.enrich(defendantsWithReferenceDataList);

        // Assertions
        Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = defendantsWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap();
        assertNotNull(pleaReferenceDataMap);
        assertTrue(pleaReferenceDataMap.containsKey(defendantId));
        assertTrue(pleaReferenceDataMap.get(defendantId).containsKey(offenceId));
        assertEquals("Indicated Plea", pleaReferenceDataMap.get(defendantId).get(offenceId).getPleaValue());
    }

}
