# 00 — Input Brief

- **Epic:** [DD-33029](https://tools.hmcts.net/jira/browse/DD-33029) — "Receive case material for migrated case"
- **Story:** [DD-43463](https://tools.hmcts.net/jira/browse/DD-43463) — "Happy path story for migrating case
  materials for a migrated case doc and storage path mapped"
- **Depends on:** DD-43827 (spike, closed). Replaces DD-33261 (withdrawn).
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`).

## Acceptance criteria (as given)

- **AC-1:** an active LIBRA case with Court Store material is accepted by CP → the materials are migrated
  and viewable/manageable in the CP Case Material UI (CDES access control).
- **AC-2:** migrated materials show as in the ticket screenshot, and Uploaded date/time is the original
  Court Store upload date, not the migration date.
- **AC-3:** after migration, manual uploads follow the existing CP process.

## Decisions

1. Happy path only. Partial or failed material handling is out of scope.
2. Uploaded date: handled on the Material side. No date work in this story.
3. The AC-2 screenshot item was uploaded via the CP UI. Illustrative only.
4. No change to anything XHIBIT-related (code, rules, fixtures, behaviour), and no change to Exhibits
   (bundle 9) behaviour.

## Questions for TA / TL

1. **LIBRA SJP cases get CC material rules.** `MigratedCaseFileAggregate` passes `CaseType.CC` for every
   migrated case (`:192`, `:373`). `CaseType` is `SJP / UNKNOWN / CC`, so CC means "not SJP" and selects
   the reference-data-driven `CCMaterialsValidationRuleProvider`, which is right for LIBRA court cases.
   LIBRA SJP cases (initiation code `J`) also get CC rules, not the SJP ones. Is that intended?
   Not changed in this story.
