package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DATE_OF_HEARING_IN_THE_PAST;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_DATE_OF_HEARING;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class DateOfHearingValidationAndEnricherRuleTest {

    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String OFFENCE_START_DATE_AFTER_HEARING = "2051-10-03";
    private static final String OFFENCE_START_DATE_BEFORE_HEARING = "2049-10-03";
    private static final String PAST_DATE_OF_HEARING = "2006-11-03";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock
    MigratedDefendant defendant;

    @Test
    public void shouldReturnEmptyListWhenDateOfHearingIsInFutureAndAfterOffenceStartDate() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(DATE_OF_HEARING);
        when(migratedHearingWithReferenceData.getDefendants()).thenReturn(List.of(defendant));
        when(defendant.getOffences()).thenReturn(getOffences(OFFENCE_START_DATE_BEFORE_HEARING));
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldBevalidWhenNoDOHButWC() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(null);
        when(migratedHearingWithReferenceData.getDefendants()).thenReturn(List.of(defendant));
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldBevalidWhenDOHEmpty() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn("  ");
        when(migratedHearingWithReferenceData.getDefendants()).thenReturn(List.of(defendant));
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDateOfHearingIsBeforeOffenceStartDate() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(DATE_OF_HEARING);
        when(migratedHearingWithReferenceData.getDefendants()).thenReturn(List.of(defendant));
        when(defendant.getOffences()).thenReturn(getOffences(OFFENCE_START_DATE_AFTER_HEARING));
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        final Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_DATE_OF_HEARING.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(DATE_OF_HEARING));
    }

    @Test
    public void shouldReturnProblemWhenDateOfHearingIsInPast() {
        when(migratedHearingWithReferenceData.getMigratedHearing().getDateOfHearing()).thenReturn(PAST_DATE_OF_HEARING);
        when(migratedHearingWithReferenceData.getDefendants()).thenReturn(List.of(defendant));
        when(migratedHearingWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        final Optional<Problem> optionalProblem = new DateOfHearingPastDateValidationAndEnricherRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(DATE_OF_HEARING_IN_THE_PAST.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_DATE_OF_HEARING.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(PAST_DATE_OF_HEARING));
    }

    private List<MigratedOffence> getOffences(final String offenceCommittedDate) {
        return Collections.singletonList(MigratedOffence.migratedOffence()
                .withOffenceCommittedDate(convertToLocalDate(offenceCommittedDate))
                .build());
    }

    private LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}