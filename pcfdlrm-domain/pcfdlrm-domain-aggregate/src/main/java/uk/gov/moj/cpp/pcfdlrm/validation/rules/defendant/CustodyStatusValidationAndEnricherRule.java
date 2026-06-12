package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_CUSTODY_STATUS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.DEFENDANT_CUSTODY_STATUS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;

import java.util.Optional;

public class CustodyStatusValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        if (nonNull(defendantWithReferenceData.getDefendant().getOrganisationName())) {
            return VALID;
        } else if (nonNull(defendantWithReferenceData.getDefendant().getIndividual()) && (defendantWithReferenceData.getDefendant().getIndividual().getCustodyStatus() == null)) {
            return newValidationResult(of(newProblem(DEFENDANT_CUSTODY_STATUS_INVALID, new ProblemValue(null, DEFENDANT_CUSTODY_STATUS.getValue(), ""))));
        }

        final String custodyStatus = defendantWithReferenceData.getDefendant().getIndividual().getCustodyStatus();
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        Optional<BailStatusReferenceData> bailStatusReferenceDataOptional = referenceDataVO.getBailStatusReferenceData().stream().filter(custodyStatusReferenceData -> custodyStatus.equals(custodyStatusReferenceData.getStatusCode())).findAny();
        if (bailStatusReferenceDataOptional.isPresent()) {
            return VALID;
        }

        bailStatusReferenceDataOptional = referenceDataQueryService.retrieveBailStatuses().stream().filter(custodyStatusReferenceData -> custodyStatus.equals(custodyStatusReferenceData.getStatusCode())).findAny();
        if (bailStatusReferenceDataOptional.isPresent()) {
            defendantWithReferenceData.getReferenceDataVO().addBailStatusReferenceData(bailStatusReferenceDataOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(DEFENDANT_CUSTODY_STATUS_INVALID, new ProblemValue(null, DEFENDANT_CUSTODY_STATUS.getValue(), custodyStatus))));

        }
    }
}
