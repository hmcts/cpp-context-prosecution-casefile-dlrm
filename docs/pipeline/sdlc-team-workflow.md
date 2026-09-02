# SDLC Plugin Workflow — Team of 4

How a team of **1 tech lead, 2 developers, 1 QA** runs the `hmcts-sdlc-orchestrator`
8-stage pipeline on this repo.

The plugin's stages and gates are defined in
`~/.claude/plugins/cache/agentic-plugins-marketplace/hmcts-sdlc-orchestrator/1.0.0/CLAUDE.md`.
This document is the team overlay: the unit of work, who owns what, where work runs
in parallel, and the repo-specific caveats.

Its sibling in `cpp-context-stagingdlrm` (`docs/pipeline/sdlc-team-workflow.md`) is
deliberately the same document with this repo's facts swapped in. **The team model,
the ownership table and the gate rules are identical in both repos** — only the
*Repo-specific caveats* and *Refinement* sections differ. If you change the shared
part here, change it there too.

**The two DLRM repos are run independently**, by separate developers, and each repo's
pipeline is **self-contained**: every artefact a stage needs to run against a story in
this repo is in this repo. Nothing here requires opening `cpp-context-stagingdlrm` —
where the two share a decision, this repo holds its own copy (see *Shared design
decisions* below). Read this document, the root `CLAUDE.md`, and the story's own
directory; that is the whole input set.

---

## The pipeline unit is a story

**One story = one complete pipeline run, all 8 stages.** The epic never enters the
pipeline — it is a Jira container that refinement splits into stories.

```
Epic DD-43191  (Jira only — split at refinement, may hold tens of stories)
  │
  ├─ DD-43194-j25-parity              ──► 1 2 3 4 5 6 7 8    ┐
  ├─ DD-43194-j25-upgrade             ──► 1 2 3 4 5 6 7 8    ├ this sprint
  ├─ DD-43xxx-…                       ──► 1 2 3 4 5 6 7 8    ┘
  └─ DD-43xxx-…                            (backlog)
```

`01-requirements.md` and `02-design.md` are the requirements and design for **that
story**, not for the epic. This scales to epics of any size — a twenty-story epic is
twenty independent pipeline runs, of which a sprint pulls only as many as the team has
owners for.

Two consequences worth stating up front:

- **A story belongs to exactly one repo.** Artefacts live in a repo, and stage 7 (CI)
  is per-repo. If a change needs work in two repos, that is two stories — `DD-43099`
  here and `DD-43078` in `cpp-context-stagingdlrm` are the two halves of one epic-level
  test-hardening effort, and `DD-43191` runs as **four** pipelines: parity and upgrade,
  in each of the two DLRM repos. The two repos' pipelines are run by different
  developers and do not wait on each other — DD-43194's two stages here are ordered
  only against **each other**, never against DD-43192 in `stagingdlrm`.
- **Stage 3 is not "split the epic"** — that already happened at refinement. At story
  level, stage 3 is writing the story properly: GDS format, testable acceptance
  criteria, ready for QA to build test specs against. If stage 3 reveals the story is
  too big, that is a signal to split it and start a second pipeline, not to carry on.

---

## Operating principle

The tech lead is the **approver of everything and the author of almost nothing**.

The obvious allocation — lead does the upstream stages, devs do the downstream ones —
fails on a team this small. The agents draft stage 5 as readily as they draft stages
1–3, so devs restricted to delivery end up reviewing agent output on the least
interesting part of the pipeline while all the design judgement concentrates in one
person. **Every owner therefore runs a full pipeline end to end**, devs included, and
the lead's distinct value is gate approval plus the cross-context calls nobody else
can make.

---

## Story ownership

Each story has one **owner** who carries it from stage 1 to stage 8.

**The sprint takes as many stories as there are owners — one in-flight story each.**
Owners are the developers plus the tech lead; QA does not own stories. For this team
that is **three concurrent pipelines** (Dev A, Dev B, tech lead), and the examples
below use three throughout. A team of four devs would run five.

