package uk.gov.moj.cpp.pcfdlrm.refdata.hearing;

import static java.util.Objects.nonNull;

import uk.gov.justice.cps.prosecution.casefile.dlrm.InitialHearing;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

public class OrganisationUnitWithCourtroomRefDataEnricher implements MigratedHearingRefDataEnricher {

    private static final int COURT_HEARING_OU_CODE_LENGTH = 7;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<MigratedHearingWithReferenceData> prosecutionWithReferenceDataList) {
        final Map<String, Optional<OrganisationUnitWithCourtroomsReferenceData>> organisationUnitWithCourtroomMap = new HashMap<>();

        prosecutionWithReferenceDataList.forEach(migratedHearingWithReferenceData -> {

                    final String ouCode = migratedHearingWithReferenceData.getMigratedHearing() != null ? migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation() : null;

                    if (isValidOuCode(ouCode) && !migratedHearingWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().isPresent()) {
                        if (!organisationUnitWithCourtroomMap.containsKey(ouCode)) {
                            final Optional<OrganisationUnitWithCourtroomsReferenceData> optionalOrganisationUnitWithCourtroomReferenceData = referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(ouCode);
                            organisationUnitWithCourtroomMap.put(ouCode, optionalOrganisationUnitWithCourtroomReferenceData);
                        }

                        final Optional<OrganisationUnitWithCourtroomsReferenceData> optionalOrganisationUnitWithCourtroomReferenceData = organisationUnitWithCourtroomMap.get(ouCode);
                        optionalOrganisationUnitWithCourtroomReferenceData.ifPresent(organisationUnitWithCourtroomReferenceData -> migratedHearingWithReferenceData.getReferenceDataVO().setOrganisationUnitWithCourtroomsReferenceData(organisationUnitWithCourtroomReferenceData));
                    }

                }
                );
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGTH == ouCode.length();
    }


}