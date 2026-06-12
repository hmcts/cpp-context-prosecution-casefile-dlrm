package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.CourtReferralCreatedValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.SjpCaseInSessionValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.SjpDocumentTypeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.SjpFileTypeValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.List;

public class SjpMaterialsValidationRuleProvider {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> REJECTION_RULES = unmodifiableList(asList(
            new SjpFileTypeValidationRule(),
            new SjpDocumentTypeValidationRule(),
            new CourtReferralCreatedValidationRule(),
            new SjpCaseInSessionValidationRule()));

    private SjpMaterialsValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules() {
        return REJECTION_RULES;
    }

}
