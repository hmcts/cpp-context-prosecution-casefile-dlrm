# 01 — Requirements and responsibility analysis

- **Story:** [DD-43494](https://tools.hmcts.net/jira/browse/DD-43494) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Base:** `team/libra1`. STAGINGDLRM read at `team/libra1` @ `0b678de`.

## Today

| Layer | Alias handling |
|---|---|
| STAGINGDLRM LIBRA gate (`libra.case-submission.json`) | `individualAliases` array (min 1 item); each alias is a closed object of `title` (max 35), `firstName`, `givenName2`, `givenName3`, `lastName` — **no `maxLength` on the names, nothing required**. |
| STAGINGDLRM convertor | Pass-through. |
| PCFDLRM command schema (`individual-alias.json`) | Same shape; names have no `maxLength`, nothing required. |
| PCFDLRM rules | None, for XHIBIT or LIBRA. |
| PCFDLRM event processor | `givenName2` and `givenName3` are trimmed and joined into `middleName`. |

So an alias name of any length, or with consecutive spaces, reaches `progression` unchanged for both
source systems. There is no XHIBIT alias rule to reuse.

## Per-field responsibility

| Field | Ticket | STAGINGDLRM | PCFDLRM | After this story | Discrepancy |
|---|---|---|---|---|---|
| forename | O, accept; ≤35 (truncate); no double spaces; "mandatory if person" in comment | no length, optional | none | unchanged | A1, A2, A3 |
| forename2 | O, accept; ≤35 (truncate); no double spaces | no length | none | unchanged | A1, A2 |
| forename3 | O, accept; ≤35 (truncate); no double spaces | no length | none | unchanged | A1, A2 |
| surname | O, accept; ≤35 (truncate); no double spaces; "mandatory if person, otherwise null" in comment | no length, optional | none | unchanged | A1, A2, A3 |

## Discrepancies for the BA

| # | Discrepancy | Classification |
|---|---|---|
| A1 | Alias names over 35 characters should be cut to the first 35. Nothing truncates or limits alias names in STAGINGDLRM or PCFDLRM, for XHIBIT or LIBRA. Truncation is a data transformation, not a reference-data check, so it belongs in the STAGINGDLRM mapping (or a STAGINGDLRM schema `maxLength` if rejection is acceptable). | Needs clarification; STAGINGDLRM change; align XHIBIT and LIBRA |
| A2 | "No consecutive spaces": not implemented anywhere. The outcome is not stated (reject, null, or collapse the spaces). | Needs clarification |
| A3 | The comments say forename and surname are mandatory for a person, but the columns say optional / accept. | Needs clarification |

## Outcome

No PCFDLRM code change for this story: none of the alias rules is reference-data or business
validation, and there is no XHIBIT alias rule to reuse. All three items go to the BA; if confirmed,
A1 and A2 are a STAGINGDLRM story (one repo per story).
