package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CourtLocationEnricherTest {


    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Prosecution prosecution;
    private final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
    @InjectMocks
    private CourtLocationEnricher courtLocationEnricher;

    @Test
    void shouldEnrichWithSendingCourtAndReceivingCourt() {
        String ouCode= "ABCDE00";
        String courtId = UUID.randomUUID().toString();
        final OrganisationUnitReferenceData organisationUnitReferenceData =
                OrganisationUnitReferenceData.organisationUnitReferenceData().withOucode(ouCode).withId(courtId).build();
        when(referenceDataQueryService.retrieveOrganisationUnits(ouCode))
                .thenReturn(List.of(organisationUnitReferenceData));
        when(prosecution.getCaseDetails().getSendingCourt()).thenReturn(ouCode);
        when(prosecution.getCaseDetails().getReceivingCourt()).thenReturn(ouCode);

        courtLocationEnricher.enrich(List.of(new ProsecutionWithReferenceData(prosecution, referenceDataVO)));

        assertThat(referenceDataVO.getSendingCourtOrganisationUnit().isPresent(), is(true));
        assertThat(referenceDataVO.getSendingCourtOrganisationUnit().get(), is(organisationUnitReferenceData));

        assertThat(referenceDataVO.getReceivingCourtOrganisationUnit().isPresent(), is(true));
        assertThat(referenceDataVO.getReceivingCourtOrganisationUnit().get(), is(organisationUnitReferenceData));

        verify(referenceDataQueryService, times(2)).retrieveOrganisationUnits(ouCode);
    }

    @Test
    void shouldNotEnrichWithSendingCourtAndReceivingCourtDoesNotMatchWithReferenceData() {
        String ouCode= "ABCDE00";
        String courtId = UUID.randomUUID().toString();
        final OrganisationUnitReferenceData organisationUnitReferenceData =
                OrganisationUnitReferenceData.organisationUnitReferenceData().withOucode(ouCode).withId(courtId).build();
        when(referenceDataQueryService.retrieveOrganisationUnits(ouCode))
                .thenReturn(List.of());
        when(prosecution.getCaseDetails().getSendingCourt()).thenReturn(ouCode);
        when(prosecution.getCaseDetails().getReceivingCourt()).thenReturn(ouCode);

        courtLocationEnricher.enrich(List.of(new ProsecutionWithReferenceData(prosecution, referenceDataVO)));

        assertThat(referenceDataVO.getSendingCourtOrganisationUnit().isEmpty(), is(true));
        assertThat(referenceDataVO.getReceivingCourtOrganisationUnit().isEmpty(), is(true));

        verify(referenceDataQueryService, times(2)).retrieveOrganisationUnits(ouCode);
    }


}