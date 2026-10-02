package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * DD-43501: which of the two {@code oneOf} shapes a guardian block has. Blank strings count as
 * absent; if both shapes are populated (malformed data) INDIVIDUAL wins.
 */
public enum ParentGuardianShape {
    INDIVIDUAL,
    ORGANISATION,
    ABSENT;

    public static ParentGuardianShape of(final ParentGuardianInformation parentGuardianInformation) {
        if (parentGuardianInformation == null) {
            return ABSENT;
        }
        if (nonNull(parentGuardianInformation.getDateOfBirth())
                || anyNotBlank(parentGuardianInformation.getGender(), parentGuardianInformation.getSelfDefinedEthnicity(), parentGuardianInformation.getObservedEthnicity())
                || isPresent(parentGuardianInformation.getPersonalInformation())) {
            return INDIVIDUAL;
        }
        if (anyNotBlank(parentGuardianInformation.getOrganisationName(), parentGuardianInformation.getCompanyTelephoneNumber())
                || isPresent(parentGuardianInformation.getAddress())) {
            return ORGANISATION;
        }
        return ABSENT;
    }

    static ParentGuardianInformation parentGuardianOf(final MigratedDefendant defendant) {
        return Optional.ofNullable(defendant)
                .map(MigratedDefendant::getIndividual)
                .map(Individual::getParentGuardianInformation)
                .orElse(null);
    }

    private static boolean isPresent(final PersonalInformation personalInformation) {
        return nonNull(personalInformation)
                && (nonNull(personalInformation.getObservedEthnicity())
                || nonNull(personalInformation.getOccupationCode())
                || anyNotBlank(personalInformation.getTitle(), personalInformation.getFirstName(), personalInformation.getGivenName2(),
                personalInformation.getGivenName3(), personalInformation.getLastName(), personalInformation.getOccupation())
                || isPresent(personalInformation.getAddress())
                || isPresent(personalInformation.getContactDetails()));
    }

    private static boolean isPresent(final Address address) {
        return nonNull(address) && anyNotBlank(address.getAddress1(), address.getAddress2(), address.getAddress3(),
                address.getAddress4(), address.getAddress5(), address.getPostcode());
    }

    private static boolean isPresent(final ContactDetails contactDetails) {
        return nonNull(contactDetails) && anyNotBlank(contactDetails.getHome(), contactDetails.getWork(), contactDetails.getMobile(),
                contactDetails.getPrimaryEmail(), contactDetails.getSecondaryEmail());
    }

    private static boolean anyNotBlank(final String... values) {
        return Stream.of(values).anyMatch(StringUtils::isNotBlank);
    }
}
