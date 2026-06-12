# engine-intent

A single `.intent` YAML file at a project root becomes the source of truth for every other authoring artefact in that project. The intent layer is **one altitude above** the existing model files: where Dirigible used to have hand-authored `.edm` / `.bpmn` / `.form` / `.report` / `.roles` / `.access` / `.csvim` and code-gen them to TS / HTML / Java / SQL on demand, the intent layer authors the **`.edm` / `.bpmn` / ... model files themselves** from one YAML.

The whole feature lives in `org.eclipse.dirigible.components.intent.*`.

## Two-stage architecture - **read this first**

```
app.intent (YAML, author-driven by Claude / human / structured panel)
   ↓  Intent generators (this engine)
<intent>.edm + <intent>.model     ← entities + relations + UI metadata   (at the project root)
<process>.bpmn                    ← processes
<form>.form                       ← forms
<report>.report                   ← reports
<intent>.roles + <intent>.access  ← permissions
<seed>.csvim + <seed>.csv         ← seed data
<schema>.dsm + <schema>.schema    ← low-level data structures (future)
   ↓  Existing Dirigible template engine + per-artefact synchronizers
[Hibernate-mapped tables, generated TS / HTML / Java / SQL artefacts under gen/...]
```

**Why the project root and not `gen/`:** the model-to-code templates ("Generate from EDM") treat
`gen/` as their exclusive output folder and **wipe it on every regeneration** - intent output placed
there would be destroyed the first time the user generates the application code. The project root is
where every platform fixture keeps hand-authored model files and the only location the downstream
template flow is proven to handle; a dedicated `models/` subfolder is a future refinement once the
templates' handling of model files in subfolders is verified. The folders layer as: `app.intent`
(authored) + root model files (intent-owned, scrubbed by this engine) → `gen/` (template-owned,
wiped by the template engine) → `custom/` (hand-written escape hatch, touched by nobody).

Intent generators stop at the **model file**. They never emit `Entity.ts`, `Controller.ts`, `Repository.ts`, HTML, Java, or SQL directly - those come from the IDE's existing "Generate from EDM / Schema / BPMN" templates, fed by the model files this engine wrote. That contract is non-negotiable; see "Wrong turns we already made" below.

## Design context (what we agreed)

Distilled from the chat that produced the initial scaffold. Read this BEFORE designing additional pieces - half of these decisions are not obvious from the code.

### Why this exists

Dirigible is already model-driven (the synchronizer model = "declarations on disk → running app"). Adding an intent layer above EDM/BPMN/form/DSM is the natural next abstraction. The second half of the pipeline (intent → standard models → generated app) reuses what already exists: project templates, decorator-driven scaffolding, the TS/Java SDKs. No new runtime concept - just a new authoring layer above the existing ones.

The dream is "no code, no modelling - just prompt": user describes what they want in natural language to Claude (or any LLM); Claude proposes a patch to `.intent`; the user accepts; the synchronizer regenerates the model files at the project root, and the template engine turns them into the app under `gen/`; Mermaid renders the intent for a quick read-only visual.

### Three things any non-trivial change here must reckon with

1. **Expressiveness ceiling - the thing that kills MDE projects.** Every EDM attribute, every BPMN gateway condition, every form validator, every report aggregation, every permission rule has to be representable in `.intent`, because the developer can NOT escape to `gen/`. Real apps always have one weird bit. We chose the **escape-hatch directory** approach over union-of-everything: a `/custom/` sibling to `/gen/` will hold hand-written code preserved across regenerations, intent declares hook points, custom files supply implementations. Pure MDE has been tried for thirty years and the escape hatch always wins. The `/custom/` folder is NOT yet wired (out of scope for this skeleton) but every generator must be written assuming it exists - never emit into `gen/` something that should have been overridable.

2. **LLM determinism - edit shape, not file shape.** "Add a `country` field to `Customer`" must produce a one-line diff to `app.intent`, not a re-emitted file with entities reordered. Claude's job is **proposing a patch** to the intent (structured operations / unified diff), not regenerating it. The UI should show patch + Mermaid preview + accept/reject before applying. The structured-edit panel in the IDE is the power-user fallback when Claude misunderstands. The intent JSON is therefore arranged so diffs are minimal and stable: entities/processes/forms/reports/permissions are arrays (preserved order), nested fields use object literals, and the parser does not normalize field order. Do not introduce auto-sorting or reformatting on save.

