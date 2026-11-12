# 01 — Requirements

- **Story:** DD-43946 — see `00-input-brief.md`.
- **Scope (this repo):** pcfdlrm. The Function App and stagingdlrm service changes are the stagingdlrm
  story; the cross-repo contract is [ADR-DD-43946](../adrs/ADR-DD-43946-libra-material-upload-date.md).

## Answered questions

Confirmed by the developer 2026-11-11.

| # | Question | Answer |
|---|----------|--------|
| Q1 | Is the CP "Uploaded" column `progression` court document `materials[].uploadDateTime`? | Yes. `progression` keeps a supplied `uploadDateTime` and orders case documents by it. |
| Q2 | Does the `material` context need a change? | No. The date only travels through `material` in `ccMetadata`. |
| Q3 | Where does LIBRA send the date, and in what format? | New optional `uploadedDateTime` on each `manifest.json` `files[]` entry, ISO-8601 UTC with milliseconds, e.g. `2026-11-11T14:04:07.107Z`. |
| Q4 | Mandatory for LIBRA, or optional? | Optional. Missing → today's behaviour (`progression` sets "Uploaded" to the time the document is added). |
| Q5 | Which CP field gets the LIBRA date? | `materials[].uploadDateTime`. `receivedDateTime` stays the pcfdlrm receive time. |
| Q6 | XHIBIT? | Unchanged. It does not send the field. |
| Q7 | Jira for the stagingdlrm half? | A sub-task under DD-43946. |
| Q8 | Future upload dates? | Not allowed. The stagingdlrm Function App rejects the whole submission; pcfdlrm does no extra check. |

## Requirements

| ID | Requirement |
|----|-------------|
| FR-1 | When a migrated material arrives with `receivedDateTime` set (the LIBRA upload date, per the ADR), `material-added` carries it as a new optional `uploadDateTime`. |
| FR-2 | `material-added.receivedDateTime` is unchanged: still the pcfdlrm receive time. |
| FR-3 | `MaterialEventProcessor` passes `uploadDateTime`, when present, through the material upload's `ccMetadata` and into the court document as `materials[0].uploadDateTime`. |
| FR-4 | No date supplied → the court document has no `uploadDateTime`, as today; `progression` sets it. |
| FR-5 | XHIBIT is unchanged. |
| FR-6 | Unit tests for FR-1 – FR-5. One IT: a LIBRA material with a supplied date reaches `progression` with that `uploadDateTime`. Existing ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA case whose material was uploaded to LIBRA on a known date, WHEN the case is
  migrated and accepted, THEN the court document sent to `progression` has that date as
  `materials[].uploadDateTime`, so CP shows it as "Uploaded".
- **AC-2:** GIVEN a migrated material with no upload date, WHEN the case is accepted, THEN the court
  document has no `uploadDateTime`, as today.
- **AC-3:** GIVEN an XHIBIT case, WHEN it is migrated, THEN its materials are handled as today.
