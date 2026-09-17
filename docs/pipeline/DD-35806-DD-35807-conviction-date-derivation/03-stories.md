# 03 — User Story

## DD-35807 — Automation coverage for conviction-date derivation (indicated guilty / guilty pleas)

**As** a tester of the `pcfdlrm` LIBRA migration path
**I want** integration-test coverage proving a guilty or indicated-guilty plea drives the
offence's conviction date
**So that** the existing derivation behaviour is verified for LIBRA, instead of resting on unit
tests that only exercise XHIBIT.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm` (Stage 7 CI is per-repo).
- `pcfdlrm-integration-test` fixtures + `ReceiveMigratedCaseFileIT` parametrization + one shared
  stub-data record (see `02-design.md` root cause).

### Acceptance criteria

See `01-requirements.md` AC-1 / AC-2:
1. LIBRA, indicated-guilty plea with a date → offence `convictionDate` equals that plea date.
2. LIBRA, guilty plea with a date → offence `convictionDate` equals that plea date.

### Definition of done

- [x] `libra-journey.json` asserts `convictionDate` (AC-2), value confirmed against a real Docker
      IT run.
- [x] New `libra-indicated-guilty-plea` command + expected fixture pair added, asserting
      `indicatedPlea` and `convictionDate` (AC-1), confirmed against a real run.
- [x] `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` extended with the new
      `@CsvSource` row — no new IT class.
- [x] `./runIntegrationTests.sh` green locally for the full `pcfdlrm-integration-test` module
      (28/28), including the two new/changed cases.
- [x] No production code touched — root cause was a stub-data jurisdiction gap
      (`referencedata.query.plea-types.json`), not a converter defect (FR-4 held).

### Out of scope

- Plea values other than `GUILTY` / `INDICATED_GUILTY` (Stage 1 open question 2).
- Verdict-derived conviction dates (Stage 1 open question 3) — already covered by existing tests.
- XHIBIT fixtures (Stage 1 open question 4) — already unit-covered for both plea codes.
