package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.SPI;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;


public class OffenceLocationHelper {

    private static final String DVLA = "DVLA";
    public static final String DEFAULT_OFFENCE_LOCATION = "No location provided";
    public static final String YES = "Y";

    private OffenceLocationHelper() {
    }

    public static String getOffenceLocation(final MigratedOffence offence, final String prosecutionAuthorityShortName) {
        if (DVLA.equals(prosecutionAuthorityShortName) && isBlank(offence.getOffenceLocation())) {
            return DEFAULT_OFFENCE_LOCATION;
        } else {
            return offence.getOffenceLocation();
        }
    }


    private static boolean isRequireOffenceLocation(final OffenceReferenceData offenceReferenceData) {
        return nonNull(offenceReferenceData) && YES.equals(offenceReferenceData.getLocationRequired());
    }

    public static String getOffenceLocation(final String offenceLocation, final Channel channel, final OffenceReferenceData offenceReferenceData) {
        if (isBlank(offenceLocation) && channel == SPI && isRequireOffenceLocation(offenceReferenceData)) {
            return DEFAULT_OFFENCE_LOCATION;
        } else {
            return offenceLocation;
        }
    }
}