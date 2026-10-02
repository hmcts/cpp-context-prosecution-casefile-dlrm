package uk.gov.moj.cpp.pcfdlrm;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;

import java.util.List;

public record DefendantValidationOutcome(MigratedDefendantWithProblem migratedDefendantWithProblem, List<Problem> libraGuardianRejections) {

    public DefendantValidationOutcome {
        libraGuardianRejections = List.copyOf(libraGuardianRejections);
    }
}
