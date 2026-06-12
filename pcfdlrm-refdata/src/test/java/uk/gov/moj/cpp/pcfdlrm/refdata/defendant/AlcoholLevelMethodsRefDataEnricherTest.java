package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlcoholLevelMethodsRefDataEnricherTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String ALCOHOL_LEVEL_METHOD = "A";
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private AlcoholLevelMethodsRefDataEnricher alcoholLevelMethodsRefDataEnricher;

    @Test
    void shouldPopulateAlcoholLevelMethodRefDataWhenAlcoholLevelMethodFound() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(ALCOHOL_LEVEL_METHOD);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(getMockAlcoholLevelMethodsReferenceData());

        alcoholLevelMethodsRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData().get(0), isA(AlcoholLevelMethodReferenceData.class));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData().get(0).getMethodCode(), is(ALCOHOL_LEVEL_METHOD));

        verify(referenceDataQueryService, times(1)).retrieveAlcoholLevelMethods();
    }

    @Test
    void shouldPopulateAlcoholLevelMethodRefDataWhenAlcoholLevelMethodFoundMultipleData() {
        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(getMockAlcoholLevelMethodsReferenceData());
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(ALCOHOL_LEVEL_METHOD), getMockDefendantsWithReferenceData(ALCOHOL_LEVEL_METHOD));
        alcoholLevelMethodsRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getAlcoholLevelMethodReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getAlcoholLevelMethodReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getAlcoholLevelMethodReferenceData().get(0), isA(AlcoholLevelMethodReferenceData.class));
        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getAlcoholLevelMethodReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getAlcoholLevelMethodReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getAlcoholLevelMethodReferenceData().get(0), isA(AlcoholLevelMethodReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveAlcoholLevelMethods();
    }

    @Test
    void shouldNotPopulateAlcoholLevelMethodRefDataWhenAlcoholLevelMethodIsNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(null);
        alcoholLevelMethodsRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNull(defendantsWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData());
        verify(referenceDataQueryService, times(0)).retrieveAlcoholLevelMethods();
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(final String alcoholLevelMethod) {
        final AlcoholRelatedOffence alcoholRelatedOffence = AlcoholRelatedOffence.alcoholRelatedOffence().withAlcoholLevelMethod(alcoholLevelMethod).build();
        final MigratedOffence offence = MigratedOffence.migratedOffence().withAlcoholRelatedOffence(alcoholRelatedOffence).build();
        final List<MigratedOffence> offences = new ArrayList<>();
        offences.add(offence);
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withOffences(offences).build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        return new DefendantsWithReferenceData(defendants);
    }

    private List<AlcoholLevelMethodReferenceData> getMockAlcoholLevelMethodsReferenceData() {
        return asList(getAlcoholRefData(ALCOHOL_LEVEL_METHOD),
                getAlcoholRefData("C")
        );
    }

    private AlcoholLevelMethodReferenceData getAlcoholRefData(String alcoholLeveLMethod) {
        return AlcoholLevelMethodReferenceData
                .alcoholLevelMethodReferenceData()
                .withId(UUID.randomUUID())
                .withMethodCode(alcoholLeveLMethod)
                .withMethodDescription("Blood")
                .withSeqNo(1)
                .build();
    }
}