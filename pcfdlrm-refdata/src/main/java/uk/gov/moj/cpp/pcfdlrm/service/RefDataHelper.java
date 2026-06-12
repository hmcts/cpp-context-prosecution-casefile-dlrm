package uk.gov.moj.cpp.pcfdlrm.service;

import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceDateCodeReferenceData.offenceDateCodeReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenderCodeReferenceData.offenderCodeReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.LicenceCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.MojOffences;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceDateCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PoliceRankReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefDataHelper {

    private static final String FIELD_VALID_FROM = "validFrom";
    private static final String FIELD_ID = "id";
    private static final String OUCODE = "oucode";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataHelper.class);

    private RefDataHelper() {
    }

    private static Optional<String> getStringFromJson(final String name, final JsonObject jsonObject) {
        return Optional.ofNullable(jsonObject.getString(name, null));
    }

    private static Optional<Integer> getIntFromJson(final String name, final JsonObject jsonObject) {
        final JsonNumber jsonNumber = jsonObject.getJsonNumber(name);
        if (jsonNumber != null) {
            return Optional.of(jsonNumber.intValue());
        }
        return Optional.empty();
    }

    public static Function<JsonValue, PoliceRankReferenceData> asPoliceRankRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), PoliceRankReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal PoliceRankReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, ProsecutorsReferenceData> asProsecutorRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), ProsecutorsReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal ProsecutorsReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, LicenceCodeReferenceData> asLicenceCodeRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), LicenceCodeReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal LicenceCodeReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, PoliceForceReferenceData> asPoliceForceRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), PoliceForceReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal PoliceForceReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, SelfdefinedEthnicityReferenceData> asSelfDefinedEnthnicityRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), SelfdefinedEthnicityReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal SelfdefinedEthnicityReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, ObservedEthnicityReferenceData> asObservedEnthnicityRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), ObservedEthnicityReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal ObservedEthnicityReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, CustodyStatusReferenceData> asCustodyStatusRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), CustodyStatusReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal CustodyStatusReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, BailStatusReferenceData> asBailStatusReferenceData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), BailStatusReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal BailStatusReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, VehicleCodeReferenceData> asVehicleCodeRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), VehicleCodeReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal VehicleCodeReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, ModeOfTrialReasonsReferenceData> asModeOfTrialReasonsRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), ModeOfTrialReasonsReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal ModeOfTrialReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, AlcoholLevelMethodReferenceData> asAlcoholLevelMethodRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), AlcoholLevelMethodReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal AlcoholLevelMethodReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, SummonsCodeReferenceData> asSummonsCodeRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), SummonsCodeReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal SummonsCodeReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), DocumentTypeAccessReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }


    public static Function<JsonValue, ReferenceDataCountryNationality> asCountryNationalityRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), ReferenceDataCountryNationality.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal country nationality reference data. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, OffenceDateCodeReferenceData> asOffenceDateCodeRefData() {
        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final OffenceDateCodeReferenceData.Builder builder = offenceDateCodeReferenceData();
            getStringFromJson(FIELD_ID, refDataObject).map(UUID::fromString).ifPresent(builder::withId);
            getIntFromJson("seqNum", refDataObject).ifPresent(builder::withSeqNum);
            getStringFromJson("dateCode", refDataObject).ifPresent(builder::withDateCode);
            getStringFromJson("dateCodeDescription", refDataObject).ifPresent(builder::withDateCodeDescription);
            getStringFromJson(FIELD_VALID_FROM, refDataObject).ifPresent(builder::withValidFrom);
            return builder.build();
        };
    }

    public static Function<JsonValue, CaseMarker> asCaseMarkerRefData() {
        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final CaseMarker.Builder builder = caseMarker();
            getStringFromJson(FIELD_ID, refDataObject).map(UUID::fromString).ifPresent(builder::withMarkerTypeId);
            getStringFromJson("label", refDataObject).ifPresent(builder::withMarkerTypeDescription);
            getStringFromJson("code", refDataObject).ifPresent(builder::withMarkerTypeCode);
            return builder.build();
        };
    }


    public static Function<JsonValue, OffenderCodeReferenceData> asOffenderCodeRefData() {
        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final OffenderCodeReferenceData.Builder builder = offenderCodeReferenceData();
            getStringFromJson(FIELD_ID, refDataObject).map(UUID::fromString).ifPresent(builder::withId);
            getIntFromJson("seqNo", refDataObject).ifPresent(builder::withSeqNum);
            getStringFromJson("code", refDataObject).ifPresent(builder::withOffenderCode);
            getStringFromJson("description", refDataObject).ifPresent(builder::withOffenderCodeDescription);
            getStringFromJson(FIELD_VALID_FROM, refDataObject).ifPresent(builder::withValidFrom);
            return builder.build();
        };
    }

    public static Function<JsonValue, OrganisationUnitWithCourtroomReferenceData> asOrganisationUnitWithCourtroomRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), OrganisationUnitWithCourtroomReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal OrganisationUnitWithCourtroomReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };

    }

    public static Function<JsonValue, OrganisationUnitReferenceData> asOrganisationUnitRefData() {
        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final OrganisationUnitReferenceData.Builder builder = organisationUnitReferenceData();

            buildOrganisationUnit(builder, refDataObject);
            return builder.build();
        };
    }

    public static Function<JsonValue, MojOffences> asMojOffencesRefData() {
        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final MojOffences.Builder builder = MojOffences.mojOffences();

            buildMojOffences(builder, refDataObject);
            return builder.build();
        };
    }

    private static void buildMojOffences(final MojOffences.Builder builder, final JsonObject refDataObject) {
        getStringFromJson("cjsOffenceCode;", refDataObject).ifPresent(builder::withCjsOffenceCode);
        getStringFromJson("offenceType;", refDataObject).ifPresent(builder::withOffenceType);
    }

    public static Function<JsonValue, ParentBundleSectionReferenceData> asParentBundleSectionRefData() {

        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final ParentBundleSectionReferenceData.Builder builder = ParentBundleSectionReferenceData.parentBundleSectionReferenceData();

            buildParentBundleSection(builder, refDataObject);
            return builder.build();
        };
    }

    public static Function<JsonValue, ParentBundleSectionReferenceData> asParentBundleSectionRefDataList() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), ParentBundleSectionReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal PoliceRankReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    private static void buildParentBundleSection(ParentBundleSectionReferenceData.Builder builder, JsonObject refDataObject) {
        getStringFromJson("id", refDataObject).map(UUID::fromString).ifPresent(builder::withId);
        getStringFromJson("cpsBundleCode", refDataObject).ifPresent(builder::withCpsBundleCode);
        getStringFromJson("targetSectionCode", refDataObject).ifPresent(builder::withTargetSectionCode);
    }


    private static void buildOrganisationUnit(OrganisationUnitReferenceData.Builder builder, JsonObject refDataObject) {
        getStringFromJson("id", refDataObject).ifPresent(builder::withId);
        getStringFromJson("address1", refDataObject).ifPresent(builder::withAddress1);
        getStringFromJson("address2", refDataObject).ifPresent(builder::withAddress2);
        getStringFromJson("address3", refDataObject).ifPresent(builder::withAddress3);
        getStringFromJson("address4", refDataObject).ifPresent(builder::withAddress4);
        getStringFromJson("address5", refDataObject).ifPresent(builder::withAddress5);
        getStringFromJson("defaultDurationHrs", refDataObject).ifPresent(builder::withDefaultDurationHrs);
        getStringFromJson("defaultStartTime", refDataObject).ifPresent(builder::withDefaultStartTime);
        getStringFromJson("isWelsh", refDataObject).map(Boolean::new).ifPresent(builder::withIsWelsh);
        getStringFromJson(OUCODE, refDataObject).ifPresent(builder::withOucode);
        getStringFromJson("oucodeL1Code", refDataObject).ifPresent(builder::withOucodeL1Code);
        getStringFromJson("oucodeL1Name", refDataObject).ifPresent(builder::withOucodeL1Name);
        getStringFromJson("oucodeL3Code", refDataObject).ifPresent(builder::withOucodeL3Code);
        getStringFromJson("oucodeL3Name", refDataObject).ifPresent(builder::withOucodeL3Name);
        getStringFromJson("oucodeL3WelshName", refDataObject).ifPresent(builder::withOucodeL3WelshName);
        getStringFromJson("postcode", refDataObject).ifPresent(builder::withPostcode);
        getStringFromJson("welshAddress1", refDataObject).ifPresent(builder::withWelshAddress1);
        getStringFromJson("welshAddress2", refDataObject).ifPresent(builder::withWelshAddress2);
        getStringFromJson("welshAddress3", refDataObject).ifPresent(builder::withWelshAddress3);
        getStringFromJson("welshAddress4", refDataObject).ifPresent(builder::withWelshAddress4);
        getStringFromJson("welshAddress5", refDataObject).ifPresent(builder::withWelshAddress5);
        getStringFromJson("welshAddress5", refDataObject).ifPresent(builder::withWelshAddress5);
        getStringFromJson("courtLocationCode", refDataObject).ifPresent(builder::withCourtLocationCode);
        getStringFromJson("lja", refDataObject).ifPresent(builder::withLja);
    }

    public static Function<JsonValue, HearingType> asHearingTypesRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), HearingType.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal HearingType. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };

    }

    public static Function<JsonValue, OffenceReferenceData> asOffenceRefData() {

        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), OffenceReferenceData.class);
            } catch (final IOException e) {
                LOGGER.error("Unable to unmarshal OffenceReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, PleaReferenceData> asPleaReferenceData() {

        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), PleaReferenceData.class);
            } catch (final IOException e) {
                LOGGER.error("Unable to unmarshal PleaReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, OrganisationUnitWithCourtroomsReferenceData> asOrganisationUnitWithCourtroomsRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), OrganisationUnitWithCourtroomsReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal OrganisationUnitWithCourtroomsReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, VerdictReferenceData> asVerdictReferenceData() {

        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), VerdictReferenceData.class);
            } catch (final IOException e) {
                LOGGER.error("Unable to unmarshal VerdictReferenceData. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

    public static Function<JsonValue, CourtApplicationType> asApplicationTypeRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), CourtApplicationType.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal CourtApplicationType. Payload :{}", jsonValue.toString(), e);
                return null;
            }
        };
    }

}
