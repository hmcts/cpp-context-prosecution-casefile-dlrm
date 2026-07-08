package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptTypeValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Test
    public void shouldReturnProblemsWhenReceiptTypesIsNull() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn(null);

        final Optional<Problem> optionalProblem = new ReceiptTypeValidationRule()
                .validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems()
                .stream()
                .findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is("RECEIPT_TYPE_IS_INVALID"));
        assertThat(optionalProblem.get().getValues().get(0).getId(), is("0"));
    }

    @Test
    public void shouldReturnProblemWhenReceiptTypesIsEmptyString() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType()).thenReturn("");

        final Optional<Problem> optionalProblem = new ReceiptTypeValidationRule()
                .validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems()
                .stream()
                .findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is("RECEIPT_TYPE_IS_INVALID"));
        assertThat(optionalProblem.get().getValues().get(0).getId(), is("0"));
    }

    @ParameterizedTest
    @CsvSource({"Either way case", "Transfer", "Voluntary bill", "Indictable"})
    public void shouldReturnEmptyProblemListWhenReceiptTypesContainsValidTypeCode(String receiptType) {

        ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(receiptType);

        final Optional<Problem> optionalProblem = new ReceiptTypeValidationRule()
                .validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemListWhenReceiptTypesHasInvalidCode() {
        ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData("Test");

        final Optional<Problem> optionalProblem = new ReceiptTypeValidationRule()
                .validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is("RECEIPT_TYPE_IS_INVALID"));
        assertThat(optionalProblem.get().getValues().get(0).getId(), is("0"));
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(String receiptType) {
        return new ProsecutionWithReferenceData(Prosecution.prosecution()
                .withCaseDetails(CaseDetails.caseDetails()
                        .withReceiptType(receiptType)
                        .build())
                .build());
    }

}