# 01 — Requirements

- **Story:** [DD-43296](https://tools.hmcts.net/jira/browse/DD-43296) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))

## Framing

Production already derives `convictionDate` from a guilty verdict when the offence's plea isn't
guilty (`00-input-brief.md`). The likely gap is automation coverage: nothing at IT level proves
this for any source system, LIBRA included, and the sole verdict-type reference-data stub can't
even trigger the "Guilty" category check.

## Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | Add a LIBRA fixture pair: a not-guilty plea + a guilty-category verdict (with a verdict date) on the same offence, on a future hearing, asserting the offence's `convictionDate` equals the verdict date. |
| FR-2 | Add a "Guilty"-category verdict-type reference-data stub row (`referencedata.query.verdict-types.json`) with a jurisdiction visible to LIBRA — confirm the correct value empirically against `VerdictDataRefDataEnricher`'s filter rather than assuming. |
| FR-3 | Run through the existing `ReceiveMigratedCaseFileIT.receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings` parametrization — no new IT class. |
| FR-4 | No production code change unless the IT run surfaces an actual defect (test-automation-only scope, same working assumption as DD-35807 until disproved). |

## Acceptance criteria

- **AC-1 — Guilty verdict:** LIBRA case, future hearing, not-guilty plea + guilty verdict on an
  offence → offence `convictionDate` equals the verdict date. *(No automation today — FR-1/FR-2.)*

## Open questions

1. **Scope confirmation.** Working assumption is automation-only, mirroring DD-35807 — but unlike
   that story, this path has *zero* IT coverage anywhere (not even XHIBIT), so a real defect
   surfacing on first run is more plausible here than it was for the plea path.
2. **Verdict-type stub jurisdiction.** `VerdictDataRefDataEnricher` needs the new stub row to carry
   `MAGISTRATES` or `EITHER` to pass the filter for a LIBRA case — to be confirmed by an actual
   Docker IT run, not assumed (per DD-35807's own experience with the equivalent plea-side stub).
3. **"Any offence on the case" breadth.** AC says "any of the offence" — read as: the derivation
   applies per-offence, not case-wide: multiple offences on a case, only some with a guilty
   verdict. Existing `libra-journey`/`libra-indicated-plea` fixtures already exercise multi-offence
   cases; this story adds one more offence-level scenario rather than a new multi-offence case
   shape, unless the ticket says otherwise.
4. **Verdict category breadth.** Reference data may define other guilty-flavoured verdict
   categories/codes beyond a plain "Guilty" category (by analogy with DD-34568's plea-value
   breadth) — out of scope unless the ticket says otherwise.
5. **Plea-derived path.** Not touched — already covered by DD-35807.
