package uk.gov.moj.cpp.pcfdlrm.refdata.hearing;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

public class MigratedHearingTypesRefDataEnricher implements MigratedHearingRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;


    @Override
    public void enrich(final List<MigratedHearingWithReferenceData> prosecutionWithReferenceDataList) {
        final HearingTypes hearingTypes = referenceDataQueryService.retrieveHearingTypes();

        prosecutionWithReferenceDataList.forEach(migratedHearingRefDataEnricher -> {
                    final MigratedHearing hearing = migratedHearingRefDataEnricher.getMigratedHearing();
                    final Optional<HearingType> hearingTypeReferenceData = findHearingType(hearing, hearingTypes);
                    hearingTypeReferenceData.ifPresent(migratedHearingRefDataEnricher.getReferenceDataVO()::setHearingType);
                }

        );

    }

    private Optional<HearingType> findHearingType(final MigratedHearing migratedHearing, final HearingTypes hearingTypes) {
        return hearingTypes.getHearingtypes()
                .stream().filter(x -> x.getHearingCode().equalsIgnoreCase(migratedHearing.getHearingType())).findAny();
    }

}
