package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholRelatedOffence.alcoholRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

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
public class OffenceDrugLevelAmountValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenceDrugLevelAmountValidationAndEnricherRule offenceDrugLevelAmountValidationAndEnricherRule;

    @Test
    public void shouldReturnEmptyListWhenNoOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }


    @Test
    public void shouldReturnProblemWhenOffenceIsWithoutAlcoholLevelInfo() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffence(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenOffenceIsWithAlcoholLevelInfo() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffenceWithAlcoholLevelInfo(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }


    private DefendantWithReferenceData getMockDefendantWithReferenceData(final MigratedOffence offence) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final MigratedDefendant.Builder defendantBuilder = MigratedDefendant.migratedDefendant().withId(UUID.randomUUID()).withInitiationCode("C");
        if (offence != null) {

            defendantBuilder
                    .withOffences(Arrays.asList(offence));

        }
        final MigratedDefendant defendant = defendantBuilder.build();
        final List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
        offenceReferenceData.add(new OffenceReferenceData.Builder()
                .withCjsOffenceCode(MOCK_OFFENCE_CODE)
                .build());

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private MigratedOffence getOffence(final String offenceCode) {
        return migratedOffence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();
    }

    private MigratedOffence getOffenceWithAlcoholLevelInfo(final String offenceCode) {
        return migratedOffence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withAlcoholRelatedOffence(alcoholRelatedOffence().withAlcoholLevelAmount(1).withAlcoholLevelMethod("A").build())
                .build();
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode).withDrugsOrAlcoholRelated("Y")
                .build()
        );
    }

}