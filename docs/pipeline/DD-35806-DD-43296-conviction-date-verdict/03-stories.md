# 03 — User Story

## DD-43296 — Automation coverage for conviction-date derivation (guilty verdict)

**As** a tester of the `pcfdlrm` LIBRA migration path
**I want** integration-test coverage proving a guilty verdict (with a not-guilty plea) drives the
offence's conviction date
**So that** the existing derivation behaviour is verified end-to-end, instead of resting on a
unit test that only exercises XHIBIT.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm` (Stage 7 CI is per-repo).
- `pcfdlrm-integration-test` fixtures + `ReceiveMigratedCaseFileIT` parametrization + one new
  verdict-type stub-data row (see `02-design.md`).

### Acceptance criteria

See `01-requirements.md` AC-1:
1. LIBRA, future hearing, not-guilty plea + guilty verdict on an offence → offence
   `convictionDate` equals the verdict date.

### Definition of done

- [x] New `libra-guilty-verdict` command + expected fixture pair added, asserting `plea`
      (`NOT_GUILTY`), `verdict`, and `convictionDate`, confirmed against a real Docker IT run.
- [x] New `"Guilty"`-category verdict-type stub row added to
      `referencedata.query.verdict-types.json`.
- [x] `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` extended with the new
      `@CsvSource` row — no new IT class.
- [x] Full `pcfdlrm-integration-test` module green locally (29/29), including the new case.
- [x] No production code touched — the one real failure (`verdictType.categoryType` missing from
      the expected fixture) was a fixture gap, not a converter defect (FR-4 held).

### Out of scope

- Plea-derived conviction dates — already covered by DD-35807.
- Verdict categories other than `Guilty` (Stage 1 open question 4).
- XHIBIT fixtures — already unit-covered.
