package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class ModeOfTrialRefDataEnricher implements DefendantRefDataEnricher {

    private static final String SUMMARY_ONLY_OFFENCE = "Summary-only offence";
    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final List<ModeOfTrialReasonsReferenceData> modeOfTrialReasonsReferenceDataList = referenceDataQueryService.retrieveModeOfTrialReasons();

        for (final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList) {
            final ReferenceDataVO referenceDataVO = defendantsWithReferenceData.getReferenceDataVO();

            // Get offences that have allocation decisions with motReasonId
            final List<UUID> offenceMotReasonIds = defendantsWithReferenceData.getDefendants().stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> nonNull(offence.getAllocationDecision()) && nonNull(offence.getAllocationDecision().getMotReasonId()))
                    .map(offence -> offence.getAllocationDecision().getMotReasonId())
                    .toList();

            // Find matching reference data by ID
            final List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceData = modeOfTrialReasonsReferenceDataList.stream()
                    .filter(modeOfTrialReasonsReferenceData -> offenceMotReasonIds.contains(UUID.fromString(modeOfTrialReasonsReferenceData.getId())))
                    .toList();
            referenceDataVO.getModeOfTrialReasonsReferenceData().addAll(modeOfTrialReferenceData);

            // Get offences that don't have allocation decisions or don't have motReasonId
            final List<MigratedOffence> offencesWithoutAllocationDecision = defendantsWithReferenceData.getDefendants().stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> isNull(offence.getAllocationDecision()) || isNull(offence.getAllocationDecision().getMotReasonId()))
                    .toList();
            if (!offencesWithoutAllocationDecision.isEmpty()) {
                final List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceDataWithSummaryOnlyOffence = modeOfTrialReasonsReferenceDataList.stream()
                        .filter(modeOfTrialReasonsReferenceData -> modeOfTrialReasonsReferenceData.getDescription().equals(SUMMARY_ONLY_OFFENCE))
                        .toList();

                referenceDataVO.getModeOfTrialReasonsReferenceData().addAll(modeOfTrialReferenceDataWithSummaryOnlyOffence);
            }

        }
    }
}
