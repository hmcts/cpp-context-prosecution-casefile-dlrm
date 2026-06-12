package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.time.ZonedDateTime.parse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.ListHearingRequest.listHearingRequest;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.PCFEnumMap.getLanguageToHearingLanguageNeeds;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter implements ParameterisedConverter<List<MigratedHearingWithReferenceData>, List<ListHearingRequest>, ParamsVO> {

    private static final Logger LOGGER = getLogger(ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter.class);

    private static final String ORGANISATION_UNIT_LEVEL_CODE_FOR_CROWN = "C";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Override
    @SuppressWarnings({"squid:S1135"})
    public List<ListHearingRequest> convert(List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList, final ParamsVO paramsVO) {
        final List<ListHearingRequest> hearingRequests = new ArrayList<>();

        if (nonNull(migratedHearingWithReferenceDataList) && !migratedHearingWithReferenceDataList.isEmpty()) {

            final UUID caseId = migratedHearingWithReferenceDataList.get(0).getCaseDetails().getCaseId();

            for (MigratedHearingWithReferenceData migratedHearingWithReferenceData : migratedHearingWithReferenceDataList) {

                final MigratedHearing migratedHearing = migratedHearingWithReferenceData.getMigratedHearing();

                final ReferenceDataVO referenceDataVO = migratedHearingWithReferenceData.getReferenceDataVO();

                if (nonNull(migratedHearing)) {
                    buildListHearingRequest(migratedHearing, referenceDataVO, migratedHearingWithReferenceData, caseId, paramsVO, hearingRequests);
                }
            }
        }

        return hearingRequests;
    }

    private static HearingType buildHearingType(final ReferenceDataVO referenceDataVO) {
        if (nonNull(referenceDataVO.getHearingType())) {
            return HearingType.hearingType().withId(referenceDataVO.getHearingType().getId()).withDescription(referenceDataVO.getHearingType().getHearingDescription()).build();
        }
        return null;
    }

    private CourtCentre buildCourtCentre(final ReferenceDataVO referenceDataVO, final MigratedHearing migratedHearing) {

        final Optional<OrganisationUnitWithCourtroomsReferenceData> organisationUnitWithCourtroomsReferenceDataOptional = referenceDataVO.getOrganisationUnitWithCourtroomsReferenceData();

        if (organisationUnitWithCourtroomsReferenceDataOptional.isPresent()) {

            final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();

            final OrganisationUnitWithCourtroomsReferenceData organisationUnitWithCourtroomReferenceData = organisationUnitWithCourtroomsReferenceDataOptional.get();

            LOGGER.info("asOrganisationUnitWithCourtroomReferenceData {} ", asOrganisationUnitWithCourtroomsReferenceData(organisationUnitWithCourtroomReferenceData));

            Optional<CourtRoom> courtRoomOptional = organisationUnitWithCourtroomReferenceData.getCourtrooms().stream()
                    .filter(courtRoom -> isMatchingCourtroom(migratedHearing, courtRoom))
                    .findFirst();

            if (courtRoomOptional.isEmpty()) {
                LOGGER.info("no matching court room ");
            }

            if (nonNull(migratedHearing) && nonNull(migratedHearing.getCourtRoomId()) && (courtRoomOptional.isPresent())) {
                final UUID roomId = getRoomId(courtRoomOptional.get());
                final String roomName = getRoomName(courtRoomOptional.get());
                courtCentreBuilder.withId(fromString(organisationUnitWithCourtroomReferenceData.getId()));
                courtCentreBuilder.withName(organisationUnitWithCourtroomReferenceData.getOucodeL3Name());
                courtCentreBuilder.withWelshName(organisationUnitWithCourtroomReferenceData.getOucodeL3WelshName());
                courtCentreBuilder.withRoomId(roomId);
                courtCentreBuilder.withRoomName(roomName);
            } else {
                courtCentreBuilder.withId(fromString(organisationUnitWithCourtroomReferenceData.getId()));
                courtCentreBuilder.withName(organisationUnitWithCourtroomReferenceData.getOucodeL3Name());
                courtCentreBuilder.withWelshName(organisationUnitWithCourtroomReferenceData.getOucodeL3WelshName());
            }

            return courtCentreBuilder.build();
        }

        return null;

    }

    private boolean isMatchingCourtroom(MigratedHearing migratedHearing, CourtRoom courtRoom) {
        if (migratedHearing == null || courtRoom == null) return false;

        final Integer courtRoomId = migratedHearing.getCourtRoomId();
        if (courtRoomId == null) return false;

        return Objects.equals(courtRoom.getCourtroomId(), courtRoomId);
    }


    private JurisdictionType determineJurisdictionType(final String oucodeL1Code, String migrationSourceSystemName) {
        LOGGER.info("Find jurisdiction type using migration system name {} and OU L1 code {}", migrationSourceSystemName, oucodeL1Code);

        if ("XHIBIT".equalsIgnoreCase(migrationSourceSystemName) &&
                nonNull(oucodeL1Code) && oucodeL1Code.equals(ORGANISATION_UNIT_LEVEL_CODE_FOR_CROWN)) {
            return CROWN;
        }

        return MAGISTRATES;
    }

    private List<ListDefendantRequest> buildListDefendantRequest(final List<MigratedDefendantWithOffences> migratedDefendantWithOffences, final UUID caseId, final SummonsApprovedOutcome summonsApprovedOutcome) {
        return migratedDefendantWithOffences.stream()
                .map(migratedDefendantWithOffence -> {
                    String hearingLanguage = migratedDefendantWithOffence.getDefendant().getHearingLanguage();

                    if (matchLanguageEnum(hearingLanguage)) {
                        hearingLanguage = "E";
                    }

                    final ListDefendantRequest.Builder listDefendantRequest = listDefendantRequest().withProsecutionCaseId(caseId)
                            .withDefendantOffences(migratedDefendantWithOffence.getOffenceids())
                            .withSummonsApprovedOutcome(summonsApprovedOutcome)
                            .withDefendantId(migratedDefendantWithOffence.getDefendant().getId())
                            .withHearingLanguageNeeds(getHearingLanguage(hearingLanguage))
                            .withSummonsRequired(SummonsType.FIRST_HEARING);

                    return listDefendantRequest.build();
                }).toList();
    }

    private ZonedDateTime getDateAndTimeOfHearing(final MigratedHearing migratedHearing) {
        if (nonNull(migratedHearing) && nonNull(migratedHearing.getDateOfHearing()) && nonNull(migratedHearing.getTimeOfHearing())) {
            return parse(migratedHearing.getDateOfHearing() + migratedHearing.getTimeOfHearing(), DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss").withZone(ZoneId.of("UTC")));
        }
        return null;
    }

    private UUID getRoomId(final CourtRoom courtRoom) {
        if (nonNull(courtRoom)) {
            return fromString(courtRoom.getId());
        }
        return null;
    }

    private String getRoomName(final CourtRoom courtRoom) {
        if (nonNull(courtRoom)) {
            return courtRoom.getCourtroomName();
        }
        return null;
    }

    private LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }


    private static boolean matchLanguageEnum(final String hearingLanguage) {
        if (isNull(hearingLanguage)) {
            return true;
        }
        try {
            Language.valueOf(hearingLanguage.toUpperCase());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private HearingLanguage getHearingLanguage(final String hearingLanguage) {
        return getLanguageToHearingLanguageNeeds().get(Language.valueOf(hearingLanguage));
    }

    public static String asOrganisationUnitWithCourtroomsReferenceData(OrganisationUnitWithCourtroomsReferenceData organisationUnitWithCourtroomsReferenceData) {
        try {
            return OBJECT_MAPPER.writeValueAsString(organisationUnitWithCourtroomsReferenceData);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to unmarshal ProsecutorsReferenceData. Payload :{}", organisationUnitWithCourtroomsReferenceData, e);
            return null;
        }
    }

    private void buildListHearingRequest(final MigratedHearing migratedHearing, final ReferenceDataVO referenceDataVO,
                                         final MigratedHearingWithReferenceData migratedHearingWithReferenceData, final UUID caseId, final ParamsVO paramsVO,
                                         final List<ListHearingRequest> hearingRequests) {
        final boolean pastHearingDate = nonNull(migratedHearing.getDateOfHearing()) && convertToLocalDate(migratedHearing.getDateOfHearing()).isBefore(LocalDate.now());

        final boolean pastWeekCommencingDate = nonNull(migratedHearing.getWeekCommencingDate())
                && nonNull(migratedHearing.getWeekCommencingDate().getStartDate())
                && convertToLocalDate(migratedHearing.getWeekCommencingDate().getStartDate()).isBefore(LocalDate.now());

        final Integer duration = nonNull(migratedHearing.getDurationMinutes()) ? migratedHearing.getDurationMinutes() : null;

        final ListHearingRequest.Builder listhearingRequestBuilder = listHearingRequest()
                .withCourtCentre(buildCourtCentre(referenceDataVO, migratedHearing))
                .withHearingType(buildHearingType(referenceDataVO))
                .withJurisdictionType(determineJurisdictionType(referenceDataVO.getOrganisationUnitWithCourtroomsReferenceData().map(OrganisationUnitWithCourtroomsReferenceData::getOucodeL1Code).orElse(null), paramsVO.getMigrationSourceSystemName()))
                .withEstimateMinutes(nonNull(duration) ? duration : referenceDataVO.getHearingType().getDefaultDurationMin());

        final ListHearingRequest listHearingRequest = listhearingRequestBuilder
                .withListedStartDateTime(getDateAndTimeOfHearing(migratedHearing))
                .withListDefendantRequests(buildListDefendantRequest(migratedHearingWithReferenceData.getMigratedDefendantWithOffences(), caseId, paramsVO.getSummonsApprovedOutcome()))
                .withWeekCommencingDate(
                        ofNullable(migratedHearing.getWeekCommencingDate())
                                .map(e -> WeekCommencingDate.weekCommencingDate().withDuration(e.getDuration()).withStartDate(e.getStartDate()).build())
                                .orElse(null))
                .build();
        if (!pastHearingDate && !pastWeekCommencingDate && nonNull(listHearingRequest.getCourtCentre()) &&
                nonNull(listHearingRequest.getHearingType()) &&
                nonNull(listHearingRequest.getJurisdictionType()) &&
                isNotEmpty(listHearingRequest.getListDefendantRequests())) {
            hearingRequests.add(listhearingRequestBuilder.build());

        }
    }

}
