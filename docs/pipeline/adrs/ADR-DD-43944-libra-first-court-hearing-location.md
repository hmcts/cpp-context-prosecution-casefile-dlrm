# ADR — Carrying the LIBRA first court hearing location to CP

- **Status:** Accepted (2026-11-13); input location revised 2026-11-25
- **Story:** [DD-43944](https://tools.hmcts.net/jira/browse/DD-43944) / epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Repos:** `cpp-context-stagingdlrm`, `cpp-context-prosecution-casefile-dlrm`, `cpp-platform-core-domain`,
  `cpp-context-progression`. This ADR is the single copy; the other stories link here.

## Context

LIBRA cases need the court the prosecuting authority first sent the case to, especially when no
hearings are migrated. The field, `firstCourtHearingLocation` (7-character OU code, optional), belongs
in `migrationSourceSystem` (developer decision). That object comes from `manifest.json` and is closed
at every layer: Function App, stagingdlrm canonical, pcfdlrm, and core `criminal-court-public-model`,
which Progression validates against.

On 2026-11-25 the business asked for the field to be part of `caseDetails` in the LIBRA extract
(`case.json`) rather than the manifest. The downstream contract (`migrationSourceSystem` from
stagingdlrm canonical onwards) is unchanged; only the Function App input changes.

## Decision

1. **Input:** LIBRA sends `caseDetails.firstCourtHearingLocation` in `case.json` (until 2026-11-25:
   `migrationSourceSystem.firstCourtHearingLocation` in `manifest.json`, which the manifest schema no
   longer accepts).
2. **Core-domain:** `DesignSchemas/global/migrationSourceSystem.json` gains optional
   `firstCourtHearingLocation` on core-domain `team/libra4` (cut from `release/17.104.x`, the line
   consumers' 17.104.4 came from); Progression and pcfdlrm bump to the version it publishes.
3. **Progression:** on its `team/libra4` (cut from `main`), bumps `coredomain`; no code change (its query schemas `$ref` the core schema).
4. **stagingdlrm:** the Function App's LIBRA case schema declares it on `caseDetails` (string, length 7),
   and the Function App moves it from `caseDetails` into `migratedCase.migrationSourceSystem` (canonical
   `caseDetails` is closed). The canonical `migrationSourceSystem` declares it; `MigratedCaseConvertor`
   copies it to pcfdlrm.
5. **pcfdlrm:** declares it, bumps `coredomain`, and copies it to `ProsecutionCase.migrationSourceSystem`. The OU code is looked up in reference data; not found → a "Case validation" warning
   (`FIRST_COURT_LOCATION_OUCODE_INVALID`) with the original value (for the reconciliation report), the field is set to null, and the case is accepted.
6. **Absent field:** today's behaviour everywhere. XHIBIT unchanged.

## Consequences

- Order: core-domain release → Progression bump **deployed** → pcfdlrm → stagingdlrm mapping. An older
  Progression rejects `initiate-court-proceedings` carrying the field, because core is closed.
- The stagingdlrm and pcfdlrm schema declarations can merge early; they accept and record the field
  without sending it on.
- Four stories, one per repo.

## Stories

| Repo | Branch | Story docs |
|---|---|---|
| `cpp-platform-core-domain` | `team/libra4` (from `release/17.104.x`) | In this repo: `docs/pipeline/DD-32995-DD-43944-libra-first-court-hearing-location/core-domain/` (core-domain has no docs folder) |
| `cpp-context-progression` | `team/libra4` (from `main`) | `openspec/changes/2026-11-13-dd-43944-first-court-hearing-location/` |
| `cpp-context-prosecution-casefile-dlrm` | `team/libra4` | `docs/pipeline/DD-32995-DD-43944-libra-first-court-hearing-location/` |
| `cpp-context-stagingdlrm` | `team/libra4` | `docs/pipeline/DD-32995-DD-43944-libra-first-court-hearing-location/` |
