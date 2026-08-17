package uk.gov.moj.cpp.pcfdlrm.aggregate;

import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.allNullDefendantInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.badPleaCodeInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.caseMarkerInvalidInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.caseMarkerNullOrEmptyInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.courtCodeInvalidInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.courtRecordSheetCountExceedsInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.courtValidInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.custodyCWithMissingCtlInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.defendantLevelInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.genderNotInCpInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.genderNotMatchInCpInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.genderProvidedInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.guiltyPleaFutureDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.guiltyPleaWithDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.hearingDefendantMatchesNoOffencesInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.invalidOffenceCodeInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.invalidProsecutingAuthorityInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.missingPleaDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.missingVerdictDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.noMatchingDefendantsForHearingInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.noMaterialsInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.nonPdfWithoutMaterialInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.notGuiltyMissingDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.notGuiltyPleaWithDateInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.nullMaterialsInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.parentGuardianNullInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.receiptTypeInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.receivedWithMaterialInput;
import static uk.gov.moj.cpp.pcfdlrm.aggregate.AggregateScenarioInputs.validReceiptTypeInput;
import static uk.gov.moj.cpp.pcfdlrm.builder.SourceSystem.sourceSystem;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.CASE_ID;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SOURCE_SYSTEM_XHIBIT_IDENDIFIER;
import static uk.gov.moj.cpp.pcfdlrm.builder.TestConstants.SUBMISSION_ID;

