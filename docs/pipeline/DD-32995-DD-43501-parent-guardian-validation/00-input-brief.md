# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases
- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) — Business validation for Parent Guardian level items
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-32995-libra-defendant-validation` (one branch and one PR for DD-43493, DD-43494 and DD-43501, which are all defendant-level validation).

## Epic framing

LIBRA cases migrated through DLRM must pass business validation before `pcfdlrm` hands them to
`progression`. Each field in the migration schema (*DLRM – CP Migration Data Schema V0.13*) has a
rule; a breach either rejects the case or nulls the value and accepts the case.

## Story ask (verbatim AC)

> **GIVEN** a case has been received onto the Common Platform via DLRM LIBRA migration,
> **WHEN** the validation routines are run for the 'Parent Guardian' elements of the payload,
> **AND** the business rules are not met for any of the Parent Guardian elements,
> **THEN** the system must reject or allow the case to be migrated to CP depending on the table.
>
> If any of the fields have invalid entries, they should be nulled and therefore empty and should
> follow the "required behaviour for missing fields".

Individual guardian (SJP N/A; Summons/Charge/Requisition):

| Field | Rule | Missing |
|---|---|---|
| surname | A35; provide if a guardian person is given | Accept |
| work / home / mobile telephone | `^[0-9+\ \-]{10,}$` | Accept |
| primary / secondary email | 1–127 of `0-9A-Za-z'.-_` @ 1–127 | Accept |
| dateOfBirth | not in the future | Accept |
| gender | M; default Not Known; 0/1/2/9; CP Gender ref data | Accept |
| observedEthnicity | CP Observed Ethnicity ref data | Accept |
| selfDefinedEthnicity | "16+1"; CP Ethnicity ref data | Accept |
| address1 | M; no-fixed-abode code if unknown | Reject |
| address2–5 | A35 | Accept |
| postcode | must match the regex where provided | Reject |

Organisation guardian: `organisationName` (M; reject if youth defendant), `companyTelephoneNumber`,
`organisation.address1` (M, reject), address2–5, postcode (accept).

## Delivery direction (from code review and the requester)

1. Reuse the existing rule constructs; no new validation framework.
2. Reuse the XHIBIT validation rules for LIBRA where the requirement is equivalent.
3. Highlight every LIBRA/XHIBIT requirement discrepancy for the BA rather than building separate
   LIBRA behaviour.
4. STAGINGDLRM owns schema-level validation (mandatory, presence, types, structure, lengths,
   patterns); PCFDLRM validates against reference data and business rules. Do not duplicate checks.

## Decisions already agreed with the requester

- LIBRA will not send organisation guardian details; the organisation rows do not apply.
- Phone and email formats are STAGINGDLRM's, not the ticket's.
- Guardian surname stays required (STAGINGDLRM).
- Guardian observed ethnicity is sent as an integer in `personalInformation.observedEthnicity`.
- A `pcfdlrm` JSON-schema failure (HTTP 400) is left as it is: no outcome is recorded in STAGINGDLRM.
