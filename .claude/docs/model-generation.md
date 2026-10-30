## Model-to-code generation is Java (`ide-template`)

Turning a model file into generated artefacts — the "Generate from EDM / model" step every template
rides on — runs entirely in Java, in `components/ide/ide-template`
(`ModelGenerationService` → `ModelGenerator` / `GlueGenerator` / `ModelParameterProcessor` /
`ModelTemplateAdapters` / `ModelTemplateRenderer`). The endpoint is
**`POST /services/ide/generate/model/{workspace}/{project}?path=<model>`** with body
`{template, parameters}`; the older JavaScript path (`generate.mjs` + `generateUtils.js` +
`parameterUtils.js` under `service-generate`) was migrated and **deleted** — see
[`GENERATION_UTILS_JAVA_PLAN.md`](GENERATION_UTILS_JAVA_PLAN.md) §6-§7. Things to know before touching it:

- **Template descriptors are data, and a generation starts no script engine (#8007).** A template's
  descriptor (sources + parameters) is a `.json` file read by `TemplateDescriptors`; a composite takes
  in its parts with `{"include": "<descriptor>.json"}` entries in `sources` / `parameters` (replaced in
  place, parameters then deduplicated by name, first wins). **The template id is still the `.js` path**
  its `.extension` registers (`template-application-dao-java/template/template.js`): intent recipes,
  `ModelTemplateAdapters` and every committed `.gen` record it, so an id resolves to the `.json` next
  to the module it names - do not rename the ids. A descriptor still written as JavaScript (a user
  project's) is called through the `ExtensionModuleEvaluator` SPI where the JavaScript engine is
  deployed; headless generation has none and refuses it by name. `project.json` is a Velocity source
  with `"overwrite": false`: a non-blank file the project has is kept. The catalogue the IDE dialogs
  read is `TemplateCatalogueEndpoint`, mapped on the retired scripts' URLs
  (`/services/js/service-template/api/templates.js`). The per-template pre-logic lives in
  `ModelTemplateAdapters`, keyed by template id, and a template the pipeline does not know is
  **refused by name** rather than half-generated. Add a template → add its adapter entry.
- **Every number in the parameter graph is a `Double`** and an absent key is not a null one
  (`ModelValues.putNumber` / `remove`) — the graph reaches the engines through a `Map` deserialization
  that types every JSON number as `Double`, so `minLength` renders as `0.0` and always did.
- **Write JSON with `JavaScriptJson`, not Gson**, for the `.gen` descriptor and the translation
  catalogs: Gson drops null keys, escapes HTML and writes `6.0` where the descriptor needs `6`.
- **The Mustache engine writes into the parameter map it is handed** (it adds an indexed twin of every
  collection), so `ModelTemplateRenderer` copies the context per invocation. Without that, the
  artefacts land in the `.gen` descriptor, which is re-read on the next regeneration and accumulates.
- **The `.gen` descriptor is named after the model file's whole name** — `myapp.model.gen`,
  `myapp.glue.gen`, `OrdersByCustomer.report.gen` — so every generation of a project keeps its own
  record. Named after the base name alone (#7057) a project's model and glue generations both wrote
  `myapp.gen` and the survivor described only the one that ran last. The readers (the entity / report /
  form-builder editors' Regenerate, `ProjectDeployer`) accept the legacy base-name form as well,
  because a project generated earlier has it committed.
- **The intent Generate is one call**: `IntentGenerationService` writes the model files and then runs
  the `.settings` recipes through this pipeline itself, reporting each one's outcome in
  `codeGenerations` (`generated`, plus `error`). No client replays a plan any more.
- **A retired template is answered with its successor, not refused bare (#8051).** The AngularJS and
  TypeScript stacks went in 14.16.0 (#6588), but projects still record them in their `.gen`.
  `RetiredTemplates` maps each to its Java successor (or to none); `GenerationEndpoint` answers such a
  generation `409 {message, template, successor}`, the three IDE clients confirm and regenerate with the
  successor, and the generation removes a legacy base-name `.gen` that records a retired template.
  `TypeScriptToJavaMigrationIT` covers it end to end.
- **A calculated-field formula is `Calc` arithmetic.** The Java templates write it into
  `Calc.eval("...")` and the Harmonia UI mirrors it. `CalculatedFormulas` (run by the DAO, REST and
  Harmonia adapters) rewrites the `entity["X"]` / `entity.X` reads of TypeScript-era models to bare
  names and **refuses the generation**, naming every field, for anything that is not arithmetic over
  the entity's properties - a JavaScript or Java expression used to generate a repository that did
  not compile, or failed on every save. A details layout without a composition master is generated
  as the plain layout (`ModelParameterProcessor.resolveDetailLayoutWithoutMaster`).
- `ModelGenerationIT` is the regression harness (it replaced the JS-vs-Java parity harness): every
  template renders, twice, with the descriptor and the cross-project entity extension asserted.

