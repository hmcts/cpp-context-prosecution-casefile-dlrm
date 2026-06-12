package uk.gov.moj.cpp.pcfdlrm.service;

import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceDataQueryService {
    List<ReferenceDataCountryNationality> retrieveCountryNationality();

    List<SummonsCodeReferenceData> retrieveSummonsCodes();

    List<DocumentTypeAccessReferenceData> retrieveDocumentsTypeAccess();

    List<AlcoholLevelMethodReferenceData> retrieveAlcoholLevelMethods();

    List<BailStatusReferenceData> retrieveBailStatuses();

    List<OffenderCodeReferenceData> retrieveOffenderCodes();

    List<SelfdefinedEthnicityReferenceData> retrieveSelfDefinedEthnicity();

    List<ObservedEthnicityReferenceData> retrieveObservedEthnicity();

    List<VehicleCodeReferenceData> retrieveVehicleCodes();

    Optional<OrganisationUnitWithCourtroomReferenceData> retrieveOrganisationUnitWithCourtroom(String ouCode);

    Optional<OrganisationUnitWithCourtroomsReferenceData> retrieveOrganisationUnitWithCourtrooms(String ouCode);

    List<OrganisationUnitReferenceData> retrieveOrganisationUnits(String ouCode);

    List<String> getInitiationCodes();

    HearingTypes retrieveHearingTypes();

    ProsecutorsReferenceData retrieveProsecutors(String originatingOrganisation);

    List<CaseMarker> getCaseMarkerDetails();

    List<OffenceReferenceData> retrieveOffenceData(MigratedOffence offence, String initiationCode);

    List<OffenceReferenceData> retrieveOffenceDataList(List<String> cjsOffenceCodeList);

    List<PoliceForceReferenceData> retrievePoliceForceCode();

    List<ParentBundleSectionReferenceData> getAllParentBundleSection(final Metadata metadata);

    List<ModeOfTrialReasonsReferenceData> retrieveModeOfTrialReasons();

    Optional<PleaReferenceData> getPleaTypeById(final UUID id);

    Optional<VerdictReferenceData> getVerdictTypeById(final UUID id);

    Optional<LjaDetails> getLjaDetails(String lja, String id);
}
