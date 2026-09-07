package uk.gov.moj.cpp.pcfdlrm.command.api.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;

class CommandApiKnowledgeBaseRuleCountTest {

    private static final String COMMAND_API_KBASE = "COMMAND_API";

    @Test
    // BC-20 rule-count guard — see docs/j25-parity-checklist.md, 01-requirements.md FR11,
    // 02-design.md §C.
    void shouldLoadNonZeroRuleCountForCommandApiKbase() {
        final var kieBase = KieServices.get().getKieClasspathContainer().getKieBase(COMMAND_API_KBASE);

        final long ruleCount = kieBase.getKiePackages().stream()
                .mapToLong(kiePackage -> kiePackage.getRules().size())
                .sum();

        assertTrue(ruleCount > 0, "Expected the " + COMMAND_API_KBASE + " kbase to load at least one rule");
    }
}
