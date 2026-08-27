## Model-to-code generation is Java (`ide-template`)

Turning a model file into generated artefacts — the "Generate from EDM / model" step every template
rides on — runs entirely in Java, in `components/ide/ide-template`
(`ModelGenerationService` → `ModelGenerator` / `GlueGenerator` / `ModelParameterProcessor` /
`ModelTemplateAdapters` / `ModelTemplateRenderer`). The endpoint is
**`POST /services/ide/generate/model/{workspace}/{project}?path=<model>`** with body
`{template, parameters}`; the older JavaScript path (`generate.mjs` + `generateUtils.js` +
`parameterUtils.js` under `service-generate`) was migrated and **deleted** — see
[`GENERATION_UTILS_JAVA_PLAN.md`](GENERATION_UTILS_JAVA_PLAN.md) §6-§7. Things to know before touching it:

- **Template descriptors are still JavaScript.** Each `template-*` module exports `getTemplate()`
  (sources + parameters), which the Java pipeline reads through
  `GenerationService.getTemplateMetadata` (a GraalJS call per template). Their `generate()` functions
  are gone: the per-template pre-logic lives in `ModelTemplateAdapters`, keyed by template id, and a
  template the pipeline does not know is **refused by name** rather than half-generated. Add a
  template → add its adapter entry.
- **Every number in the parameter graph is a `Double`** and an absent key is not a null one
  (`ModelValues.putNumber` / `remove`) — the graph reaches the engines through a `Map` deserialization
  that types every JSON number as `Double`, so `minLength` renders as `0.0` and always did.
- **Write JSON with `JavaScriptJson`, not Gson**, for the `.gen` descriptor and the translation
  catalogs: Gson drops null keys, escapes HTML and writes `6.0` where the descriptor needs `6`.
- **The Mustache engine writes into the parameter map it is handed** (it adds an indexed twin of every
  collection), so `ModelTemplateRenderer` copies the context per invocation. Without that, the
  artefacts land in the `.gen` descriptor, which is re-read on the next regeneration and accumulates.
- **The intent Generate is one call**: `IntentGenerationService` writes the model files and then runs
  the `.settings` recipes through this pipeline itself, reporting each one's outcome in
  `codeGenerations` (`generated`, plus `error`). No client replays a plan any more.
- `ModelGenerationIT` is the regression harness (it replaced the JS-vs-Java parity harness): every
  template renders, twice, with the descriptor and the cross-project entity extension asserted.

