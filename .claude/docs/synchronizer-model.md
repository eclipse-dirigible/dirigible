## How a feature reaches the running system (the synchronizer model)

This is the single most important architectural pattern to understand before changing engines or adding artefact types. Dirigible does not "deploy" projects — instead, files in the repository (`/registry/public/...`) are continuously reconciled into runtime state by **synchronizers**.

Each artefact type has three collaborating pieces:

1. An **Artefact** JPA entity (`extends Artefact` from `components/core/core-base`) — the persisted projection of a file.
2. A **Synchronizer** (`extends BaseSynchronizer<A, ID>` or `MultitenantBaseSynchronizer`) — registered as a Spring bean, scans the repository for a specific file extension/pattern, parses it, upserts the artefact, and reacts to lifecycle phases (`CREATE`, `UPDATE`, `DELETE`, `START`, `STOP`). Ordering across synchronizer types is fixed in `SynchronizersOrder`.
3. An optional **engine/service/endpoint** that consumes the live artefact (e.g. Quartz scheduler for jobs, Flowable for `.bpmn`, Camel for routes, Spring MVC endpoints for `expose` declarations).

Existing synchronizer implementations (grep `extends BaseSynchronizer` / `extends MultitenantBaseSynchronizer`) give a complete inventory of supported artefact types: `Job`, `Bpmn`, `Camel`, `Listener`, `Csvim`, `DataSource`, `Table`, `View`, `Access` (security), `Expose` (URL routing), `ExtensionPoint` / `Extension`, `Markdown`, `Proxy`, `PrintTemplate` (`.print` → CMS seeding), etc. Adding a new artefact type means producing a new synchronizer + entity + (usually) a service, then registering it in the relevant `group-*` aggregator.

JS/TS user code is **not** synchronized — it is loaded on demand by `engine-javascript` (`JavascriptEndpoint` at `/services/js/...`, `/public/js/...`) via `DirigibleJavascriptCodeRunner` backed by Graalium/GraalVM polyglot. The `api-*` Java modules under `components/api/` register the JS-callable APIs (`@dirigible/db`, `@dirigible/http`, etc.) into the GraalJS context — pre-built TS/JS bundles for those APIs live in `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/`.

