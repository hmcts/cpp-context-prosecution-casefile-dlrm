package uk.gov.moj.cpp.pcfdlrm.command.api.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;

class CommandApiKnowledgeBaseRuleCountTest {

    private static final String COMMAND_API_KBASE = "COMMAND_API";

    @Test
    // BC-20: a Drools test harness silently loading 0 rules lets deny-tests pass vacuously (false
    // confidence). This repo's kmodule.xml declares exactly one kbase (COMMAND_API) with one
    // stateless ksession (COMMAND_API_SESSION) — no multi-kbase 0-rule trap to guard against here
    // (contrast cpp-context-system-doc-generator's QUERY_API kbase, which is legitimately empty).
    // StatelessKieSession (what ReceiveMigratedCaseRuleTest extends via BaseDroolsAccessControlTest)
    // does not expose the KieBase, so this loads it independently via KieServices — confirmed against
    // the real access-control-test-utils/kie-api 7.69.0.Final jars before writing this assertion,
    // per 02-design.md §C's instruction not to copy the investigation report's snippet unverified.
    void shouldLoadNonZeroRuleCountForCommandApiKbase() {
        final var kieBase = KieServices.get().getKieClasspathContainer().getKieBase(COMMAND_API_KBASE);

        final long ruleCount = kieBase.getKiePackages().stream()
                .mapToLong(kiePackage -> kiePackage.getRules().size())
                .sum();

        assertTrue(ruleCount > 0, "Expected the " + COMMAND_API_KBASE + " kbase to load at least one rule");
    }
}