> **This cap is per-team, and this repo has its own team.** `pcfdlrm` and `stagingdlrm`
> are staffed separately, so this repo's concurrency is set by the owners working
> *here* — the other repo's in-flight stories neither add to nor consume it. Where the
> two repos are covered by a smaller shared group, count the owners actually available
> to this repo and size the sprint from that number, not from the table's three.

| #  | Stage                           | Author                              | Gate  | Approver       |
|----|---------------------------------|-------------------------------------|-------|----------------|
| 1  | Requirements                    | Story owner                         | Human | Tech lead + QA |
| 2a | Design — *cross-context impact* | Tech lead **with** story owner      | Human | Tech lead      |
| 2b | Design — *inside this service*  | Story owner                         | Human | Tech lead      |
| 3  | User Story                      | Story owner drafts, team refines    | Human | Whole team     |
| 4  | Test Specs                      | **QA**                              | Human | QA + tech lead |
| 5  | Code                            | Story owner (+ peer dev if pairing) | Auto  | —              |
| 6  | Code Review                     | Peer dev first pass                 | Human | Tech lead      |
| 7  | Build & Test                    | Story owner                         | Auto  | —              |
| 8  | Deploy Sandbox                  | Story owner                         | Human | Tech lead      |

QA owns stage 4 on every story — it is their specialism, not a rotation, and they sit
across all three in-flight pipelines (see *Dual-track scheduling*).

### Who approves the tech lead's own story

The lead cannot approve their own gates — *cross-review, never self-review* applies to
them too. On the story the lead owns:

- **Stages 1, 2a, 2b, 3, 8** — approved by the developer who is not reviewing at the
  time; QA co-approves stage 1 as normal.
- **Stage 4** — QA authors it, so QA plus the peer developer approve.
- **Stage 6** — a developer does the first pass *and* signs the gate.

This is the one place the model needs a named substitute. Agree who it is at sprint
planning rather than discovering it at the gate.

### Why stage 2 splits

There is one genuinely lead-shaped question in stage 2: does this story cross a
context boundary? This repo sits in the middle of a five-context chain — `stagingdlrm`
upstream, `progression`, `listing` and `material` downstream — so the question is live
on more stories here than in most contexts. Which services change, what arrives on
`public.event`, whether `public.pcfdlrm.migrated-case-file-processed` changes shape,
what `referencedata` has to resolve. That needs cross-context knowledge a dev may not
have yet — **that is 2a**.

Everything below it is **2b**, and the person building it should own it:

- aggregate changes in `MigratedCaseFileAggregate` and its `validation/` rule classes
- new domain events, their JSON schemas, and the descriptor entries that route them
- view-store shape and Liquibase changesets under `pcfdlrm-viewstore-liquibase`
- whether a change belongs in the event processor or the (still stubbed) event listener
- reference-data lookups and enrichers in `pcfdlrm-refdata`

Splitting 2a from 2b is what stops "architecture" being a black box the devs receive.
For most stories 2a is a single line — *no cross-context impact* — and the real work
is all in 2b.

---

## Before the pipeline: refinement

Splitting the epic into stories is a **human activity that happens outside the
pipeline**, at refinement, led by the tech lead. Two things must come out of it:

**Independently deliverable stories.** Each story must be shippable on its own. If two
stories cannot be deployed in either order, they are not really two stories.

**No module collisions.** This is the one job nobody but the tech lead can do.
Modules that change together in this repo:

- `pcfdlrm-command-handler` + `pcfdlrm-domain-aggregate` (handlers and the aggregate's
  `validation/` rules move as one unit)
- `pcfdlrm-event-processor` + `subscriptions-descriptor.yaml` +
  `public-publications-descriptor.yaml` + the domain-event schema tree
- `pcfdlrm-refdata` + whichever processor consumes the enricher

Two concurrent stories both touching `MigratedCaseFileAggregate` will conflict badly.
Catching that at refinement is worth more than any artefact produced later.

