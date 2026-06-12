package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;


import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;

import java.util.List;

import javax.inject.Inject;

public class InitiationTypesRefDataEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final List<String> initiationTypesRef = referenceDataQueryService.getInitiationCodes();

        prosecutionWithReferenceDataList.forEach(each -> {
            final String initiationCode = each.getProsecution().getCaseDetails().getInitiationCode();

            final List<String> initiationTypes = initiationTypesRef.stream()
                    .filter(code -> code.equals(initiationCode))
                    .collect(toList());

            each.getReferenceDataVO().setInitiationTypes(initiationTypes);
        });
    }
}
