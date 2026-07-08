package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantAlias.defendantAlias;
import static uk.gov.justice.core.courts.Ethnicity.ethnicity;
import static uk.gov.justice.core.courts.InitiationCode.valueFor;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCaseFileMigratedDefendantToCCDefendantConverter implements ParameterisedConverter<List<MigratedDefendant>, List<Defendant>, ParamsVO> {

    @Inject
    private ProsecutionMigrationCaseToCCPersonDefendantConverter prosecutionMigrationCaseToCCPersonDefendantConverter;

    @Inject
    private ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter;

    @Inject
    private ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter;

    @Override
    @SuppressWarnings("squid:S1188")
    public List<Defendant> convert(final List<MigratedDefendant> migratedDefendants, final ParamsVO paramsVO) {

        return migratedDefendants.stream()
                .map(defendant -> {
                    paramsVO.setCustodyTimeLimit(ofNullable(defendant)
                            .map(MigratedDefendant::getIndividual)
                            .map(Individual::getCustodyTimeLimit)
                            .orElse(null));
                    paramsVO.setCustodyStatus(ofNullable(defendant)
                            .map(MigratedDefendant::getIndividual)
                            .map(Individual::getCustodyStatus)
                            .orElse(null));
                            final Defendant.Builder builder = defendant()
                                    .withId(defendant.getId())
                                    .withMasterDefendantId(defendant.getId())
                                    .withInitiationCode(valueFor(defendant.getInitiationCode()).orElse(null))
                                    .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                                    .withCroNumber(defendant.getCroNumber())
                                    .withOffences(prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter.convert(defendant.getOffences(), paramsVO))
                                    .withPersonDefendant(defendant.getIndividual() != null ? prosecutionMigrationCaseToCCPersonDefendantConverter.convert(defendant, paramsVO.getReferenceDataVO()) : null)
                                    .withPncId(defendant.getPncIdentifier())
                                    .withCroNumber(defendant.getCroNumber())
                                    .withProsecutionAuthorityReference(defendant.getProsecutorDefendantReference())
                                    .withProsecutionCaseId(paramsVO.getCaseId())
                                    .withLegalEntityDefendant(prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter.convert(defendant))
                                    .withAssociatedPersons(defendant.getIndividual() != null ? buildAssociatedPersons(defendant.getIndividual().getParentGuardianInformation(), paramsVO.getReferenceDataVO()) : null);

                            if (isNotEmpty(defendant.getIndividualAliases())) {
                                builder.withAliases(buildIndividualAliases(defendant.getIndividualAliases()));
                            } else if (isNotEmpty(defendant.getAliasForCorporate())) {
                                builder.withAliases(buildCorporateAliases(defendant.getAliasForCorporate()));
                            }
                            return builder.build();
                        }
                )
                .toList();
    }

    @SuppressWarnings("squid:S1168")
    private List<AssociatedPerson> buildAssociatedPersons(final ParentGuardianInformation parentGuardianInformation, final ReferenceDataVO referenceDataVO) {

        if (nonNull(parentGuardianInformation) && StringUtils.isEmpty(parentGuardianInformation.getOrganisationName())) {
            final AssociatedPerson associatedPerson = associatedPerson()
                    .withPerson(Person.person()
                            .withGender(getGender(parentGuardianInformation.getGender()).orElse(null))
                            .withDateOfBirth(nonNull(parentGuardianInformation.getDateOfBirth()) ? parentGuardianInformation.getDateOfBirth().toString() : null)
                            .withMiddleName(buildMiddleName(parentGuardianInformation))
                            .withLastName(nonNull(parentGuardianInformation.getPersonalInformation()) ? parentGuardianInformation.getPersonalInformation().getLastName() : null)
                            .withFirstName(nonNull(parentGuardianInformation.getPersonalInformation()) ? parentGuardianInformation.getPersonalInformation().getFirstName() : null)
                            .withTitle(nonNull(parentGuardianInformation.getPersonalInformation()) ? parentGuardianInformation.getPersonalInformation().getTitle() : null)
                            .withAddress(buildAddress(parentGuardianInformation))
                            .withContact(null != parentGuardianInformation.getPersonalInformation() ? buildContact(parentGuardianInformation.getPersonalInformation()) : null)
                            .withEthnicity(buildEthnicity(parentGuardianInformation, referenceDataVO))
                            .build())
                    .withRole("ParentGuardian")

                    .build();

            return List.of(associatedPerson);
        } else {
            return null;
        }
    }

    private ContactNumber buildContact(final PersonalInformation personalInformation) {

        if (personalInformation.getContactDetails() == null) {
            return null;
        }

        return contactNumber()
                .withWork(personalInformation.getContactDetails().getWork())
                .withSecondaryEmail(personalInformation.getContactDetails().getSecondaryEmail())
                .withPrimaryEmail(personalInformation.getContactDetails().getPrimaryEmail())
                .withMobile(personalInformation.getContactDetails().getMobile())
                .withHome(personalInformation.getContactDetails().getHome())
                .build();
    }

    private Ethnicity buildEthnicity(final ParentGuardianInformation parentGuardianInformation, final ReferenceDataVO referenceDataVO) {
        final Ethnicity.Builder ethnicityBuiler = ethnicity();

        if (parentGuardianInformation.getSelfDefinedEthnicity() != null) {
            referenceDataVO.getSelfdefinedEthnicityReferenceData().stream()
                    .filter(selfDefinedEthnicityReferenceData -> selfDefinedEthnicityReferenceData.getCode().equalsIgnoreCase(parentGuardianInformation.getSelfDefinedEthnicity()))
                    .findAny().ifPresent(selfDefinedEthnicityReferenceDataPG -> {
                        ethnicityBuiler.withSelfDefinedEthnicityId(selfDefinedEthnicityReferenceDataPG.getId());
                        ethnicityBuiler.withSelfDefinedEthnicityCode(selfDefinedEthnicityReferenceDataPG.getCode());
                        ethnicityBuiler.withSelfDefinedEthnicityDescription(selfDefinedEthnicityReferenceDataPG.getDescription());
                    });
        }

        if (parentGuardianInformation.getObservedEthnicity() != null) {
            referenceDataVO.getObservedEthnicityReferenceData().stream()
                    .filter(observedEthnicityReferenceData -> observedEthnicityReferenceData.getEthnicityCode().equalsIgnoreCase(parentGuardianInformation.getObservedEthnicity()))
                    .findAny().ifPresent(observedEthnicityReferenceDataPG -> {
                        ethnicityBuiler.withObservedEthnicityId(observedEthnicityReferenceDataPG.getId());
                        ethnicityBuiler.withObservedEthnicityCode(observedEthnicityReferenceDataPG.getEthnicityCode());
                        ethnicityBuiler.withObservedEthnicityDescription(observedEthnicityReferenceDataPG.getEthnicityDescription());
                    });
        }

        return ethnicityBuiler.build();
    }

    private List<DefendantAlias> buildIndividualAliases(final List<IndividualAlias> individualAliases) {
        final List<DefendantAlias> defendantAliases = new ArrayList<>();
        individualAliases.forEach(individualAlias ->
                defendantAliases.add(defendantAlias()
                        .withTitle(individualAlias.getTitle())
                        .withFirstName(individualAlias.getFirstName())
                        .withMiddleName(buildAliasMiddleName(individualAlias))
                        .withLastName(individualAlias.getLastName())
                        .build())

        );
        return defendantAliases;
    }

    private String buildAliasMiddleName(final IndividualAlias individualAlias) {
        final String middleName = join(trim(individualAlias.getGivenName2()), " ", trim(individualAlias.getGivenName3())).trim();
        return isEmpty(middleName) ? null : middleName;
    }

    private List<DefendantAlias> buildCorporateAliases(final List<String> corporateAliases) {
        final List<DefendantAlias> defendantAliases = new ArrayList<>();
        corporateAliases.forEach(corporateAlias ->
                defendantAliases.add(defendantAlias().withLegalEntityName(corporateAlias).build())
        );
        return defendantAliases;
    }

    private String buildMiddleName(final ParentGuardianInformation parentGuardianInformation) {
        if (nonNull(parentGuardianInformation.getPersonalInformation()) && nonNull(parentGuardianInformation.getPersonalInformation().getGivenName2()) && nonNull(parentGuardianInformation.getPersonalInformation().getGivenName3())) {
            return join(parentGuardianInformation.getPersonalInformation().getGivenName2(), " ", parentGuardianInformation.getPersonalInformation().getGivenName3());
        }
        return null;
    }

    private Optional<Gender> getGender(String gender) {
        if (null != gender) {
            return Gender.valueFor(gender.toUpperCase());
        } else {
            return Optional.empty();
        }
    }

    private Address buildAddress(final ParentGuardianInformation parentGuardianInformation) {
        if (null != parentGuardianInformation.getPersonalInformation() && null != parentGuardianInformation.getPersonalInformation().getAddress()) {

            final uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address address = parentGuardianInformation.getPersonalInformation().getAddress();
            return Address.address()
                    .withAddress1(address.getAddress1())
                    .withAddress2(address.getAddress2())
                    .withAddress3(address.getAddress3())
                    .withAddress4(address.getAddress4())
                    .withAddress5(address.getAddress5())
                    .withPostcode(address.getPostcode())
                    .build();
        }

        return null;
    }

}