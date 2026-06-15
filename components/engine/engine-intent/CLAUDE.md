# engine-intent

A single `.intent` YAML file at a project root becomes the source of truth for every other authoring artefact in that project. The intent layer is **one altitude above** the existing model files: where Dirigible used to have hand-authored `.edm` / `.bpmn` / `.form` / `.report` / `.roles` / `.access` / `.csvim` and code-gen them to TS / HTML / Java / SQL on demand, the intent layer authors the **`.edm` / `.bpmn` / ... model files themselves** from one YAML.

The whole feature lives in `org.eclipse.dirigible.components.intent.*`.

## Editor-first architecture - **read this first**

**The intent is an authoring artifact, not a runtime artifact.** The platform draws this line
sharply and it dictates the whole design: authoring artifacts (`.edm`, `.model`, `.form`,
`.report`) get **editors in the workspace plus an explicit Generate step**; only runtime artifacts
(`.roles`, `.bpmn`, `.csvim`, `.table`, jobs, listeners, ...) are reconciled from the registry by
synchronizers. The `.edm` is the precedent: it has NO synchronizer - and neither does the intent.

The developer workflow (the only flow):

```
1. create a project in your workspace
2. create app.intent (any *.intent) at the project root, authored by hand / Claude / structured panel
3. double-click → the Intent Editor opens: editable YAML text left, live read-only diagram right
   (ER + one flowchart per process + forms/reports/roles/seeds summaries), validation issues inline
4. click Generate → the six generators write the derived model files NEXT TO app.intent,
   IN YOUR WORKSPACE PROJECT (nothing is published):
       <intent>.edm + <intent>.model     ← entities + relations + UI metadata
       <process>.bpmn                    ← processes
       <form>.form                       ← forms
       <report>.report                   ← reports
       <intent>.roles                    ← permissions
       <seed>.csvim + <seed>.csv         ← seed data
5. (follow-up: Generate also chains the model-to-code templates via .gen descriptors)
6. publish → the registry receives intent + models + generated code together;
   the per-artefact synchronizers bring the runtime live as for any other project
```

