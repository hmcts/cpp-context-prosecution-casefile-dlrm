package uk.gov.moj.cpp.pcfdlrm.builder;

import static java.util.UUID.fromString;

import java.util.UUID;

public class TestConstants {

    public static final UUID DEFENDANT_ID = fromString("9824e40f-0289-4221-8854-346eb28c8f27");
    public static final UUID DEFENDANT_ID2 = fromString("9924e40f-0289-4221-8854-346eb28c8f27");
    public static final String SOURCE_SYSTEM_XHIBIT = "XHIBIT";
    public static final String SOURCE_SYSTEM_XHIBIT_IDENDIFIER = "XHIBIT-123";
    public static final UUID CASE_ID = fromString("a4391799-f828-4515-a355-61f1d5d9690c");
    public static final UUID SUBMISSION_ID = fromString("e3e3e3e3-3333-4333-8333-333333333333");

    public static final String PG = "individual_parentGuardianInformation";
    public static final String PG_WORK = PG + "_personalInformation_contactDetails_work";
    public static final String PG_HOME = PG + "_personalInformation_contactDetails_home";
    public static final String PG_MOBILE = PG + "_personalInformation_contactDetails_mobile";
    public static final String PG_PRIMARY_EMAIL = PG + "_personalInformation_contactDetails_primaryEmail";
    public static final String PG_SECONDARY_EMAIL = PG + "_personalInformation_contactDetails_secondaryEmail";
    public static final String PG_DATE_OF_BIRTH = PG + "_dateOfBirth";
    public static final String PG_OBSERVED_ETHNICITY = PG + "_observedEthnicity";
    public static final String PG_SELF_DEFINED_ETHNICITY = PG + "_selfDefinedEthnicity";
    public static final String PG_GENDER = PG + "_gender";
    public static final String PG_POSTCODE = PG + "_personalInformation_address_postcode";
    public static final String PG_COMPANY_TELEPHONE = PG + "_companyTelephoneNumber";
    public static final String PG_ORG_POSTCODE = PG + "_address_postcode";

    public static final String LIBRA = "LIBRA";

    private TestConstants() {
    }

    /** Individual guardian address line key, {@code line} 1–5. */
    public static String pgAddressKey(final int line) {
        return PG + "_personalInformation_address_address" + line;
    }

    /** Organisation guardian address line key, {@code line} 1–5. */
    public static String pgOrgAddressKey(final int line) {
        return PG + "_address_address" + line;
    }
}
