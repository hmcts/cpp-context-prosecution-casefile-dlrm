package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
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
class ProsecutorRefDataEnricherTest {

    private static final String ORIGINATING_ORGANISATION = "ORIGINATING_ORGANISATION";
    private static final UUID AUTHORITY_ID = randomUUID();
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private ProsecutorRefDataEnricher prosecutorRefDataEnricher;

    @Test
    void shouldPopulateProsecutorWhenOuCodeIsNotNull() {

        when(referenceDataQueryService.retrieveProsecutors(ORIGINATING_ORGANISATION)).thenReturn(getMockProsecutionRefData());

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(true),
                getMockProsecutionWithReferenceData(true));

        prosecutorRefDataEnricher.enrich(prosecutionWithReferenceDataList);
        assertNotNull(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

        assertNotNull(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveProsecutors(ORIGINATING_ORGANISATION);
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

        Prosecutor.Builder prosecutorBuilder = Prosecutor.prosecutor();
            prosecutorBuilder.withProsecutingAuthority(ORIGINATING_ORGANISATION);
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(prosecutorBuilder.build())
                .build();
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(caseDetails).build();

        return new ProsecutionWithReferenceData(prosecution);
    }

    private ProsecutionWithReferenceData getMockNspProsecutionWithReferenceData() {

        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(Prosecutor.prosecutor().withProsecutingAuthority("NSP-ORG").build())
                .build();
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(caseDetails).build();

        return new ProsecutionWithReferenceData(prosecution);
    }

}
