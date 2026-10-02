package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.parentGuardianOf;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

/**
 * DD-43501: runs the delegate only when the defendant's guardian has the given {@link ParentGuardianShape},
 * so an organisation guardian never reaches an individual rule and vice versa.
 */
public final class ParentGuardianShapeGate implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private final ParentGuardianShape shape;
    private final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate;

    private ParentGuardianShapeGate(final ParentGuardianShape shape, final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate) {
        this.shape = shape;
        this.delegate = delegate;
    }

    public static ParentGuardianShapeGate onlyFor(final ParentGuardianShape shape, final ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate) {
        return new ParentGuardianShapeGate(shape, delegate);
    }

    public ParentGuardianShape shape() {
        return shape;
    }

    public ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> delegate() {
        return delegate;
    }

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        return ParentGuardianShape.of(parentGuardianOf(defendantWithReferenceData.getDefendant())) == shape
                ? delegate.validate(defendantWithReferenceData, referenceDataQueryService)
                : VALID;
    }
}
