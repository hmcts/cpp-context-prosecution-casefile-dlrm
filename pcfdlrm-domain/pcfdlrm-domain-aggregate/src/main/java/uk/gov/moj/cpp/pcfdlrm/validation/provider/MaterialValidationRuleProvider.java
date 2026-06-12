package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import uk.gov.moj.cpp.pcfdlrm.CaseType;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;

import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.Collections;
import java.util.List;

public class MaterialValidationRuleProvider {

    private MaterialValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules(final CaseType caseType) {
        if (caseType == CaseType.SJP) {
            return SjpMaterialsValidationRuleProvider.getRejectionRules();
        } else if (caseType == CaseType.CC) {
            return CCMaterialsValidationRuleProvider.getRejectionRules();
        } else {
            return Collections.emptyList();
        }
    }
}
