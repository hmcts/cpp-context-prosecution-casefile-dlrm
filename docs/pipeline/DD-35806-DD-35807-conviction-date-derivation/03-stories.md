# 03 — User Story

## DD-35807 — Automation coverage for conviction-date derivation (indicated guilty / guilty pleas)

**As** a tester of the `pcfdlrm` LIBRA migration path
**I want** integration-test coverage proving a guilty or indicated-guilty plea drives the
offence's conviction date
**So that** the existing derivation behaviour is verified for LIBRA and protected against
regression, instead of resting on unit tests that only exercise XHIBIT.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm` (Stage 7 CI is per-repo).
- `pcfdlrm-integration-test` fixtures + `ReceiveMigratedCaseFileIT` parametrization only.

### Acceptance criteria

See `01-requirements.md` AC-1 / AC-2. Summary:

1. **LIBRA, indicated-guilty plea with a date** → offence's `convictionDate` equals that plea date.
2. **LIBRA, guilty plea with a date** → offence's `convictionDate` equals that plea date.

### Definition of done

- [ ] `libra-journey.json` expected fixture asserts `convictionDate` for the existing guilty-plea
      offence (AC-2), value confirmed against a real Docker IT run, not assumed.
- [ ] New `libra-indicated-guilty-plea` command + expected fixture pair added, asserting both
      `indicatedPlea` and `convictionDate` (AC-1), values likewise confirmed against a real run.
- [ ] `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` extended with the new
      `@CsvSource` row — no new IT class.
- [ ] `./runIntegrationTests.sh` green locally for the full `pcfdlrm-integration-test` module,
      including the two new/changed cases.
- [ ] `mvn clean install` (unit tests) still green — no production code touched.
- [ ] If either baseline run in Stage 5 shows the converter is *not* actually deriving the
      conviction date for one of these cases, stop and raise it rather than loosening the
      assertion — that would be a production defect, outside this story's scope (FR-4).

### Out of scope

- Any production code change (unless a real defect is found — see above).
- Plea values other than `GUILTY` / `INDICATED_GUILTY` (Stage 1 open question 2).
- Verdict-derived conviction dates (Stage 1 open question 3) — already covered by existing tests.
- XHIBIT fixtures (Stage 1 open question 4) — XHIBIT already has unit coverage for both plea codes.
