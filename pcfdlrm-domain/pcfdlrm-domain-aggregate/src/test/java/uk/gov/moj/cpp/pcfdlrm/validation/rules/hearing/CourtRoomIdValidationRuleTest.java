package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
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
class CourtRoomIdValidationRuleTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    private final CourtRoomIdValidationRule rule = new CourtRoomIdValidationRule();

    @Test
    void shouldReturnValidWhenCourtRoomIdIsNull() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(null);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdIsZero() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(0);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdIsNegative() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(-1);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenNoOrganisationUnitReferenceDataPresent() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(5);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnValidWhenCourtRoomIdMatchesExistingCourtroom() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(1);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVOWithCourtroom(1));

        assertThat(rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().isEmpty(), is(true));
    }

    @Test
    void shouldReturnInvalidWhenCourtRoomIdDoesNotMatchAnyCourtroom() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getCourtRoomId()).thenReturn(999);
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVOWithCourtroom(1));

        final Optional<Problem> problem = rule.validate(migratedHearingWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(COURTROOM_ID_INVALID.name()));
    }

    private ReferenceDataVO referenceDataVOWithCourtroom(final int courtroomId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomsReferenceData(
                OrganisationUnitWithCourtroomsReferenceData.organisationUnitWithCourtroomsReferenceData()
                        .withCourtrooms(List.of(CourtRoom.courtRoom().withCourtroomId(courtroomId).build()))
                        .build());
        return referenceDataVO;
    }
}