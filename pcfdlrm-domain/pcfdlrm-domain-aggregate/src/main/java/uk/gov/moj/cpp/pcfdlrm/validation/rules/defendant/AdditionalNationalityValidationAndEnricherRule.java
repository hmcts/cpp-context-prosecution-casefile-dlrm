package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;

public class AdditionalNationalityValidationAndEnricherRule extends AbstractNationalityValidationRule {

    @Override
    protected String getNationality(SelfDefinedInformation selfDefinedInformation) {
        return selfDefinedInformation.getAdditionalNationality();
    }

    @Override
    protected ProblemCode getProblemCode() {
        return ProblemCode.DEFENDANT_ADDITIONAL_NATIONALITY_INVALID;
    }

    @Override
    protected FieldName getProblemCodeFieldName() {
        return FieldName.DEFENDANT_ADDITIONAL_NATIONALITY;
    }
}