**Why the project root and not `gen/`:** the model-to-code templates ("Generate from EDM") treat
`gen/` as their exclusive output folder and **wipe it on every regeneration** - intent output placed
there would be destroyed the first time the user generates the application code. The project root is
where every real-world codbex application keeps its model files. The folders layer as: `app.intent`
(authored) + root model files (intent-owned, scrubbed by this engine's Generate) → `gen/`
(template-owned, wiped by the template engine) → `custom/` (hand-written escape hatch, touched by
nobody).

The published `app.intent` in the registry is an inert source file (exactly like a published
`.edm`). Consequence to document, not fix: an intent-only project deployed headlessly (git →
registry) does not self-materialize - the generated models are committed/published artifacts,
produced in the workspace.

Intent generators stop at the **model file**. They never emit `Entity.ts`, `Controller.ts`, `Repository.ts`, HTML, Java, or SQL directly - those come from the IDE's existing "Generate from EDM / Schema / BPMN" templates, fed by the model files this engine wrote. That contract is non-negotiable; see "Wrong turns we already made" below.

## Design context (what we agreed)

Distilled from the chat that produced the initial scaffold. Read this BEFORE designing additional pieces - half of these decisions are not obvious from the code.

### Why this exists

Dirigible is already model-driven (the synchronizer model = "declarations on disk → running app"). Adding an intent layer above EDM/BPMN/form/DSM is the natural next abstraction. The second half of the pipeline (intent → standard models → generated app) reuses what already exists: project templates, decorator-driven scaffolding, the TS/Java SDKs. No new runtime concept - just a new authoring layer above the existing ones.

The dream is "no code, no modelling - just prompt": user describes what they want in natural language to Claude (or any LLM); Claude proposes a patch to `.intent`; the user accepts in the Intent Editor; Generate refreshes the model files at the project root, and the template engine turns them into the app under `gen/`; the editor's diagram pane renders the intent for a quick read-only visual at every step.

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

4. **An IntentSynchronizer + JPA artefact for the intent itself.** The first incarnation treated the intent as a runtime artifact: a `BaseSynchronizer` parsed published `.intent` files into a `DIRIGIBLE_INTENTS` table and regenerated the models **in the registry** during reconciliation. Two unfixable consequences: generation happened only after publish (invisible in the Projects view, unusable by the modelers and by "Generate from EDM", which all work on the workspace), and the UI could only be a registry-reading perspective instead of an editor. The platform's own line is: **authoring artifacts (`.edm`, `.form`, `.report`, `.intent`) get editors + an explicit Generate in the workspace; only runtime artifacts get synchronizers** - note the `.edm` has no synchronizer either. The synchronizer, the JPA artefact, and the Mermaid perspective were all removed in favour of `editor-intent` + the parse/generate endpoints. Do not reintroduce them; this separation is hard-won low-code-platform experience.

The general rule the first two violated: **intent generators must never reference paths or routes that belong to the template engine's output**, because the intent layer should be agnostic about which template is selected.

## Module layout

```
components/engine/engine-intent/                   # backend: parser + generators + REST services
├── pom.xml                                # depends on core-base, core-database, core-repository, ide-workspace
├── about.html
├── CLAUDE.md                              # this file
└── src/main/java/org/eclipse/dirigible/components/intent/
    ├── model/                             # POJOs for the intent document (plain-Gson-mapped after YAML → Map → JSON round-trip)
    │   ├── IntentModel.java               # root: name / entities / processes / forms / reports / permissions / seeds
    │   ├── EntityIntent.java / FieldIntent.java / RelationIntent.java
    │   ├── ProcessIntent.java / StepIntent.java
    │   └── FormIntent.java / ReportIntent.java / PermissionIntent.java / SeedIntent.java
    ├── parser/
    │   ├── IntentParser.java              # YAML → Map (SnakeYAML SafeConstructor) → JSON → IntentModel (plain Gson) + structural validation
    │   └── IntentValidationException.java # collects every structural issue in one shot
    ├── generator/
    │   ├── IntentTargetGenerator.java     # SPI - one per slice (entities, processes, forms, ...)
    │   ├── IntentGenerationContext.java   # parsed model + target project paths; writeModelFile() is the only write surface
    │   ├── IntentNaming.java              # shared naming: baseName, upperSnake, tableName (<INTENT>_<ENTITY>)
    │   ├── IntentGenerationService.java   # runs the SPI beans in @Order, scrubs stale output, returns written/scrubbed
    │   ├── edm/EdmIntentGenerator.java                # @Order(200); <intent>.edm (XML) + <intent>.model (JSON)
    │   ├── bpmn/BpmnIntentGenerator.java              # @Order(300); <process>.bpmn per process
    │   ├── form/FormIntentGenerator.java              # @Order(400); <form>.form per form
    │   ├── report/ReportIntentGenerator.java          # @Order(500); <report>.report per report
    │   ├── permission/PermissionIntentGenerator.java  # @Order(600); <intent>.roles
    │   └── csvim/CsvimIntentGenerator.java            # @Order(700); <seed>.csvim + <seed>.csv per seed
    └── endpoint/IntentEndpoint.java       # POST /services/ide/intent/parse + /generate (workspace-targeted)
```

The editor lives in one sibling UI module:

- `components/ui/editor-intent` - registered for content type `application/yaml+intent` (the `intent` extension is mapped in `ContentTypeHelper`) via the `platform-editors` extension point, like every other specialized editor. Split layout: an embedded Monaco editor (YAML highlighting, theme-synced to the IDE via `ThemingHub`, loaded from the `/webjars/monaco-editor/...` webjar that `editor-monaco` already ships) left (Save, ctrl+s, dirty tracking via `LayoutHub`; `$scope.text` stays the single source the parse/save/diagram code reads, kept in sync from Monaco's change event), live diagram right (an mxGraph ER-style diagram for the entities + one top-down mxGraph flowchart per process + forms/reports/roles/seeds summaries), validation issues as an inline strip fed by `POST /parse` on a 600ms debounce. The Generate button calls `POST /generate` and refreshes the project tree via the `projects.tree.refresh` dialog-hub topic (the same mechanism the form-builder uses). The diagram uses **mxGraph 4.2.2** (the same engine the EDM / schema / mapping modelers use), depended on as `dirigible-components-resources-mxgraph` and loaded from `/services/web/resources/mxgraph/4.2.2/src/js/mxClient.js` with `mxBasePath` set in the editor HTML.

### Intent Editor diagram = mxGraph, fixed-colour palette (read before touching `editor-intent/js/editor.js`)

The diagram pane was Mermaid through mid-2026 and had two unfixable-in-practice theming defects (invisible connector lines in dark mode; a "Syntax error in text" bomb on every light↔dark switch). It was **rewritten on mxGraph** - the rendering engine Dirigible already trusts in the EDM/schema/mapping modelers - which removed both defects by construction. Do not reintroduce Mermaid.

**How it renders.** `render()` tears down any live graphs, then builds one read-only `mxGraph` per section into a freshly created container appended to `#intent-diagrams`:
- `renderEntities()` - one blue HTML-label card per entity (name over its field list, PK marked); a solid edge for a required relation, dashed for an optional one. Laid out left-to-right with `mxHierarchicalLayout(graph, mxConstants.DIRECTION_WEST)` - the same layout the processes use; the organic layout was tried first but collapsed every card onto the origin because they all start at (0,0).
- `renderProcess()` - slate start/end ellipses, blue user-task / green service-task rounded rects, amber decision rhombus; decision steps emit a conditioned edge (label = `if`) to `then` and a default edge to `else` (falling back to the next step), exactly like `BpmnIntentGenerator`. Laid out top-down with `mxHierarchicalLayout(graph, mxConstants.DIRECTION_NORTH)`.
Each graph is `setEnabled(false)` (read-only, no selection/editing), HTML labels on, and sized to its content (`sizeToContent` → the pane scrolls). The forms/reports/roles/seeds summaries stay as Angular-bound HTML below the diagrams.

**Why theming is now a non-issue (the whole point of the rewrite).** The cells use **fixed brand colours** (`COLOR` map at the top of the controller: entity blue `#3584e4`, service green `#26a269`, decision amber `#e9a319`, terminal slate `#708090`, edge mid-gray `#7a8896`, white labels) chosen to read on **both** the light and dark IDE themes, painted on a transparent canvas whose background is the theme's. So the diagram looks identical in either theme and there is **no recolour-on-theme-switch step to break** - the controller deliberately has no `ThemingHub` listener for the diagram. This matches the schema/entity modelers' approach (solid-coloured shapes, white text) and is what the user asked for. If you ever need a colour to follow the theme foreground instead, resolve the CSS var to a concrete `rgb()` in JS first (mxGraph writes `stroke`/`fill` as raw SVG presentation attributes via `mxSvgCanvas2D`, and `var(...)` in an SVG attribute is not reliably resolved) - do **not** put `var(--…)` into an mxGraph style string for stroke/fill and assume it tracks the theme.

**Stale-jar trap (still the most likely reason "nothing changed").** `editor-intent` is static web resources **bundled into the `build/application` fat jar**. `mvn install -pl components/ui/editor-intent` updates the `.m2` artifact but NOT an already-built `dirigible-application-*-executable.jar`. After editing, `mvn -P quick-build install -pl components/ui/editor-intent` **and** `mvn -P quick-build package -pl build/application`, then restart. Verify the running jar is current: `curl -s -u admin:admin http://localhost:8080/services/web/editor-intent/js/editor.js | grep -c mxGraph` (expect a non-zero count; `mermaid` should be gone). `IntentEditorLoadsIT` does not have this problem (failsafe repackages independently), which is why a green IT never proves the user's jar is current.

`IntentEditorLoadsIT` now asserts the mxGraph diagram renders (`.intent-diagram svg` is visible) **and** that the parsed `Book` entity's label appears inside `.intent-diagram` - so it fails on an empty or broken diagram, unlike the old "any `<svg>` exists" check that a Mermaid error bomb satisfied. It does not separately exercise a theme switch because the fixed-colour palette renders identically in both themes.

### General build/serve gotcha (applies to all UI modules)

Web resources under `components/ui/*` and `components/.../resources` are bundled into the `build/application` fat jar. After editing them: `mvn -P quick-build install -pl <module>` then `mvn -P quick-build package -pl build/application`, and restart the running jar. A locally running instance does NOT hot-reload changed module resources. Integration tests repackage `tests-integrations` and are unaffected - so a passing IT never proves the user's running jar is current.

Six concrete generators currently live in-module:

- [`EdmIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/edm/EdmIntentGenerator.java) writes `<intent>.edm` (XML) plus `<intent>.model` (JSON twin) from the entities + relations declared in the intent. Each entity is fleshed out with EDM editor defaults (icons, menu keys, layout type, perspective metadata, widget types) derived from the entity / field names so the produced model is a complete, openable EDM document. Conventions mirrored from the real codbex apps (which are written by the Dirigible authors themselves, so they *are* the canonical Dirigible conventions - `codbex-invoices`, `codbex-uoms` are good references): **property `name`s are PascalCase** (`id`->`Id`, `loanedOn`->`LoanedOn`, FK `member`->`Member`) via `IntentNaming.pascalCase`, while the physical column **`dataName` stays UPPER_SNAKE** and intent-prefixed (`ORDERS_COUNTRY`) - authoring stays lower camelCase, only the generated model names are PascalCased; every property carries **`auditType="NONE"`**, and a required field/FK also carries **`isRequiredProperty="true"`** (the generated REST controller's required-value validation keys on it, not on `dataNullable`). **Every to-one FK property carries the full relationship metadata the generation reads** (the `.model` has no separate relations array, so it must live on the property): `relationshipType` (`COMPOSITION` for `composition: true`, else `ASSOCIATION`), `relationshipCardinality` (`1_n` composition, `n_1` manyToOne association, `1_1` oneToOne association), `relationshipName` = the FK constraint name `<owner>_<target>` (e.g. `Loan_Member` - used as the DB FK constraint name in the schema template), `relationshipEntityName` = target entity, `relationshipEntityPerspectiveName` = target's resolved perspective, `relationshipEntityPerspectiveLabel="Entities"`. **The dropdown's data-service URL (`api/<perspective>/<entity>Service.ts`) and the create-detail dialog are built from `relationshipEntityName` + `relationshipEntityPerspectiveName`** - omitting them generated `api/undefined/undefinedController.ts` and a dead dropdown. A `composition: true` relation additionally makes the owner DEPENDENT/MANAGE_DETAILS, inheriting its transitively-resolved parent perspective; every other to-one stays a PRIMARY association DROPDOWN. Dropdown key/value and `referencedProperty` come from the target entity's actual PK and `name`-like fields (PascalCased); the `.model` JSON carries `entities`/`perspectives`/`navigations` (no `relations` key - relations are XML-only, interleaved with their owning `<entity>`). The `.edm` also carries an **`mxGraphModel`** diagram with a `style="entity"` vertex per entity (carrying an `<Entity>` value), a child vertex per property (carrying a `<Property>` value), and an edge per FK relation - the EDM editor renders the canvas *exclusively* by decoding `mxGraphModel`, so without it the editor opens empty. Entities are placed in a fixed grid for deterministic output.
- [`BpmnIntentGenerator`](src/main/java/org/eclipse/dirigible/components/intent/generator/bpmn/BpmnIntentGenerator.java) writes one `<process>.bpmn` per process. Minimal Flowable-flavoured BPMN 2.0 - one start event, one end event, the declared steps, and the sequence flows that connect them. Decisions emit an exclusiveGateway with a conditioned outgoing flow to `args.then` and a default flow to `args.else` (falling back to the next step in the chain when omitted). The `trigger` block is parsed but not consumed yet - a warning is logged. Emits the **`bpmndi:BPMNDiagram`** block (plus the `omgdc`/`omgdi` namespaces): the Flowable/Oryx modeler renders the canvas *only* from the diagram interchange - a process with no `BPMNShape`s opens empty. Nodes are laid out left-to-right along the linear chain at a fixed lane; edges connect source-right to target-left. The layout is deterministic (byte-stable across regenerations); the modeler re-routes on first manual edit.
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

- **No artefact type, no JPA table, no synchronizer.** The intent never touches the database; the `.intent` file in the workspace is the single source of truth.
- **Content type `application/yaml+intent`** mapped from the `intent` extension in `ContentTypeHelper` (`modules/commons/commons-helpers`) - this is what routes a double-click to the Intent Editor.
- **Module registered in:** `components/pom.xml` (both `engine/engine-intent` and `ui/editor-intent`), `components/group/group-engines/pom.xml` (engine), `components/group/group-ide/pom.xml` (editor).

## Services flow

1. `POST /services/ide/intent/parse` (body: raw YAML) - `IntentParser.parse` → `IntentModel` JSON, or `422 {"issues": [...]}` with every structural problem at once. The editor calls this on a debounce to refresh the diagram and the validation strip; nothing is persisted.
2. `POST /services/ide/intent/generate?workspace=&project=&path=` - resolves the current user's workspace project via `WorkspaceService` (so it is inherently user-scoped), reads the intent file, runs every registered `IntentTargetGenerator` (failures per generator are logged and isolated), then scrubs stale intent-owned files. Returns `{"written": [...], "scrubbed": [...]}`.
3. **Stale-output scrub.** Model-layer files at the project root that the pass did not re-emit are deleted. The extension filter keeps the scrub away from the `.intent` file itself, code files, and subfolders (`gen/`, `custom/` - only direct child resources are considered). Removing a process / form / seed from the intent removes its model file on the next Generate.
4. **Generation is idempotent and diff-stable** - identical input produces byte-identical output, and byte-identical content is not rewritten.

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
      - { name: id,      type: integer, primaryKey: true, generated: true }
      - { name: name,    type: string,  required: true, length: 200 }
      - { name: country, type: string,  length: 2 }
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

Logical field types (`FieldIntent.type`) are: `string`, `text`, `integer`, `int`, `long`, `decimal`, `double`, `boolean`, `date`, `timestamp`, `uuid`. Generators map them to JDBC + EDM types. **Primary keys must be an integer type (`integer`/`int`/`long`)** - the codbex model convention is integer auto-increment identifiers, and a non-integer auto-increment column is invalid SQL (a `uuid`/`VARCHAR` PK produced `AUTO_INCREMENT` on a `VARCHAR(36)` column, which H2 rejects); the parser enforces this and the EDM generator only emits `dataAutoIncrement` for integer columns. `uuid` remains valid for non-PK fields (maps to `VARCHAR(36)`). Relation kinds: `oneToMany`, `manyToOne`, `oneToOne`, `manyToMany`. Step kinds: `userTask`, `serviceTask`, `decision`, `script`, `end`.

Semantics worth knowing:

- **`composition: true` on a to-one relation makes it a composition.** The owning entity becomes DEPENDENT (managed as details under its parent's perspective) and the FK is NOT NULL. `required: true` *alone* only makes the FK NOT NULL - the entity stays a top-level PRIMARY association (plain dropdown, its own perspective). Composition is **opt-in**, matching codbex (where it is an explicit `relationshipType="COMPOSITION"` and most required FKs are plain associations); `composition` already implies NOT NULL, so `required` need not also be set. Only a `manyToOne`/`oneToOne` can be a composition; an entity's *first* `composition` to-one is its composition parent. Declare the inverse `oneToMany` on the master (`Member` with `loans: oneToMany to Loan` + `Loan.member` `composition: true`) so `Loan` is managed as a detail of `Member`; the `oneToMany` itself is navigation-only (the EDM generator ignores `oneToMany`/`manyToMany` since the FK lives on the child). (This replaced the earlier "first required to-one is automatically a composition" heuristic, which made entities like a `Loan` with a required `member` FK silently nest under `Member` instead of staying top-level.) **Every to-one FK property** (composition or association) carries `relationshipType` / `relationshipCardinality` (`1_n` / `n_1` / `1_1`) / `relationshipName` (`<owner>_<target>`) / `relationshipEntityName` / `relationshipEntityPerspectiveName` - the last two drive the generated dropdown's data URL, so they are not optional.
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
- **Don't add a Mermaid round-trip authoring editor.** Mermaid is read-only visualisation (it lives in the right pane of the Intent Editor). Authoring is the YAML text pane + prompt + structured panel.
- **Diagram editors render only from their layout blocks - always emit them.** The EDM editor decodes `mxGraphModel`, the BPMN modeler decodes `bpmndi:BPMNDiagram`; both open to an empty canvas if the block is missing (this shipped once - the generators wrongly assumed the editors auto-lay-out on open). Any generator whose target opens in a visual modeler must emit a deterministic layout, not just the logical model.
- **Don't reuse the existing modelers for intent projects.** That would re-expose `gen/` as an authoring surface and undo the whole point.
- **Don't read env vars or system properties directly** - go through `DirigibleConfig` per the platform-wide rule.

## Follow-ups

- `.access` rules from intent. The current PermissionIntentGenerator deliberately emits only `.roles`; URL-shaped constraints (the `<path, method, roles>` table in `.access`) need to know the paths the downstream template engine will publish, so they live with that template. A future pass should either (a) wire intent to feed those paths into the EDM template generator so it can emit the matching `.access`, or (b) add a custom-action `.access` block to intent for non-CRUD operations like {@code Order:approve} where there is no template-owned URL.
- Lower-priority model-layer generators: DSM / schema / table / view / csvim / csv. The EDM-only entry already covers the same surface implicitly, so these are optional refinements rather than gaps.
- Chain the model-to-code templates from the editor's Generate button so the developer sees the full app, not just the model files. Today the user opens the generated `.edm`/`.form` and clicks Generate there. **The hook is the `.gen` descriptor** that real-world codbex application projects keep next to each model file (`<model>.gen` beside `<model>.model`, one `<form>.gen` per form): it records `templateId`, `filePath`, `genFolderName`, `tablePrefix`, `dataSource` and the perspective layout - exactly the parameters the IDE generation service replays. A future `GenDescriptorIntentGenerator` could emit these with `"tablePrefix": ""` baked in (the prefix already lives in the intent-prefixed `dataName`), making the editor's Generate chain straight into `GenerateService.generateFromModel(...)` - the exact mechanism the form-builder's Regenerate button already uses (see `editor-form-builder/js/editor.js`).
- Reference layout: production codbex application projects are the canonical real-world shape this engine generates towards - model files (`<app>.edm`/`.model`, `<process>.bpmn`, `*.form`, `*.report`) at the project root, template output under `gen/`, hand-written BPMN service-task handlers under `tasks/` (our `custom/` concept), generated translation skeletons under `translations/`. Their `.form` files match the FormIntentGenerator output shape key-for-key (`metadata`/`feeds`/`scripts`/`code`/`form`).
- Translation skeletons (`translations/<locale>/<form>.form.json`, `<model>.model.json`) as an additional intent generator, mirroring the production project layout.
- Claude chat + patch-preview as a third pane (or dialog) of the Intent Editor. Needs a separate LLM bridge module (Anthropic API key via `DirigibleConfig`, request shaping, structured-patch responses, accept / reject flow). Out of scope for this PR.
- Mark intent-generated model files as not-for-hand-editing in the IDE (decoration in the Projects view or a banner in the modelers).
- `/custom/` escape-hatch directory + per-slice hook points in the generators (the generators must learn to preserve `/custom/` files alongside their gen output).
- `reverse-engineer intent` command for migrating classic projects.
- Wire the `trigger` block: `onCreate: <Entity>` should start the process when the entity is created (listener or interceptor on the generated persistence layer), `onSchedule` should map to a timer start event or a `.job` artefact.

**Done:**

- Editor-first architecture: `editor-intent` (split text + live diagram, Save, Generate) registered for `*.intent`; `POST /parse` + workspace-targeted `POST /generate`; no synchronizer, no JPA artefact, nothing in the registry until publish.
- Structural validation on parse: duplicate names, dangling relation / form / report / seed targets, unknown field / relation / step kinds, decision then/else target checks, multi-PK and empty-seed checks. Surfaced via `IntentValidationException` with the complete list of issues in one error message; the editor shows them inline.
- All six v1 model-layer generators: `EdmIntentGenerator` (entities -> .edm + .model), `BpmnIntentGenerator` (processes -> .bpmn), `FormIntentGenerator` (forms -> .form), `ReportIntentGenerator` (reports -> .report), `PermissionIntentGenerator` (permissions -> .roles), `CsvimIntentGenerator` (seeds -> .csvim + .csv).
- Integration test [`IntentEngineIT`](../../../tests/tests-integrations/src/main/java/org/eclipse/dirigible/integration/tests/api/IntentEngineIT.java): parse (full model + all-issues-at-once validation), generate into the workspace project with content assertions on every artefact, the stale-output scrub, and 422 on invalid intents. HTTP-only, no Selenide, no synchronization cycles - the whole class runs in under a minute.
- Stale-output scrub on regeneration (the project-root ownership contract above).
- Reliable `forceProcessSynchronizers` (bounded wait instead of silent skip) in `core-initializers` - kept even though intent no longer uses it; it fixes a whole class of IT flakes.
- Diagram pane rendered with mxGraph 4.2.2 (replacing Mermaid) using a fixed brand-colour palette that reads on both themes - this resolved the dark-mode invisible-lines and theme-switch "Syntax error" defects by construction.
- Source pane is an embedded Monaco editor with YAML highlighting (replacing the plain textarea), theme-synced to the IDE via `ThemingHub`, reusing the `/webjars/monaco-editor/...` webjar `editor-monaco` already ships.
- Integer-only primary keys: the parser rejects a non-integer PK (`integer`/`int`/`long`) and the EDM generator only emits `dataAutoIncrement` for integer columns - fixes the `AUTO_INCREMENT` on a `VARCHAR(36)` uuid PK that H2 rejected during full-stack table creation.
- Opt-in composition: `composition: true` (not "first required to-one") marks a master-detail; `required` alone is just a NOT NULL FK association.
- EDM output aligned to the codbex/Dirigible conventions: PascalCase property names (columns stay UPPER_SNAKE), `auditType="NONE"` and `isRequiredProperty` on every property, and **full relationship metadata on every FK property** (`relationshipType`/`Cardinality`/`Name` + `relationshipEntityName`/`relationshipEntityPerspectiveName`/`relationshipEntityPerspectiveLabel`) - the last two are what the generated UI uses to build the FK dropdown's data-service URL; without them the dropdown loaded `api/undefined/undefinedController.ts` and failed.
- REST template fix (`template-application-rest-v2`): the max-length validation is now guarded by `$property.dataLength`, so a CLOB/text property (no length) no longer leaks a literal `${property.dataLength}` into the generated controller. The Java REST template already had the guard.

**Known follow-up (not done):** the `.form` / `.report` generators still emit the authored lower-camelCase field names, while the EDM now exposes PascalCase property names - if cross-artefact field-name consistency is wanted, extend `IntentNaming.pascalCase` to `FormIntentGenerator` / `ReportIntentGenerator` too.
