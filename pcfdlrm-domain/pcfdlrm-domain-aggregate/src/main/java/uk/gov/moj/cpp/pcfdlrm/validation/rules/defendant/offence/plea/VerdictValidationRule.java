package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea;

import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class VerdictValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {
    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService context) {
        final List<MigratedOffence> offences = defendantWithReferenceData.getDefendant().getOffences();
        if(null!=offences) {
            final boolean hasVerdict = offences.stream().anyMatch(e -> Objects.nonNull(e.getVerdict()));
            if (!hasVerdict) {
                return VALID;
            }
        } else {
            return VALID;
        }



        List<Problem> problems = new ArrayList<>();

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictRefedataMap = Optional.ofNullable(defendantWithReferenceData.getReferenceDataVO().getVerdictReferenceDataMap()).orElse(new HashMap<UUID, Map<UUID, VerdictReferenceData>>());
        Map<UUID, VerdictReferenceData> verdictdata = verdictRefedataMap.get(defendantWithReferenceData.getDefendant().getId());
        for(MigratedOffence offence:defendantWithReferenceData.getDefendant().getOffences() ) {
            final boolean hasVerdictId = Objects.nonNull(verdictdata) && Objects.nonNull(verdictdata.get(offence.getOffenceId()));
            if(!hasVerdictId && Optional.ofNullable(offence.getVerdict()).map(MigratedVerdict::getId).isPresent()){
                final Problem problem = newProblem(ProblemCode.INVALID_VERDICT, new ProblemValue(ProblemCode.INVALID_VERDICT.name(), "verdict id", Optional.ofNullable(offence.getVerdict()).map(v -> v.getId().toString()).orElse("Invalid verdict id")));
                problems.add(problem);
            }

            validateVerdictDate(offence, hasVerdictId, problems);

        }

        return problems.isEmpty() ? ValidationResult.VALID : newValidationResult(problems);
    }

    private void validateVerdictDate(MigratedOffence offence, boolean hasVerdictId, List<Problem> problems) {
        if (!hasVerdictId) {
            return;
        }

        Optional<LocalDate> verdictDate = Optional.ofNullable(offence.getVerdict()).map(MigratedVerdict::getVerdictDate);
        
        if (verdictDate.isEmpty()) {
            final Problem problem = newProblem(ProblemCode.VERDICT_DATE_ABSENT, new ProblemValue(ProblemCode.VERDICT_DATE_ABSENT.name(), "verdict date", "Absent"));
            problems.add(problem);
        } else if (verdictDate.get().isAfter(LocalDate.now())) {
            final Problem problem = newProblem(ProblemCode.VERDICT_DATE_CANNOT_BE_FUTURE_DATE, new ProblemValue(ProblemCode.VERDICT_DATE_CANNOT_BE_FUTURE_DATE.name(), "verdict date", verdictDate.get().toString()));
            problems.add(problem);
        }
    }
}
