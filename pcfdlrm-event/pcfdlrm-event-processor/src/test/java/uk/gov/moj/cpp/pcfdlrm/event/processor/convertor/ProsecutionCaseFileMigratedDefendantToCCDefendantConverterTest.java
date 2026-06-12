package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.GIVEN_NAME_2;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.GIVEN_NAME_3;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData.alcoholLevelMethodReferenceData;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest {

    private static final String EITHER_WAY = "Either Way";
    private static final String XHIBIT = "XHIBIT";

    @InjectMocks
    private ProsecutionCaseFileMigratedDefendantToCCDefendantConverter converter;

    @Spy
    private ProsecutionMigrationCaseToCCPersonDefendantConverter prosecutionMigrationCaseToCCPersonDefendantConverter;

    @Spy
    private ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @BeforeEach
    void setUp() {
        converter = new ProsecutionCaseFileMigratedDefendantToCCDefendantConverter();

        var prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter = new ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter();
        setField(prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter, "referenceDataQueryService", referenceDataQueryService);

        setField(converter, "prosecutionMigrationCaseToCCPersonDefendantConverter", prosecutionMigrationCaseToCCPersonDefendantConverter);
        setField(converter, "prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter", prosecutionMigrationCaseFileToCCLegalEntityDefendantConverter);
        setField(converter, "prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter", prosecutionCaseFileMigratedOffenceToCourtsOffenceConverter);
    }

    @Test
    void shouldConvertToCourtsDefendant() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(defendants, paramsVO);

        assertThat(defendants.size(), equalTo(courtsDefendants.size()));
        assertThat(defendants.get(0).getOffences().size(), equalTo(courtsDefendants.get(0).getOffences().size()));
        assertThat("offenceLegalisation", equalTo(courtsDefendants.get(0).getOffences().get(0).getOffenceLegislation()));
        assertAliasMiddleNameValues(courtsDefendants);
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingAmount(), is(500));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodCode(), is("A"));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodDescription(), is("Blood"));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getRole(), is("ParentGuardian"));
        assertNull(courtsDefendants.get(1).getAssociatedPersons());
    }

    @Test
    void shouldConvertToCourtsDefendantWithNoAliases() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY, true);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(defendants, paramsVO);

        assertThat(defendants.size(), equalTo(courtsDefendants.size()));
        assertNull(courtsDefendants.get(0).getAliases());
        assertNull(courtsDefendants.get(1).getAliases());
    }


    @Test
    void shouldConvertToCourtsDefendantWhenParentGuardianOrganisationNameIsNotEmpty() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);

        Prosecution prosecution = prosecutionWithReferenceData.getProsecution();

        final MigratedDefendant firstDefendant = MigratedDefendant.migratedDefendant()
                .withValuesFrom(prosecution.getDefendants().get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(prosecution.getDefendants().get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(prosecution.getDefendants().get(0).getIndividual().getParentGuardianInformation())
                                .withOrganisationName("organisationName")
                                .build())
                        .build())
                .build();
        final Prosecution newProsecution = Prosecution.prosecution()
                .withValuesFrom(prosecution)
                .withDefendants(Arrays.asList(firstDefendant, prosecution.getDefendants().get(1)))
                .build();
        prosecutionWithReferenceData.setProsecution(newProsecution);

        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(defendants, paramsVO);

        assertThat(defendants.size(), equalTo(courtsDefendants.size()));
        assertThat(defendants.get(0).getOffences().size(), equalTo(courtsDefendants.get(0).getOffences().size()));
        assertThat("offenceLegalisation", equalTo(courtsDefendants.get(0).getOffences().get(0).getOffenceLegislation()));
        assertAliasMiddleNameValues(courtsDefendants);
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingAmount(), is(500));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodCode(), is("A"));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodDescription(), is("Blood"));
        assertNull(courtsDefendants.get(0).getAssociatedPersons());
    }

    @Test
    void shouldConvertToCourtsDefendantWithCorporateDefendant() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        
        // Create a corporate defendant (no individual, but has corporate aliases)
        final MigratedDefendant corporateDefendant = MigratedDefendant.migratedDefendant()
                .withId(randomUUID())
                .withIndividual(null)
                .withAliasForCorporate(Arrays.asList("Corporate Alias 1", "Corporate Alias 2"))
                .withOffences(defendants.get(0).getOffences())
                .withInitiationCode("J")
                .withCroNumber("CRO123")
                .withPncIdentifier("PNC123")
                .withProsecutorDefendantReference("PROS123")
                .build();
        
        final List<MigratedDefendant> corporateDefendants = singletonList(corporateDefendant);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(corporateDefendants, paramsVO);

        assertThat(corporateDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getPersonDefendant(), is(nullValue()));
        assertThat(courtsDefendants.get(0).getAliases().size(), is(2));
        assertThat(courtsDefendants.get(0).getAliases().get(0).getLegalEntityName(), is("Corporate Alias 1"));
        assertThat(courtsDefendants.get(0).getAliases().get(1).getLegalEntityName(), is("Corporate Alias 2"));
        assertNull(courtsDefendants.get(0).getAssociatedPersons());
    }

    @Test
    void shouldConvertToCourtsDefendantWithEmptyDefendantList() {
        final List<MigratedDefendant> emptyDefendants = List.of();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(randomUUID());
        paramsVO.setReferenceDataVO(buildProsecutionWithReferenceData(EITHER_WAY).getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(emptyDefendants, paramsVO);

        assertThat(courtsDefendants.size(), is(0));
    }

    @Test
    void shouldConvertToCourtsDefendantWithNullIndividualAndNoCorporateAliases() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant nullIndividualDefendant = MigratedDefendant.migratedDefendant()
                .withId(randomUUID())
                .withIndividual(null)
                .withAliasForCorporate(null)
                .withOffences(defendants.get(0).getOffences())
                .withInitiationCode("J")
                .withCroNumber("CRO123")
                .withPncIdentifier("PNC123")
                .withProsecutorDefendantReference("PROS123")
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(nullIndividualDefendant);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getPersonDefendant(), is(nullValue()));
        assertNull(courtsDefendants.get(0).getAliases());
        assertNull(courtsDefendants.get(0).getAssociatedPersons());
    }

    @Test
    void shouldConvertToCourtsDefendantWithParentGuardianWithPersonalInformationMandatoryField() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullPersonalInfo = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withLastName("lastname")
                                        .build())
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullPersonalInfo);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getLastName(),is("lastname"));
    }

    @Test
    void shouldConvertToCourtsDefendantWithParentGuardianNullAddress() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullAddress = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation().getPersonalInformation())
                                        .withAddress(null)
                                        .build())
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullAddress);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getAddress(), is(nullValue()));
    }

    @Test
    void shouldConvertToCourtsDefendantWithParentGuardianNullContactDetails() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullContact = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation().getPersonalInformation())
                                        .withContactDetails(null)
                                        .build())
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullContact);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getContact(), is(nullValue()));
    }

    @Test
    void shouldConvertToCourtsDefendantWithInvalidGender() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithInvalidGender = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withGender("INVALID_GENDER")
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithInvalidGender);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getGender(), is(nullValue()));
    }

    @Test
    void shouldConvertToCourtsDefendantWithNullGender() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullGender = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withGender(null)
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullGender);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getGender(), is(nullValue()));
    }

    @Test
    void shouldConvertToCourtsDefendantWithCorporateAliasesContainingEmptyValues() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant corporateDefendantWithEmptyAliases = MigratedDefendant.migratedDefendant()
                .withId(randomUUID())
                .withIndividual(null)
                .withAliasForCorporate(asList("Valid Alias", "", null, "   "))
                .withOffences(defendants.get(0).getOffences())
                .withInitiationCode("J")
                .withCroNumber("CRO123")
                .withPncIdentifier("PNC123")
                .withProsecutorDefendantReference("PROS123")
                .build();
        
        final List<MigratedDefendant> testDefendants =singletonList(corporateDefendantWithEmptyAliases);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAliases().size(), is(4));
        assertThat(courtsDefendants.get(0).getAliases().get(0).getLegalEntityName(), is("Valid Alias"));
        assertThat(courtsDefendants.get(0).getAliases().get(1).getLegalEntityName(), is(""));
        assertThat(courtsDefendants.get(0).getAliases().get(2).getLegalEntityName(), is(nullValue()));
        assertThat(courtsDefendants.get(0).getAliases().get(3).getLegalEntityName(), is("   "));
    }

    @Test
    void shouldConvertToCourtsDefendantWithNullDateOfBirth() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullDateOfBirth = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withDateOfBirth(null)
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullDateOfBirth);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getDateOfBirth(), is(nullValue()));
    }

    @Test
    void shouldConvertToCourtsDefendantWithNullMiddleNames() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<MigratedDefendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();

        final MigratedDefendant defendantWithNullMiddleNames = MigratedDefendant.migratedDefendant()
                .withValuesFrom(defendants.get(0))
                .withIndividual(Individual.individual()
                        .withValuesFrom(defendants.get(0).getIndividual())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation())
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withValuesFrom(defendants.get(0).getIndividual().getParentGuardianInformation().getPersonalInformation())
                                        .withGivenName2(null)
                                        .withGivenName3(null)
                                        .build())
                                .withOrganisationName(null)
                                .build())
                        .build())
                .build();
        
        final List<MigratedDefendant> testDefendants = singletonList(defendantWithNullMiddleNames);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(testDefendants, paramsVO);

        assertThat(testDefendants.size(), equalTo(courtsDefendants.size()));
        assertThat(courtsDefendants.get(0).getAssociatedPersons().get(0).getPerson().getMiddleName(), is(nullValue()));
    }

    private void assertAliasMiddleNameValues(final List<uk.gov.justice.core.courts.Defendant> courtsDefendants) {
        assertThat(courtsDefendants.get(0).getAliases().get(0).getMiddleName(), is(GIVEN_NAME_2 + " " + GIVEN_NAME_3));
        assertThat(courtsDefendants.get(0).getAliases().get(1).getMiddleName(), is(GIVEN_NAME_2));
        assertThat(courtsDefendants.get(0).getAliases().get(2).getMiddleName(), is(GIVEN_NAME_3));
        assertThat(courtsDefendants.get(0).getAliases().get(3).getMiddleName(), is(nullValue()));
    }
}