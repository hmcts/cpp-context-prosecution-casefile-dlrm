# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases
- **Story:** [DD-43494](https://tools.hmcts.net/jira/browse/DD-43494) — Business validation for Individual Alias level items
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-32995-libra-defendant-validation` (one branch and one PR for DD-43493, DD-43494 and DD-43501, which are all defendant-level validation)

## Epic framing

LIBRA cases migrated through DLRM must pass business validation before `pcfdlrm` hands them to
`progression`. Each field in the migration schema (*DLRM – CP Migration Data Schema V0.13*) has a
rule; a breach either rejects the case or nulls the value and accepts the case.

## Story ask (verbatim AC)

> **GIVEN** a case has been received onto the Common Platform via DLRM LIBRA migration,
> **WHEN** the validation routines are run for the 'Individual Alias' elements of the payload,
> **AND** the business rules are not met for any of the Individual Alias elements,
> **THEN** the system must reject or allow the case to be migrated to CP depending on the table.
>
> If any of the fields have invalid entries, they should be nulled and therefore empty and should
> follow the "required behaviour for missing fields".

| Field | Format | Rule | SJP / Sum / Chg / Req | Missing |
|---|---|---|---|---|
| Alias forename | A35 | CJS 3.50. Comment: mandatory if the defendant is a person; alphanumeric, no consecutive spaces; if over 35 characters use the first 35 | O / O / O / O | Accept |
| Alias forename2 | A35 | CJS 3.50; no consecutive spaces; first 35 characters | O / O / O / O | Accept |
| Alias forename3 | A35 | CJS 3.50; no consecutive spaces; first 35 characters | O / O / O / O | Accept |
| Alias surname | A35 | CJS 3.39. Comment: mandatory if the defendant is a person, otherwise null; no consecutive spaces; first 35 characters | O / O / O / O | Accept |

## Delivery direction (from code review and the requester)

1. Reuse the existing rule constructs; no new validation framework.
2. Reuse the XHIBIT validation rules for LIBRA where the requirement is equivalent.
3. Highlight every LIBRA/XHIBIT requirement discrepancy for the BA rather than building separate
   LIBRA behaviour.
4. STAGINGDLRM owns schema-level validation (mandatory, presence, types, structure, lengths,
   patterns); PCFDLRM validates against reference data and business rules. Do not duplicate checks.
