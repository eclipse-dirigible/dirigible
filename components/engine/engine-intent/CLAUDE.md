# engine-intent

A single `.intent` YAML file at a project root becomes the source of truth for every other artefact in that project. EDM, DSM, BPMN, forms, reports, generated TS / Java are all `gen/` artefacts owned by this engine - developers must not edit them. This is one altitude higher than the existing pattern (where the standard models were the source and HTML / SQL / ORM mappings were the gen output): here the standard models themselves are gen.

The whole feature lives in `org.eclipse.dirigible.components.intent.*`.

## Design context (what we agreed)

Distilled from the chat that produced the initial scaffold. Read this BEFORE designing additional pieces - half of these decisions are not obvious from the code.

### Why this exists

Dirigible is already model-driven (the synchronizer model = "declarations on disk → running app"). Adding an intent layer above EDM/BPMN/form/DSM is the natural next abstraction. The second half of the pipeline (intent → standard models → generated app) reuses what already exists: project templates, decorator-driven scaffolding, the TS/Java SDKs. No new runtime concept - just a new authoring layer above the existing ones.

The dream is "no code, no modelling - just prompt": user describes what they want in natural language to Claude (or any LLM); Claude proposes a patch to `.intent`; the user accepts; the synchronizer regenerates the whole app under `gen/`; Mermaid renders the intent for a quick read-only visual.

### Three things any non-trivial change here must reckon with

1. **Expressiveness ceiling - the thing that kills MDE projects.** Every EDM attribute, every BPMN gateway condition, every form validator, every report aggregation, every permission rule has to be representable in `.intent`, because the developer can NOT escape to `gen/`. Real apps always have one weird bit. We chose the **escape-hatch directory** approach over union-of-everything: a `/custom/` sibling to `/gen/` will hold hand-written code preserved across regenerations, intent declares hook points, custom files supply implementations. Pure MDE has been tried for thirty years and the escape hatch always wins. The `/custom/` folder is NOT yet wired (out of scope for this skeleton) but every generator must be written assuming it exists - never emit into `gen/` something that should have been overridable.

2. **LLM determinism - edit shape, not file shape.** "Add a `country` field to `Customer`" must produce a one-line diff to `app.intent`, not a re-emitted file with entities reordered. Claude's job is **proposing a patch** to the intent (structured operations / unified diff), not regenerating it. The UI should show patch + Mermaid preview + accept/reject before applying. The structured-edit panel in the IDE is the power-user fallback when Claude misunderstands. The intent JSON is therefore arranged so diffs are minimal and stable: entities/processes/forms/reports/permissions are arrays (preserved order), nested fields use object literals, and the parser does not normalize field order. Do not introduce auto-sorting or reformatting on save.

3. **Intent is structured, not free text.** The LLM converts NL → structured YAML; transforms from intent to EDM/BPMN/form are pure deterministic functions; Mermaid renders from the same model. Three distinct stages. The LLM is replaceable / optional - the intent format must be authorable by a human in a structured editor too.

### Concrete agreements

- **YAML, not JSON.** Optimised for human authoring (comments, multi-line strings, no quote noise, friendlier LLM diffs). Parsed via SnakeYAML's `SafeConstructor` (already on the classpath transitively via Spring Boot) then round-tripped through Gson to land in the typed POJOs - one mapping path for both surfaces.
- **Safe YAML loading is non-negotiable.** `IntentParser` constructs SnakeYAML with `SafeConstructor`, which blocks `!!type` / `!!new` tags. Intents arrive from LLM output and human paste; YAML deserialisation must never become a code-execution surface. Do not swap to `Constructor` for "ergonomics".
- **One `.intent` file per project**, at the project root. There is no plan to support multiple intents per project - the whole model lives in one place so the LLM has the whole picture to diff against. (Re-evaluate if intents grow past ~2000 lines in practice; until then, one file.)
- **Everything else under `/gen/` is owned by the regeneration pass.** Developers must not edit `gen/`; the IDE surface should mark it read-only. The synchronizer does NOT need to enforce this - anything hand-edited there is overwritten on next intent change anyway.
- **Existing projects without an intent stay "classic"** (hand-edit EDM/BPMN/form as before). An "intent project" is detected by the presence of `app.intent` at project root. A future `reverse-engineer intent` command can scan EDM/BPMN/form and propose an intent file to migrate; out of scope for now.
- **Mermaid renders the intent for visualisation**, read-only. We do NOT build a Mermaid round-trip editor (it is a poor authoring surface). Editing is via the LLM prompt + structured panel; the existing modelers are NOT re-used for intent projects (they would let developers edit gen/ in disguise).
- **Run-once-fix-it via Claude.** When something can't be expressed, the answer is to extend `.intent` (add a field to the schema, add a generator that consumes it), not to leak into gen/.

