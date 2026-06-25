package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static java.util.stream.Stream.builder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.pcfdlrm.CaseType.CC;
import static uk.gov.moj.cpp.pcfdlrm.ProsecutionCaseFileHelper.buildDefendantWithReferenceData;
import static uk.gov.moj.cpp.pcfdlrm.ProsecutionCaseFileHelper.buildMigratedHearingRefData;
import static uk.gov.moj.cpp.pcfdlrm.ProsecutionCaseFileHelper.validateDefendantErrors;
import static uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedCreationPending.migratedCaseValidatedCreationPending;
import static uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings.migratedCaseValidatedWithWarnings;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANT_COUNT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.NO_MATCHING_DEFENDANTS_FOR_HEARING;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PLEA_DATE_ABSENT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PLEA_DATE_CANNOT_BE_FUTURE_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PROSECUTOR_OUCODE_NOT_RECOGNISED;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.RECEIPT_TYPE_IS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.VERDICT_DATE_ABSENT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.VERDICT_DATE_CANNOT_BE_FUTURE_DATE;
import static uk.gov.moj.cpp.pcfdlrm.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.pcfdlrm.validation.provider.CcProsecutionValidationRuleProvider.getCaseValidationRules;
import static uk.gov.moj.cpp.pcfdlrm.validation.provider.CcProsecutionValidationRuleProvider.getMigratedHearingValidationRules;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.CASE_LEVEL;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategoryLevel.DEFENDANT_LEVEL;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded.materialAdded;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAddedPendingProcess.materialAddedPendingProcess;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialReadyForCourtDocument.materialReadyForCourtDocument;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialRejected.materialRejected;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed.migratedCaseFileProcessed;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.pcfdlrm.CaseType;
import uk.gov.moj.cpp.pcfdlrm.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedMaterialsWithOriginatingSystem;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedCreationPending;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.pcfdlrm.validation.provider.MaterialFileTypwWithCountValidationRuleProvider;
import uk.gov.moj.cpp.pcfdlrm.validation.provider.MaterialValidationRuleProvider;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CourtDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DefendantDocument;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAddedPendingProcess;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialReadyForCourtDocument;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseNotFoundInAutomation;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class MigratedCaseFileAggregate implements Aggregate {

    @Serial
    private static final long serialVersionUID = -6786924305887766569L;
    private static final String XHIBIT = "XHIBIT";
    private static final String MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION = "Migrated case not found in Automation";
    public static final String NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING = "No matching defendants with hearings found for the hearing";
    public static final String OFFENCE_VALIDATION = "Offence validation";
    public static final String HEARING_VALIDATION = "Hearing validation";
    public static final String INVALID_OFFENCE_CODE = "Invalid offence code";
    public static final String MISSING_OR_INVALID_PLEA_DATE = "Missing or Invalid plea date";
    public static final String MISSING_OR_INVALID_VERDICT_DATE = "Missing or Invalid verdict date";
    public static final String COURT_RECORD_SHEET_NOT_PDF = "Court Record Sheet must be a PDF file";
    public static final String COURT_RECORD_SHEET_FILE_TYPE_INVALID = "Court Record Sheet file type is not valid for XHIBIT migration";
    public static final String COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANTS = "Number of Court Record Sheets exceeds number of defendants";

    private UUID submissionId;

    private ReceiveMigratedCaseFile receiveMigratedCaseFile;

    private final List<MaterialAdded> materialsAdded = new ArrayList<>();

    private final Map<UUID, CourtDocument> materialsAddedPostProcessing = new HashMap<>();

    private final Map<UUID, CourtDocument> materialsReadyForCourtDocument = new HashMap<>();

    private MigratedCaseValidatedCreationPending migratedCaseValidatedCreationPending;

    private MigratedCaseFileProcessed migratedCaseFileProcessed;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(MigratedCaseFileReceived.class).apply(e -> {
                    this.submissionId = e.getReceiveMigratedCaseFile().getSubmissionId();
                    this.receiveMigratedCaseFile = e.getReceiveMigratedCaseFile();
                }),
                when(MaterialAdded.class).apply(materialsAdded::add),
                when(MigratedCaseValidatedCreationPending.class).apply(this::accept),
                when(MaterialAddedPendingProcess.class).apply(e -> this.materialsAddedPostProcessing.put(e.getMaterialId(), e.getCourtDocument())),
                when(MaterialReadyForCourtDocument.class).apply(e -> this.materialsReadyForCourtDocument.put(e.getMaterialId(), e.getCourtDocument())),
                when(MigratedCaseFileProcessed.class).apply(this::acceptMigratedcaseFleProcessed),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> receiveMigratedCaseFile(CaseProcessingArgs caseProcessingArgs) {
        ReceiveMigratedCaseFile receiveMigratedCaseFile = caseProcessingArgs.getReceiveMigratedCaseFile();
        final ProsecutionWithReferenceData receivedProsecutionWithReferenceData = caseProcessingArgs.getReceivedProsecutionWithReferenceData();
        final List<CaseRefDataEnricher> caseRefDataEnrichers = caseProcessingArgs.getCaseRefDataEnrichers();
        final List<DefendantRefDataEnricher> defendantRefDataEnrichers = caseProcessingArgs.getDefendantRefDataEnrichers();
        final ReferenceDataQueryService referenceDataQueryService = caseProcessingArgs.getReferenceDataQueryService();
        final Map<String, ImmutablePair<String, String>> sections = caseProcessingArgs.getSections();
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = caseProcessingArgs.getDocumentMetadataReferenceDataList();
        final List<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers = caseProcessingArgs.getMigratedHearingRefDataEnrichers();


        final Stream.Builder<Object> builder = builder();


        final String sourceSystemName = receiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName();

        final Prosecution prosecution = receivedProsecutionWithReferenceData.getProsecution();
        final Channel prosecutionChannel = prosecution.getChannel();
        final String receivedInitiationCode = prosecution.getCaseDetails().getInitiationCode();

        caseRefDataEnrichers.forEach(x -> x.enrich(receivedProsecutionWithReferenceData));

        final DefendantsWithReferenceData defendantsWithReferenceData =
                buildDefendantWithReferenceData(
                        receivedProsecutionWithReferenceData,
                        defendantRefDataEnrichers,
                        receiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName());


        final MigratedCaseDetails migratedCaseDetails = receiveMigratedCaseFile.getMigratedCaseDetails();

        final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList = new ArrayList<>();
        final List<Problem> materialsProblems = validate(new MigratedMaterialsWithOriginatingSystem(receiveMigratedCaseFile.getMaterials(), migratedCaseDetails.getMigrationSourceSystem().getMigrationSourceSystemName(),  sections, migratedCaseDetails.getDefendants().size()),
                        referenceDataQueryService,
                        MaterialFileTypwWithCountValidationRuleProvider.getRejectionRules(CC));


        if (hasInvalidFileTypeForXhibit(materialsProblems)) {
            builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                    .withDescription(COURT_RECORD_SHEET_NOT_PDF)
                    .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                    .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                    .withProcessingIsSuccessful(false)
                    .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                    .build());
            return apply(builder.build());
        }

        if (hasInvalidFileTypeForXhibitMigration(materialsProblems)) {
            builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                    .withDescription(COURT_RECORD_SHEET_FILE_TYPE_INVALID)
                    .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                    .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                    .withProcessingIsSuccessful(false)
                    .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                    .build());
            return apply(builder.build());
        }

        if (hasCourtRecordSheetCountExceedsDefendantCount(materialsProblems)) {
            builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                    .withDescription(COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANTS)
                    .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                    .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                    .withProcessingIsSuccessful(false)
                    .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                    .build());
            return apply(builder.build());
        }

        final List<Problem> caseProblems = validate(
                receivedProsecutionWithReferenceData,
                referenceDataQueryService, getCaseValidationRules(receivedInitiationCode));

        if (isNotEmpty(caseProblems) && isXhibit(receiveMigratedCaseFile)) {

            if (hasInvalidOuCode(caseProblems)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription("Either Sending or Receiving Court not found")
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return apply(builder.build());
            }

            if (hasInvalidReceiptTypes(caseProblems)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription("Invalid receipt types")
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return apply(builder.build());
            }

            if (hasInvalidProsecutingAuthority(caseProblems)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription("Invalid Prosecuting Authority")
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return apply(builder.build());
            }
        }

        final HearingValidationResult hearingValidationResult = validateHearings(receiveMigratedCaseFile, referenceDataQueryService, migratedHearingRefDataEnrichers, migratedCaseDetails, migratedHearingWithReferenceDataList);
        final List<Problem> hearingsProblems = hearingValidationResult.problems();

        if (hasNoMatchingDefendantsForXhibitHearing(hearingsProblems, receiveMigratedCaseFile)) {
            builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                    .withDescription(NO_MATCHING_DEFENDANTS_WITH_HEARINGS_FOUND_FOR_HEARING)
                    .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                    .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                    .withProcessingIsSuccessful(false)
                    .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                    .build());
            return apply(builder.build());
        }

        final MigratedDefendantWithProblem migratedDefendantWithProblem = validateDefendantErrors(prosecution.getCaseDetails(),
                prosecutionChannel, defendantsWithReferenceData,
                referenceDataQueryService, builder, false, sourceSystemName);

        if (hasOffenceProblems(receiveMigratedCaseFile, migratedDefendantWithProblem, builder, migratedCaseDetails)) {
            return apply(builder.build());
        }

        if (isNotEmpty(caseProblems) && isXhibit(receiveMigratedCaseFile)) {
            final List<MigratedCaseValidatedWithWarnings> caseValidationWarningsList = caseProblems.stream()
                    .filter(problem -> problem.getCode().equalsIgnoreCase(ProblemCode.CASE_MARKER_IS_INVALID.name()))
                    .map(problem -> migratedCaseValidatedWithWarnings()
                            .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                            .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                            .withType("Case validation")
                            .withWarnings(problem.getCode() + " : " + problem.getValues().stream().map(ProblemValue::getValue).toList())
                            .build())
                    .toList();

            caseValidationWarningsList.forEach(builder::add);
        }

        generateXhibitHearingWarnings(receiveMigratedCaseFile, hearingsProblems, builder, migratedCaseDetails);

        if (hasXhibitDefendantProblems(migratedDefendantWithProblem, receiveMigratedCaseFile)) {

            final List<MigratedCaseValidatedWithWarnings> migratedCaseValidatedWithWarningsList = generateOffenceWarnings(
                    migratedCaseDetails.getCaseDetails().getCaseId(),
                    migratedCaseDetails.getCaseDetails().getProsecutorCaseReference(),
                    migratedDefendantWithProblem.getDefendantProblems());

            migratedCaseValidatedWithWarningsList.forEach(builder::add);
        }

        final List<MigratedDefendant> migratedDefendants = migratedDefendantWithProblem.getMigratedDefendants();
        UUID caseId = receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails().getCaseId();

        String prosecutingAuthority = getProsecutingAuthority(receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails());
        String prosecutorDefendantId = receiveMigratedCaseFile.getMigratedCaseDetails().getDefendants().get(0).getId().toString();

        final List<MigratedHearing> defaultedHearings = hearingValidationResult.hearings();

        receiveMigratedCaseFile = ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                .withValuesFrom(receiveMigratedCaseFile)
                .withMigratedCaseDetails(MigratedCaseDetails.migratedCaseDetails()
                        .withValuesFrom(migratedCaseDetails)
                        .withDefendants(migratedDefendants)
                        .withHearings(defaultedHearings)
                        .build()).build();

        final ReceiveMigratedCaseFile finalReceiveMigratedCaseFile = receiveMigratedCaseFile;
        Optional.ofNullable(receiveMigratedCaseFile.getMaterials())
                .filter(materials -> !materials.isEmpty())
                .ifPresentOrElse(
                        materials -> {
                            materials
                                    .forEach(migratedMaterial -> {
                                        MaterialAdded materialAdded = addMaterial(caseId, prosecutingAuthority, prosecutorDefendantId, migratedMaterial, false, ZonedDateTime.now(),
                                                referenceDataQueryService, sections, finalReceiveMigratedCaseFile.getMigratedCaseDetails().getDefendants(), documentMetadataReferenceDataList, CC);
                                        if (materialAdded != null) {
                                            builder.add(materialAdded);
                                        }
                                    });


                            builder.add(migratedCaseValidatedCreationPending()
                                    .withMigratedCaseSubmission( ReceiveMigratedCaseFile.receiveMigratedCaseFile()
                                            .withValuesFrom(finalReceiveMigratedCaseFile)
                                            .withMigratedCaseDetails(MigratedCaseDetails.migratedCaseDetails()
                                                    .withValuesFrom(migratedCaseDetails)
                                                    .withDefendants(migratedDefendants)
                                                    .withHearings(defaultedHearings)
                                                    .build()).build())
                                    .withProsecutionWithReferenceData(receivedProsecutionWithReferenceData)
                                    .withMigratedHearingWithReferenceData(migratedHearingWithReferenceDataList)
                                    .build());
                        },
                        () -> {
                            if (isXhibit(finalReceiveMigratedCaseFile)) {
                                builder.add(MigratedCaseFileReceived.migratedCaseFileReceived()
                                        .withMigratedCaseSubmission(finalReceiveMigratedCaseFile)
                                        .withReferenceDataVO(receivedProsecutionWithReferenceData.getReferenceDataVO())
                                        .withMigratedHearingWithReferenceData(migratedHearingWithReferenceDataList)
                                        .build());
                            }
                        }
                );

        return apply(builder.build());
    }

    private record HearingValidationResult(List<MigratedHearing> hearings, List<Problem> problems) {}

    private HearingValidationResult validateHearings(final ReceiveMigratedCaseFile receiveMigratedCaseFile, final ReferenceDataQueryService referenceDataQueryService, final List<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers, final MigratedCaseDetails migratedCaseDetails, final List<MigratedHearingWithReferenceData> migratedHearingWithReferenceDataList) {
        if (isNotEmpty(migratedCaseDetails.getHearings())) {
            final List<MigratedHearing> updatedHearings = new ArrayList<>();
            final List<Problem> allProblems = new ArrayList<>();
            for (final MigratedHearing hearing : migratedCaseDetails.getHearings()) {
                final MigratedHearingWithReferenceData refData = buildMigratedHearingRefData(
                        migratedHearingRefDataEnrichers,
                        migratedCaseDetails.getCaseDetails(),
                        hearing,
                        receiveMigratedCaseFile.getMigratedCaseDetails().getDefendants());
                final List<Problem> problems = validate(refData, referenceDataQueryService, getMigratedHearingValidationRules());
                allProblems.addAll(problems);
                final MigratedHearing effectiveHearing = (problems.isEmpty() && isFixedHearing(hearing) && hearing.getTimeOfHearing() == null)
                        ? MigratedHearing.migratedHearing().withValuesFrom(hearing).withTimeOfHearing(toDefaultUtcTime(hearing.getDateOfHearing())).build()
                        : hearing;
                updatedHearings.add(effectiveHearing);
                refData.setMigratedHearing(effectiveHearing);
                migratedHearingWithReferenceDataList.add(refData);
            }
            return new HearingValidationResult(updatedHearings, allProblems);
        }
        return new HearingValidationResult(migratedCaseDetails.getHearings(), List.of());
    }

    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static boolean isFixedHearing(final MigratedHearing hearing) {
        return hearing.getWeekCommencingDate() == null
                && isNotBlank(hearing.getDateOfHearing())
                && hearing.getCourtRoomId() != null
                && hearing.getCourtRoomId() > 0;
    }

    private static String toDefaultUtcTime(final String dateOfHearing) {
        return LocalDateTime.of(LocalDate.parse(dateOfHearing), LocalTime.of(10, 0))
                .atZone(LONDON)
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(TIME_FORMAT);
    }

    private void generateXhibitHearingWarnings(final ReceiveMigratedCaseFile receiveMigratedCaseFile, final List<Problem> hearingsProblems, final Stream.Builder<Object> builder, final MigratedCaseDetails migratedCaseDetails) {
        if (isNotEmpty(hearingsProblems) && isXhibit(receiveMigratedCaseFile)) {
            final List<MigratedCaseValidatedWithWarnings> migratedCaseValidatedWithWarningsList = generateHearingsWarnings(
                    migratedCaseDetails.getCaseDetails().getCaseId(),
                    migratedCaseDetails.getCaseDetails().getProsecutorCaseReference(),
                    hearingsProblems);
            migratedCaseValidatedWithWarningsList.forEach(builder::add);
        }
    }

    private boolean hasOffenceProblems(final ReceiveMigratedCaseFile receiveMigratedCaseFile, final MigratedDefendantWithProblem migratedDefendantWithProblem, final Stream.Builder<Object> builder, final MigratedCaseDetails migratedCaseDetails) {
        if(isXhibit(receiveMigratedCaseFile)) {

            if (hasInvalidOffenceCode(migratedDefendantWithProblem)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription(INVALID_OFFENCE_CODE)
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return true;
            }

            if(hasInvalidPleaDate(migratedDefendantWithProblem)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription(MISSING_OR_INVALID_PLEA_DATE)
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return true;
            }

            if(hasInvalidVerdictDate(migratedDefendantWithProblem)) {
                builder.add(MigratedCaseFileProcessed.migratedCaseFileProcessed()
                        .withDescription(MISSING_OR_INVALID_VERDICT_DATE)
                        .withCaseId(migratedCaseDetails.getCaseDetails().getCaseId())
                        .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                        .withProcessingIsSuccessful(false)
                        .withCaseUrn(migratedCaseDetails.getCaseDetails().getProsecutorCaseReference())
                        .build());

                return true;
            }
        }
        return false;
    }


    private List<MigratedCaseValidatedWithWarnings> generateHearingsWarnings(UUID caseId, String caseUrn, final List<Problem> problems) {

        return problems.stream()
                .map(problem -> migratedCaseValidatedWithWarnings()
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .withType(HEARING_VALIDATION)
                        .withWarnings(problem.getCode() + " : " + problem.getValues().stream().map(ProblemValue::getValue).toList())
                        .build())
                .toList();
    }


    private List<MigratedCaseValidatedWithWarnings> generateOffenceWarnings(UUID caseId, String caseUrn, final List<DefendantProblem> defendantProblems) {
       List<String> problemsToRaise = List.of(ProblemCode.INVALID_PLEA.name(),ProblemCode.PLEA_DATE_ABSENT.name(),ProblemCode.PLEA_DATE_CANNOT_BE_FUTURE_DATE.name(),ProblemCode.CONVICTION_DATE_ABSENT.name(),
               ProblemCode.INVALID_VERDICT.name(), VERDICT_DATE_ABSENT.name());
        List< Problem> problems = defendantProblems.stream().flatMap(e->e.getProblems().stream()).filter(e-> problemsToRaise.contains(e.getCode())).toList();

        return problems.stream()
                .map(problem -> migratedCaseValidatedWithWarnings()
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .withType(OFFENCE_VALIDATION)
                        .withWarnings(problem.getCode() + " : " + problem.getValues().stream().map(ProblemValue::getValue).toList())
                        .build())
                .toList();
    }

    private boolean hasInvalidOffenceCode(final MigratedDefendantWithProblem migratedDefendantWithProblem) {
        final List<Problem> errors = migratedDefendantWithProblem.getDefendantProblems().stream()
                .flatMap(e -> e.getProblems().stream())
                .filter(e -> e.getCode().equals(OFFENCE_CODE_IS_INVALID.name()))
                .toList();
        return !errors.isEmpty();
    }

    private boolean hasInvalidPleaDate(final MigratedDefendantWithProblem migratedDefendantWithProblem) {
        final List<Problem> errors = migratedDefendantWithProblem.getDefendantProblems().stream()
                .flatMap(e -> e.getProblems().stream())
                .filter(e -> e.getCode().equals(PLEA_DATE_ABSENT.name()) || e.getCode().equals(PLEA_DATE_CANNOT_BE_FUTURE_DATE.name()))
                .toList();
        return !errors.isEmpty();
    }

    private boolean hasInvalidVerdictDate(final MigratedDefendantWithProblem migratedDefendantWithProblem) {
        final List<Problem> errors = migratedDefendantWithProblem.getDefendantProblems().stream()
                .flatMap(e -> e.getProblems().stream())
                .filter(e -> e.getCode().equals(VERDICT_DATE_ABSENT.name()) ||  e.getCode().equals(VERDICT_DATE_CANNOT_BE_FUTURE_DATE.name()))
                .toList();
        return !errors.isEmpty();
    }

    private boolean isXhibit(final ReceiveMigratedCaseFile receiveMigratedCaseFile) {
        return receiveMigratedCaseFile.getMigratedCaseDetails().getMigrationSourceSystem().getMigrationSourceSystemName().equals(XHIBIT);
    }

    private boolean hasInvalidOuCode(final List<Problem> caseProblems) {
        return caseProblems.stream().anyMatch(e -> e.getCode().equals(COURT_LOCATION_OUCODE_INVALID.name()));
    }

    private boolean hasInvalidProsecutingAuthority(final List<Problem> caseProblems) {
        return caseProblems.stream().anyMatch(e -> e.getCode().equals(PROSECUTOR_OUCODE_NOT_RECOGNISED.name()));
    }

    private boolean hasInvalidReceiptTypes(final List<Problem> caseProblems) {
        return caseProblems.stream().anyMatch(e -> e.getCode().equals(RECEIPT_TYPE_IS_INVALID.name()));
    }

    private boolean hasInvalidFileTypeForXhibit(final List<Problem> materialsProblems) {
        return materialsProblems.stream().anyMatch(e -> e.getCode().equals(INVALID_FILE_TYPE_FOR_XHIBIT.name()));
    }

    private boolean hasInvalidFileTypeForXhibitMigration(final List<Problem> materialsProblems) {
        return materialsProblems.stream().anyMatch(e -> e.getCode().equals(INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION.name()));
    }

    private boolean hasCourtRecordSheetCountExceedsDefendantCount(final List<Problem> materialsProblems) {
        return materialsProblems.stream().anyMatch(e -> e.getCode().equals(COURT_RECORD_SHEET_COUNT_EXCEEDS_DEFENDANT_COUNT.name()));
    }

    private boolean hasNoMatchingDefendantsForXhibitHearing(final List<Problem> hearingsProblems, final ReceiveMigratedCaseFile receiveMigratedCaseFile) {
        return hasNoMatchingDefendantsForHearing(hearingsProblems) && isXhibit(receiveMigratedCaseFile);
    }

    private boolean hasNoMatchingDefendantsForHearing(final List<Problem> problems) {
        return problems.stream().anyMatch(p -> p.getCode().equals(NO_MATCHING_DEFENDANTS_FOR_HEARING.name()));
    }

    private boolean hasXhibitDefendantProblems(final MigratedDefendantWithProblem migratedDefendantWithProblem, final ReceiveMigratedCaseFile receiveMigratedCaseFile) {
        return isNotEmpty(migratedDefendantWithProblem.getDefendantProblems()) && isXhibit(receiveMigratedCaseFile);
    }

    public Stream<Object> materialAddedPostProcessing(final CourtDocument courtDocument, final UUID materialId) {
        final Stream.Builder<Object> builder = builder();
        builder.add(materialAddedPendingProcess()
                .withCourtDocument(courtDocument)
                .withMaterialId(materialId)
                .build()
        );

        if (materialsAddedPostProcessing.size() == getMaterialsAdded().size() - 1) {
            builder.add(MigratedCaseFileReceived.migratedCaseFileReceived()
                    .withMigratedCaseSubmission(migratedCaseValidatedCreationPending.getReceiveMigratedCaseFile())
                    .withReferenceDataVO(migratedCaseValidatedCreationPending.getProsecutionWithReferenceData().getReferenceDataVO())
                    .withMigratedHearingWithReferenceData(migratedCaseValidatedCreationPending.getMigratedHearingWithReferenceDataList())
                    .build());
        }
        return apply(builder.build());
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public ReceiveMigratedCaseFile getReceiveMigratedCaseFile() {
        return receiveMigratedCaseFile;
    }

    @SuppressWarnings("squid:S00107")
    private MaterialAdded addMaterial(final UUID caseId, final String prosecutingAuthority,
                                      final String prosecutorDefendantId, final MigratedMaterial migratedMaterial, final Boolean isCpsCase,
                                      final ZonedDateTime receivedDateTime, final ReferenceDataQueryService referenceDataQueryService,
                                      final Map<String, ImmutablePair<String, String>> sections, final List<MigratedDefendant> defendants,
                                      final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList, CaseType caseType) {

        final ImmutablePair<String, String> fileType = Optional.ofNullable(sections.get(migratedMaterial.getFileType()))
                .orElseThrow(() -> new NotYetImplementedException("File type matching cps bundle code is not found in map"));
        Material material = Material.material()
                .withDocumentType(fileType.right)
                .withFileType(migratedMaterial.getFileType())
                .withFileCloudLocation(migratedMaterial.getAzureLocation())
                .build();
        Object materialEvent = validateMaterialWithDocumentDetails(caseId, prosecutingAuthority, prosecutorDefendantId, material,
                referenceDataQueryService, receivedDateTime, isCpsCase, sections.get(migratedMaterial.getFileType()).left, defendants,
                documentMetadataReferenceDataList, caseType);
        if (materialEvent instanceof MaterialAdded materialAdded) {
            return materialAdded;
        } else {
            throw new NotYetImplementedException("Only happy path implemented now");
        }
    }

    public Stream<Object> acceptMigratedCase() {
        final Stream.Builder<Object> builder = builder();

        if (Objects.isNull(receiveMigratedCaseFile)) {
            builder.add(MigratedCaseNotFoundInAutomation.migratedCaseNotFoundInAutomation().withDescription(MIGRATED_CASE_NOT_FOUND_IN_AUTOMATION).build());

        } else {

            materialsAddedPostProcessing.forEach((id, value) -> builder.add(materialReadyForCourtDocument()
                    .withMaterialId(id)
                    .withCourtDocument(CourtDocument.courtDocument()
                            .withValuesFrom(value)
                            .withDocumentCategory(DocumentCategory.documentCategory()
                                    .withValuesFrom(value.getDocumentCategory())
                                    .withDefendantDocument(DefendantDocument.defendantDocument()
                                            .withValuesFrom(value.getDocumentCategory().getDefendantDocument())
                                            .withDefendants(receiveMigratedCaseFile.getMigratedCaseDetails().getDefendants()
                                                    .stream().map(MigratedDefendant::getId).toList())
                                            .build())
                                    .build())
                            .build())
                    .build()));

            builder.add(migratedCaseFileProcessed()
                    .withCaseId(receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails().getCaseId())
                    .withCaseUrn(receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails().getProsecutorCaseReference())
                    .withSubmissionId(receiveMigratedCaseFile.getSubmissionId())
                    .withProcessingIsSuccessful(true)
                    .build());
        }
        return apply(builder.build());
    }

    /*
    This following exception wil either move outside the class or completely removed
    when we bring in unhappy path and raise material rejected event
     */
    public static class NotYetImplementedException extends RuntimeException {
        public NotYetImplementedException(String message) {
            super(message);
        }
    }

    private String getDocumentCategoryLevel(final String referenceDataDocumentCategory) {
        if (CASE_LEVEL.toString().equalsIgnoreCase(referenceDataDocumentCategory)) {
            return CASE_LEVEL.toString();
        } else {
            return DEFENDANT_LEVEL.toString();
        }
    }

    private String getProsecutingAuthority(CaseDetails caseDetails) {
        if (caseDetails != null && caseDetails.getProsecutor() != null) {
            return caseDetails.getProsecutor().getProsecutingAuthority();
        }
        return null;
    }

    List<MaterialAdded> getMaterialsAdded() {
        return materialsAdded;
    }

    private void accept(MigratedCaseValidatedCreationPending e) {
        migratedCaseValidatedCreationPending = e;
    }

    private void acceptMigratedcaseFleProcessed(MigratedCaseFileProcessed e) {
        migratedCaseFileProcessed = e;
    }

    public Map<UUID, CourtDocument> getMaterailsReadyForCourtDocuments() {
        return Collections.unmodifiableMap(this.materialsReadyForCourtDocument);
    }

    @SuppressWarnings("squid:S00107")
    private Object validateMaterialWithDocumentDetails(final UUID caseId, final String prosecutingAuthority, final String prosecutorDefendantId,
                                                       final Material material, final ReferenceDataQueryService referenceDataQueryService,
                                                       final ZonedDateTime receivedDateTime, final Boolean isCpsCase,
                                                       final String sectionCodeToSet, final List<MigratedDefendant> defendants,
                                                       final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList, CaseType caseType) {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData =
                new CaseDocumentWithReferenceData(null, false, material,
                        prosecutorDefendantId, defendants, material.getDocumentType(), false, false, documentMetadataReferenceDataList);

        final List<Problem> rejections = validate(caseDocumentWithReferenceData,
                referenceDataQueryService,
                MaterialValidationRuleProvider.getRejectionRules(caseType));

        String cmsDocumentId = null;

        if (rejections.isEmpty()) {
            final MaterialAdded.Builder materialAddedBuilder = materialAdded();

            if (caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
                materialAddedBuilder.withDocumentTypeId(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getId().toString());
                materialAddedBuilder.withDocumentCategory(getDocumentCategoryLevel(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory()));
            }

            return materialAddedBuilder
                    .withCaseId(caseId)
                    .withCaseType(caseType.name())
                    .withDefendantId(caseDocumentWithReferenceData.getDefendantId())
                    .withDefendantName("Defendant Name")
                    .withDocumentType(caseDocumentWithReferenceData.getDocumentType())
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withProsecutorDefendantId(prosecutorDefendantId)
                    .withMaterial(material)
                    .withIsCpsCase(isCpsCase)
                    .withReceivedDateTime(receivedDateTime)
                    .withSectionCode(sectionCodeToSet)
                    .build();
        } else {

            return materialRejected()
                    .withCaseId(caseId)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withProsecutorDefendantId(prosecutorDefendantId)
                    .withMaterial(material)
                    .withErrors(rejections)
                    .withIsCpsCase(isCpsCase)
                    .withCmsDocumentId(cmsDocumentId)
                    .withReceivedDateTime(receivedDateTime)
                    .build();
        }
    }

    Map<UUID, CourtDocument> getMaterialsAddedPostProcessing() {
        return materialsAddedPostProcessing;
    }

    MigratedCaseFileProcessed getMigratedCaseFileProcessed() {
        return migratedCaseFileProcessed;
    }

    public MigratedCaseValidatedCreationPending getMigratedCaseValidatedCreationPending() {
        return migratedCaseValidatedCreationPending;
    }
}
