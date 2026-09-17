# 00 — Input Brief

- **Epic:** [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806)
- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) — "Derive the conviction date
  (Indicated guilty and guilty pleas) Automation Journey"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-35807`

> ACs below as supplied verbatim — no Jira fetch (Atlassian MCP not authenticated this session).
> Reconcile against the live ticket before sign-off.

## Acceptance criteria (as given)

**AC-1 — Indicated guilty plea:** GIVEN a migrated LIBRA case with a future hearing, WHEN an
offence carries an indicated guilty plea + date, THEN the backend sets that date as the offence's
conviction date.

**AC-2 — Guilty plea:** same, for a guilty plea.

## Investigation

`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.getConvictionDate()`
(`pcfdlrm-event/pcfdlrm-event-processor/.../convertor/...Converter.java:551`) already derives
`convictionDate` from `pleaDate` whenever reference-data `pleaTypeGuiltyFlag == "Yes"` — true for
both `GUILTY` and `INDICATED_GUILTY`, source-system-agnostic. Unit-tested for XHIBIT only
(`...ConverterTest.shouldSetConvictionDateAsPleaDateWhenPleaIsGuilty`, `@CsvSource({"G","IG"})`).

No LIBRA IT coverage existed:
- `libra-journey.json` (guilty plea) didn't assert `convictionDate` — AC-2 unverified.
- No LIBRA `INDICATED_GUILTY` fixture at all — AC-1 uncovered.

Scoped as an automation-coverage gap, not a production-code change (confirmed — see
`02-design.md`).
