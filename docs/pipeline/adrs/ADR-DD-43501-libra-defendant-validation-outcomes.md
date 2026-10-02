# ADR: LIBRA-scoped defendant validation outcomes (reject / null / default)

- **Status:** Proposed
- **Date:** 2026-10-02
- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) /
  epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Repo:** `cpp-context-prosecution-casefile-dlrm`
- **Design:** `docs/pipeline/DD-32995-DD-43501-parent-guardian/02-design.md`

## Context

DD-32995 requires every field in the LIBRA migration schema to end in one of three outcomes:

- **reject** the whole case,
- **null** the value and warn, or
- **default** the value and warn.

`pcfdlrm` already has these outcomes, but only for XHIBIT or for every source system together:

- **Rule selection is not source-aware for defendants.** `DLRM_MIGRATION` defendants resolve to
  `defendantValidationMapDlrm`, which is the same object as the SPI map, for both XHIBIT and
  LIBRA (`CcProsecutionValidationRuleProvider.java:310`, `:338-339`).
- **Sanitising is XHIBIT-only.** It is `applyRuleToDefendantFields`, gated on
  `"XHIBIT".equals(...)` (`ProsecutionCaseFileHelper.java:118-120`).
- **Defendant-driven rejection is XHIBIT-only.** It is `hasOffenceProblems`, gated on `isXhibit`
  (`MigratedCaseFileAggregate.java:420-460`).
- **Problem codes are shared across sources with different meanings.** For example,
  `INVALID_GUARDIAN_POST_CODE` is a warning for XHIBIT but a rejection reason for LIBRA under
  DD-43501.

DD-43501 (parent/guardian) is the first story that needs a LIBRA defendant-level rejection and
LIBRA sanitising. The other DD-32995 stories (non-guardian defendant fields, offences) will need
the same thing.

## Decision

1. **Select the rule set by source system.** `getDefendantValidationRules` gains a
   `sourceSystemName` overload. LIBRA S/C/Q cases resolve to a separate
   `defendantValidationMapDlrmLibra`. It is built by *filtering and replacing* entries in the
   shared sets, so it does not copy them. The existing maps and the 3-arg method are unchanged.
2. **Keep one outcome table.** `LibraParentGuardianOutcomes` is the only place that says, for
   each LIBRA problem code, whether it means REJECT, NULL or DEFAULT, and which field a NULL
   removes. Validation, the sanitiser and the reject decision all read this table. Later stories
   add their own table, or extend this one, rather than adding new `hasX(...)` checks in the
   aggregate.
3. **Collect rejections per defendant, in scope, in the helper.** The helper returns
   `DefendantValidationOutcome`: the existing `MigratedDefendantWithProblem` plus the list of
   in-scope rejection problems. The aggregate rejects when that list is non-empty. It never
   rejects on a problem code alone, because the same code can be a warning in another scope.
4. **Use the existing outcome contract.** A rejection emits one
   `MigratedCaseFileProcessed(processingIsSuccessful=false)`. Its description is a fixed prefix
   followed by the distinct problem codes, with no values and no identifiers. The event is
   published unchanged as `public.pcfdlrm.migrated-case-file-processed`. There is no new event
   and no new field.
5. **Redact values by convention.** A LIBRA problem sets `ProblemValue.value` to its field key,
   so warnings and `DefendantValidationFailed` carry the field path but no data.

## Consequences

- XHIBIT, SPI, MCC, CIVIL and LIBRA J/R/O behaviour is unchanged. The existing provider tests
  that pin the rule classes per channel and code still prove this.
- LIBRA S/C/Q cases that used to pass with warnings can now be rejected. Fixtures with partial
  guardian blocks must be updated.
- `stagingdlrm` sees new `description` strings on failed outcomes. The prefix must not overlap
  `stagingdlrm`'s `stagingContextErrors` markers (`StagingDlrmEventProcessor.java:51-55`), or the
  failure would be counted as a staging-context error.
- The decision is two-way. There are no schema, contract or data changes, and reverting removes
  the behaviour. Moving one code between REJECT, NULL and DEFAULT is a single entry in the table.

## Alternatives considered

- **Extend the shared rule sets or edit the existing rules in place.** Rejected: it breaks
  FR-020 (no regression for other sources), and LIBRA needs different regexes and redaction.
- **Reject in the aggregate by matching problem codes.** Rejected: codes are shared across
  scopes with different outcomes (for example `INVALID_GUARDIAN_POST_CODE`).
- **A new rejection event or a structured reasons field on the public event.** Rejected: it is a
  contract change for `stagingdlrm` and `progression`. Description text meets the current need.
- **Add a field to the generated `MigratedDefendantWithProblem`.** Rejected: it means a schema
  edit for an internal value. A hand-written class is enough.
