# 01 — Requirements

- **Story:** [DD-34568](https://tools.hmcts.net/jira/browse/DD-34568) (epic
  [DD-34567](https://tools.hmcts.net/jira/browse/DD-34568))
- **Status:** Drafted from instruction + code investigation — pending Jira reconciliation.

## Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | When a migrated offence's resolved plea value is `INDICATED_GUILTY` or `INDICATED_NOT_GUILTY`, the outgoing courts `Offence` (on `progression.initiate-court-proceedings`) MUST carry an `indicatedPlea` object and MUST NOT carry a `plea` object. |
| FR-2 | For all other plea values (`GUILTY`, `NOT_GUILTY`, `GUILTY_SINGLE_JUSTICE_PROCEDURE`, `ADMITS_BREACH`, `UNFIT_TO_PLEAD`, `CONSENTS`, …) behaviour is unchanged: `plea` is populated, `indicatedPlea` is null. |
| FR-3 | `indicatedPlea` MUST satisfy the courts schema: `offenceId`, `indicatedPleaValue`, `indicatedPleaDate`, `source` all present. |
| FR-4 | `indicatedPleaValue` maps 1:1 from the plea value string to the enum (`INDICATED_GUILTY` / `INDICATED_NOT_GUILTY`). |
| FR-5 | `indicatedPleaDate` mirrors the existing `plea` date rule: use the migrated plea date; if the plea type's guilty flag is `No` and the date is missing, default to today. |
| FR-6 | Guilty-derived downstream logic MUST be preserved. An `INDICATED_GUILTY` plea continues to count as "guilty" for: conviction-date derivation, convicting-court resolution, custody-time-limit suppression, and verdict handling — exactly as before this change (when the value lived on `plea`). |

## Acceptance criteria (GDS "Given/When/Then")

- **AC-1 — Indicated guilty**
  - *Given* a migrated offence whose reference-data plea value is `INDICATED_GUILTY`
  - *When* the case is converted for `progression`
  - *Then* the offence has an `indicatedPlea` with `indicatedPleaValue = INDICATED_GUILTY`, the offence id, the plea date, and a `source`; and the offence's `plea` is null.

- **AC-2 — Indicated not guilty**
  - *Given* a migrated offence whose plea value is `INDICATED_NOT_GUILTY`
  - *Then* the offence has an `indicatedPlea` with `indicatedPleaValue = INDICATED_NOT_GUILTY`; `plea` is null.

- **AC-3 — Missing date default**
  - *Given* an `INDICATED_NOT_GUILTY` plea with no plea date
  - *Then* `indicatedPleaDate` defaults to today.

- **AC-4 — Non-indicated unchanged**
  - *Given* a `GUILTY` or `NOT_GUILTY` plea
  - *Then* `plea` is populated as today and `indicatedPlea` is null.

- **AC-5 — Guilty derivation preserved**
  - *Given* an `INDICATED_GUILTY` plea for a defendant in custody with a custody time limit
  - *Then* no custody time limit is set on the offence (indicated guilty still counts as guilty).

## Open questions (require PO / ticket confirmation)

1. **`source` value (BLOCKING for schema validity).** Courts `indicatedPlea.source` is a required
   enum `ONLINE | IN_COURT`. Migrated legacy data carries no plea channel. **Assumption made:
   `IN_COURT`** (see ADR `ADR-DD-34568-indicated-plea-source.md`). Confirm with the PO /
   `progression` team.
2. **`NO_INDICATION`.** The enum also allows `NO_INDICATION`. The story names only the two
   indicated values, so `NO_INDICATION` is treated as a normal `plea` value (not moved to
   `indicatedPlea`). Confirm this is correct, or whether a `NO_INDICATION` plea value can occur.
3. **`originatingHearingId`.** Optional; not available from migrated data, so left unset. Confirm
   `progression` does not need it for migrated cases.
4. **Plea-value case/format.** Reference data supplies UPPER_SNAKE (`INDICATED_GUILTY`). Matching
   is case-insensitive to be safe. Confirm no other spellings reach the converter.
