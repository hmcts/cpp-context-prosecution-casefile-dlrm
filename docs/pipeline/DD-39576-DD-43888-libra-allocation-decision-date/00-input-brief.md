# 00 — Input Brief

- **Parent:** [DD-39576](https://tools.hmcts.net/jira/browse/DD-39576)
- **Bug:** [DD-43888](https://tools.hmcts.net/jira/browse/DD-43888) — BE-Libra-AutoRoute-allocation decision date validation is not fired
- **Blocks:** [DD-43502](https://tools.hmcts.net/jira/browse/DD-43502) — Business validation for Offence level items (epic DD-32995)
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)

## Bug

A LIBRA case whose offence has an `allocationDecision` with only `motReasonId` (no
`allocationDecisionDate`) is accepted. Expected: rejected.

Latest ticket comment: BA agrees `allocationDecisionDate` is CM for a magistrates' court case.

## DD-43502 rows (verbatim)

| Field | Format | Rule | SJP/Summons/Charge/Requisition | Missing |
|---|---|---|---|---|
| `allocationDecision` | A36 | — | O | Accept the case |
| `allocationDecisionDate` | D10 | Should be provided if allocation decision is provided | CM | If there is an allocation decision then reject otherwise, accept the case |

## Cause

DD-43502 left `allocationDecisionDate` unvalidated, pending the BA
(`DD-32995-DD-43502-offence-level-business-validation/01-requirements.md:20`, FR-7).

- `migrated-allocation-decision.json` requires only `motReasonId`.
- No validation rule reads the allocation decision; `MigratedCaseFileAggregate.getLibraOffenceRejection`
  has no allocation-date check.
- `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.getAllocationDecisionDate` sends a null
  date to progression.

## Related (asked on DD-43888, awaiting BA)

An unknown `motReasonId` (not in mode-of-trial ref data) is accepted and the whole allocation
decision is dropped (`ModeOfTrialRefDataEnricher.java:32-41`; converter `retrieveModeOfTrialReason`
→ null → `buildAllocationDecision` returns null).
