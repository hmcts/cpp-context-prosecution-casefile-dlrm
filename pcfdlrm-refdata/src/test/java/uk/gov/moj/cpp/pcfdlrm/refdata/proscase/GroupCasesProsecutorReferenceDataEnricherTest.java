package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupCasesProsecutorReferenceDataEnricherTest {

    private static final String ORIGINATING_ORGANISATION = "ORIGINATING_ORGANISATION";
    private static final UUID AUTHORITY_ID = randomUUID();

    @InjectMocks
    private GroupCasesProsecutorReferenceDataEnricher groupCasesProsecutorReferenceDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void shouldPopulateProsecutorWhenOuCodeIsNotNull() {

        when(referenceDataQueryService.retrieveProsecutors(ORIGINATING_ORGANISATION)).thenReturn(getMockProsecutionRefData());

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(true), getMockProsecutionWithReferenceData(true));
        groupCasesProsecutorReferenceDataEnricher.enrich(prosecutionWithReferenceDataList);

        verify(referenceDataQueryService, times(1)).retrieveProsecutors(ORIGINATING_ORGANISATION);
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
    }

    @Test
    void shouldPopulateCivilProsecutor() {

        when(referenceDataQueryService.retrieveProsecutors(any(String.class))).thenReturn(getMockProsecutionRefData());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithCivilAuthority();
        groupCasesProsecutorReferenceDataEnricher.enrich(prosecutionWithReferenceData);

        verify(referenceDataQueryService, times(1)).retrieveProsecutors(any(String.class));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

    }

    private ProsecutorsReferenceData getMockProsecutionRefData() {
        return ProsecutorsReferenceData.prosecutorsReferenceData()
                .withFullName("Blake Austin")
                .withShortName("Blake")
                .withMajorCreditorCode("1L")
                .withSequenceNumber(1)
                .withId(randomUUID())
                .withContactEmailAddress("contact@cpp.co.uk")
                .build();
    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData(boolean withOuCode) {

        final Prosecutor.Builder prosecutorBuilder = Prosecutor.prosecutor();
            prosecutorBuilder.withProsecutingAuthority(ORIGINATING_ORGANISATION);

        return new ProsecutionWithReferenceData(
                Prosecution.prosecution()
                        .withCaseDetails(
                                CaseDetails.caseDetails()
                                        .withProsecutor(prosecutorBuilder.build())
                                        .build())
                        .build());
    }

    private ProsecutionWithReferenceData getProsecutionWithCivilAuthority() {

        return new ProsecutionWithReferenceData(Prosecution.prosecution()
                .withCaseDetails(
                        CaseDetails.caseDetails()
                                .withProsecutor(
                                        Prosecutor.prosecutor()
                                                .withProsecutingAuthority("THREE_RIVER")
                                                .build()
                                )
                                .build()
                )
                .build());
    }

}