Note this is an **intra-repo** concern only — two stories in two different repos
cannot conflict in the merge sense. They can still conflict on a *contract*, which is
what *Stories that span repos* below is for.

---

## Dual-track scheduling

"One in-flight story per owner" counts stories being **built**. Because the six human
gates leave every pipeline parked at intervals, each owner also runs the next story's
early stages in the gaps:

- **Delivery track** — stages 5–8 for the story currently being built.
- **Discovery track** — stages 1–4 for the next story off the backlog.

Gates are where the tracks interleave: when a story is parked awaiting gate approval,
its owner moves to the other track rather than idling.

QA is in both tracks continuously — stage 4 for the stories entering the queue, stage
7 verification for the ones leaving it.

---

## Running the sprint's pipelines in parallel

One in-flight story per owner — for this team, three concurrent pipelines:

```
Sprint N
  Story 1  ──  Dev A       ──►  1 2 3 4 5 6 7 8
  Story 2  ──  Dev B       ──►  1 2 3 4 5 6 7 8
  Story 3  ──  Tech lead   ──►  1 2 3 4 5 6 7 8
```

Each runs on its own branch, in its own `docs/pipeline/` directory, with no shared
artefacts. The only sync points are the gates.

Stories are pulled from the backlog independently — they need not come from the same
epic, or even the same repo. Three stories from three different epics is a perfectly
normal sprint; the directory prefix just makes their lineage visible.

### Cross-review, never self-review

The peer dev reviews first; the tech lead approves. The `code-reviewer` agent produces
the structured report, but a **human still signs the gate**.

---

## Artefact paths

The plugin's own `CLAUDE.md` writes to a flat layout (`requirements.md`,
`user-stories/`, `test-specs/`, …). **This repo overrides it** with a per-ticket
directory — see the root `CLAUDE.md`. With per-story pipelines the directory is named:

```
docs/pipeline/<EPIC-JIRA-ID>-<STORY-JIRA-ID>-<slug-for-story>/
```

The **epic's** Jira ID comes first, so an epic's stories sort together in a flat
listing however many there are. The **story's** own Jira ID follows it, so either key
can be grepped without opening a file:

```
docs/pipeline/
├── DD-43191-DD-43194-j25-parity/
│   ├── 00-input-brief.md      # epic framing + this story's request
│   ├── 01-requirements.md     # for THIS story
│   ├── 02-design.md           # for THIS story
│   └── 03-stories.md
├── DD-43191-DD-43194-j25-upgrade/
│   └── 00-input-brief.md
└── DD-43067-DD-43099-pcfdlrm-test-hardening/
    └── 00-input-brief.md
```

Three rules that fall out of this:

- **The story key makes the directory unique**, so slugs only need to be *descriptive*.
  Still name them after what the story changes, not after the epic — `j25-parity`,
  not `j25-part-1`. Where two stages of one effort share a Jira key, the slug is what
  separates them, so it has to carry real meaning.
- **A story with no parent epic uses its own ticket ID alone** as the prefix.
- **Existing directories are not renamed** when the convention changes. The prefix form
  tells you which era a directory belongs to; that is cheaper than a rewrite that breaks
  every link into it.

The repo convention wins, and with concurrent per-story pipelines it is essential —
the plugin's flat layout means several story owners running stage 1 at the same time
overwrite each other's `requirements.md`. Make sure the root `CLAUDE.md` is loaded in
every session, or the agents will follow their own convention.

### Shared design decisions go in an ADR — and this repo holds its own copy

When several stories in an epic depend on the same decision, it goes in an ADR
**once per repo**, in `docs/pipeline/adrs/`, and each story's `02-design.md` links to
it by relative path. The plugin's `adr-template` skill covers the format.

