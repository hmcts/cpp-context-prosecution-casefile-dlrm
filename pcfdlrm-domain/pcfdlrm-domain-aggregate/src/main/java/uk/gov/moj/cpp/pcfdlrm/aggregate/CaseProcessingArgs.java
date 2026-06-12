package uk.gov.moj.cpp.pcfdlrm.aggregate;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class CaseProcessingArgs {

    private final ReceiveMigratedCaseFile receiveMigratedCaseFile;
    private final ProsecutionWithReferenceData receivedProsecutionWithReferenceData;
    private final List<CaseRefDataEnricher> caseRefDataEnrichers;
    private final List<DefendantRefDataEnricher> defendantRefDataEnrichers;
    private final ReferenceDataQueryService referenceDataQueryService;
    private final Map<String, ImmutablePair<String, String>> sections;
    private final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList;
    private final List<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers;

    public CaseProcessingArgs(
            ReceiveMigratedCaseFile receiveMigratedCaseFile,
            ProsecutionWithReferenceData receivedProsecutionWithReferenceData,
            List<CaseRefDataEnricher> caseRefDataEnrichers,
            List<DefendantRefDataEnricher> defendantRefDataEnrichers,
            ReferenceDataQueryService referenceDataQueryService,
            Map<String, ImmutablePair<String, String>> sections,
            List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList,
            List<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers
    ) {
        this.receiveMigratedCaseFile = receiveMigratedCaseFile;
        this.receivedProsecutionWithReferenceData = receivedProsecutionWithReferenceData;
        this.caseRefDataEnrichers = caseRefDataEnrichers;
        this.defendantRefDataEnrichers = defendantRefDataEnrichers;
        this.referenceDataQueryService = referenceDataQueryService;
        this.sections = sections;
        this.documentMetadataReferenceDataList = documentMetadataReferenceDataList;
        this.migratedHearingRefDataEnrichers = migratedHearingRefDataEnrichers;
    }

    public ReceiveMigratedCaseFile getReceiveMigratedCaseFile() {
        return receiveMigratedCaseFile;
    }

    public ProsecutionWithReferenceData getReceivedProsecutionWithReferenceData() {
        return receivedProsecutionWithReferenceData;
    }

    public List<CaseRefDataEnricher> getCaseRefDataEnrichers() {
        return caseRefDataEnrichers;
    }

    public List<DefendantRefDataEnricher> getDefendantRefDataEnrichers() {
        return defendantRefDataEnrichers;
    }

    public ReferenceDataQueryService getReferenceDataQueryService() {
        return referenceDataQueryService;
    }

    public Map<String, ImmutablePair<String, String>> getSections() {
        return sections;
    }

    public List<DocumentTypeAccessReferenceData> getDocumentMetadataReferenceDataList() {
        return documentMetadataReferenceDataList;
    }

    public List<MigratedHearingRefDataEnricher> getMigratedHearingRefDataEnrichers() {
        return migratedHearingRefDataEnrichers;
    }
}

