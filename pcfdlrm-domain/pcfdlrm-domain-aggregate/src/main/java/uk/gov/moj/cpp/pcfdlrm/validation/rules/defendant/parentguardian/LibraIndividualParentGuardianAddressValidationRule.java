package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_GUARDIAN_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ADDRESS_LINE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS1;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS2;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS3;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS4;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS5;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_POST_CODE;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;

import java.util.List;

public class LibraIndividualParentGuardianAddressValidationRule extends AbstractLibraParentGuardianAddressValidationRule {

    public LibraIndividualParentGuardianAddressValidationRule() {
        super(PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID, PARENT_GUARDIAN_ADDRESS_LINE_INVALID, INVALID_GUARDIAN_POST_CODE,
                List.of(PARENT_GUARDIAN_ADDRESS1, PARENT_GUARDIAN_ADDRESS2, PARENT_GUARDIAN_ADDRESS3, PARENT_GUARDIAN_ADDRESS4, PARENT_GUARDIAN_ADDRESS5),
                PARENT_GUARDIAN_POST_CODE);
    }

    @Override
    protected Address addressOf(final ParentGuardianInformation parentGuardianInformation) {
        final PersonalInformation personalInformation = parentGuardianInformation.getPersonalInformation();
        return personalInformation == null ? null : personalInformation.getAddress();
    }
}
