package uk.gov.moj.cpp.pcfdlrm.refdata.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.buildHearingTypes;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.buildSelfDefinedInformationEthnicity;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.buildVehicleCodes;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataAlcoholLevelMethods;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataAllParentBundleSectionReferenceData;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataCountryNationalities;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataDocumentsTypeAccess;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataInitiationTypeJsonObject;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataObservedEthnicity;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataOffenceData;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataOffenderCodes;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataPoliceForces;
import static uk.gov.moj.cpp.pcfdlrm.refdata.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataSummonsCodes;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryServiceImpl;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataQueryServiceTest {

    public static final String VALID_FROM = "validFrom";
    public static final String SEQUENCE = "sequence";
    private static final String VALUE_COURT_ID_MATCHING = "B05BK00";
    private static final String VALUE_COURT_ID_NOT_MATCHING = "XYZ123";
    private static final String VALUE_COURT_NAME = "Birkenhead";
    private static final String VALUE_TYPE_OF_COURT = "Magistrates' Courts";
    private static final String KEY_ID = "id";
    private static final String VALID_INITIATION_CODE = "S";
    private static final String INVALID_INITIATION_CODE = "A";
    private static final String ORGANISATION_UNITS = "organisationunits";
    private static final String OUCODE = "oucode";
    private static final String OUCODE_L_1_CODE = "oucodeL1Code";
    private static final String COURT_ROOM_ID_VALUE = "123";
    private static final String COURT_ROOM_NAME_VALUE_1 = "Courtroom 01";
    private static final String WELSH_VENUE_NAME = "oucodeL3WelshName";
    private static final String WELSH_VENUE_NAME_VALUE = "Canolfan Gyfiawnder Yr Wyddgrug";
    private static final String OUCODE_L3NAME_VALUE = "Wrexham Magistrates' Court";
    private static final String HEARING_CODE1 = "PTP";
    private static final String HEARING_DESCRIPTION1 = "Plea & Trial Preparation";
    private static final String HEARING_CODE2 = "PTP2";

    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_VERDICT_TYPES = "verdictTypes";
    private static final String FIELD_PLEA_TYPE_GUILTY_FLAG = "pleaTypeGuiltyFlag";
    private static final String GUILTY_FLAG_YES = "Yes";
    private static final String GUILTY_FLAG_NO = "No";
    private static final String FIELD_PLEA_VALUE = "pleaValue";
    private static final String FIELD_VERDICT_CODE = "verdictCode";
    private static final String PLEA_TYPE_CODE = "pleaTypeCode";
    private static final String GUILTY = "GUILTY";
    private static final String NOT_GUILTY = "NOT_GUILTY";
    private static final String INDICATED_GUILTY = "INDICATED_GUILTY";
    private static final String NGJAA = "NGJAA";
    @InjectMocks
    private ReferenceDataQueryServiceImpl referenceDataService;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Test
    public void shouldRetrieveCountryNationalityList() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.country-nationality");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataCountryNationalities();
        final Envelope<JsonObject> countryNationalities = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(countryNationalities);

        final List<ReferenceDataCountryNationality> referenceDataCountryNationalities = referenceDataService.retrieveCountryNationality();
        assertThat(referenceDataCountryNationalities, is(notNullValue()));
        assertThat(referenceDataCountryNationalities.size(), is(3));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.country-nationality");
    }


    @Test
    public void shouldRetrieveOffenderCodeList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.offender-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenderCodes();
        final Envelope<JsonObject> offenderCodes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(offenderCodes);

        final List<OffenderCodeReferenceData> referenceDataList = referenceDataService.retrieveOffenderCodes();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(2));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.offender-codes");
    }

    @Test
    public void shouldRetrieveSummonsCodesList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.summons-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataSummonsCodes();
        final Envelope<JsonObject> summonsCodes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(summonsCodes);

        final List<SummonsCodeReferenceData> referenceDataList = referenceDataService.retrieveSummonsCodes();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(2));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.summons-codes");
    }

    @Test
    public void shouldRetrieveDocumentsMetadataList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.documents-type-access");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataDocumentsTypeAccess();
        final Envelope<JsonObject> documentsMetadata = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(documentsMetadata);

        final List<DocumentTypeAccessReferenceData> referenceDataList = referenceDataService.retrieveDocumentsTypeAccess();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(1));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.get-all-document-type-access");
    }

    @Test
    public void shouldRetrieveAlcoholLevelMethodsList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.alcohol-level-methods");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataAlcoholLevelMethods();

        final Envelope<JsonObject> alcoholLevelMetMethods = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class)))
                .thenReturn(alcoholLevelMetMethods);

        final List<AlcoholLevelMethodReferenceData> referenceDataList = referenceDataService.retrieveAlcoholLevelMethods();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(3));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(), eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.alcohol-level-methods");
    }

    @Test
    public void shouldReturnSelfDefinedInformationEthnicity() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildSelfDefinedInformationEthnicity();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<SelfdefinedEthnicityReferenceData> refEthnicityData = referenceDataService.retrieveSelfDefinedEthnicity();
        assertThat(refEthnicityData, is(notNullValue()));
        assertThat(refEthnicityData.size(), is(2));

    }

    @Test
    public void shouldReturnVehicleCodeRefData() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildVehicleCodes();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<VehicleCodeReferenceData> refVehicleCodeRefData = referenceDataService.retrieveVehicleCodes();
        assertThat(refVehicleCodeRefData, is(notNullValue()));
        assertThat(refVehicleCodeRefData.size(), is(2));

    }

    @Test
    public void shouldRetrieveOrganisationUnitWithCourtRoom() {
        // Given: A ouCode that includes courtroom number (e.g., "C01BL01" for Courtroom 01)
        final String ouCodeWithCourtroom = "C01BL01";
        final String expectedCourtroomName = "Courtroom 01";
        
        // Setup mock response
        setupMockOrganisationUnitResponse();

        // When: Call the method
        final Optional<OrganisationUnitWithCourtroomReferenceData> result = 
            referenceDataService.retrieveOrganisationUnitWithCourtroom(ouCodeWithCourtroom);

        // Then: Verify the result
        assertThat("Result should be present", result.isPresent(), is(true));
        final OrganisationUnitWithCourtroomReferenceData organisationUnitData = result.get();
        
        // Verify organisation unit details
        assertThat(organisationUnitData.getId(), is("89592405-c29b-3706-b1d3-b1dd3a08b227"));
        assertThat(organisationUnitData.getOucode(), is("C01BL00"));
        assertThat(organisationUnitData.getOucodeL1Code(), is("C"));
        assertThat(organisationUnitData.getOucodeL1Name(), is("Crown Courts"));
        assertThat(organisationUnitData.getOucodeL3Name(), is("Blackfriars Crown Court"));
        assertThat(organisationUnitData.getOucodeL3WelshName(), is("Llys Y Goron Blackfriars"));
        assertThat(organisationUnitData.getAddress1(), is("1-15 Pocock Street"));
        assertThat(organisationUnitData.getAddress2(), is("London"));
        assertThat(organisationUnitData.getPostcode(), is("SE1 0BJ"));
        assertThat(organisationUnitData.getDefaultStartTime(), is("10:00:00"));
        assertThat(organisationUnitData.getDefaultDurationHrs(), is("07:00:00"));
        
        // Verify courtroom details
        assertThat(organisationUnitData.getCourtRoom(), notNullValue());
        assertThat(organisationUnitData.getCourtRoom().getId(), is("d0624ee3-9198-3c8b-94d6-42fb197ebe5e"));
        assertThat(organisationUnitData.getCourtRoom().getCourtroomId(), is(235));
        assertThat(organisationUnitData.getCourtRoom().getCourtroomName(), is(expectedCourtroomName));
    }

    @Test
    public void shouldRetrieveOrganisationUnitWithCourtRooms() {
        // Given: A ouCode that includes courtroom number (e.g., "C01BL01" for Courtroom 01)
        final String ouCodeWithCourtroom = "C01BL01";
        
        // Setup mock response
        setupMockOrganisationUnitResponse();

        // When: Call the method
        final Optional<OrganisationUnitWithCourtroomsReferenceData> result = 
            referenceDataService.retrieveOrganisationUnitWithCourtrooms(ouCodeWithCourtroom);

        // Then: Verify the result
        assertThat("Result should be present", result.isPresent(), is(true));
        final OrganisationUnitWithCourtroomsReferenceData organisationUnitData = result.get();
        
        // Verify organisation unit details
        assertThat(organisationUnitData.getId(), is("89592405-c29b-3706-b1d3-b1dd3a08b227"));
        assertThat(organisationUnitData.getOucode(), is("C01BL00"));
        assertThat(organisationUnitData.getOucodeL1Code(), is("C"));
        assertThat(organisationUnitData.getOucodeL1Name(), is("Crown Courts"));
        assertThat(organisationUnitData.getOucodeL3Name(), is("Blackfriars Crown Court"));
        assertThat(organisationUnitData.getOucodeL3WelshName(), is("Llys Y Goron Blackfriars"));
        assertThat(organisationUnitData.getAddress1(), is("1-15 Pocock Street"));
        assertThat(organisationUnitData.getAddress2(), is("London"));
        assertThat(organisationUnitData.getPostcode(), is("SE1 0BJ"));
        assertThat(organisationUnitData.getDefaultStartTime(), is("10:00:00"));
        assertThat(organisationUnitData.getDefaultDurationHrs(), is("07:00:00"));
        
        // Verify courtrooms details - should contain all courtrooms
        assertThat(organisationUnitData.getCourtrooms(), notNullValue());
        assertThat(organisationUnitData.getCourtrooms().size(), is(9));
        
        // Verify first courtroom details
        final CourtRoom firstCourtroom = organisationUnitData.getCourtrooms().get(0);
        assertThat(firstCourtroom.getId(), is("d0624ee3-9198-3c8b-94d6-42fb197ebe5e"));
        assertThat(firstCourtroom.getCourtroomId(), is(235));
        assertThat(firstCourtroom.getCourtroomName(), is("Courtroom 01"));
        
        // Verify last courtroom details
        final CourtRoom lastCourtroom = organisationUnitData.getCourtrooms().get(8);
        assertThat(lastCourtroom.getId(), is("68644f24-2ec4-3b69-b2e4-885f09464108"));
        assertThat(lastCourtroom.getCourtroomId(), is(243));
        assertThat(lastCourtroom.getCourtroomName(), is("Courtroom 09"));
    }

    @Test
    public void shouldReturnEmptyOptionalWhenOrganisationUnitsArrayIsEmpty() {
        // Given: A ouCode that includes courtroom number
        final String ouCodeWithCourtroom = "C01BL01";
        
        // Setup mock response with empty organisation units array
        setupMockEmptyOrganisationUnitResponse();

        // When: Call the method
        final Optional<OrganisationUnitWithCourtroomsReferenceData> result = 
            referenceDataService.retrieveOrganisationUnitWithCourtrooms(ouCodeWithCourtroom);

        // Then: Verify the result is empty
        assertThat("Result should be empty", result.isPresent(), is(false));
    }

    private void setupMockOrganisationUnitResponse() {
        // Build the Envelope with the JSON structure provided
        final JsonArray courtroomsArray = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("id", "d0624ee3-9198-3c8b-94d6-42fb197ebe5e")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 235)
                        .add("courtroomName", "Courtroom 01")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "60853c27-8a9d-349a-aeb5-7f5049a774dd")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 236)
                        .add("courtroomName", "Courtroom 02")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "67348d5d-4742-3ba6-9e9b-29a7595b5c3e")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 237)
                        .add("courtroomName", "Courtroom 03")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "9c52e029-c289-3409-8254-96d3d0c5b505")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 238)
                        .add("courtroomName", "Courtroom 04")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "7e611d8a-d60c-3152-8e6c-3bad3d9ef449")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 239)
                        .add("courtroomName", "Courtroom 05")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "4bc6a3a5-e17e-3871-8386-3d773abd4f38")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 240)
                        .add("courtroomName", "Courtroom 06")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "aee93b5c-bbda-30b9-9da4-fd87cb26be8d")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 241)
                        .add("courtroomName", "Courtroom 07")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "eed82bc1-ad5a-385d-a138-dc112b3c7712")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 242)
                        .add("courtroomName", "Courtroom 08")
                        .add("venueId", 51)
                        .build())
                .add(createObjectBuilder()
                        .add("id", "68644f24-2ec4-3b69-b2e4-885f09464108")
                        .add("venueName", "BLACKFRIARS CROWN COURT")
                        .add("courtroomId", 243)
                        .add("courtroomName", "Courtroom 09")
                        .add("venueId", 51)
                        .build())
                .build();

        final JsonObject organisationUnit = createObjectBuilder()
                .add("id", "89592405-c29b-3706-b1d3-b1dd3a08b227")
                .add("oucode", "C01BL00")
                .add("lja", "2570")
                .add("oucodeL1Code", "C")
                .add("courtId", "428")
                .add("oucodeL1Name", "Crown Courts")
                .add("oucodeL3Name", "Blackfriars Crown Court")
                .add("oucodeL3WelshName", "Llys Y Goron Blackfriars")
                .add("address1", "1-15 Pocock Street")
                .add("address2", "London")
                .add("postcode", "SE1 0BJ")
                .add("defaultStartTime", "10:00:00")
                .add("defaultDurationHrs", "07:00:00")
                .add("oucodeL2Code", "1")
                .add("oucodeL2Name", "London")
                .add("courtLocationCode", "0428")
                .add("region", "London")
                .add("courtrooms", courtroomsArray)
                .build();

        final JsonObject response = createObjectBuilder()
                .add("organisationunits", createArrayBuilder().add(organisationUnit))
                .build();

        final Envelope<JsonObject> mockEnvelope = Envelope.envelopeFrom(
                Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("Test").build(), 
                response
        );

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockEnvelope);
    }

    private void setupMockEmptyOrganisationUnitResponse() {
        final JsonObject response = createObjectBuilder()
                .add("organisationunits", createArrayBuilder())
                .build();

        final Envelope<JsonObject> mockEnvelope = Envelope.envelopeFrom(
                Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("Test").build(), 
                response
        );

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockEnvelope);
    }


    @Test
    public void shouldRetrieveOrganisationUnitBySpiOucode() {

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(buildOrganisationUnit());

        final OrganisationUnitReferenceData organisationUnitReferenceData = referenceDataService.retrieveOrganisationUnits(VALUE_COURT_ID_MATCHING).get(0);

        assertThat(organisationUnitReferenceData, notNullValue());
        assertThat(organisationUnitReferenceData.getId(), notNullValue());
        assertThat(organisationUnitReferenceData.getOucode(), is(VALUE_COURT_ID_MATCHING));
        assertThat(organisationUnitReferenceData.getOucodeL3WelshName(), is(WELSH_VENUE_NAME_VALUE));
    }

    @Test
    public void shouldGetVerdictTypeById() {
        final UUID verdictId = UUID.randomUUID();
        final JsonObject payload = createObjectBuilder().add(FIELD_VERDICT_TYPES, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(KEY_ID, verdictId.toString())
                                .add(FIELD_VERDICT_CODE, NGJAA))
                        .add(createObjectBuilder()
                                .add(KEY_ID, UUID.randomUUID().toString())
                                .add(FIELD_VERDICT_CODE, "OTHER"))
                ).build();
        final Envelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("name").build(), payload);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);

        final Optional<VerdictReferenceData> result = referenceDataService.getVerdictTypeById(verdictId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getVerdictCode(), Is.is(NGJAA));
        assertThat(result.get().getId(), is(verdictId));
    }

    @Test
    public void shouldGetPleaTypeById() {
        final UUID pleaId = UUID.randomUUID();
        final JsonObject payload = createObjectBuilder().add(FIELD_PLEA_STATUS_TYPES, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(KEY_ID, pleaId.toString())
                                .add(FIELD_PLEA_VALUE, GUILTY)
                                .add(PLEA_TYPE_CODE, "G")
                                .add(FIELD_PLEA_TYPE_GUILTY_FLAG, GUILTY_FLAG_YES))
                        .add(createObjectBuilder()
                                .add(KEY_ID, UUID.randomUUID().toString())
                                .add(FIELD_PLEA_VALUE, NOT_GUILTY)
                                .add(PLEA_TYPE_CODE, "NG")
                                .add(FIELD_PLEA_TYPE_GUILTY_FLAG, GUILTY_FLAG_NO)))
                .build();
        final Envelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("name").build(), payload);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);

        final Optional<PleaReferenceData> result = referenceDataService.getPleaTypeById(pleaId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getPleaTypeCode(), Is.is("G"));
        assertThat(result.get().getId(), is(pleaId));
    }

    @Test
    public void shouldReturnHearingTypesRefData() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildHearingTypes();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final HearingTypes hearingTypesRefData = referenceDataService.retrieveHearingTypes();
        assertThat(hearingTypesRefData, is(notNullValue()));
        assertThat(hearingTypesRefData.getHearingtypes().size(), is(2));
        assertThat(hearingTypesRefData.getHearingtypes().get(0).getHearingCode(), is(HEARING_CODE1));
        assertThat(hearingTypesRefData.getHearingtypes().get(0).getHearingDescription(), is(HEARING_DESCRIPTION1));
        assertThat(hearingTypesRefData.getHearingtypes().get(1).getHearingCode(), is(HEARING_CODE2));

    }

    @Test
    public void shouldReturnObservedEthnicityRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.observed-ethnicities");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataObservedEthnicity();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<ObservedEthnicityReferenceData> observedEthnicityReferenceData = referenceDataService.retrieveObservedEthnicity();

        assertThat(observedEthnicityReferenceData, is(notNullValue()));
        assertThat(observedEthnicityReferenceData.size(), is(2));

    }

    @Test
    public void shouldReturnPoliceForcesRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.police-forces");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataPoliceForces();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<PoliceForceReferenceData> policeForceReferenceDataList = referenceDataService.retrievePoliceForceCode();

        assertThat(policeForceReferenceDataList.get(0), instanceOf(PoliceForceReferenceData.class));
        assertThat(policeForceReferenceDataList.size(), is(2));
    }

    @Test
    public void shouldReturnInitiationCodesRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.initiation-types");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataInitiationTypeJsonObject();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<String> initiationCodes = referenceDataService.getInitiationCodes();

        assertThat(initiationCodes, is(notNullValue()));
        assertThat(initiationCodes.size(), is(2));

    }

    private JsonObject getMockReferenceDataInitiationCodes() {
        return JsonObjects.createObjectBuilder().add("initiationTypes",
                JsonObjects.createArrayBuilder()
                        .add(
                                JsonObjects.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 10)
                                        .add("code", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                JsonObjects.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 20)
                                        .add("code", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())

                .build();
    }

    @Test
    public void shouldReturnProsecutorsRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.licence-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataProsecutors();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final ProsecutorsReferenceData prosecutors = referenceDataService.retrieveProsecutors("string");
        assertThat(prosecutors, is(notNullValue()));
        assertThat(prosecutors.getSequenceNumber(), is(10));
        assertThat(prosecutors.getFullName(), is("fullName"));
        assertThat(prosecutors.getContactEmailAddress(), is("contact@cpp.co.uk"));
    }

    private JsonObject getMockReferenceDataProsecutors() {
        return JsonObjects.createObjectBuilder()
                .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                .add("sequenceNumber", 10)
                .add("fullName", "fullName")
                .add("contactEmailAddress", "contact@cpp.co.uk")
                .build();
    }

    @Test
    public void shouldReturnOffenceDataRefData() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedataoffences.query.offences-list");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenceData();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final MigratedOffence offenceData = MigratedOffence.migratedOffence().withOffenceCode("OFCODE_12").withOffenceCommittedDate(LocalDate.of(2018, 10, 10)).withChargeDate(LocalDate.of(2018, 10, 10)).build();

        final List<OffenceReferenceData> offenceReferenceData = referenceDataService.retrieveOffenceData(offenceData, "S");

        assertThat(offenceReferenceData, is(notNullValue()));
        assertThat(offenceReferenceData.size(), is(2));
        assertThat(offenceReferenceData.get(0).getCjsOffenceCode(), is("cjsOffenceCode"));
    }

    @Test
    public void shouldReturnAllParentBundleRefData() {

        final Metadata metadata = getMockMetadataWithName("referencedata.query.get-all-parent-bundle-section");

        final JsonObject mockRefDataJsonObject = getMockReferenceDataAllParentBundleSectionReferenceData(
                10, "1", "IDPC", Boolean.TRUE, "IDPC");

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, mockRefDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<ParentBundleSectionReferenceData> parentBundleSectionReferenceDataList =
                referenceDataService.getAllParentBundleSection(mockRefDataEnvelope.metadata());

        assertThat(parentBundleSectionReferenceDataList.size(), is(1));
        assertThat(parentBundleSectionReferenceDataList.get(0).getId(), is(notNullValue()));
        assertThat(parentBundleSectionReferenceDataList.get(0).getCpsBundleCode(), is("1"));
        assertThat(parentBundleSectionReferenceDataList.get(0).getTargetSectionCode(), is("IDPC"));

    }


    private void verifyEnvelopeData(final JsonEnvelope envelope, final String expectedName) {
        assertThat(envelope, is(notNullValue()));
        final Metadata requestMetadata = envelope.metadata();
        assertThat(requestMetadata.name(), is(expectedName));
    }

    private Metadata getMockMetadataWithName(final String name) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(name)
                .withStreamId(randomUUID())
                .withUserId("mr user")
                .build();
    }

    private Envelope<JsonObject> buildOrganisationUnit() {
        final JsonObject organisationUnit = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(OUCODE, VALUE_COURT_ID_MATCHING)
                .add(OUCODE_L_1_CODE, "B")
                .add(WELSH_VENUE_NAME, WELSH_VENUE_NAME_VALUE)
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("Test"),
                createObjectBuilder()
                        .add(ORGANISATION_UNITS, createArrayBuilder()
                                .add(organisationUnit))
                        .build());

    }


}