# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases
- **Bug:** [DD-43870](https://tools.hmcts.net/jira/browse/DD-43870) — DLRM Libra auto - Future DOB is not getting rejected
- **Blocks:** [DD-43493](https://tools.hmcts.net/jira/browse/DD-43493) — Business validation for Defendant level items
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)

## Epic framing

LIBRA cases migrated through DLRM must pass business validation before `pcfdlrm` hands them to
`progression`. Each field in the migration schema has a rule. When a rule is broken, the case is
either rejected, or the value is nulled and the case accepted.

## Bug

A LIBRA case whose defendant has a future date of birth is accepted. The DOB is passed
through to CP, which shows a negative age (a 2099 date of birth shows as "-72 years old").

For comparison, an SPI case with a future DOB lands in CP's "Fix case interface errors" screen with
"Defendant dob is future".

## Decision (ticket comments)

- **Ticket comment, about 1 month ago:** the Xhibit and Libra cases get separate tickets. A future defendant DOB
  is emptied.
- **Ticket comment, 3 days ago (agreed by two stakeholders):** "accepting the case but nullifying the
  future DOB and adding the case to the reconciliation report is the way to go."

So: accept the case, null the future DOB and report it.

**Scope (agreed 2026-11-06):** the change applies to **both LIBRA and XHIBIT**, in this story.

## Current behaviour (code)

- `DefendantDateOfBirthValidationRule` raises `DEFENDANT_DOB_IN_FUTURE` (Europe/London "today")
  as a **warning**. The case is accepted and the value kept. This was decision D6 in DD-43493
  (`DD-32995-DD-43493-defendant-validation/01-requirements.md:94`, "keep the current behaviour …
  value kept"). This ticket overturns D6 for both sources.
- The warning is already recorded: `defendant-validation-failed` (DLRM channel) and
  `MigratedCaseValidatedWithWarnings` ("Defendant validation", `DEFENDANT_DOB_IN_FUTURE : [<dob>]`).
- `ProsecutionCaseFileHelper.validateDefendantErrors` already clears a future **parent/guardian**
  DOB for LIBRA only (`applyParentGuardianRules`, DD-43501 G1). The defendant DOB has no
  equivalent.
- `ProsecutionMigrationCaseToCCPersonDefendantConverter:97` handles a null DOB safely.

## Reconciliation report

Adding the case to the reconciliation report is a **manual process**, tracked in a separate sub-task
of DD-43870 (agreed 2026-11-06). It is out of scope for this story; no code is needed for it here.
