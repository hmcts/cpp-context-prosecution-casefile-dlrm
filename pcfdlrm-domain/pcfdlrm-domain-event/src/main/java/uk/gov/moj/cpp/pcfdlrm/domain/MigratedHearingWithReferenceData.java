package uk.gov.moj.cpp.pcfdlrm.domain;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MigratedHearingWithReferenceData implements Serializable {

    private CaseDetails caseDetails;

    private ReferenceDataVO referenceDataVO = new ReferenceDataVO();

    private MigratedHearing migratedHearing;

    private List<MigratedDefendantWithOffences> migratedDefendantWithOffences;

    public List<MigratedDefendantWithOffences> getMigratedDefendantWithOffences() {
        return migratedDefendantWithOffences;
    }

    public void setMigratedDefendantWithOffences(final List<MigratedDefendantWithOffences> migratedDefendantWithOffences) {
        this.migratedDefendantWithOffences = migratedDefendantWithOffences;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final CaseDetails caseDetails) {
        this.caseDetails = caseDetails;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public MigratedHearing getMigratedHearing() {
        return migratedHearing;
    }

    public void setMigratedHearing(final MigratedHearing migratedHearing) {
        this.migratedHearing = migratedHearing;
    }

    @JsonIgnore
    public List<MigratedDefendant> getDefendants() {
        return this.migratedDefendantWithOffences.stream().map(MigratedDefendantWithOffences::getDefendant).toList();
    }


}
