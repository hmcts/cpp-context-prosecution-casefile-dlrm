package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static org.mockito.Mockito.when;

import java.util.Optional;

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
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit())
                .thenReturn(Optional.of(Mockito.mock(OrganisationUnitReferenceData.class)));


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToTrueWhenSendingCourtIsBlank() {

        ReceivingCourtValidationRules receivingCourtValidationRules = new ReceivingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn(null);
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit())
                .thenReturn(Optional.of(Mockito.mock(OrganisationUnitReferenceData.class)));


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToFalseWhenCourtFound() {

        ReceivingCourtValidationRules receivingCourtValidationRules = new ReceivingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getReceivingCourt()).thenReturn("ABCDE00");
        when(mocked.getReferenceDataVO().getReceivingCourtOrganisationUnit())
                .thenReturn(Optional.empty());


        final ValidationResult result = receivingCourtValidationRules.validate(mocked, null);

        Assertions.assertFalse(result.isValid());

    }

}