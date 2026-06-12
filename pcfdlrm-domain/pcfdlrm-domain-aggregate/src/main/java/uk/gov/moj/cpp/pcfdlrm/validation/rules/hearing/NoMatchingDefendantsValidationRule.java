package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.NO_MATCHING_DEFENDANTS_FOR_HEARING;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.LISTED_DEFENDANTS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;

import java.util.List;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

public class NoMatchingDefendantsValidationRule implements ValidationRule<MigratedHearingWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigratedHearingWithReferenceData migratedHearingWithReferenceData,
                                     final ReferenceDataQueryService referenceDataQueryService) {
        final MigratedHearing migratedHearing = migratedHearingWithReferenceData.getMigratedHearing();

        if (isNull(migratedHearing)) {
            return VALID;
        }
        final List<MigratedDefendantWithOffences> defendantsWithOffences = migratedHearingWithReferenceData.getMigratedDefendantWithOffences();
        if (isNull(defendantsWithOffences) || defendantsWithOffences.isEmpty()) {
            return newValidationResult(of(newProblem(NO_MATCHING_DEFENDANTS_FOR_HEARING,
                    new ProblemValue(null, LISTED_DEFENDANTS.getValue(), "No matching defendants with offences"))));
        }
        return VALID;
    }
}
