package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ReceiptTypeValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    private static final List<String> RECEIPT_TYPES = List.of(
            "Either way case",
            "Transfer",
            "Voluntary bill",
            "Indictable");

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final String receiptType = prosecutionWithReferenceData.getProsecution().getCaseDetails().getReceiptType();

        if (isNull(receiptType)) {
            return getValidationResult(List.of(new ProblemValue("0", FieldName.RECEIPT_TYPES.getValue(), receiptType)));
        }

        final Predicate<String> stringPredicate = RECEIPT_TYPES::contains;

        final List<ProblemValue> problemValues = new ArrayList<>();

        if(stringPredicate.negate().test(receiptType)) {
            problemValues.add(new ProblemValue("0", FieldName.RECEIPT_TYPES.getValue(), receiptType));
        }

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return getValidationResult(problemValues);
    }

    private ValidationResult getValidationResult(final List<ProblemValue> problemValues) {
        return newValidationResult(of(Problems.newProblem(ProblemCode.RECEIPT_TYPE_IS_INVALID, problemValues.toArray(new ProblemValue[0]))));
    }
}
