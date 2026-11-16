# 00 — Input Brief

> **Self-contained** — everything an SDLC stage needs for this story is in this directory.
>
> Kept in the pcfdlrm repo because `cpp-platform-core-domain` has no docs folder (developer, 2026-11-16).
> The code change itself is in `cpp-platform-core-domain` (PR #228).

| | |
|---|---|
| Epic | [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases |
| Story | [DD-43944](https://tools.hmcts.net/jira/browse/DD-43944) — Libra Case Level - Add Court Hearing Location (core-domain half) |
| Repo | `cpp-platform-core-domain` — **this repo only**. Branch `team/libra4` (cut from `release/17.104.x`); changes go in through `dev/*` PRs. |
| Shared decision | [ADR-DD-43944](../../adrs/ADR-DD-43944-libra-first-court-hearing-location.md) in `cpp-context-prosecution-casefile-dlrm` (single copy) |

## Epic framing

LIBRA cases are migrated to CP through stagingdlrm and pcfdlrm, which sends each case to Progression
(`initiate-court-proceedings`) as a core `ProsecutionCase`. Migration details travel in
`ProsecutionCase.migrationSourceSystem`, defined in this repo.

## Request

LIBRA sends `firstCourtHearingLocation` (A7 OU code, optional): the court to which the case was
originally sent by the prosecuting authority, "of use in particular when no hearings are received". The
developer decided it travels inside `migrationSourceSystem` and reaches Progression there.

## Current behaviour (`release/17.104.x` @ `66ce1aa14`)

- `migrationSourceSystem.json` exists twice, kept in step by hand (DD-42378 changed both in one commit):
  - `DesignSchemas/global/migrationSourceSystem.json`
  - `criminal-court-public-model/src/main/resources/json/schema/global/migrationSourceSystem.json` (packaged)
- Properties: `migrationSourceSystemName`, `migrationSourceSystemCaseIdentifier`,
  `migrationCaseStatus` (ACTIVE / INACTIVE), `defendantFineAccountNumbers`. **`additionalProperties: false`**,
  so a payload carrying any other field fails validation in every consumer.
- Consumers on this line: pcfdlrm, stagingdlrm and Progression use 17.104.4; Progression's
  `main` uses 17.104.6.
