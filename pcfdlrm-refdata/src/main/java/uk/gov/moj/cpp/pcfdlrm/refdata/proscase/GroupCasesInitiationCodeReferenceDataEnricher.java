package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

import java.util.List;

import javax.inject.Inject;

public class GroupCasesInitiationCodeReferenceDataEnricher implements GroupCasesReferenceDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final List<String> initiationTypes = referenceDataQueryService.getInitiationCodes();
        prosecutionWithReferenceDataList.forEach(each -> each.getReferenceDataVO().setInitiationTypes(initiationTypes));
    }
}
