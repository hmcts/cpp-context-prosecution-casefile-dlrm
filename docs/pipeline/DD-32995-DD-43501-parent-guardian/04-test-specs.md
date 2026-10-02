# 04 — Test Specs (A-TDD)

- **Story:** DD-43501 (epic DD-32995). Repo `pcfdlrm`, branch `dev/dd-43501` (from `team/libra1` @ `f5c3f4d`).
- **Inputs:** `03-stories.md` (S1–S5, all ACs), `02-design.md` (class/method/code names), `01-requirements.md`, ADR-DD-43501.
- **Approach:**
  - All coverage is unit level, in `pcfdlrm-domain/pcfdlrm-domain-aggregate/src/test`.
  - Existing test classes were extended wherever one exists. New classes were created only for production
    classes the design adds (package `validation/rules/defendant/parentguardian/`).
  - Matrices use `@ParameterizedTest`. Guardians are built from shared `ObjectBuilder` factories.
  - No new `*IT.java` was added. The only IT-side change is the fixture edit the design requires.
  - No Gherkin `.feature` file was written, because the repo has no Cucumber (no `*.feature` files and no
    cucumber dependency). The table below is the AC contract.
- **State:** the tests are red until the implementation exists. `mvn test-compile` fails only on the missing
  production symbols listed under [Production API the tests assume](#production-api-the-tests-assume).

Paths are relative to `pcfdlrm-domain/pcfdlrm-domain-aggregate/src/test/java/uk/gov/moj/cpp/pcfdlrm/`.
Class abbreviations used in the table:

| Abbr | Class | Status | File |
|---|---|---|---|
| PCV | `PostCodeValidationRuleTest` | existing | `validation/rules/defendant/PostCodeValidationRuleTest.java` |
| DOB | `ParentGuardianDateOfBirthValidationRuleTest` | existing | `validation/rules/defendant/ParentGuardianDateOfBirthValidationRuleTest.java` |
| OBE | `ParentGuardianObservedEthnicityValidationAndEnricherRuleTest` | existing | `validation/rules/defendant/…ObservedEthnicity…Test.java` |
| SDE | `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRuleTest` | existing | `validation/rules/defendant/…SelfDefinedEthnicity…Test.java` |
| PRV | `CcProsecutionValidationRuleProviderTest` | existing | `validation/provider/CcProsecutionValidationRuleProviderTest.java` |
| HLP | `ProsecutionCaseFileHelperTest` | existing | `ProsecutionCaseFileHelperTest.java` |
| AGG | `MigratedCaseFileAggregateTest` | existing | `aggregate/MigratedCaseFileAggregateTest.java` |
| SHP | `ParentGuardianShapeTest` (shape + `ParentGuardianShapeGate`) | **new** | `validation/rules/defendant/parentguardian/ParentGuardianShapeTest.java` |
| TEL | `LibraParentGuardianTelephoneValidationRuleTest` | **new** | `…/parentguardian/LibraParentGuardianTelephoneValidationRuleTest.java` |
| EML | `LibraParentGuardianEmailValidationRuleTest` | **new** | `…/parentguardian/LibraParentGuardianEmailValidationRuleTest.java` |
| GEN | `LibraParentGuardianGenderValidationRuleTest` (rule + `LibraGenderCode`) | **new** | `…/parentguardian/LibraParentGuardianGenderValidationRuleTest.java` |
| ADR | `LibraParentGuardianAddressValidationRuleTest` (individual + organisation rules) | **new** | `…/parentguardian/LibraParentGuardianAddressValidationRuleTest.java` |
| OUT | `LibraParentGuardianOutcomesTest` (outcome table + `LibraParentGuardianSanitiser` + regex audit) | **new** | `…/parentguardian/LibraParentGuardianOutcomesTest.java` |

Shared test support that was extended (not new):

- `builder/ObjectBuilder.java` gains these factories: `validGuardianContactDetails()`, `validGuardianAddress()`,
  `individualParentGuardian(ContactDetails, Address)`, `validIndividualParentGuardian()`,
  `organisationParentGuardian(Address)` and `defendantWithParentGuardian(UUID, ParentGuardianInformation)`.
- `builder/TestConstants.java` gains the literal guardian field keys `PG_*`, `pgAddressKey(n)` and
  `pgOrgAddressKey(n)`, plus `LIBRA`.
- `aggregate/AggregateScenarioInputs.java` gains `parentGuardianCaseInput(source, initiationCode, guardians...)`.

## AC → test traceability

| AC | Test (Class#method) |
|---|---|
| AC-S1-001 | HLP#shouldRaiseNoParentGuardianProblemWhenLibraDefendantHasNoParentGuardian; SHP#shouldClassifyParentGuardianShape (absent rows); SHP#shouldRunDelegateOnlyForItsShape (ABSENT rows) |
| AC-S1-002 | TEL#shouldRaiseOneProblemPerInvalidNumber; TEL#shouldValidateIndividualGuardianTelephoneNumbers (9 vs 10 characters, `(`, `.`, tab, blank); OUT#shouldNullExactlyTheFieldNamedByTheProblemKey (work/home/mobile); HLP#shouldSanitiseIndividualParentGuardianAndRaiseOneRedactedProblemPerField |
| AC-S1-003 | EML#shouldValidateGuardianEmailAddresses (127 vs 128, `+`, no `@`, both fields); OUT#shouldNullExactlyTheFieldNamedByTheProblemKey (primary/secondary) |
| AC-S1-004 | DOB#shouldReturnEmptyListWhenParentGuardianDateOfBirthIsToday; DOB#shouldRedactDateOfBirthWhenWrappedInRedactingValidationRule; OUT#…NullExactly… (dateOfBirth); HLP#shouldSanitiseIndividualParentGuardian… |
| AC-S1-005 | GEN#shouldNormaliseLibraGenderCode; GEN#shouldRaiseRedactedGenderProblemOnlyWhenUnmappable; OUT#shouldNormaliseIndividualGuardianGender; HLP#shouldMapLibraParentGuardianGender |
| AC-S1-006 | SHP#shouldRunDelegateOnlyForItsShape; OUT#shouldNeverTouchGenderOfOrganisationGuardian; HLP#shouldSanitiseOrganisationParentGuardianWithoutTouchingGender |
| AC-S1-007 | OBE#shouldRedactObservedEthnicityWhenWrappedInRedactingValidationRule; SDE#shouldRedactSelfDefinedEthnicityWhenWrappedInRedactingValidationRule; OUT#…NullExactly… (both ethnicities) |
| AC-S1-008 | The redaction assertion (value == key) in every TEL/EML/GEN/ADR row, DOB/OBE/SDE `shouldRedact…`, HLP#shouldSanitiseIndividualParentGuardian… (also checks `DefendantValidationFailed` problems), AGG#shouldAcceptLibraCaseWithSanitisedParentGuardianAndRedactedWarnings (warning text) |
| AC-S1-009 | PRV#shouldKeepTodaysDefendantRulesOutsideLibraScope; HLP#shouldKeepTodaysBehaviourOutsideLibraParentGuardianScope; PCV#shouldKeepWarningOnInvalidGuardianPostCodeForNoArgConstructor |
| AC-S2-001 | HLP#shouldSanitiseOrganisationParentGuardianWithoutTouchingGender (no `organisationName`, no problem for it) |
| AC-S2-002 | HLP#shouldKeepOrganisationNameWithNoBusinessRule (255 characters kept, no rule). The 256-character case is a schema 400, see [Not coverable at unit level](#not-coverable-at-unit-level) |
| AC-S2-003 | TEL#shouldValidateOrganisationGuardianCompanyTelephoneNumber; OUT#…NullExactly… (company number); HLP#shouldSanitiseOrganisationParentGuardian… |
| AC-S2-004 | ADR#shouldValidateOrganisationGuardianAddress (address5 of 36 characters); OUT#…NullExactly… (organisation address2–5); HLP#shouldSanitiseOrganisationParentGuardian… |
| AC-S2-005 | ADR#shouldValidateOrganisationGuardianAddress (postcode); OUT#…NullExactly… (organisation postcode); HLP#shouldSanitiseOrganisationParentGuardian… (nulled, not rejected) |
| AC-S2-006 | Redaction assertion in TEL (organisation rows), ADR (organisation rows) and HLP#shouldSanitiseOrganisationParentGuardian… |
| AC-S2-007 | PRV#shouldKeepTodaysDefendantRulesOutsideLibraScope (XHIBIT rows); HLP#shouldKeepTodaysBehaviourOutsideLibraParentGuardianScope (XHIBIT: company number kept) |
| AC-S3-001 | AGG#shouldRejectOnParentGuardianOnlyInLibraScope (XHIBIT C: accepted, `INVALID_GUARDIAN_POST_CODE : [NOT A POSTCODE]` warning); PCV#shouldValidateGuardianPostCodeOnlyWhenEnabled; PCV#shouldKeepWarningOnInvalidGuardianPostCodeForNoArgConstructor |
| AC-S3-002 | HLP#shouldKeepTodaysBehaviourOutsideLibraParentGuardianScope (LIBRA J). Not at aggregate level: the SJP case rule set needs a prosecutor and offences that the guardian-only scenario does not build. |
| AC-S3-003 | AGG#shouldRejectOnParentGuardianOnlyInLibraScope (S, C, Q); HLP#shouldCollectLibraParentGuardianRejections (S/C/Q rows) |
| AC-S3-004 | ADR#shouldValidateIndividualGuardianAddress (no personalInformation / no address); HLP#shouldCollectLibraParentGuardianRejections |
| AC-S3-005 | ADR#shouldValidateIndividualGuardianAddress (`NFA`, `nfa`); HLP#shouldCollectLibraParentGuardianRejections |
| AC-S3-006 | ADR#shouldValidateIndividualGuardianAddress (`""`, `"   "`, 35 vs 36); HLP#shouldCollectLibraParentGuardianRejections |
| AC-S3-007 | ADR#shouldValidateIndividualGuardianAddress (address3 of 36 characters); HLP#shouldRemoveOverlongIndividualAddressLineWithoutRejecting; OUT#…NullExactly… (address2–5) |
| AC-S3-008 | ADR#shouldValidateIndividualGuardianAddress (absent, blank, `SW1A 1AA`, `SW1A1AA`, `ZZ99 9ZZ`, `zz999zz`, `NOT A POSTCODE`, over 8 characters); HLP#shouldCollectLibraParentGuardianRejections; PRV#shouldNotValidateGuardianPostCodeInGenericPostCodeRuleForLibra |
| AC-S3-009 | AGG#shouldRejectLibraCaseOnceWithEveryParentGuardianReason (two defendants, one event, ordered and distinct reasons, caseId/caseUrn/submissionId, no Received/CreationPending/MaterialAdded/warnings); ADR (address1-then-postcode order row); AGG#shouldPinParentGuardianRejectionDescriptionClearOfStagingDlrmMarkers |
| AC-S3-010 | HLP#shouldSanitiseIndividualParentGuardianAndRaiseOneRedactedProblemPerField (whole-defendant equality); AGG#shouldAcceptLibraCaseWithSanitisedParentGuardianAndRedactedWarnings |
| AC-S3-011 | Three IT fixtures edited (see [Fixtures modified](#fixtures-modified)). The existing aggregate row "No materials, LIBRA" (`AggregateScenarios#xhibitGateScenarios`) is unchanged and must stay green (see [Deviations](#deviations-and-decisions-for-the-implementer)) |
| AC-S4-001 | ADR#shouldValidateOrganisationGuardianAddress (no address); HLP#shouldCollectLibraParentGuardianRejections; AGG#shouldRejectLibraCaseOnceWithEveryParentGuardianReason (mixed-shape row = S4 DoD) |
| AC-S4-002 | ADR#shouldValidateOrganisationGuardianAddress (blank, 35 vs 36); HLP#shouldCollectLibraParentGuardianRejections |
| AC-S4-003 | Schema 400, see [Not coverable at unit level](#not-coverable-at-unit-level) |
| AC-S4-004 | ADR (no address, F-2 row); HLP#shouldCollectLibraParentGuardianRejections; AGG#shouldRejectLibraCaseOnceWithEveryParentGuardianReason (LIBRA-feed-shape row) |
| AC-S5-001 | PRV#shouldKeepTodaysDefendantRulesOutsideLibraScope; HLP#shouldKeepTodaysBehaviourOutsideLibraParentGuardianScope; OUT#shouldNotClassifyNonGuardianCodes; and every existing test, unmodified |
| AC-S5-002 | OUT#shouldClassifyEveryLibraParentGuardianProblemCode; OUT#shouldNullExactlyTheFieldNamedByTheProblemKey; OUT#shouldLeaveRejectFieldsUntouched; the code + field-key pins in TEL/ADR |
| AC-S5-003 | OUT#shouldShipOnlyTheReviewedBoundedRegexes (exact regex text, and an adversarial input fails within 1 s) |
| AC-S5-004 | The redaction assertion in TEL, EML, GEN and ADR covers every new code; DOB/OBE/SDE cover the reused codes; HLP/AGG cover the events |
| AC-S5-005 | Schema 400, see [Not coverable at unit level](#not-coverable-at-unit-level) |

Every AC in `03-stories.md` (S1 ×9, S2 ×7, S3 ×11, S4 ×4, S5 ×5 = 36) is mapped above.

## Fixtures modified

These three IT fixtures each gain a guardian `personalInformation.address` with address1
"1 Example Street", address2 "Exampletown" and postcode "SW1A 1AA". The fixtures are LIBRA `Q`, so without
the address they would be rejected under FR-010.

- `pcfdlrm-integration-test/src/test/resources/command-json/pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`
- `pcfdlrm-integration-test/src/test/resources/command-json/pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json`
- `pcfdlrm-integration-test/src/test/resources/command-json/pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json`.
  No Java code references this fixture today; it was edited anyway, for consistency.

**Not modified:** `src/test/resources/json/aggregate/migrated-case-file-received-no-materials-libra.json`.
Its scenario (`noMaterialsInput(LIBRA)`) sets no initiation code on either the case or the defendant. The
resolved code is therefore null, `LibraParentGuardianScope` is false, and the case is not rejected.

Editing this fixture would also mean changing the shared input and the shared
`defendant-validation-failed-no-materials.json` that XHIBIT also uses, or adding a near-duplicate of it.
The row now serves as the regression guard that LIBRA with no S/C/Q code is unchanged. In-scope LIBRA
accept and reject at aggregate level are covered by the new `AGG` tests, using `parentGuardianCaseInput`.

## Not coverable at unit level

| AC | Why | What still guards it |
|---|---|---|
| AC-S2-002 (256-character half), AC-S4-003, AC-S5-005 | These are JSON-schema rejections. The framework REST adapter returns HTTP 400 from `JsonSchemaValidationInterceptor` before the command reaches the handler or aggregate (`02-design.md`, "Current behaviour when the receive payload fails JSON schema validation"). No aggregate or helper test can observe them, and the brief rules out new ITs. | The story changes no schema (`02-design.md` Contracts; S1–S4 DoD). At review, check that `git diff --stat` touches no `*.json` under `src/raml/json/schema` or `pcfdlrm-domain-value-schema`. |
| AC-S3-009, public-event half | `public.pcfdlrm.migrated-case-file-processed` is published by `MigratedCaseFileProcessedProcessor`, which is unchanged. | AGG pins every field the processor copies (caseId, caseUrn, submissionId, description, processingIsSuccessful). |
| AC-S3-011, IT half | The edited fixtures are only exercised by `./runIntegrationTests.sh`. | The fixture edits above; CI `context-validation` on `team/*`. |

## Production API the tests assume

The implementation agent must provide these exact names. Where a name is a test-side choice that
`02-design.md` does not fix, it is marked *(test choice)*.

Package `uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian`:

- `enum ParentGuardianShape { INDIVIDUAL, ORGANISATION, ABSENT }` and `static ParentGuardianShape of(ParentGuardianInformation)`.
  `of` must accept `null`.
- `ParentGuardianShapeGate implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>`, with:
  - `static ParentGuardianShapeGate onlyFor(ParentGuardianShape, ValidationRule<…>)`. The return type is the
    concrete class *(test choice)*.
  - `ParentGuardianShape shape()` and `ValidationRule<…> delegate()` *(test choice)*. The provider test uses
    them to see through the decorators.
  - It must not throw on a defendant whose `individual` is null.
- `RedactingValidationRule implements ValidationRule<…>`, with `static … of(ValidationRule<…>)` and
  `ValidationRule<…> delegate()` *(test choice)*.
- `LibraGenderCode.normalise(String) → Optional<String>`. A CP `Gender` name in any case is returned exactly
  as given, so `"male"` stays `"male"` (literal reading of the design).
- Rule classes:
  - `new LibraParentGuardianTelephoneValidationRule(ParentGuardianShape mode)`. The constructor parameter is a
    *(test choice)*; the design says only "individual mode / ORG mode".
  - `new LibraParentGuardianEmailValidationRule()`
  - `new LibraParentGuardianGenderValidationRule()`
  - `new LibraIndividualParentGuardianAddressValidationRule()`
  - `new LibraOrganisationParentGuardianAddressValidationRule()`
- `LibraParentGuardianOutcomes`, with:
  - nested `enum Outcome { REJECT, NULL, DEFAULT }` *(test choice)*
  - `static Optional<Outcome> outcomeOf(ProblemCode)` *(test choice)*
  - `static boolean isReject(String code)`
- `LibraParentGuardianScope.applies(Channel, String migrationSourceSystemName, String initiationCode)`. A null
  channel, a null code and a lower-case `"libra"` all give false.
- `LibraParentGuardianSanitiser.sanitise(MigratedDefendant.Builder, List<Problem>)`.

Elsewhere:

- `uk.gov.moj.cpp.pcfdlrm.DefendantValidationOutcome`, with accessors
  `MigratedDefendantWithProblem migratedDefendantWithProblem()` and `List<Problem> libraGuardianRejections()`.
- `ProsecutionCaseFileHelper.validateDefendants(CaseDetails, Channel, DefendantsWithReferenceData,
  ReferenceDataQueryService, Stream.Builder<Object>, Boolean, String) → DefendantValidationOutcome`, with the
  same parameters as `validateDefendantErrors`.
- `CcProsecutionValidationRuleProvider.getDefendantValidationRules(String, Channel, Boolean, String)`.
- `PostCodeValidationRule(boolean validateParentGuardianPostCode)`. The no-arg constructor keeps today's
  behaviour.
- `MigratedCaseFileAggregate.PARENT_GUARDIAN_VALIDATION_FAILED = "Parent guardian validation failed: "` (public;
  the constant name is a *(test choice)*).
- `ProblemCode`: the nine new codes in design C8. `Constants`: `CP_TELEPHONE`, `LIBRA_GUARDIAN_EMAIL`,
  `NO_FIXED_ABODE_POST_CODE`. Both are resolved from strings at runtime, so a missing entry fails the test,
  not the compile.
- `FieldName`: constant names are **not** pinned. The tests assert the literal key text from `TestConstants`
  (`individual_parentGuardianInformation_…`), because that text is the contract.

## Deviations and decisions for the implementer

1. **Rule order is pinned in two places.**
   - Within each address rule: address1, then address2–5, then postcode.
   - In the aggregate description: defendant order, then rule order. In the multi-defendant AGG rows,
     REJECT codes are raised by the address rules only, so only the order in which those rules run within
     a defendant matters. The design places individual before organisation.
2. **D-1 (blank = present and invalid)** is pinned for phone, email and gender. Blank postcode and blank
   address1 follow their own FR rules.
3. **`a@b` is valid.** The ticket regex does not require a dot in the domain. The design's test list includes
   "domain with no dot" without saying what should happen, so the tests follow the regex.
4. **Postcode over 8 characters** is pinned as a REJECT for individual guardians (Q7 default, design C3).
   `SW1A   1AA` matches `POST_CODE_REGEX` and fails only on length.
5. **Out-of-scope behaviour.** For LIBRA J/R/O and for XHIBIT, `ProsecutionCaseFileHelperTest` pins four
   things:
   - no rejection
   - the invalid home number and the company number are kept
   - no problem is keyed under `individual_parentGuardianInformation_…`
   - the generic `PARENT_GUARDIAN_GENDER_INVALID` (key `parentguardian.gender`) is still raised
6. **Compile check.** `mvn -o -pl pcfdlrm-domain/pcfdlrm-domain-aggregate -am test-compile` fails only on the
   symbols above. The test sources were also compiled with `javac`, outside the repo, against scratch stubs
   of exactly this API, and compiled with zero errors. No production code was written in the repo.
