package uk.gov.moj.cpp.pcfdlrm.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Event("pcfdlrm.events.migrated-case-validated-creation-pending")
public class MigratedCaseValidatedCreationPending implements Serializable {

    @Serial
    private static final long serialVersionUID = -2134598733437545928L;

    private final ReceiveMigratedCaseFile receiveMigratedCaseFile;

    @SuppressWarnings("squid:S1948")
    private final ProsecutionWithReferenceData prosecutionWithReferenceData;

    private final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList;

    public MigratedCaseValidatedCreationPending(final ReceiveMigratedCaseFile receiveMigratedCaseFile, ProsecutionWithReferenceData prosecutionWithReferenceData, List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList) {
        this.receiveMigratedCaseFile = receiveMigratedCaseFile;
        this.prosecutionWithReferenceData = prosecutionWithReferenceData;
        this.migratedHearingWithReferenceDataList = migratedHearingWithReferenceDataList;
    }

    public ReceiveMigratedCaseFile getReceiveMigratedCaseFile() {
        return receiveMigratedCaseFile;
    }

    public List<MigratedHearingWithReferenceData> getMigratedHearingWithReferenceDataList() {
        return migratedHearingWithReferenceDataList;
    }

    public ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return prosecutionWithReferenceData;
    }

    public static Builder migratedCaseValidatedCreationPending() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final MigratedCaseValidatedCreationPending that = (MigratedCaseValidatedCreationPending) obj;

        return java.util.Objects.equals(this.receiveMigratedCaseFile, that.receiveMigratedCaseFile) &&
                java.util.Objects.equals(this.prosecutionWithReferenceData, that.prosecutionWithReferenceData) &&
                java.util.Objects.equals(this.migratedHearingWithReferenceDataList, that.migratedHearingWithReferenceDataList) ;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(receiveMigratedCaseFile, prosecutionWithReferenceData, migratedHearingWithReferenceDataList);
    }

    @Override
    public String toString() {
        return "MigratedCaseSubmissionReceived{" +
                "CaseDetails='" + receiveMigratedCaseFile + "'," +
                "prosecutionWithReferenceData='" + prosecutionWithReferenceData + "'" +
                "migratedHearingWithReferenceDataList='" + migratedHearingWithReferenceDataList + "'" +
                "}";
    }

    public static class Builder {
        private ReceiveMigratedCaseFile receiveMigratedCaseFile;

        private  ProsecutionWithReferenceData prosecutionWithReferenceData;

        private  List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList;

        public Builder withMigratedCaseSubmission(final ReceiveMigratedCaseFile receiveMigratedCaseFile) {
            this.receiveMigratedCaseFile = receiveMigratedCaseFile;
            return this;
        }


        public Builder withProsecutionWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
            this.prosecutionWithReferenceData = prosecutionWithReferenceData;
            return this;
        }

        public Builder withMigratedHearingWithReferenceData(final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList) {
            this.migratedHearingWithReferenceDataList = migratedHearingWithReferenceDataList;
            return this;
        }

        public Builder withValuesFrom(final MigratedCaseValidatedCreationPending migratedCaseFileReceived) {
            this.receiveMigratedCaseFile = migratedCaseFileReceived.getReceiveMigratedCaseFile();
            this.prosecutionWithReferenceData = migratedCaseFileReceived.getProsecutionWithReferenceData();
            this.migratedHearingWithReferenceDataList = migratedCaseFileReceived.getMigratedHearingWithReferenceDataList();
            return this;
        }

        public MigratedCaseValidatedCreationPending build() {
            return new MigratedCaseValidatedCreationPending(receiveMigratedCaseFile, prosecutionWithReferenceData, migratedHearingWithReferenceDataList);
        }
    }
}

