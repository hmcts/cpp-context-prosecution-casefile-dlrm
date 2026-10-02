package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_GUARDIAN_ORGANISATION_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS1;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS2;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS3;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS4;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS5;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_POST_CODE;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;

import java.util.List;

/**
 * DD-43501: address1 (REJECT, even though the LIBRA feed sends no organisation address today), address2–5
 * (NULL) and postcode (NULL) for an organisation guardian's {@code address}.
 */
public class LibraOrganisationParentGuardianAddressValidationRule extends AbstractLibraParentGuardianAddressValidationRule {

    public LibraOrganisationParentGuardianAddressValidationRule() {
        super(PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID, PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID, INVALID_GUARDIAN_ORGANISATION_POST_CODE,
                List.of(PARENT_GUARDIAN_ORGANISATION_ADDRESS1, PARENT_GUARDIAN_ORGANISATION_ADDRESS2, PARENT_GUARDIAN_ORGANISATION_ADDRESS3,
                        PARENT_GUARDIAN_ORGANISATION_ADDRESS4, PARENT_GUARDIAN_ORGANISATION_ADDRESS5),
                PARENT_GUARDIAN_ORGANISATION_POST_CODE);
    }

    @Override
    protected Address addressOf(final ParentGuardianInformation parentGuardianInformation) {
        return parentGuardianInformation.getAddress();
    }
}
