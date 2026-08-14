package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static java.util.Collections.singletonList;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarios.PLEA_DATE_ANCHOR;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildMigratedCaseDetails;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildProsecution;
import static uk.gov.moj.cpp.pcfdlrm.builder.ObjectBuilder.buildReceiveMigratedCaseFile;
import static uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem.sourceSystem;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.CASE_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.DEFENDANT_ID2;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT_IDENDIFIER;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.E;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.W;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The per-scenario {@link CaseFileInput} builders feeding {@link AggregateScenarios}' scenario
 * table — one method per named {@link AggregateScenario} row.
 */
final class AggregateScenarioInputs {

    private AggregateScenarioInputs() {
    }

    /**
     * Fixed ID, not {@code randomUUID()} — a {@code MaterialAdded} event carries whichever entry
     * matched, so a random ID here made every fixture built on it non-deterministic. Every material
     * in this test class uses {@code fileType "99"}, which {@code getSections()} maps to
     * {@code ("PSJH", "Private section - Judges & HMCTS")} — {@code CCDocumentTypeValidationRule}
     * matches on the {@code section} string, so this is the only entry any test can ever resolve to.
     */
    static List<MigratedMaterial> createMigratedMaterials(final int fileCount, final String fileType) {
        List<MigratedMaterial> migratedMaterials = new ArrayList<>();
        MigratedMaterial migratedMaterial1 = MigratedMaterial.migratedMaterial()
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID.toString())
                .withAzureLocation("azure/abc.pdf")
                .withDocumentType(3)
                .withFileName("abc." + fileType)
                .withFileType("99").build();
        MigratedMaterial migratedMaterial2 = MigratedMaterial.migratedMaterial()
                .withDefendantId(DEFENDANT_ID2.toString())
                .withCaseId(CASE_ID)
                .withAzureLocation("azure/def.pdf")
                .withDocumentType(3)
                .withFileName("def." + fileType)
                .withFileType("99").build();

        if (fileCount != 1) {
            migratedMaterials.add(migratedMaterial2);
        }

        migratedMaterials.add(migratedMaterial1);