import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedCreationPending;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseValidatedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.DefendantValidationFailed;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.MigratedCaseFileProcessed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * The scenario table for {@link MigratedCaseFileAggregateTest#shouldEmitExpectedEventsForScenario}
 * and {@link MigratedCaseFileAggregateTest#shouldDefaultHearingTimeTo10AmOnlyForFixedHearingWithNoWarnings} —
 * every {@code @MethodSource} supplier, the shared noise/exclusion constants both those tests and
 * the hand-written hearing-time tests rely on, and the {@link ExpectedEvent} factory helpers.
 * Per-scenario {@link CaseFileInput} construction lives in {@link AggregateScenarioInputs}.
 */
final class AggregateScenarios {

    static final String FUTURE_HEARING_DATE_GMT = nextFutureDate(1, 15);

    static final String FUTURE_HEARING_DATE_BST = nextFutureDate(6, 15);

    static final String FUTURE_WEEK_COMMENCING_START_DATE = nextFutureDate(6, 14);

    static final List<String> FUTURE_DATE_OF_HEARING_EXCLUSIONS = List.of(
            "receiveMigratedCaseFile.migratedCaseDetails.hearings[0].dateOfHearing",
            "migratedHearingWithReferenceDataList[0].migratedHearing.dateOfHearing");

    static final List<String> FUTURE_WEEK_COMMENCING_START_DATE_EXCLUSIONS = List.of(
            "receiveMigratedCaseFile.migratedCaseDetails.hearings[0].weekCommencingDate.startDate",
            "migratedHearingWithReferenceDataList[0].migratedHearing.weekCommencingDate.startDate");

    static final List<ExpectedEvent> HEARING_DEFENDANT_VALIDATION_NOISE = List.of(
            new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-hearing-defendant.json"),
            warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
            warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
            warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"));

    /** The ETHNICITY/NATIONALITY/CUSTODY-empty warning triple shared by most main-path scenarios below. */
    private static final List<ExpectedEvent> DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE = List.of(
            warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
            warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
            warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"));

    /**
     * {@code addMaterial}'s {@code receivedDateTime} is stamped from {@code ZonedDateTime.now()} in
     * {@code src/main} — out of scope to change here — so it is the one field every
     * {@code MaterialAdded} fixture must exclude; presence is still checked, only the value is
     * skipped (see {@code WholePayloadMatcher}).
     */
    static final List<String> MATERIAL_ADDED_EXCLUSIONS = List.of("receivedDateTime");

    /** Fixed, not {@code LocalDate.now()} — a plea date baked into a static whole-payload fixture is non-deterministic otherwise. */
    static final LocalDate PLEA_DATE_ANCHOR = LocalDate.of(2024, 1, 15);

    /**
     * For {@code guiltyPleaFutureDateInput()} only — that scenario needs a plea date genuinely in
     * the future relative to the real clock (see its comment), so its two date occurrences can't
     * be pinned to a fixed value and are excluded from the whole-payload comparison instead.
     */
    static final List<String> PLEA_FUTURE_DATE_EXCLUSIONS = List.of("defendant.offences[0].plea.pleaDate", "problems[2].values[0].value");

    private AggregateScenarios() {
    }

    /**
     * Next future occurrence of {@code month}/{@code day}, strictly after today. Keeps the GMT
     * (January) / BST (June) hearing-date scenarios below perpetually future — and perpetually in
     * the same DST season — rather than baking in a fixed year that lapses into the past once the
     * real clock catches up (2027-01-15/2027-06-15 would have started failing from 2027 onward).
     */
    private static String nextFutureDate(final int month, final int day) {
        final LocalDate today = LocalDate.now();
        LocalDate candidate = LocalDate.of(today.getYear(), month, day);
        if (!candidate.isAfter(today)) {
            candidate = candidate.plusYears(1);
        }
        return candidate.toString();
    }

    static Stream<Arguments> fixedHearingTimeDefaultingScenarios() {
        final List<ExpectedEvent> pastNoWarning = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        pastNoWarning.add(warning("Hearing validation", "DATE_OF_HEARING_IN_THE_PAST : [2026-03-05]"));
        pastNoWarning.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-past-no-warning.json"));

        final List<ExpectedEvent> futureWithTime = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureWithTime.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "09:30"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        final List<ExpectedEvent> futureNoTimeGmt = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureNoTimeGmt.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "10:00:00"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        final List<ExpectedEvent> futureNoTimeBst = new ArrayList<>(HEARING_DEFENDANT_VALIDATION_NOISE);
        futureNoTimeBst.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-hearing-future.json", Map.of("timeOfHearing", "09:00:00"), FUTURE_DATE_OF_HEARING_EXCLUSIONS));

        return Stream.of(
                Arguments.of("2026-03-05", null, "past dateOfHearing raises warning — should not default timeOfHearing", pastNoWarning),
                Arguments.of(FUTURE_HEARING_DATE_GMT, "09:30", "future dateOfHearing with existing time — should not overwrite timeOfHearing", futureWithTime),
                Arguments.of(FUTURE_HEARING_DATE_GMT, null, "GMT: future dateOfHearing with no time — should default to 10:00:00 UTC", futureNoTimeGmt),
                Arguments.of(FUTURE_HEARING_DATE_BST, null, "BST: future dateOfHearing with no time — should default to 09:00:00 UTC", futureNoTimeBst)
        );
    }

    static Stream<AggregateScenario> aggregateScenarios() {
        return Stream.of(xhibitGateScenarios(), failFastScenarios(), hasOffenceProblemsGateScenarios(), materialsMainPathScenarios(), defendantProblemsScenarios(), pleaScenarios(), genderCourtMarkerScenarios())
                .flatMap(s -> s);
    }

    private static Stream<AggregateScenario> xhibitGateScenarios() {
        final List<ExpectedEvent> defendantValidationNoise = new ArrayList<>();
        defendantValidationNoise.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-no-materials.json"));
        defendantValidationNoise.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);

        final List<ExpectedEvent> xhibitExpected = new ArrayList<>(defendantValidationNoise);
        xhibitExpected.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-no-materials.json"));

        final List<ExpectedEvent> xhibitExpectedNullMaterials = new ArrayList<>(defendantValidationNoise);
        xhibitExpectedNullMaterials.add(new ExpectedEvent(MigratedCaseFileReceived.class, "json/aggregate/migrated-case-file-received-no-materials-null.json"));

        return Stream.of(
                new AggregateScenario("isXhibit() true for XHIBIT — MigratedCaseFileReceived reaches the stream",
                        noMaterialsInput(sourceSystem(SOURCE_SYSTEM_XHIBIT, SOURCE_SYSTEM_XHIBIT_IDENDIFIER)), xhibitExpected),
                new AggregateScenario("isXhibit() false for LIBRA — MigratedCaseFileReceived is withheld",
                        noMaterialsInput(sourceSystem("LIBRA", "LIBRA-123")), defendantValidationNoise),
                new AggregateScenario("No materials present — materials list is null rather than empty, same isXhibit() outcome",
                        nullMaterialsInput(), xhibitExpectedNullMaterials)
        );
    }

    /**
     * The nine cheapest, single-event fail-fast paths (03-stories.md PR3 background) — eight
     * converted from scenarios that already existed, one ("Invalid Prosecuting Authority",
     * {@code hasInvalidProsecutingAuthority()}) new: it did not exist before this story
     * (02-design.md, Coverage).
     */
    private static Stream<AggregateScenario> failFastScenarios() {
        return Stream.of(
                new AggregateScenario("Court record sheet count exceeds defendant count",
                        courtRecordSheetCountExceedsInput(),
                        List.of(processedFailure("Number of Court Record Sheets exceeds number of defendants"))),
                new AggregateScenario("Sending court code invalid — Either Sending or Receiving Court not found",
                        courtCodeInvalidInput(CaseDetails.caseDetails().withReceiptType("Either way case").withSendingCourt("AB00001").build()),
                        List.of(processedFailure("Either Sending or Receiving Court not found"))),
                new AggregateScenario("Receiving court code invalid — Either Sending or Receiving Court not found",
                        courtCodeInvalidInput(CaseDetails.caseDetails().withReceiptType("Either way case").withReceivingCourt("AB00001").build()),
                        List.of(processedFailure("Either Sending or Receiving Court not found"))),
                new AggregateScenario("Receipt type null — Invalid receipt types",
                        receiptTypeInput(null),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Receipt type empty — Invalid receipt types",
                        receiptTypeInput(""),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Receipt type unrecognised — Invalid receipt types",
                        receiptTypeInput("Bring back"),
                        List.of(processedFailure("Invalid receipt types"))),
                new AggregateScenario("Hearing has no listed defendants — No matching defendants with hearings found for the hearing",
                        noMatchingDefendantsForHearingInput(),
                        List.of(processedFailure("No matching defendants with hearings found for the hearing"))),
                new AggregateScenario("Hearing defendant matches but no offences match — No matching defendants with hearings found for the hearing",
                        hearingDefendantMatchesNoOffencesInput(),
                        List.of(processedFailure("No matching defendants with hearings found for the hearing"))),
                new AggregateScenario("Invalid Prosecuting Authority — hasInvalidProsecutingAuthority() (new scenario)",
                        invalidProsecutingAuthorityInput(),
                        List.of(processedFailure("Invalid Prosecuting Authority")))
        );
    }

    /**
     * The three remaining R3a scenarios behind the {@code hasOffenceProblems()} gate — none existed
     * before this story (02-design.md, Coverage). Each is a defendant-level offence problem the
     * aggregate turns into a single fail-fast {@link MigratedCaseFileProcessed}, but only after
     * {@code validateDefendantErrors} has already added one {@link DefendantValidationFailed} for
     * the same defendant — hence two events, not one.
     */
    private static Stream<AggregateScenario> hasOffenceProblemsGateScenarios() {
        return Stream.of(
                new AggregateScenario("Invalid offence code — hasOffenceProblems()/hasInvalidOffenceCode() (new scenario)",
                        invalidOffenceCodeInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-invalid-offence-code.json"),
                                processedFailure("Invalid offence code"))),
                new AggregateScenario("Guilty plea missing plea date — hasOffenceProblems()/hasInvalidPleaDate() (new scenario)",
                        missingPleaDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-missing-plea-date.json"),
                                processedFailure("Missing or Invalid plea date"))),
                new AggregateScenario("Verdict missing verdict date — hasOffenceProblems()/hasInvalidVerdictDate() (new scenario)",
                        missingVerdictDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-missing-verdict-date.json"),
                                processedFailure("Missing or Invalid verdict date")))
        );
    }

    private static Stream<AggregateScenario> materialsMainPathScenarios() {
        final List<ExpectedEvent> receivedWithMaterialExpected = new ArrayList<>();
        receivedWithMaterialExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-received-with-material.json"));
        receivedWithMaterialExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        receivedWithMaterialExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        receivedWithMaterialExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-received-with-material.json"));

        final List<ExpectedEvent> defendantLevelExpected = new ArrayList<>();
        defendantLevelExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-defendant-level.json"));
        defendantLevelExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        defendantLevelExpected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"));
        defendantLevelExpected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        defendantLevelExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        defendantLevelExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-defendant-level.json"));

        return Stream.of(
                new AggregateScenario("Receive migrated case file with one PDF material — happy path",
                        receivedWithMaterialInput(), receivedWithMaterialExpected),
                new AggregateScenario("Non-PDF material without a matching defendant — fails fast, not a PDF",
                        nonPdfWithoutMaterialInput(),
                        List.of(processedFailure("Court Record Sheet must be a PDF file"))),
                new AggregateScenario("Defendant-level material, no documentation language, invalid parent guardian gender",
                        defendantLevelInput(), defendantLevelExpected)
        );
    }

    private static Stream<AggregateScenario> defendantProblemsScenarios() {
        final List<ExpectedEvent> allNullDefendantExpected = new ArrayList<>();
        allNullDefendantExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-all-null.json"));
        allNullDefendantExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        allNullDefendantExpected.add(warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [null]"));
        allNullDefendantExpected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [null]"));
        allNullDefendantExpected.add(warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [null]"));
        allNullDefendantExpected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        allNullDefendantExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        allNullDefendantExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-all-null.json"));

        return Stream.of(
                new AggregateScenario("All defendant fields null — every defendant-level warning fires",
                        allNullDefendantInput(), allNullDefendantExpected)
        );
    }

    private static Stream<AggregateScenario> pleaScenarios() {
        final List<ExpectedEvent> arrestAndChargeDateNoise = List.of(
                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                warning("Defendant validation", "ARREST_DATE_IN_FUTURE : [null]"),
                warning("Defendant validation", "CHARGE_DATE_IN_FUTURE : [Charge date not provided]"),
                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : []"),
                warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [null]"),
                warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [null]"),
                warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [null]"),
                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));

        final List<ExpectedEvent> guiltyPleaWithDateExpected = new ArrayList<>();
        guiltyPleaWithDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-plea-with-date.json"));
        guiltyPleaWithDateExpected.addAll(arrestAndChargeDateNoise);
        guiltyPleaWithDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-guilty-plea-with-date.json", MATERIAL_ADDED_EXCLUSIONS));
        guiltyPleaWithDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-guilty-plea-with-date.json"));

        final List<ExpectedEvent> notGuiltyPleaWithDateExpected = new ArrayList<>();
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-plea-with-date.json"));
        notGuiltyPleaWithDateExpected.addAll(arrestAndChargeDateNoise);
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        notGuiltyPleaWithDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-not-guilty-plea-with-date.json"));

        final List<ExpectedEvent> notGuiltyMissingDateExpected = new ArrayList<>();
        notGuiltyMissingDateExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-not-guilty-missing-plea-date.json"));
        notGuiltyMissingDateExpected.addAll(arrestAndChargeDateNoise);
        notGuiltyMissingDateExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        notGuiltyMissingDateExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-not-guilty-missing-plea-date.json"));

        final List<ExpectedEvent> badPleaCodeExpected = new ArrayList<>();
        badPleaCodeExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-bad-plea-code.json"));
        badPleaCodeExpected.addAll(arrestAndChargeDateNoise);
        badPleaCodeExpected.add(warning("Offence validation", "INVALID_PLEA : [e1e1e1e1-1111-4111-8111-111111111111]"));
        badPleaCodeExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        badPleaCodeExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-bad-plea-code.json"));

        return Stream.of(
                new AggregateScenario("Guilty plea with plea code and plea date — happy path",
                        guiltyPleaWithDateInput(), guiltyPleaWithDateExpected),
                new AggregateScenario("Not guilty plea with plea code and plea date — happy path",
                        notGuiltyPleaWithDateInput(), notGuiltyPleaWithDateExpected),
                new AggregateScenario("Guilty plea with a future plea date — fails fast",
                        guiltyPleaFutureDateInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-guilty-plea-future-date.json", PLEA_FUTURE_DATE_EXCLUSIONS),
                                processedFailure("Missing or Invalid plea date"))),
                new AggregateScenario("Not guilty plea with a missing plea date — plea date not required for NG",
                        notGuiltyMissingDateInput(), notGuiltyMissingDateExpected),
                new AggregateScenario("Unrecognised plea code — offence validation warning, still creation-pending",
                        badPleaCodeInput(), badPleaCodeExpected)
        );
    }

    private static Stream<AggregateScenario> genderCourtMarkerScenarios() {
        final List<ExpectedEvent> genderProvidedExpected = new ArrayList<>();
        genderProvidedExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-provided.json"));
        genderProvidedExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        genderProvidedExpected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        genderProvidedExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        genderProvidedExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-gender-provided.json"));

        final List<ExpectedEvent> genderNotMatchInCpExpected = new ArrayList<>();
        genderNotMatchInCpExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-match-in-cp.json"));
        genderNotMatchInCpExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        genderNotMatchInCpExpected.add(warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [NOTINCP]"));
        genderNotMatchInCpExpected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [NOTINCP]"));
        genderNotMatchInCpExpected.add(warning("Defendant validation", "DOCUMENTATION_LANGUAGE_INVALID : [NOTINCP]"));
        genderNotMatchInCpExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        genderNotMatchInCpExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-gender-not-match-in-cp.json"));

        final List<ExpectedEvent> courtValidExpected = new ArrayList<>();
        courtValidExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-court-valid.json"));
        courtValidExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        courtValidExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        courtValidExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-court-valid.json"));

        final List<ExpectedEvent> caseMarkerInvalidExpected = new ArrayList<>();
        caseMarkerInvalidExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-case-marker-invalid.json"));
        caseMarkerInvalidExpected.add(warning("Case validation", "CASE_MARKER_IS_INVALID : [ABC001]"));
        caseMarkerInvalidExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        caseMarkerInvalidExpected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"));
        caseMarkerInvalidExpected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        caseMarkerInvalidExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        caseMarkerInvalidExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-case-marker-invalid.json"));

        final List<ExpectedEvent> parentGuardianNullExpected = new ArrayList<>();
        parentGuardianNullExpected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-parent-guardian-null.json"));
        parentGuardianNullExpected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        parentGuardianNullExpected.add(warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"));
        parentGuardianNullExpected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        parentGuardianNullExpected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        parentGuardianNullExpected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-parent-guardian-null.json"));

        return Stream.of(
                new AggregateScenario("Defendant and parent/guardian gender provided and valid in CP",
                        genderProvidedInput(), genderProvidedExpected),
                new AggregateScenario("Defendant and parent/guardian gender provided but not recognised in CP",
                        genderNotMatchInCpInput(), genderNotMatchInCpExpected),
                new AggregateScenario("Sending and receiving court both valid",
                        courtValidInput(), courtValidExpected),
                new AggregateScenario("Receipt type valid — Either way case",
                        validReceiptTypeInput("Either way case"), receiptTypeExpected("Either way case")),
                new AggregateScenario("Receipt type valid — Transfer",
                        validReceiptTypeInput("Transfer"), receiptTypeExpected("Transfer")),
                new AggregateScenario("Receipt type valid — Voluntary bill",
                        validReceiptTypeInput("Voluntary bill"), receiptTypeExpected("Voluntary bill")),
                new AggregateScenario("Receipt type valid — Indictable",
                        validReceiptTypeInput("Indictable"), receiptTypeExpected("Indictable")),
                new AggregateScenario("Case marker with an unrecognised marker type code",
                        caseMarkerInvalidInput(), caseMarkerInvalidExpected),
                new AggregateScenario("Defendant and parent/guardian gender provided but not recognised in CP (no case marker)",
                        genderNotInCpInput(), genderNotInCpExpected("Either way case")),
                new AggregateScenario("Case marker with a null marker type code",
                        caseMarkerNullOrEmptyInput(null),
                        caseMarkerNullOrEmptyExpected(warning("Case validation", "CASE_MARKER_IS_INVALID : [null]"), "json/aggregate/migrated-case-validated-creation-pending-case-marker-null.json")),
                new AggregateScenario("Case marker with an empty marker type code",
                        caseMarkerNullOrEmptyInput(""),
                        caseMarkerNullOrEmptyExpected(warning("Case validation", "CASE_MARKER_IS_INVALID : []"), "json/aggregate/migrated-case-validated-creation-pending-case-marker-empty.json")),
                new AggregateScenario("Parent/guardian information entirely null",
                        parentGuardianNullInput(), parentGuardianNullExpected),
                new AggregateScenario("Custody status C with a missing custody time limit",
                        custodyCWithMissingCtlInput(),
                        List.of(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-custody-c-ctl-null.json"),
                                warning("Defendant validation", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID : [British]"),
                                warning("Defendant validation", "DEFENDANT_NATIONALITY_INVALID : [HUN]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_STATUS_INVALID : [C]"),
                                warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"),
                                warning("Defendant validation", "DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING : [DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING]"),
                                new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS),
                                new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-custody-c-ctl-null.json")))
        );
    }

    /** A {@code MigratedCaseValidatedWithWarnings} event carrying a single warning {@code type}/{@code message} pair. */
    static ExpectedEvent warning(final String type, final String message) {
        return new ExpectedEvent(MigratedCaseValidatedWithWarnings.class, String.format(
                "{\"caseId\": \"%s\", \"type\": \"%s\", \"message\": \"%s\"}", CASE_ID, type, message), true, Map.of(), List.of());
    }

    /** A failed {@code MigratedCaseFileProcessed} event carrying a single failure {@code description}. */
    static ExpectedEvent processedFailure(final String description) {
        return new ExpectedEvent(MigratedCaseFileProcessed.class, String.format(
                "{\"caseId\": \"%s\", \"description\": \"%s\", \"processingIsSuccessful\": false, \"submissionId\": \"%s\"}",
                CASE_ID, description, SUBMISSION_ID), true, Map.of(), List.of());
    }

    private static List<ExpectedEvent> receiptTypeExpected(final String receiptType) {
        final List<ExpectedEvent> expected = new ArrayList<>();
        expected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-in-cp.json"));
        expected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        expected.add(warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"));
        expected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"));
        expected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        expected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        expected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, "json/aggregate/migrated-case-validated-creation-pending-receipt-type.json", Map.of("receiptType", receiptType)));
        return expected;
    }

    private static List<ExpectedEvent> genderNotInCpExpected(final String receiptType) {
        return receiptTypeExpected(receiptType);
    }

    private static List<ExpectedEvent> caseMarkerNullOrEmptyExpected(final ExpectedEvent caseMarkerWarning, final String creationPendingFixture) {
        final List<ExpectedEvent> expected = new ArrayList<>();
        expected.add(new ExpectedEvent(DefendantValidationFailed.class, "json/aggregate/defendant-validation-failed-gender-not-in-cp.json"));
        expected.add(caseMarkerWarning);
        expected.addAll(DEFENDANT_ETHNICITY_NATIONALITY_CUSTODY_NOISE);
        expected.add(warning("Defendant validation", "DEFENDANT_GENDER_INVALID : [XXX]"));
        expected.add(warning("Defendant validation", "PARENT_GUARDIAN_GENDER_INVALID : [YYYY]"));
        expected.add(warning("Defendant validation", "HEARING_LANGUAGE_INVALID : [null]"));
        expected.add(new ExpectedEvent(MaterialAdded.class, "json/aggregate/material-added-standard-pdf.json", MATERIAL_ADDED_EXCLUSIONS));
        expected.add(new ExpectedEvent(MigratedCaseValidatedCreationPending.class, creationPendingFixture));
        return expected;
    }
}

/**
 * The row and shared assertion block for T2 (see
 * docs/pipeline/DD-43067-DD-43099-pcfdlrm-test-hardening/03-stories.md, Aggregate scenario
 * harness). Every scenario supplies its own fully-built {@link CaseFileInput} — the harness makes
 * no assumption about what varies between rows, so it serves the {@code isXhibit()} gate proof
 * (PR2) and the fail-fast rows (PR3) alike.
 */
record AggregateScenario(String name, CaseFileInput input, List<ExpectedEvent> expected) {
    @Override
    public String toString() {
        return name;
    }
}
