package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class ProsecutorRefDataEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId = new HashMap<>();
        final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority = new HashMap<>();

        prosecutionWithReferenceDataList.forEach(
                each -> {
                    final Prosecutor prosecutor = each.getProsecution().getCaseDetails().getProsecutor();
                    if (nonNull(prosecutor)) {
                        ProsecutorsReferenceData prosecutorsReferenceData = getRefProsecutorFromMap(prosecutor, refProsecutorByAuthorityId, refProsecutorByAuthority);

                        if (isNull(prosecutorsReferenceData)) {
                            prosecutorsReferenceData = this.getRefProsecutor(prosecutor);
                            putRefProsecutorToMap(prosecutor, prosecutorsReferenceData, refProsecutorByAuthorityId, refProsecutorByAuthority);
                        }

                        each.getReferenceDataVO().setProsecutorsReferenceData(prosecutorsReferenceData);
                    }

                }

        );
    }

    private ProsecutorsReferenceData getRefProsecutorFromMap(final Prosecutor prosecutor, final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId, final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority){
            return refProsecutorByAuthority.get(prosecutor.getProsecutingAuthority());
    }

    private void putRefProsecutorToMap(final Prosecutor prosecutor, final ProsecutorsReferenceData prosecutorsReferenceData, final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId, final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority){
        if (nonNull(prosecutor.getProsecutingAuthority())) {
            refProsecutorByAuthority.put(prosecutor.getProsecutingAuthority(), prosecutorsReferenceData);
        }
    }

    private ProsecutorsReferenceData getRefProsecutor(final Prosecutor prosecutor) {
        return referenceDataQueryService.retrieveProsecutors(prosecutor.getProsecutingAuthority());
    }
}
