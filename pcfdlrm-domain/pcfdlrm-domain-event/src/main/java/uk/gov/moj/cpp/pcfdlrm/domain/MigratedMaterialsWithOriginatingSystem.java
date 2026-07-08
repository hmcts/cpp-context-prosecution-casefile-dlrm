package uk.gov.moj.cpp.pcfdlrm.domain;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Domain class that contains migrated materials along with the originating system information
 * for validation purposes.
 */
public class MigratedMaterialsWithOriginatingSystem {

    final private List<MigratedMaterial> materials;
    final private String migrationSourceSystemName;
    final private Map<String, ImmutablePair<String, String>> sections;
    final private int defendantCount;


    public MigratedMaterialsWithOriginatingSystem(final List<MigratedMaterial> materials, final String migrationSourceSystemName, final Map<String, ImmutablePair<String, String>> sections, final int defendantCount) {
        this.materials = materials;
        this.migrationSourceSystemName = migrationSourceSystemName;
        this.sections = sections;
        this.defendantCount = defendantCount;
    }

    public List<MigratedMaterial> getMaterials() {
        return materials;
    }

    public String getMigrationSourceSystemName() {
        return migrationSourceSystemName;
    }

    public Map<String, ImmutablePair<String, String>> getSections() {
        return sections;
    }

    public int getDefendantCount() {
        return defendantCount;
    }
}
