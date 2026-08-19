package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.CHARGE;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.OTHER;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.REQUISITION;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SJP;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SUMMONS;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
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
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorSJPValidationRule;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CcProsecutionValidationRuleProviderTest {

    @Test
    void shouldValidateDefendantValidateSpiRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(CHARGE.getCode(), Channel.SPI, Boolean.FALSE);

        assertEquals(Set.of(
                AdditionalNationalityValidationAndEnricherRule.class,
                ArrestDateValidationRule.class,
                BailConditionsValidationAndEnricherRule.class,
                ChargeDateValidationRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                CourtReceivedFromCodeCourtValidationRules.class,
                CourtReceivedToCodeCourtValidationRules.class,
                CroNumberSpiValidationRule.class,
                CustodyStatusValidationAndEnricherRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantInitiationCodeValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                NationalityValidationAndEnricherRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceDrugLevelAmountValidationAndEnricherRule.class,
                OffenceDrugLevelMethodValidationAndEnricherRule.class,
                OffenceGenericValidationAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenderCodeValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                PleaValidationRule.class,
                PncIdSpiValidationRule.class,
                PostCodeValidationRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                VehicleCodeValidationAndEnricherRule.class,
                VerdictValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateCaseValidationRulesForSummons() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(SUMMONS.getCode(), "XHIBIT");

        assertEquals(Set.of(
                SummonsCodeValidationRule.class,
                CaseInitiationValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                SendingCourtValidationRules.class,
                ReceivingCourtValidationRules.class,
                CaseMarkersValidationAndEnricherRule.class,
                ReceiptTypeValidationRule.class,
                PoliceForceCodeValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateCaseValidationRulesForLibraSjp() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(SJP.getCode(), "LIBRA");

        assertEquals(Set.of(
                CaseInitiationValidationRule.class,
                SummonsCodeValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                ProsecutorSJPValidationRule.class,
                ProsecutorAOCPValidationRule.class
        ), classesOf(validationRules));
    }

    // AC-T2-6 — absent/unrecognised source systems resolve to the LIBRA/PCF-shaped set, not XHIBIT's.
    @Test
    void shouldValidateCaseValidationRulesForAbsentSourceSystem() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(SUMMONS.getCode(), null);

        assertEquals(Set.of(
                CaseInitiationValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                CaseMarkersValidationAndEnricherRule.class,
                PoliceForceCodeValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateCaseValidationRulesForRequisition() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(REQUISITION.getCode(), "XHIBIT");

        assertEquals(Set.of(
                CaseInitiationValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                SendingCourtValidationRules.class,
                ReceivingCourtValidationRules.class,
                CaseMarkersValidationAndEnricherRule.class,
                ReceiptTypeValidationRule.class,
                PoliceForceCodeValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateCaseValidationRulesForSjp() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(SJP.getCode(), "XHIBIT");

        assertEquals(Set.of(
                ProsecutorAOCPValidationRule.class,
                CaseInitiationValidationRule.class,
                SummonsCodeValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                SendingCourtValidationRules.class,
                ReceivingCourtValidationRules.class,
                ProsecutorSJPValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateCaseValidationRulesForDefaultInitiationCode() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(OTHER.getCode(), "XHIBIT");

        assertEquals(Set.of(
                CaseInitiationValidationRule.class,
                ProsecutorReferenceDataValidationRule.class,
                SendingCourtValidationRules.class,
                ReceivingCourtValidationRules.class,
                CaseMarkersValidationAndEnricherRule.class,
                ReceiptTypeValidationRule.class,
                PoliceForceCodeValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromCPPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(SUMMONS.getCode(), Channel.CPPI, Boolean.FALSE);

        assertEquals(Set.of(
                AdditionalNationalityValidationAndEnricherRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                CourtReceivedFromCodeCourtValidationRules.class,
                CourtReceivedToCodeCourtValidationRules.class,
                CroNumberValidationRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                NationalityValidationAndEnricherRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceDrugLevelAmountValidationAndEnricherRule.class,
                OffenceDrugLevelMethodValidationAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenderCodeValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                PleaValidationRule.class,
                PncIdValidationRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                StatementOfFactsValidationRule.class,
                StatementOfFactsWelshValidationRule.class,
                VehicleCodeValidationAndEnricherRule.class,
                VerdictValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromSPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(SUMMONS.getCode(), Channel.SPI, Boolean.FALSE);

        assertEquals(Set.of(
                AdditionalNationalityValidationAndEnricherRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                CourtReceivedFromCodeCourtValidationRules.class,
                CourtReceivedToCodeCourtValidationRules.class,
                CroNumberSpiValidationRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantInitiationCodeValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                NationalityValidationAndEnricherRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceDrugLevelAmountValidationAndEnricherRule.class,
                OffenceDrugLevelMethodValidationAndEnricherRule.class,
                OffenceGenericValidationAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenderCodeValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                PleaValidationRule.class,
                PncIdSpiValidationRule.class,
                PostCodeValidationRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                StatementOfFactsValidationRule.class,
                StatementOfFactsWelshValidationRule.class,
                VehicleCodeValidationAndEnricherRule.class,
                VerdictValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateDefendantValidateCPPIRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(CHARGE.getCode(), Channel.CPPI, Boolean.FALSE);

        assertEquals(Set.of(
                AdditionalNationalityValidationAndEnricherRule.class,
                ArrestDateValidationRule.class,
                BailConditionsValidationAndEnricherRule.class,
                ChargeDateValidationRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                CourtReceivedFromCodeCourtValidationRules.class,
                CourtReceivedToCodeCourtValidationRules.class,
                CroNumberValidationRule.class,
                CustodyStatusValidationAndEnricherRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                NationalityValidationAndEnricherRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceDrugLevelAmountValidationAndEnricherRule.class,
                OffenceDrugLevelMethodValidationAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenderCodeValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                PleaValidationRule.class,
                PncIdValidationRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                VehicleCodeValidationAndEnricherRule.class,
                VerdictValidationRule.class
        ), classesOf(validationRules));
    }

    @Test
    void shouldValidateDefendantValidateMCCRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(CHARGE.getCode(), Channel.MCC, Boolean.FALSE);

        assertEquals(Set.of(
                AdditionalNationalityValidationAndEnricherRule.class,
                ArrestDateValidationRule.class,
                BailConditionsValidationAndEnricherRule.class,
                ChargeDateValidationRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                CourtReceivedFromCodeCourtValidationRules.class,
                CourtReceivedToCodeCourtValidationRules.class,
                CroNumberValidationRule.class,
                CustodyStatusValidationAndEnricherRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                NationalityValidationAndEnricherRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceDrugLevelAmountValidationAndEnricherRule.class,
                OffenceDrugLevelMethodValidationAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenderCodeValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                PleaValidationRule.class,
                PncIdValidationRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                VehicleCodeValidationAndEnricherRule.class,
                VerdictValidationRule.class
        ), classesOf(validationRules));
    }

    // AC-T2-3 — J takes SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE at the defendant level, asserted
    // explicitly so a later change can't quietly move J back onto the common path (FR13).
    @Test
    void shouldValidateDefendantValidateDlrmRulesForSjp() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(SJP.getCode(), Channel.DLRM_MIGRATION, Boolean.FALSE);

        assertEquals(Set.of(
                NationalityValidationAndEnricherRule.class,
                AdditionalNationalityValidationAndEnricherRule.class,
                ParentGuardianDateOfBirthValidationRule.class,
                ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
                ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
                ParentGuardianPrimaryEmailAddressValidationRule.class,
                ParentGuardianSecondaryEmailAddressValidationRule.class,
                ChargeDateValidationRule.class,
                DefendantDateOfBirthValidationRule.class,
                DefendantPerceivedBirthYearValidationRule.class,
                ObservedEthnicityValidationAndEnricherRule.class,
                SelfDefinedEthnicityValidationAndEnricherRule.class,
                IndividualDefendantPrimaryEmailAddressValidationRule.class,
                IndividualDefendantSecondaryEmailAddressValidationRule.class,
                CorporateDefendantPrimaryEmailAddressValidationRule.class,
                CorporateDefendantSecondaryEmailAddressValidationRule.class,
                OffenceAlcoholLevelValidationAndEnricherRule.class,
                OffenceCodeValidationAndEnricherRule.class,
                OffenceBackDutyValidationRuleAndEnricherRule.class,
                OffenceLocationValidationAndEnricherRule.class,
                OffenceGenericValidationAndEnricherRule.class,
                PostCodeValidationRule.class,
                DefendantInitiationCodeValidationRule.class
        ), classesOf(validationRules));
    }

    // Mirrors production's COMMON_DEFENDANT_RULE_SET / SPI_DEFENDANT_RULE_SET — shared by every
    // defendantValidationMapSpi-routed test below, since both DLRM tests only differ in the
    // per-initiation-code set layered on top (CHARGE_DEFENDANT_RULE_SET vs DEFAULT_DEFENDANT_RULE_SET).
    private static final Set<Class<?>> COMMON_DEFENDANT_RULE_CLASSES = Set.of(
            IndividualDefendantPrimaryEmailAddressValidationRule.class,
            IndividualDefendantSecondaryEmailAddressValidationRule.class,
            CorporateDefendantPrimaryEmailAddressValidationRule.class,
            CorporateDefendantSecondaryEmailAddressValidationRule.class,
            ParentGuardianDateOfBirthValidationRule.class,
            ParentGuardianObservedEthnicityValidationAndEnricherRule.class,
            ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule.class,
            ParentGuardianPrimaryEmailAddressValidationRule.class,
            ParentGuardianSecondaryEmailAddressValidationRule.class,
            OffenderCodeValidationAndEnricherRule.class,
            SelfDefinedEthnicityValidationAndEnricherRule.class,
            OffenceLocationValidationAndEnricherRule.class,
            ObservedEthnicityValidationAndEnricherRule.class,
            OffenceAlcoholLevelValidationAndEnricherRule.class,
            DefendantDateOfBirthValidationRule.class,
            NationalityValidationAndEnricherRule.class,
            VehicleCodeValidationAndEnricherRule.class,
            DefendantPerceivedBirthYearValidationRule.class,
            OffenceCodeValidationAndEnricherRule.class,
            OffenceDrugLevelMethodValidationAndEnricherRule.class,
            OffenceDrugLevelAmountValidationAndEnricherRule.class,
            OffenceBackDutyValidationRuleAndEnricherRule.class,
            CourtReceivedFromCodeCourtValidationRules.class,
            CourtReceivedToCodeCourtValidationRules.class,
            PleaValidationRule.class,
            VerdictValidationRule.class
    );

    private static final Set<Class<?>> SPI_DEFENDANT_RULE_CLASSES = Set.of(
            PncIdSpiValidationRule.class,
            CroNumberSpiValidationRule.class,
            OffenceGenericValidationAndEnricherRule.class,
            PostCodeValidationRule.class,
            DefendantInitiationCodeValidationRule.class
    );

    @Test
    void shouldValidateDefendantValidateDlrmRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(CHARGE.getCode(), Channel.DLRM_MIGRATION, Boolean.FALSE);

        // CHARGE_DEFENDANT_RULE_SET layered on COMMON + SPI.
        assertEquals(union(COMMON_DEFENDANT_RULE_CLASSES, SPI_DEFENDANT_RULE_CLASSES, Set.of(
                BailConditionsValidationAndEnricherRule.class,
                ArrestDateValidationRule.class,
                ChargeDateValidationRule.class,
                AdditionalNationalityValidationAndEnricherRule.class,
                CustodyStatusValidationAndEnricherRule.class
        )), classesOf(validationRules));
    }

    // FR12b Option A — R has no map entry, so it takes the composed fallback triple; pinned by name.
    @Test
    void shouldValidateDefendantValidateDlrmRulesForRemittanceFallback() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules("R", Channel.DLRM_MIGRATION, Boolean.FALSE);

        // DEFAULT_DEFENDANT_RULE_SET layered on COMMON + SPI — the fallback triple.
        assertEquals(union(COMMON_DEFENDANT_RULE_CLASSES, SPI_DEFENDANT_RULE_CLASSES, Set.of(
                ArrestDateValidationRule.class,
                ChargeDateValidationRule.class,
                CustodyStatusValidationAndEnricherRule.class
        )), classesOf(validationRules));
    }

    @SafeVarargs
    private static Set<Class<?>> union(final Set<Class<?>>... sets) {
        return Stream.of(sets).flatMap(Set::stream).collect(Collectors.toSet());
    }

    private static <T> Set<Class<?>> classesOf(final List<ValidationRule<T, ReferenceDataQueryService>> validationRules) {
        return validationRules.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
