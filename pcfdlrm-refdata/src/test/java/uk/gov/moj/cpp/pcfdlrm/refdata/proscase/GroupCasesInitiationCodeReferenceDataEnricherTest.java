package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupCasesInitiationCodeReferenceDataEnricherTest {

    private static final List<String> INITIATION_CODES = asList("C", "S");

    @InjectMocks
    private GroupCasesInitiationCodeReferenceDataEnricher groupCasesInitiationCodeReferenceDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void shouldPopulateInitiationCodeReferenceData() {

        when(referenceDataQueryService.getInitiationCodes()).thenReturn(INITIATION_CODES);

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(), getMockProsecutionWithReferenceData());
        groupCasesInitiationCodeReferenceDataEnricher.enrich(prosecutionWithReferenceDataList);

        verify(referenceDataQueryService, times(1)).getInitiationCodes();
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().size(), is(2));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().get(0), is("C"));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().get(1), is("S"));

        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().size(), is(2));
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().get(0), is("C"));
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().get(1), is("S"));

    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData() {

        return new ProsecutionWithReferenceData(
                Prosecution.prosecution()
                        .withCaseDetails(
                                CaseDetails.caseDetails()
                                        .build()
                        )
                        .build()
        );

    }

}