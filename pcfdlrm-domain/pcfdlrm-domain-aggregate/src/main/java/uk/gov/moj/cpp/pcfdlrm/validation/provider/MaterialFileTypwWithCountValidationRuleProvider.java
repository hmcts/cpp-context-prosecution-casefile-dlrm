package uk.gov.moj.cpp.pcfdlrm.validation.provider;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.pcfdlrm.CaseType;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedMaterialsWithOriginatingSystem;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.migratescasedetails.ExhibitFiileTypeValidationRule;

import java.util.Collections;
import java.util.List;

public class MaterialFileTypwWithCountValidationRuleProvider {

    private MaterialFileTypwWithCountValidationRuleProvider() {
    }

    public static List<ValidationRule<MigratedMaterialsWithOriginatingSystem, ReferenceDataQueryService>> getRejectionRules(final CaseType caseType) {
        if (caseType == CaseType.CC) {

            return unmodifiableList(asList(
                    new ExhibitFiileTypeValidationRule()
            ));
        } else {
            return Collections.emptyList();
        }
    }
}
