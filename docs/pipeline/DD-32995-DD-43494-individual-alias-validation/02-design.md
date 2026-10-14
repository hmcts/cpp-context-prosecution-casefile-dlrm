# 02 — Design

- **Story:** [DD-43494](https://tools.hmcts.net/jira/browse/DD-43494) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))

No PCFDLRM change. The alias rules are not reference-data or business validation, and there is no
XHIBIT alias rule to reuse.

Decisions:

- **A1:** open. Truncation in STAGINGDLRM (`MigratedCaseConvertor.buildIndividualAliases`) was
  withdrawn after TL review (2026-10-13); alias names pass through unchanged.
- **A2:** ignored (BA, 2026-10-12) — no consecutive-spaces rule.
- **A3:** alias names stay optional everywhere (BA, 2026-10-12) — nothing required in the alias schema.
