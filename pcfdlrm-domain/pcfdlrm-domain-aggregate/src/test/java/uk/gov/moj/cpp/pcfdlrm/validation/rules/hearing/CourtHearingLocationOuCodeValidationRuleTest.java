package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_HEARING_LOCATION_OUCODE_INVALID;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtHearingLocationOuCodeValidationRuleTest {

    private static final String VALID_OU_CODE = "C50EX00";
    private static final String INVALID_OU_CODE = "BAD";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    private final CourtHearingLocationOuCodeValidationRule rule = new CourtHearingLocationOuCodeValidationRule();

    @Test
    void shouldReturnValidWhenOrganisationUnitAlreadyResolved() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(anOrganisationUnit());
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(VALID_OU_CODE);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnInvalidWhenOuCodeIsNull() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(null);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
    }

    @Test
    void shouldReturnInvalidWhenOuCodeHasInvalidLength() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(INVALID_OU_CODE);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
    }

    @Test
    void shouldResolveAndReturnValidWhenOuCodeResolvesFromReferenceData() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(VALID_OU_CODE);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(VALID_OU_CODE)).thenReturn(Optional.of(anOrganisationUnit()));

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
        assertThat(referenceDataVO.getOrganisationUnitWithCourtroomsReferenceData().isPresent(), is(true));
    }

    @Test
    void shouldReturnInvalidWhenOuCodeDoesNotResolveFromReferenceData() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(VALID_OU_CODE);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(VALID_OU_CODE)).thenReturn(Optional.empty());

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
    }

    private OrganisationUnitWithCourtroomsReferenceData anOrganisationUnit() {
        return OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(1).build()))
                .build();
    }
}