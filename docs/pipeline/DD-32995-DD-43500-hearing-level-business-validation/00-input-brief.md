# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Story:** [DD-43500](https://tools.hmcts.net/jira/browse/DD-43500) — "Business validation for Hearing
  level items"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`). stagingdlrm unchanged.

> AC taken from the ticket export — no Jira fetch. Reconcile against the live ticket before sign-off.

## Acceptance criteria (as given)

**AC-1 — Hearing-level business validation:** GIVEN a case received onto CP via DLRM LIBRA migration,
WHEN validation runs for the hearing elements and a business rule is not met, THEN the system rejects
or accepts the case per the table below.

> Ticket note: invalid entries are nulled and follow "required behaviour for missing fields". Per the
> DD-43499 decision this holds for optional fields only; all fields below are mandatory → follow XHIBIT.

| Field | Format | Business rule | SJP / Summons / Charge / Postal Req | Ref data | Missing → |
|---|---|---|---|---|---|
| `courtHearingLocation` | A7 | — | M / M / M / M | organisationUnit (OU code) | Follow XHIBIT |
| `courtRoomId` | A36 | As per CP court room ref data | M / M / M / M | courtRoom | Follow XHIBIT |
| `dateOfHearing` | D10 | — | M / M / M / M | — | Accept case, don't create hearing |
| `timeOfHearing` | T8 | — | M / M / M / M | — | Accept case, default 10:00 |
| `durationMinutes` | N5 | Converted to hours/days; zero → CP default (typically 20 min) | M / M / M / M | — | Accept case, CP ref-data default |
| `hearingType` | A10 | — | M / M / M / M | hearingType | Follow XHIBIT (ticket: data mapping to double-check) |

## Investigation

stagingdlrm read from `origin/team/libra1`.

### stagingdlrm

**Schema** (`migrated-hearing.json`, shared by XHIBIT and LIBRA) already enforces every Format entry
except `courtRoomId`:

| Field | Schema today |
|---|---|
| `courtHearingLocation` | required, length 7 |
| `courtRoomId` | integer (ticket says A36) |
| `dateOfHearing` | `date` definition |
| `timeOfHearing` | length 8, `HH:mm:ss` |
| `durationMinutes` | integer, max 99999 |
| `hearingType` | required, maxLength 10 |

A schema failure rejects the whole submission, so "invalid → null → missing behaviour" can't happen
for format errors. No per-source schema conditionals (stagingdlrm ADR-002).

**Rule engine** (`MigratedCaseValidationRuleEngine`, LIBRA set, added in DD-43081): `RequiredFieldRule`
on every hearing's `courtRoomId`, `dateOfHearing`, `timeOfHearing` → missing = reject. Contradicts
the ticket for all three.

### pcfdlrm

Hearing rule set (`CcProsecutionValidationRuleProvider` ~L111) runs for all sources:
court location OU + court room (`CourtHearingLocationValidationRule`), past date, date earlier than
offence, hearing type, week commencing, no matching defendants.

| Behaviour | XHIBIT | LIBRA today |
|---|---|---|
| Hearing problems → `migrated-case-validated-with-warnings` (case accepted) | yes | **no** — gated by `isXhibit` (`MigratedCaseFileAggregate` ~L411) |
| `NO_MATCHING_DEFENDANTS_FOR_HEARING` → reject | yes | no — gated (~L542) |
| Hearing skipped if OU / hearing type not in ref data, or past date (processor converter) | yes | yes (source-agnostic) |
| No matching court room → listed without room | yes | yes |
| Missing time on a fixed hearing → 10:00 London (aggregate ~L387) | yes | yes, but unreachable (staging rejects) |
| `durationMinutes` null / 0 → hearing-type default (converter ~L240) | yes | yes |
