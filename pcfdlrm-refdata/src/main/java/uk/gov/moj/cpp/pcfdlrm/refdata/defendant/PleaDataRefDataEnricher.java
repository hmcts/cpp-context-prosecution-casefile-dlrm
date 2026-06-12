package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Jurisdiction;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class PleaDataRefDataEnricher implements DefendantRefDataEnricher {

    private static final String XHIBIT = "XHIBIT";

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> prosecutionWithReferenceDataList) {

        for (DefendantsWithReferenceData defendantsWithReferenceData : prosecutionWithReferenceDataList) {
            Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();

            final List<MigratedDefendant> defendants = defendantsWithReferenceData.getDefendants();

            for (MigratedDefendant migratedDefendant : defendants) {
                final List<MigratedOffence> offences = migratedDefendant.getOffences();
                Map<UUID, PleaReferenceData> offencePleaReferenceDataMap = new HashMap<>();
                for (MigratedOffence offence : offences) {
                    process(defendantsWithReferenceData, offence, offencePleaReferenceDataMap);
                }
                if(!offencePleaReferenceDataMap.isEmpty()){
                    pleaReferenceDataMap.put(migratedDefendant.getId(),offencePleaReferenceDataMap);
                }

            }
            if (!pleaReferenceDataMap.isEmpty()) {
                defendantsWithReferenceData.getReferenceDataVO().setPleaReferenceDataMap(pleaReferenceDataMap);
            }
        }

    }

    private void process(final DefendantsWithReferenceData defendantsWithReferenceData, final MigratedOffence offence, final Map<UUID, PleaReferenceData> offencePleaReferenceDataMap) {
        final MigratedPlea plea = offence.getPlea();
        if (hasPleaId(plea)) {
            referenceDataQueryService.getPleaTypeById(plea.getId())
                    .filter(e -> e.getJurisdiction() == getJurisdiction(defendantsWithReferenceData)
                            || e.getJurisdiction() == Jurisdiction.EITHER
                            || "IG".equalsIgnoreCase(e.getPleaTypeCode()))
                    .ifPresent(pleaReferenceData -> offencePleaReferenceDataMap.put(offence.getOffenceId(), pleaReferenceData));
        }
    }

    private boolean hasPleaId(final MigratedPlea plea) {
        return plea != null && plea.getId() != null;
    }

    private Jurisdiction getJurisdiction(DefendantsWithReferenceData defendantsWithReferenceData ) {
        return defendantsWithReferenceData.getMigrationSourceSystemName().equals(XHIBIT) ? Jurisdiction.CROWN : Jurisdiction.MAGISTRATES;
    }
}
