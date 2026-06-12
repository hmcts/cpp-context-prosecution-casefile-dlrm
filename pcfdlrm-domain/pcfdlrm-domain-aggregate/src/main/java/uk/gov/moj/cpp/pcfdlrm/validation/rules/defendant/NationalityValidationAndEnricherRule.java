package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;


import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_NATIONALITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_NATIONALITY;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;

public class NationalityValidationAndEnricherRule extends AbstractNationalityValidationRule {

    @Override
    protected String getNationality(SelfDefinedInformation selfDefinedInformation) {
        return selfDefinedInformation.getNationality();
    }

    @Override
    protected ProblemCode getProblemCode() {
        return DEFENDANT_NATIONALITY_INVALID;
    }

    @Override
    protected FieldName getProblemCodeFieldName() {
        return DEFENDANT_NATIONALITY;
    }

}
