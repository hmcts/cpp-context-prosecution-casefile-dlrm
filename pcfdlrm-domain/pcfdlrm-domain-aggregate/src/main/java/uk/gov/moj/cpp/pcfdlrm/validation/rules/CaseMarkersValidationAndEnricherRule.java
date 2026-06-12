package uk.gov.moj.cpp.pcfdlrm.validation.rules;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;


import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.Problems;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;

import java.util.ArrayList;
import java.util.List;

public class CaseMarkersValidationAndEnricherRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {


    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final List<CaseMarker> caseMarkers = prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers();

        if (isEmpty(caseMarkers)) {
            return VALID;
        }

        final List<String> caseMarkerTypeCodes = caseMarkers.stream().map(CaseMarker::getMarkerTypeCode).toList();

        final List<CaseMarker> caseMarkersRefData = referenceDataQueryService.getCaseMarkerDetails().stream()
                .filter(caseMarker -> caseMarkerTypeCodes.stream().anyMatch(caseMarker.getMarkerTypeCode()::equalsIgnoreCase))
                .collect(toList());
        prosecutionWithReferenceData.getReferenceDataVO().setCaseMarkers(caseMarkersRefData);

        final List<ProblemValue> problemValues = new ArrayList<>();
        for (int i = 0; i < caseMarkers.size(); i++) {
            final CaseMarker curCaseMarker = caseMarkers.get(i);
            final boolean isValidCaseMarker = caseMarkersRefData.stream().anyMatch(caseMarkerRefData -> caseMarkerRefData.getMarkerTypeCode().equalsIgnoreCase(curCaseMarker.getMarkerTypeCode()));
            if (!isValidCaseMarker) {
                problemValues.add(new ProblemValue(Integer.toString(i), FieldName.CASE_MARKERS.getValue(), curCaseMarker.getMarkerTypeCode()));
            }
        }


        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(Problems.newProblem(ProblemCode.CASE_MARKER_IS_INVALID, problemValues.toArray(new ProblemValue[0]))));
    }
}
