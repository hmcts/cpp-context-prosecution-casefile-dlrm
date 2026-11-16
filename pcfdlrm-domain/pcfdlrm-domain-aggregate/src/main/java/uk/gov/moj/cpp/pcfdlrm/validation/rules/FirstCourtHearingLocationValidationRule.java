package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.FIRST_COURT_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.FIRST_COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;

/**
 * Checks {@code migrationSourceSystem.firstCourtHearingLocation} against reference-data organisation units.
 * An unknown OU code is a warning: the aggregate clears the value and accepts the case.
 */
public class FirstCourtHearingLocationValidationRule implements ValidationRule<MigrationSourceSystem, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final MigrationSourceSystem migrationSourceSystem, final ReferenceDataQueryService referenceDataQueryService) {
        final String firstCourtHearingLocation = migrationSourceSystem.getFirstCourtHearingLocation();

        if (firstCourtHearingLocation == null
                || !referenceDataQueryService.retrieveOrganisationUnits(firstCourtHearingLocation).isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(FIRST_COURT_LOCATION_OUCODE_INVALID,
                new ProblemValue(null, FIRST_COURT_HEARING_LOCATION.getValue(), firstCourtHearingLocation))));
    }
}
