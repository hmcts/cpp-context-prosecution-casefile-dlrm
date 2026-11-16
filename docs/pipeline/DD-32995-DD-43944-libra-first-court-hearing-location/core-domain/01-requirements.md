# 01 — Requirements

- **Story:** DD-43944 (core-domain half) — see `00-input-brief.md`.
- **Contract:** [ADR-DD-43944](../../adrs/ADR-DD-43944-libra-first-court-hearing-location.md).

## Answered questions

| # | Question | Answer |
|---|----------|--------|
| Q1 | Where does the field go? | Core `migrationSourceSystem` (developer, 2026-11-13). |
| Q2 | Branch? | `team/libra4`, cut from `release/17.104.x` (developer, 2026-11-13). |

## Open questions

| # | Question | Proposed |
|---|----------|----------|
| Q3 | Constrain the value in core? | `type: string` with a description only, no length. stagingdlrm checks the length and pcfdlrm checks reference data; core stays loose like the other `migrationSourceSystem` strings. |
| Q4 | Version Progression and pcfdlrm bump to? | The `-LIBRA4-SNAPSHOT` version `library-release-pipeline` publishes from `team/libra4` when this story merges. The pipeline tags and publishes each merge (e.g. `17.104.7-LIBRA4-SNAPSHOT`, resolvable from artifactory after #227) and moves the branch on (now `17.104.8-LIBRA4-SNAPSHOT`), the same pattern stagingdlrm uses for pcfdlrm's `-LIBRA4-SNAPSHOT` builds. Expected: `17.104.8-LIBRA4-SNAPSHOT` if this is the next merge. |

## Requirements

| ID | Requirement |
|----|-------------|
| FR-1 | Both copies of `migrationSourceSystem.json` declare optional `firstCourtHearingLocation` (string, with a description). Not required; `additionalProperties: false` kept. |
| FR-2 | No other schema or generated-model change. Existing payloads without the field still validate. |
| FR-3 | The generated `uk.gov.justice.core.courts.MigrationSourceSystem` has `getFirstCourtHearingLocation()` / `withFirstCourtHearingLocation()`. |
| FR-4 | `mvn clean install` green. |

## Acceptance criteria

- **AC-1:** GIVEN a `ProsecutionCase` whose `migrationSourceSystem` has `firstCourtHearingLocation`,
  WHEN validated against `criminal-court-public-model`, THEN it is valid.
- **AC-2:** GIVEN one without it, THEN it is valid, as today.
