# 01 — Requirements

- **Story:** [DD-43499](https://tools.hmcts.net/jira/browse/DD-43499) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Status:** Stage 1 approved 2026-10-01.
- **Scope of this doc:** pcfdlrm business validation. stagingdlrm schema gaps are listed below for
  a separate stagingdlrm story (one story per repo).

## Framing

stagingdlrm's API schema already guarantees presence/length of every mandatory case field except
the Summons-only `informant` (`00-input-brief.md`). So the pcfdlrm work is the **business** checks:
LIBRA case-level rules exist but their result is discarded. Make LIBRA act on them exactly as
XHIBIT does. XHIBIT behaviour unchanged.

"Reject" = `migrated-case-file-processed`, `processingIsSuccessful=false`, with a description —
the existing XHIBIT mechanism.

## Functional requirements (pcfdlrm)

Invalid values follow XHIBIT's existing handling (user decision). XHIBIT today, per case-level
problem code (`MigratedCaseFileAggregate` ~L214-287):

| ID | Requirement | XHIBIT precedent |
|----|-------------|------------------|
| FR-1 | For LIBRA, act on the prosecuting-authority and case-marker problems the same way XHIBIT does. | — |
| FR-2 | `prosecutingAuthority` not a recognised prosecutor OU code → reject, "Invalid Prosecuting Authority". | `PROSECUTOR_OUCODE_NOT_RECOGNISED` → reject |
| FR-3 | Invalid `caseMarkers` → accept, emit `migrated-case-validated-with-warnings`; invalid markers not sent to progression — exactly as XHIBIT. | `CASE_MARKER_IS_INVALID` → warning; converter drops invalid markers |
| FR-4 | `initiationCode` not in refdata initiation-types → accept (problem raised, not acted on). | `CASE_INITIATION_CODE_INVALID` → ignored |
| FR-5 | `originatingOrganisation`, `cpsOrganisation` → no business check. | no rule |
| FR-6 | XHIBIT validation paths unchanged. | — |
| FR-7 | Unit tests per outcome. No negative LIBRA ITs (user direction); existing ITs stay green. | — |

## For a stagingdlrm story (schema)

| ID | Gap |
|----|-----|
| S-1 | `prosecutorCaseReference`: add `[A-Za-z0-9-]` pattern. |
| S-2 | `migrationSourceSystemCaseIdentifier`: add maxLength 100. |
| S-3 | `informant`: missing on a LIBRA Summons (`S`) → reject, as a **business rule** in `MigratedCaseValidationRuleEngine` (LIBRA set) — `migrated-case-submission-rejected`. Not mapped to pcfdlrm (unchanged). |

## Acceptance criteria

- **AC-1:** LIBRA case rejected or accepted per the ticket table, invalid values handled as
  XHIBIT (FR-1 – FR-5; S-1 – S-3 in stagingdlrm).

## Open questions

1. ~~Invalid values~~ — **resolved:** mandatory field invalid → follow XHIBIT; **optional field
   invalid → nullify** (ticket note, confirmed by user). **Diverges from ticket** in two places:
   ticket says reject for a bad `originatingOrganisation` (FR-5: no check) and `initiationCode`
   (FR-4: accept; staging enum C/Q/J/R/S already blocks bad codes, so only a refdata mismatch
   reaches here). Confirm with BA.
2. ~~Generated `prosecutorCaseReference`~~ — **resolved:** never generated (known); the ticket's
   "generate then accept" doesn't apply. Stays required in stagingdlrm. Out of scope.
3. ~~`migrationSourceSystemCaseIdentifier` uniqueness~~ — **resolved:** ignore; it is never
   unique (known). Out of scope.
4. ~~Initiation code `R`~~ — **resolved:** not a blocker. stagingdlrm allows it for LIBRA; pcfdlrm
   doesn't refuse it (refdata check only, ignored per XHIBIT; falls to the common LIBRA rule set).
5. ~~CPS `prosecutingAuthority` (A codes)~~ — **resolved:** same as XHIBIT — one
   `get.prosecutor.by.oucode` lookup, no separate cpsOrg handling.
6. ~~`informant` Summons check — SV or BV?~~ — **resolved: BV in stagingdlrm** (S-3):
   - The API schema is shared by XHIBIT and LIBRA and holds no source-system conditionals (ADR-002);
     "required if LIBRA and `S`" would need nested draft-04 `anyOf`/`not` — the complication.
   - Precedent: per-source conditional rules already live in the rule engine — LIBRA hearing
     `RequiredFieldRule`s, XHIBIT `AtLeastOneOfRule` (`dateOfCommittal`/`dateOfSending`), and
     DD-43203 initiation-code sets.
   - pcfdlrm's `SummonsCodeValidationRule` is the same shape (applies only when `S`) but can't be
     used: `informant` isn't mapped to pcfdlrm by design.
7. ~~Story split~~ — **resolved:** same story, fulfilled separately per repo. stagingdlrm half
   (S-1 – S-3) handed off in `cpp-context-stagingdlrm`
   `docs/pipeline/DD-32995-DD-43499-case-level-business-validation/00-input-brief.md`.
