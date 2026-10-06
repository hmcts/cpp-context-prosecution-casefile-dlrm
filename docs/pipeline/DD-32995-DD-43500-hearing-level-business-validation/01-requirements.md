# 01 — Requirements

- **Story:** DD-43500 (epic DD-32995) — see `00-input-brief.md`.
- **Scope:** LIBRA hearing-level business validation. XHIBIT behaviour unchanged.
- **Format column: out of scope.** The shared schema already enforces it (except `courtRoomId`) and
  can't be changed per source without affecting XHIBIT.

## pcfdlrm

| ID | Requirement | XHIBIT precedent |
|----|-------------|------------------|
| FR-1 | Invalid `courtHearingLocation`, `courtRoomId`, `hearingType`, or a past / pre-offence `dateOfHearing` → accept, emit hearing `migrated-case-validated-with-warnings` — as XHIBIT. | `generateXhibitHearingWarnings` |
| FR-2 | Hearing with unknown OU / hearing type or past date is not listed; unknown court room → listed without room. | Already source-agnostic — no change |
| FR-3 | Missing `timeOfHearing` → rejected in stagingdlrm (see note); ticket's 10:00 default not reached. | No change |
| FR-4 | Missing / zero `durationMinutes` → hearing-type default duration. | Already source-agnostic — no change |
| FR-5 | Missing `dateOfHearing` → rejected in stagingdlrm (see note); ticket's "accept, no hearing" not reached. | No change |
| FR-6 | Hearing with no matching defendants → reject, for all sources. **Not in the ticket table** — user decision 2026-10-05; flag to BA. | `NO_MATCHING_DEFENDANTS_FOR_HEARING` reject (~L542) |
| FR-6a | XHIBIT validation paths unchanged. | — |
| FR-7 | Unit tests only for the new LIBRA outcomes; no negative LIBRA ITs; existing ITs stay green. | — |

## stagingdlrm — no change

LIBRA `RequiredFieldRule`s (DD-43081) already reject a hearing missing `courtRoomId`,
`dateOfHearing` or `timeOfHearing`. Kept as is (user decision 2026-10-05). Diverges from the ticket's
"accept" rows; flag to BA.

## Acceptance criteria

- **AC-1:** LIBRA hearing problems handled as XHIBIT: warnings, case accepted; reject only on no
  matching defendants (pcfdlrm) or a missing court room / date / time (stagingdlrm) (FR-1 – FR-6).

## Open questions

1. ~~Epic key~~ — **resolved:** DD-32995.
2. ~~Missing `dateOfHearing`~~ — **moot:** stagingdlrm rejects it for LIBRA (OQ-4).
3. ~~`courtRoomId` A36 vs integer~~ — **resolved:** keep integer; ticket differs from schema. Flag to BA.
4. ~~stagingdlrm required rules vs ticket~~ — **resolved:** keep them; no stagingdlrm change.
5. ~~No-matching-defendants reject~~ — **resolved:** applies to all sources (FR-6). Beyond the ticket; flag to BA.
6. **`durationMinutes` conversion to days** — done downstream (listing), not here. Out of scope.
7. **`hearingType` data mapping** — ticket says to double-check the mapping with the named BA contacts. Not a pcfdlrm change.
