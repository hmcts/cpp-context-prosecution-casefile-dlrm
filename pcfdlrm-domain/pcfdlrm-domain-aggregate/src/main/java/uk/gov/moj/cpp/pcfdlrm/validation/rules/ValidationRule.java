package uk.gov.moj.cpp.pcfdlrm.validation.rules;

@FunctionalInterface
public interface ValidationRule<T, S> {

    ValidationResult validate(final T input, final S context);

}
