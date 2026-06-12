package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENDER_CODE_IS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_OFFENDER_CODE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenderCodeValidationAndEnricherRuleTest {
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String OFFENDER_CODE = "PYO";
    private static final String OFFENDER_CODE_DESCRIPTIOON = "Persistent Young Offender";
    private static final Integer OFFENDER_SEGNUM = 10;
    private static final String OFFENDER_VALIDFROM = "2019-04-01";
    private static final String WRONG_OFFENDER_CODE = "WRONG CODE";
    private static final String MOCK_OFFENDER_CODE = "MOCK CODE";
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenderCodeValidationAndEnricherRule offenderCodeValidationAndEnricherRule;

    @Test
    public void shouldValidateInvalidOffenderCodeAndNotPopulateOffenderCodeRefData() {

        when(referenceDataQueryService.retrieveOffenderCodes()).thenReturn(getMockOffenderCodesReferenceData());
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(WRONG_OFFENDER_CODE);

        final Optional<Problem> problem = offenderCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem, equalTo(Optional.of(newProblem(OFFENDER_CODE_IS_INVALID, DEFENDANT_OFFENDER_CODE.getValue(), WRONG_OFFENDER_CODE))));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().size(), is(1));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getOffenderCode(), is(not(WRONG_OFFENDER_CODE)));
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getOffenderCodeDescription());
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getSeqNum());
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getValidFrom());
        verify(referenceDataQueryService, times(1)).retrieveOffenderCodes();

    }

    @Test
    public void shouldNotInvalidateValidOffenderCodeAndShouldPopulateOffenderCodeRefData() {

        when(referenceDataQueryService.retrieveOffenderCodes()).thenReturn(getMockOffenderCodesReferenceData());
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(OFFENDER_CODE);

        final Optional<Problem> problem = offenderCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(false));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().size(), is(2));
        assertNotNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(1).getId());
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(1).getOffenderCode(), is(OFFENDER_CODE));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(1).getOffenderCodeDescription(), is(OFFENDER_CODE_DESCRIPTIOON));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(1).getSeqNum(), is(OFFENDER_SEGNUM));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(1).getValidFrom(), is(OFFENDER_VALIDFROM));
        verify(referenceDataQueryService, times(1)).retrieveOffenderCodes();

    }

    @Test
    public void shouldNotInvalidateNullOffenderCodeAndShouldNotPopulateOffenderCodeRefData() {

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);

        final Optional<Problem> problem = offenderCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(false));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().size(), is(1));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getOffenderCode(), is(MOCK_OFFENDER_CODE));
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getOffenderCodeDescription());
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getSeqNum());
        assertNull(defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().get(0).getValidFrom());

    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String offenderCode) {
        final Individual individual = Individual.individual().withOffenderCode(offenderCode).build();

        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(DEFENDANT_ID).withIndividual(individual).build();

        final List<OffenderCodeReferenceData> offenderCodeReferenceData = new ArrayList<>();
        offenderCodeReferenceData.add(new OffenderCodeReferenceData.Builder()
                .withId(UUID.randomUUID())
                .withOffenderCode(MOCK_OFFENDER_CODE)
                .build());
        referenceDataVO.setOffenderCodeReferenceData(offenderCodeReferenceData);
        return new DefendantWithReferenceData(defendant, referenceDataVO, null);
    }

    private List<OffenderCodeReferenceData> getMockOffenderCodesReferenceData() {
        return Arrays.asList(OffenderCodeReferenceData
                .offenderCodeReferenceData()
                .withId(UUID.randomUUID())
                .withOffenderCode(OFFENDER_CODE)
                .withOffenderCodeDescription(OFFENDER_CODE_DESCRIPTIOON)
                .withSeqNum(OFFENDER_SEGNUM)
                .withValidFrom(OFFENDER_VALIDFROM)
                .build()
        );
    }

}