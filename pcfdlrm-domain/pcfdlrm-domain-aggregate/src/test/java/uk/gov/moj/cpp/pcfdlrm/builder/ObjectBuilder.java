package uk.gov.moj.cpp.pcfdlrm.builder;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT_IDENDIFIER;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;


import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigrationSourceSystem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class ObjectBuilder {


    public static MigratedCaseDetails buildMigratedCaseDetails(final CaseDetails caseDetails, final String defendantGender, final String parentGuardianGender, final String documentationLanguage, final String hearingLanguage, final String offenceCode, final String pleaCode, final LocalDate pleaDate) {
        return MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(CaseDetails.caseDetails()
                        .withValuesFrom(caseDetails)
                        .withReceivingCourt("C50EX00")
                        .withSendingCourt("B01LY00")
                        .withDateOfSending(LocalDate.now().minusYears(1))
                        .build())
                .withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemCaseIdentifier(SOURCE_SYSTEM_XHIBIT_IDENDIFIER)
                        .withMigrationSourceSystemName(SOURCE_SYSTEM_XHIBIT)
                        .build())
                .withDefendants(getMigratedDefendants(defendantGender, parentGuardianGender, documentationLanguage, hearingLanguage, offenceCode, pleaCode, pleaDate))

                .build();
    }

    private static List<MigratedDefendant> getMigratedDefendants(final String defendantGender, final String parentGuardianGender, final String documentationLanguage, final String hearingLanguage, final String offenceCode, final String pleaCode, final LocalDate pleaDate) {
        final MigratedPlea.Builder migratedPleaBuilder = MigratedPlea.migratedPlea();

        final boolean hasPlea = pleaCode != null && !pleaCode.trim().isEmpty();

        if (hasPlea) {
            migratedPleaBuilder.withId(randomUUID());
        }

        if (pleaDate != null) {
            migratedPleaBuilder.withPleaDate(pleaDate);
        }

        final MigratedPlea plea = hasPlea ? migratedPleaBuilder.build() : null;

        final MigratedDefendant.Builder builder = MigratedDefendant.migratedDefendant()
                .withId(DEFENDANT_ID)
                .withDocumentationLanguage(documentationLanguage)
                .withHearingLanguage(hearingLanguage)
                .withIndividual(Individual.individual()
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withNationality("HUN")
                                .withAdditionalNationality("SVK")
                                .withEthnicity("British")
                                .withGender(defendantGender)
                                .build())
                        .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                .withGender(parentGuardianGender)
                                .withDateOfBirth(LocalDate.now().minusYears(20))
                                .build())
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withFirstName("John")
                                .withLastName("Smith")
                                .withTitle("Mr")
                                .build()).build());
        if (offenceCode != null && !offenceCode.trim().isEmpty()) {
            builder.withOffences(Collections.singletonList(
                    MigratedOffence.migratedOffence()
                            .withOffenceCode(offenceCode)
                            .withOffenceId(randomUUID())
                            .withOffenceSequenceNumber(1)
                            .withPlea(plea)
                            .build()));


        }

        return Collections.singletonList(builder.build());
    }

    public static Prosecution buildProsecution(Prosecution prosecution, MigratedCaseDetails migratedCaseDetails) {
        return Prosecution.prosecution()
                .withValuesFrom(prosecution)
                .withDefendants(migratedCaseDetails.getDefendants())
                .withChannel(DLRM_MIGRATION)
                .build();
    }

    public static ReceiveMigratedCaseFile buildReceiveMigratedCaseFile(MigratedCaseDetails migratedCaseDetails, List<MigratedMaterial> migratedMaterials) {
        return ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withMaterials(migratedMaterials)
                .withMigratedCaseDetails(migratedCaseDetails)
                .withSubmissionId(randomUUID())
                .withChannel(Channel.DLRM_MIGRATION)
                .build();
    }
}
