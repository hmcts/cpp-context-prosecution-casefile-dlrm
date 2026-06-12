package uk.gov.moj.cpp.pcfdlrm.event.processor.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholRelatedOffence.alcoholRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.IndividualAlias.individualAlias;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.E;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.W;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData.observedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfdefinedEthnicityReferenceData.selfdefinedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleRelatedOffence.vehicleRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails.migratedCaseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial.migratedMaterial;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem.migrationSourceSystem;

import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Details;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

public class CaseReceivedHelper {

    public static final UUID MOCKED_CASE_ID = fromString("dfd9e505-66bc-42b5-901c-4447ba3bfcd4");
    public static final UUID MOCKED_OFFENCE_ID = fromString("afd9e999-66bc-42b5-901c-4447ba3bfcd4");

    public static final String GIVEN_NAME_2 = "GivenName2";
    public static final String GIVEN_NAME_3 = "GivenName3";

    public static final String SELF_DEFINED_ETHNICITY_CODE = "selfDefinedEthnicity";
    public static final String OBSERVED_ETHNICITY_CODE = "1";
    public static final String NATIONALITY_CODE = "nationality";
    public static final String ADDITIONAL_NATIONALITY_CODE = "additionalNationality";
    public static final String TRANSFER = "Transfer";

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial) {
        return buildProsecutionWithReferenceData(modeOfTrial, randomUUID().toString());
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final boolean isAliasesEmpty) {
        return buildProsecutionWithReferenceData(modeOfTrial, randomUUID().toString(), isAliasesEmpty);
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final String motReasonId) {
        return buildProsecutionWithReferenceData(modeOfTrial, motReasonId, false);
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final String motReasonId, final boolean isAliasesEmpty) {

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional =
                of(organisationUnitWithCourtroomReferenceData()
                        .withId(randomUUID().toString())
                        .withOucodeL3Name("South Western (Lavender Hill)")
                        .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(buildProsecution(isAliasesEmpty));
        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(organisationUnitWithCourtroomReferenceDataOptional.get());
        referenceDataVO.setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(randomUUID())
                .withShortName("DVLA")
                .withFullName("fullName")
                .withMajorCreditorCode("CreditorCode")
                .withInformantEmailAddress("test@email.com")
                .withOucode("oucode")
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .build());
        referenceDataVO.setCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setSelfdefinedEthnicityReferenceData(singletonList(selfdefinedEthnicityReferenceData().withCode(SELF_DEFINED_ETHNICITY_CODE).withId(randomUUID()).withDescription("description").build()));
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(ADDITIONAL_NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));

        return prosecutionWithReferenceData;
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrial(final String modeOfTrial) {
        return buildReferenceDataWithOffenceAndModeOfTrial(modeOfTrial, randomUUID().toString());
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrial(final String modeOfTrial, final String motReasonId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));

        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataWithNotGuiltyPlea(final String modeOfTrial, final String defendantId, final String offenceId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(List.of(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build()));

        referenceDataVO.setPleaReferenceDataMap(Map.of(UUID.fromString(defendantId), Map.of(UUID.fromString(offenceId),
                PleaReferenceData.pleaReferenceData()
                        .withPleaValue("Not Guilty")
                        .withPleaTypeCode("NG")
                        .withPleaTypeGuiltyFlag("No")
                        .build())));

        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataWithGuiltyPlea(final String modeOfTrial, final String defendantId, final String offenceId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(List.of(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build()));

        referenceDataVO.setPleaReferenceDataMap(Map.of(UUID.fromString(defendantId), Map.of(UUID.fromString(offenceId),
                PleaReferenceData.pleaReferenceData()
                        .withPleaValue("Guilty")
                        .withPleaTypeCode("N")
                        .withPleaTypeGuiltyFlag("Yes")
                        .build())));

        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataIncludingDvlaCode() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(getOffenceReferenceData());
        return referenceDataVO;
    }

    private static List<OffenceReferenceData> getOffenceReferenceData() {
        return singletonList(new OffenceReferenceData(true, "TVL-ABC", buildPressRestrictableReferenceData().getCppDateOfLastUpdate(), Details.details().build(),
                buildPressRestrictableReferenceData().getDrugsOrAlcoholRelated(), "dvlaCode", true, buildPressRestrictableReferenceData().getLegislation(), buildPressRestrictableReferenceData().getLegislationWelsh(),
                buildPressRestrictableReferenceData().getLocationRequired(), "Max Penalty", buildPressRestrictableReferenceData().getModeOfTrial(),
                buildPressRestrictableReferenceData().getModeOfTrialDerived(), buildPressRestrictableReferenceData().getOffenceEndDate(), randomUUID(), buildPressRestrictableReferenceData().getOffenceStartDate(), buildPressRestrictableReferenceData().getPnldDateOfLastUpdate(), buildPressRestrictableReferenceData().getProsecutionTimeLimit(),
                buildPressRestrictableReferenceData().getReportRestrictResultCode(), buildPressRestrictableReferenceData().getTitle(), buildPressRestrictableReferenceData().getTitleWelsh(), buildPressRestrictableReferenceData().getValidFrom(), buildPressRestrictableReferenceData().getValidTo()));
    }

    private static List<OffenceReferenceData> buildOffenceReferenceData(final String modeOfTrial) {
        return ImmutableList.of(offenceReferenceData()
                .withCjsOffenceCode("TVL-ABC")
                .withLegislation("offenceLegalisation")
                .withLegislationWelsh("offenceLegalisationWelsh")
                .withOffenceId(fromString("d8c63737-3c60-496b-94bb-30faa761f00a"))
                .withTitle("Offence Tittle")
                .withTitleWelsh("Offence Tittle Welsh")
                .withModeOfTrialDerived(modeOfTrial)
                .withMaxPenalty("Max Penalty")
                .build());
    }

    public static Prosecution buildProsecution(final boolean isAliasesEmpty) {
        return prosecution()
                .withCaseDetails(caseDetails().withInitiationCode("Z")
                        .withCaseId(MOCKED_CASE_ID)
                        .withProsecutor(buildProsecutionSubmissionDetails())
                        .withOriginatingOrganisation("orignatingOrganisation")
                        .withCpsOrganisation("cpsOrganisation")
                        .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                        .withClassOfCase("Class 1")
                        .withSummonsCode("M")
                        .withTrialReceiptType(TRANSFER)
                        .build())
                .withChannel(SPI)
                .withDefendants(asList(buildDefendant(isAliasesEmpty), buildCorporateDefendant(isAliasesEmpty)))
                .build();
    }


    public static Prosecutor buildProsecutionSubmissionDetails() {
        return prosecutor()
                .withProsecutingAuthority("TVL")
                .withProsecutingAuthority("ProsecutionAuthority")
                .withReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                        .withContactEmailAddress("contact@cpp.co.uk")
                        .withAddress(Address.address()
                                .withAddress1("address1")
                                .withAddress2("address2")
                                .withAddress3("address3")
                                .withAddress4("address4")
                                .withAddress5("address5")
                                .withPostcode("postcode")
                                .build())
                        .withFullName("fullname")
                        .withId(randomUUID())
                        .withMajorCreditorCode("mojor")
                        .withOucode("Ou")
                        .build())
                .build();
    }

    private static MigratedDefendant buildCorporateDefendant(final boolean isAliasesEmpty) {
        final MigratedDefendant.Builder defendantBuilder = migratedDefendant();
        defendantBuilder
                .withId(randomUUID())
                .withDocumentationLanguage(E.name())
                .withHearingLanguage(E.name())
                .withOrganisationName("organisation name");
        if (isAliasesEmpty) {
            defendantBuilder.withAliasForCorporate(emptyList());
        } else {
            defendantBuilder.withAliasForCorporate(asList("alias1", "alias2", "alias3"));
        }

        defendantBuilder.withOffences(buildOffences())
                .withLanguageRequirement("No");
        return defendantBuilder.build();
    }


    public static MigratedDefendant buildDefendantWithTitle(String title) {
        return buildDefendantWithTitle(title, false);
    }

    public static MigratedDefendant buildDefendantWithTitle(String title, final boolean isIndividualAliasesEmpty) {
        return migratedDefendant()
                .withId(randomUUID())
                .withDocumentationLanguage(E.name())
                .withHearingLanguage(E.name())
                .withInitiationCode("S")
                .withIndividualAliases(buildIndividualAliases(isIndividualAliasesEmpty))
                .withIndividual(Individual.individual()
                        .withPersonalInformation(personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress4("address line 5")
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withContactDetails(contactDetails()
                                        .withPrimaryEmail("primaryemail")
                                        .withSecondaryEmail("secondaryemail")
                                        .withHome("homePhone")
                                        .withWork("workPhone")
                                        .withMobile("mobile")
                                        .build())
                                .withContactDetails(contactDetails()
                                        .withPrimaryEmail("primaryemail")
                                        .withSecondaryEmail("secondaryemail")
                                        .withHome("homePhone")
                                        .withWork("workPhone")
                                        .withMobile("mobile")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle(title)
                                .withObservedEthnicity(Integer.parseInt(OBSERVED_ETHNICITY_CODE))
                                .build())
                        .withPerceivedBirthYear("1970")
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(MALE.name())
                                .withNationality(NATIONALITY_CODE)
                                .withAdditionalNationality(ADDITIONAL_NATIONALITY_CODE)
                                .withEthnicity(SELF_DEFINED_ETHNICITY_CODE)
                                .build())
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation()
                                        .withAddress(Address.address()
                                                .withAddress1("66 Exeter Street")
                                                .withAddress2("address line 2")
                                                .withAddress3("address line 3")
                                                .withAddress4("address line 4")
                                                .withAddress4("address line 5")
                                                .withPostcode("M60 1NW")
                                                .build())
                                        .withContactDetails(contactDetails()
                                                .withPrimaryEmail("primaryemail")
                                                .withSecondaryEmail("secondaryemail")
                                                .withHome("homePhone")
                                                .withWork("workPhone")
                                                .withMobile("mobile")
                                                .build())
                                        .withFirstName("Eugene")
                                        .withLastName("Tooms")
                                        .withTitle(title)
                                        .build())
                                .withSelfDefinedEthnicity(SELF_DEFINED_ETHNICITY_CODE)
                                .withObservedEthnicity(OBSERVED_ETHNICITY_CODE)
                                .build())
                        .withNationalInsuranceNumber("1922492")
                        .withDriverNumber("2362435")
                        .build())
                .withOffences(buildOffences())
                .withLanguageRequirement("No")
                .build();
    }

    public static MigratedDefendant buildDefendantWithCustodyStatus(final String title, final UUID defendantId, final String custodyStatus, final LocalDate custodyTimeLimit) {
        return migratedDefendant()
                .withId(defendantId)
                .withIndividual(Individual.individual()
                        .withCustodyStatus(custodyStatus)
                        .withCustodyTimeLimit(custodyTimeLimit)
                        .withPersonalInformation(personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress4("address line 5")
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle(title)
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(MALE.name())
                                .build())
                        .withNationalInsuranceNumber("1922492")
                        .withDriverNumber("2362435")
                        .build())
                .withHearingLanguage(E.name())
                .withDocumentationLanguage(W.name())
                .withOffences(buildOffences())
                .build();
    }

    private static List<IndividualAlias> buildIndividualAliases(final boolean returnIndividualAliasesEmpty) {
        if (returnIndividualAliasesEmpty) {
            return emptyList();
        }

        return asList(
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName2("   " + GIVEN_NAME_2 + " ")
                        .withGivenName3(" " + GIVEN_NAME_3 + "   ")
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName2(GIVEN_NAME_2)
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName3(GIVEN_NAME_3)
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withLastName("LastName")
                        .build());
    }

    public static MigratedDefendant buildDefendant(final boolean isIndividualAliasesEmpty) {
        return buildDefendantWithTitle("MR", isIndividualAliasesEmpty);
    }


    public static List<MigratedOffence> buildOffences() {
        return singletonList(migratedOffence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withAlcoholRelatedOffence(alcoholRelatedOffence()
                        .withAlcoholLevelAmount(500)
                        .withAlcoholLevelMethod("A").build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("TVL-ABC")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withReferenceData(buildNonPressRestrictableReferenceData())
                .withProsecutorOfferAOCP(true)
                .build());
    }


    private static OffenceReferenceData buildPressRestrictableReferenceData() {
        return offenceReferenceData().withReportRestrictResultCode("D45").build();
    }

    public static OffenceReferenceData buildNonPressRestrictableReferenceData() {
        return offenceReferenceData().withReportRestrictResultCode("").build();
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation(final String modeOfTrial, final String courtId) {
        return buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation(modeOfTrial, randomUUID().toString(), courtId);
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation(final String modeOfTrial, final String motReasonId, final String courtId) {
        OrganisationUnitReferenceData organisationUnitReferenceData = organisationUnitReferenceData().withId(courtId).withOucodeL3Name("thecourt").build();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .withId(randomUUID()).withShortName("TFL").build());
        referenceDataVO.setCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setReceivingCourtOrganisationUnit(organisationUnitReferenceData);
        referenceDataVO.setSendingCourtOrganisationUnit(organisationUnitReferenceData);
        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataWithNullContactInfo(final String courtId) {
        OrganisationUnitReferenceData organisationUnitReferenceData = organisationUnitReferenceData().withId(courtId).withOucodeL3Name("thecourt").build();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData("Either Way"));
        referenceDataVO.setModeOfTrialReferenceData(singletonList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build()));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .withId(randomUUID())
                .withShortName("TFL")
                .withInformantEmailAddress(null)
                .withContactEmailAddress(null)
                .build());
        referenceDataVO.setCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setReceivingCourtOrganisationUnit(organisationUnitReferenceData);
        referenceDataVO.setSendingCourtOrganisationUnit(organisationUnitReferenceData);
        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataWithNullOrganizationUnit() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData("Either Way"));
        referenceDataVO.setModeOfTrialReferenceData(singletonList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build()));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .withId(randomUUID())
                .withShortName("TFL")
                .build());
        referenceDataVO.setCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setReceivingCourtOrganisationUnit(null);
        referenceDataVO.setSendingCourtOrganisationUnit(null);
        return referenceDataVO;
    }

    public static List<MigratedMaterial> getMaterials(final UUID caseId) {
        List<MigratedMaterial> materials = new ArrayList<>();
        MigratedMaterial migratedMaterial1 = migratedMaterial().withCaseId(caseId).withId(UUID.randomUUID())
                .withDefendantId("c2391758-f829-4514-a222-61f1d5d9690d").withDocumentType(12)
                .withDocumentCategory("DocumentCategory1").withFileName("FileName1").withFileType("FileType1")
                .build();

        MigratedMaterial migratedMaterial2 = migratedMaterial().withCaseId(caseId).withId(UUID.randomUUID())
                .withDefendantId("c2391758-f829-4514-a222-61f1d5d9610d").withDocumentType(13)
                .withDocumentCategory("DocumentCategory2").withFileName("FileName2").withFileType("FileType2")
                .build();

        materials.add(migratedMaterial1);
        materials.add(migratedMaterial2);

        return materials;
    }

    public static MigratedCaseDetails getMigratedCaseDetails(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN")
                .withInitiationCode("Q").withOrganisationName("OrganisationName")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();
        defendants.add(defendant);
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithNoDefendants(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(new ArrayList<>())
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithMultipleDefendants(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();

        MigratedDefendant defendant1 = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN1")
                .withInitiationCode("Q").withOrganisationName("OrganisationName1")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();

        MigratedDefendant defendant2 = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690e")).withAsn("ASN2")
                .withInitiationCode("Q").withOrganisationName("OrganisationName2")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();

        defendants.add(defendant1);
        defendants.add(defendant2);

        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithNoCaseMarkers(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(null)
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN")
                .withInitiationCode("Q").withOrganisationName("OrganisationName")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();
        defendants.add(defendant);
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithDifferentInitiationCode(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("J").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN")
                .withInitiationCode("J").withOrganisationName("OrganisationName")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();
        defendants.add(defendant);
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithDifferentSourceSystem(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN")
                .withInitiationCode("Q").withOrganisationName("OrganisationName")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();
        defendants.add(defendant);
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("XHIBIT")
                        .withMigrationSourceSystemCaseIdentifier("XHIBIT NUMBER")
                        .build()
                ).build();
    }

    public static MigratedCaseDetails getMigratedCaseDetailsWithNullDates(final UUID caseId) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference("CASE-URN")
                .withInitiationCode("Q").withSummonsCode("SummonsCode")
                .withCaseMarkers(singletonList(caseMarker().withMarkerTypeCode("ML").build()))
                .withSendingCourt("ABCDEF00")
                .withReceivingCourt("ABCDEF00")
                .withDateOfCommittal(null)
                .withDateOfSending(null)
                .build();
        final List<MigratedDefendant> defendants = new ArrayList<>();
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant().withId(UUID.fromString("c2391758-f829-4514-a222-61f1d5d9690d")).withAsn("ASN")
                .withInitiationCode("Q").withOrganisationName("OrganisationName")
                .withOffences(buildOffences())
                .withHearingLanguage(E.name()).build();
        defendants.add(defendant);
        return migratedCaseDetails().withCaseDetails(caseDetails).withDefendants(defendants)
                .withMigrationSourceSystem(migrationSourceSystem()
                        .withMigrationSourceSystemName("LIBRA")
                        .withMigrationSourceSystemCaseIdentifier("LIBRA NUMBER")
                        .build()
                ).build();
    }

}
