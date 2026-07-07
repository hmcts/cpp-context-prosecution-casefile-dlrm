package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatementOfFactsWelshValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final String SUMMONS_CODE_SUMMCA = "M";
    private static final String WELSH_DOCUMENTATION_LANGUAGE = "W";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final String summonsCode = defendantWithReferenceData.getCaseDetails().getSummonsCode();

        if (!SUMMONS_CODE_SUMMCA.equals(summonsCode)) {
            return VALID;
        }

        if(null == defendantWithReferenceData.getDefendant().getDocumentationLanguage() || !WELSH_DOCUMENTATION_LANGUAGE.equals(defendantWithReferenceData.getDefendant().getDocumentationLanguage().toString())) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final List<MigratedOffence> offenceList = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> isEmpty(trim(offence.getStatementOfFactsWelsh())))
                .toList();

        offenceList.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_STATEMENT_OF_FACTS_WELSH.getValue(), offence.getStatementOfFactsWelsh() == null ? "" : offence.getStatementOfFactsWelsh())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.STATEMENT_OF_FACTS_WELSH_REQUIRED, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
