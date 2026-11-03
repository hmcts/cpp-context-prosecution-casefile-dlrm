# 03 — User Story

## DD-43463 — LIBRA case material migration (happy path)

**As** an authorised HMCTS user
**I want** a migrated LIBRA case's Court Store materials sent to CP when the case is accepted
**So that** all its case documents are available in the CP Case Material UI.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`.
- `MigratedCaseFileAggregate.acceptMigratedCase`, plus IT reference-data stub rows and one new test in
  `ReceiveMigratedCaseFileIT`
  (see `02-design.md` C-1 – C-3).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-8. On accept, each material raises `material-ready-for-court-document`:
1. LIBRA case-level material (bundle 3 or 8) → court document with `caseDocument`, no error.
2. LIBRA defendant-level material (bundle 1, 2, 6 or 99) → `defendantDocument` listing all case
   defendants, as today.
3. XHIBIT outcomes unchanged.
4. Unknown bundle code → fails as today.

### Definition of done

- [x] C-1 – C-3 implemented; no other production change; nothing XHIBIT-related changed.
- [x] Unit tests in `MigratedCaseFileAggregateTest` (`02-design.md` Tests).
- [x] `mvn clean install` green; `./runIntegrationTests.sh` green (plus one positive LIBRA material IT;
      no negative LIBRA IT).

### Out of scope

- Partial or failed material handling; Uploaded date (Material side); Exhibits (bundle 9);
  `CaseType.CC` for LIBRA SJP (TA/TL question).
