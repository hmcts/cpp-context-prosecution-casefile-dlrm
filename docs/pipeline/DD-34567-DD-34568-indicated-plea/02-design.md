# 02 — Design

- **Story:** [DD-34568](https://tools.hmcts.net/jira/browse/DD-34568)
- **Change scope:** one production class + its unit test. No schema, RAML, subscription, or
  Liquibase changes (the courts `Offence` model already carries `indicatedPlea`, generated from
  `criminal-court-public-model` 17.104.4).

## Layer analysis (the three-layer rule)

- **Command side / aggregate:** unchanged. Validation of migrated pleas
  (`PleaValidationRule`) is untouched; indicated pleas already validate.
- **Event listener:** unchanged (still a stub).
- **Event processor:** the change lives here — in the offence converter used when
  `MigratedCaseReceivedProcessor` builds the `progression.initiate-court-proceedings` command.

No event is added or removed, so the "add/remove an event" gotcha (subscriptions + schema tree)
does not apply.

## Component touched

`pcfdlrm-event/pcfdlrm-event-processor/src/main/java/uk/gov/moj/cpp/pcfdlrm/event/processor/convertor/ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.java`

### Before

`buildOffence` always did `final Plea plea = convertPlea(...)`, set `.withPlea(plea)`, and
derived "is this guilty?" from the built `Plea` object via `hasGuiltyPlea(Plea)`
(`INDICATED_GUILTY` or `GUILTY`). `hasGuiltyPlea` fed convicting-court and custody-time-limit
decisions.

### After

`buildOffence` now:
1. Resolves the plea reference data once (`findPleaReferenceData`) and reads the plea value string.
2. Computes `indicatedPleaValue = toIndicatedPleaValue(pleaValue)` — non-null iff the value is
   `INDICATED_GUILTY` / `INDICATED_NOT_GUILTY`.
3. Gates the diversion on source system: `isIndicatedPlea = indicatedPleaValue != null &&
   "LIBRA".equals(migrationSourceSystemName)`. **XHIBIT is excluded** — an indicated value on an
   XHIBIT migration stays on `plea`, exactly as before this change.
4. If diverted: `plea = null`, `indicatedPlea = convertIndicatedPlea(...)`. Else: `plea =
   convertPlea(...)` (unchanged), `indicatedPlea = null`.
5. Sets both `.withPlea(plea).withIndicatedPlea(indicatedPlea)` (exactly one is non-null).

Note the guilty-derivation (`guiltyPlea`) is computed from the plea *value* and is
**source-independent**, so XHIBIT's `INDICATED_GUILTY` keeps counting as guilty just as it did
when the value lived on `plea`.

### The critical decision — preserve guilty derivation

Nulling `plea` for indicated pleas would have silently broken the guilty-derived logic, because
it read `INDICATED_GUILTY` off the `Plea` object. To prevent that regression, "is guilty" was
**re-based on the plea *value* string**, independent of which object holds it:

- New `boolean guiltyPlea = isGuiltyPleaValue(pleaValue)` (`GUILTY` or `INDICATED_GUILTY`) —
  computed once in `buildOffence`.
- `hasGuiltyPlea(Plea)` removed. `getConvictingCourt(...)` and `isCustodyLimitTobeSet(...)` now
  take the `guiltyPlea` boolean.
- `getConvictionDate(...)` was already value-driven (reads the reference-data guilty flag, not the
  `Plea` object), so it is unchanged and still fires for `INDICATED_GUILTY`.
- `convertVerdict(...)` still receives `plea`; it only nulls a verdict for exactly `GUILTY`, which
  an indicated plea never was, so passing `null` for indicated pleas preserves behaviour.

### `indicatedPlea` field mapping

| Field | Source |
|-------|--------|
| `offenceId` | `offence.getOffenceId()` |
| `indicatedPleaValue` | mapped from plea value string to enum |
| `indicatedPleaDate` | migrated plea date; if guilty-flag `No` and missing → today (mirrors `convertPlea`) |
| `source` | **`IN_COURT`** — assumption, see ADR (open question #1) |
| `originatingHearingId` | unset (not available from migrated data) |

## Test strategy

Unit tests added to `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest`:
- `shouldPopulateIndicatedPleaAndClearPleaWhenPleaValueIsIndicatedGuilty` (AC-1)
- `shouldPopulateIndicatedPleaAndClearPleaWhenPleaValueIsIndicatedNotGuilty` (AC-2)
- `shouldDefaultIndicatedPleaDateToTodayWhenIndicatedNotGuiltyAndPleaDateMissing` (AC-3)
- `shouldTreatIndicatedGuiltyAsGuiltyWhenDerivingCustodyTimeLimit` (AC-5 — the regression guard)
- `shouldKeepIndicatedGuiltyOnPleaObjectForXhibit` (AC-6 — XHIBIT excluded)
- AC-4 is covered by the pre-existing `GUILTY`/`NOT_GUILTY` tests, which still pass.

(The indicated-plea diversion tests set `migrationSourceSystemName = LIBRA`; the two XHIBIT tests
assert the value stays on `plea` for both indicated values.)

**Processor-level tests** (`MigratedCaseReceivedProcessorTest`, DD-34568):
- `shouldEmitIndicatedPleaAndNoPleaForLibraIndicatedNotGuilty`
- `shouldEmitIndicatedPleaAndNoPleaForLibraIndicatedGuilty`

These run the real converter tree and assert on the serialised `progression.initiate-court-proceedings`
command JSON (`offences[0].indicatedPlea` present with `source`, `offences[0].plea` absent) — proving
the object survives POJO → JSON onto the outbound command, not just the converter return value.

**Integration test** (`ReceiveMigratedCaseFileIT.receiveMigratedLibraCaseFileWithIndicatedPlea`):
a LIBRA case whose offence plea points at the new `INDICATED_NOT_GUILTY` plea-type stub row; asserts
the emitted `initiate-court-proceedings` matches `json/xhibit/initiate-court-proceedings/libra-indicated-plea.json`
(the `libra-journey` fixture plus an `indicatedPlea` object). Runs only in the Docker IT env
(`./runIntegrationTests.sh`), not in `mvn verify`.

**Reference-data stub:** added the missing `INDICATED_NOT_GUILTY` row (code `ING`, MAGISTRATES,
guilty-flag `No`) to `referencedata.query.plea-types.json`.

**Result:** `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest` — 48 tests, 0 failures;
`MigratedCaseReceivedProcessorTest` — 12 tests, 0 failures; full `pcfdlrm-event-processor` module —
0 failures/errors. IT module test-compiles; the IT itself needs the Docker env to run.
