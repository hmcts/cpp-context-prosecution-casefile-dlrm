package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtReferral.courtReferral;
import static uk.gov.justice.core.courts.InitiateCourtProceedings.initiateCourtProceedings;
import static uk.gov.justice.core.courts.InitiationCode.valueFor;
import static uk.gov.justice.core.courts.Marker.marker;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.pcfdlrm.domain.OffenceIdsWithCourtHearingLocation;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

public class MigratedCaseToProsecutionCaseConverter implements Converter<MigratedCaseFileReceived, InitiateCourtProceedings> {

    @Inject
    private ProsecutionCaseFileMigratedDefendantToCCDefendantConverter prosecutionCaseFileMigratedDefendantToCCDefendantConverter;

    @Inject
    private ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter;

    @Override
    public InitiateCourtProceedings convert(final MigratedCaseFileReceived source) {
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final CaseDetails caseDetails = source.getReceiveMigratedCaseFile().getMigratedCaseDetails().getCaseDetails();
        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = source
                .getReferenceDataVO()
                .getOrganisationUnitWithCourtroomReferenceData();

        final ReferenceDataVO referenceDataVO = source.getReferenceDataVO();

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(source.getReferenceDataVO());
        paramsVO.setCaseId(caseDetails.getCaseId());
        paramsVO.setChannel(source.getReceiveMigratedCaseFile().getChannel());
        paramsVO.setReceivedFromCourtOUCode(caseDetails.getCourtReceivedFromCode());
        paramsVO.setInitiationCode(caseDetails.getInitiationCode());
        paramsVO.setMigrationSourceSystemName(source.getReceiveMigratedCaseFile().getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName());

        source.getMigratedHearingWithReferenceDataList().stream()
                .map(hearing -> {
                    String courtHearingLocation = hearing.getReferenceDataVO().getCourtHearingLocation();
                    List<UUID> offenceIds = hearing.getMigratedDefendantWithOffences().stream()
                            .flatMap(defendantWithOffences -> defendantWithOffences.getOffenceids().stream())
                            .toList();
                    return new OffenceIdsWithCourtHearingLocation(offenceIds, courtHearingLocation);
                })
                .filter(offenceIdsWithLocation -> !offenceIdsWithLocation.getOffenceIds().isEmpty())
                .forEach(paramsVO::addOffenceIdsWithCourtHearingLocation);

        organisationUnitWithCourtroomReferenceData.ifPresent(unitWithCourtroomReferenceData -> paramsVO.setOucodeL1Code(unitWithCourtroomReferenceData.getOucodeL1Code()));
        List<MigratedDefendant> migratedDefendants = source.getReceiveMigratedCaseFile().getMigratedCaseDetails().getDefendants();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(buildProsecutorCaseIdentifier(caseDetails, source.getReferenceDataVO()))
                .withCaseMarkers(caseDetails.getCaseMarkers() != null && !caseDetails.getCaseMarkers().isEmpty() ? buildCaseMarkers(source) : null)
                .withDefendants(prosecutionCaseFileMigratedDefendantToCCDefendantConverter.convert(source.getReceiveMigratedCaseFile().getMigratedCaseDetails().getDefendants(), paramsVO))
                .withId(caseDetails.getCaseId())
                .withInitiationCode(valueFor(caseDetails.getInitiationCode()).orElse(null))
                .withSummonsCode(caseDetails.getSummonsCode())
                .withOriginatingOrganisation(caseDetails.getOriginatingOrganisation())
                .withCpsOrganisation(caseDetails.getCpsOrganisation())
                .withStatementOfFacts(nonNull(migratedDefendants) && !migratedDefendants.isEmpty() ? migratedDefendants.get(0).getOffences().get(0).getStatementOfFacts() : "")
                .withStatementOfFactsWelsh(nonNull(migratedDefendants) && !migratedDefendants.isEmpty() ? migratedDefendants.get(0).getOffences().get(0).getStatementOfFactsWelsh() : "")
                .withClassOfCase(caseDetails.getClassOfCase())
                .withTrialReceiptType(caseDetails.getTrialReceiptType())
                .withReceiptType(caseDetails.getReceiptType())
                .withMigrationSourceSystem(buildMigrationSourceSystem(source.getReceiveMigratedCaseFile().getMigratedCaseDetails().getMigrationSourceSystem()))
                .withCommittalDate(Optional.ofNullable(caseDetails.getDateOfCommittal()).map(LocalDate::toString).orElse(null))
                .withRetrialIndicator(caseDetails.getRetrialIndicator())
                .withReceivingCourt(referenceDataVO.getReceivingCourtOrganisationUnit().map(this::getCourtCentre).orElse(null))
                .withSendingCourt(referenceDataVO.getSendingCourtOrganisationUnit().map(this::getCourtCentre).orElse(null))
                .withDateOfSendingCase(Optional.ofNullable(caseDetails.getDateOfSending()).map(LocalDate::toString).orElse(null))
                .build();

        prosecutionCases.add(prosecutionCase);

        final CourtReferral.Builder courtReferralBuilder = courtReferral()
                .withProsecutionCases(prosecutionCases);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter.
                convert(source.getMigratedHearingWithReferenceDataList(), paramsVO);

        if (CollectionUtils.isNotEmpty(listHearingRequests)) {
            courtReferralBuilder.withListHearingRequests(listHearingRequests);
        }

        return initiateCourtProceedings()
                .withInitiateCourtProceedings(courtReferralBuilder.build())
                .build();
    }

