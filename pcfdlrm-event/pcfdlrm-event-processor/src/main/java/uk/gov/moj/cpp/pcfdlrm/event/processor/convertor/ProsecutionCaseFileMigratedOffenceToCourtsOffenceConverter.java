package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.lang.Integer.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.CommittingCourt.committingCourt;
import static uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit;
import static uk.gov.justice.core.courts.InitiationCode.O;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.MCC;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtIndicatedSentence;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.VehicleCodeType;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedAllocationDecision;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter implements ParameterisedConverter<List<MigratedOffence>, List<uk.gov.justice.core.courts.Offence>, ParamsVO> {

    private static final Logger LOGGER = getLogger(ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.class);

    private static final String MAGISTRATES_COURT_HOUSE_TYPE = "B";
    private static final String SUMMARY_ONLY_MODE_OF_TRIAL = "Summary";
    private static final String GUILTY = "GUILTY";
    private static final String INDICATED_GUILTY = "INDICATED_GUILTY";
    private static final String SEE_INDICTMENT_OR_CHARGE_SHEET_FOR_PARTICULARS = "See indictment or charge sheet for particulars";
    private static final String XHIBIT = "XHIBIT";
    private static final int COURT_HEARING_OU_CODE_LENGTH = 7;
    public static final String SUMMARY_ONLY_OFFENCE = "Summary-only offence";

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public List<Offence> convert(final List<MigratedOffence> source, final ParamsVO param) {
        return source.stream().map(offence -> buildOffence(offence, param)).toList();
    }

    private uk.gov.justice.core.courts.Offence buildOffence(final MigratedOffence offence, final ParamsVO paramsVO) {
        final ReferenceDataVO referenceDataVO = paramsVO.getReferenceDataVO();
        final boolean isXhibit = paramsVO.getMigrationSourceSystemName().equals(XHIBIT);
        final String modeOfTrialDerived = getModeOfTrialDerived(offence.getOffenceCode(), referenceDataVO);

        final Plea plea = convertPlea(offence, referenceDataVO);
        final LocalDate convictionDate = getConvictionDate(offence, referenceDataVO);
        final String offenceWording = isXhibit ? getWording(offence) : offence.getOffenceWording();
        final CustodyTimeLimit custodyTimeLimit = ofNullable(paramsVO.getCustodyTimeLimit())
                .map(e-> custodyTimeLimit()
                .withTimeLimit(e.toString())
                .build()).orElse(null);

        Verdict verdict = convertVerdict(offence, referenceDataVO, plea);
        final Offence.Builder offenceBuilder = offence()
                .withId(offence.getOffenceId())
                .withArrestDate(getDate(offence.getArrestDate()))
                .withChargeDate(getDate(offence.getChargeDate()))
                .withCount(ofNullable(offence.getCount()).orElseGet(()-> 0))
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(getOffenceId(offence.getOffenceCode(), referenceDataVO))
                .withOffenceFacts(buildOffenceFacts(offence))
                .withOffenceTitle(getOffenceTitle(offence.getOffenceCode(), referenceDataVO))
                .withOffenceTitleWelsh(getOffenceWelshTitle(offence.getOffenceCode(), referenceDataVO))
                .withOrderIndex(offence.getOffenceSequenceNumber())
                .withStartDate(offence.getOffenceCommittedDate().toString())
                .withEndDate(getDate(offence.getOffenceCommittedEndDate()))
                .withWording(offenceWording)
                .withWordingWelsh(isXhibit ?
                        (isNotEmpty(offence.getOffenceWordingWelsh()) ? offence.getOffenceWordingWelsh() : offenceWording) :
                        offence.getOffenceWordingWelsh())
                .withModeOfTrial(modeOfTrialDerived)
                .withOffenceLegislation(getOffenceLegislation(offence.getOffenceCode(), referenceDataVO))
                .withOffenceLegislationWelsh(getOffenceLegislationWelsh(offence.getOffenceCode(), referenceDataVO))
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withCommittingCourt(getCommittingCourtFromReferenceData(paramsVO))
                .withPlea(plea)
                .withVerdict(verdict)
                .withAllocationDecision(buildAllocationDecision(offence, paramsVO))
                .withDvlaOffenceCode(getDvlaCode(offence.getOffenceCode(), referenceDataVO))
                .withMaxPenalty(getMaxPenalty(offence.getOffenceCode(), referenceDataVO))
                .withConvictingCourt(getConvictingCourt(offence, paramsVO, plea))
                .withCustodyTimeLimit(isCustodyLimitTobeSet(offence, plea, referenceDataVO) ? custodyTimeLimit : null);

        if (convictionDate != null) {
            offenceBuilder.withConvictionDate(convictionDate.toString());
        } else {
            ofNullable(deriveConvictionDateFromVerdict(verdict))
                    .ifPresent(offenceBuilder::withConvictionDate);
        }

        return offenceBuilder.build();
    }

    private boolean isCustodyLimitTobeSet(final MigratedOffence offence, final Plea plea, final ReferenceDataVO referenceDataVO) {
        boolean guiltyPlea = hasGuiltyPlea(plea);
        final boolean guiltyVerdict = hasGuiltyVerdict(offence, referenceDataVO);
        return !guiltyPlea && !guiltyVerdict ;

    }

    private String getWording(final MigratedOffence offence) {
        return StringUtils.isNotEmpty(offence.getOffenceWording()) ? offence.getOffenceWording() : SEE_INDICTMENT_OR_CHARGE_SHEET_FOR_PARTICULARS;
    }

    private String getDvlaCode(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getDvlaCode).orElse(null);
    }

    private String getMaxPenalty(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getMaxPenalty).orElse(null);
    }

    private boolean hasVerdict(final MigratedOffence offence) {
        return ofNullable(offence)
                .map(MigratedOffence::getVerdict)
                .map(MigratedVerdict::getId)
                .isPresent();
    }
    private boolean hasPlea(final MigratedOffence offence){
        return ofNullable(offence)
                .map(MigratedOffence::getPlea)
                .map(MigratedPlea::getId)
                .isPresent();
    }

    private CourtCentre getConvictingCourt(final MigratedOffence offence, final ParamsVO paramsVO, Plea plea) {
        if(!hasVerdict(offence)  && !hasPlea(offence)){
            return null;
        }

        CourtCentre courtCentre = isValidOuCode(offence.getConvictingCourtCode()) ? getCourtCentre(offence.getConvictingCourtCode()) : null;

        if (isNull(courtCentre) && (hasGuiltyPlea(plea) || hasGuiltyVerdict(offence, paramsVO.getReferenceDataVO()))) {
           courtCentre = getConvictingCourtFromHearing(paramsVO, offence.getOffenceId());
        }
        return courtCentre;
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGTH == ouCode.length();
    }

    private CourtCentre getConvictingCourtFromHearing(final ParamsVO paramsVO, final UUID offenceId) {
        return paramsVO.getOffenceIdsWithCourtHearingLocationList()
                .stream()
                .filter(offenceIdsWithLocation -> Objects.nonNull(offenceIdsWithLocation.getOffenceIds()) &&
                        offenceIdsWithLocation.getOffenceIds().contains(offenceId))
                .findFirst()
                .map(offenceIdsWithLocation -> {
                    String ouCode = offenceIdsWithLocation.getCourtHearingLocation();
                    final String convictingCourtCode = ouCode.substring(0, ouCode.length() - 2) + "00";
                    return getCourtCentre(convictingCourtCode);
                })
                .orElse(null);
    }

    private CourtCentre getCourtCentre(String convictingCourtCode) {
        final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnits(convictingCourtCode);
        LOGGER.info("convictingCourtCode is {} ", convictingCourtCode);

        if (CollectionUtils.isNotEmpty(organisationUnits)) {
            final OrganisationUnitReferenceData organisationUnitReferenceData = organisationUnits.get(0);
            final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
            courtCentreBuilder.withId(fromString(organisationUnitReferenceData.getId()));
            courtCentreBuilder.withName(organisationUnitReferenceData.getOucodeL3Name());
            courtCentreBuilder.withWelshName(organisationUnitReferenceData.getOucodeL3WelshName());
            if (nonNull(organisationUnitReferenceData.getCourtLocationCode())) {
                courtCentreBuilder.withCourtLocationCode(organisationUnitReferenceData.getCourtLocationCode());
            } else {
                final Optional<LjaDetails> ljaDetailsOptional = referenceDataQueryService.getLjaDetails(organisationUnitReferenceData.getLja(), organisationUnitReferenceData.getId());
                ljaDetailsOptional.ifPresent(courtCentreBuilder::withLja);
            }
            return courtCentreBuilder.build();
        }
        return null;
    }

    /**
     * Regarding whether motReasonId exists in offence It returns matching motreasonId with
     * referenceData or  returns a modeOfTrialReason which description is Summary-only offence
     *
     * @param offence  - the offence to use motReasonId
     * @param paramsVO - which includes Reference Data with all ModeOfTrialReasons
     * @return ModeOfTrialReasonsReferenceData
     **/
    private ModeOfTrialReasonsReferenceData retrieveModeOfTrialReason(final MigratedOffence offence, final ParamsVO paramsVO) {
        return paramsVO.getReferenceDataVO().getModeOfTrialReasonsReferenceData().stream()
                .filter(mot -> validateAllocationDecisionWhenExist(offence, mot) || validateAllocationDecisionWhenNotExist(offence, mot))
                .findFirst()
                .orElse(null);
    }

    private boolean validateAllocationDecisionWhenExist(final MigratedOffence offence, final ModeOfTrialReasonsReferenceData mot) {
        return nonNull(offence.getAllocationDecision()) &&
                nonNull(offence.getAllocationDecision().getMotReasonId()) &&
                mot.getId().equals(offence.getAllocationDecision().getMotReasonId().toString());
    }

    private static boolean validateAllocationDecisionWhenNotExist(final MigratedOffence offence, final ModeOfTrialReasonsReferenceData mot) {
        return (isNull(offence.getAllocationDecision()) || isNull(offence.getAllocationDecision().getMotReasonId())) &&
                nonNull(offence.getReferenceData()) &&
                nonNull(offence.getReferenceData().getModeOfTrialDerived()) &&
                SUMMARY_ONLY_MODE_OF_TRIAL.equals(offence.getReferenceData().getModeOfTrialDerived()) &&
                SUMMARY_ONLY_OFFENCE.equals(mot.getDescription());
    }

    private AllocationDecision buildAllocationDecision(final MigratedOffence offence, final ParamsVO paramsVO) {

        final ModeOfTrialReasonsReferenceData modeOfTrialReason = retrieveModeOfTrialReason(offence, paramsVO);

        if (nonNull(modeOfTrialReason)) {
            return allocationDecision()
                    .withOffenceId(offence.getOffenceId())
                    .withMotReasonId(fromString(modeOfTrialReason.getId()))
                    .withMotReasonCode(modeOfTrialReason.getCode())
                    .withMotReasonDescription(modeOfTrialReason.getDescription())
                    .withSequenceNumber(valueOf(modeOfTrialReason.getSeqNum()))
                    .withCourtIndicatedSentence(buildCourtIndicatedSentence(offence))
                    .build();
        }
        return null;
    }

    private CourtIndicatedSentence buildCourtIndicatedSentence(final MigratedOffence offence) {
        return ofNullable(offence.getAllocationDecision())
                .map(MigratedAllocationDecision::getCourtIndicatedSentence)
                .map(e -> CourtIndicatedSentence.courtIndicatedSentence()
                        .withCourtIndicatedSentenceTypeId(e.getCourtIndicatedSentenceTypeId())
                        .withCourtIndicatedSentenceDescription(e.getCourtIndicatedSentenceDescription())
                        .build())
                .orElse(null);
    }

    private String getDate(final LocalDate date) {
        return nonNull(date) ? date.toString() : null;
    }

    private String getOffenceLegislation(String offenceCode, ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getLegislation).orElse(null);
    }

    private String getOffenceLegislationWelsh(String offenceCode, ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getLegislationWelsh).orElse(null);
    }

    private OffenceFacts buildOffenceFacts(final MigratedOffence offence) {
        final VehicleCode vehicleCode = getVehicleCode(offence);
        final String vehicleRegistration = buildVehicleRegistration(offence);
        final String alcoholReadingMethodCode = offence.getAlcoholRelatedOffence() != null ? offence.getAlcoholRelatedOffence().getAlcoholLevelMethod() : null;
        final Integer alcoholReadingAmount = getAlcoholReadingAmount(offence);

        if (vehicleCode == null && isEmpty(vehicleRegistration) && isEmpty(alcoholReadingMethodCode) && alcoholReadingAmount == null) {
            return null;
        }

        return OffenceFacts.offenceFacts()
                .withVehicleCode(vehicleCode)
                .withVehicleRegistration(vehicleRegistration)
                .withAlcoholReadingMethodCode(alcoholReadingMethodCode)
                .withAlcoholReadingMethodDescription(getAlcoholLevelMethodDescription(alcoholReadingMethodCode))
                .withAlcoholReadingAmount(alcoholReadingAmount)
                .build();
    }

    private String getAlcoholLevelMethodDescription(final String alcoholMethodCode) {
        final List<AlcoholLevelMethodReferenceData> alcoholLevelMethodReferenceData = referenceDataQueryService.retrieveAlcoholLevelMethods();
        return alcoholLevelMethodReferenceData.stream().filter(am -> am.getMethodCode().equals(alcoholMethodCode))
                .map(AlcoholLevelMethodReferenceData::getMethodDescription)
                .findFirst()
                .orElse(null);
    }

    private Integer getAlcoholReadingAmount(final MigratedOffence offence) {
        return (offence.getAlcoholRelatedOffence() != null && offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() != null) ? offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() : null;
    }

    private String buildVehicleRegistration(final MigratedOffence offence) {
        return (offence.getVehicleRelatedOffence() != null && offence.getVehicleRelatedOffence().getVehicleRegistrationMark() != null) ? offence.getVehicleRelatedOffence().getVehicleRegistrationMark() : null;
    }

    private VehicleCode getVehicleCode(final MigratedOffence offence) {

        if (null != offence.getVehicleRelatedOffence()) {
            final Optional<VehicleCode> vehicleCodeForCC = VehicleCodeType.valueFor(offence.getVehicleRelatedOffence().getVehicleCode());
            if (vehicleCodeForCC.isPresent()) {
                return vehicleCodeForCC.get();
            }
        }
        return null;
    }

    private UUID getOffenceId(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getOffenceId).orElse(null);
    }

    private String getModeOfTrialDerived(final String offenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getModeOfTrialDerived).orElse(null);
    }

    private String getOffenceTitle(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getTitle).orElse(null);
    }

    private String getOffenceWelshTitle(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getTitleWelsh).orElse(null);
    }

    private Optional<OffenceReferenceData> getOffenceReferenceData(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        return referenceDataVO.getOffenceReferenceData().stream().
                filter(offenceReferenceData1 -> offenceReferenceData1.getCjsOffenceCode().equals(cjsOffenceCode)).findFirst();
    }

    private boolean hasGuiltyVerdict(final MigratedOffence offence, final ReferenceDataVO referenceDataVO) {
        if (offence.getVerdict() == null) {
            return false;
        }
        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = referenceDataVO.getVerdictReferenceDataMap();
        if (verdictReferenceDataMap == null) {
            return false;
        }
        return verdictReferenceDataMap.values().stream()
                .filter(m -> m.containsKey(offence.getOffenceId()))
                .findFirst()
                .map(m -> m.get(offence.getOffenceId()))
                .filter(v -> GUILTY.equalsIgnoreCase(v.getCategory()))
                .isPresent();
    }

    private boolean hasGuiltyPlea(final Plea plea) {
        return plea != null && (INDICATED_GUILTY.equalsIgnoreCase(plea.getPleaValue()) || GUILTY.equalsIgnoreCase(plea.getPleaValue()));
    }

    private Verdict convertVerdict(final MigratedOffence offence, final ReferenceDataVO referenceDataVO, final Plea plea) {
        Verdict convertedVerdict = null;

        final boolean isVerdictToBeNulled = nonNull(plea)
                && nonNull(plea.getPleaValue())
                && GUILTY.equalsIgnoreCase(plea.getPleaValue());
        if (isVerdictToBeNulled) {
            return convertedVerdict;
        }

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = referenceDataVO.getVerdictReferenceDataMap();
        if (Objects.isNull(verdictReferenceDataMap)) {
            return convertedVerdict;
        }

        final Collection<Map<UUID, VerdictReferenceData>> vals = verdictReferenceDataMap.values();
        Optional<VerdictReferenceData> result = vals.stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getKey().equals(offence.getOffenceId()))
                .map(Map.Entry::getValue)
                .findFirst();

        if (offence.getVerdict() != null && result.isPresent()) {
            final MigratedVerdict verdict = offence.getVerdict();

            final Verdict.Builder builder = Verdict.verdict()
                    .withOffenceId(offence.getOffenceId())
                    .withVerdictDate(ofNullable(verdict.getVerdictDate()).map(LocalDate::toString).orElse(null));


            final VerdictReferenceData verdictType = result.get();
            builder.withVerdictType(VerdictType.verdictType()
                    .withId(verdictType.getId())
                    .withCategory(verdictType.getCategory())
                    .withCategoryType(verdictType.getCategoryType())
                    .withCjsVerdictCode(verdictType.getCjsVerdictCode())
                    .withDescription(verdictType.getDescription())
                    .withVerdictCode(verdictType.getVerdictCode())
                    .build());

            convertedVerdict = builder.build();
        }

        return convertedVerdict;
    }


    private Plea convertPlea(final MigratedOffence offence, final ReferenceDataVO referenceDataVO) {
        Plea convertedPlea = null;
        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = referenceDataVO.getPleaReferenceDataMap();
        if (Objects.isNull(pleaReferenceDataMap)) {
            return convertedPlea;
        }
        final Collection<Map<UUID, PleaReferenceData>> vals = pleaReferenceDataMap.values();
        Optional<PleaReferenceData> result = vals.stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getKey().equals(offence.getOffenceId()))
                .map(Map.Entry::getValue)
                .findFirst();

        if (offence.getPlea() != null && result.isPresent()) {
            final MigratedPlea plea = offence.getPlea();

            final PleaReferenceData pleaReferenceData = result.get();

            final Plea.Builder builder = Plea.plea()
                    .withOffenceId(offence.getOffenceId())
                    .withPleaValue(pleaReferenceData.getPleaValue());

            if ("No".equalsIgnoreCase(pleaReferenceData.getPleaTypeGuiltyFlag())) {
                final String now = LocalDate.now().toString();
                builder.withPleaDate(ofNullable(plea.getPleaDate()).map(LocalDate::toString).orElse(now));
            } else {
                builder.withPleaDate(ofNullable(plea.getPleaDate()).map(LocalDate::toString).orElse(null));
            }

            convertedPlea = builder.build();
        }

        return convertedPlea;
    }

    /**
     * Get Conviction Date based on plea date
     *
     * @param offence         - offence
     * @param referenceDataVO - referenceData
     * @return - returns Conviction date
     */
    private LocalDate getConvictionDate(final MigratedOffence offence, final ReferenceDataVO referenceDataVO) {

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = referenceDataVO.getPleaReferenceDataMap();

        if (Objects.isNull(pleaReferenceDataMap)) {
            return null;
        }

        Optional<PleaReferenceData> pleaReferenceDataOptional = pleaReferenceDataMap.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getKey().equals(offence.getOffenceId()))
                .map(Map.Entry::getValue)
                .findFirst();

        if (pleaReferenceDataOptional.isPresent()) {
            final MigratedPlea plea = offence.getPlea();

            final PleaReferenceData pleaReferenceData = pleaReferenceDataOptional.get();

            if ("Yes".equalsIgnoreCase(pleaReferenceData.getPleaTypeGuiltyFlag())) {
                return plea.getPleaDate();
            }
        }

        return null;
    }

    /**
     * firstly checks whether initiation type is Trial or Committal for sentence and channel is MCC
     * then return committingCourt regarding query from referencedata.query.organisationunits
     *
     * @param paramsVO which includes channel, initiationType and receivedFromCourtOUCode
     * @return committingCourt
     */
    private CommittingCourt getCommittingCourtFromReferenceData(final ParamsVO paramsVO) {
        if (isNotEmpty(paramsVO.getReceivedFromCourtOUCode()) && O.name().equals(paramsVO.getInitiationCode()) && MCC.equals(paramsVO.getChannel())) {
            final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnits(paramsVO.getReceivedFromCourtOUCode());

            if (CollectionUtils.isNotEmpty(organisationUnits)) {
                final OrganisationUnitReferenceData organisationUnit = organisationUnits.get(0);
                return committingCourt()
                        .withCourtCentreId(fromString(organisationUnit.getId()))
                        .withCourtHouseType(MAGISTRATES_COURT_HOUSE_TYPE.equalsIgnoreCase(organisationUnit.getOucodeL1Code()) ? MAGISTRATES : CROWN)
                        .withCourtHouseCode(organisationUnit.getOucodeL3Code())
                        .withCourtHouseName(organisationUnit.getOucodeL3Name())
                        .withCourtHouseShortName(organisationUnit.getOucodeL3Name())
                        .build();
            }
        }
        return null;
    }

    /**
     * Derives conviction date from verdict if the verdict category is "Guilty"
     *
     * @param verdict - the verdict to check for guilty category
     * @return String conviction date derived from verdict date if guilty and verdict date is non-null, null otherwise
     */
    private String deriveConvictionDateFromVerdict(final Verdict verdict) {
        if (verdict != null && verdict.getVerdictType() != null && "Guilty".equalsIgnoreCase(verdict.getVerdictType().getCategory())) {
            return verdict.getVerdictDate();
        }
        return null;
    }
}

