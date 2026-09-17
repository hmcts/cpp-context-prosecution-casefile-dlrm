# 01 — Requirements

- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))
- **Status:** Draft — open questions below must be closed (or explicitly waived) at the human
  gate before Stage 2 (Design).

## Framing

Production logic already derives the conviction date from a guilty plea date for both `GUILTY`
and `INDICATED_GUILTY` reference-data values, regardless of source system (see `00-input-brief.md`
investigation notes). The gap is **automation coverage**: nothing at IT level proves this holds
for LIBRA migrations, and the AC's `GIVEN ... migrated LIBRA case` framing plus the epic's
"Automation Journey" naming both point at closing that gap rather than changing behaviour.

## Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | Extend the LIBRA "journey" integration-test fixture pair so the existing `GUILTY`-plea case (`pcfdlrm.command.receive-migrated-case-file-libra-journey.json`) asserts `convictionDate` equals the migrated plea date on the resulting `progression.initiate-court-proceedings` offence. |
| FR-2 | Add a LIBRA IT fixture pair (command + expected `initiate-court-proceedings` JSON) for an `INDICATED_GUILTY` plea (reference-data id `9a1e0d34-5b2c-4f8e-bd6c-7a9f1e8d3c2b`), asserting `convictionDate` equals the migrated plea date and that the offence carries `indicatedPlea` (not `plea`), per the existing DD-34568 diversion rule. |
| FR-3 | Both new/extended cases run through `ReceiveMigratedCaseFileIT` (the existing `@ParameterizedTest receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` or an equivalent), not a new bespoke test class, consistent with the existing "journey" pattern. |
| FR-4 | No production code changes unless the IT run surfaces an actual defect — this is scoped as test-automation-only per `feedback: stay-within-tests-only-scope`. |

## Acceptance criteria (GDS "Given/When/Then", as supplied)

- **AC-1 — Indicated guilty:** given a migrated LIBRA case with an indicated-guilty plea and date
  on an offence, when the future hearing is created in CP, then the backend sets that plea date as
  the offence's conviction date. *(Automation currently absent — FR-2.)*
- **AC-2 — Guilty:** given a migrated LIBRA case with a guilty plea and date on an offence, when
  the future hearing is created in CP, then the backend sets that plea date as the offence's
  conviction date. *(Behaviour exists and is exercised by `libra-journey`, but unverified —
  FR-1.)*

## Open questions

1. **Scope confirmation.** Is this genuinely automation-only, or does the live Jira ticket
   describe a known defect in LIBRA conviction-date derivation? Nothing found in code or existing
   tests suggests a defect — flagging per "never invent requirements" rather than assuming.
2. **Plea-value breadth.** The ACs name only "indicated guilty" and "guilty". Other guilty-flag
   `Yes` plea types (e.g. `GUILTY_SINGLE_JUSTICE_PROCEDURE`) exist in reference data per the
   DD-34568 requirements doc — out of scope here unless the story says otherwise.
3. **Verdict-derived conviction dates.** `deriveConvictionDateFromVerdict` (guilty verdict, no
   plea) is a separate path already covered by `shouldSetConvictionDateFromVerdictWhenGuiltyVerdict`
   — out of scope, the ACs only describe plea-driven derivation.
4. **XHIBIT parity.** Not requested by the ACs (LIBRA-only) and XHIBIT already has unit coverage
   for both plea codes — no XHIBIT IT fixture work proposed.
