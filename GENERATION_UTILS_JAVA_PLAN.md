# Plan: Migrate the Generation Utils to Java

**Status:** assessed 2026-08-09 against master (post-14.17.0). **PR 1 implemented** on
`feat/generation-utils-java` — the Java utils + the parity harness, both paths live, no consumer
switched. PRs 2 and 3 not started. See §6 for what PR 1 actually landed and the two plan
assumptions it corrected.

**Goal:** the template-generation pipeline (model → generated files) executes entirely in Java. The
JS "generation utils" — `generateUtils.js`, `parameterUtils.js`, the `generate.mjs` endpoint, and
the `templates.js` list service — are ported; the GraalJS boot + per-source-file JSON serialization
disappears from the Generate path; the intent engine executes its `codeGenerations` plan
**in-process** instead of returning it for client-side replay.

Every claim below carries a `file:line` verified on 2026-08-09. Line numbers will drift — re-verify
with grep before each edit.

---

## 0. What exists today (the map)

### 0.1 The JS side being migrated (~2,850 lines of logic)

| File | Lines | Role |
|---|---|---|
| `components/ui/service-generate/.../service-generate/template/generateUtils.js` | 1,883 | The engine-room. Exports exactly 2 functions: `generateGeneric(model, parameters, templateSources)` (`:132-185`, the `.form`/`.report` path) and `generateFiles(model, parameters, templateSources)` (`:223-1669` — **1,447 lines, 77% of the file**, the entity-model path). |
| `components/ui/service-generate/.../service-generate/template/parameterUtils.js` | 649 | 5 exports: `humanizeName` (`:23`), `process(model, parameters)` (`:54-490`, 437 lines — the per-entity/per-property derivation pass), `getUniqueParameters` (`:492`), `parseDataTypes` (`:508`, SQL type → `{java, ts, javaClass}` lookup), `resolveJavaClass` (`:626`), `sanitizeJavaIdentifier` (`:640`). |
| `components/ui/service-generate/.../service-generate/generate.mjs` | 227 | The HTTP endpoint: `POST /services/js/service-generate/generate.mjs/model/{workspace}/{project}?path=…`, body `{template, parameters}`. Flow: dynamic-import the template module (`:5-12`) → `getModel` + `augmentWithExtensions` (`:115-186`, the **cross-project EXTENSION-entity collection**) → stamp `projectName/workspaceName/filePath/templateId/fileName/genFolderName` (`:69-78`) → `template.generate(model, parameters)` → derive the touched `gen/<sub>` folders from output paths and clean **only those** (`:87-99`) → write files → write the `<fileName>.gen` audit descriptor (`:105`) → `lifecycle.publish` (`:107`) → 201. |
| `components/ui/service-template/.../service-template/api/templates.js` | 89 | Enumerates the `platform-templates` extension point (27 `template.extension` registrations under `components/template/`), `await import`s each module, calls `getTemplate()`, serves `GET ""` / `GET "extensions"` / `GET "menu"` sorted by order/label/name. HTTP path hard-coded in the AngularJS provider `service-template/templates.js:13`. ⚠️ A near-duplicate parameterized enumerator exists at `platform-core/.../extension-services/templates.js` with **no in-repo callers** — likely dead; resolve during the port. |

### 0.2 What is ALREADY Java (do not rebuild)

- **Rendering engines** — `components/engine/engine-template*` (931 lines): `TemplateEngine` +
  `TemplateEnginesManager`, `Velocity/Mustache/JavascriptGenerationEngine`. The JS utils only reach
  them through the `@aerokit/sdk/template` TS facade → `TemplateEnginesFacade`
  (`components/api/api-template`, 181 lines). **The port removes the JS→Java JSON round-trip per
  render call (`engines.ts:74-76` serializes the whole parameter graph per source file), not the
  engines.**
