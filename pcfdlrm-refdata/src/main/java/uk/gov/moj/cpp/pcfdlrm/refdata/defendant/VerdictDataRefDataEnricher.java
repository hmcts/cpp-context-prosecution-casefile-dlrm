package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class VerdictDataRefDataEnricher implements DefendantRefDataEnricher {

    private static final String XHIBIT = "XHIBIT";

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> prosecutionWithReferenceDataList) {

        for (DefendantsWithReferenceData defendantsWithReferenceData : prosecutionWithReferenceDataList) {
            Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();

            final List<MigratedDefendant> defendants = defendantsWithReferenceData.getDefendants();
            for (MigratedDefendant migratedDefendant : defendants) {
                final List<MigratedOffence> offences = migratedDefendant.getOffences();
                Map<UUID, VerdictReferenceData> offenceVerdictReferenceDataMap = new HashMap<>();
                for (MigratedOffence offence : offences) {
                    process(defendantsWithReferenceData, offence, offenceVerdictReferenceDataMap);
                }
                if(!offenceVerdictReferenceDataMap.isEmpty()){
                    verdictReferenceDataMap.put(migratedDefendant.getId(),offenceVerdictReferenceDataMap);
                }

            }
            if (!verdictReferenceDataMap.isEmpty()) {
                defendantsWithReferenceData.getReferenceDataVO().setVerdictReferenceDataMap(verdictReferenceDataMap);
            }
        }

    }

    private void process(final DefendantsWithReferenceData defendantsWithReferenceData, final MigratedOffence offence, final Map<UUID, VerdictReferenceData> offenceVerdictReferenceDataMap) {
        final MigratedVerdict verdict = offence.getVerdict();
        if (hasVerdictId(verdict)) {
            referenceDataQueryService.getVerdictTypeById(verdict.getId())
                    .filter(e -> e.getJurisdiction() == getJurisdiction(defendantsWithReferenceData)
                            || e.getJurisdiction() == Jurisdiction.EITHER
                            || e.getJurisdiction() == Jurisdiction.MAGISTRATES)
                    .ifPresent(verdictReferenceData -> offenceVerdictReferenceDataMap.put(offence.getOffenceId(), verdictReferenceData));
        }
    }

    private boolean hasVerdictId(final MigratedVerdict verdict) {
        return verdict != null && verdict.getId() != null;
    }

    private Jurisdiction getJurisdiction(DefendantsWithReferenceData defendantsWithReferenceData ) {
        return defendantsWithReferenceData.getMigrationSourceSystemName().equals(XHIBIT) ? Jurisdiction.CROWN : Jurisdiction.MAGISTRATES;
    }
}
