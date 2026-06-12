package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial.CCDocumentDefendantLevelValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial.CCDocumentTypeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ccmaterial.CaseEjectValidationRule;

import java.util.List;

public class CCMaterialsValidationRuleProvider {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> REJECTION_RULES = unmodifiableList(asList(
            new CCDocumentTypeValidationRule(),
            new CCDocumentDefendantLevelValidationRule(),
            new CaseEjectValidationRule()
    ));

    private CCMaterialsValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules() {
        return REJECTION_RULES;
    }

}
