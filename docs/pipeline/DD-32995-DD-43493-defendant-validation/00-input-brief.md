# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995) — Business validation for Libra cases
- **Story:** [DD-43493](https://tools.hmcts.net/jira/browse/DD-43493) — Business validation for Defendant level items
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-32995-libra-defendant-validation` (one branch and one PR for DD-43493, DD-43494 and DD-43501, which are all defendant-level validation)

## Epic framing

LIBRA cases migrated through DLRM must pass business validation before `pcfdlrm` hands them to
`progression`. Each field in the migration schema (*DLRM – CP Migration Data Schema V0.13*) has a
rule; a breach either rejects the case or nulls the value and accepts the case.

## Story ask (verbatim AC)

> **GIVEN** a case has been received onto the Common Platform via DLRM LIBRA migration,
> **WHEN** the validation routines are run for the 'Defendant' elements of the payload,
> **AND** the business rules are not met for any of the Defendant elements,
> **THEN** the system must reject or allow the case to be migrated to CP depending on the table.
>
> If any of the fields have invalid entries, they should be nulled and therefore empty and should
> follow the "required behaviour for missing fields".

Ticket comment (29/Sep/26): two rows are marked red pending a data-mapping exercise before their
rules can apply. The red marking does not survive in the export, so the rows are not identified here.

## Delivery direction (from code review and the requester)

1. Reuse the existing rule constructs (`ValidationRule`, `CcProsecutionValidationRuleProvider`,
   `ProsecutionCaseFileHelper`). No new validation framework.
2. Reuse the XHIBIT validation rules for LIBRA where the requirement is equivalent.
3. Highlight every LIBRA/XHIBIT requirement discrepancy for the BA rather than building separate
   LIBRA behaviour.
4. STAGINGDLRM owns schema-level validation (mandatory, presence, types, structure, lengths,
   patterns). PCFDLRM validates against reference data and business rules. Do not duplicate a check
   STAGINGDLRM already makes.

## Decisions already agreed with the requester (apply to all three stories)

- Phone and email formats are STAGINGDLRM's (`^[\+]?[0-9()\-\.\s]+$` max 35; RFC-style email), not
  the ticket's `^[0-9+\ \-]{10,}$` and simplified email rule.
- A `pcfdlrm` JSON-schema failure (HTTP 400) is left as it is: no outcome is recorded in STAGINGDLRM.
