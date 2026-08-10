# Stories — LIBRA enabler: PCFDLRM test hardening

> Stage 3 artefact. Source: [`02-design.md`](./02-design.md).
>
> **The stories themselves are not written yet.** This file currently holds only the implementation
> sketches handed forward from stage 2, so the design stays at design altitude. Stage 3 adds the
> stories above this section and leaves the sketches in place.
>
> Conventions that stage 3 must follow — fixture layout, the dump-then-prove-it-bites discipline, the
> round-trip fidelity test — are in
> [`02-design.md` § Conventions for stage 3](./02-design.md#conventions-for-stage-3). They are not
> repeated here; a second copy would drift.

## Stories

*To be written by stage 3, from the four tasks in
[`02-design.md` § Tasks](./02-design.md#tasks).*

---

## Implementation sketches

Illustrative, not prescriptive. They fix the *shape* the design argues for — an ordered event list as
the expected value, and a capture point that sees the converted payload. Names, packaging and
matcher choice are the implementer's.

### Aggregate scenario harness (T2)

The row and the one shared assertion block. Every aggregate scenario goes through this, which is what
makes R1's no-getters rule structural rather than a review checklist.

```java
record ExpectedEvent(Class<?> type, String fixture) {}

record AggregateScenario(String name,
                         String sourceSystem,
                         CaseFileInput input,
                         List<ExpectedEvent> expected) {}
```

```java
final List<Object> actual = aggregate.receiveMigratedCaseFile(argsFor(scenario)).toList();

// Count first: an extra or missing event should fail here, naming the problem,
// rather than surfacing as a confusing payload diff at position 0.
assertThat(actual, hasSize(scenario.expected().size()));

for (int i = 0; i < actual.size(); i++) {
    final ExpectedEvent e = scenario.expected().get(i);
    assertThat("event " + i, actual.get(i), instanceOf(e.type()));
    assertWholePayload(actual.get(i), FixtureLoader.load(e.fixture()), EXCLUSIONS);
}
```

`assertWholePayload` serialises via the framework `ObjectToJsonObjectConverter` and compares with
`WholePayloadMatcher` (JSONassert STRICT, anchored enumerated exclusions).

Two notes for whoever builds this:

- The same block serves `materialAddedPostProcessing` and `acceptMigratedCase` — all three entry
  points return `Stream<Object>`. Broadening coverage of those two is a **follow-up ticket**, not this
  story, but the harness should not assume `receiveMigratedCaseFile`.
- Nine of the eleven `MigratedCaseFileProcessed` emissions are fail-fast early returns producing a
  single-event stream distinguished only by `description`. Those rows are near-trivial once this
  block exists, and are the right place to settle the fixture convention before the main path.
- **Four of those rows do not exist today** and must be written: `Invalid Prosecuting Authority`
  (gate `:221`), and `INVALID_OFFENCE_CODE` / `MISSING_OR_INVALID_PLEA_DATE` /
  `MISSING_OR_INVALID_VERDICT_DATE` (gate `:433`). R3a is not satisfiable by converting the existing
  39 — see [`02-design.md` § Coverage](./02-design.md#coverage-what-r3-forces-what-defers).

### Converter seam harness (T3)

Capture where the converted payload is, not where the envelope is finally sent.

```java
@Captor
private ArgumentCaptor<Envelope<JsonValue>> converted;

// ...

verify(envelopeHelper).withMetadataInPayloadForEnvelope(converted.capture());

assertThat(converted.getValue().metadata().name(), is("progression.initiate-court-proceedings"));
assertWholePayload(converted.getValue().payload(), FixtureLoader.load(fixture), EXCLUSIONS);
```

The real converter tree has to be assembled first — all six converters use private `@Inject` fields
and `@InjectMocks` populates only one level of three. A test-side factory using
`FieldUtils.writeField(target, name, value, true)` (commons-lang3, already on the classpath) builds
the tree and injects a stubbed `ReferenceDataQueryService` into the two converters that need one.

The input `MigratedCaseFileReceived` is deserialised from a fixture rather than hand-built, which is
why the round-trip fidelity test matters here specifically: a field absent from the generated POJO is
dropped silently on the way in, and the STRICT comparison on the way out still passes.
