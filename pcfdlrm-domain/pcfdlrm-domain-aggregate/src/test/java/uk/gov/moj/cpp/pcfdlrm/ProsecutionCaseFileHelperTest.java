package uk.gov.moj.cpp.pcfdlrm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.defendantWithParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.individualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.organisationParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianAddress;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validGuardianContactDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.validIndividualParentGuardian;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.CASE_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.LIBRA;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.PG;
import static uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader.fixture;
import static uk.gov.moj.cpp.pcfdlrm.test.WholePayloadMatcher.matchesWholePayload;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData.bailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData.observedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.DefendantValidationFailed;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileHelperTest {
    @Mock
    private List<MigratedHearingRefDataEnricher> enrichers;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void buildMigratedHearingRefData() {

        CaseDetails caseDetails = CaseDetails.caseDetails().build();

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        UUID offId11 = UUID.randomUUID();
        final MigratedOffence o11 =  buildMigratedOffence(offId11,"offId11") ;
        UUID offId12 = UUID.randomUUID();
        final MigratedOffence o12 =  buildMigratedOffence(offId12,"offId12") ;
        UUID offId13 = UUID.randomUUID();
        final MigratedOffence o13 =  buildMigratedOffence(offId13,"offId13") ;

        UUID offId21 = UUID.randomUUID();
        final MigratedOffence o21 = buildMigratedOffence(offId21,"offId21") ;
        UUID offId22 = UUID.randomUUID();
        final MigratedOffence o22 =  buildMigratedOffence(offId22,"offId22") ;
        UUID offId23 = UUID.randomUUID();
        final MigratedOffence o23 =  buildMigratedOffence(offId23,"offId23") ;


        UUID offId31 = UUID.randomUUID();
        final MigratedOffence o31 = buildMigratedOffence(offId31,"offId31") ;


        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one" ,List.of(o11,o12,o13)),
                buildMigratedDefendant(id2, "two-two" ,List.of(o21,o22,o23)),
                buildMigratedDefendant(id3, "three-three" ,List.of(o31))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(ListedDefendant.listedDefendant().withProsecutorDefendantId("one-one").withListedOffences(List.of("offId11","offId12")).build()
                        ,ListedDefendant.listedDefendant().withProsecutorDefendantId("two-two").withListedOffences(List.of("offId21","offId22")).build() ))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertNotNull(result, "Result should not be null");
        final Map<UUID, List<UUID>> defendantWithOffences = result.getMigratedDefendantWithOffences().stream().collect(Collectors.toMap(e -> e.getDefendant().getId(), MigratedDefendantWithOffences::getOffenceids));
        assertEquals(2, defendantWithOffences.size(), "Expected exactly 2 defendants in the result");


        assertAll(
                ()->assertTrue(defendantWithOffences.get(id1).containsAll(List.of(offId11,offId12))),
                ()->assertTrue(defendantWithOffences.get(id2).containsAll(List.of(offId21,offId22)))
        );

    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenNoListedDefendants() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(buildMigratedOffence(UUID.randomUUID(), "offId11")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of())
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenListedDefendantIdNotFound() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(buildMigratedOffence(UUID.randomUUID(), "offId11")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(
                        ListedDefendant.listedDefendant()
                                .withProsecutorDefendantId("unknown-defendant")
                                .withListedOffences(List.of("offId11"))
                                .build()))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenListedOffenceDoesNotMatch() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(
                        buildMigratedOffence(UUID.randomUUID(), "offId11"),
                        buildMigratedOffence(UUID.randomUUID(), "offId12")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(
                        ListedDefendant.listedDefendant()
                                .withProsecutorDefendantId("one-one")
                                .withListedOffences(List.of("offId11", "offId-INVALID"))
                                .build()))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void shouldValidateDefendantErrorsWhenCustodyStatusInvalidOnXhibitSetsStatusToUAndPreservesObservedEthnicity() {
        final Integer observedEthnicityCode = 12;

        final MigratedDefendant defendant = migratedDefendant()
                .withDocumentationLanguage("E")
                .withHearingLanguage("E")
                .withIndividual(Individual.individual()
                        .withCustodyStatus("INVALID_STATUS")
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withObservedEthnicity(observedEthnicityCode)
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withGender("MALE")
                                .build())
                        .build())
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setObservedEthnicityReferenceData(List.of(
                observedEthnicityReferenceData()
                        .withEthnicityCode(String.valueOf(observedEthnicityCode))
                        .withId(UUID.randomUUID())
                        .withEthnicityDescription("description")
                        .build()
        ));

        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(UUID.randomUUID()).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails);

        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(List.of(
                bailStatusReferenceData().withStatusCode("U").build()
        ));

        final MigratedDefendantWithProblem result = ProsecutionCaseFileHelper.validateDefendants(
                caseDetails, DLRM_MIGRATION, defendantsWithReferenceData, referenceDataQueryService,
                Stream.builder(), false, "XHIBIT").migratedDefendantWithProblem();

        assertMigratedDefendantMatchesFixture(result.getMigratedDefendants().get(0),
                "json/prosecution-case-file-helper/migrated-defendant-custody-status-normalised.json");
    }

    @Test
    void shouldApplyRuleToDefendantFieldsNormalisingGenderAndLanguageOnXhibitPath() {
        final MigratedDefendant defendant = migratedDefendant()
                .withDocumentationLanguage("ZZ")
                .withHearingLanguage("ZZ")
                .withIndividual(Individual.individual()
                        .withPersonalInformation(PersonalInformation.personalInformation().build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withGender("XXX")
                                .build())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withGender("YYY")
                                .build())
                        .build())
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(UUID.randomUUID()).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails);

        final MigratedDefendantWithProblem result = ProsecutionCaseFileHelper.validateDefendants(
                caseDetails, DLRM_MIGRATION, defendantsWithReferenceData, referenceDataQueryService,
                Stream.builder(), false, "XHIBIT").migratedDefendantWithProblem();

        assertMigratedDefendantMatchesFixture(result.getMigratedDefendants().get(0),
                "json/prosecution-case-file-helper/migrated-defendant-gender-and-language-normalised.json");
    }

    @Test
    void shouldValidateDefendantErrorsWhenCustodyStatusInvalidOnXhibitAndUStatusFoundAddsUBailStatusToReferenceDataVO() {
        final MigratedDefendant defendant = migratedDefendant()
                .withDocumentationLanguage("E")
                .withHearingLanguage("E")
                .withIndividual(Individual.individual()
                        .withCustodyStatus("INVALID_STATUS")
                        .withPersonalInformation(PersonalInformation.personalInformation().build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withGender("MALE")
                                .build())
                        .build())
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(UUID.randomUUID()).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails);

        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(List.of(
                bailStatusReferenceData().withStatusCode("U").build()
        ));

        ProsecutionCaseFileHelper.validateDefendants(
                caseDetails, DLRM_MIGRATION, defendantsWithReferenceData, referenceDataQueryService,
                Stream.builder(), false, "XHIBIT");

        assertThat(referenceDataVO.getBailStatusReferenceData().size(), is(1));
        assertThat(referenceDataVO.getBailStatusReferenceData().get(0).getStatusCode(), is("U"));
    }

    private final Stream.Builder<Object> events = Stream.builder();

    @Test
    void shouldRaiseNoParentGuardianProblemWhenLibraDefendantHasNoParentGuardian() {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID, null);

        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "C", defendant);

        assertThat(parentGuardianProblems(outcome), is(empty()));
        assertThat(codesOf(outcome), not(hasItem("PARENT_GUARDIAN_GENDER_INVALID")));
        assertThat(outcome.libraGuardianRejections(), is(empty()));
        assertThat(outcome.migratedDefendantWithProblem().getMigratedDefendants().get(0), is(defendant));
    }

    @Test
    void shouldSanitiseIndividualParentGuardianAndRaiseOneRedactedProblemPerField() {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID,
                individualParentGuardian(validGuardianContactDetails().withHome("012345678").withPrimaryEmail("first+tag@example.org").build(), validGuardianAddress().build())
                        .withDateOfBirth(LocalDate.now(ZoneId.of("Europe/London")).plusDays(1))
                        .withGender(null)
                        .build());

        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "S", defendant);

        assertThat(outcome.migratedDefendantWithProblem().getMigratedDefendants().get(0), is(defendantWithParentGuardian(DEFENDANT_ID,
                individualParentGuardian(validGuardianContactDetails().withHome(null).withPrimaryEmail(null).build(), validGuardianAddress().build())
                        .withDateOfBirth(null)
                        .withGender("NOT_KNOWN")
                        .build())));
        assertThat(parentGuardianProblems(outcome).stream().map(Problem::getCode).toList(), containsInAnyOrder(
                "PARENT_GUARDIAN_HOME_TELEPHONE_INVALID",
                "DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID",
                "DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE",
                "PARENT_GUARDIAN_GENDER_INVALID"));
        assertThat(outcome.libraGuardianRejections(), is(empty()));
        assertRedacted(parentGuardianProblems(outcome));
        assertRedacted(defendantValidationFailedParentGuardianProblems());
    }

    @ParameterizedTest(name = "gender \"{0}\" -> {1}, warning: {2}")
    @CsvSource(nullValues = "NONE", value = {
            "0,    NOT_KNOWN,     false",
            "1,    MALE,          false",
            "2,    FEMALE,        false",
            "9,    NOT_SPECIFIED, false",
            "NONE, NOT_KNOWN,     true",
            "X,    NOT_KNOWN,     true"
    })
    void shouldMapLibraParentGuardianGender(final String gender, final String expectedGender, final boolean expectWarning) {
        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "C", defendantWithParentGuardian(DEFENDANT_ID, validIndividualParentGuardian().withGender(gender).build()));

        assertThat(guardianOf(outcome).getGender(), is(expectedGender));
        assertThat(parentGuardianProblems(outcome).stream().map(Problem::getCode).toList(), is(expectWarning ? List.of("PARENT_GUARDIAN_GENDER_INVALID") : List.of()));
        assertThat(outcome.libraGuardianRejections(), is(empty()));
    }

    @Test
    void shouldSanitiseOrganisationParentGuardianWithoutTouchingGender() {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID,
                organisationParentGuardian(validGuardianAddress().withAddress5("x".repeat(36)).withPostcode("NOT A POSTCODE").build())
                        .withOrganisationName(null)
                        .withCompanyTelephoneNumber("ext 22")
                        .build());

        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "Q", defendant);

        assertThat(guardianOf(outcome), is(organisationParentGuardian(validGuardianAddress().withPostcode(null).build())
                .withOrganisationName(null)
                .withCompanyTelephoneNumber(null)
                .build()));
        assertThat(parentGuardianProblems(outcome).stream().map(Problem::getCode).toList(), containsInAnyOrder(
                "PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID",
                "PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID",
                "INVALID_GUARDIAN_ORGANISATION_POST_CODE"));
        assertThat(codesOf(outcome), not(hasItem("PARENT_GUARDIAN_GENDER_INVALID")));
        assertThat(outcome.libraGuardianRejections(), is(empty()));
        assertRedacted(parentGuardianProblems(outcome));
    }

    @Test
    void shouldKeepOrganisationNameWithNoBusinessRule() {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID,
                organisationParentGuardian(validGuardianAddress().build()).withOrganisationName("o".repeat(255)).build());

        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "C", defendant);

        assertThat(outcome.migratedDefendantWithProblem().getMigratedDefendants().get(0), is(defendant));
        assertThat(parentGuardianProblems(outcome), is(empty()));
    }

    @Test
    void shouldRemoveOverlongIndividualAddressLineWithoutRejecting() {
        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, "C", defendantWithParentGuardian(DEFENDANT_ID,
                individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress3("x".repeat(36)).build()).build()));

        assertThat(guardianOf(outcome), is(validIndividualParentGuardian().build()));
        assertThat(parentGuardianProblems(outcome).stream().map(Problem::getCode).toList(), is(List.of("PARENT_GUARDIAN_ADDRESS_LINE_INVALID")));
        assertThat(outcome.libraGuardianRejections(), is(empty()));
    }

    static Stream<Arguments> libraRejections() {
        final String address1 = "PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID";
        final String orgAddress1 = "PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID";
        return Stream.of(
                arguments("S, no address (AC-S3-003)", "S", individualParentGuardian(validGuardianContactDetails().build(), null).build(), List.of(address1)),
                arguments("C, no address (AC-S3-003/004)", "C", individualParentGuardian(validGuardianContactDetails().build(), null).build(), List.of(address1)),
                arguments("Q, no address (AC-S3-003)", "Q", individualParentGuardian(validGuardianContactDetails().build(), null).build(), List.of(address1)),
                arguments("address1 whitespace (AC-S3-006)", "C", individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress1("   ").build()).build(), List.of(address1)),
                arguments("address1 NFA (AC-S3-005)", "C", individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withAddress1("NFA").build()).build(), List.of()),
                arguments("postcode absent (AC-S3-008, Q1)", "C", individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withPostcode(null).build()).build(), List.of()),
                arguments("postcode SW1A 1AA (AC-S3-008)", "C", validIndividualParentGuardian().build(), List.of()),
                arguments("postcode NOT A POSTCODE (AC-S3-008)", "C", individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withPostcode("NOT A POSTCODE").build()).build(),
                        List.of("INVALID_GUARDIAN_POST_CODE")),
                arguments("postcode ZZ99 9ZZ (AC-S3-008, Q14)", "C", individualParentGuardian(validGuardianContactDetails().build(), validGuardianAddress().withPostcode("ZZ99 9ZZ").build()).build(), List.of()),
                arguments("organisation, no address — as LIBRA sends it (AC-S4-001/004)", "C", organisationParentGuardian(null).build(), List.of(orgAddress1)),
                arguments("organisation, address1 blank (AC-S4-002)", "S", organisationParentGuardian(validGuardianAddress().withAddress1(" ").build()).build(), List.of(orgAddress1))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("libraRejections")
    void shouldCollectLibraParentGuardianRejections(final String description, final String initiationCode,
                                                    final ParentGuardianInformation guardian, final List<String> expectedRejectionCodes) {
        final DefendantValidationOutcome outcome = validateDefendants(LIBRA, initiationCode, defendantWithParentGuardian(DEFENDANT_ID, guardian));

        assertThat(outcome.libraGuardianRejections().stream().map(Problem::getCode).toList(), is(expectedRejectionCodes));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"LIBRA, J", "LIBRA, R", "LIBRA, O", "XHIBIT, C", "XHIBIT, S"})
    void shouldKeepTodaysBehaviourOutsideLibraParentGuardianScope(final String sourceSystemName, final String initiationCode) {
        final MigratedDefendant defendant = defendantWithParentGuardian(DEFENDANT_ID,
                individualParentGuardian(validGuardianContactDetails().withHome("012345678").build(), null)
                        .withGender("X")
                        .withCompanyTelephoneNumber("ext 22")
                        .build());

        final DefendantValidationOutcome outcome = validateDefendants(sourceSystemName, initiationCode, defendant);

        assertThat(outcome.libraGuardianRejections(), is(empty()));
        assertThat(guardianOf(outcome).getPersonalInformation().getContactDetails().getHome(), is("012345678"));
        assertThat(guardianOf(outcome).getCompanyTelephoneNumber(), is("ext 22"));
        assertThat(parentGuardianProblems(outcome), is(empty()));
        assertThat(codesOf(outcome), hasItem("PARENT_GUARDIAN_GENDER_INVALID"));
    }

    private DefendantValidationOutcome validateDefendants(final String sourceSystemName, final String initiationCode, final MigratedDefendant... defendants) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(CASE_ID).withInitiationCode(initiationCode).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendants));
        defendantsWithReferenceData.setReferenceDataVO(new ReferenceDataVO());
        defendantsWithReferenceData.setCaseDetails(caseDetails);
        return ProsecutionCaseFileHelper.validateDefendants(caseDetails, DLRM_MIGRATION, defendantsWithReferenceData,
                referenceDataQueryService, events, false, sourceSystemName);
    }

    private static ParentGuardianInformation guardianOf(final DefendantValidationOutcome outcome) {
        return outcome.migratedDefendantWithProblem().getMigratedDefendants().get(0).getIndividual().getParentGuardianInformation();
    }

    private static List<String> codesOf(final DefendantValidationOutcome outcome) {
        return outcome.migratedDefendantWithProblem().getDefendantProblems().stream()
                .flatMap(defendantProblem -> defendantProblem.getProblems().stream())
                .map(Problem::getCode)
                .toList();
    }

    /** Problems raised by the LIBRA guardian rules — their keys all sit under {@code individual_parentGuardianInformation}. */
    private static List<Problem> parentGuardianProblems(final DefendantValidationOutcome outcome) {
        return outcome.migratedDefendantWithProblem().getDefendantProblems().stream()
                .flatMap(defendantProblem -> defendantProblem.getProblems().stream())
                .filter(problem -> problem.getValues().get(0).getKey().startsWith(PG))
                .toList();
    }

    private List<Problem> defendantValidationFailedParentGuardianProblems() {
        return events.build()
                .filter(DefendantValidationFailed.class::isInstance)
                .map(DefendantValidationFailed.class::cast)
                .flatMap(event -> event.getProblems().stream())
                .filter(problem -> problem.getValues().get(0).getKey().startsWith(PG))
                .toList();
    }

    private static void assertRedacted(final List<Problem> problems) {
        assertThat(problems, not(empty()));
        assertThat(problems.stream().map(p -> p.getValues().get(0).getValue().equals(p.getValues().get(0).getKey())).toList(), everyItem(is(true)));
    }

    private static void assertMigratedDefendantMatchesFixture(final MigratedDefendant defendant, final String fixturePath) {
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        assertThat(objectToJsonObjectConverter.convert(defendant).toString(), matchesWholePayload(fixture(fixturePath), List.of()));
    }

    private MigratedOffence buildMigratedOffence(final UUID offenceID, final String prosecutionOffenceId){
        return migratedOffence().withOffenceId(offenceID).withProsecutorOffenceId(prosecutionOffenceId).build();
    }

    private MigratedDefendant buildMigratedDefendant(final UUID defendantId,final  String prosecutiorDefendantId,final  List<MigratedOffence> offences){
        return  migratedDefendant()
                .withProsecutorDefendantId(prosecutiorDefendantId)
                .withId(defendantId)
                .withOffences(offences)
                .build();
    }

}
