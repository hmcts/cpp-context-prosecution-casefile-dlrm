# 03 — User Story

## DD-34568 — Emit `indicatedPlea` for indicated plea values

**As** the `progression` context consuming migrated prosecution case files
**I want** an offence's indicated plea to arrive in the dedicated `indicatedPlea` object
**So that** the allocation decision is driven correctly instead of an indicated plea being
mistaken for an entered `plea`.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm` (Stage 7 CI is per-repo).
- Converter that builds `progression.initiate-court-proceedings`.

### Acceptance criteria

See `01-requirements.md` AC-1 … AC-6. Summary:

1. **LIBRA** `INDICATED_GUILTY` → offence carries `indicatedPlea` (correct enum, offence id, date,
   source); `plea` is null.
2. **LIBRA** `INDICATED_NOT_GUILTY` → same, with `INDICATED_NOT_GUILTY`.
3. Missing date on a LIBRA indicated-not-guilty plea → `indicatedPleaDate` defaults to today.
4. Non-indicated plea values behave exactly as before (`plea` set, `indicatedPlea` null).
5. `INDICATED_GUILTY` still counts as guilty for conviction date, convicting court, and custody
   time limit (both source systems).
6. **XHIBIT** indicated values are unchanged — they stay on `plea`, no `indicatedPlea`.

### Definition of done

- [x] Converter change implemented.
- [x] Unit tests for AC-1..AC-6; existing suite still green (48/48 in the converter test).
- [x] Scope limited to LIBRA migrations; XHIBIT behaviour unchanged and covered by tests for both
      indicated values.
- [x] Processor-level tests assert `indicatedPlea` (and absent `plea`) on the serialised
      `progression.initiate-court-proceedings` command for both indicated values (12/12).
- [x] Integration test added (`receiveMigratedLibraCaseFileWithIndicatedPlea`) + `INDICATED_NOT_GUILTY`
      plea-type stub row. Runs in the Docker IT env only.
- [x] Full `pcfdlrm-event-processor` module tests green.
- [ ] Open question #1 (`source` = `IN_COURT`) confirmed by PO / `progression` team.
- [ ] (Recommended) integration-test coverage in `pcfdlrm-integration-test`.
- [ ] Jira DD-34567 / DD-34568 text reconciled against this pipeline (see brief provenance note).

### Out of scope

- Public event `public.pcfdlrm.migrated-case-file-processed` (status-only; no plea data).
- Any schema / RAML / subscription / Liquibase change (courts model already supports
  `indicatedPlea`).
- The pre-existing, unrelated test-compile break in `pcfdlrm-command-handler`
  (`MigratedDefendant.getNumPreviousConvictions` missing) — flag separately.
