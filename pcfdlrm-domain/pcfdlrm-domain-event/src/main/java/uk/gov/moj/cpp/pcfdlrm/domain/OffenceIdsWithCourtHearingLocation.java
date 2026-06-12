package uk.gov.moj.cpp.pcfdlrm.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class OffenceIdsWithCourtHearingLocation implements Serializable {
    
    private List<UUID> offenceIds;
    private String courtHearingLocation;
    
    public OffenceIdsWithCourtHearingLocation(List<UUID> offenceIds, String courtHearingLocation) {
        this.offenceIds = offenceIds;
        this.courtHearingLocation = courtHearingLocation;
    }
    
    public List<UUID> getOffenceIds() {
        return offenceIds;
    }
    
    public void setOffenceIds(List<UUID> offenceIds) {
        this.offenceIds = offenceIds;
    }
    
    public String getCourtHearingLocation() {
        return courtHearingLocation;
    }
    
    public void setCourtHearingLocation(String courtHearingLocation) {
        this.courtHearingLocation = courtHearingLocation;
    }
} 