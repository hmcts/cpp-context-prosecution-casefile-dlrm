package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.ABSENT;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian.ParentGuardianShape.INDIVIDUAL;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class LibraParentGuardianSanitiser {

    private LibraParentGuardianSanitiser() {
    }

    public static void sanitise(final MigratedDefendant.Builder migratedDefendantBuilder, final List<Problem> defendantProblemList) {
        final Individual individual = migratedDefendantBuilder.build().getIndividual();
        final ParentGuardianInformation parentGuardianInformation = individual == null ? null : individual.getParentGuardianInformation();
        final ParentGuardianShape shape = ParentGuardianShape.of(parentGuardianInformation);
        if (shape == ABSENT) {
            return;
        }

        ParentGuardianInformation sanitised = shape == INDIVIDUAL ? withNormalisedGender(parentGuardianInformation) : parentGuardianInformation;
        for (final UnaryOperator<ParentGuardianInformation> nulling : nullingFunctionsFor(defendantProblemList)) {
            sanitised = nulling.apply(sanitised);
        }

        if (!sanitised.equals(parentGuardianInformation)) {
            migratedDefendantBuilder.withIndividual(Individual.individual()
                    .withValuesFrom(individual)
                    .withParentGuardianInformation(sanitised)
                    .build());
        }
    }

    private static List<UnaryOperator<ParentGuardianInformation>> nullingFunctionsFor(final List<Problem> defendantProblemList) {
        return defendantProblemList.stream()
                .filter(problem -> LibraParentGuardianOutcomes.isNull(problem.getCode()))
                .flatMap(problem -> problem.getValues().stream())
                .map(problemValue -> LibraParentGuardianOutcomes.nullingFunctionFor(problemValue.getKey()))
                .flatMap(Optional::stream)
                .toList();
    }

    private static ParentGuardianInformation withNormalisedGender(final ParentGuardianInformation parentGuardianInformation) {
        final String gender = LibraGenderCode.normalise(parentGuardianInformation.getGender()).orElse(NOT_KNOWN.name());
        return Objects.equals(gender, parentGuardianInformation.getGender())
                ? parentGuardianInformation
                : ParentGuardianInformation.parentGuardianInformation().withValuesFrom(parentGuardianInformation).withGender(gender).build();
    }
}
