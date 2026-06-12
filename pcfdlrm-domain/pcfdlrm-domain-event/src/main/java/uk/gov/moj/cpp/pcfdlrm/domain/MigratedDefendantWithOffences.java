package uk.gov.moj.cpp.pcfdlrm.domain;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class MigratedDefendantWithOffences implements Serializable {

    private final MigratedDefendant defendant;
    private final List<UUID> offenceids;

    public MigratedDefendantWithOffences(final MigratedDefendant defendant, final List<UUID> offenceids) {
        this.defendant = defendant;
        this.offenceids = offenceids;
    }

    public MigratedDefendant getDefendant() {
        return defendant;
    }

    public List<UUID> getOffenceids() {
        return offenceids;
    }
}
