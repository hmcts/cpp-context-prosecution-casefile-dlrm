package uk.gov.moj.cpp.pcfdlrm.domain;


import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

@SuppressWarnings("squid:S2384")
public class DefendantWithReferenceData {

    private  MigratedDefendant defendant;

    public void setDefendant(final MigratedDefendant defendant) {
        this.defendant = defendant;
    }

    private final ReferenceDataVO referenceDataVO;
    private final CaseDetails caseDetails;


    public DefendantWithReferenceData(final MigratedDefendant defendant, ReferenceDataVO referenceDataVO, CaseDetails caseDetails) {
        this.defendant = defendant;
        this.referenceDataVO = referenceDataVO;
        this.caseDetails = caseDetails;
    }

    public MigratedDefendant getDefendant() {
        return defendant;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

}
