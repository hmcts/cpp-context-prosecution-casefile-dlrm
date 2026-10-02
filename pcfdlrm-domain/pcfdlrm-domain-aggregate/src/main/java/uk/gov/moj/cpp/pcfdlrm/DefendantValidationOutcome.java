package uk.gov.moj.cpp.pcfdlrm;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;

import java.util.List;

/**
 * DD-43501: the result of validating a case's defendants. {@code libraGuardianRejections} holds, in
 * defendant then rule order, only the REJECT problems raised for defendants in LIBRA parent/guardian scope,
 * so the aggregate never rejects on a problem code that is a warning in another scope.
 */
public record DefendantValidationOutcome(MigratedDefendantWithProblem migratedDefendantWithProblem, List<Problem> libraGuardianRejections) {

    public DefendantValidationOutcome {
        libraGuardianRejections = List.copyOf(libraGuardianRejections);
    }
}
