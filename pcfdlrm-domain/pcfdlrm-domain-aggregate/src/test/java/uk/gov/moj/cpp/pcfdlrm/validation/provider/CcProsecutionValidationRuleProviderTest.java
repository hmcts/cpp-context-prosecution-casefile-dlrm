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
                .getCaseValidationRules(SUMMONS.getCode());

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
    void shouldValidateCaseValidationRulesForRequisition() {
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(REQUISITION.getCode());

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
                .getCaseValidationRules(SJP.getCode());

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
                .getCaseValidationRules(OTHER.getCode());

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

    @Test
    void shouldValidateDefendantValidateDlrmRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(CHARGE.getCode(), Channel.DLRM_MIGRATION, Boolean.FALSE);

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

    private static <T> Set<Class<?>> classesOf(final List<ValidationRule<T, ReferenceDataQueryService>> validationRules) {
        return validationRules.stream().map(Object::getClass).collect(Collectors.toSet());
    }
}
