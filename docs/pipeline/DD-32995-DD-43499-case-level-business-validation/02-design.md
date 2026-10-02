# 02 — Design

- **Story:** [DD-43499](https://tools.hmcts.net/jira/browse/DD-43499) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)) — pcfdlrm half
- **Status:** Stage 2 approved 2026-10-01.
- **Inputs:** `01-requirements.md` (approved 2026-10-01). No production code changed at this stage.

## Summary

No new pattern, rule, event or schema. LIBRA already runs the case-level rules; the aggregate
discards the result behind two `isXhibit(...)` gates. Lift only the in-scope parts out of them —
the prosecuting-authority reject and the case-marker warning — so LIBRA acts on those exactly as
XHIBIT does (FR-1). XHIBIT-only rejects stay gated.

## Current flow (`MigratedCaseFileAggregate`, receive path)

1. Enrich case refdata (`ProsecutorRefDataEnricher` etc.) — ~L159.
2. Material rejection rules — ~L174-207.
3. Case rules via `getCaseValidationRules(initiationCode, sourceSystemName)` — ~L210.
4. **Gate A** `if (caseProblems && isXhibit)` — ~L214: reject on `COURT_LOCATION_OUCODE_INVALID`,
   `RECEIPT_TYPE_IS_INVALID`, `PROSECUTOR_OUCODE_NOT_RECOGNISED` → `migrated-case-file-processed`
   (`processingIsSuccessful=false`), return.
5. Hearing, defendant, offence validation — ~L253-273.
6. **Gate B** `if (caseProblems && isXhibit)` — ~L275: `CASE_MARKER_IS_INVALID` →
   `migrated-case-validated-with-warnings`.
7. Received / creation-pending — ~L334-367.

## Change

| # | File | Change |
|---|------|--------|
| C-1 | `MigratedCaseFileAggregate.java` ~L214 | Gate A: keep `isXhibit` around the court / receipt-type rejects (XHIBIT-only); move the `hasInvalidProsecutingAuthority` reject out of the gate so it applies to all sources. |
| C-2 | `MigratedCaseFileAggregate.java` ~L275 | Gate B: drop `&& isXhibit(receiveMigratedCaseFile)`. |

After C-1/C-2 the only new LIBRA effects are:

| Problem (LIBRA) | Outcome | FR |
|---|---|---|
| `PROSECUTOR_OUCODE_NOT_RECOGNISED` | Reject, "Invalid Prosecuting Authority" | FR-2 |
| `CASE_MARKER_IS_INVALID` | Accept + `migrated-case-validated-with-warnings` | FR-3 |
| `CASE_INITIATION_CODE_INVALID`, `POLICE_FORCE_CODE_INVALID`, SJP/AOCP prosecutor codes | Raised, not acted on (as XHIBIT) | FR-4 |

XHIBIT is unaffected (FR-6): same checks, same order (court, receipt type, prosecuting authority).
LIBRA test inputs carry no receipt type (XHIBIT-only field).

**Precedent:** DD-43130 removed an `isXhibit` guard on the no-materials path for the same reason
(comment at ~L360).

## Options considered

| Option | Why not |
|---|---|
| **A. Lift only the in-scope checks out of the gates (chosen)** | — Smallest change; XHIBIT-only checks untouched. |
| A2. Drop both gates entirely | Also exposes XHIBIT-only court / receipt-type rejects to LIBRA — out of scope. |
| B. Keep gates, add a LIBRA-only branch for prosecutor + markers | Duplicates the reject/warning blocks; two copies to drift. |
| C. Per-source reject-code sets | Over-engineered for two codes; nothing else needs it. |

## Downstream

- Reject → `MigratedCaseFileProcessedProcessor` republishes as
  `public.pcfdlrm.migrated-case-file-processed` (unchanged; already used for LIBRA hearing/offence
  rejects). No new subscription or schema.
- Warning event has no processor subscription (event store only) — same as XHIBIT today.
- **Invalid markers never reach progression**: both received paths (~L362,
  ~L562) go through `MigratedCaseToProsecutionCaseConverter`, which keeps only refdata-matched
  markers (~L180-184) — same as XHIBIT. Unchanged, as XHIBIT: all-invalid →
  `caseMarkers: []`; rule matches case-insensitively, converter case-sensitively.
- Side benefit: an unknown LIBRA prosecutor no longer reaches the processor converter, which
  dereferences `prosecutorsReferenceData` without a null check (`MigratedCaseToProsecutionCaseConverter`
  ~L142). No converter change (out of scope).

## Edge cases

- **Absent/unknown source system** falls to the LIBRA rule set and will now be acted on. Not
  reachable: stagingdlrm requires `migrationSourceSystemName` ∈ {LIBRA, XHIBIT}.
- **LIBRA SJP (`J`)** uses `SJP_CASE_RULE_SET_LIBRA`, which also includes
  `ProsecutorReferenceDataValidationRule` → unknown prosecutor rejects SJP too (as XHIBIT).
- **Existing LIBRA fixtures** whose case markers aren't in the refdata stub would now also emit a
  warning event; none of the current unit fixtures do.

## Tests

| Level | Change |
|---|---|
| Unit — `AggregateScenarios` | Add LIBRA rows for "Invalid Prosecuting Authority" and the case-marker warning, built from a LIBRA-only case builder (ticket case-level fields only). |

**No LIBRA ITs** — negative (invalid-input) LIBRA IT tests are not to be written (agreed scope).
Both new outcomes are covered at unit level. No new endpoint or `@Handles`, so the per-endpoint IT
rule doesn't apply. Existing ITs must stay green via `./runIntegrationTests.sh`.

## ADR

Not needed — follows existing XHIBIT behaviour and the DD-43130 precedent; no new decision.

## Open questions

None.
