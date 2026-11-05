# 02 — Design

- **Story:** [DD-43463](https://tools.hmcts.net/jira/browse/DD-43463)
- **Inputs:** `01-requirements.md` (approved 2026-11-03). No production code changed at this stage.

## Summary

No new event, schema, subscription or rule. One change in `MigratedCaseFileAggregate.acceptMigratedCase`:
a non-XHIBIT case-level court document is passed through unchanged. Every other document, including
all XHIBIT documents, goes through today's code. LIBRA defendant-level documents keep today's
"all case defendants" behaviour (FR-4).

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `MigratedCaseFileAggregate.java` `acceptMigratedCase` (`:661-674`) | `withCourtDocument(...)` becomes: if `isXhibit(receiveMigratedCaseFile)` or `value.getDocumentCategory().getDefendantDocument() != null` → today's builder, lines unchanged; else → `value`. | FR-3, FR-4, FR-7 |
| C-2 | IT stubs `referencedata.get-all-document-type-access.json`, `referencedata.query.get.all-parent-bundle-section.json` | Add the STE rows `WS` (Case level), `MCEB` and `UM` (Defendant level), and bundle `3` → `WS`. New rows only; existing rows unchanged. | FR-8 |
| C-3 | `ReceiveMigratedCaseFileIT` (no new fixture) | Reuse the XHIBIT allocation-decision payload with LIBRA swapped in and one material per accepted bundle (1, 2, 3, 6, 8, 99). | FR-8 |

## Options considered

| Option | Why not |
|---|---|
| **A. XHIBIT or defendant-level → today's code; else pass through (chosen)** | — XHIBIT path untouched; LIBRA defendant-level unchanged. |
| B. Null-safe `withValuesFrom` for everyone | Edits the XHIBIT path (user: nothing XHIBIT). |
| C. Pass every LIBRA category through | Defendant-level docs would go to the first defendant only (user: keep all defendants). |

## Downstream

- `material-ready-for-court-document` → `MaterialReadyForCourtDocumentProcessor` → `progression.add-court-document`
  (unchanged). A case-level document now arrives with `documentCategory.caseDocument` only.

## Edge cases

- **Unknown bundle code:** fails at `:637-638`, as today (open question 3).
- **Applications category:** `MaterialEventProcessor` builds `applicationDocument`; not reachable for
  accepted bundles (STE has no Applications row for them). Would pass through under C-1. Not tested.

## Tests (FR-8)

- `MigratedCaseFileAggregateTest`:
  - LIBRA case-level court document → `material-ready-for-court-document` carries the same
    `documentCategory` (`caseDocument`, no `defendantDocument`).
  - LIBRA defendant-level → `defendantDocument.defendants` = all case defendants (as today).
  - XHIBIT defendant-level → as today (all case defendants). Both use a two-defendant case.
- `ReceiveMigratedCaseFileIT`: new LIBRA test — receive the C-3 payload, check each `material-added`
  level (3, 8 Case; others Defendant), echo it as `material.material-added`, trigger accept, check each
  `material-ready-for-court-document` (`caseDocument` or `defendantDocument`). No negative LIBRA IT;
  existing ITs stay green.
