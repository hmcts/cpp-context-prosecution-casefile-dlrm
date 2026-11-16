# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases
- **Story:** [DD-43944](https://tools.hmcts.net/jira/browse/DD-43944) — Libra Case Level - Add Court Hearing Location
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`). Base branch `team/libra4`.
- **Other half:** the stagingdlrm story in `cpp-context-stagingdlrm`, same directory name. The shared
  decision is [ADR-DD-43944](../adrs/ADR-DD-43944-libra-first-court-hearing-location.md) in this repo.

## Epic framing

DD-32995 brings each LIBRA field's business validation in line with the LIBRA extract. stagingdlrm
validates and maps a LIBRA submission to pcfdlrm (`receive-migrated-case-file`); pcfdlrm validates
against reference data and sends the case to Progression (`initiate-court-proceedings`). XHIBIT is in
production and must not change.

## Request

As given in the ticket: add an optional field giving the court to which the case was originally sent
by the prosecuting authority: `firstCourtHearingLocation`, A7, OU code, optional for every case type.
"Will be of use in particular when no hearings are received."

**Requirement change (developer, 2026-11-13):** the field goes **inside `migrationSourceSystem`**.

**Decision (developer, 2026-11-13):** for Progression, core-domain is updated so the field is part of
core `migrationSourceSystem` and reaches Progression inside `ProsecutionCase.migrationSourceSystem`.

## Current behaviour (code, `team/libra4`)

- **pcfdlrm `migrated-migrationSourceSystem.json`:** closed; `migrationSourceSystemName`,
  `migrationSourceSystemCaseIdentifier`. Referenced from `migrated-case-details.json`.
- **Aggregate:** reads `migrationSourceSystemName` to pick the LIBRA or XHIBIT rule sets; the whole
  `receiveMigratedCaseFile` (including `migrationSourceSystem`) is recorded in
  `migrated-case-file-received`.
- **To Progression:** `MigratedCaseToProsecutionCaseConverter.buildMigrationSourceSystem` copies only
  the name and case identifier into `ProsecutionCase.migrationSourceSystem`. The core schema
  (`criminal-court-public-model` 17.104.4, and core-domain `main` / `team/dlrm*`) is closed and has no
  court field.
- **Existing court route to Progression:** `caseDetails.sendingCourt` → `CourtLocationEnricher`
  (reference-data OU lookup) → `ProsecutionCase.sendingCourt` (`CourtCentre`) when found. LIBRA sends
  no `sendingCourt`, and the LIBRA case rule sets leave out `SendingCourtValidationRules`.
- **No hearings:** no `listHearingRequests`; the case still reaches Progression.
- **Progression:** the three query schemas that expose `migrationSourceSystem`
  (`prosecutioncase.caag`, `inactive.migration.case.summary`, `get-case-at-a-glance`) all `$ref` the
  core schema, and no Progression Java rebuilds the object, so Progression needs only a `coredomain`
  bump to accept and return the field.
- **Core-domain line:** pcfdlrm, stagingdlrm and Progression use `coredomain` 17.104.4, released from
  `cpp-platform-core-domain` `release/17.104.x` (currently 17.104.7-SNAPSHOT).
