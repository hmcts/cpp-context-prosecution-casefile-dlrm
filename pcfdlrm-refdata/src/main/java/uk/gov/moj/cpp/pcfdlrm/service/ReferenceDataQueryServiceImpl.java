package uk.gov.moj.cpp.pcfdlrm.service;

import static java.util.Objects.isNull;
import static java.util.Optional.*;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asAlcoholLevelMethodRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asBailStatusReferenceData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asCaseMarkerRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asCountryNationalityRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asDocumentsMetadataRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asHearingTypesRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asModeOfTrialReasonsRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asObservedEnthnicityRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asOffenceRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asOffenderCodeRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asOrganisationUnitRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asOrganisationUnitWithCourtroomsRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asParentBundleSectionRefDataList;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asPleaReferenceData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asPoliceForceRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asProsecutorRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asSelfDefinedEnthnicityRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asSummonsCodeRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asVehicleCodeRefData;
import static uk.gov.moj.cpp.pcfdlrm.service.RefDataHelper.asVerdictReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes.hearingTypes;

import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.*;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReferenceDataQueryServiceImpl implements ReferenceDataQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataQueryServiceImpl.class);
    public static final String ID = "id";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisationunits";
    private static final String REFERENCEDATA_QUERY_COURTROOMS = "referencedata.query.courtrooms";
    private static final String REFERENCEDATA_QUERY_ALCOHOL_LEVEL_METHODS = "referencedata.query.alcohol-level-methods";
    private static final String REFERENCEDATA_QUERY_BAIL_STATUS = "referencedata.query.bail-statuses";
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS = "referencedataoffences.query.offences-all-versions";
    private static final String REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST = "referencedataoffences.query.offences-list";
    private static final String REFERENCEDATA_QUERY_SUMMONS_CODES = "referencedata.query.summons-codes";
    private static final String REFERENCEDATA_QUERY_COUNTRY_NATIONALITIES = "referencedata.query.country-nationality";
    private static final String REFERENCEDATA_QUERY_INITIATION_TYPES = "referencedata.query.initiation-types";
    private static final String REFERENCEDATA_QUERY_OFFENDER_CODES = "referencedata.query.offender-codes";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS = "referencedata.get-all-document-type-access";
    private static final String REFERENCEDATA_QUERY_POLICE_FORCE = "referencedata.query.police-forces";
    private static final String REFERENCEDATA_QUERY_ALL_PARENT_BUNDLE_SECTION = "referencedata.query.get-all-parent-bundle-section";
    private static final String REFERENCEDATA_QUERY_MODE_OF_TRIAL_REASONS = "referencedata.query.mode-of-trial-reasons";

    private static final String REFERENCEDATA_QUERY_PLEA_TYPES = "referencedata.query.plea-types";
    private static final String REFERENCEDATA_QUERY_VERDICT_TYPES = "referencedata.query.verdict-types";





    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_VERDICT_TYPES = "verdictTypes";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_USAGE = "usage";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_TEAMS = "teams";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_CASE_MARKER = "C";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_TEAM_CC = "CC";
    private static final String REFERENCE_DATA_QUERY_CASE_MARKERS = "referencedata.case-markers.v2";
    private static final String FIELD_CASE_MARKERS = "caseMarkers";
    private static final String ORGANISATION_UNITS = "organisationunits";
    private static final String OUCODE = "oucode";
    private static final String SPIOUCODE = "spiOucode";
    private static final String REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY = "referencedata.query.ethnicities";
    private static final String REFERENCE_DATA_QUERY_OBSERVED_ETHNICITY = "referencedata.query.observed-ethnicities";
    private static final String REFERENCE_DATA_QUERY_PROSECUTORS_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String FIELD_SELF_ETHNICITIES = "ethnicities";
    private static final String FIELD_OBSERVED_ETHNICITIES = "observedEthnicities";
    private static final String REFERENCEDATA_QUERY_VEHICLE_CODE = "referencedata.query.vehicle-codes";
    private static final String FIELD_VEHICLE_CODES = "vehicleCodes";
    private static final String FIELD_ALCOHOL_LEVEL_METHODS = "alcoholLevelMethods";
    private static final String FIELD_BAIL_STATUSES = "bailStatuses";
    private static final String FIELD_HEARING_TYPES = "hearingTypes";
    private static final String FIELD_SUMMONS_CODES = "summonsCodes";
    private static final String FIELD_COUNTRY_NATIONALITIES = "countryNationality";
    private static final String FIELD_INITIATION_TYPES = "initiationTypes";
    private static final String FIELD_OFFENDER_CODES = "offenderCodes";
    private static final String FIELD_DOCUMENTS_TYPE_ACCESS = "documentsTypeAccess";
    private static final String FIELD_POLICE_FORCES = "policeForces";
    private static final String FIELD_MODE_OF_TRIAL_REASONS = "modeOfTrialReasons";
    public static final String QUERY_PARAM_OU_COURTROOM_NAME = "ouCourtRoomName";
    private static final String ENFORCEMENT_AREA_QUERY_NAME = "referencedata.query.enforcement-area";
    private static final String COURT_CODE_QUERY_PARAMETER = "localJusticeAreaNationalCourtCode";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Override
    public List<ReferenceDataCountryNationality> retrieveCountryNationality() {
        return getRefDataStream(REFERENCEDATA_QUERY_COUNTRY_NATIONALITIES, FIELD_COUNTRY_NATIONALITIES, createObjectBuilder()).map(asCountryNationalityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<SummonsCodeReferenceData> retrieveSummonsCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_SUMMONS_CODES, FIELD_SUMMONS_CODES, createObjectBuilder()).map(asSummonsCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<AlcoholLevelMethodReferenceData> retrieveAlcoholLevelMethods() {
        return getRefDataStream(REFERENCEDATA_QUERY_ALCOHOL_LEVEL_METHODS, FIELD_ALCOHOL_LEVEL_METHODS, createObjectBuilder()).map(asAlcoholLevelMethodRefData()).collect(Collectors.toList());
    }

    @Override
    public List<BailStatusReferenceData> retrieveBailStatuses() {
        return getRefDataStream(REFERENCEDATA_QUERY_BAIL_STATUS, FIELD_BAIL_STATUSES, createObjectBuilder()).map(asBailStatusReferenceData()).collect(Collectors.toList());
    }

    @Override
    public List<OffenderCodeReferenceData> retrieveOffenderCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_OFFENDER_CODES, FIELD_OFFENDER_CODES, createObjectBuilder()).map(asOffenderCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<SelfdefinedEthnicityReferenceData> retrieveSelfDefinedEthnicity() {
        return getRefDataStream(REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY, FIELD_SELF_ETHNICITIES, createObjectBuilder()).map(asSelfDefinedEnthnicityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<ObservedEthnicityReferenceData> retrieveObservedEthnicity() {
        return getRefDataStream(REFERENCE_DATA_QUERY_OBSERVED_ETHNICITY, FIELD_OBSERVED_ETHNICITIES, createObjectBuilder()).map(asObservedEnthnicityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<PoliceForceReferenceData> retrievePoliceForceCode() {
        return getRefDataStream(REFERENCEDATA_QUERY_POLICE_FORCE, FIELD_POLICE_FORCES, createObjectBuilder()).map(asPoliceForceRefData()).collect(Collectors.toList());
    }

    @Override
    public List<VehicleCodeReferenceData> retrieveVehicleCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_VEHICLE_CODE, FIELD_VEHICLE_CODES, createObjectBuilder()).map(asVehicleCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<ModeOfTrialReasonsReferenceData> retrieveModeOfTrialReasons() {
        return getRefDataStream(REFERENCEDATA_QUERY_MODE_OF_TRIAL_REASONS, FIELD_MODE_OF_TRIAL_REASONS, createObjectBuilder()).map(asModeOfTrialReasonsRefData()).collect(Collectors.toList());
    }

    @Override
    public Optional<PleaReferenceData> getPleaTypeById(final UUID id) {
        LOGGER.info("Get plea type data by id {} ", id);
        final Envelope<JsonObject> pleaTypes = requester.requestAsAdmin(envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_PLEA_TYPES), createObjectBuilder()), JsonObject.class);
        final JsonArray response = pleaTypes.payload().getJsonArray(FIELD_PLEA_STATUS_TYPES);
        if (response == null) {
            return empty();
        }
        return response.stream()
                .map(asPleaReferenceData())
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    @Override
    public Optional<VerdictReferenceData> getVerdictTypeById(final UUID id) {
        LOGGER.info("Get verdict type data by id {} ", id);
        final Envelope<JsonObject> verdictTypes = requester.requestAsAdmin(envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_VERDICT_TYPES), createObjectBuilder()), JsonObject.class);
        final JsonArray response = verdictTypes.payload().getJsonArray(FIELD_VERDICT_TYPES);
        if (response == null) {
            return empty();
        }
        return response.stream()
                .map(asVerdictReferenceData())
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    @Override
    public Optional<LjaDetails> getLjaDetails(final String ljaCode, final String courtCentreId) {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(ENFORCEMENT_AREA_QUERY_NAME)
                        .withId(randomUUID())
                        .build(),
                createObjectBuilder().add(COURT_CODE_QUERY_PARAMETER, ljaCode).build());

        final Envelope<EnforcementArea> responseEnvelope = requester.requestAsAdmin(requestEnvelope, EnforcementArea.class);

        final EnforcementArea enforcementArea = responseEnvelope.payload();

        if (isNull(enforcementArea)) {
            return empty();
        }

        return of(LjaDetails.ljaDetails()
                .withLjaCode(enforcementArea.getLocalJusticeArea().getNationalCourtCode())
                .withLjaName(enforcementArea.getLocalJusticeArea().getName())
                .withWelshLjaName(enforcementArea.getLocalJusticeArea().getWelshName())
                .build());
    }

    @Override
    public Optional<OrganisationUnitWithCourtroomReferenceData> retrieveOrganisationUnitWithCourtroom(final String ouCode) {
        final String courtroom = ouCode.substring(ouCode.length() - 2);
        final String ouCodeWithoutCourtRoom = ouCode.substring(0, ouCode.length() - 2) + "00";
        
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_COURTROOMS),
                createObjectBuilder().add(OUCODE, ouCodeWithoutCourtRoom));
        final JsonValue response = requester.requestAsAdmin(envelope, JsonObject.class).payload();
        
        if (response == null) {
            return Optional.empty();
        }
        
        final JsonArray organisationUnits = response.asJsonObject().getJsonArray(ORGANISATION_UNITS);
        if (organisationUnits == null) {
            return Optional.empty();
        }
        
        return findMatchingCourtroom(organisationUnits, courtroom);
    }

    private Optional<OrganisationUnitWithCourtroomReferenceData> findMatchingCourtroom(final JsonArray organisationUnits, final String courtroom) {
        final String expectedCourtroomName = "Courtroom " + courtroom;
        
        for (JsonValue unit : organisationUnits) {
            final JsonObject organisationUnit = unit.asJsonObject();
            final JsonArray courtrooms = organisationUnit.getJsonArray("courtrooms");
            
            if (courtrooms == null) {
                continue;
            }
            
            final Optional<CourtRoom> matchingCourtroom = findCourtroomByName(courtrooms, expectedCourtroomName);
            if (matchingCourtroom.isPresent()) {
                return Optional.of(createOrganisationUnitWithCourtroom(organisationUnit, matchingCourtroom.get()));
            }
        }
        
        return Optional.empty();
    }

    private Optional<CourtRoom> findCourtroomByName(final JsonArray courtrooms, final String expectedCourtroomName) {
        for (JsonValue courtroomValue : courtrooms) {
            final JsonObject courtroomObject = courtroomValue.asJsonObject();
            final String courtroomName = courtroomObject.getString("courtroomName", "");
            
            if (expectedCourtroomName.equals(courtroomName)) {
                return Optional.of(createCourtRoom(courtroomObject));
            }
        }
        return Optional.empty();
    }

    private CourtRoom createCourtRoom(final JsonObject courtroomObject) {
        return CourtRoom.courtRoom()
                .withId(courtroomObject.getString("id"))
                .withCourtroomId(courtroomObject.getInt("courtroomId"))
                .withCourtroomName(courtroomObject.getString("courtroomName"))
                .build();
    }

    private OrganisationUnitWithCourtroomReferenceData createOrganisationUnitWithCourtroom(final JsonObject organisationUnit, final CourtRoom courtRoom) {
        return OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData()
                .withId(organisationUnit.getString("id"))
                .withOucode(organisationUnit.getString(OUCODE))
                .withOucodeL1Code(organisationUnit.getString("oucodeL1Code"))
                .withOucodeL1Name(organisationUnit.getString("oucodeL1Name"))
                .withOucodeL3Name(organisationUnit.getString("oucodeL3Name"))
                .withOucodeL3WelshName(organisationUnit.getString("oucodeL3WelshName"))
                .withAddress1(organisationUnit.getString("address1"))
                .withAddress2(organisationUnit.getString("address2"))
                .withPostcode(organisationUnit.getString("postcode"))
                .withDefaultStartTime(organisationUnit.getString("defaultStartTime"))
                .withDefaultDurationHrs(organisationUnit.getString("defaultDurationHrs"))
                .withCourtRoom(courtRoom)
                .build();
    }

    @Override
    public Optional<OrganisationUnitWithCourtroomsReferenceData> retrieveOrganisationUnitWithCourtrooms(String ouCode) {

        final String ouCodeWithoutCourtRoom = ouCode.substring(0, ouCode.length() - 2) + "00";

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_COURTROOMS),
                createObjectBuilder().add(OUCODE, ouCodeWithoutCourtRoom));
        final Envelope<JsonObject> results = requester.requestAsAdmin(envelope, JsonObject.class);

        final JsonArray response = results.payload().getJsonArray(ORGANISATION_UNITS);

        if (response == null) {
            return Optional.empty();
        }
        List<OrganisationUnitWithCourtroomsReferenceData> ounits = response.stream().map(asOrganisationUnitWithCourtroomsRefData()).toList();
        if(ounits.isEmpty()){
            return Optional.empty();
        }else{
            return  Optional.of(ounits.get(0));
        }

    }

    @Override
    public List<DocumentTypeAccessReferenceData> retrieveDocumentsTypeAccess() {
        return getRefDataStream(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS, FIELD_DOCUMENTS_TYPE_ACCESS, createObjectBuilder().add("date", LocalDate.now().toString()))
                .map(asDocumentsMetadataRefData())
                .collect(Collectors.toList());
    }

    @Override
    public List<OrganisationUnitReferenceData> retrieveOrganisationUnits(final String ouCode) {
        return getRefDataStream(REFERENCEDATA_QUERY_ORGANISATION_UNIT, ORGANISATION_UNITS, createObjectBuilder().add(SPIOUCODE, ouCode)).map(asOrganisationUnitRefData()).collect(Collectors.toList());
    }

    @Override
    public List<String> getInitiationCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_INITIATION_TYPES, FIELD_INITIATION_TYPES, createObjectBuilder())
                .map(initiationType -> (JsonObject) initiationType)
                .map(initiationType -> initiationType.getString("code")).collect(Collectors.toList());
    }

    @Override
    public HearingTypes retrieveHearingTypes() {
        return hearingTypes()
                .withHearingtypes(getRefDataStream(REFERENCEDATA_QUERY_HEARING_TYPES, FIELD_HEARING_TYPES, createObjectBuilder()).map(asHearingTypesRefData()).collect(Collectors.toList()))
                .build();
    }

    @Override
    public ProsecutorsReferenceData retrieveProsecutors(final String originatingOrganisation) {

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_PROSECUTORS_BY_OUCODE), createObjectBuilder().add(OUCODE, originatingOrganisation));
        final JsonValue response = requester.requestAsAdmin(envelope, JsonObject.class).payload();
        ProsecutorsReferenceData prosecutorsReferenceData = null;
        if (null != response) {
            prosecutorsReferenceData = asProsecutorRefData().apply(response);
        }

        return prosecutorsReferenceData;
    }

    @Override
    public List<CaseMarker> getCaseMarkerDetails() {
        return getRefDataStream(REFERENCE_DATA_QUERY_CASE_MARKERS, FIELD_CASE_MARKERS, createObjectBuilder()
                .add(REFERENCE_DATA_QUERY_CATEGORIES_TEAMS, REFERENCE_DATA_QUERY_CATEGORIES_TEAM_CC)
                .add(REFERENCE_DATA_QUERY_CATEGORIES_USAGE, REFERENCE_DATA_QUERY_CATEGORIES_CASE_MARKER))
                .map(asCaseMarkerRefData())
                .collect(Collectors.toList());
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceData(final MigratedOffence offence, final String initiationCode) {
        LOGGER.info("Requesting {} for initiationCode {} with offence {} ", REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS, initiationCode, offence);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add("cjsoffencecodes", offence.getOffenceCode());

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS), jsonObjectBuilder);

        final JsonArray response = requester.requestAsAdmin(envelope, JsonObject.class).payload().getJsonArray("offences");

        List<OffenceReferenceData> offenceReferenceDataList = null;
        if (null != response) {
            offenceReferenceDataList =
                    response.stream().map(asOffenceRefData()).collect(Collectors.toList());
        }
        return offenceReferenceDataList;
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceDataList(final List<String> cjsOffenceCodeList) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add("cjsoffencecode", String.join(",", cjsOffenceCodeList));

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST), jsonObjectBuilder);
        final JsonArray response = requester.requestAsAdmin(envelope, JsonObject.class).payload().getJsonArray("offences");

        List<OffenceReferenceData> offenceReferenceDataList = null;
        if (null != response) {
            offenceReferenceDataList =
                    response.stream().map(asOffenceRefData()).collect(Collectors.toList());
        }
        return offenceReferenceDataList;
    }



    @Override
    public List<ParentBundleSectionReferenceData> getAllParentBundleSection(final Metadata metadata) {
        return getRefDataStream(REFERENCEDATA_QUERY_ALL_PARENT_BUNDLE_SECTION, "bundles", createObjectBuilder())
                .map(asParentBundleSectionRefDataList())
                .collect(Collectors.toList());
    }

    private Stream<JsonValue> getRefDataStream(final String queryName, final String fieldName, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(queryName), jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class)
                .payload()
                .getJsonArray(fieldName)
                .stream();
    }

    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }
}