- **`components/ide/ide-template`** (1,204 lines) — the strongest precedent and probable host:
  - `GenerationEndpoint.java:69` — `POST /services/ide/generate/file/{workspace}/{project}/{*path}`
    (the file-from-template path already served by Java).
  - `GenerationService.java` — template resolution registry-then-classpath (`:129-160`),
    `ACTION_COPY`/`ACTION_GENERATE` (`:58,61`), Mustache default (`:64`), collection iteration
    (`generateWithTemplateIterable:191+`), workspace writes, `publisherService.publish` (`:126`),
    engine dispatch (`generateContent:324-352`).
  - **`getTemplateMetadata:168-186` already boots a `DirigibleJavascriptCodeRunner`, runs a JS
    template module's `getTemplate(parameters)`, and Gson-deserializes it into
    `GenerationTemplateMetadata`.** The Java side can read the JS template descriptors today.
  - POJOs mirroring the descriptor shape: `GenerationTemplateMetadata(Source|Parameter)`,
    `GenerationTemplateParameters`, `JavaNames`.
  - **Gaps:** no `translate` action in `GenerationTemplateMetadataSource` (fields
    `location/action/rename/start/end/collection/type/engine/handler`), and
    `generateWithTemplateIterable` has none of the entity partitions or glue loops (§0.3).
- **`engine-intent`'s Java generators** write model files (`.model`/`.form`/`.bpmn`/…) in Java
  already; `IntentNaming.humanize/pluralize` and `JavaNames` are the canonical implementations that
  `parameterUtils.humanizeName` (`:16-20`) and `generateUtils.pluralizeLabel` (`:24`) explicitly
  mirror. The port collapses the mirrors into the originals.

### 0.3 The semantics that MUST survive the port (parity checklist)

`generateFiles` (`generateUtils.js:223-1669`):

1. **Actions are `copy` / `generate` / `translate` — there is NO `append`.** `translate` is
   JS-only today (`:1568-1666`): parses the source `.template` as JSON, merges entity singular +
   `_plural` labels (keyed by `dataName`, via `humanizeName`/`pluralizeLabel` with
   `PLURALIZE_OVERRIDES :25`), property labels, perspectives, navigations, dashboard widgets,
   `customActionLabels` → `translations.actions`, `processTaskLabels` → `translations.processes`;
   output path hard-coded `i18n/en-US/<modelfile>.json`, whole catalog nested under
   `parameters.tprefix`.
2. **Pre-pass before any rendering** (`:233-325`): `mergeExtensionEntities` (`:192-221` — folds
   `type === "EXTENSION"` entities' properties into the base named by `extensionReferencedEntity`,
   skipping PK/audit/name-colliding props with base-wins, then filters the EXTENSION entities out);
   `annotateDocumentModels` (`:238`, documentMaster/documentItem back-links);
   `annotateGuardedRollups` (`:239`); `entityLabel` default via `humanizeName`; then **~28
   pre-computed entity partitions** (models, apiModels, daoModels, uiManage/List/Master/Details/
   Document/Calendar/Slots/Setting, personal*/partner* families, report families —
   `reportFilterModels` are grafted onto their report at `:255-265`).
3. **The switch has 47 cases** (`:345-1554`) in two families:
   - **23 entity-shaped cases** delegating to `generateCollection` (`:1671`) — which applies the
     SETTING/REPORT perspective overrides (`:1677-1687`), resolves `perspectiveViews` (`:1691`),
     appends `-details` views for master layouts (`:1692-1694`), and `cleanData`s before rendering.
   - **22 intent-glue cases**, each a bespoke hand-written loop + parameter object: `uiNavigations
     :473`, `triggers :493`, `resolvers :550`, `fieldLoaders :582`, `timerLoaders :609`, `waits
     :637`, `aborts :669`, `setters :694`, `writers :723`, `notifications :751`, `schedules :808`,
     `integrations :898`, `inbound :923`, `rollups :946`, `expansions :997`, `settlements :1040`,
     `generates :1081`, `transitions :1129`, `sends :1195`, `posts :1254`, `aggregates :1296`,
     `postings :1365`, `printFeeders :1418`, `snapshots :1489`, `numbering :1528`.
   - `adminModel :423-466` is one render per *model* (not per entity) building an
     `adminEntitiesJson` blob; `default :1555` renders once with `parameters.models = entities`.
