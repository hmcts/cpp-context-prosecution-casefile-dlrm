package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ReceivingCourtValidationRules;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

class ReceivingCourtValidationRulesTest {


    @Test
    void shouldValidateToTrueWhenCourtFound() {

        ReceivingCourtValidationRules receivingCourtValidationRules = new ReceivingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn("ABCDE00");
        Mockito.mock(OrganisationUnitReferenceData.class);
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit().isPresent())
                .thenReturn(true);


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToTrueWhenSendingCourtIsBlank() {

        ReceivingCourtValidationRules receivingCourtValidationRules = new ReceivingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn(null);
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit().isPresent())
                .thenReturn(true);


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToFalseWhenCourtFound() {

        ReceivingCourtValidationRules receivingCourtValidationRules = new ReceivingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn("ABCDE00");
        Mockito.mock(OrganisationUnitReferenceData.class);
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit().isPresent())
                .thenReturn(false);


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertFalse(result.isValid());

    }

}