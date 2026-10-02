package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.parentGuardianOf;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.RedactingValidationRule.redactedProblem;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * DD-43501: the checks both LIBRA guardian address rules share, in this order: address1 (missing, blank or
 * over 35 characters, ignoring surrounding spaces), then address2–5 (present and blank or over 35), then postcode (non-blank and
 * over 8 characters, or matching neither {@link Constants#POST_CODE_REGEX} nor
 * {@link Constants#NO_FIXED_ABODE_POST_CODE}). Subclasses say where the address is and which codes and keys
 * to raise; whether a code rejects or nulls is {@link LibraParentGuardianOutcomes}' decision.
 */
abstract class AbstractLibraParentGuardianAddressValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final int MAX_ADDRESS_LINE_LENGTH = 35;
    private static final int MAX_POST_CODE_LENGTH = 8;
    private static final Pattern POST_CODE_FORMAT = Pattern.compile(Constants.POST_CODE_REGEX.getValue());
    private static final Pattern NO_FIXED_ABODE_POST_CODE_FORMAT = Pattern.compile(Constants.NO_FIXED_ABODE_POST_CODE.getValue());

    private final ProblemCode address1Code;
    private final ProblemCode addressLineCode;
    private final ProblemCode postCodeCode;
    private final List<FieldName> addressLineFields;
    private final FieldName postCodeField;

    /**
     * @param addressLineFields the field keys of address1 to address5, in that order
     */
    protected AbstractLibraParentGuardianAddressValidationRule(final ProblemCode address1Code, final ProblemCode addressLineCode, final ProblemCode postCodeCode,
                                                               final List<FieldName> addressLineFields, final FieldName postCodeField) {
        this.address1Code = address1Code;
        this.addressLineCode = addressLineCode;
        this.postCodeCode = postCodeCode;
        this.addressLineFields = List.copyOf(addressLineFields);
        this.postCodeField = postCodeField;
    }

    /** @return the guardian address this rule checks, or null when there is none */
    protected abstract Address addressOf(ParentGuardianInformation parentGuardianInformation);

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ParentGuardianInformation parentGuardianInformation = parentGuardianOf(defendantWithReferenceData.getDefendant());
        if (parentGuardianInformation == null) {
            return VALID;
        }

        final Address address = addressOf(parentGuardianInformation);
        final List<Problem> problems = new ArrayList<>();

        if (address == null || isBlank(address.getAddress1()) || address.getAddress1().trim().length() > MAX_ADDRESS_LINE_LENGTH) {
            problems.add(redactedProblem(address1Code, addressLineFields.get(0)));
        }
        if (address != null) {
            checkAddressLine(address.getAddress2(), addressLineFields.get(1), problems);
            checkAddressLine(address.getAddress3(), addressLineFields.get(2), problems);
            checkAddressLine(address.getAddress4(), addressLineFields.get(3), problems);
            checkAddressLine(address.getAddress5(), addressLineFields.get(4), problems);
            if (isInvalidPostCode(address.getPostcode())) {
                problems.add(redactedProblem(postCodeCode, postCodeField));
            }
        }
        return newValidationResult(problems);
    }

    private void checkAddressLine(final String addressLine, final FieldName fieldName, final List<Problem> problems) {
        if (addressLine != null && (isBlank(addressLine) || addressLine.trim().length() > MAX_ADDRESS_LINE_LENGTH)) {
            problems.add(redactedProblem(addressLineCode, fieldName));
        }
    }

    private static boolean isInvalidPostCode(final String postCode) {
        if (isBlank(postCode)) {
            return false;
        }
        final String trimmedPostCode = postCode.trim();
        return trimmedPostCode.length() > MAX_POST_CODE_LENGTH
                || !(POST_CODE_FORMAT.matcher(trimmedPostCode).matches() || NO_FIXED_ABODE_POST_CODE_FORMAT.matcher(trimmedPostCode).matches());
    }
}
