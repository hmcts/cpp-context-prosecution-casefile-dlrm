package uk.gov.moj.cpp.pcfdlrm.event;


import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Event("pcfdlrm.events.migrated-case-file-received")
public class MigratedCaseFileReceived implements Serializable {

    @Serial
    private static final long serialVersionUID = -2134598733437545928L;

    private final ReceiveMigratedCaseFile receiveMigratedCaseFile;

    @SuppressWarnings("squid:S1948")
    private final ReferenceDataVO referenceDataVO;

    private final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList;

    public MigratedCaseFileReceived(final ReceiveMigratedCaseFile receiveMigratedCaseFile, ReferenceDataVO referenceDataVO, List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList) {
        this.receiveMigratedCaseFile = receiveMigratedCaseFile;
        this.referenceDataVO = referenceDataVO;
        this.migratedHearingWithReferenceDataList = migratedHearingWithReferenceDataList;
    }

    public ReceiveMigratedCaseFile getReceiveMigratedCaseFile() {
        return receiveMigratedCaseFile;
    }

    public List<MigratedHearingWithReferenceData> getMigratedHearingWithReferenceDataList() {
        return migratedHearingWithReferenceDataList;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public static Builder migratedCaseFileReceived() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final MigratedCaseFileReceived that = (MigratedCaseFileReceived) obj;

        return java.util.Objects.equals(this.receiveMigratedCaseFile, that.receiveMigratedCaseFile) &&
                java.util.Objects.equals(this.referenceDataVO, that.referenceDataVO) &&
                java.util.Objects.equals(this.migratedHearingWithReferenceDataList, that.migratedHearingWithReferenceDataList) ;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(receiveMigratedCaseFile, referenceDataVO, migratedHearingWithReferenceDataList);
    }

    @Override
    public String toString() {
        return "MigratedCaseSubmissionReceived{" +
                "CaseDetails='" + receiveMigratedCaseFile + "'," +
                "referenceDataVO='" + referenceDataVO + "'" +
                "migratedHearingWithReferenceDataList='" + migratedHearingWithReferenceDataList + "'" +
                "}";
    }

    public static class Builder {
        private ReceiveMigratedCaseFile receiveMigratedCaseFile;

        private  ReferenceDataVO referenceDataVO;

        private  List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList;

        public Builder withMigratedCaseSubmission(final ReceiveMigratedCaseFile receiveMigratedCaseFile) {
            this.receiveMigratedCaseFile = receiveMigratedCaseFile;
            return this;
        }


        public Builder withReferenceDataVO(final ReferenceDataVO referenceDataVO) {
            this.referenceDataVO = referenceDataVO;
            return this;
        }

        public Builder withMigratedHearingWithReferenceData(final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList) {
            this.migratedHearingWithReferenceDataList = migratedHearingWithReferenceDataList;
            return this;
        }

        public Builder withValuesFrom(final MigratedCaseFileReceived migratedCaseFileReceived) {
            this.receiveMigratedCaseFile = migratedCaseFileReceived.getReceiveMigratedCaseFile();
            this.referenceDataVO = migratedCaseFileReceived.getReferenceDataVO();
            this.migratedHearingWithReferenceDataList = migratedCaseFileReceived.getMigratedHearingWithReferenceDataList();
            return this;
        }

        public MigratedCaseFileReceived build() {
            return new MigratedCaseFileReceived(receiveMigratedCaseFile, referenceDataVO,migratedHearingWithReferenceDataList);
        }
    }
}

