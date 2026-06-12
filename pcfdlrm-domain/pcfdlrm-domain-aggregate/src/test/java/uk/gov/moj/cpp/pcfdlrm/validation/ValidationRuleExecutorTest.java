package uk.gov.moj.cpp.pcfdlrm.validation;


import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ValidationRuleExecutorTest {

    private static final String PROBLEM_CODE_1 = "PROBLEM_CODE_1";
    private static final String PROBLEM_CODE_2 = "PROBLEM_CODE_2";

    @Mock
    private ValidationRule<MigratedCaseFileReceived, MigratedCaseFileAggregate> validationRule1, validationRule2;

    @Mock
    private MigratedCaseFileReceived migratedCaseFileReceived;

    @Mock
    private MigratedCaseFileAggregate migratedCaseFileAggregate;

    @Test
    void shouldReturnEmptyProblemListsWhenAllRulesPassed() {
        shouldCollectProblemsFromAllValidationRules(null, null);
    }

    @Test
    public void shouldReturnListOffProblemsWhenFirstRuleFailed() {
        final Problem problem = new Problem(PROBLEM_CODE_1, asList(new ProblemValue(null, "dob", "12-10-2018")));

        shouldCollectProblemsFromAllValidationRules(problem, null);
    }

    @Test
    void shouldReturnListOffProblemsWhenAllRulesFailed() {
        final Problem problem1 = new Problem(PROBLEM_CODE_1, asList(new ProblemValue(null, "key1", "value1"), new ProblemValue(null, "key2", "value2")));
        final Problem problem2 = new Problem(PROBLEM_CODE_2, asList(new ProblemValue(null, "dob", "12-11-2018")));

        shouldCollectProblemsFromAllValidationRules(problem1, problem2);
    }

    @Test
    void shouldRethrowAnyExceptionThrownByAnyRule() {
        final Exception exception = new RuntimeException("Exception from rule");

        when(validationRule1.validate(migratedCaseFileReceived, migratedCaseFileAggregate)).thenThrow(exception);

        try {
            ValidationRuleExecutor.validate(migratedCaseFileReceived, migratedCaseFileAggregate, asList(validationRule1, validationRule2));
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e, equalTo(exception));
        }

        verify(validationRule1).validate(migratedCaseFileReceived, migratedCaseFileAggregate);
        verify(validationRule2, never()).validate(migratedCaseFileReceived, migratedCaseFileAggregate);
    }

    private void shouldCollectProblemsFromAllValidationRules(final Problem problem1, final Problem problem2) {
        when(validationRule1.validate(migratedCaseFileReceived, migratedCaseFileAggregate)).thenReturn(ValidationResult.newValidationResult(Optional.ofNullable(problem1)));
        when(validationRule2.validate(migratedCaseFileReceived, migratedCaseFileAggregate)).thenReturn(ValidationResult.newValidationResult(Optional.ofNullable(problem2)));

        final List<Problem> problems = ValidationRuleExecutor.validate(migratedCaseFileReceived, migratedCaseFileAggregate, asList(validationRule1, validationRule2));

        assertThat(problems, containsInAnyOrder(Stream.of(problem1, problem2).filter(Objects::nonNull).toArray()));

        verify(validationRule1).validate(migratedCaseFileReceived, migratedCaseFileAggregate);
        verify(validationRule2).validate(migratedCaseFileReceived, migratedCaseFileAggregate);
    }
}
