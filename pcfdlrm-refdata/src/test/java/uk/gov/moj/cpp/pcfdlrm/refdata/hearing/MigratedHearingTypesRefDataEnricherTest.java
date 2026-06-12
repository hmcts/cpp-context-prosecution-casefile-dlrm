package uk.gov.moj.cpp.pcfdlrm.refdata.hearing;

import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigratedHearingTypesRefDataEnricherTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private MigratedHearingTypesRefDataEnricher enricher;

    @Test
    void shouldEnrichWithMatchingHearingType() {
        HearingType hearingType = HearingType.hearingType().
                withHearingCode("CODE123").
                build();

        HearingTypes hearingTypes = new HearingTypes(List.of(hearingType));

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withHearingType("CODE123")
                .build();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        MigratedHearingWithReferenceData hearingWithRefData = new MigratedHearingWithReferenceData();
        hearingWithRefData.setMigratedHearing(migratedHearing);
        hearingWithRefData.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        // Act
        enricher.enrich(Collections.singletonList(hearingWithRefData));

        // Assert
        Assertions.assertEquals("CODE123", referenceDataVO.getHearingType().getHearingCode());
    }

    @Test
    void shouldNotEnrichWhenNoMatchingHearingType() {
        HearingType hearingType = HearingType.hearingType().
                withHearingCode("CODE123").
                build();

        HearingTypes hearingTypes = new HearingTypes(List.of(hearingType));

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withHearingType("NO_CODE")
                .build();

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        MigratedHearingWithReferenceData hearingWithRefData = new MigratedHearingWithReferenceData();
        hearingWithRefData.setMigratedHearing(migratedHearing);
        hearingWithRefData.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        // Act
        enricher.enrich(Collections.singletonList(hearingWithRefData));

        // Assert
        Assertions.assertNull(referenceDataVO.getHearingType());
    }
}
