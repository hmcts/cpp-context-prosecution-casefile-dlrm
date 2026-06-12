package uk.gov.moj.cpp.pcfdlrm.refdata.hearing;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.*;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganisationUnitWithCourtroomRefDataEnricherTest {

    @InjectMocks
    private OrganisationUnitWithCourtroomRefDataEnricher organisationUnitWithCourtroomRefDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void testShouldPopulateOrganisationUnitWithCourtroomsWhenOuCodeFound() {
        String ouCode = "ouCode1";
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(ouCode)).thenReturn(getMockOrganisationUnitsWithCourtrooms(ouCode));

        final List<MigratedHearingWithReferenceData> migratedhearingWithReferenceDataList = asList(getMigratedHearingWithReferenceData(ouCode), getMigratedHearingWithReferenceData(ouCode));

        organisationUnitWithCourtroomRefDataEnricher.enrich(migratedhearingWithReferenceDataList);
        assertNotNull(migratedhearingWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData());
        assertThat(migratedhearingWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().isPresent(), is(true));
        assertThat(migratedhearingWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().get(), isA(OrganisationUnitWithCourtroomsReferenceData.class));

        assertNotNull(migratedhearingWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData());
        assertThat(migratedhearingWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().isPresent(), is(true));
        assertThat(migratedhearingWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().get(), isA(OrganisationUnitWithCourtroomsReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveOrganisationUnitWithCourtrooms(ouCode);
    }

    @Test
    void testShouldNotPopulateOrganisationUnitWithCourtroomsWhenOuCodeNotExistInTheRequest() {
        String ouCode = null;

        final MigratedHearingWithReferenceData defendantsWithReferenceData = getMigratedHearingWithReferenceData(ouCode);
        organisationUnitWithCourtroomRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().isPresent(), is(false));
        verifyNoInteractions(referenceDataQueryService);

    }


    @Test
    void testShouldNotPopulateOrganisationUnitWithCourtroomsWhenOuCodeLengthIsNotValid() {
        String ouCode = "ouCode";
        final MigratedHearingWithReferenceData defendantsWithReferenceData = getMigratedHearingWithReferenceData(ouCode);
        organisationUnitWithCourtroomRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomsReferenceData().isPresent(), is(false));
        verifyNoInteractions(referenceDataQueryService);

    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceData(final String ouCode) {

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withHearingType("CODE123")
                .withCourtHearingLocation(ouCode)
                .build();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        MigratedHearingWithReferenceData hearingWithRefData = new MigratedHearingWithReferenceData();
        hearingWithRefData.setMigratedHearing(migratedHearing);
        hearingWithRefData.setReferenceDataVO(referenceDataVO);
        return hearingWithRefData;
    }

    private Optional<OrganisationUnitWithCourtroomsReferenceData> getMockOrganisationUnitsWithCourtrooms(String ouCode) {
        return of(organisationUnitWithCourtroomsReferenceData()
                .withOucode(ouCode)
                .withId(randomUUID().toString())
                .build());
    }

}
