package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static uk.gov.justice.core.courts.Gender.valueOf;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.PCFEnumMap.getLanguageToDocumentationLanguageNeeds;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.PCFEnumMap.getLanguageToHearingLanguageNeeds;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.DriverLicenseCode;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.DriverLicenseCodeType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class ProsecutionMigrationCaseToCCPersonDefendantConverter implements ParameterisedConverter<MigratedDefendant, PersonDefendant, ReferenceDataVO> {

    @Override
    public PersonDefendant convert(final MigratedDefendant defendant, final ReferenceDataVO referenceDataVO) {

        return personDefendant()
                .withBailStatus(getBailStatus(defendant.getIndividual(), referenceDataVO))
                .withBailConditions(defendant.getIndividual().getBailConditions())
                .withArrestSummonsNumber(defendant.getAsn())
                .withDriverLicenseIssue(defendant.getIndividual().getDriverLicenceIssue())
                .withDriverLicenceCode(getDriverLicenseCode(defendant.getIndividual().getDriverLicenceCode()))
                .withDriverNumber(defendant.getIndividual().getDriverNumber())
                .withPerceivedBirthYear(null != defendant.getIndividual().getPerceivedBirthYear() ? tryParse(defendant.getIndividual().getPerceivedBirthYear()) : null)
                .withPersonDetails(buildPersonDetails(defendant, referenceDataVO))
                .withVehicleOperatorLicenceNumber(null)
                .build();
    }

    private DriverLicenseCode getDriverLicenseCode(final String driverLicenceCode) {

        return Optional.ofNullable(driverLicenceCode)
                .flatMap(DriverLicenseCodeType::valueFor)
                .orElse(null);

    }

    private BailStatus getBailStatus(final Individual individual, final ReferenceDataVO referenceDataVO) {
        final String custodyStatus = individual.getCustodyStatus();
        final LocalDate custodyTimeLimit = individual.getCustodyTimeLimit();
        final BailStatus.Builder bailStatusBuilder = BailStatus.bailStatus();

        BailStatus bailStatus = referenceDataVO.getBailStatusReferenceData()
                .stream().filter(bs -> bs.getStatusCode().equals(custodyStatus))
                .map(bailStatusReferenceData ->
                        BailStatus.bailStatus()
                                .withId(bailStatusReferenceData.getId())
                                .withCode(bailStatusReferenceData.getStatusCode())
                                .withDescription(bailStatusReferenceData.getStatusDescription())
                                .build())
                .findAny().orElse(null);
        if (nonNull(bailStatus)) {
            bailStatusBuilder.withValuesFrom(bailStatus);
            if ("C".equals(bailStatus.getCode()) && nonNull(custodyTimeLimit)) {
                bailStatusBuilder
                        .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                .withTimeLimit(custodyTimeLimit.toString())
                                .build())
                        .build();
            }
        }
        return nonNull(bailStatus) ? bailStatusBuilder.build() : null;
    }

    @SuppressWarnings({"squid:S1067"})
    private Person buildPersonDetails(final MigratedDefendant defendant, final ReferenceDataVO referenceDataVO) {
        final Person.Builder personBuilder = Person.person()
                .withAdditionalNationalityCode(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality())
                .withAdditionalNationalityId(getNationalityId(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality(), referenceDataVO))
                .withAdditionalNationalityDescription(getNationalityDescription(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality(), referenceDataVO))
                .withNationalInsuranceNumber(defendant.getIndividual().getNationalInsuranceNumber())
                .withAddress(buildAddress(defendant))
                .withContact(buildContactNumber(defendant))
                .withDateOfBirth(null != defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth() ? defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth().toString() : null)
                .withDocumentationLanguageNeeds(getDocumentationLanguageNeeds(defendant))
                .withHearingLanguageNeeds(getHearingLanguageNeeds(defendant))
                .withEthnicity(buildEthnicity(defendant.getIndividual().getSelfDefinedInformation().getEthnicity(), defendant.getIndividual().getPersonalInformation().getObservedEthnicity(), referenceDataVO))
                .withFirstName(defendant.getIndividual().getPersonalInformation().getFirstName())
                .withGender(valueOf(defendant.getIndividual().getSelfDefinedInformation().getGender()))
                .withInterpreterLanguageNeeds(defendant.getLanguageRequirement())
                .withLastName(defendant.getIndividual().getPersonalInformation().getLastName())
                .withMiddleName(buildMiddleName(defendant))
                .withOccupation(defendant.getIndividual().getPersonalInformation().getOccupation())
                .withOccupationCode(null != defendant.getIndividual().getPersonalInformation().getOccupationCode() ? String.valueOf(defendant.getIndividual().getPersonalInformation().getOccupationCode()) : null)
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withTitle(defendant.getIndividual().getPersonalInformation().getTitle());
        if (defendant.getIndividual() != null) {
            final String nationality = defendant.getIndividual().getSelfDefinedInformation().getNationality();
            if (!isEmpty(nationality)) {
                personBuilder.withNationalityCode(defendant.getIndividual().getSelfDefinedInformation().getNationality())
                        .withNationalityId(getNationalityId(nationality, referenceDataVO))
                        .withNationalityDescription(getNationalityDescription(nationality, referenceDataVO));
            }
        }
        return personBuilder.build();
    }

    private String buildMiddleName(final MigratedDefendant defendant) {
        if (null == defendant.getIndividual().getPersonalInformation().getGivenName2() && null == defendant.getIndividual().getPersonalInformation().getGivenName3()) {
            return null;
        }

        return join(defendant.getIndividual().getPersonalInformation().getGivenName2(), " ", (defendant.getIndividual().getPersonalInformation().getGivenName3()));
    }

    private Ethnicity buildEthnicity(final String selfDefinedEthnicity, final Integer observedEthnicity,
                                     final ReferenceDataVO referenceDataVO) {

        Optional<ObservedEthnicityReferenceData> optObservedEthnicityReferenceData = Optional.empty();
        Optional<SelfdefinedEthnicityReferenceData> optSelfDefinedEthnicityReferenceData = Optional.empty();


        if (null != observedEthnicity) {
            optObservedEthnicityReferenceData = referenceDataVO.getObservedEthnicityReferenceData().stream()
                    .filter(s -> tryParse(s.getEthnicityCode()).intValue() == (observedEthnicity)).findFirst();
        }

        if (null != selfDefinedEthnicity) {
            optSelfDefinedEthnicityReferenceData = referenceDataVO.getSelfdefinedEthnicityReferenceData().stream()
                    .filter(s -> s.getCode().equals(selfDefinedEthnicity)).findFirst();
        }

        return buildEthnicityWithOptionalFields(optObservedEthnicityReferenceData, optSelfDefinedEthnicityReferenceData);

    }

    private Ethnicity buildEthnicityWithOptionalFields(final Optional<ObservedEthnicityReferenceData> optObservedEthnicityReferenceData, final Optional<SelfdefinedEthnicityReferenceData> optSelfDefinedEthnicityReferenceData) {
        if (!optObservedEthnicityReferenceData.isPresent() && !optSelfDefinedEthnicityReferenceData.isPresent()) {
            return null;
        }
        return Ethnicity.ethnicity()
                .withObservedEthnicityCode(optObservedEthnicityReferenceData.isPresent() ? optObservedEthnicityReferenceData.get().getEthnicityCode() : null)
                .withObservedEthnicityDescription(optObservedEthnicityReferenceData.isPresent() ? optObservedEthnicityReferenceData.get().getEthnicityDescription() : null)
                .withObservedEthnicityId(optObservedEthnicityReferenceData.isPresent() ? optObservedEthnicityReferenceData.get().getId() : null)
                .withSelfDefinedEthnicityCode(optSelfDefinedEthnicityReferenceData.isPresent() ? optSelfDefinedEthnicityReferenceData.get().getCode() : null)
                .withSelfDefinedEthnicityDescription(optSelfDefinedEthnicityReferenceData.isPresent() ? optSelfDefinedEthnicityReferenceData.get().getDescription() : null)
                .withSelfDefinedEthnicityId(optSelfDefinedEthnicityReferenceData.isPresent() ? optSelfDefinedEthnicityReferenceData.get().getId() : null)
                .build();
    }


    private UUID getNationalityId(final String nationality, final ReferenceDataVO referenceDataVO) {
        if (null == nationality) {
            return null;
        }

        final Optional<ReferenceDataCountryNationality> referenceDataCountryNationality = referenceDataVO.getCountryNationalityReferenceData().stream()
                .filter(s -> isNationalityMatch(nationality, s)).findFirst();

        return referenceDataCountryNationality.map(dataCountryNationality -> fromString(dataCountryNationality.getId())).orElse(null);

    }

    private String getNationalityDescription(final String nationality, final ReferenceDataVO referenceDataVO) {
        if (null == nationality) {
            return null;
        }

        final Optional<ReferenceDataCountryNationality> referenceDataCountryNationality = referenceDataVO.getCountryNationalityReferenceData().stream()
                .filter(s -> isNationalityMatch(nationality, s)).findFirst();

        return referenceDataCountryNationality.map(ReferenceDataCountryNationality::getNationality).orElse(null);

    }

    private boolean isNationalityMatch(final String nationality, final ReferenceDataCountryNationality referenceDataCountryNationality) {
        if (nationality.equals(referenceDataCountryNationality.getIsoCode())) {
            return true;
        }

        return (null != referenceDataCountryNationality.getCjsCode() && nationality.equals(referenceDataCountryNationality.getCjsCode()));
    }

    private HearingLanguage getDocumentationLanguageNeeds(final MigratedDefendant defendant) {
        return getLanguageToDocumentationLanguageNeeds().get(Language.valueOf(defendant.getDocumentationLanguage()));
    }

    private HearingLanguage getHearingLanguageNeeds(final MigratedDefendant defendant) {
        return getLanguageToHearingLanguageNeeds().get(Language.valueOf(defendant.getHearingLanguage()));
    }

    private ContactNumber buildContactNumber(final MigratedDefendant defendant) {

        final ContactDetails contactDetails = defendant.getIndividual().getPersonalInformation().getContactDetails();
        if ((contactDetails == null) || (isEmpty(contactDetails.getHome()) && isEmpty(contactDetails.getWork())
                && isEmpty(contactDetails.getMobile()) && isEmpty(contactDetails.getPrimaryEmail()) && isEmpty(contactDetails.getSecondaryEmail()))) {
            return null;
        } else {
            return ContactNumber.contactNumber()
                    .withHome(contactDetails.getHome())
                    .withMobile(contactDetails.getMobile())
                    .withPrimaryEmail(contactDetails.getPrimaryEmail())
                    .withSecondaryEmail(contactDetails.getSecondaryEmail())
                    .withWork(contactDetails.getWork())
                    .build();
        }
    }

    private Address buildAddress(final MigratedDefendant defendant) {
        return Address.address().withAddress1(defendant.getIndividual().getPersonalInformation().getAddress().getAddress1())
                .withAddress2(defendant.getIndividual().getPersonalInformation().getAddress().getAddress2())
                .withAddress3(defendant.getIndividual().getPersonalInformation().getAddress().getAddress3())
                .withAddress4(defendant.getIndividual().getPersonalInformation().getAddress().getAddress4())
                .withAddress5(defendant.getIndividual().getPersonalInformation().getAddress().getAddress5())
                .withPostcode(defendant.getIndividual().getPersonalInformation().getAddress().getPostcode())
                .build();
    }

    public Integer tryParse(final String text) {
        try {
            return Integer.parseInt(text);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
