package uk.gov.moj.cpp.pcfdlrm.domain;

import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ParamsVO {
    private ReferenceDataVO referenceDataVO;
    private UUID caseId;
    private Channel channel;
    private String oucodeL1Code;
    private String receivedFromCourtOUCode;
    private String initiationCode;
    private String migrationSourceSystemName;
    private List<OffenceIdsWithCourtHearingLocation> offenceIdsWithCourtHearingLocationList = new ArrayList<>();

    private SummonsApprovedOutcome summonsApprovedOutcome;
    private LocalDate custodyTimeLimit;
    private String custodyStatus;

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public String getOucodeL1Code() {
        return oucodeL1Code;
    }

    public void setOucodeL1Code(final String oucodeL1Code) {
        this.oucodeL1Code = oucodeL1Code;
    }

    public String getReceivedFromCourtOUCode() {
        return receivedFromCourtOUCode;
    }

    public void setReceivedFromCourtOUCode(final String receivedFromCourtOUCode) {
        this.receivedFromCourtOUCode = receivedFromCourtOUCode;
    }

    public String getInitiationCode() {
        return initiationCode;
    }

    public void setInitiationCode(final String initiationCode) {
        this.initiationCode = initiationCode;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    public void setSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.summonsApprovedOutcome = summonsApprovedOutcome;
    }

    public String getMigrationSourceSystemName() {
        return migrationSourceSystemName;
    }

    public void setMigrationSourceSystemName(final String migrationSourceSystemName) {
        this.migrationSourceSystemName = migrationSourceSystemName;
    }

    public List<OffenceIdsWithCourtHearingLocation> getOffenceIdsWithCourtHearingLocationList() {
        return offenceIdsWithCourtHearingLocationList;
    }

    public void addOffenceIdsWithCourtHearingLocation(OffenceIdsWithCourtHearingLocation offenceIdsWithCourtHearingLocation) {
        this.offenceIdsWithCourtHearingLocationList.add(offenceIdsWithCourtHearingLocation);
    }

    public LocalDate getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public void setCustodyTimeLimit(final LocalDate custodyTimeLimit) {
        this.custodyTimeLimit = custodyTimeLimit;
    }

    public String getCustodyStatus() {
        return custodyStatus;
    }

    public void setCustodyStatus(final String custodyStatus) {
        this.custodyStatus = custodyStatus;
    }
}