**ADRs are named `<JIRA-KEY>-<slug>.md`, never numbered** — `adrs/DD-43191-j25-parity-method.md`. A
sequential number has to be allocated at authoring time, which cannot work with stories running in
parallel on separate branches: two authors both reach for the next integer and neither sees the other
until merge. Keying to the epic or story that produced the ADR makes collision impossible, needs no
register and no approval round-trip, gives a mirrored ADR the same filename in both repos automatically,
and makes `grep DD-43191` find the stories and their ADRs together. An epic with several ADRs
distinguishes them by slug, exactly as its story directories do. Pre-existing numbered ADRs keep their
names.

**A decision shared with `cpp-context-stagingdlrm` is mirrored here, not linked
across.** Because the two repos are worked independently, a cross-repo URL would make
this repo's pipeline non-self-contained: the other repo's branch may not be checked
out, may be ahead, or may not yet carry the file at all. So:

- The ADR is **authored once and committed to both repos** under the same filename, with an explicit header line naming the mirror — e.g.
  *"Mirrored in `cpp-context-stagingdlrm/docs/pipeline/adrs/006-…md`. Amend both, or
  neither."*
- Amendments go into **both repos in the same pair of PRs**. A one-sided amendment is
  the drift this rule exists to catch, and the header line is what makes it visible at
  review time.
- Sections that apply to only one repo say so inline (*"stagingDLRM only"*), so the
  mirrored file stays byte-identical rather than forking into two near-copies.

Do not restate an ADR's content in a design doc — link to the local copy and move on.

---

## Stories that span repos

A story lives in one repo. An epic whose stories land in different repos is just
several independent pipelines, each in its own repo — nothing special is needed
**unless the stories depend on each other**. For this repo they very often do, because
almost everything it receives comes from `stagingdlrm` and almost everything it emits
is consumed by `progression`.

When they do — a REST call, or an event one publishes and the other consumes:

**Write the contract as an ADR before either story starts stage 5.** The schema/RAML
change is settled up front, not discovered during implementation. Two devs building
against unstated assumptions about the same payload is the failure mode, and it stays
invisible until integration — and with the two repos worked in isolation, nobody is
holding both halves in their head. Per the rule above, that ADR is **mirrored into both
repos** so each dev has it locally. Validate both sides afterwards with the
`api-contract-check` skill.

**Neither dev is blocked on the other's gates.** Cross-repo coupling is the contract and
the deploy order, nothing else. If a story here cannot reach stage 8 until something
merges in `stagingdlrm`, that is a fact for stage 2a and the sprint plan — it is never a
reason for this repo's pipeline to pause mid-stage waiting on another repo's gate.

**Stage 4 needs a third test scope.** Per-repo tests cover each story; the interaction
between them is covered by neither. That test belongs in `cpp-apitests`, so QA's stage
4 output is three sets of specs, not two. Use the `cpp-test-authoring` skill for it.

**Stage 8 needs a deploy order.** CI is per-repo, so the two builds are independent,
but deploy order matters. Decide it at 2a alongside the contract: if the change is
backward-compatible, deploy the consumer first.

**Slice test:** if the two stories cannot be deployed independently, the split is
wrong. Either make the change backward-compatible, or accept that this is one unit of
work delivered by a pair — not two stories running in parallel.

---

## Right-sizing the gates

Per-story pipelines multiply gate approvals: **six human gates × every story in the
sprint**. For this team that is around eighteen, nearly all landing on the tech lead —
who is also carrying a story of their own. That is the real cost of this model, and it
grows linearly as the team does.

Three things keep it manageable:

**Batch the upstream gates.** Approve stages 1–3 for every story in one refinement
session rather than being interrupted per story. This alone collapses roughly half the
gate count into a single meeting.

**Stagger pipeline starts.** If all three stories hit stage 2 the same afternoon, the
lead becomes a queue. Starting them a day apart spreads the gates across the sprint at
no cost to throughput.

**Keep trivial artefacts trivial.** The plugin's hard rules forbid skipping or
reordering stages, so do not mark them N/A — but a stage's output can legitimately be
one line. For a small story, `02-design.md` reading *"2a: no cross-context impact. 2b:
adds one field to the view store, see changeset."* is a complete and honest artefact.
The gate still happens; it just takes ten seconds.