        return migratedMaterials;
    }

    static CaseFileInput noMaterialsInput(final SourceSystem sourceSystem) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "MALE", E.name(), W.name(), null, null, null, sourceSystem);
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput nullMaterialsInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "MALE", E.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, null);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput courtRecordSheetCountExceedsInput() {
        final MigratedMaterial material1 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString()).withAzureLocation("azure/abc.pdf").withDocumentType(3).withFileName("abc.pdf").withFileType("99").build();
        final MigratedMaterial material2 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID2.toString()).withAzureLocation("azure/def.pdf").withDocumentType(3).withFileName("def.pdf").withFileType("99").build();
        final MigratedMaterial material3 = MigratedMaterial.migratedMaterial().withCaseId(CASE_ID).withDefendantId(DEFENDANT_ID.toString()).withAzureLocation("azure/ghi.pdf").withDocumentType(3).withFileName("ghi.pdf").withFileType("99").build();
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("FEMALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of(material1, material2, material3));
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(CaseDetails.caseDetails().withReceiptType("Either way case").build()).build();
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput courtCodeInvalidInput(final CaseDetails caseDetails) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, caseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput receiptTypeInput(final String receiptType) {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("FEMALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails().withReceiptType(receiptType).build());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput noMatchingDefendantsForHearingInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails migCaseDetailsWithHearing = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withHearings(List.of(MigratedHearing.migratedHearing().withListedDefendants(List.of()).build())).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithHearing);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithHearing, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput hearingDefendantMatchesNoOffencesInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedDefendant defendantWithNonMatchingOffences = migratedDefendant().withId(DEFENDANT_ID).withProsecutorDefendantId("DEF-001")
                .withOffences(List.of(migratedOffence().withOffenceId(UUID.fromString("b1b1b1b1-1111-4111-8111-111111111111")).withProsecutorOffenceId("OFF-001").withOffenceSequenceNumber(1).build())).build();
        final MigratedDefendant secondDefendantWithNonMatchingOffences = migratedDefendant().withId(DEFENDANT_ID2).withProsecutorDefendantId("DEF-002")
                .withOffences(List.of(migratedOffence().withOffenceId(UUID.fromString("b2b2b2b2-2222-4222-8222-222222222222")).withProsecutorOffenceId("OFF-002").withOffenceSequenceNumber(1).build())).build();
        final MigratedCaseDetails migCaseDetailsWithMismatch = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendantWithNonMatchingOffences, secondDefendantWithNonMatchingOffences))
                .withHearings(List.of(MigratedHearing.migratedHearing().withListedDefendants(List.of(
                        ListedDefendant.listedDefendant().withProsecutorDefendantId("DEF-001").withListedOffences(List.of("OFF-999")).build(),
                        ListedDefendant.listedDefendant().withProsecutorDefendantId("DEF-002").withListedOffences(List.of("OFF-888", "OFF-001")).build()
                )).build())).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithMismatch);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithMismatch, createMigratedMaterials(1, "pdf"));
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput invalidProsecutingAuthorityInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withProsecutor(Prosecutor.prosecutor().withProsecutingAuthority("NOTREG").build())
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, List.of());
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput invalidOffenceCodeInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "BadOffenceCode", null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput missingPleaDateInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").withPleaValue("Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput missingVerdictDateInput() {
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedOffence baseOffence = migCaseDetails.getDefendants().get(0).getOffences().get(0);
        final MigratedOffence offenceWithVerdict = MigratedOffence.migratedOffence().withValuesFrom(baseOffence)
                .withVerdict(MigratedVerdict.migratedVerdict().withId(UUID.fromString("f1f1f1f1-1111-4111-8111-111111111111")).build())
                .build();
        final MigratedDefendant defendantWithVerdict = MigratedDefendant.migratedDefendant().withValuesFrom(migCaseDetails.getDefendants().get(0))
                .withOffences(List.of(offenceWithVerdict)).build();
        final MigratedCaseDetails migCaseDetailsWithVerdict = MigratedCaseDetails.migratedCaseDetails().withValuesFrom(migCaseDetails)
                .withDefendants(List.of(defendantWithVerdict)).build();
        final Prosecution prosecution = buildProsecution(migCaseDetailsWithVerdict);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetailsWithVerdict, createMigratedMaterials(1, "pdf"));
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setVerdictReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offenceWithVerdict.getOffenceId(),
                VerdictReferenceData.verdictReferenceData().build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput allNullDefendantInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput guiltyPleaWithDateInput() {
        final List<MigratedMaterial> migratedMaterials = List.of(createMigratedMaterials(2, "pdf").get(0));
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaValue("Guilty").withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput notGuiltyPleaWithDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "NG", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaValue("Not Guilty").withPleaTypeCode("NG").withPleaTypeGuiltyFlag("No").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput guiltyPleaFutureDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        // Genuinely LocalDate.now() here, not PLEA_DATE_ANCHOR — this scenario specifically exercises
        // the PLEA_DATE_CANNOT_BE_FUTURE_DATE rule, which compares against the real clock at
        // execution time. A fixed anchor date would eventually stop being "in the future" and this
        // scenario would silently stop testing what its name says. The resulting non-deterministic
        // date is excluded from the whole-payload comparison below (see PLEA_FUTURE_DATE_EXCLUSIONS).
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "G", LocalDate.now().plusDays(1), sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").withPleaValue("Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput notGuiltyMissingDateInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "NG", null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final MigratedOffence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of(DEFENDANT_ID, Map.of(offence.getOffenceId(),
                PleaReferenceData.pleaReferenceData().withPleaTypeCode("NG").withPleaTypeGuiltyFlag("No").withPleaValue("Not Guilty").build())));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput badPleaCodeInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(List.of(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("998A").build()));
        referenceDataVO.setPleaReferenceDataMap(Map.of());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(null, null, null, null, "998A", "badPlea", PLEA_DATE_ANCHOR, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput genderProvidedInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails(MALE.name(), FEMALE.name(), W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput genderNotMatchInCpInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("NOTINCP", "NOTINCP", "NOTINCP", W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput courtValidInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", E.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withSendingCourt("AB00001")
                .withReceivingCourt("AB00001")
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.getReferenceDataVO().setSendingCourtOrganisationUnit(OrganisationUnitReferenceData.organisationUnitReferenceData().build());
        prosecutionWithReferenceData.getReferenceDataVO().setReceivingCourtOrganisationUnit(OrganisationUnitReferenceData.organisationUnitReferenceData().build());
        return new CaseFileInput(receiveMigratedCase, prosecutionWithReferenceData);
    }

    static CaseFileInput validReceiptTypeInput(final String receiptType) {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails().withReceiptType(receiptType).build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput caseMarkerInvalidInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withCaseMarkers(List.of(CaseMarker.caseMarker()
                        .withMarkerTypeId(UUID.fromString("c1c1c1c1-1111-4111-8111-111111111111"))
                        .withMarkerTypeCode("ABC001")
                        .withMarkerTypeDescription("Test Code")
                        .build()))
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput genderNotInCpInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput caseMarkerNullOrEmptyInput(final String markerTypeCode) {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails, CaseDetails.caseDetails()
                .withReceiptType("Either way case")
                .withCaseMarkers(List.of(CaseMarker.caseMarker()
                        .withMarkerTypeId(UUID.fromString("c2c2c2c2-2222-4222-8222-222222222222"))
                        .withMarkerTypeCode(markerTypeCode)
                        .withMarkerTypeDescription("Test Code")
                        .build()))
                .build());
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput parentGuardianNullInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("XXX", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(singletonList(MigratedDefendant.migratedDefendant()
                        .withValuesFrom(migCaseDetails.getDefendants().get(0))
                        .withIndividual(Individual.individual()
                                .withValuesFrom(migCaseDetails.getDefendants().get(0).getIndividual())
                                .withParentGuardianInformation(null)
                                .build())
                        .build()))
                .build();
        final Prosecution prosecution = buildProsecution(amendedMigratedDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput custodyCWithMissingCtlInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final MigratedCaseDetails amendedMigratedDetails = MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migCaseDetails)
                .withDefendants(singletonList(MigratedDefendant.migratedDefendant()
                        .withValuesFrom(migCaseDetails.getDefendants().get(0))
                        .withIndividual(Individual.individual()
                                .withValuesFrom(migCaseDetails.getDefendants().get(0).getIndividual())
                                .withCustodyStatus("C")
                                .withParentGuardianInformation(null)
                                .build())
                        .build()))
                .build();
        final Prosecution prosecution = buildProsecution(amendedMigratedDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(amendedMigratedDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput defendantLevelInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "YYYY", W.name(), null, null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput receivedWithMaterialInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "pdf");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", "FEMALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }

    static CaseFileInput nonPdfWithoutMaterialInput() {
        final List<MigratedMaterial> migratedMaterials = createMigratedMaterials(1, "doc");
        final MigratedCaseDetails migCaseDetails = buildMigratedCaseDetails("MALE", " MALE", W.name(), W.name(), null, null, null, sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER));
        final Prosecution prosecution = buildProsecution(migCaseDetails);
        final ReceiveMigratedCaseFile receiveMigratedCase = buildReceiveMigratedCaseFile(migCaseDetails, migratedMaterials);
        return new CaseFileInput(receiveMigratedCase, new ProsecutionWithReferenceData(prosecution));
    }
}

/** The command + reference data pair a single {@link AggregateScenario} feeds into the aggregate. */
record CaseFileInput(ReceiveMigratedCaseFile receiveMigratedCaseFile, ProsecutionWithReferenceData prosecutionWithReferenceData) {
}
