package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.FIRST_COURT_LOCATION_OUCODE_INVALID;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;

import java.util.List;

import org.junit.jupiter.api.Test;

class FirstCourtHearingLocationValidationRuleTest {

    private static final String OU_CODE = "B01LY00";

    private final ReferenceDataQueryService referenceDataQueryService = mock(ReferenceDataQueryService.class);

    private final FirstCourtHearingLocationValidationRule rule = new FirstCourtHearingLocationValidationRule();

    @Test
    void shouldBeValidWhenOuCodeIsFoundInReferenceData() {
        when(referenceDataQueryService.retrieveOrganisationUnits(OU_CODE))
                .thenReturn(List.of(mock(OrganisationUnitReferenceData.class)));

        assertThat(rule.validate(migrationSourceSystem(OU_CODE), referenceDataQueryService).isValid(), is(true));
    }

    @Test
    void shouldRaiseProblemWithOriginalValueWhenOuCodeIsNotFound() {
        when(referenceDataQueryService.retrieveOrganisationUnits(OU_CODE)).thenReturn(emptyList());

        final List<Problem> problems = rule.validate(migrationSourceSystem(OU_CODE), referenceDataQueryService).problems();

        assertThat(problems.stream().map(Problem::getCode).toList(), contains(FIRST_COURT_LOCATION_OUCODE_INVALID.name()));
        assertThat(problems.get(0).getValues().get(0).getKey(), is("firstCourtHearingLocation"));
        assertThat(problems.get(0).getValues().get(0).getValue(), is(OU_CODE));
    }

    @Test
    void shouldBeValidWithoutLookupWhenFieldIsAbsent() {
        assertThat(rule.validate(migrationSourceSystem(null), referenceDataQueryService).isValid(), is(true));

        verify(referenceDataQueryService, never()).retrieveOrganisationUnits(anyString());
    }

    private static MigrationSourceSystem migrationSourceSystem(final String firstCourtHearingLocation) {
        return MigrationSourceSystem.migrationSourceSystem()
                .withMigrationSourceSystemName("LIBRA")
                .withMigrationSourceSystemCaseIdentifier("LIBRA123")
                .withFirstCourtHearingLocation(firstCourtHearingLocation)
                .build();
    }
}
