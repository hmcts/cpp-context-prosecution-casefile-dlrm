package uk.gov.moj.cpp.pcfdlrm.domain;


import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.List;


@SuppressWarnings("squid:S2384")
public class DefendantsWithReferenceData {

    private String prosecutionAuthorityShortName;
    private List<MigratedDefendant> defendants;
    private CaseDetails caseDetails;
    private ReferenceDataVO referenceDataVO = new ReferenceDataVO();

    private  String migrationSourceSystemName;

    public DefendantsWithReferenceData(final List<MigratedDefendant> defendants) {
        this.defendants = defendants;
    }

    public DefendantsWithReferenceData(final List<MigratedDefendant> defendants, final String prosecutionAuthorityShortName) {
        this.defendants = defendants;
        this.prosecutionAuthorityShortName = prosecutionAuthorityShortName;
    }

    public String getProsecutionAuthorityShortName() {
        return prosecutionAuthorityShortName;
    }

    public List<MigratedDefendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(final List<MigratedDefendant> defendants) {
        this.defendants = defendants;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final CaseDetails caseDetails) {
        this.caseDetails = caseDetails;
    }

    public String getMigrationSourceSystemName() {
        return migrationSourceSystemName;
    }

    public void setMigrationSourceSystemName(final String migrationSourceSystemName) {
        this.migrationSourceSystemName = migrationSourceSystemName;
    }
}
