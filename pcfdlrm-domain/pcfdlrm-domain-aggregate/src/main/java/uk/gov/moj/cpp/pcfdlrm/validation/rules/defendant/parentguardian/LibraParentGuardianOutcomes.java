package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static java.util.Map.entry;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_GUARDIAN_ORGANISATION_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_GUARDIAN_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ADDRESS_LINE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_GENDER_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_HOME_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_WORK_TELEPHONE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS2;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS3;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS4;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ADDRESS5;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_COMPANY_TELEPHONE_NUMBER;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_HOME_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_MOBILE_TELEPHONE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS2;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS3;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS4;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_ADDRESS5;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_ORGANISATION_POST_CODE;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName.PARENT_GUARDIAN_WORK_TELEPHONE;

import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;

import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class LibraParentGuardianOutcomes {

    public enum Outcome {
        REJECT,
        NULL,
        DEFAULT
    }

    private static final Map<String, Outcome> OUTCOMES = Map.ofEntries(
            entry(PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID.name(), Outcome.REJECT),
            entry(INVALID_GUARDIAN_POST_CODE.name(), Outcome.REJECT),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID.name(), Outcome.REJECT),
            entry(PARENT_GUARDIAN_WORK_TELEPHONE_INVALID.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_HOME_TELEPHONE_INVALID.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID.name(), Outcome.NULL),
            entry(DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID.name(), Outcome.NULL),
            entry(DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID.name(), Outcome.NULL),
            entry(DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE.name(), Outcome.NULL),
            entry(DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID.name(), Outcome.NULL),
            entry(DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_ADDRESS_LINE_INVALID.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID.name(), Outcome.NULL),
            entry(INVALID_GUARDIAN_ORGANISATION_POST_CODE.name(), Outcome.NULL),
            entry(PARENT_GUARDIAN_GENDER_INVALID.name(), Outcome.DEFAULT));

    private static final Map<String, UnaryOperator<ParentGuardianInformation>> NULLING_FUNCTIONS = Map.ofEntries(
            entry(PARENT_GUARDIAN_WORK_TELEPHONE.getValue(), contactDetailsField(builder -> builder.withWork(null))),
            entry(PARENT_GUARDIAN_HOME_TELEPHONE.getValue(), contactDetailsField(builder -> builder.withHome(null))),
            entry(PARENT_GUARDIAN_MOBILE_TELEPHONE.getValue(), contactDetailsField(builder -> builder.withMobile(null))),
            entry(PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS.getValue(), contactDetailsField(builder -> builder.withPrimaryEmail(null))),
            entry(PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS.getValue(), contactDetailsField(builder -> builder.withSecondaryEmail(null))),
            entry(PARENT_GUARDIAN_DATE_OF_BIRTH.getValue(), guardianField(builder -> builder.withDateOfBirth(null))),
            entry(PARENT_GUARDIAN_OBSERVED_ETHNICITY.getValue(), guardianField(builder -> builder.withObservedEthnicity(null))),
            entry(PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY.getValue(), guardianField(builder -> builder.withSelfDefinedEthnicity(null))),
            entry(PARENT_GUARDIAN_ADDRESS2.getValue(), individualAddressField(builder -> builder.withAddress2(null))),
            entry(PARENT_GUARDIAN_ADDRESS3.getValue(), individualAddressField(builder -> builder.withAddress3(null))),
            entry(PARENT_GUARDIAN_ADDRESS4.getValue(), individualAddressField(builder -> builder.withAddress4(null))),
            entry(PARENT_GUARDIAN_ADDRESS5.getValue(), individualAddressField(builder -> builder.withAddress5(null))),
            entry(PARENT_GUARDIAN_COMPANY_TELEPHONE_NUMBER.getValue(), guardianField(builder -> builder.withCompanyTelephoneNumber(null))),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS2.getValue(), organisationAddressField(builder -> builder.withAddress2(null))),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS3.getValue(), organisationAddressField(builder -> builder.withAddress3(null))),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS4.getValue(), organisationAddressField(builder -> builder.withAddress4(null))),
            entry(PARENT_GUARDIAN_ORGANISATION_ADDRESS5.getValue(), organisationAddressField(builder -> builder.withAddress5(null))),
            entry(PARENT_GUARDIAN_ORGANISATION_POST_CODE.getValue(), organisationAddressField(builder -> builder.withPostcode(null))));

    private LibraParentGuardianOutcomes() {
    }

    public static Optional<Outcome> outcomeOf(final ProblemCode problemCode) {
        return Optional.ofNullable(OUTCOMES.get(problemCode.name()));
    }

    public static boolean isReject(final String problemCode) {
        return problemCode != null && OUTCOMES.get(problemCode) == Outcome.REJECT;
    }

    static boolean isNull(final String problemCode) {
        return problemCode != null && OUTCOMES.get(problemCode) == Outcome.NULL;
    }

    static Optional<UnaryOperator<ParentGuardianInformation>> nullingFunctionFor(final String fieldKey) {
        return Optional.ofNullable(NULLING_FUNCTIONS.get(fieldKey));
    }

    private static UnaryOperator<ParentGuardianInformation> guardianField(final UnaryOperator<ParentGuardianInformation.Builder> nulling) {
        return guardian -> nulling.apply(ParentGuardianInformation.parentGuardianInformation().withValuesFrom(guardian)).build();
    }

    private static UnaryOperator<ParentGuardianInformation> organisationAddressField(final UnaryOperator<Address.Builder> nulling) {
        return guardian -> guardian.getAddress() == null ? guardian : ParentGuardianInformation.parentGuardianInformation()
                .withValuesFrom(guardian)
                .withAddress(nulling.apply(Address.address().withValuesFrom(guardian.getAddress())).build())
                .build();
    }

    private static UnaryOperator<ParentGuardianInformation> individualAddressField(final UnaryOperator<Address.Builder> nulling) {
        return personalInformationField(personalInformation -> personalInformation.getAddress() == null ? personalInformation : PersonalInformation.personalInformation()
                .withValuesFrom(personalInformation)
                .withAddress(nulling.apply(Address.address().withValuesFrom(personalInformation.getAddress())).build())
                .build());
    }

    private static UnaryOperator<ParentGuardianInformation> contactDetailsField(final UnaryOperator<ContactDetails.Builder> nulling) {
        return personalInformationField(personalInformation -> personalInformation.getContactDetails() == null ? personalInformation : PersonalInformation.personalInformation()
                .withValuesFrom(personalInformation)
                .withContactDetails(nulling.apply(ContactDetails.contactDetails().withValuesFrom(personalInformation.getContactDetails())).build())
                .build());
    }

    private static UnaryOperator<ParentGuardianInformation> personalInformationField(final UnaryOperator<PersonalInformation> change) {
        return guardian -> guardian.getPersonalInformation() == null ? guardian : ParentGuardianInformation.parentGuardianInformation()
                .withValuesFrom(guardian)
                .withPersonalInformation(change.apply(guardian.getPersonalInformation()))
                .build();
    }
}
