package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildDefendantWithCustodyStatus;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildDefendantWithTitle;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionMigrationCaseToCCPersonDefendantConverterTest {

    public static final String CUSTODY_STATUS = "C";
    public static final String STATUS_CODE = "C";
    private ProsecutionMigrationCaseToCCPersonDefendantConverter converter;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @Test
    void shouldNotConvertTheTitle() {
        final MigratedDefendant defendant = buildDefendantWithTitle("Baroness");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertThat(personDefendant.getPersonDetails().getTitle(), is("Baroness"));
    }

    @Test
    void shouldConvertDefendantToPersonDefendant() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));
        assertThat(personDefendant.getBailConditions(), is(defendant.getIndividual().getBailConditions()));
        assertThat(personDefendant.getBailStatus(), is(defendant.getIndividual().getCustodyStatus()));
        assertThat(personDefendant.getDriverLicenseIssue(), is(defendant.getIndividual().getDriverLicenceIssue()));
        assertThat(personDefendant.getDriverLicenceCode(), is(defendant.getIndividual().getDriverLicenceCode()));
        assertThat(personDefendant.getDriverNumber(), is(defendant.getIndividual().getDriverNumber()));
        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));

        assertNotNull(personDefendant.getPersonDetails().getNationalityCode());
        assertNotNull(personDefendant.getPersonDetails().getAdditionalNationalityCode());
        assertThat(personDefendant.getPersonDetails().getNationalInsuranceNumber(),
                is(defendant.getIndividual().getNationalInsuranceNumber()));
        assertThat(personDefendant.getPersonDetails().getNationalityCode(),
                is(defendant.getIndividual().getSelfDefinedInformation().getNationality()));
        assertThat(personDefendant.getPersonDetails().getAdditionalNationalityCode(),
                is(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality()));
        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));
        assertThat(personDefendant.getPersonDetails().getHearingLanguageNeeds(), is(HearingLanguage.ENGLISH));
        assertThat(personDefendant.getPersonDetails().getDocumentationLanguageNeeds(), is(HearingLanguage.ENGLISH));

    }

    @Test
    void bailStatusCodeShouldBeMatchedWhenCustodyStatusAndStatusCodeAreSame() {
        final MigratedDefendant defendant =
                buildDefendantWithCustodyStatus("Mr", UUID.randomUUID(), CUSTODY_STATUS, LocalDate.parse("2026-01-29", DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, STATUS_CODE,"description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertThat(personDefendant.getBailStatus().getCode() , is(STATUS_CODE));
        assertThat(personDefendant.getBailStatus().getCustodyTimeLimit().getTimeLimit(),is(personDefendant.getBailStatus().getCustodyTimeLimit().getTimeLimit()));
    }


    @Test
    void bailStatusIsEmptyWhenCustodyStatusAndStatusCodeAreDifferent() {
        final MigratedDefendant defendant =
                buildDefendantWithCustodyStatus("Mr", UUID.randomUUID(), CUSTODY_STATUS, LocalDate.parse("2008-05-05", DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, "D","description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertNull(personDefendant.getBailStatus());
    }

    @Test
    void shouldConvertWhenCustodyStatusInCustodyAndCustodyTimeLimitNull() {
        final MigratedDefendant defendant =
                buildDefendantWithCustodyStatus("Mr", UUID.randomUUID(), CUSTODY_STATUS, null);

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, "C","description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertNull(personDefendant.getBailStatus().getCustodyTimeLimit());
    }

    @Test
    void shouldHandleContactDetails() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNotNull(personDefendant.getPersonDetails().getContact());
        assertThat(personDefendant.getPersonDetails().getContact().getHome(), is("homePhone"));
        assertThat(personDefendant.getPersonDetails().getContact().getWork(), is("workPhone"));
        assertThat(personDefendant.getPersonDetails().getContact().getMobile(), is("mobile"));
        assertThat(personDefendant.getPersonDetails().getContact().getPrimaryEmail(), is("primaryemail"));
        assertThat(personDefendant.getPersonDetails().getContact().getSecondaryEmail(), is("secondaryemail"));
    }

    @Test
    void shouldHandleAddress() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNotNull(personDefendant.getPersonDetails().getAddress());
        assertThat(personDefendant.getPersonDetails().getAddress().getAddress1(), is("66 Exeter Street"));
        assertThat(personDefendant.getPersonDetails().getAddress().getAddress2(), is("address line 2"));
        assertThat(personDefendant.getPersonDetails().getAddress().getPostcode(), is("M60 1NW"));
    }

    @Test
    void shouldHandleSelfDefinedInformation() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPersonDetails().getNationalityCode(), is("nationality"));
        assertThat(personDefendant.getPersonDetails().getAdditionalNationalityCode(), is("additionalNationality"));
    }

    @Test
    void shouldHandleNationality() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPersonDetails().getNationalityCode(), is("nationality"));
        assertThat(personDefendant.getPersonDetails().getAdditionalNationalityCode(), is("additionalNationality"));
    }

    @Test
    void shouldHandleDriverLicenseInformation() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getDriverNumber(), is("2362435"));
    }

    @Test
    void shouldHandleBailConditions() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNull(personDefendant.getBailConditions());
    }

    @Test
    void shouldHandleSpecificRequirements() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNull(personDefendant.getPersonDetails().getSpecificRequirements());
    }

    @Test
    void shouldHandleLanguageRequirement() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPersonDetails().getInterpreterLanguageNeeds(), is("No"));
    }

    @Test
    void shouldHandleOccupationInformation() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNull(personDefendant.getPersonDetails().getOccupation());
        assertNull(personDefendant.getPersonDetails().getOccupationCode());
    }

    @Test
    void shouldHandleMiddleName() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNull(personDefendant.getPersonDetails().getMiddleName());
    }

    @Test
    void shouldHandleDateOfBirth() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPersonDetails().getDateOfBirth(), is("1989-04-18"));
    }

    @Test
    void shouldHandlePerceivedBirthYear() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPerceivedBirthYear(), is(1970));
    }

    @Test
    void shouldHandleInvalidPerceivedBirthYear() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertThat(personDefendant.getPerceivedBirthYear(), is(1970));
    }

    @Test
    void shouldHandleNullReferenceData() {
        final MigratedDefendant defendant = buildDefendantWithTitle("MR");
        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();

        try {
            PersonDefendant personDefendant = converter.convert(defendant, null);

            assertNotNull(personDefendant);
            assertNull(personDefendant.getBailStatus());
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("Cannot invoke \"uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO.getBailStatusReferenceData()\" because \"referenceDataVO\" is null"));
        }
    }

    @Test
    void shouldHandleEmptyBailStatusReferenceData() {
        final MigratedDefendant defendant = buildDefendantWithCustodyStatus("Mr", UUID.randomUUID(), CUSTODY_STATUS, LocalDate.parse("2026-01-29", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(new ArrayList<>());

        converter = new ProsecutionMigrationCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertNotNull(personDefendant);
        assertNull(personDefendant.getBailStatus());
    }



}