package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffenceRefDataEnricherTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_UUID = randomUUID();
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private OffenceDataRefDataEnricher offenceDataRefDataEnricher;

    @BeforeEach
     void setup() {
        when(referenceDataQueryService.retrieveOffenceDataList(any())).thenReturn(getMockOffenceReferenceData());
    }

    @Test
     void shouldPopulateOffenceRefData() {
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(buildOffence(), null),
                getMockDefendantsWithReferenceData(buildOffence(), null));

        offenceDataRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(0).getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceDataList.get(0).getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(1).getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceDataList.get(1).getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffence().getOffenceCode()));
    }

    @Test
     void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenEmpty() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(buildOffenceWithEmptyOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithEmptyOffenceLocation().getOffenceCode()));
    }

    @Test
     void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(buildOffenceWithNullOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithNullOffenceLocation().getOffenceCode()));
    }

    @Test
     void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenSpace() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(buildOffenceWithSpacedOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithSpacedOffenceLocation().getOffenceCode()));
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(final MigratedOffence offence, final String prosecutionAuthorityShortName) {
        final List<MigratedOffence> offences = new ArrayList<>();
        offences.add(offence);

        final MigratedDefendant defendant = new MigratedDefendant.Builder().withId(DEFENDANT_ID).withOffences(offences).withInitiationCode("S").build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendants, prosecutionAuthorityShortName);
        defendantsWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withInitiationCode("P").build());
        return defendantsWithReferenceData;
    }

    @Test
     void shouldPopulateOffenceRefDataOnceWhenDuplicateOffencesPresent() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithSameOffences(buildOffence(), null);
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffence().getOffenceCode()));
    }

    private DefendantsWithReferenceData getMockDefendantsWithSameOffences(final MigratedOffence offence, final String prosecutionAuthorityShortName) {
        final List<MigratedOffence> offences = new ArrayList<>();
        offences.add(offence);

        final MigratedDefendant defendant =  MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withOffences(offences).withInitiationCode("S").build();
        final MigratedDefendant defendant1 =  MigratedDefendant.migratedDefendant().withId(randomUUID()).withOffences(offences).withInitiationCode("S").build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        defendants.add(defendant1);
        DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendants, prosecutionAuthorityShortName);
        defendantsWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withInitiationCode("P").build());
        return defendantsWithReferenceData;
    }

    private MigratedOffence buildOffence() {
        return MigratedOffence.migratedOffence().withOffenceCode("cjsOffenceCode").withMaxPenalty("Max Penalty").build();
    }

    private MigratedOffence buildOffenceWithEmptyOffenceLocation() {
        return MigratedOffence.migratedOffence()
                .withOffenceCode("cjsOffenceCode2")
                .withOffenceLocation("").build();
    }

    private MigratedOffence buildOffenceWithSpacedOffenceLocation() {
        return MigratedOffence.migratedOffence()
                .withOffenceCode("cjsOffenceCode3")
                .withOffenceLocation(" ").build();
    }

    private MigratedOffence buildOffenceWithNullOffenceLocation() {
        return MigratedOffence.migratedOffence()
                .withOffenceCode("cjsOffenceCode4")
                .withOffenceLocation(null).build();
    }

    private List<OffenceReferenceData> getMockOffenceReferenceData() {
        return asList(OffenceReferenceData
                .offenceReferenceData()
                .withCjsOffenceCode("cjsOffenceCode")
                .withOffenceId(OFFENCE_UUID)
                .withValidFrom("2019-04-01")
                .build()
        );
    }
}
