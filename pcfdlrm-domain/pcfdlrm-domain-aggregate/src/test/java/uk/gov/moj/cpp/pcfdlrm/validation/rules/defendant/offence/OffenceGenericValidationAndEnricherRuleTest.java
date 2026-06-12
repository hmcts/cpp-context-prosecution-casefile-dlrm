package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_CODE_IS_GENERIC;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

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
public class OffenceGenericValidationAndEnricherRuleTest {

    private static final String GENERIC_OFFENCE_CODE = "998";
    private static final String GENERIC_ALTERED_OFFENCE_CODE = "998A";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @InjectMocks
    private OffenceGenericValidationAndEnricherRule offenceGenericValidationAndEnricherRule;

    @Test
    public void shouldCreateProblemWhenOffenceCodeIsGeneric() {

        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(GENERIC_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(GENERIC_OFFENCE_CODE);

        final Optional<Problem> problem = offenceGenericValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
    }

    @Test
    public void shouldNotCreateProblemWhenOffenceCodeIsGenericAltered() {

        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(GENERIC_OFFENCE_CODE));
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(GENERIC_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(GENERIC_ALTERED_OFFENCE_CODE);

        final Optional<Problem> problem = offenceGenericValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(false));

    }

    @Test
    public void shouldValidateGenericOffenceCode() {

        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(GENERIC_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(GENERIC_OFFENCE_CODE);

        final Optional<Problem> problem = offenceGenericValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_CODE_IS_GENERIC.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is(OFFENCE_CODE.getValue()));
        assertThat(problem.get().getValues().get(0).getValue(), is("998"));
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String offenceCode) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final MigratedOffence offence = MigratedOffence.migratedOffence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();

        final MigratedDefendant defendant = migratedDefendant().withId(UUID.randomUUID()).withInitiationCode("C")

                .withOffences(Arrays.asList(offence))
                .build();
        final List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
        offenceReferenceData.add(new OffenceReferenceData.Builder()
                .withCjsOffenceCode(GENERIC_OFFENCE_CODE)
                .withLocationRequired("Y")
                .build());

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode).withLocationRequired("Y")
                .build()
        );
    }
}