package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_HEARING_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURTROOM_ID_INVALID;

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
class CourtHearingLocationValidationRuleTest {

    private static final String VALID_OU_CODE = "C50EX00";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    private final CourtHearingLocationValidationRule rule = new CourtHearingLocationValidationRule();

    @Test
    void shouldReturnValidWhenHearingIsNull() {
        when(migratedHearingWithReferenceData.getMigratedHearing()).thenReturn(null);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnInvalidWhenCourtHearingLocationIsNull() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(null);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdIsNull() {
        withValidOuCode();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(null);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdIsZero() {
        withValidOuCode();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(0);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdIsNegative() {
        withValidOuCode();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(-1);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdMatchesExistingCourtroom() {
        withValidOuCode();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(1);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnInvalidWhenCourtRoomIdDoesNotMatchAnyCourtroom() {
        withValidOuCode();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(999);

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURTROOM_ID_INVALID.name()));
    }

    private void withValidOuCode() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtHearingLocation()).thenReturn(VALID_OU_CODE);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtrooms(VALID_OU_CODE))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(1).build()))
                        .build()));
    }
}