    private MigrationSourceSystem buildMigrationSourceSystem(final uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem migrationSourceSystem) {
        return  MigrationSourceSystem.migrationSourceSystem().withMigrationSourceSystemName(migrationSourceSystem.getMigrationSourceSystemName()).
                withMigrationSourceSystemCaseIdentifier(migrationSourceSystem.getMigrationSourceSystemCaseIdentifier())
                .build();
    }

    private CourtCentre getCourtCentre(final OrganisationUnitReferenceData organisationUnitReferenceData) {
            final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
            courtCentreBuilder.withId(fromString(organisationUnitReferenceData.getId()));
            courtCentreBuilder.withName(organisationUnitReferenceData.getOucodeL3Name());
            courtCentreBuilder.withWelshName(organisationUnitReferenceData.getOucodeL3WelshName());
            return courtCentreBuilder.build();

    }

    @SuppressWarnings({"squid:S1135"})
    private ProsecutionCaseIdentifier buildProsecutorCaseIdentifier(final CaseDetails caseDetails, final ReferenceDataVO referenceDataVO) {
        final ProsecutorsReferenceData prosecutorsReferenceData = referenceDataVO.getProsecutorsReferenceData();
        return prosecutionCaseIdentifier().withCaseURN(caseDetails.getProsecutorCaseReference())
                .withProsecutionAuthorityId(prosecutorsReferenceData.getId())
                .withProsecutionAuthorityCode(prosecutorsReferenceData.getShortName())
                .withAddress(ofNullable(prosecutorsReferenceData.getAddress()).map(address -> Address.address()
                        .withAddress1(address.getAddress1())
                        .withAddress2(address.getAddress2())
                        .withAddress3(address.getAddress3())
                        .withAddress4(address.getAddress4())
                        .withAddress5(address.getAddress5())
                        .withPostcode(address.getPostcode()).build()).orElse(null))
                .withMajorCreditorCode(prosecutorsReferenceData.getMajorCreditorCode())
                .withProsecutionAuthorityName(prosecutorsReferenceData.getFullName())
                .withProsecutionAuthorityOUCode(prosecutorsReferenceData.getOucode())
                .withContact(buildContact(prosecutorsReferenceData.getInformantEmailAddress(), prosecutorsReferenceData.getContactEmailAddress()))
                .withProsecutorCategory(prosecutorsReferenceData.getProsecutorCategory())
                .build();

    }

    private ContactNumber buildContact(String informantEmailAddress, String contactEmailAddress) {
        return ofNullable(informantEmailAddress)
                .map(email -> contactNumber().withPrimaryEmail(email).build())
                .orElse(ofNullable(contactEmailAddress)
                        .map(email -> contactNumber().withPrimaryEmail(email).build())
                        .orElse(null));
    }

    private List<Marker> buildCaseMarkers(final MigratedCaseFileReceived migratedCaseFileReceived) {

        final List<CaseMarker> allCaseMarkers = getEnrichedCaseMarkers(migratedCaseFileReceived);

        return allCaseMarkers.stream()
                .map(this::buildMarker)
                .toList();
    }

    private List<CaseMarker> getEnrichedCaseMarkers(final MigratedCaseFileReceived migratedCaseFileReceived) {
        return migratedCaseFileReceived.getReceiveMigratedCaseFile().getMigratedCaseDetails().getCaseDetails().getCaseMarkers().stream()
                .map(caseMarker -> getEnrichedCaseMarkerFromList(caseMarker.getMarkerTypeCode(), migratedCaseFileReceived.getReferenceDataVO().getCaseMarkers()))
                .filter(Objects::nonNull)
                .toList();
    }

    private CaseMarker getEnrichedCaseMarkerFromList(final String caseMarkerTypeCode, final List<CaseMarker> caseMarkers) {
        for (final CaseMarker caseMarker : caseMarkers) {
            if (caseMarker.getMarkerTypeCode().equals(caseMarkerTypeCode)) {
                return caseMarker;
            }
        }
        return null;
    }

    private Marker buildMarker(final CaseMarker caseMarker) {
        return marker()
                .withId(randomUUID())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeId())
                .build();
    }

}
