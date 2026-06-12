package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

class SendingCourtValidationRulesTest {


    @Test
    void shouldValidateToTrueWhenCourtFound() {

        SendingCourtValidationRules sendingCourtValidationRules = new SendingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getSendingCourt()).thenReturn("ABCDE00");
        Mockito.mock(OrganisationUnitReferenceData.class);
        when(mocked.getReferenceDataVO().getSendingCourtOrganisationUnit().isPresent())
                .thenReturn(true);


        final ValidationResult result = sendingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToTrueWhenSendingCourtIsBlank() {

        SendingCourtValidationRules sendingCourtValidationRules = new SendingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getSendingCourt()).thenReturn(null);
        when(mocked.getReferenceDataVO().getSendingCourtOrganisationUnit().isPresent())
                .thenReturn(true);


        final ValidationResult result = sendingCourtValidationRules.validate(mocked, null);

        Assertions.assertTrue(result.isValid());

    }

    @Test
    void shouldValidateToFalseWhenCourtFound() {

        SendingCourtValidationRules sendingCourtValidationRules = new SendingCourtValidationRules();
        ProsecutionWithReferenceData mocked = Mockito.mock(ProsecutionWithReferenceData.class, Answers.RETURNS_DEEP_STUBS);

        when(mocked.getProsecution().getCaseDetails().getSendingCourt()).thenReturn("ABCDE00");
        Mockito.mock(OrganisationUnitReferenceData.class);
        when(mocked.getReferenceDataVO().getSendingCourtOrganisationUnit().isPresent())
                .thenReturn(false);


        final ValidationResult result = sendingCourtValidationRules.validate(mocked, null);

        Assertions.assertFalse(result.isValid());

    }

}