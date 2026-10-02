package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.justice.core.courts.Gender.NOT_SPECIFIED;

import uk.gov.justice.core.courts.Gender;

import java.util.Map;
import java.util.Optional;

public final class LibraGenderCode {

    private static final Map<String, String> LIBRA_GENDER_CODES = Map.of(
            "0", NOT_KNOWN.name(),
            "1", MALE.name(),
            "2", FEMALE.name(),
            "9", NOT_SPECIFIED.name());

    private LibraGenderCode() {
    }

    public static Optional<String> normalise(final String gender) {
        if (isBlank(gender)) {
            return Optional.empty();
        }
        final String trimmedGender = gender.trim();
        final String libraGender = LIBRA_GENDER_CODES.get(trimmedGender);
        if (libraGender != null) {
            return Optional.of(libraGender);
        }
        return stream(Gender.values()).anyMatch(cpGender -> cpGender.name().equalsIgnoreCase(trimmedGender))
                ? Optional.of(trimmedGender)
                : Optional.empty();
    }
}
