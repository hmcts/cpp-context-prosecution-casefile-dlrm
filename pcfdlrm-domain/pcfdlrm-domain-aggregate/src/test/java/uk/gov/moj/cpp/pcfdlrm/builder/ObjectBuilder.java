package uk.gov.moj.cpp.pcfdlrm.builder;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
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
import java.util.UUID;

/**
 * Builds the migrated-case test inputs. Every value this builder mints is fixed: two calls with
 * identical arguments produce byte-identical output, so a serialised payload can be pinned against a
 * committed fixture. Nothing here reads the clock and nothing here calls {@code randomUUID()}.
 */
public class ObjectBuilder {

    /**
     * Fixed stand-ins for the values this builder used to mint per call. Dates are literals, not
     * offsets from {@code now()}: {@code DATE_OF_SENDING} and {@code PARENT_GUARDIAN_DATE_OF_BIRTH}
     * are both read by validation rules and both must stay in the past, which a literal guarantees
     * for as long as the suite exists.
     */
    private static final LocalDate DATE_OF_SENDING = LocalDate.of(2024, 1, 15);
    private static final LocalDate PARENT_GUARDIAN_DATE_OF_BIRTH = LocalDate.of(1980, 6, 1);
    private static final UUID PLEA_ID = fromString("e1e1e1e1-1111-4111-8111-111111111111");
    private static final UUID OFFENCE_ID = fromString("e2e2e2e2-2222-4222-8222-222222222222");
    private static final UUID SUBMISSION_ID = fromString("e3e3e3e3-3333-4333-8333-333333333333");

    public static MigratedCaseDetails buildMigratedCaseDetails(final CaseDetails caseDetails, final String defendantGender, final String parentGuardianGender, final String documentationLanguage, final String hearingLanguage, final String offenceCode, final String pleaCode, final LocalDate pleaDate, final SourceSystem sourceSystem) {
        return MigratedCaseDetails.migratedCaseDetails()
                .withCaseDetails(CaseDetails.caseDetails()
                        .withValuesFrom(caseDetails)
                        .withReceivingCourt("C50EX00")
                        .withSendingCourt("B01LY00")
                        .withDateOfSending(DATE_OF_SENDING)
                        .build())
                .withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemCaseIdentifier(sourceSystem.migrationSourceSystemCaseIdentifier())
                        .withMigrationSourceSystemName(sourceSystem.migrationSourceSystemName())
                        .build())
                .withDefendants(getMigratedDefendants(defendantGender, parentGuardianGender, documentationLanguage, hearingLanguage, offenceCode, pleaCode, pleaDate))

                .build();
    }

    private static List<MigratedDefendant> getMigratedDefendants(final String defendantGender, final String parentGuardianGender, final String documentationLanguage, final String hearingLanguage, final String offenceCode, final String pleaCode, final LocalDate pleaDate) {
        final MigratedPlea.Builder migratedPleaBuilder = MigratedPlea.migratedPlea();

        final boolean hasPlea = pleaCode != null && !pleaCode.trim().isEmpty();

        if (hasPlea) {
            migratedPleaBuilder.withId(PLEA_ID);
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
                                .withDateOfBirth(PARENT_GUARDIAN_DATE_OF_BIRTH)
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
                            .withOffenceId(OFFENCE_ID)
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
                .withSubmissionId(SUBMISSION_ID)
                .withChannel(Channel.DLRM_MIGRATION)
                .build();
    }
}
