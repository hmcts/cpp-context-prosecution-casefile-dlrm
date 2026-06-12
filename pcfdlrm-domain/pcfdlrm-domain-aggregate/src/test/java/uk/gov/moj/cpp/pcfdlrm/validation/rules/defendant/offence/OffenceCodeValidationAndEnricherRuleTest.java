package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
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
public class OffenceCodeValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenceCodeValidationAndEnricherRule offenceCodeValidationAndEnricherRule;

    @Test
    public void shouldNotInvalidateValidOffenceCode() {

        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(MOCK_OFFENCE_CODE);

        final ValidationResult problem = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(problem.isValid(), is(true));

    }

    @Test
    public void shouldValidateInvalidOffenceCode() {
        final String offenceCode2 = "code2";
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offenceCode2);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_CODE_IS_INVALID.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is(OFFENCE_CODE.getValue()));
        assertThat(problem.get().getValues().get(0).getValue(), is(offenceCode2));
    }

    @Test
    public void shouldNotRaiseProblemWhenOffenceCodeIsGenericAltered() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData("998A");
        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);
        Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(false));
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String offenceCode) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final MigratedOffence offence = migratedOffence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withOffenceSequenceNumber(1)
                .build();

        final MigratedDefendant defendant = migratedDefendant().withId(UUID.randomUUID())

                .withOffences(Arrays.asList(offence))
                .withInitiationCode("C")
                .build();
        final List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
        offenceReferenceData.add(new OffenceReferenceData.Builder()
                .withCjsOffenceCode(MOCK_OFFENCE_CODE)
                .build());

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode)
                .build()
        );
    }

}