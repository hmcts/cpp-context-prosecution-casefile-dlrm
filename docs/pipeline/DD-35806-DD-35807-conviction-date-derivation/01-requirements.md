# 01 — Requirements

- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))

## Framing

Production already derives `convictionDate` from a guilty plea date for both `GUILTY` and
`INDICATED_GUILTY`, regardless of source system (`00-input-brief.md`). The gap is automation
coverage: nothing at IT level proved this for LIBRA.

## Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | Extend `libra-journey` (existing `GUILTY`-plea fixture pair) to assert `convictionDate` on the resulting `progression.initiate-court-proceedings` offence. |
| FR-2 | Add a LIBRA `INDICATED_GUILTY` fixture pair (reference-data id `9a1e0d34-5b2c-4f8e-bd6c-7a9f1e8d3c2b`), asserting `convictionDate` and that the offence carries `indicatedPlea` (not `plea`), per the existing DD-34568 diversion rule. |
| FR-3 | Both cases run through the existing `ReceiveMigratedCaseFileIT.receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` parametrization — no new test class. |
| FR-4 | No production code change unless the IT run surfaces an actual defect (test-automation-only scope). |

## Acceptance criteria

- **AC-1 — Indicated guilty:** plea date → offence conviction date. *(No automation — FR-2.)*
- **AC-2 — Guilty:** plea date → offence conviction date. *(Exercised by `libra-journey` but
  unverified — FR-1.)*

## Open questions

1. **Scope confirmation.** Automation-only, not a known defect — nothing in code/tests suggests
   otherwise.
2. **Plea-value breadth.** Other guilty-flag `Yes` types (e.g. `GUILTY_SINGLE_JUSTICE_PROCEDURE`)
   exist in reference data (DD-34568) — out of scope unless the ticket says otherwise.
3. **Verdict-derived conviction dates.** Separate path (`deriveConvictionDateFromVerdict`),
   already covered by `shouldSetConvictionDateFromVerdictWhenGuiltyVerdict` — out of scope.
4. **XHIBIT parity.** Not requested (LIBRA-only AC) and already unit-covered for both plea codes —
   no XHIBIT IT work proposed.