4. **Real logic (not marshalling) lives in exactly 4 places:** `rollups` coalesces by
   `(childEntity, fkProperty, topicSuffix)` (`:955-968`) and emits Java source via
   `renderRollupAggregate` (`:1800-1862` — 63 lines of Java-source-as-JS-string-concat; a Java port
   is *cleaner*); `aggregates` expands each descriptor into 3-4 handler variants; `adminModel`;
   the `translate` catalog.
5. **File paths are ALWAYS rendered with Mustache** regardless of the body engine
   (`getMustacheEngine().generate(location, template.rename, params)` — ~30 occurrences).
   Body engine selection (`getGenerationEngine :1713-1736`): `velocity|javascript|mustache`,
   **defaulting to Mustache both when `engine` is undefined and when unknown**; optional
   `template.sm`/`template.em` marker overrides (`:1729-1734`).
6. **`cleanData` (`:1864-1883`)** — recursive NaN/`"NaN"` scrub before every render, in-place.
7. **Migration shims** `migrateForm :83-108` / `migrateReport :119-130` are commented "*should be
   deleted in the future*" — decide: port or drop (dropping requires confirming no old-format
   `.form`/`.report` files remain in the wild; safer to port and delete later).

`parameterUtils.process()` (`:54-490`) — the derivation pass, all in-place mutation:
`javaGenFolderName`/`javaPerspectiveName`, per-entity datasource vs
`DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT`, `tablePrefix` substitution into
`dataCount`/`dataQuery`, PROJECTION owner resolution tolerant of 2- and 3-segment
`projectionReferencedModel` (issue #6423, `:43-52,78-88`), Base64-decoded `importsCode`,
`checks` → row/guard/document split, the big property pass (`:110-265`: string→boolean coercion of
~8 flags, `widgetLabel` default, `parseDataTypes`+`resolveJavaClass`, `primaryKeys`,
`masterEntity/Id`, `hasDates/hasFloats/hasDropdowns/hasProcess`, `masterProperties`,
dropdown/DOCUMENT_STATUS controller URL construction in both `/services/java/...Controller` and
`/services/ts/...Service.ts` forms, personal/partner identity classes), the second pass
(`:267-383`: personal/partner composition-child scope inheritance one hop, `sensitiveProperties`,
`myChildren`/`partnerChildren`, `labelParts` FK resolution), the third pass (`:389-444`:
`dependsOn` trigger URL resolution order-independent, `widgetPatternJs/Java` escaping,
`widgetOptionsFilterValueJs` pre-rendering), then `parameters.perspectives` and `parameters.roles`.

`generate.mjs` semantics to preserve: the **cross-project EXTENSION augmentation**
(`augmentWithExtensions :139-186` — scans every sibling project's `<name>.model` for EXTENSION
entities whose `extensionReferencedModel` names THIS project, appends them deterministically
sorted); the **targeted `gen/<sub>` scrub** derived from actual output paths (`:87-99` — a template
writing under `gen/events` must not wipe `gen/<model>`); the `.gen` audit file; the publish call.

### 0.4 Template modules (KEEP, mostly)

9 template modules reference the utils (11 grep hits for `service-generate/template/` minus
generateUtils itself and a comment in `application-core/shell/js/stores/reports.js:25` that
hand-mirrors `sanitizeJavaIdentifier` — collapse that duplication too). Their `generate()` functions
are 4-15-line adapters; `getTemplate()` is pure declarative data. Total: ~1,308 lines `template.js`
+ ~1,140 lines harmonia `ui/*.js` sub-descriptors (the harmonia composite spreads **13 sibling
modules → ~62 source descriptors**, plus 5 rest + 1 schema).

Per-template `generate()` pre-logic that must move into the Java pipeline (or per-template hooks):
- `template-application-ui-harmonia-java/template/template.js:12-39` sets `javaRuntime`,
  `appTitle/appDescription/appIcon/appLanguages`, `customWidgets`, inverts `processTaskLabels`
  into `processTaskKeys`.
- `template-form-builder-harmonia/template/template.js:12-14` indents `model.code` by 4 spaces
  before delegating to `generateGeneric`.
- Composition is spread-and-concat: full-stack = `[...schema.sources, ...rest.sources,
  ...ui.getSources(parameters)]` + parameter dedupe by name (`template.js:50-65`).

135 `.template` files under `components/template/` (harmonia 51, events-java 25, rest 5, dao 4,
form-builder 3, schema 1, mapping-java 1) are **untouched** — already rendered by the Java engines.

### 0.5 All invokers (the switch-over surface)

`generate.mjs` callers:
- AngularJS provider `GenerateService.generateFromModel` — `service-generate/generate.js:14,21-26`
- editor-entity `js/editor.js:169,191-192,226`; editor-form-builder `js/editor.js:2698,2722-2723,2753`;
  editor-report `js/editor.js:173,187-188`; view-projects `js/projects.js:941,999,1043,1060,2127`
- **editor-intent replays the `codeGenerations` plan** — `js/editor.js:255-282,293`
- **resources-builder headless replay** — `builder/js/services/intentApi.js:22,128,136` driven by
  `pipeline.js:130-143`
- ITs: `IntentEngineIT.java:1567,1956,2149`, `IntentEmissionCoverageIT:2328`,
  `IntentCrossModuleCollisionIT:189`, `IntentCrossModelFieldRetirementIT:259`,
  `IntentCrossModelScheduleSourceIT:264`
- `tests-framework/BaseTestProject.java` drives the same path through the UI (Selenide), no direct
  HTTP.

`templates.js` consumers: script tags in `editor-entity/modeler.html:25`,
`editor-form-builder/editor.html:27`, `view-projects/projects.html:25`, `view-welcome/welcome.html:24`.

The intent seam: `IntentGenerationService.buildCodeGenerationPlan:142-164` emits
`{path, templateId, parameters}` per written model file; `IntentEndpoint.java:91`
(`POST /services/ide/intent/generate`) returns it in `GenerationResult` (`:81`); the client replays.
**After the port, execute the plan in-process at `IntentGenerationService.java:132`** and keep
returning `codeGenerations` for information/compatibility.

---

## 1. Design decisions (defaults chosen; flag disagreement before starting)

1. **Hybrid template layer first.** JS `getTemplate()` descriptors stay (Java already reads them via
   `GenerationService.getTemplateMetadata`); only *execution* moves to Java. The per-template
   `generate()` pre-logic (§0.4) moves into the Java pipeline keyed by templateId, or into small
   optional descriptor fields. Converting descriptors to pure JSON (fully JS-free templates) is a
   follow-up, not a prerequisite.
2. **Host the port in `components/ide/ide-template`** (extend `GenerationService`; add
   `ModelGenerationService` + `parameterUtils` port as package-private collaborators), NOT a new
   module. The endpoint goes on the existing `GenerationEndpoint` as
   `POST /services/ide/generate/model/{workspace}/{project}/{*path}`.
3. **Keep the request contract** `{template, parameters}` + model path so clients are a URL swap.
   Keep the old JS endpoint alive through PR 2; delete in PR 3.
4. **Naming collapses into the canonical Java implementations**: `IntentNaming` for
   humanize/pluralize (including `HUMANIZE_OVERRIDES`/`PLURALIZE_OVERRIDES` — verify the override
   maps match before deleting the JS mirrors), `JavaNames`/new util for `sanitizeJavaIdentifier`.
   Watch the dependency direction: `ide-template` must not depend on `engine-intent` — if needed,
   move `IntentNaming` (or extract the naming core) into a neutral module both depend on.
5. **Parameter graph representation in Java**: keep it as a Gson `JsonObject`/`Map<String,Object>`
   bag (the templates' Velocity/Mustache contexts are stringly anyway), NOT a typed model. The 47
   cases marshal fields; a typed model would triple the port size for no rendering benefit.
6. **Migration shims (`migrateForm`/`migrateReport`) are ported**, marked deprecated, deleted in a
   later cleanup once the wild has no old-format files.

## 2. Delivery plan — three stacked PRs

### PR 1 — the Java utils + parity harness (no behavior change; both paths live)

1. Port `parameterUtils` → `ModelGenerationParameters` (or similar) in `ide-template`:
   `process`, `parseDataTypes`, `resolveJavaClass`, `sanitizeJavaIdentifier`; `humanizeName`
   delegates to the canonical naming implementation (§1.4).
2. Port `generateUtils` → `ModelGenerationService`: `generateGeneric`, `generateFiles` with the
   full 47-case dispatch, `generateCollection`, the pre-pass (EXTENSION merge, document/rollup
   annotations, 28 partitions), `cleanData`, engine selection incl. Mustache-for-paths and sm/em,
   and the `translate` action (new constant + handling in `GenerationTemplateMetadataSource`).
3. Port the `generate.mjs` orchestration (model load + cross-project EXTENSION augmentation +
   targeted gen-scrub + `.gen` audit + publish) as a service method, not yet exposed via HTTP.
4. **The parity harness — the actual deliverable of this PR.** An IT
   (`extends IntegrationTest`, no UI) that, for each fixture model: runs the JS path via HTTP and
   the Java path in-process, captures both file sets, and asserts **byte-identical** paths +
   contents. Fixtures: the models exercised by `IntentEngineIT`, `IntentEmissionCoverageIT`,
   `IntentCrossModuleCollisionIT`, `IntentCrossModelFieldRetirementIT`,
   `IntentCrossModelScheduleSourceIT`, plus an `.edm`-only, a `.form`, and a `.report` case.
   Sort-stability caveats: JS object-key iteration order and `JSON.stringify` formatting differ from
   Gson — the `.gen` audit file and the `translate` JSON catalog need deliberate key ordering +
   pretty-print matching (or the harness normalizes JSON files structurally instead of byte-wise;
   byte-wise for everything else).

**Verify:** parity IT green over all fixtures; unit tests for `parseDataTypes`, the EXTENSION merge,
`rollups` coalescing, `translate` catalog; `mvn -T 1C clean install -P quick-build`; formatter.

### PR 2 — the Java endpoint + consumer switch + in-process intent execution

1. `POST /services/ide/generate/model/{workspace}/{project}/{*path}` on `GenerationEndpoint`
   (roles as today's DEVELOPER-gated `/services/ide/**`; the JS endpoint's implicit auth was the
   JS-endpoint gate).
2. URL swap in: `service-generate/generate.js` provider, editor-entity, editor-form-builder,
   editor-report, view-projects, editor-intent replay, resources-builder `intentApi.js`. (UI module
   changes need the fat-jar repackage to verify locally — root `CLAUDE.md` rule.)
3. **In-process intent execution**: `IntentGenerationService.java:132` executes the plan after
   writing models; response keeps `codeGenerations` (now informational). Simplify/remove the client
   replay loops in editor-intent (`:255-282`) and builder `pipeline.js:130-143` — the Builder's
   staged progress UI keeps its steps but "Generate code (i/n)" becomes part of the single
   generate call's result.
4. Switch the 5 ITs from HTTP replay to asserting the in-process result (or keep them HTTP against
   the new endpoint — smaller diff, same coverage).

**Verify:** smoke IT set + the intent IT family + `IntentEditorLoadsIT` (editor Generate journey) +
`IntentBuilderShellIT` (builder publish pipeline); full-instance regen check per the "green =
full-instance verification" rule.

### PR 3 — deletion + list-service decision

1. Delete `generate.mjs`, `generateUtils.js`, `parameterUtils.js`, the now-dead `generate()`
   adapters in the 9 template modules (keep `getTemplate()`), and the parity harness's JS leg
   (keep the Java-path assertions as regression fixtures).
2. `templates.js` list service: either port to a small Java endpoint on `ide-template`
   (enumerating `platform-templates` via `ExtensionsFacade` + `getTemplateMetadata`) and swap the 4
   script-tag consumers, or explicitly keep it as the one remaining JS piece (it's 89 lines and
   read-only). Default: port it — it completes the story and kills the GraalJS boot on every
   Generate-dialog open. Resolve the dead near-duplicate in `platform-core/extension-services/`.
3. Docs sweep: root `CLAUDE.md` (generation flow references), this file's status, release notes
   (the endpoint URL change for any external callers of `/services/js/service-generate/…`).

## 3. Sizing

~3.5-4.5k new Java lines incl. tests; ~2,850 JS lines deleted plus the client replay loops.
Comparable to the document-numbering wave. Risk is front-loaded into PR 1's parity harness — PRs 2
and 3 are mechanical once the diff is empty.

## 4. Gotchas for the implementer (accumulated from this repo)

- **`JsonHelper`/`GsonHelper` exclude fields without `@Expose` and pretty-print** — use a plain
  Gson for the parameter bag and the `.gen`/translate outputs (root `CLAUDE.md`).
- **Synchronizer artefact locations are registry-relative; `IRepository` paths are
  repository-absolute** — the port writes to the *workspace* via `WorkspaceService` like
  `GenerationService` does; don't hand-build repository paths.
- **JS truthiness vs Java**: the property pass coerces `"true"`/`"false"` STRINGS to booleans
  (`parameterUtils :110-265`); several templates test flags Velocity-style (`#if($flag)`) — the Java
  bag must carry real booleans, matching what the JS handed Velocity after coercion.
- **Velocity/Mustache contexts**: `TemplateEnginesManager` engines take parameter maps — confirm
  how the current facade's JSON→Map conversion types numbers (Gson doubles vs ints; `widgetSize` is
  a plain integer column count and `5.0` in a rendered template would be a regression). The parity
  harness catches this class of bug — that's why it's byte-wise.
- **Ordering determinism**: EXTENSION merge sorts by `extensionReferencedEntity.name`
  (`generate.mjs:181`); keep every partition/loop in model order; `LinkedHashMap` everywhere.
- **`-T 1C` truncation trap** (memory: BPM events wave): verify the SDK/dist resources survive the
  parallel build when touching api-modules-*.
- **Formatter + javadoc**: new public classes in `ide-template` need javadoc that compiles under
  the release profile (`-P release -Dgpg.skip=true …` on the touched modules before push).
- Pre-push checklist: `mvn formatter:format` + `formatter:validate`, unit tests, smoke ITs headless.

## 5. Payoff (why this is worth it)

1. Intent Generate collapses from `1 + N` HTTP round-trips (each booting a GraalJS context and
   JSON-serializing the whole model per source file — ~68 sources for a Harmonia full-stack) to one
   in-process call; client replay loops deleted.
2. One naming implementation (`IntentNaming`) instead of three documented mirrors.
3. GraalJS leaves the generation path entirely — consistent with the Java-only application-layer
   direction (Harmonia-only templates #6591, Camel→Java #6586).

## 6. What PR 1 landed, and two corrections to this plan

### 6.1 The code

All under `components/ide/ide-template/.../service/model/`, package-private except the service and the
generated-file record:

| Class | Ported from |
|---|---|
| `ModelGenerationService` (public) | `generate.mjs` - model read, cross-project EXTENSION augmentation, targeted `gen/<sub>` scrub, `.gen` descriptor, publish. `render()` returns everything a generation *would* write, without writing; `generate()` writes and publishes. |
| `ModelGenerator` | `generateUtils.generateFiles` / `generateGeneric` - the pre-pass, the 28 partitions, `generateCollection`, `adminModel`, the whole-model fallback, the two translate actions |
| `GlueGenerator` | the 22 glue collections, incl. the roll-up coalescing and the four aggregate variants |
| `ModelParameterProcessor` | `parameterUtils.process` |
| `ModelDataTypes` | `parameterUtils.parseDataTypes` / `resolveJavaClass` |
| `ModelTranslations` | the catalogs plus `migrateForm` / `migrateReport` (ported, as §1.6 decided) |
| `RollupAggregates` | `renderRollupAggregate` |
| `ModelTemplateAdapters` | the per-template `generate()` pre-logic of §0.4, keyed by templateId |
| `ModelTemplateRenderer` | template loading + engine selection |
| `ModelValues`, `ModelJson`, `JavaScriptJson` | the value semantics the port needs - see §6.3 |

Naming collapsed into `modules/commons/commons-helpers`' new `NamingHelper`, which both `IntentNaming`
and the pipeline now delegate to (§6.2). Parity harness: `GenerationParityIT` - 11 fixture/template
cases (a rich model and a simple one through schema/DAO/REST/full-stack, process glue, a form, a
report, and the same simple model twice more with a sibling project contributing an entity extension:
one field to merge, plus a primary key, a colliding name and an audit column to skip), one test method
(the base class discards the context per method, so a method per case would boot the application once
per case), reporting every difference rather than the first.

**Not ported: `template-mapping-java`.** Its `generate()` is a mapping compiler - it derives Java
expressions from the mapping's columns - not the marshalling every other template's pre-logic is. The
Java pipeline rejects it by name; the JavaScript path still serves it. Port it before PR 2 switches
the `.mapping` consumers, or leave that one consumer on the JavaScript endpoint.

### 6.2 Correction: `IntentNaming.humanize` is NOT the JS `humanizeName`

§1.4 assumed the two were the same function and told the implementer to "verify the override maps
match". The override maps do match - the *algorithms* do not. `IntentNaming.humanize` splits on `-`
and `_` and humanizes each segment (`payment_method` -> `Payment Method`); the JS `humanizeName` walks
characters only (`payment_method` -> `Payment_method`). Collapsing them would silently relabel every
generated artefact whose property names contain a separator.

They decompose cleanly instead: the JS function *is* `IntentNaming.humanize`'s per-segment core. So
`NamingHelper` exposes both - `humanizeIdentifier` (the character walk, what generation uses) and
`humanizeName` (separator-aware, what the intent generators use, implemented over the former) - and
`pluralizeLabel`, which really was identical. One implementation of each rule, no behaviour change on
either side. Whether the generation path *should* adopt the separator-aware form is a separate,
deliberate decision: it changes generated labels, so it needs its own change and regeneration sweep.

### 6.3 Correction: three JSON-writing differences, not one

§4 flagged `JsonHelper`/`GsonHelper`'s `@Expose` filtering and pretty-printing. Byte-parity actually
turns on three more, all of which show up in the `.gen` descriptor and the translation catalogs:

1. **Gson drops null-valued keys**; `JSON.stringify` keeps them (`"fk":null`).
2. **Gson escapes HTML characters** as `<` etc.; `JSON.stringify` does not.
3. **Gson writes an integral double as `6.0`**; JavaScript writes `6`.

Rather than configure around the first two and still lose on the third, `JavaScriptJson` writes the
JavaScript form directly, in both a pretty and a compact mode (`adminEntitiesJson` and `queryJava` are
compact; the descriptor and catalogs are two-space).

And the typing rule the whole port rests on, which §4 got half-right: **every number in the parameter
graph is a `Double`.** That is not a choice - the graph reaches a template engine as the result of
`GsonHelper.fromJson(json, Map.class)`, whose object type adapter maps every JSON number to `Double`.
The JavaScript path renders `minLength` as `0.0` today, so the Java path must too. Numbers computed
during derivation are therefore stored as `Double`, and `ModelValues.putNumber` is the only way to
store one. The companion rule: JavaScript `undefined` means *the key is absent*, while a Java `null`
is a present key with a null value - so where the original assigned `undefined`, the port removes the
key (`ModelValues.remove`, `GlueGenerator.copy`).

### 6.4 A platform finding, left as a follow-up: the Mustache engine mutates its input

`MustacheGenerationEngine.decorateParameters` writes into the parameter map it is handed, recursively,
adding an indexed twin of every collection under a `_`-suffixed key (so `{{#items_}}{{index}}{{/items_}}`
works). Nobody noticed because the only callers reached it through a fresh map: the JS facade
deserializes the parameters per call, and the file-generation path never looks at its map again. The
Java pipeline hands it a live graph, so the artefacts landed in the parameters and from there in the
`.gen` descriptor - which is re-read on regeneration, so they would have accumulated.

Worked around here by copying the context per engine invocation (`ModelTemplateRenderer.context`),
which is what the engines have effectively always received. The engine not mutating its caller's map
would be the better fix, but it changes shared platform behaviour and belongs in its own change rather
than in a port that is meant to be output-identical.

### 6.5 Harness gotcha worth keeping

A workspace path is resolved against `UserFacade.getName()`, so the in-process pipeline (on the test
thread) and the HTTP endpoint (as the authenticated tenant) see *different* workspaces of the same
name. The harness seeds both - the Java side through `WorkspaceService`, the JavaScript side over HTTP
- under one workspace name per case, because the workspace name is baked into the `.gen` descriptor
and so has to match. It reads the JavaScript output straight out of the repository, since the
workspace service would resolve the wrong user's copy.
