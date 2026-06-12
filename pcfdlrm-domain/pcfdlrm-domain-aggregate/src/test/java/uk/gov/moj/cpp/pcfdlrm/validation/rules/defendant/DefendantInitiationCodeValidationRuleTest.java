package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_INITIATION_CODE_INVALID;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantInitiationCodeValidationRuleTest {
    @InjectMocks
    private DefendantInitiationCodeValidationRule defendantInitiationCodeValidationRule;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;


    @Test
    public void shouldReturnProblemWhenDefendantAndCaseCodesNotMatching() {
        final String caseInitiationCode = "S";
        final String defendantInitiationCode = "J";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(caseInitiationCode, defendantInitiationCode);

        final Optional<Problem> problem = defendantInitiationCodeValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.get().getCode(), is(DEFENDANT_INITIATION_CODE_INVALID.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is("defendantInitiationCode"));

    }
    @Test
    public void shouldReturnProblemWhenDefendantAndCaseCodesNotMatch() {
        final String caseInitiationCode = "J";
        final String defendantInitiationCode = "S";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(caseInitiationCode, defendantInitiationCode);

        final Optional<Problem> problem = defendantInitiationCodeValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.get().getCode(), is(DEFENDANT_INITIATION_CODE_INVALID.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is("defendantInitiationCode"));
    }

    @Test
    public void shouldNoReturnProblemWhenDefendantAndCaseCodesMatchForIntiationCodeAsJ() {
        final String caseInitiationCode = "J";
        final String defendantInitiationCode = "J";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(caseInitiationCode, defendantInitiationCode);

        final Optional<Problem> problem = defendantInitiationCodeValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat("true", !problem.isPresent());
    }
    @Test
    public void shouldNoReturnProblemWhenDefendantAndCaseCodesMatch() {
        final String caseInitiationCode = "S";
        final String defendantInitiationCode = "C";
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(caseInitiationCode, defendantInitiationCode);

        final Optional<Problem> problem = defendantInitiationCodeValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat("true", !problem.isPresent());
    }
    @Test
    public void shouldReturnNoProblemWhenDefendantInitiationCodeIsNull() {
        final String caseInitiationCode = "S";
        final String defendantInitiationCode = null;
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(caseInitiationCode, defendantInitiationCode);

        final Optional<Problem> problem = defendantInitiationCodeValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat("true", !problem.isPresent());
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String caseInitiationCode, final String defendantInitiationCode) {
        final MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withInitiationCode(defendantInitiationCode).build();
        final CaseDetails caseDetails = new CaseDetails.Builder().withInitiationCode(caseInitiationCode).build();
        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }
}
