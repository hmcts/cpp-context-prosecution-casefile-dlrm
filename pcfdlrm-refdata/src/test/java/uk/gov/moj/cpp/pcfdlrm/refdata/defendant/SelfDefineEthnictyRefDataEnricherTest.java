package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class SelfDefineEthnictyRefDataEnricherTest {


    private static final UUID DEFENDANT_ID = randomUUID();
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private SelfDefineEthnictyRefDataEnricher selfDefineEthnictyRefDataEnricher;

    @Test
     void testShouldPopulateSelfDefinedEthnicityWhenEthnicityFound() {
        when(referenceDataQueryService.retrieveSelfDefinedEthnicity()).thenReturn(getMockSelfDefinedEthnicityRefData());

        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData("W1"), getMockDefendantsWithReferenceData("W1"));

        selfDefineEthnictyRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getSelfdefinedEthnicityReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0), isA(SelfdefinedEthnicityReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0).getCode(), is("W1"));

        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getSelfdefinedEthnicityReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0), isA(SelfdefinedEthnicityReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0).getCode(), is("W1"));
        verify(referenceDataQueryService, times(1)).retrieveSelfDefinedEthnicity();
    }

    @Test
     void testShouldPopulateSelfDefinedEthnicityWhenParentGuardianSelfDefinedEthnicityFound() {
        when(referenceDataQueryService.retrieveSelfDefinedEthnicity()).thenReturn(getMockSelfDefinedEthnicityRefData());

        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithParentGuardianReferenceData("W1");
        selfDefineEthnictyRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0), isA(SelfdefinedEthnicityReferenceData.class));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData().get(0).getCode(), is("W1"));
        verify(referenceDataQueryService).retrieveSelfDefinedEthnicity();
    }

    @Test
     void testShouldNotPopulateSelfDefinedEthnicityWhenEthnicityIsNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(null);
        selfDefineEthnictyRefDataEnricher.enrich(defendantsWithReferenceData);
        assertTrue(defendantsWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData().isEmpty());
    }

    private List<SelfdefinedEthnicityReferenceData> getMockSelfDefinedEthnicityRefData() {
        List<SelfdefinedEthnicityReferenceData> referenceData = new ArrayList<>();
        referenceData.add(getSelfDefinedEthnicityRefData("W1"));
        referenceData.add(getSelfDefinedEthnicityRefData("W2"));
        return referenceData;
    }

    private SelfdefinedEthnicityReferenceData getSelfDefinedEthnicityRefData(String code) {
        return SelfdefinedEthnicityReferenceData.selfdefinedEthnicityReferenceData()
                .withCode(code)
                .withDescription("British")
                .withId(UUID.randomUUID())
                .build()
                ;
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(String ethnicity) {
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withEthnicity(ethnicity).build();
        final Individual individual = Individual.individual().withSelfDefinedInformation(selfDefinedInformation).build();
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withIndividual(individual).build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }

    private DefendantsWithReferenceData getMockDefendantsWithParentGuardianReferenceData(final String ethnicity) {
        final ParentGuardianInformation parentGuardianInformation = ParentGuardianInformation.parentGuardianInformation().withSelfDefinedEthnicity(ethnicity).build();
        final Individual individual = Individual.individual().withParentGuardianInformation(parentGuardianInformation).build();
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withIndividual(individual).build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }
}