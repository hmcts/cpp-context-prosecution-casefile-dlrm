# 02 — Design

- **Story:** [DD-43494](https://tools.hmcts.net/jira/browse/DD-43494) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))

No PCFDLRM change. The alias rules (35-character truncation, no consecutive spaces, conditional
mandatory names) are not reference-data or business validation, there is no XHIBIT alias rule to
reuse, and their outcomes need BA confirmation (A1–A3 in `01-requirements.md`).

If the BA confirms them, the natural home is STAGINGDLRM, as a separate story in
`cpp-context-stagingdlrm` (one repo per story):

- truncation of alias names to 35 characters in `MigratedCaseConvertor.buildIndividualAliases`;
- the consecutive-spaces rule as a schema pattern or a mapping step, depending on the agreed outcome;
- applied to XHIBIT and LIBRA alike, since both pass aliases through unchanged today.
