package uk.gov.moj.cpp.pcfdlrm.validation.rules.hearing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.HEARING_TYPE_CODE_INVALID;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class HearingTypeCodeValidationRuleTest {

    private static final String HEARING_TYPE_CODE = "FPTP";
    private static final String HEARING_DESCRIPTION = "Further Plea & Trial Preparation";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MigratedHearingWithReferenceData migratedHearingWithReferenceData;

    @Mock
    MigratedDefendant defendant;

    @Test
    public void shouldReturnEmptyListWhenCaseHearingTypeCodeIsValid() {

        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(HEARING_TYPE_CODE, HEARING_DESCRIPTION);
        when(migratedHearingWithReferenceData.getMigratedHearing().getHearingType()).thenReturn(HEARING_TYPE_CODE);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        final ValidationResult optionalProblem = new HearingTypeCodeValidationRule().validate(migratedHearingWithReferenceData, referenceDataQueryService);
        assertThat(optionalProblem.isValid(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenCaseHearingTypeCodeIsInvalid() {

        final String invalidPoliceForceCode = "X";
        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(invalidPoliceForceCode, "invalid hearing type");

        when(migratedHearingWithReferenceData.getMigratedHearing().getHearingType()).thenReturn("X");
        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        final Optional<Problem> optionalProblem = new HearingTypeCodeValidationRule().validate(migratedHearingWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(HEARING_TYPE_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("hearingTypeCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidPoliceForceCode));
    }

    private HearingTypes getMockHearingTypesReferenceData(final String hearingCode, final String hearingDescription) {
        List<HearingType> hearingTypesReferenceData = new ArrayList<>();
        hearingTypesReferenceData.add(HearingType.hearingType()
                .withId(UUID.randomUUID())
                .withSeqId(20)
                .withHearingCode(HEARING_TYPE_CODE)
                .withHearingDescription(HEARING_DESCRIPTION)
                .build()

        );
        return HearingTypes.hearingTypes().withHearingtypes(hearingTypesReferenceData).build();
    }
}