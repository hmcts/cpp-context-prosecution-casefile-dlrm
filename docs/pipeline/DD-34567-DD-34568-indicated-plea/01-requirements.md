# 01 — Requirements

- **Story:** [DD-34568](https://tools.hmcts.net/jira/browse/DD-34568) (epic
  [DD-34567](https://tools.hmcts.net/jira/browse/DD-34567))
- **Status:** Requirements finalised — all open questions closed against the shipped implementation.

## Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | For **LIBRA** migrations only: when a migrated offence's resolved plea value is `INDICATED_GUILTY` or `INDICATED_NOT_GUILTY`, the outgoing courts `Offence` (on `progression.initiate-court-proceedings`) MUST carry an `indicatedPlea` object and MUST NOT carry a `plea` object. |
| FR-1b | For **XHIBIT** migrations: behaviour is unchanged — an indicated plea value stays on the `plea` object and no `indicatedPlea` is emitted. The diversion is gated on `migrationSourceSystemName == "LIBRA"`. |
| FR-2 | For all other plea values (`GUILTY`, `NOT_GUILTY`, `GUILTY_SINGLE_JUSTICE_PROCEDURE`, `ADMITS_BREACH`, `UNFIT_TO_PLEAD`, `CONSENTS`, …) behaviour is unchanged for both source systems: `plea` is populated, `indicatedPlea` is null. |
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
  - *Given* an `INDICATED_GUILTY` LIBRA plea for a defendant in custody with a custody time limit
  - *Then* no custody time limit is set on the offence (indicated guilty still counts as guilty).

- **AC-6 — XHIBIT unchanged**
  - *Given* an `INDICATED_GUILTY` plea on an **XHIBIT** migration
  - *Then* the value stays on `plea` (`pleaValue = INDICATED_GUILTY`) and `indicatedPlea` is null.

## Resolved questions

All four questions raised during drafting are now closed. Resolutions 2–4 are settled in the
shipped implementation (`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`); resolution
1 is a locked design assumption pending only a downstream sign-off that does not block delivery.

1. **`source` value.** *Closed — assumption accepted.* Courts `indicatedPlea.source` is a required
   enum `ONLINE | IN_COURT`; migrated legacy data carries no plea channel. Defaulted to
   **`IN_COURT`** (see ADR `ADR-DD-34568-indicated-plea-source.md`,
   `convertIndicatedPlea(...)` line ~446). Legacy cases originate from the court estate, so
   `IN_COURT` is the faithful representation. The `progression` team confirmation is an audit
   check only — if it ever comes back otherwise the fix is the one-line default in
   `convertIndicatedPlea(...)`; it does not change the design.
2. **`NO_INDICATION`.** *Closed — as designed.* `toIndicatedPleaValue(...)` maps only
   `INDICATED_GUILTY` and `INDICATED_NOT_GUILTY`, returning `null` for everything else, so
   `NO_INDICATION` (and any other value) stays on the `plea` object and is never diverted to
   `indicatedPlea`. This matches the story, which names only the two indicated values.
3. **`originatingHearingId`.** *Closed — left unset.* Optional in the courts schema and not
   available from migrated data, so the field is not populated; the emitted `indicatedPlea`
   validates without it.
4. **Plea-value case/format.** *Closed — case-insensitive matching.* All plea-value comparisons in
   the converter use `equalsIgnoreCase`, so the UPPER_SNAKE reference-data spelling
   (`INDICATED_GUILTY`) matches regardless of any casing variance in the source data.
