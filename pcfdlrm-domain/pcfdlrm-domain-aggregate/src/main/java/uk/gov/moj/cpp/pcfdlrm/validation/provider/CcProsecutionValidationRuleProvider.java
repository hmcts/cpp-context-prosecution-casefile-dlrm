package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.CHARGE;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.OTHER;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.REQUISITION;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SJP;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SUMMONS;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.SPI;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.CaseInitiationValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.CaseMarkersValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.PoliceForceCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ProsecutorReferenceDataValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ReceiptTypeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ReceivingCourtValidationRules;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.SendingCourtValidationRules;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.AdditionalNationalityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.BailConditionsValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CorporateDefendantPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CorporateDefendantSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CourtReceivedFromCodeCourtValidationRules;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CourtReceivedToCodeCourtValidationRules;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CustodyStatusValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.DefendantDateOfBirthValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.DefendantInitiationCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.DefendantPerceivedBirthYearValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.IndividualDefendantPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.IndividualDefendantSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.NationalityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ObservedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.OffenderCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ParentGuardianDateOfBirthValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ParentGuardianObservedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ParentGuardianPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ParentGuardianSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PostCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.SelfDefinedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.ArrestDateValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.ChargeDateValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceAlcoholLevelValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceBackDutyValidationRuleAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceDrugLevelAmountValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceDrugLevelMethodValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceGenericValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceLocationValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.StatementOfFactsValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.StatementOfFactsWelshValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.VehicleCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea.PleaValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea.VerdictValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.CourtHearingLocationValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.DateOfHearingPastDateValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.DateOfHearingValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.HearingTypeCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.NoMatchingDefendantsValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing.WeekCommencingStartDateValidationAndEnricher;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorSJPValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CcProsecutionValidationRuleProvider {

    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> SUMMONS_CASE_RULE_SET = unmodifiableList(asList(
            new SummonsCodeValidationRule()
    ));

    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> SJP_CASE_RULE_SET = unmodifiableList(asList(
            new CaseInitiationValidationRule(),
            new SummonsCodeValidationRule(),
            new ProsecutorReferenceDataValidationRule(),
            new SendingCourtValidationRules(),
            new ReceivingCourtValidationRules(),
            new ProsecutorSJPValidationRule(),
            new ProsecutorAOCPValidationRule()
    ));
    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> COMMON_CASE_RULE_SET = List.of(
            new CaseInitiationValidationRule(),
            new ProsecutorReferenceDataValidationRule(),
            new SendingCourtValidationRules(),
            new ReceivingCourtValidationRules(),
            new CaseMarkersValidationAndEnricherRule(),
            new ReceiptTypeValidationRule(),
            new PoliceForceCodeValidationRule());

    private static final List<ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService>> MIGRATED_HEARING_RULE_SET = unmodifiableList(asList(
            new CourtHearingLocationValidationRule(),
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new DateOfHearingValidationAndEnricherRule(),
            new HearingTypeCodeValidationRule(),
            new WeekCommencingStartDateValidationAndEnricher(),
            new NoMatchingDefendantsValidationRule()
    ));

    private static final Map<String, List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>> caseValidationMap = of(
            SUMMONS.getCode(), Stream.of(SUMMONS_CASE_RULE_SET, COMMON_CASE_RULE_SET).flatMap(Collection::stream).collect(toList()),
            CHARGE.getCode(), COMMON_CASE_RULE_SET,
            REQUISITION.getCode(), COMMON_CASE_RULE_SET,
            SJP.getCode(), SJP_CASE_RULE_SET);

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SJP_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new NationalityValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new ChargeDateValidationRule(),
            new DefendantDateOfBirthValidationRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE = unmodifiableList(asList(
            new NationalityValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new ChargeDateValidationRule(),
            new DefendantDateOfBirthValidationRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule(),
            new DefendantInitiationCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> CHARGE_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new BailConditionsValidationAndEnricherRule(),
            new ArrestDateValidationRule(),
            new ChargeDateValidationRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new CustodyStatusValidationAndEnricherRule()

    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> DEFAULT_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new ArrestDateValidationRule(),
            new ChargeDateValidationRule(),
            new CustodyStatusValidationAndEnricherRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> OTHER_DEFENDANT_RULE_SET = unmodifiableList(Collections.singletonList(
            new CustodyStatusValidationAndEnricherRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SUMMONS_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new AdditionalNationalityValidationAndEnricherRule(),
            new StatementOfFactsValidationRule(),
            new StatementOfFactsWelshValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SUMMONS_DEFENDANT_RULE_MCC_SET = unmodifiableList(asList(
            new AdditionalNationalityValidationAndEnricherRule(),
            new StatementOfFactsWelshValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> REQUISITION_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new ChargeDateValidationRule(),
            new AdditionalNationalityValidationAndEnricherRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> COMMON_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new OffenderCodeValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new DefendantDateOfBirthValidationRule(),
            new NationalityValidationAndEnricherRule(),
            new VehicleCodeValidationAndEnricherRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceDrugLevelMethodValidationAndEnricherRule(),
            new OffenceDrugLevelAmountValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new CourtReceivedFromCodeCourtValidationRules(),
            new CourtReceivedToCodeCourtValidationRules(),
            new PleaValidationRule(),
            new CourtReceivedToCodeCourtValidationRules(),
            new VerdictValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SPI_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new PncIdSpiValidationRule(),
            new CroNumberSpiValidationRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule(),
            new DefendantInitiationCodeValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> NON_POLICE_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new PncIdValidationRule(),
            new CroNumberValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> GROUP_CIVIL_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new ChargeDateValidationRule(),
            new DefendantDateOfBirthValidationRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new OffenderCodeValidationAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new NationalityValidationAndEnricherRule(),
            new VehicleCodeValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new CourtReceivedFromCodeCourtValidationRules(),
            new CourtReceivedToCodeCourtValidationRules()
    ));

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMap = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            SJP.getCode(), SJP_DEFENDANT_RULE_SET);

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapMCC = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_MCC_SET).flatMap(Collection::stream).collect(toList()),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            SJP.getCode(), SJP_DEFENDANT_RULE_SET);

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapSpi = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            SJP.getCode(), SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE);

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapForGroupCivilCases = of(
            SUMMONS.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()),
            CHARGE.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList()));

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapDlrm = defendantValidationMapSpi;


    private CcProsecutionValidationRuleProvider() {
    }

    public static List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> getCaseValidationRules(final String caseInitiationCode) {
        return caseValidationMap.getOrDefault(caseInitiationCode, COMMON_CASE_RULE_SET);
    }

    public static List<ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService>> getMigratedHearingValidationRules() {
        return MIGRATED_HEARING_RULE_SET;
    }

    public static List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> getDefendantValidationRules(final String defendantInitiationCode,
                                                                                                                          final Channel channel, final Boolean isGroupCase) {
        if (nonNull(channel) && SPI.equals(channel)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapSpi);
        } else if (nonNull(channel) && MCC.equals(channel)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapMCC);
        } else if (nonNull(channel) && CIVIL.equals(channel) && isGroupCase) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapForGroupCivilCases);
        }  else if (nonNull(channel) && DLRM_MIGRATION.equals(channel)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapDlrm);
        }
        return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, defendantValidationMap);
    }

    private static List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> getValidationRules(
            final String defendantInitiationCode,
            final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> commonDefendantRules,
            final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> spiDefendantRules,
            final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMap) {

        if (defendantValidationMap.containsKey(defendantInitiationCode)) {
            return defendantValidationMap.get(defendantInitiationCode);
        } else {
            return Stream.of(commonDefendantRules, spiDefendantRules, DEFAULT_DEFENDANT_RULE_SET).flatMap(Collection::stream).collect(toList());
        }
    }

    private static List<ValidationRule<MigratedDefendant, ReferenceDataQueryService>> getDlrmDefendantValidationRules(
            final String defendantInitiationCode,
            final Map<String, List<ValidationRule<MigratedDefendant, ReferenceDataQueryService>>> defendantValidationMap) {

        // What will fo here
        return defendantValidationMap.getOrDefault(defendantInitiationCode, null);
    }

}