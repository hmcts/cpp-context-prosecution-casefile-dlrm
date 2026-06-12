package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.pcfdlrm.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.StatementOfFactsValidationRule;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.CaseInitiationValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ProsecutorReferenceDataValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.OffenceGenericValidationAndEnricherRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.prosecutors.ProsecutorSJPValidationRule;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CcProsecutionValidationRuleProviderTest {

    private static final String INITIATION_CODE_CHARGE_CASE = "C";
    private static final String INITIATION_CODE_FOR_SUMMONS = "S";
    private static final String INITIATION_CODE_FOR_SJP = "J";

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
    void shouldValidateSJPCaseCreationValidationRule() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_FOR_SJP);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorAOCPValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(SummonsCodeValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorSJPValidationRule.class)));

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
