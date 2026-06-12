package uk.gov.moj.cpp.pcfdlrm.refdata.proscase;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

public class CourtLocationEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {

        prosecutionWithReferenceDataList.forEach(e -> {
                    final List<OrganisationUnitReferenceData> sendingCourts = Optional.ofNullable(e.getProsecution().getCaseDetails().getSendingCourt())
                            .map(referenceDataQueryService::retrieveOrganisationUnits)
                            .orElse(List.of());

                    if (isNotEmpty(sendingCourts)) {
                        e.getReferenceDataVO().setSendingCourtOrganisationUnit(sendingCourts.get(0));
                    }

                    final List<OrganisationUnitReferenceData> receivingCourts = Optional.ofNullable(e.getProsecution().getCaseDetails().getReceivingCourt())
                            .map(referenceDataQueryService::retrieveOrganisationUnits)
                            .orElse(List.of());

                    if (isNotEmpty(receivingCourts)) {
                        e.getReferenceDataVO().setReceivingCourtOrganisationUnit(receivingCourts.get(0));
                    }
                });
    }
}