3. **Intent is structured, not free text.** The LLM converts NL → structured YAML; transforms from intent to EDM/BPMN/form are pure deterministic functions; Mermaid renders from the same model. Three distinct stages. The LLM is replaceable / optional - the intent format must be authorable by a human in a structured editor too.

### Concrete agreements

- **Intent generators target the model layer ONLY.** Output extensions are restricted to `.edm` / `.model` / `.bpmn` / `.form` / `.report` / `.roles` / `.access` / `.dsm` / `.schema` / `.table` / `.view` / `.csvim` / `.csv`. Anything code-shaped (`Entity.ts`, `Controller.ts`, `Repository.ts`, `*.java`, `*.html`, `*.sql`) is the **template engine's** output and must not appear in any intent generator. If you find yourself emitting code, you are at the wrong altitude.
- **YAML, not JSON.** Optimised for human authoring (comments, multi-line strings, no quote noise, friendlier LLM diffs). Parsed via SnakeYAML's `SafeConstructor` (already on the classpath transitively via Spring Boot) then round-tripped through a **plain Gson** instance to land in the typed POJOs. NOT through the platform's `JsonHelper`/`GsonHelper`: those are configured with `excludeFieldsWithoutExposeAnnotation()`, which silently maps every un-annotated POJO field to null - the parse "succeeds" with an empty model and every generator quietly skips (this bug shipped once; the IT only caught it because it asserts file existence). The parser's Gson also sets `ToNumberPolicy.LONG_OR_DOUBLE` so YAML integers in seed rows stay integral (`id: 1` -> CSV `1`, not `1.0`). `JsonHelper` remains fine for the generators' Map-shaped output (maps are not field-reflected).
- **Safe YAML loading is non-negotiable.** `IntentParser` constructs SnakeYAML with `SafeConstructor`, which blocks `!!type` / `!!new` tags. Intents arrive from LLM output and human paste; YAML deserialisation must never become a code-execution surface. Do not swap to `Constructor` for "ergonomics".
- **One `.intent` file per project**, at the project root. There is no plan to support multiple intents per project - the whole model lives in one place so the LLM has the whole picture to diff against. (Re-evaluate if intents grow past ~2000 lines in practice; until then, one file.)
- **In an intent project, model-layer files at the project root are owned by the regeneration pass; `/gen/` stays the template engine's.** Developers must not hand-edit the generated `.edm`/`.bpmn`/`.form`/... - anything hand-edited is overwritten, and files no longer backed by the intent are scrubbed on the next regeneration (so adding `app.intent` to a classic project hands ownership of its root-level model files to the intent engine - migrate them into the intent first). `gen/` keeps its existing platform meaning: the model-to-code templates' output folder, wiped by them on every regeneration.
- **Existing projects without an intent stay "classic"** (hand-edit EDM/BPMN/form as before). An "intent project" is detected by the presence of `app.intent` at project root. A future `reverse-engineer intent` command can scan EDM/BPMN/form and propose an intent file to migrate; out of scope for now.
- **Mermaid renders the intent for visualisation**, read-only. We do NOT build a Mermaid round-trip editor (it is a poor authoring surface). Editing is via the LLM prompt + structured panel; the existing modelers are NOT re-used for intent projects (they would let developers edit gen/ in disguise).
- **Run-once-fix-it via Claude.** When something can't be expressed, the answer is to extend `.intent` (add a field to the schema, add a generator that consumes it), not to leak into gen/.

### Wrong turns we already made

These mistakes have been made and reverted. They are documented here so they are not made again.