If the load is still unworkable after all three, the next lever is dropping stage 2a
to lead-*notification* rather than lead-*approval* for stories with no cross-context
impact. That is a real deviation from the plugin's hard rules, so make it a team
decision and record it as an ADR — do not let it happen by drift.

---

## Repo-specific caveats

### The three-layer rule outranks any agent's plan

Every change touching events must be reasoned across command side, event listener and
event processor — the root `CLAUDE.md` section *Architecture — the three layers you
must reason across* is the authority, and *Critical gotcha — when adding/removing an
event* is the one that bites: a subscription without a matching JSON schema is a
runtime 500 on dispatch, not a build failure, so **stage 7 will be green and sandbox
will not**. Stage 2b must name every descriptor and schema file the story touches, and
stage 6 should check that list against the diff.

Always re-read the authoritative routing files listed in the root `CLAUDE.md` before
reasoning about a flow. They are the source of truth; an agent's summary of them is not.

### The event listener is a stub

`pcfdlrm-event/pcfdlrm-event-listener` is wired into the reactor but has no
`subscriptions-descriptor.yaml` and no Java listeners, while `pcfdlrm-viewstore-liquibase`
is already in place. A story that introduces a read model therefore has more stage 2b
surface than the module layout suggests — it creates the descriptor rather than editing
one. Flag that at refinement so it is not sized as an edit.

### Cross-context version bumps are enforced in CI

When a story bumps any upstream interface version in `pom.xml`, the matching
schema/RAML classifier dependency must move to the same version or schema validation
fails at runtime. `RequireLatestMojInterfaceRule` blocks stale interface versions in CI,
so this surfaces at stage 7 as an enforcer failure — cheap to fix, but only if stage 2b
listed the bump. See *Cross-context dependency bumps* in the root `CLAUDE.md`.

### Integration tests need Docker

`runIntegrationTests.sh` drives `pcfdlrm-integration-test` and needs a working local
Docker environment. Where an owner cannot run it, stage 7 IT verification is
authored-not-executed until CI runs it — say so at the gate rather than reporting a
green stage 7.

### No Spec-Kit, no local agent overrides

A previous `.claude/agents/` + `.claude/rules/` + `.specify/` Spec-Kit setup was
installed in this repo but never driven, and has been removed in favour of the plugin.
Do not reintroduce it, and do not let an agent resurrect its conventions — the plugin's
agents are reused as-is here.

### Skills that do not apply here

Do **not** use `springboot-service-from-template`, `springboot-api-from-template`,
`terraform-validate`, or `helm-config-validator` — there is no Spring Boot, Terraform,
or Helm chart in this repo.

The `architecture-designer` agent will offer the MbD-vs-context-service choice at
stage 2a; for changes inside this service the answer is already **CQRS context
service**.

Beyond the shared set, this repo does make real use of `event-flow-mapper` (the
three-layer/public-event tracing above), `migration-reviewer` (viewstore Liquibase) and
`rbac-auditor`.

---

## Tech lead load — the known risk

The lead carries refinement, stage 2a on every story, story slicing, six gate
approvals per story they do not own, **and** a full pipeline of their own. That is the
most loaded role in this model by a wide margin, and it is the first thing to watch.

Mitigations, in the order to reach for them:

1. **Batch and stagger the gates** (see *Right-sizing the gates*).
2. **Delegate the stage 6 first pass** to the peer developer, with the lead approving
   only once that pass is clean.
3. **Drop the lead's own story.** If gates are slipping, the lead owning a pipeline is
   the thing to give up — not the gate quality. The sprint then takes one fewer story,
   which is the honest trade rather than a hidden one.

Option 3 is the release valve. A sprint of two well-gated stories beats three with
rubber-stamped approvals, and the whole value of the pipeline is in the gates being
real.
