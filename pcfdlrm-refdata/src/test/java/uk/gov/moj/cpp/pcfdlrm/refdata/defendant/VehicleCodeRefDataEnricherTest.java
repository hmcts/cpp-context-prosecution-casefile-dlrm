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
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleRelatedOffence;
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
 class VehicleCodeRefDataEnricherTest {
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String VEHICLE_CODE = "1";
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private VehicleCodeRefDataEnricher vehicleCodeRefDataEnricher;

    @Test
     void testShouldPopulateVehicleCodeRefDataWhenVehicleCodeFound() {
        when(referenceDataQueryService.retrieveVehicleCodes()).thenReturn(getMockVehicleCodeReferenceData());
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(VEHICLE_CODE), getMockDefendantsWithReferenceData(VEHICLE_CODE));
        vehicleCodeRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().get(0), isA(VehicleCodeReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().get(0).getCode(), is(VEHICLE_CODE));

        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().get(0), isA(VehicleCodeReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().get(0).getCode(), is(VEHICLE_CODE));
        verify(referenceDataQueryService, times(1)).retrieveVehicleCodes();
    }

    @Test
     void testShouldNotPopulateVehicleCodeRefDataWhenVehicleCodeIsNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(null);
        vehicleCodeRefDataEnricher.enrich(defendantsWithReferenceData);
        assertTrue(defendantsWithReferenceData.getReferenceDataVO().getVehicleCodesReferenceData().isEmpty());
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(String vehicleCode) {
        final VehicleRelatedOffence vehicleRelatedOffence = VehicleRelatedOffence.vehicleRelatedOffence().withVehicleCode(vehicleCode).build();
        final MigratedOffence offence = MigratedOffence.migratedOffence().withVehicleRelatedOffence(vehicleRelatedOffence).build();
        final List<MigratedOffence> offences = new ArrayList<>();
        offences.add(offence);

        final MigratedDefendant defendant =  MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withOffences(offences).build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }

    private List<VehicleCodeReferenceData> getMockVehicleCodeReferenceData() {
        List<VehicleCodeReferenceData> vehicleCodeReferenceData = new ArrayList<>();
        vehicleCodeReferenceData.add(getVehicleCodeReferenceData("1"));
        vehicleCodeReferenceData.add(getVehicleCodeReferenceData("2"));
        return vehicleCodeReferenceData;
    }

    private VehicleCodeReferenceData getVehicleCodeReferenceData(String code) {
        return VehicleCodeReferenceData.vehicleCodeReferenceData()
                .withCode(code)
                .withDescription("Large Goods Vehicle")
                .withId(UUID.randomUUID())
                .withSeqNum(20)
                .withValidFrom("2019-04-01")
                .build();
    }
}