1. **EntityIntentGenerator that wrote `gen/<Entity>Entity.ts` directly.** This was at the wrong altitude - it tried to emit the `@Entity()` / `@Table()` / `@Column()` decorator-driven TS file that the platform's `EntitySynchronizer` (extension `Entity.ts`) consumes. That artefact is itself the output of "Generate from EDM" in the IDE; intent should emit the `.edm` instead and let the existing pipeline produce the TS. The generator was committed once (commit `9570405aa9`) and later deleted - do not bring it back. The replacement is [`EdmIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/edm/EdmIntentGenerator.java) at `@Order(200)`.
2. **PermissionIntentGenerator that wrote `.access` constraints with URLs targeting the (also-wrong) generated `Controller.ts` paths.** Same mistake one altitude up: the access URLs were `/services/ts/<project>/gen/<Entity>Controller.ts/*`, which assumed the missing TS controllers existed. The right output for permissions is the same `.roles` + `.access` artefacts but with paths reflecting whatever the EDM template emits, OR (preferred) lean on the `.edm` entity's own `generateDefaultRoles="true"` flag and let the template produce roles + access in lockstep with the generated UI. Not yet implemented; see the follow-up list.
3. **Generators that wrote `IRepository` paths without the `/registry/public` prefix.** Synchronizer artefact locations are *registry-relative* (`/orders/app.intent` - see `SynchronizationWalker.walk`, which strips the registry folder), but `IRepository` paths are *repository-absolute*. The first regeneration implementation derived the project root straight from the location and wrote `gen/` output to `/orders/gen/...` - i.e. **outside** the registry, where no IT assertion, no Registry view, and crucially no downstream synchronizer would ever see it. The whole two-stage pipeline was dead and the IT could not pass (it was committed red). `IntentRegenerationService.resolveProjectRoot` now prepends `IRepositoryStructure.PATH_REGISTRY_PUBLIC`; the same convention is visible in `SynchronizationProcessor`'s cleanup pass and `CsvimProcessor.getCsvResource`. When in doubt: locations are registry-relative, repository paths are not.

The general rule the first two violated: **intent generators must never reference paths or routes that belong to the template engine's output**, because the intent layer should be agnostic about which template is selected.

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
    │   ├── IntentModel.java               # root: entities / processes / forms / reports / permissions / seeds
    │   ├── EntityIntent.java
    │   ├── FieldIntent.java
    │   ├── RelationIntent.java
    │   ├── ProcessIntent.java
    │   ├── StepIntent.java
    │   ├── FormIntent.java
    │   ├── ReportIntent.java
    │   ├── PermissionIntent.java
    │   └── SeedIntent.java
    ├── parser/
    │   ├── IntentParser.java              # YAML → Map (SnakeYAML SafeConstructor) → JSON → IntentModel (Gson) + structural validation
    │   └── IntentValidationException.java # collects every structural issue in one shot
    ├── generator/
    │   ├── IntentTargetGenerator.java     # SPI - one per slice (entities, processes, forms, ...)
    │   ├── IntentGenerationContext.java   # carries Intent + IntentModel + project paths; writeModelFile() is the only write surface
    │   ├── IntentNaming.java              # shared naming: baseName, upperSnake, tableName (<INTENT>_<ENTITY>)
    │   ├── IntentRegenerationService.java # collects every SPI bean, runs them in @Order, scrubs stale model output
    │   ├── edm/EdmIntentGenerator.java                # @Order(200); writes <intent>.edm (XML) + <intent>.model (JSON)
    │   ├── bpmn/BpmnIntentGenerator.java              # @Order(300); writes <process>.bpmn per process
    │   ├── form/FormIntentGenerator.java              # @Order(400); writes <form>.form per form (typed controls + action buttons + stub code)
    │   ├── report/ReportIntentGenerator.java          # @Order(500); writes <report>.report per report (dimensions + parsed measures)
    │   ├── permission/PermissionIntentGenerator.java  # @Order(600); writes <intent>.roles per intent (deduped role names + descriptions)
    │   └── csvim/CsvimIntentGenerator.java            # @Order(700); writes <seed>.csvim + <seed>.csv per seed
    ├── synchronizer/IntentSynchronizer.java   # BaseSynchronizer; regen pass in finishing()
    └── endpoint/IntentEndpoint.java           # /services/ide/intent/* - list projects, fetch parsed intent, fetch raw YAML, force regenerate
