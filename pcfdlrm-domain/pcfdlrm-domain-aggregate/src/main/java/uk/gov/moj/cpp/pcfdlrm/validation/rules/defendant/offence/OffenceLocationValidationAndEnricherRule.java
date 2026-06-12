package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.offence;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_REQUIRES_A_LOCATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_LOCATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceLocationValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty()) {
            return VALID;
        }

        final List<Problem> problems = defendantWithReferenceData.getDefendant().getOffences().stream()
                .map(offence -> verifyOffenceLocationRequired(offence, defendantWithReferenceData.getCaseDetails().getInitiationCode(), defendantWithReferenceData, referenceDataQueryService))
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (null == problems || problems.isEmpty()) {
            return VALID;
        }

        return newValidationResult(problems);
    }

    private Problem verifyOffenceLocationRequired(final MigratedOffence offence, final String initiationCode, final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        List<OffenceReferenceData> offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull).collect(Collectors.toList());

        if (offenceReferenceDataListFromVO.isEmpty()) {

            if (referenceDataQueryService == null) {
                return getProblemForOffence(offence, offence.getOffenceLocation());
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

                offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                        .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }

        if (offenceReferenceDataListFromVO != null && !offenceReferenceDataListFromVO.isEmpty()) {
            return validateOffenceLocation(offence, offenceReferenceDataListFromVO);
        }

        return null;

    }

    @SuppressWarnings("squid:S1155")
    private Problem validateOffenceLocation(final MigratedOffence offence, final List<OffenceReferenceData> offenceReferenceDataListFromVO) {
        if (offenceReferenceDataListFromVO.size() == 0) {
            return getProblemForOffence(offence, offence.getOffenceLocation());
        }

        if (hasRequiredLocation(offenceReferenceDataListFromVO) && isEmpty(offence.getOffenceLocation())) {
            return getProblemForOffence(offence, offence.getOffenceLocation());
        }

        return null;
    }

    private boolean hasRequiredLocation(final List<OffenceReferenceData> offenceReferenceData) {
        return offenceReferenceData.stream().map(OffenceReferenceData::getLocationRequired)
                .map("Y"::equals).findAny()
                .orElse(false);
    }


    private Problem getProblemForOffence(final MigratedOffence offence, final String offenceLocation) {
        return newProblem(
                OFFENCE_REQUIRES_A_LOCATION,
                new ProblemValue(offence.getOffenceId().toString(), OFFENCE_LOCATION.getValue(), offenceLocation == null ? "" : offenceLocation),
                new ProblemValue(offence.getOffenceId().toString(), OFFENCE_CODE.getValue(), offence.getOffenceCode()),
                new ProblemValue(offence.getOffenceId().toString(), OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(offence.getOffenceSequenceNumber())));
    }

}