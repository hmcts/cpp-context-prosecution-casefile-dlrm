package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence.plea;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PleaValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {
    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService context) {

        final List<MigratedOffence> offences = defendantWithReferenceData.getDefendant().getOffences();

        if(isNotEmpty(offences)) {
            final boolean hasPleas = offences.stream().anyMatch(e -> Objects.nonNull(e.getPlea()));
            if (!hasPleas) {
                return VALID;
            }
        } else {
            return VALID;
        }

        List<Problem> problems = new ArrayList<>();

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaRefedataMap = ofNullable(defendantWithReferenceData.getReferenceDataVO().getPleaReferenceDataMap()).orElse(new HashMap<>());
        final Map<UUID, PleaReferenceData> pleaData = pleaRefedataMap.getOrDefault(defendantWithReferenceData.getDefendant().getId(), new HashMap<>());
        for(final MigratedOffence offence : defendantWithReferenceData.getDefendant().getOffences() ) {
            final boolean hasPleaId = Objects.nonNull(pleaData.get(offence.getOffenceId()));
            final PleaReferenceData pleaReferenceData = hasPleaId ? pleaData.get(offence.getOffenceId()) : PleaReferenceData.pleaReferenceData().build();
            addOffenceProblems(offence, hasPleaId, pleaReferenceData.getPleaTypeGuiltyFlag(), problems);
        }

        return problems.isEmpty() ? ValidationResult.VALID : newValidationResult(problems);
    }

    private void addOffenceProblems(final MigratedOffence offence, final boolean hasPleaId, final String pleaTypeGuiltyFlag, final List<Problem> problems) {
        if(!hasPleaId && ofNullable(offence.getPlea()).map(MigratedPlea::getId).isPresent()) {
            final Problem problem = newProblem(ProblemCode.INVALID_PLEA, new ProblemValue(ProblemCode.INVALID_PLEA.name(), "plea id", ofNullable(offence.getPlea()).map(p -> p.getId().toString()).orElse("Invalid plea id")));
            problems.add(problem);
        }

        if(hasPleaId
                && ofNullable(offence.getPlea()).map(MigratedPlea::getPleaDate).filter(e->e.isAfter(LocalDate.now())).isPresent()) {
            final Problem problem = newProblem(ProblemCode.PLEA_DATE_CANNOT_BE_FUTURE_DATE, new ProblemValue(ProblemCode.PLEA_DATE_CANNOT_BE_FUTURE_DATE.name(), "plea date", offence.getPlea().getPleaDate().toString()));
            problems.add(problem);
        }

        if(hasPleaId && pleaTypeGuiltyFlag.equals("Yes")
                && ofNullable(offence.getPlea()).map(MigratedPlea::getPleaDate).isEmpty()) {
            final Problem problem = newProblem(ProblemCode.PLEA_DATE_ABSENT, new ProblemValue(ProblemCode.PLEA_DATE_ABSENT.name(), "plea date", "Absent"));
            problems.add(problem);
        }
    }
}
