package uk.gov.moj.cpp.pcfdlrm.event;


import uk.gov.justice.domain.annotation.Event;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Event("pcfdlrm.events.migrated-case-validated-with-warnings")
public class MigratedCaseValidatedWithWarnings implements Serializable {

    @Serial
    private static final long serialVersionUID = -4026125723466581174L;

    private final UUID caseId;

    private final String caseUrn;

    private final String type;

    private final String message;

    public MigratedCaseValidatedWithWarnings(final UUID caseId, final String caseUrn, final String type, final String message) {
        this.caseId = caseId;
        this.message = message;
        this.caseUrn = caseUrn;
        this.type = type;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getMessage() {
        return message;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getType() {
        return type;
    }

    public static Builder migratedCaseValidatedWithWarnings() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final MigratedCaseValidatedWithWarnings that = (MigratedCaseValidatedWithWarnings) obj;

        return Objects.equals(this.caseId, that.caseId) &&
                Objects.equals(this.caseUrn, that.caseUrn) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, caseUrn, type, message);
    }

    @Override
    public String toString() {
        return "MigratedCaseValidatedWithWarnings{" +
                "caseId=" + caseId +
                ", message='" + message + '\'' +
                ", caseUrn='" + caseUrn + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public static class Builder {

        private UUID caseId;

        private String message;

        private String caseUrn;

        private String type;

        public Builder withValuesFrom(final MigratedCaseValidatedWithWarnings migratedCaseFileReceivedwithWarnings) {
            this.caseId = migratedCaseFileReceivedwithWarnings.getCaseId();
            this.caseUrn = migratedCaseFileReceivedwithWarnings.getCaseUrn();
            this.type = migratedCaseFileReceivedwithWarnings.getType();
            this.message = migratedCaseFileReceivedwithWarnings.getMessage();
            return this;
        }

        public MigratedCaseValidatedWithWarnings build() {
            return new MigratedCaseValidatedWithWarnings(caseId, caseUrn, type, message);
        }

        public Builder withWarnings(final String message) {
            this.message = message;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withCaseUrn(final String caseUrn) {
            this.caseUrn = caseUrn;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }
    }
}