```

The IDE perspective lives in two sibling UI modules:

- `components/ui/perspective-intent` - perspective shell (id `intent`, order 1020, icon a three-node graph SVG). Default region `center`, view `intent-mermaid`.
- `components/ui/view-intent-mermaid` - read-only Mermaid ER renderer + toolbar (project picker, reload, regenerate, source / diagram toggle). Mermaid is bundled as the `org.webjars.npm:mermaid` dependency and loaded from `/webjars/mermaid/dist/mermaid.min.js` - the platform pattern for third-party frontend libraries (NOT a CDN: nothing else in the IDE loads off-host, and air-gapped deployments must keep working). Server returns parsed `IntentModel` JSON; the view converts to `erDiagram` spec client-side.

Six concrete generators currently live in-module:

- [`EdmIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/edm/EdmIntentGenerator.java) writes `<intent>.edm` (XML) plus `<intent>.model` (JSON twin) from the entities + relations declared in the intent. Each entity is fleshed out with EDM editor defaults (icons, menu keys, layout type, perspective metadata, widget types) derived from the entity / field names so the produced model is a complete, openable EDM document. Conventions mirrored from real editor-written documents (`DependsOnScenariosTestProject/sales-order.edm` is the reference): `dataName` is intent-prefixed (`ORDERS_COUNTRY`); a `required` to-one relation is a **composition** (FK property carries the `relationship*` attributes, the owner becomes DEPENDENT/MANAGE_DETAILS and inherits the - transitively resolved - parent perspective) while an optional one is a plain association DROPDOWN; dropdown key/value and `referencedProperty` come from the target entity's actual PK and `name`-like fields; the `.model` JSON carries `entities`/`perspectives`/`navigations` (no `relations` key - relations are XML-only, interleaved with their owning `<entity>`).
- [`BpmnIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/bpmn/BpmnIntentGenerator.java) writes one `<process>.bpmn` per process. Minimal Flowable-flavoured BPMN 2.0 - one start event, one end event, the declared steps, and the sequence flows that connect them. Decisions emit an exclusiveGateway with a conditioned outgoing flow to `args.then` and a default flow to `args.else` (falling back to the next step in the chain when omitted). The `trigger` block is parsed but not consumed yet - a warning is logged. **No `bpmndi` diagram block** - Flowable runs without it and the BPMN editor auto-lays out on first edit, which keeps the output deterministic and avoids x/y churn between regenerations.
- [`FormIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/form/FormIntentGenerator.java) writes one `<form>.form` per form. Controls are typed by looking up each declared field against the bound entity (string/uuid -> input-textfield, text -> input-textarea, integer/decimal -> input-number, boolean -> input-checkbox, date -> input-date, timestamp -> input-datetime-local). Actions become buttons in a trailing `container-hbox`; the button colour is inferred from the action name (approve -> positive, reject/decline/delete/cancel -> negative, save/submit -> emphasized). A stub controller code block declares `on<Action>Clicked` handlers as TODOs - wiring to a backend is left to the downstream template engine or a hand-authored override under `custom/`.
- [`ReportIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/report/ReportIntentGenerator.java) writes one `<report>.report` per report. Dimensions become columns with `aggregate: NONE`; measures are parsed by the `aggregate(field)` convention (`count(*)`, `sum(total)`, `avg(price)`, `min(...)`, `max(...)`) into columns with the matching aggregate. `baseTable` comes from `IntentNaming.tableName` - the same intent-prefixed name the EDM declares as `dataName`, so the two can never drift. `query` / `joins` / `filters` / `orders` are left empty - the report editor builds the SQL on open.
- [`PermissionIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/permission/PermissionIntentGenerator.java) writes `<intent>.roles` from the intent's `permissions` block (deduped by role name). It deliberately does NOT emit `.access` constraints - URL-shaped access rules belong to whichever downstream template materializes the UI for an entity / form / report, because only that template knows the paths it will publish. The `can: [Resource:action, ...]` tokens on each permission are an authoring hint to downstream UI generators about which actions each role may invoke; the actual `<path, method, role>` mapping is the downstream template's contract, not intent's.
- [`CsvimIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/csvim/CsvimIntentGenerator.java) writes `<seed>.csvim` + `<seed>.csv` per seed. The `table` is `IntentNaming.tableName` (same as the EDM `dataName`); the `file` path is **project-qualified** (`/<project>/<seed>.csv`) because `CsvimProcessor` resolves it against `/registry/public`. CSVIM defaults match the existing platform samples (`header: true`, `useHeaderNames: true`, field delim `,`, enclosing `"`, `version: 1.0`, schema `PUBLIC`). The CSV header carries `<ENTITY>_<FIELD>` upper-snake column names; row order matches the entity's declared field order so row authors can omit fields without misaligning columns. Cells containing the delimiter, the quote, or a newline are quoted and inner quotes doubled. Note the target table only exists after the downstream "Generate from EDM" output is published - until then the CSVIM import is retried by its own synchronizer.

Together they cover every intent block defined today.

| Intent block | Output | Spring `@Order` |
|---|---|---|
| `entities` | `<intent>.edm` + `<intent>.model` | 200 (done) |
| `processes[]` | `<process>.bpmn` (one per process) | 300 (done) |
| `forms[]` | `<form>.form` | 400 (done) |
| `reports[]` | `<report>.report` | 500 (done) |
| `permissions` | `<intent>.roles` | 600 (done) |
| `seeds[]` | `<seed>.csvim` + `<seed>.csv` | 700 (done) |
| (future) low-level `schemas[]` | `<schema>.dsm` + `<schema>.schema` | 250 |
| (future) custom-action `.access` rules | `<intent>.access` | 650 |

All implementations are Spring `@Component` beans implementing `IntentTargetGenerator`; ordering via `@Order`. Leave gaps of 100 so future generators can slot between.

## Wiring

- **Artefact type string `intent`, file extension `.intent`, JPA table `DIRIGIBLE_INTENTS`.**
- **`SynchronizersOrder.INTENT = 5`** - lower than every other artefact (EXTENSIONPOINT = 10 is the previous floor) so the intent's regenerated model files are on disk before any other synchronizer starts the NEXT cycle.
- **`IntentSynchronizer extends BaseSynchronizer<Intent, Long>`** - single-tenant. Intent itself carries no runtime state; downstream synchronizers handle their own tenancy.
- **Module registered in:**
  - `components/pom.xml` (Maven reactor)
  - `components/group/group-engines/pom.xml` (so `build/application` picks it up via the group aggregator)

## Synchronizer flow

1. `parseImpl(location, content)` - reads the YAML bytes, persists `Intent` with the raw payload in `INTENT_CONTENT`, marks the intent dirty for `finishing()`. Structural validation is deferred to `IntentParser.parse` in the regeneration pass so that a malformed YAML body doesn't block the artefact from being recorded.
2. `completeImpl(wrapper, phase)` - pure book-keeping (CREATED / UPDATED / DELETED). No runtime side-effects.
3. `finishing()` - for every intent marked dirty this cycle, calls `IntentRegenerationService.regenerate(intent)`. Each registered `IntentTargetGenerator` writes its slice through `IntentGenerationContext.writeModelFile`. Failures in one generator are logged and isolated; the others still run.
4. **Stale-output scrub.** After the generators run, `IntentRegenerationService` deletes model-layer files at the project root that the pass did not re-emit. The extension filter keeps the scrub away from `app.intent`, code files and subfolders (`gen/`, `custom/` - only direct child resources are considered). Removing a process / form / seed from the intent therefore removes its model file on the next regeneration instead of leaving a stale `.bpmn` deployed in Flowable. `cleanupImpl` runs the same scrub with an empty keep-set when the `.intent` file itself is deleted.
5. **`forceProcessSynchronizers` is reliable now.** It used to silently no-op when the scheduled `SynchronizationJob` was mid-run (a race that flaked `IntentEngineIT` and `LocalNativeAppLifecycleIT`); it now retries until a full pass has actually consumed the force flag, bounded at five minutes. Tests can write a resource, force, and immediately assert.

### Open design question: same-cycle vs next-cycle visibility

`SynchronizationProcessor` walks the repository **once per cycle**, dispatching every file to the first synchronizer whose `isAccepted` matches. Files written **during** the cycle - including everything `IntentRegenerationService` writes - are NOT visible to other synchronizers in the same cycle. They are picked up on the **next** reconciliation.

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
        args: { if: "amount > 10000", then: cfoReview, else: end }
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

seeds:
  - name: countries
    entity: Country
    rows:
      - { id: 1, name: Afghanistan, code2: AF, code3: AFG, numeric: "004" }
      - { id: 2, name: Albania,     code2: AL, code3: ALB, numeric: "008" }
```

Logical field types (`FieldIntent.type`) are: `string`, `text`, `integer`, `decimal`, `boolean`, `date`, `uuid`. Generators map them to JDBC + EDM types. Relation kinds: `oneToMany`, `manyToOne`, `oneToOne`, `manyToMany`. Step kinds: `userTask`, `serviceTask`, `decision`, `script`, `end`.

Semantics worth knowing:

- **`required: true` on a to-one relation means composition.** The owning entity becomes DEPENDENT (managed as details under its parent's perspective) and the FK is NOT NULL. An optional to-one is an association: plain dropdown, the entity keeps its own top-level perspective. An entity's *first* required to-one is its composition parent; further required relations are NOT NULL associations.
- **Decision steps**: `if` + `then` are mandatory; `else` is optional and receives the gateway-default flow (so the conditioned branch can actually be skipped - without `else` the default falls through to the next step in the chain). `then`/`else` must name a declared step or the literal `end`; the parser validates this so a typo fails at parse time instead of producing BPMN Flowable rejects.
- **`trigger` is parsed but not consumed yet.** `BpmnIntentGenerator` logs a warning when a process declares one; wiring `onCreate`/`onSchedule` to process starts is on the follow-up list. Do not promise trigger behavior to users until that lands.
- **The YAML `name:` field is the intent's identity for outputs.** `IntentNaming.baseName` prefers it over the artefact name derived from the file name (which is conventionally just `app` from `app.intent`); single-file outputs are `<name>.edm` / `<name>.model` / `<name>.roles` and the table prefix is its upper-snake.
- **Physical table names are intent-prefixed**: `<INTENT>_<ENTITY>` upper-snake (`ORDERS_ORDER`), via `IntentNaming.tableName`, consistently across `.edm` `dataName`, `.report` `baseTable` and `.csvim` `table`. This avoids SQL reserved words (`ORDER`, `USER`, ...) and cross-project collisions in a shared schema. If the downstream "Generate from EDM" wizard asks for a table prefix, intent projects must leave it empty - the prefix is already part of `dataName`.

### YAML authoring rules

- **Comments are allowed and encouraged.** Lines starting with `#` survive a SnakeYAML load → JSON round-trip only as dropped content, so they are NOT preserved across regeneration of the intent itself - but since the intent is the only authored artefact and no tool ever rewrites it, comments authored by a developer stay put. The LLM patch path must respect the surrounding comments.
- **No anchors / aliases (`&foo` / `*foo`)** at v1. They cut copy-paste corners for the author but make the file far harder for the LLM to diff. If duplication becomes painful, introduce a top-level `defaults:` block rather than YAML's structural aliasing.
- **No multi-document YAML (`---`).** One intent file, one YAML document.
- **Tags forbidden.** Already enforced by `SafeConstructor`; mentioned here so a future reader does not "fix" it.

## Things to not do

- **Don't emit code-shaped files from any intent generator.** No `*.ts`, `*.java`, `*.html`, `*.sql`, `*.css`. Output extensions are restricted to the model layer (`.edm` / `.model` / `.bpmn` / `.form` / `.report` / `.roles` / `.access` / `.dsm` / `.schema` / `.table` / `.view` / `.csvim` / `.csv`). The existing template engine produces code; the intent layer produces models.
- **Don't make intent multi-tenant.** Authoring is single-tenant; generated artefacts handle their own tenancy.
- **Don't let intent rewrite or sort itself.** Diff stability matters - the LLM has to produce minimal patches, which only works if the on-disk shape is stable. No auto-formatting, no field reordering.
- **Write only via `IntentGenerationContext.writeModelFile` (project root).** The post-pass scrub deletes model files that were not re-emitted through `writeModelFile` - a generator that writes through `IRepository` directly would see its output deleted right after producing it. Never write into `gen/`: the model-to-code templates wipe that folder wholesale on every regeneration.
- **Don't reference template-engine output paths.** Intent generators must be ignorant of which downstream template the user will run. The `.access` constraints must not name `gen/<Entity>Controller.ts` paths; either use the EDM's own `generateDefaultRoles="true"` flag (preferred) or emit role / path tokens the template engine resolves itself.
- **Don't add a Mermaid editor.** Mermaid is for visualisation. Authoring is prompt + structured panel.
- **Don't reuse the existing modelers for intent projects.** That would re-expose `gen/` as an authoring surface and undo the whole point.
- **Don't read env vars or system properties directly** - go through `DirigibleConfig` per the platform-wide rule.

## Follow-ups

- `.access` rules from intent. The current PermissionIntentGenerator deliberately emits only `.roles`; URL-shaped constraints (the `<path, method, roles>` table in `.access`) need to know the paths the downstream template engine will publish, so they live with that template. A future pass should either (a) wire intent to feed those paths into the EDM template generator so it can emit the matching `.access`, or (b) add a custom-action `.access` block to intent for non-CRUD operations like {@code Order:approve} where there is no template-owned URL.
- Lower-priority model-layer generators: DSM / schema / table / view / csvim / csv. The EDM-only entry already covers the same surface implicitly, so these are optional refinements rather than gaps.
- Trigger the "Generate from EDM" template programmatically on intent change so the developer sees the full app, not just the model files. Today the user has to open the EDM editor and click Generate manually. **The hook is the `.gen` descriptor** that real-world codbex application projects keep next to each model file (`<model>.gen` beside `<model>.model`, one `<form>.gen` per form): it records `templateId`, `filePath`, `genFolderName`, `tablePrefix`, `dataSource` and the perspective layout - exactly the parameters the IDE generation service replays. A future `GenDescriptorIntentGenerator` could emit these with `"tablePrefix": ""` baked in (the prefix already lives in the intent-prefixed `dataName`), making model-to-code generation one click or fully automatic.
- Reference layout: production codbex application projects are the canonical real-world shape this engine generates towards - model files (`<app>.edm`/`.model`, `<process>.bpmn`, `*.form`, `*.report`) at the project root, template output under `gen/`, hand-written BPMN service-task handlers under `tasks/` (our `custom/` concept), generated translation skeletons under `translations/`. Their `.form` files match the FormIntentGenerator output shape key-for-key (`metadata`/`feeds`/`scripts`/`code`/`form`).
- Translation skeletons (`translations/<locale>/<form>.form.json`, `<model>.model.json`) as an additional intent generator, mirroring the production project layout.
- Claude chat + patch-preview in the perspective. Needs a separate LLM bridge module (Anthropic API key via `DirigibleConfig`, request shaping, structured-patch responses, accept / reject flow). Out of scope for this PR.
- Read-only Monaco markers for intent-generated model files so the IDE marks them not-for-editing.
- `/custom/` escape-hatch directory + per-slice hook points in the generators (the generators must learn to preserve `/custom/` files alongside their gen output).
- `reverse-engineer intent` command for migrating classic projects.
- Same-cycle visibility (open design question above).
- Wire the `trigger` block: `onCreate: <Entity>` should start the process when the entity is created (listener or interceptor on the generated persistence layer), `onSchedule` should map to a timer start event or a `.job` artefact.

**Done:**

- Structural validation on parse: duplicate names, dangling relation / form / report / seed targets, unknown field / relation / step kinds, multi-PK and empty-seed checks. Surfaced via `IntentValidationException` with the complete list of issues in one error message.
- All six v1 model-layer generators: `EdmIntentGenerator` (entities -> .edm + .model), `BpmnIntentGenerator` (processes -> .bpmn), `FormIntentGenerator` (forms -> .form), `ReportIntentGenerator` (reports -> .report), `PermissionIntentGenerator` (permissions -> .roles), `CsvimIntentGenerator` (seeds -> .csvim + .csv).
- Integration test [`IntentEngineIT`](../../../tests/tests-integrations/src/main/java/org/eclipse/dirigible/integration/tests/api/IntentEngineIT.java) covering stage one of the Orders pipeline - intent -> persisted artefact -> generated model files (five entities, composition + association relations, process with every step kind incl. a meaningful then/else decision, two forms, report, roles, seeds), the stale-output scrub on regeneration, model-file cleanup on intent removal, and the REST endpoints. HTTP-only, no Selenide. It asserts the *shape* of the model files; pushing them through the downstream synchronizers/templates (stage two) is not covered yet.
- Stale-output scrub + gen cleanup on intent deletion (the flat-gen-root ownership contract above).
- Reliable `forceProcessSynchronizers` (bounded wait instead of silent skip) in `core-initializers`.
- Mermaid served as a webjar instead of from a CDN.
