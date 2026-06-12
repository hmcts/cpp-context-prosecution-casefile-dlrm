package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.ALCOHOL_DRUG_LEVEL_AMOUNT_MISSING;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_ALCOHOL_LEVEL_AMOUNT;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceDrugLevelAmountValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {


        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty()) {
            return VALID;
        }

        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream().map(offence ->
                verifyAlcoholDrugLevelAmountRequired(offence, defendantWithReferenceData.getCaseDetails().getInitiationCode(), defendantWithReferenceData, referenceDataQueryService)).filter(Objects::nonNull).collect(Collectors.toList());

        if (null == problemValues || problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(ALCOHOL_DRUG_LEVEL_AMOUNT_MISSING, problemValues.toArray(new ProblemValue[problemValues.size()]))));

    }

    private ProblemValue verifyAlcoholDrugLevelAmountRequired(final MigratedOffence offence, final String initiationCode, final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();
        final List<OffenceReferenceData> offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull).collect(Collectors.toList());

        if (offenceReferenceDataListFromVO != null && !offenceReferenceDataListFromVO.isEmpty()) {
            return validateAlcoholDrugLevelAmount(offence, offenceReferenceDataListFromVO);
        }

        final List<OffenceReferenceData> newOffenceReferenceDataList = referenceDataQueryService.retrieveOffenceData(offence, initiationCode).stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (newOffenceReferenceDataList != null && !newOffenceReferenceDataList.isEmpty()) {
            if (referenceDataVO.getOffenceReferenceData() != null) {
                referenceDataVO.getOffenceReferenceData().addAll(newOffenceReferenceDataList);
            } else {
                final List<OffenceReferenceData> offenceReferenceDataList = new ArrayList<>();
                offenceReferenceDataList.addAll(newOffenceReferenceDataList);
                referenceDataVO.setOffenceReferenceData(offenceReferenceDataList);
            }
            return null;
        }
        return null;

    }

    private ProblemValue validateAlcoholDrugLevelAmount(final MigratedOffence offence, final List<OffenceReferenceData> offenceReferenceDataListFromVO) {
        if (offenceReferenceDataListFromVO.isEmpty()) {
            return getProblemValueForAlcoholAmount(offence, offence.getAlcoholRelatedOffence() != null ? offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() : null);
        }

        if (hasDrugsOrAlcoholRelatedFlag(offenceReferenceDataListFromVO) &&
                (offence.getAlcoholRelatedOffence() == null || (offence.getAlcoholRelatedOffence() != null && offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() == null))) {
            return getProblemValueForAlcoholAmount(offence, null);
        }

        return null;
    }

    private boolean hasDrugsOrAlcoholRelatedFlag(final List<OffenceReferenceData> offenceReferenceData) {
        return offenceReferenceData.stream().map(OffenceReferenceData::getDrugsOrAlcoholRelated).filter(Objects::nonNull).map(String::trim)
                .map("Y"::equals).findAny()
                .orElse(false);
    }


    private ProblemValue getProblemValueForAlcoholAmount(final MigratedOffence offence, final Integer offenceAlcoholLevelAmount) {
        return new ProblemValue(offence.getOffenceId().toString(), OFFENCE_ALCOHOL_LEVEL_AMOUNT.getValue(), offenceAlcoholLevelAmount == null ? "" : offenceAlcoholLevelAmount.toString());
    }

}