## Module layout

```
components/engine/engine-intent/
├── pom.xml                                # depends only on core-base, core-database, core-repository
├── about.html
├── CLAUDE.md                              # this file
└── src/main/java/org/eclipse/dirigible/components/intent/
    ├── domain/Intent.java                 # JPA entity; ARTEFACT_TYPE = "intent", table DIRIGIBLE_INTENTS
    ├── repository/IntentRepository.java   # Spring Data
    ├── service/IntentService.java         # CRUD via BaseArtefactService
    ├── model/                             # POJOs for the intent document (Gson-mapped after YAML → Map → JSON round-trip)
    │   ├── IntentModel.java               # root: entities / processes / forms / reports / permissions
    │   ├── EntityIntent.java
    │   ├── FieldIntent.java
    │   ├── RelationIntent.java
    │   ├── ProcessIntent.java
    │   ├── StepIntent.java
    │   ├── FormIntent.java
    │   ├── ReportIntent.java
    │   └── PermissionIntent.java
    ├── parser/IntentParser.java           # YAML → Map (SnakeYAML SafeConstructor) → JSON → IntentModel (Gson)
    ├── generator/
    │   ├── IntentTargetGenerator.java     # SPI - one per slice (entities, processes, forms, ...)
    │   ├── IntentGenerationContext.java   # carries Intent + IntentModel + projectRoot + IRepository
    │   └── IntentRegenerationService.java # collects every SPI bean and runs them in @Order
    └── synchronizer/IntentSynchronizer.java   # BaseSynchronizer; regen pass in finishing()
```

No generator implementations live in this module yet - only the SPI. Concrete generators (entity → `gen/<name>.entity` + `gen/<name>.dsm`, process → `gen/<name>.bpmn`, form → `gen/<name>.form`, report → `gen/<name>.report`, permission → `gen/<name>.roles` + `gen/<name>.access`, controller → `gen/<name>.controller.ts`) belong in follow-up modules or sibling generator packages that depend on this one plus the relevant target artefact module. They are Spring `@Component` beans implementing `IntentTargetGenerator`; ordering via `@Order`.

## Wiring

- **Artefact type string `intent`, file extension `.intent`, JPA table `DIRIGIBLE_INTENTS`.**
- **`SynchronizersOrder.INTENT = 5`** - lower than every other artefact (EXTENSIONPOINT = 10 is the previous floor) so the intent's regenerated `gen/` files are on disk before any other synchronizer starts the NEXT cycle.
- **`IntentSynchronizer extends BaseSynchronizer<Intent, Long>`** - single-tenant. Intent itself carries no runtime state; downstream synchronizers handle their own tenancy.
- **Module registered in:**
  - `components/pom.xml` (Maven reactor)
  - `components/group/group-engines/pom.xml` (so `build/application` picks it up via the group aggregator)

## Synchronizer flow

1. `parseImpl(location, content)` - reads the YAML bytes, persists `Intent` with the raw payload in `INTENT_CONTENT`, marks the intent dirty for `finishing()`. Structural validation is deferred to `IntentParser.parse` in the regeneration pass so that a malformed YAML body doesn't block the artefact from being recorded.
2. `completeImpl(wrapper, phase)` - pure book-keeping (CREATED / UPDATED / DELETED). No runtime side-effects.
3. `finishing()` - for every intent marked dirty this cycle, calls `IntentRegenerationService.regenerate(intent)`. Each registered `IntentTargetGenerator` writes its slice under `<projectRoot>/gen/`. Failures in one generator are logged and isolated; the others still run.

### Open design question: same-cycle vs next-cycle visibility

`SynchronizationProcessor` walks the repository **once per cycle**, dispatching every file to the first synchronizer whose `isAccepted` matches. Files written **during** the cycle - including everything `IntentRegenerationService` writes under `gen/` - are NOT visible to other synchronizers in the same cycle. They are picked up on the **next** reconciliation.

This is acceptable for the scaffold: developer publishes, intent regenerates, second reconciliation (auto or manual) brings the rest live. UX-wise it is a half-beat behind. Real options to fix:

1. **Pre-pass orchestration.** Have `SynchronizationProcessor` invoke a new "before walk" hook on each synchronizer; `IntentSynchronizer` regenerates there. Cleanest, requires editing `core-initializers`.
2. **Self-triggered second cycle.** At the end of `IntentSynchronizer.finishing()`, if any intent was dirty, schedule a follow-up `forceProcessSynchronizers()` call. Simple, risks a tight loop if not guarded; use the existing `processing` AtomicBoolean.
3. **Live with two cycles.** Document it; the IDE "publish" button fires two `forceProcessSynchronizers()` calls back to back.

The skeleton picks option 3 because it is the only one that does not touch other modules. Pick option 1 when the surface is otherwise stable.

## Intent YAML shape (v1 draft)

The shape the model POJOs serialize to / deserialize from. Keep field names stable - this is the schema the LLM is prompted against. Every collection defaults to empty so partial intents (e.g. entities only) parse cleanly. Field names are camelCase to match the POJOs after the SnakeYAML → Map → Gson → POJO round-trip (Gson does not do snake_case-to-camelCase rewriting by default).

```yaml
name: orders
description: Order management with approval workflow
version: 1

entities:
  - name: Customer
    description: Buyer account
    fields:
      - { name: id,      type: uuid,   primaryKey: true, generated: true }
      - { name: name,    type: string, required: true, length: 200 }
      - { name: country, type: string, length: 2 }
    relations:
      - { name: orders, kind: oneToMany, to: Order }

processes:
  - name: OrderApproval
    trigger: { onCreate: Order }
    steps:
      - name: managerReview
        kind: userTask
        args: { assignee: manager, form: ApproveOrder }
      - name: bigOrder
        kind: decision
        args: { if: "amount > 10000", then: cfoReview }
      - name: cfoReview
        kind: userTask
        args: { assignee: cfo, form: ApproveOrder }

forms:
  - name: ApproveOrder
    forEntity: Order
    fields: [items, total]
    actions: [approve, reject]

reports:
  - name: OrdersByCountry
    source: Order
    dimensions: [customer.country]
    measures: ["count(*)", "sum(total)"]

permissions:
  - { role: Sales,   can: [Customer:read, Customer:write, Order:create] }
  - { role: Manager, can: [Order:approve] }
```

Logical field types (`FieldIntent.type`) are: `string`, `text`, `integer`, `decimal`, `boolean`, `date`, `uuid`. Generators map them to JDBC + EDM types. Relation kinds: `oneToMany`, `manyToOne`, `oneToOne`, `manyToMany`. Step kinds: `userTask`, `serviceTask`, `decision`, `script`, `end`.

### YAML authoring rules

- **Comments are allowed and encouraged.** Lines starting with `#` survive a SnakeYAML load → JSON round-trip only as dropped content, so they are NOT preserved across regeneration of the intent itself - but since the intent is the only authored artefact and no tool ever rewrites it, comments authored by a developer stay put. The LLM patch path must respect the surrounding comments.
- **No anchors / aliases (`&foo` / `*foo`)** at v1. They cut copy-paste corners for the author but make the file far harder for the LLM to diff. If duplication becomes painful, introduce a top-level `defaults:` block rather than YAML's structural aliasing.
- **No multi-document YAML (`---`).** One intent file, one YAML document.
- **Tags forbidden.** Already enforced by `SafeConstructor`; mentioned here so a future reader does not "fix" it.

## Things to not do

- **Don't make intent multi-tenant.** Authoring is single-tenant; generated artefacts handle their own tenancy.
- **Don't let intent rewrite or sort itself.** Diff stability matters - the LLM has to produce minimal patches, which only works if the on-disk shape is stable. No auto-formatting, no field reordering.
- **Don't generate outside `gen/`.** The synchronizer relies on this to scrub stale gen/ files between cycles without risking developer-authored files. `IntentGenerationContext.getGenRoot()` is the only path generators write to.
- **Don't add a Mermaid editor.** Mermaid is for visualisation. Authoring is prompt + structured panel.
- **Don't reuse the existing modelers for intent projects.** That would re-expose `gen/` as an authoring surface and undo the whole point.
- **Don't read env vars or system properties directly** - go through `DirigibleConfig` per the platform-wide rule.

## Follow-ups not in this skeleton

- Concrete `IntentTargetGenerator` implementations per slice (entities, processes, forms, reports, permissions, controllers).
- IDE perspective with Mermaid renderer + Claude chat + patch preview + accept/reject. Lives in `components/ui/perspective-intent` once the generators land.
- Read-only Monaco model for paths under `**/gen/**`.
- `/custom/` escape-hatch directory + per-slice hook points in the generators.
- `reverse-engineer intent` command for migrating classic projects.
- Same-cycle visibility (open design question above).
- Schema validation on parse (currently the parser accepts anything SnakeYAML + Gson can map).
