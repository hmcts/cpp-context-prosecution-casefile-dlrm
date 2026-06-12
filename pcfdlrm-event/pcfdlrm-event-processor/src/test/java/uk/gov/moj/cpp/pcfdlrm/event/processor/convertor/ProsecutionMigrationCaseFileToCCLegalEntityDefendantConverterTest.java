package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverterTest {

    @InjectMocks
    private ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter converter;


    @Test
    public void shouldReturnNullWhenOrganisationNameIsNull() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName(null)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNull(result);
    }

    @Test
    public void shouldHandleEmptyOrganisationName() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("", result.getOrganisation().getName());
    }

    @Test
    public void shouldHandleWhitespaceOrganisationName() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("   ")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("   ", result.getOrganisation().getName());
    }

    @Test
    public void shouldConvertMigratedDefendantToLegalEntityDefendant() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withAddress(buildTestAddress())
                .withEmailAddress1("test1@example.com")
                .withEmailAddress2("test2@example.com")
                .withTelephoneNumberBusiness("123456789")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertEquals("test1@example.com", result.getOrganisation().getContact().getPrimaryEmail());
        assertEquals("test2@example.com", result.getOrganisation().getContact().getSecondaryEmail());
        assertEquals("123456789", result.getOrganisation().getContact().getWork());
        assertEquals("123 Test St", result.getOrganisation().getAddress().getAddress1());
    }

    @Test
    public void shouldHandleNullAddress() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withAddress(null)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNull(result.getOrganisation().getAddress());
    }

    @Test
    public void shouldHandleEmptyContactInformation() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1(null)
                .withEmailAddress2(null)
                .withTelephoneNumberBusiness(null)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNull(result.getOrganisation().getContact());
    }

    @Test
    public void shouldHandleOnlyPrimaryEmail() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1("test@example.com")
                .withEmailAddress2(null)
                .withTelephoneNumberBusiness(null)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertEquals("test@example.com", result.getOrganisation().getContact().getPrimaryEmail());
        assertNull(result.getOrganisation().getContact().getSecondaryEmail());
        assertNull(result.getOrganisation().getContact().getWork());
    }

    @Test
    public void shouldHandleOnlySecondaryEmail() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1(null)
                .withEmailAddress2("test2@example.com")
                .withTelephoneNumberBusiness(null)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNull(result.getOrganisation().getContact().getPrimaryEmail());
        assertEquals("test2@example.com", result.getOrganisation().getContact().getSecondaryEmail());
        assertNull(result.getOrganisation().getContact().getWork());
    }

    @Test
    public void shouldHandleOnlyTelephoneNumber() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1(null)
                .withEmailAddress2(null)
                .withTelephoneNumberBusiness("123456789")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNull(result.getOrganisation().getContact().getPrimaryEmail());
        assertNull(result.getOrganisation().getContact().getSecondaryEmail());
        assertEquals("123456789", result.getOrganisation().getContact().getWork());
    }

    @Test
    public void shouldHandleEmptyStringContactInformation() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1("")
                .withEmailAddress2("")
                .withTelephoneNumberBusiness("")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNull(result.getOrganisation().getContact());
    }

    @Test
    public void shouldHandleWhitespaceContactInformation() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1("   ")
                .withEmailAddress2("   ")
                .withTelephoneNumberBusiness("   ")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNotNull(result.getOrganisation().getContact());
        assertEquals("   ", result.getOrganisation().getContact().getPrimaryEmail());
        assertEquals("   ", result.getOrganisation().getContact().getSecondaryEmail());
        assertEquals("   ", result.getOrganisation().getContact().getWork());
    }

    @Test
    public void shouldHandleAddressWithNullFields() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withAddress(buildAddressWithNullFields())
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNotNull(result.getOrganisation().getAddress());
        assertEquals("123 Test St", result.getOrganisation().getAddress().getAddress1());
        assertNull(result.getOrganisation().getAddress().getAddress2());
        assertNull(result.getOrganisation().getAddress().getAddress3());
        assertNull(result.getOrganisation().getAddress().getAddress4());
        assertNull(result.getOrganisation().getAddress().getAddress5());
        assertNull(result.getOrganisation().getAddress().getPostcode());
    }

    @Test
    public void shouldHandleAddressWithEmptyFields() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withAddress(buildAddressWithEmptyFields())
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNotNull(result.getOrganisation().getAddress());
        assertEquals("123 Test St", result.getOrganisation().getAddress().getAddress1());
        assertEquals("", result.getOrganisation().getAddress().getAddress2());
        assertEquals("", result.getOrganisation().getAddress().getAddress3());
        assertEquals("", result.getOrganisation().getAddress().getAddress4());
        assertEquals("", result.getOrganisation().getAddress().getAddress5());
        assertEquals("", result.getOrganisation().getAddress().getPostcode());
    }

    @Test
    public void shouldHandleLongOrganisationName() {
        String longName = "A".repeat(1000);
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName(longName)
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals(longName, result.getOrganisation().getName());
    }

    @Test
    public void shouldHandleSpecialCharactersInOrganisationName() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test & Co. Ltd. - Special Characters: @#$%^&*()")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test & Co. Ltd. - Special Characters: @#$%^&*()", result.getOrganisation().getName());
    }

    @Test
    public void shouldHandleUnicodeCharactersInOrganisationName() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation with Unicode: 测试公司 🏢")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation with Unicode: 测试公司 🏢", result.getOrganisation().getName());
    }

    @Test
    public void shouldHandleMixedContactInformation() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withEmailAddress1("test@example.com")
                .withEmailAddress2("")
                .withTelephoneNumberBusiness("123456789")
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertEquals("test@example.com", result.getOrganisation().getContact().getPrimaryEmail());
        assertEquals("", result.getOrganisation().getContact().getSecondaryEmail());
        assertEquals("123456789", result.getOrganisation().getContact().getWork());
    }

    @Test
    public void shouldHandleCompleteAddressWithAllFields() {
        MigratedDefendant defendant = MigratedDefendant.migratedDefendant()
                .withOrganisationName("Test Organisation")
                .withAddress(buildCompleteTestAddress())
                .build();

        LegalEntityDefendant result = converter.convert(defendant);

        assertNotNull(result);
        assertEquals("Test Organisation", result.getOrganisation().getName());
        assertNotNull(result.getOrganisation().getAddress());
        assertEquals("123 Test St", result.getOrganisation().getAddress().getAddress1());
        assertEquals("Test Area", result.getOrganisation().getAddress().getAddress2());
        assertEquals("Test City", result.getOrganisation().getAddress().getAddress3());
        assertEquals("Test County", result.getOrganisation().getAddress().getAddress4());
        assertEquals("Test Country", result.getOrganisation().getAddress().getAddress5());
        assertEquals("TEST123", result.getOrganisation().getAddress().getPostcode());
    }

    private uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address buildTestAddress() {
        return uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address()
                .withAddress1("123 Test St")
                .withAddress2("Test Area")
                .withAddress3("Test City")
                .withAddress4("Test County")
                .withAddress5("Test Country")
                .withPostcode("TEST123")
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address buildAddressWithNullFields() {
        return uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address()
                .withAddress1("123 Test St")
                .withAddress2(null)
                .withAddress3(null)
                .withAddress4(null)
                .withAddress5(null)
                .withPostcode(null)
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address buildAddressWithEmptyFields() {
        return uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address()
                .withAddress1("123 Test St")
                .withAddress2("")
                .withAddress3("")
                .withAddress4("")
                .withAddress5("")
                .withPostcode("")
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address buildCompleteTestAddress() {
        return uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Address.address()
                .withAddress1("123 Test St")
                .withAddress2("Test Area")
                .withAddress3("Test City")
                .withAddress4("Test County")
                .withAddress5("Test Country")
                .withPostcode("TEST123")
                .build();
    }
}
