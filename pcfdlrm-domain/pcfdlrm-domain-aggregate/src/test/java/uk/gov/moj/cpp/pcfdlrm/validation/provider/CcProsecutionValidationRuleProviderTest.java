package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.OTHER;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.REQUISITION;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SJP;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SUMMONS;

import uk.gov.moj.cpp.pcfdlrm.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.StatementOfFactsValidationRule;
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
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceGenericValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorSJPValidationRule;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CcProsecutionValidationRuleProviderTest {

    private static final String INITIATION_CODE_CHARGE_CASE = "C";
    private static final String INITIATION_CODE_FOR_SUMMONS = "S";

    @Test
    void shouldValidateDefendantValidateSpiRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.SPI, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(OffenceGenericValidationAndEnricherRule.class)));
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

    private static Set<Class<?>> classesOf(final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules) {
        return validationRules.stream().map(Object::getClass).collect(Collectors.toSet());
    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromCPPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.CPPI, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromSPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.SPI, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    void shouldValidateDefendantValidateCPPIRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.CPPI, Boolean.FALSE);

        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(OffenceGenericValidationAndEnricherRule.class)));
    }

    @Test
    void shouldValidateDefendantValidateMCCRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.MCC, Boolean.FALSE);

        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(OffenceGenericValidationAndEnricherRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
    }

    @Test
    void shouldValidateDefendantValidateDlrmRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.DLRM_MIGRATION, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(OffenceGenericValidationAndEnricherRule.class)));
    }

}
