package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;

/**
 * DD-43501: replaces every {@link ProblemValue#getValue()} with its key, so warnings and
 * {@code DefendantValidationFailed} name the guardian field but never carry its data.
 */
public final class RedactingValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate;

    private RedactingValidationRule(final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate) {
        this.delegate = delegate;
    }

    public static RedactingValidationRule of(final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate) {
        return new RedactingValidationRule(delegate);
    }

    public ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate() {
        return delegate;
    }

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ValidationResult validationResult = delegate.validate(defendantWithReferenceData, referenceDataQueryService);
        return validationResult.isValid()
                ? validationResult
                : newValidationResult(validationResult.problems().stream().map(RedactingValidationRule::redact).toList());
    }

    /** The one factory the LIBRA guardian rules build their problems with: the value is the field key. */
    static Problem redactedProblem(final ProblemCode code, final FieldName fieldName) {
        return Problems.newProblem(code, new ProblemValue(null, fieldName.getValue(), fieldName.getValue()));
    }

    private static Problem redact(final Problem problem) {
        return new Problem(problem.getCode(), problem.getValues().stream()
                .map(value -> new ProblemValue(value.getId(), value.getKey(), value.getKey()))
                .toList());
    }
}
