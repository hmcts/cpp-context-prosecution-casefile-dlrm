package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;


import static java.util.stream.Collectors.toList;


import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;


public class ObservedEthnicityRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final List<ObservedEthnicityReferenceData> refEthnicities = referenceDataQueryService.retrieveObservedEthnicity();

        for(final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList) {
            final List<String> ethnicityList = defendantsWithReferenceData.getDefendants().stream()
                    .filter(x -> x.getIndividual() != null
                            && x.getIndividual().getPersonalInformation() != null
                            && x.getIndividual().getPersonalInformation().getObservedEthnicity() != null
                    ).map(x -> x.getIndividual().getPersonalInformation().getObservedEthnicity().toString())
                    .collect(toList());

            final List<String> ethnicityParentGuardianList = defendantsWithReferenceData.getDefendants().stream()
                    .filter(x -> x.getIndividual() != null
                            && x.getIndividual().getParentGuardianInformation() != null
                            && x.getIndividual().getParentGuardianInformation().getObservedEthnicity() != null
                    ).map(x -> x.getIndividual().getParentGuardianInformation().getObservedEthnicity())
                    .collect(toList());

            if (!ethnicityList.isEmpty() || !ethnicityParentGuardianList.isEmpty()) {
                final List<ObservedEthnicityReferenceData> ethnicities = refEthnicities.stream()
                        .filter(observedEthnicityReferenceData -> (ethnicityList.contains(observedEthnicityReferenceData.getEthnicityCode())) ||
                                ethnicityParentGuardianList.stream().anyMatch(observedEthnicityReferenceData.getEthnicityCode()::equalsIgnoreCase))
                        .collect(Collectors.toList());
                defendantsWithReferenceData.getReferenceDataVO().setObservedEthnicityReferenceData(ethnicities);
            }
        }
    }
}
