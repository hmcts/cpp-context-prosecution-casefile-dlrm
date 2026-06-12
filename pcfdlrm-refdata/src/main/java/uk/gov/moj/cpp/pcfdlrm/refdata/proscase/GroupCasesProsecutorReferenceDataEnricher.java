package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

import java.util.List;

import javax.inject.Inject;

public class GroupCasesProsecutorReferenceDataEnricher implements GroupCasesReferenceDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {

        final Prosecutor prosecutor = prosecutionWithReferenceDataList.stream()
                .map(ProsecutionWithReferenceData::getProsecution)
                .map(Prosecution::getCaseDetails)
                .map(CaseDetails::getProsecutor)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Prosecutor not present"));

        final ProsecutorsReferenceData prosecutorsReferenceData = this.getRefProsecutor(prosecutor);
        if (nonNull(prosecutorsReferenceData)) {
            prosecutionWithReferenceDataList.forEach(each -> each.getReferenceDataVO().setProsecutorsReferenceData(prosecutorsReferenceData));
        }

    }

    private ProsecutorsReferenceData getRefProsecutor(final Prosecutor prosecutor) {
        return referenceDataQueryService.retrieveProsecutors(prosecutor.getProsecutingAuthority());
    }
}
