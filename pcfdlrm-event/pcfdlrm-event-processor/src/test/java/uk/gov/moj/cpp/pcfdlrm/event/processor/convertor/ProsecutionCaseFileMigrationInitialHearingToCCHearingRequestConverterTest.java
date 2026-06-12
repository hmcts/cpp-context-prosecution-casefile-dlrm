package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant.listedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedWeekCommencingDate.migratedWeekCommencingDate;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.*;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedWeekCommencingDate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverterTest {

    @InjectMocks
    private ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("W","WELSH"),
                Arguments.of("E","ENGLISH")

        );
    }

    @Test
    void shouldConvertInitialHearingToCCHearingRequest() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter.convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(2, listHearingRequests.get(0).getWeekCommencingDate().getDuration());
        assertEquals(JurisdictionType.CROWN, listHearingRequests.get(0).getJurisdictionType());

    }

    @Test
    void shouldConvertInitialHearingToCCHearingRequestWhenNoWC() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(now(), null, true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter.convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getWeekCommencingDate());
        assertEquals(JurisdictionType.CROWN, listHearingRequests.get(0).getJurisdictionType());
    }


    @Test
    void shouldSetDefaultLanguageWhenLanguageIsUnrecognized() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(now(), null, true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter.convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(JurisdictionType.CROWN, listHearingRequests.get(0).getJurisdictionType());
        assertEquals("ENGLISH",listHearingRequests.get(0).getListDefendantRequests().get(0).getHearingLanguageNeeds().name());
    }

    @Test
    void shouldConvertInitialHearingToCCHearingRequestWhenOuCodeIsNotMatching() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData =
                getMigratedHearingWithReferenceData(true, false);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(0, listHearingRequests.size());

    }

    @Test
    void shouldNotConvertInitialHearingToCCHearingRequestWhenHearingDateIsPast() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData =
                getMigratedHearingWithReferenceData(now().minusDays(1), now(), true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(0, listHearingRequests.size());

    }

    @Test
    void shouldNotConvertInitialHearingToCCHearingRequestWhenHearingDateIsFuture() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData =
                getMigratedHearingWithReferenceData(now().plusDays(1), now(), true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());

    }

    @Test
    void shouldNotConvertInitialHearingToCCHearingRequestWhenHearingDateIsEmpty() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData =
                getMigratedHearingWithReferenceData(null, now(), true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());

    }

    @Test
    void shouldNotConvertInitialHearingToCCHearingRequestWhenCourtRoomDoesNotMatches() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceDataWithCourt();

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals("courtRoom", listHearingRequests.get(0).getCourtCentre().getRoomName());
    }

    @Test
    void shouldReturnEmptyListWhenInputIsNull() {
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(null, paramsVO);

        assertEquals(0, listHearingRequests.size());
    }

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(), paramsVO);

        assertEquals(0, listHearingRequests.size());
    }

    @Test
    void shouldNotConvertWhenHearingTypeIsNull() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        migratedHearingWithReferenceData.getReferenceDataVO().setHearingType(null);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(0, listHearingRequests.size());
    }

    @Test
    void shouldReturnMagistratesJurisdictionForNonXHIBIT() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("OTHER_SYSTEM");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(JurisdictionType.MAGISTRATES, listHearingRequests.get(0).getJurisdictionType());
    }

    @Test
    void shouldReturnMagistratesJurisdictionWhenOuCodeIsNull() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        UUID caseId = UUID.randomUUID();
        OrganisationUnitWithCourtroomsReferenceData organisationUnit =
                OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withAddress1("123 Justice St")
                        .withId(caseId.toString())
                        .withOucode("C43AY01")
                        .withOucodeL1Code(null) // Set to null
                        .withOucodeL3Name("District Courts")
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withId(UUID.randomUUID().toString()).withCourtroomId(1).withCourtroomName("01").build()))
                        .build();
        migratedHearingWithReferenceData.getReferenceDataVO().setOrganisationUnitWithCourtroomsReferenceData(organisationUnit);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(JurisdictionType.MAGISTRATES, listHearingRequests.get(0).getJurisdictionType());
    }

    @ParameterizedTest
    @MethodSource("data")
    void shouldHandleNullHearingLanguage(String hearingLanguage,String expectedLanguage) {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withHearingLanguage(hearingLanguage)
                .withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(UUID.randomUUID()).withProsecutorOffenceId("offId11").build()))
                .withId(UUID.randomUUID()).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, List.of(UUID.randomUUID()))));

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(expectedLanguage, listHearingRequests.get(0).getListDefendantRequests().get(0).getHearingLanguageNeeds().name());
    }

    @Test
    void shouldHandleNullDateOfHearing() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("10:05:01.001")
                .withDurationMinutes(60)
                .withHearingType("A")
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()));

        MigratedHearing migratedHearing = migratedHearingBuilder.build();
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getListedStartDateTime());
    }

    @Test
    void shouldHandleNullTimeOfHearing() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withDurationMinutes(60)
                .withHearingType("A")
                .withDateOfHearing(now().plusDays(1).toString())
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()));

        MigratedHearing migratedHearing = migratedHearingBuilder.build();
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getListedStartDateTime());
    }

    @Test
    void shouldHandleNullCourtRoom() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withTimeOfHearing("10:05:01")
                .withDurationMinutes(60)
                .withHearingType("A")
                .withDateOfHearing(now().plusDays(1).toString())
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()));
        // Don't set court room
        MigratedHearing migratedHearing = migratedHearingBuilder.build();
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getCourtCentre().getRoomName());
    }

    @Test
    void shouldHandleNullMigratedHearing() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        migratedHearingWithReferenceData.setMigratedHearing(null);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

       assertEquals(0,listHearingRequests.size());
    }

    @Test
    void shouldHandleNullCourtRoomInOrganisationUnit() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        // Set court room in organisation unit to null
        UUID caseId = UUID.randomUUID();
        OrganisationUnitWithCourtroomReferenceData organisationUnit =
                OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData()
                        .withAddress1("123 Justice St")
                        .withId(caseId.toString())
                        .withOucode("C43AY01")
                        .withOucodeL1Code("C")
                        .withOucodeL3Name("District Courts")
                        .withCourtRoom(null) // Set to null
                        .build();
        migratedHearingWithReferenceData.getReferenceDataVO().setOrganisationUnitWithCourtroomReferenceData(organisationUnit);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getCourtCentre().getRoomName());
    }

    @Test
    void shouldHandleEmptyListDefendantRequests() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        // Set empty list of defendants
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(0, listHearingRequests.size());
    }

    @Test
    void shouldHandleNullDurationMinutes() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        // Set duration minutes to null
        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("10:05:01")
                .withHearingType("A")
                .withDateOfHearing(now().plusDays(1).toString())
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()));
        // Don't set duration minutes
        MigratedHearing migratedHearing = migratedHearingBuilder.build();
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        // Should use default duration from hearing type
        assertEquals(migratedHearingWithReferenceData.getReferenceDataVO().getHearingType().getDefaultDurationMin(),
                listHearingRequests.get(0).getEstimateMinutes());
    }

    @Test
    void shouldHandlePastWeekCommencingDate() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(now().plusDays(1), now().minusDays(1), true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(0, listHearingRequests.size());
    }

    @Test
    void shouldHandleNullWeekCommencingDateStartDate() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);
        // Set week commencing date with null start date
        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("00:05:01")
                .withDurationMinutes(60)
                .withHearingType("A")
                .withDateOfHearing(now().plusDays(1).toString())
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()))
                .withWeekCommencingDate(migratedWeekCommencingDate()
                        .withDuration(2)
                        .withStartDate(null) // Set to null
                        .build());
        MigratedHearing migratedHearing = migratedHearingBuilder.build();
        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
    }

    @Test
    void shouldHandleNullSummonsApprovedOutcome() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);
        paramsVO.setSummonsApprovedOutcome(null);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getListDefendantRequests().get(0).getSummonsApprovedOutcome());
    }

    @Test
    void shouldHandleMultipleDefendants() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        // Add multiple defendants
        UUID caseId1 = UUID.randomUUID();
        UUID caseId2 = UUID.randomUUID();

        final MigratedDefendant defendant1 = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("E")
                .withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(caseId1).withProsecutorOffenceId("offId11").build()))
                .withId(caseId1).build();

        final MigratedDefendant defendant2 = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("W")
                .withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(caseId2).withProsecutorOffenceId("offId12").build()))
                .withId(caseId2).build();

        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(
                new MigratedDefendantWithOffences(defendant1, List.of(caseId1)),
                new MigratedDefendantWithOffences(defendant2, List.of(caseId2))
        ));

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(2, listHearingRequests.get(0).getListDefendantRequests().size());
        assertEquals("ENGLISH", listHearingRequests.get(0).getListDefendantRequests().get(0).getHearingLanguageNeeds().name());
        assertEquals("WELSH", listHearingRequests.get(0).getListDefendantRequests().get(1).getHearingLanguageNeeds().name());
    }

    @Test
    void shouldHandleNullOffenceIds() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        // Set offence IDs to null
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("ENGLISH")
                .withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(UUID.randomUUID()).withProsecutorOffenceId("offId11").build()))
                .withId(UUID.randomUUID()).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, null)));

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertNull(listHearingRequests.get(0).getListDefendantRequests().get(0).getDefendantOffences());
    }

    @Test
    void shouldHandleEmptyOffenceIds() {
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = getMigratedHearingWithReferenceData(true);

        // Set offence IDs to empty list
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("ENGLISH")
                .withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(UUID.randomUUID()).withProsecutorOffenceId("offId11").build()))
                .withId(UUID.randomUUID()).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, List.of())));

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName("XHIBIT");
        paramsVO.setChannel(DLRM_MIGRATION);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
                .convert(List.of(migratedHearingWithReferenceData), paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertEquals(0, listHearingRequests.get(0).getListDefendantRequests().get(0).getDefendantOffences().size());
    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceData(boolean hasWeekCommencingDate) {
        return getMigratedHearingWithReferenceData(hasWeekCommencingDate, true, true);
    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceData(boolean hasWeekCommencingDate, boolean organisationUnitExist) {
        return getMigratedHearingWithReferenceData(hasWeekCommencingDate, organisationUnitExist, true);
    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceData(boolean hasWeekCommencingDate, boolean organisationUnitExist, boolean dateOfHearingFuture) {
        UUID caseId = UUID.randomUUID();
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = new MigratedHearingWithReferenceData();

        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("10:05:01")
                .withDurationMinutes(60)
                .withWeekCommencingDate(getBuild(hasWeekCommencingDate))
                .withHearingType("A")
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()));

        if (dateOfHearingFuture) {
            migratedHearingBuilder.withDateOfHearing(now().plusDays(1).toString());
        } else {
            migratedHearingBuilder.withDateOfHearing(now().minusDays(1).toString());
        }

        MigratedHearing migratedHearing = migratedHearingBuilder.build();

        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);
        migratedHearingWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withCaseId(caseId).build());
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withHearingLanguage("E").withOffences(List.of(MigratedOffence.migratedOffence().withOffenceId(caseId).withProsecutorOffenceId("offId11").build())).withId(caseId).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, List.of(caseId))));

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();

        if (organisationUnitExist) {
            OrganisationUnitWithCourtroomsReferenceData organisationUnit =
                    OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                            .withAddress1("123 Justice St")
                            .withAddress2("Suite 400")
                            .withAddress3("City Center")
                            .withAddress4("Judicial District")
                            .withAddress5("London")
                            .withDefaultDurationHrs("2h")
                            .withDefaultStartTime("09:00")
                            .withId(caseId.toString())
                            .withIsWelsh(false)
                            .withOucode("C43AY01")
                            .withOucodeL1Code("C")
                            .withOucodeL1Name("National Courts")
                            .withOucodeL3Code("L3-02")
                            .withOucodeL3Name("District Courts")
                            .withOucodeL3WelshName("Llys Dosbarth")
                            .withCourtrooms(List.of(CourtRoom.courtRoom().withId(UUID.randomUUID().toString()).withCourtroomId(1).withCourtroomName("01").build()))
                            .build();
            referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(organisationUnit);
        }
        referenceDataVO.setHearingType(HearingType.hearingType().withId(caseId).withHearingDescription("description").build());
        migratedHearingWithReferenceData.setReferenceDataVO(referenceDataVO);
        return migratedHearingWithReferenceData;
    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceData(LocalDate hearingDate, LocalDate weekCommencingDate, boolean organisationUnitExist) {
        UUID caseId = UUID.randomUUID();
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = new MigratedHearingWithReferenceData();

        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("23:59:59")
                .withDurationMinutes(60)
                .withHearingType("A")
                .withListedDefendants(List.of(listedDefendant()
                        .withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80")
                        .withListedOffences(List.of("offId11")).build()));

        if (nonNull(hearingDate)) {
            migratedHearingBuilder.withDateOfHearing(hearingDate.toString());
        }

        if (nonNull(weekCommencingDate)) {
            migratedHearingBuilder.withWeekCommencingDate(migratedWeekCommencingDate()
                    .withDuration(2)
                    .withStartDate(weekCommencingDate.toString())
                    .build());
        }


        MigratedHearing migratedHearing = migratedHearingBuilder.build();

        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);
        migratedHearingWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withCaseId(caseId).build());
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("X")
                .withOffences(List.of(MigratedOffence.migratedOffence()
                        .withOffenceId(caseId).withProsecutorOffenceId("offId11")
                        .build()))
                .withId(caseId).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, List.of(caseId))));

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();

        if (organisationUnitExist) {
            OrganisationUnitWithCourtroomsReferenceData organisationUnit =
                    OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                            .withAddress1("123 Justice St")
                            .withAddress2("Suite 400")
                            .withAddress3("City Center")
                            .withAddress4("Judicial District")
                            .withAddress5("London")
                            .withDefaultDurationHrs("2h")
                            .withDefaultStartTime("09:00")
                            .withId(caseId.toString())
                            .withIsWelsh(false)
                            .withOucode("C43AY01")
                            .withOucodeL1Code("C")
                            .withOucodeL1Name("National Courts")
                            .withOucodeL3Code("L3-02")
                            .withOucodeL3Name("District Courts")
                            .withOucodeL3WelshName("Llys Dosbarth")
                            .withCourtrooms(List.of(CourtRoom.courtRoom().withId(UUID.randomUUID().toString()).withCourtroomId(1).withCourtroomName("01").build()))
                            .build();
            referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(organisationUnit);
        }
        referenceDataVO.setHearingType(HearingType.hearingType().withId(caseId).withHearingDescription("description").build());
        migratedHearingWithReferenceData.setReferenceDataVO(referenceDataVO);
        return migratedHearingWithReferenceData;
    }

    private MigratedHearingWithReferenceData getMigratedHearingWithReferenceDataWithCourt() {
        UUID caseId = UUID.randomUUID();
        MigratedHearingWithReferenceData migratedHearingWithReferenceData = new MigratedHearingWithReferenceData();

        MigratedHearing.Builder migratedHearingBuilder = MigratedHearing.migratedHearing()
                .withCourtHearingLocation("C55BN00")
                .withCourtRoomId(1234)
                .withTimeOfHearing("10:05:09")
                .withDurationMinutes(60)
                .withHearingType("A")
                .withListedDefendants(List.of(listedDefendant().withProsecutorDefendantId("a9860e1a-8695-4fd4-8046-c1c4fe6c7f80").withListedOffences(List.of("offId11")).build()))
                .withDateOfHearing(now().toString());

        MigratedHearing migratedHearing = migratedHearingBuilder.build();

        migratedHearingWithReferenceData.setMigratedHearing(migratedHearing);
        migratedHearingWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withCaseId(caseId).build());
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withHearingLanguage("W")
                .withOffences(List.of(MigratedOffence.migratedOffence()
                        .withOffenceId(caseId).withProsecutorOffenceId("offId11")
                        .build()))
                .withId(caseId).build();
        migratedHearingWithReferenceData.setMigratedDefendantWithOffences(List.of(new MigratedDefendantWithOffences(defendant, List.of(caseId))));

        ReferenceDataVO referenceDataVO = new ReferenceDataVO();

        OrganisationUnitWithCourtroomsReferenceData organisationUnit =
                OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withAddress1("123 Justice St")
                        .withAddress2("Suite 400")
                        .withAddress3("City Center")
                        .withAddress4("Judicial District")
                        .withAddress5("London")
                        .withDefaultDurationHrs("2h")
                        .withDefaultStartTime("09:00")
                        .withId(caseId.toString())
                        .withIsWelsh(false)
                        .withOucode("C43AY01")
                        .withOucodeL1Code("C")
                        .withOucodeL1Name("National Courts")
                        .withOucodeL3Code("L3-02")
                        .withOucodeL3Name("District Courts")
                        .withOucodeL3WelshName("Llys Dosbarth")
                        .withCourtrooms (List.of(CourtRoom.courtRoom().withId(UUID.randomUUID().toString()).withCourtroomId(1234).withCourtroomName("courtRoom").build()))
                        .build();
        referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(organisationUnit);
        referenceDataVO.setHearingType(HearingType.hearingType().withId(caseId).withHearingDescription("description").build());
        migratedHearingWithReferenceData.setReferenceDataVO(referenceDataVO);
        return migratedHearingWithReferenceData;
    }

    private MigratedWeekCommencingDate getBuild(boolean hasWeekCommencingDate) {
        return hasWeekCommencingDate ? migratedWeekCommencingDate()
                .withDuration(2)
                .withStartDate(now().plusDays(10).toString())
                .build() : null;
    }

}