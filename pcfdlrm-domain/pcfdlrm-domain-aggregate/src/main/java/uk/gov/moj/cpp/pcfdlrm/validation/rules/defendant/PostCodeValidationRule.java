package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.Constants;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PostCodeValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern POST_CODE_FORMAT = Pattern.compile(Constants.POST_CODE_REGEX.getValue());

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        if (isDefendantPostCodeAbsent(defendantWithReferenceData) && isParentGuardianPostCodeAbsent(defendantWithReferenceData) && isDefendantIndividualPostCodeAbsent(defendantWithReferenceData)) {
            return ValidationResult.VALID;
        }

        final String postcode = isDefendantPostCodeAbsent(defendantWithReferenceData) ? null : defendantWithReferenceData.getDefendant().getAddress().getPostcode();

        final String parentGuardianPostCode = isParentGuardianPostCodeAbsent(defendantWithReferenceData) ? null : defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation().getAddress().getPostcode();

        final String defendantIndividualPostCode = isDefendantIndividualPostCodeAbsent(defendantWithReferenceData) ? null : defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getAddress().getPostcode();

        final Problem defendantPostCodeProblem = isDefendantPostCodeAbsent(defendantWithReferenceData) || POST_CODE_FORMAT.matcher(postcode).matches() ?
                null :
                Problems.newProblem(ProblemCode.INVALID_DEFENDANT_POST_CODE, new ProblemValue(null, FieldName.DEFENDANT_POST_CODE.getValue(), postcode));

        final Problem defendantIndividualPostCodeProblem = isDefendantIndividualPostCodeAbsent(defendantWithReferenceData) || POST_CODE_FORMAT.matcher(defendantIndividualPostCode).matches() ?
                null :
                Problems.newProblem(ProblemCode.INVALID_DEFENDANT_INDIVIDUAL_POST_CODE, new ProblemValue(null, FieldName.DEFENDANT_INDIVIDUAL_POST_CODE.getValue(), defendantIndividualPostCode));

        final Problem guardianPostCodeProblem = isParentGuardianPostCodeAbsent(defendantWithReferenceData) || POST_CODE_FORMAT.matcher(parentGuardianPostCode).matches() ?
                null :
                Problems.newProblem(ProblemCode.INVALID_GUARDIAN_POST_CODE, new ProblemValue(null, FieldName.PARENT_GUARDIAN_POST_CODE.getValue(), parentGuardianPostCode));

        final List<Problem> problemList = new ArrayList<>();

        if (defendantPostCodeProblem != null) {
            problemList.add(defendantPostCodeProblem);
        }

        if (guardianPostCodeProblem != null) {
            problemList.add(guardianPostCodeProblem);
        }
        if (defendantIndividualPostCodeProblem != null) {
            problemList.add(defendantIndividualPostCodeProblem);
        }

        return problemList.isEmpty() ? ValidationResult.VALID : ValidationResult.newValidationResult(problemList);
    }


    private boolean isParentGuardianPostCodeAbsent(final DefendantWithReferenceData defendantWithReferenceData) {
        return (defendantWithReferenceData.getDefendant().getIndividual() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation()  == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation().getAddress() == null ||
                isBlank(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation().getAddress().getPostcode()));
    }

    private boolean isDefendantPostCodeAbsent(final DefendantWithReferenceData defendantWithReferenceData) {
        return (defendantWithReferenceData.getDefendant().getAddress() == null
                || isBlank(defendantWithReferenceData.getDefendant().getAddress().getPostcode()));
    }

    private boolean isDefendantIndividualPostCodeAbsent(final DefendantWithReferenceData defendantWithReferenceData) {
        final String postCode = ofNullable(defendantWithReferenceData.getDefendant())
                .map(MigratedDefendant::getIndividual)
                .map(Individual::getPersonalInformation)
                .map(PersonalInformation::getAddress)
                .map(Address::getPostcode)
                .orElse("");
        return isBlank(postCode);
    }

}
