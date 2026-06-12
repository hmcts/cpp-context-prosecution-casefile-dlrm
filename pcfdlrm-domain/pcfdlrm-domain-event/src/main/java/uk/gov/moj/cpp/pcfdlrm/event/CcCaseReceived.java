package uk.gov.moj.cpp.pcfdlrm.event;

import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;


import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("pcfdlrm.events.cc-case-received")
public class CcCaseReceived implements Serializable {
    private static final long serialVersionUID = 609898103244023485L;

    @SuppressWarnings("squid:S1948")
    private final ProsecutionWithReferenceData prosecutionWithReferenceData;
    @SuppressWarnings("squid:S1948")
    private final SummonsApprovedOutcome summonsApprovedOutcome;

    @JsonCreator
    public CcCaseReceived(final ProsecutionWithReferenceData prosecutionWithReferenceData, final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.prosecutionWithReferenceData = prosecutionWithReferenceData;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
    }

    public static Builder ccCaseReceived() {
        return new Builder();
    }

    public ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return prosecutionWithReferenceData;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CcCaseReceived that = (CcCaseReceived) o;
        return Objects.equals(getProsecutionWithReferenceData(), that.getProsecutionWithReferenceData()) && Objects.equals(getSummonsApprovedOutcome(), that.getSummonsApprovedOutcome());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProsecutionWithReferenceData(), getSummonsApprovedOutcome());
    }

    @Override
    public String toString() {
        return "CcCaseReceived{" +
                "prosecutionWithReferenceData=" + prosecutionWithReferenceData +
                ", summonsApprovedOutcome=" + summonsApprovedOutcome +
                '}';
    }

    public static class Builder {
        private ProsecutionWithReferenceData prosecutionWithReferenceData;
        private SummonsApprovedOutcome summonsApprovedOutcome;

        public Builder withProsecutionWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
            this.prosecutionWithReferenceData = prosecutionWithReferenceData;
            return this;
        }

        public Builder withSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
            this.summonsApprovedOutcome = summonsApprovedOutcome;
            return this;
        }

        public CcCaseReceived build() {
            return new CcCaseReceived(prosecutionWithReferenceData, summonsApprovedOutcome);
        }
    }
}
