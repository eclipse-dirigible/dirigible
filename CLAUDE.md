# CLAUDE.md

Provides behavioral guidelines to reduce common LLM coding mistakes and guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Project

Eclipse Dirigible — a high-productivity application platform (hpaPaaS). The runtime is a single Spring Boot fat jar that bundles an in-browser IDE plus execution engines (GraalJS, Flowable BPM, Camel, Quartz, Lucene, CMS, OData, etc.). It supports "In-System Programming": users develop and modify the running system through the browser. The shipped artifact is `build/application/target/dirigible-application-*-executable.jar`; the entry point is `org.eclipse.dirigible.DirigibleApplication` (`build/application/src/main/java/org/eclipse/dirigible/DirigibleApplication.java`).

## Prerequisites

- Java JDK 21 (project compiles to Java 21; CI builds/tests run on Corretto 24, bytecode target stays 21)
- Maven 3.8.x
- Node.js 22.x with global installs of `typescript` and `esbuild` (frontend WebJars are transpiled/bundled at build time)
- `ttyd` (only required at runtime for the in-IDE terminal on port 9000, and for integration tests)

## Build

The whole project is a multi-module Maven build at the root. The Maven coordinates are `org.eclipse.dirigible:dirigible-parent` (currently `13.0.0-SNAPSHOT`).

| Goal                                | Command                                                            |
| ----------------------------------- | ------------------------------------------------------------------ |
| Full build with all unit tests      | `mvn clean install`                                                |
| Fast build (no tests/javadoc/license/format) | `mvn -T 1C clean install -P quick-build`                  |
| Unit tests only (no ITs)            | `mvn clean install -P unit-tests`                                  |
| Run unit + integration tests        | `mvn clean install -P tests`                                       |
| Integration tests only              | `mvn clean install -P integration-tests -D selenide.headless=true` |
| A specific IT (comma-separated)     | `mvn clean install -P integration-tests -Dit.test="CsvimIT,CreateNewProjectIT" -D selenide.headless=true` |
| A specific unit test (single module) | `mvn -pl <module-path> -am test -Dtest=ClassName#method`          |
| Format Java code                    | `mvn formatter:format` (or `-P format`)                            |
| Validate formatting (what CI runs)  | `mvn -T 1C formatter:validate`                                     |
| Static analysis                     | `mvn clean install -P spotbugs`                                    |
| Coverage report (JaCoCo)            | `mvn clean -B package -P coverage`                                 |
| Update license headers              | `mvn license:format -P license -DskipExistingHeaders=false`        |

The Java formatter profile is `dirigible-formatter.xml` at the repo root — CI fails the build on formatter violations, so run `mvn formatter:format` (or rely on the IDE save action configured per `CONTRIBUTING.md`) before pushing.

Tests follow Maven's surefire/failsafe split: `*Test` runs under surefire (unit), `*IT` runs under failsafe (integration). Integration tests are Selenide-based UI tests living in `tests/tests-integrations/src/main/java/.../tests/`. The build profile `integration-tests` disables surefire and only runs failsafe.

## Run

After a build:

```
java -jar build/application/target/dirigible-application-*-executable.jar
```

UI at `http://localhost:8080`, default credentials `admin`/`admin`. Useful URLs on the same port:

- `/` — redirects to the IDE entrypoint (default `services/web/shell-ide/`, configurable via `DIRIGIBLE_HOME_URL`)
- `/swagger-ui/index.html`, `/api-docs` — OpenAPI / Swagger UI for built-in REST endpoints
- `/spring-admin/` — Spring Boot Admin (server profile enabled in this app)
- `/actuator/health/readiness`, `/actuator/health/liveness` — health probes (also what the CI DAST job polls)
- `/services/...` — secured Spring-side endpoints (see `BaseEndpoint.PREFIX_ENDPOINT_*`)
- `/public/...` — unauthenticated counterpart
- `/services/js/<project>/<file>.{js,mjs,ts}` — execute a JS/TS file from the user repository (the same path under `/public/js/` is the unauthenticated variant)
- `/odata/v2/...` — OData services (CXF base path)
- `/websockets/...` — WebSocket endpoints

Default DB is **file-backed** H2 at `jdbc:h2:file:./target/dirigible/h2/DefaultDB;LOCK_TIMEOUT=10000` (see `components/data/data-sources/src/main/resources/META-INF/dirigible/datasources/DefaultDB.datasource` — this is also why `mvn clean` is required when switching DBs, the old H2 files survive otherwise). Switch by exporting `DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER/URL/USERNAME/PASSWORD`. Server port is overridable via `DIRIGIBLE_SERVER_PORT` (default 8080). The in-IDE terminal launches `ttyd` on 9000. The Dockerfile additionally exposes 8081 as the Graalium debug port (env var `DIRIGIBLE_JAVASCRIPT_GRAALVM_DEBUGGER_PORT`; debugging is on by default via `DIRIGIBLE_GRAALIUM_ENABLE_DEBUG=true`).

The on-disk Dirigible repository ("registry") defaults to `./target/` relative to the working directory (`DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER`). Inside it the canonical layout is `/registry/public/<project>/...` for published artefacts and `/users/<user>/workspace/<project>/...` for in-IDE workspaces (see `IRepositoryStructure`). Multi-tenancy is on by default (`DIRIGIBLE_MULTI_TENANT_MODE=true`).

For remote debugging:

```
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar build/application/target/dirigible-application-*-executable.jar
```

## Local dev-loop automation (`.claude/` commands + script)

The repo ships Claude Code slash commands (`/dirigible-start`, `/dirigible-stop`, `/dirigible-logs`, `/dirigible-test`, `/dirigible-pr` in `.claude/commands/`) wrapping one cross-platform Node.js driver (`.claude/scripts/dirigible.mjs`) for the local build/run/log/test/PR loop. Prefer these over hand-running `mvn`/`java`. **Maintainer reference (read before changing the commands or driver): [`.claude/scripts/README.md`](.claude/scripts/README.md)** — driver subcommands, cross-platform mechanics, the background-task log-tail rationale, and the team-wide permission whitelist in `.claude/settings.json`. The user-facing guide is the "Claude Code Commands" section of the root `README.md`.

## Repository layout

The Maven hierarchy (from root `pom.xml`) is: `modules`, `components`, `build`, `dependencies`, `tests`, `cli`. Each is a Maven aggregator.

- **`modules/`** — low-level platform building blocks consumed by everything else: `commons`, `database`, `engines` (script engine abstractions), `odata`, `parsers`, `repository`. These are pure libraries with no Spring wiring.
- **`components/`** — the Spring application surface, split by concern. The `build/application` module wires all of these together via Spring Boot auto-configuration — there is no manual "list of features"; capability comes from whichever components/* JARs are on the classpath.
  - `components/core/` — base Spring infrastructure: `core-base`, `core-configurations`, `core-database`, `core-extensions`, `core-initializers`, `core-registry`, `core-repository`, `core-spring`, `core-tenants`, `core-tracing`, `core-version`, `core-healthcheck`, `core-project`.
  - `components/engine/` — runtime execution engines: `engine-java` (client `.java` runtime — synchronizer + compiler + SPI; see "Client Java code" below), `engine-javascript` (GraalVM polyglot), `engine-typescript`, `engine-bpm-flowable` (workflows), `engine-camel` (integration routes), `engine-jobs` (Quartz), `engine-listeners` (message listeners), `engine-cms-*` (internal / S3 / SharePoint), `engine-odata`, `engine-openapi`, `engine-web`, `engine-websockets`, `engine-template-*`, `engine-open-telemetry`, etc. Each engine registers itself as a Spring component and contributes routes/processors/scheduled jobs to the running app.
  - `components/api/` — Java implementations of JS/TS APIs that user code in the IDE can `import` (e.g. `api-database`, `api-http`, `api-mail`, `api-git`, `api-bpm`, `api-cms`, `api-s3`, `api-kafka`, `api-rabbitmq`, `api-mongodb`, `api-pdf`, ...). These are bridged into GraalJS by `engine-javascript`.
  - `components/data/` — data-layer tooling: `data-sources`, `data-structures` (HDB-style table definitions), `data-management`, `data-import`/`-export`, `data-csvim` (CSV import model), `data-store` (TS `@Entity` decorators → Hibernate via HBM XML), `data-store-java` (Java `@Entity` annotations → same Hibernate machinery; consumes `engine-java`'s SPI), `data-transfer`, `data-processes`.
  - `components/ide/` — the browser IDE: a large set of `ide-ui-*` WebJar modules (Monaco, perspectives for git/bpm/database/jobs/etc.) plus backend services `ide-git`, `ide-terminal`, `ide-workspace`, `ide-console`. Frontend assets are bundled with `esbuild`/`tsc` during the Maven build, hence the Node toolchain prerequisite.
  - `components/platform/`, `components/platform-ide/`, `components/security/`, `components/template/`, `components/ui/`, `components/resources/` — perspective/branding/security plumbing and shared UI assets.
  - `components/group/` — _Maven aggregators only_ (`group-api`, `group-engines`, `group-ide`, ...). When you add a new component module, register it in the matching `group-*/pom.xml` so the assembly picks it up.
- **`build/application/`** — the only thing that turns the above into a runnable jar. `DirigibleApplication.java` is intentionally small (just `@SpringBootApplication` + `@EnableAdminServer` + Camel OpenTelemetry). `Dockerfile` lives here too.
- **`tests/tests-framework/`** — shared Selenide/Spring test base classes used by ITs.
- **`tests/tests-integrations/`** — `*IT.java` UI integration tests plus project fixtures under `src/main/resources/<TestName>/`. Each IT fixture directory is a Dirigible project that gets imported and exercised through the running app.
- **`cli/`** — standalone helper that starts the Dirigible jar against a given user project path; produces `cli/target/dirigible-cli-*-executable.jar`. See `cli/README.md`.
- **`maven-plugins/flowable-bpmn-docs-maven-plugin/`** — custom Maven plugin generating BPMN docs.
- **`dependencies/`** — BOM-style module aggregating third-party version pins (the actual `<properties>` for versions still live in the root `pom.xml`).
- **`open-telemetry/`**, **`samples/`**, **`misc/`**, **`npm/`** — auxiliary content, not part of the Maven reactor in the root build (verify in the relevant `pom.xml` before assuming).

## How a feature reaches the running system (the synchronizer model)

This is the single most important architectural pattern to understand before changing engines or adding artefact types. Dirigible does not "deploy" projects — instead, files in the repository (`/registry/public/...`) are continuously reconciled into runtime state by **synchronizers**.

Each artefact type has three collaborating pieces:

1. An **Artefact** JPA entity (`extends Artefact` from `components/core/core-base`) — the persisted projection of a file.
2. A **Synchronizer** (`extends BaseSynchronizer<A, ID>` or `MultitenantBaseSynchronizer`) — registered as a Spring bean, scans the repository for a specific file extension/pattern, parses it, upserts the artefact, and reacts to lifecycle phases (`CREATE`, `UPDATE`, `DELETE`, `START`, `STOP`). Ordering across synchronizer types is fixed in `SynchronizersOrder`.
3. An optional **engine/service/endpoint** that consumes the live artefact (e.g. Quartz scheduler for jobs, Flowable for `.bpmn`, Camel for routes, Spring MVC endpoints for `expose` declarations).

Existing synchronizer implementations (grep `extends BaseSynchronizer` / `extends MultitenantBaseSynchronizer`) give a complete inventory of supported artefact types: `Job`, `Bpmn`, `Camel`, `Listener`, `Csvim`, `DataSource`, `Table`, `View`, `Access` (security), `Expose` (URL routing), `ExtensionPoint` / `Extension`, `Markdown`, `Proxy`, etc. Adding a new artefact type means producing a new synchronizer + entity + (usually) a service, then registering it in the relevant `group-*` aggregator.

JS/TS user code is **not** synchronized — it is loaded on demand by `engine-javascript` (`JavascriptEndpoint` at `/services/js/...`, `/public/js/...`) via `DirigibleJavascriptCodeRunner` backed by Graalium/GraalVM polyglot. The `api-*` Java modules under `components/api/` register the JS-callable APIs (`@dirigible/db`, `@dirigible/http`, etc.) into the GraalJS context — pre-built TS/JS bundles for those APIs live in `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/`.

## Client Java code (`engine-java` + `data-store-java`)

Client `.java` sources dropped under `/registry/public/<project>/...` ARE synchronized — by `JavaSynchronizer` (`components/engine/engine-java`, artifact `dirigible-components-engine-java`). Knowing the moving parts saves a lot of grep time:

- **One compiler, one classloader, one batch per cycle.** `JavaSynchronizer.parseImpl` only parses + persists the `JavaFile` artefact (and enforces global FQN uniqueness). The `completeImpl` calls just flip a dirty flag. The expensive work — single `javac` task over every client source, single fresh `ClientClassLoader` install, consumer fan-out — happens in `finishing()`. Cross-file references resolve in user code because every client class shares the one `ClientClassLoader` (parent = platform CL). The previous generation's CL becomes unreachable on swap and GC reclaims its Metaspace.
- **SPI: `org.eclipse.dirigible.engine.java.spi.JavaClassConsumer`.** Every loaded class is offered to every registered consumer. Four live in the codebase today, ordered via Spring `@Order` so cross-consumer dependencies resolve within one rebuild cycle:
  - `EntityClassConsumer` (data-store-java, `@Order(100)`) claims `@Entity` classes → registers them with `JavaEntityManager` (table created first).
  - `RepositoryClassConsumer` (data-store-java, `@Order(200)`) claims `@Repository` classes → instantiates each via public no-arg ctor, stores singleton in `RepositoryRegistry`.
  - `ControllerClassConsumer` (engine-java, `@Order(300)`) claims classes annotated with `@Controller` → resolves `@Inject` fields via the `DependencyResolver` chain (`RepositoryRegistry` is the only resolver today), registers routes with `ControllerRouter`, emits OpenAPI fragment via `JavaControllerOpenApiPublisher`.
  - `HandlerClassConsumer` (engine-java, unordered → LOWEST_PRECEDENCE) claims `implements JavaHandler` → publishes to `JavaClassRegistry`.

  `JavaLoader.rebuild()` iterates **consumer-outer / class-inner** (each consumer drains its claimed classes before the next consumer runs). With the `@Order` chain above this guarantees `@Inject CountryRepository` resolves inside `ControllerClassConsumer` because `RepositoryClassConsumer` has already registered every `@Repository` for that rebuild generation. A class may be exactly one of {handler, controller}; carrying both shapes is rejected with an error log. Future "react to compiled client classes" features should plug into this SPI rather than introduce a second synchronizer.
- **Two annotation packages, both in `engine-java`** (engine-java sits on the compile-time classpath of every client `.java`):
  - `org.eclipse.dirigible.engine.java.annotations.*` — entity surface mirroring JPA signatures: `@Entity`, `@Table`, `@Id`, `@GeneratedValue` (+ `GenerationType`), `@Column`, `@Transient`, `@CreatedAt`/`@UpdatedAt`/`@CreatedBy`/`@UpdatedBy`, `@Documentation` (now also valid on methods for OpenAPI summaries).
  - `org.eclipse.dirigible.engine.java.annotations.http.*` — REST surface: `@Controller` (class marker), `@Get`/`@Post`/`@Put`/`@Patch`/`@Delete` (method-level with `value()` path suffix), `@Body`/`@PathParam`/`@QueryParam`/`@Context` (parameter binding), `@Roles` (class- or method-level, any-of role check).
- **`JavaEndpoint` dispatches controllers first.** The single URL pattern `/services/java/{project}/{*classPath}` (+ `/public/java/...`) funnels into `dispatch(httpMethod, project, classPath, req, resp)` which (1) tries `ControllerRouter.match` and invokes via `ControllerInvoker` if it hits; (2) falls through to `JavaClassRegistry.find` + `JavaHandler.handle`; (3) returns 404 otherwise. The handler path keeps the TCCL swap; controllers reuse their long-lived instance.
- **Controller routing is Spring-style.** Base path is the class FQN with slashes (`demo/CountryController`); each `@Get("/list")`/`@Get("/{id}")`/etc. annotation supplies a suffix. `ControllerRouter` picks the longest matching basePath (so nested-package controllers win over their outer namespaces) and then the most specific route within (literal paths beat `{placeholder}` patterns via `PathPattern.specificity`). Path placeholders compile to named regex groups; `TypeCoercer` handles String/int/long/UUID/enum/boolean conversion at bind time and surfaces parse failures as `400`. `@Body` deserializes via Spring's primary `ObjectMapper`. Return values: `void` → write-it-yourself, `String`/`CharSequence` → `text/plain`, everything else → Jackson JSON.
- **`@Roles` mirrors `UserFacade.isInRole` semantics** without pulling `api-security` (which transitively brings `engine-javascript`). `ControllerInvoker.checkRoles` short-circuits on `Configuration.isAnonymousModeEnabled()`/`isAnonymousUserEnabled()`, then on the `DEVELOPER`/`ADMINISTRATOR` super-roles, then iterates the declared roles via `HttpServletRequest.isUserInRole`. Method-level `@Roles` overrides class-level for that method only.
- **Auto-generated OpenAPI.** `JavaControllerOpenApiPublisher` reflects each controller class into a minimal OpenAPI 3 JSON fragment and saves it as an `OpenAPI` artefact at location `java-controller://<project>::<fqn>` via `OpenAPIService`. The existing aggregator at `OpenAPIEndpoint.getVersion()` (`/services/openapi`) merges every stored artefact — Java-controller fragments appear alongside TS-controller fragments without further wiring. Body/return-type schemas are conservative (`object`/`array`/scalar); the hook is in place to enrich later.
- **`data-store-java` runs Hibernate in dynamic-map mode** — `session.save(entityName, Map<String, Object>)` rather than `session.save(typedBean)`. Hibernate never has to load the user's `Class<?>`, which sidesteps the cross-classloader gymnastics. `JavaEntityStore` provides a typed CRUD API to clients; bean ↔ Map conversion (with column-name / `@Transient` handling and JDBC-type coercion) lives in `EntityBeanMapper`. The SessionFactory is rooted at `DataSourcesManager.getDefaultDataSource()` (the user-data DB), not SystemDB.
- **HBM XML serializer is reused** from `data-store`: `JavaEntityToHbmMapper` (in `data-store-java`) reflects over annotations and feeds `HbmXmlDescriptor` / `HbmPropertyDescriptor` from `data-store`. If you change the HBM serializer for one, audit both.
- **Reaching platform beans from client code.** Client classes are loaded via `ClientClassLoader`, not Spring-scanned, so `@Autowired` is a no-op on them. Use `BeanProvider.getBean(...)` (from `components-core-base`) inside controller / handler methods to fetch `JavaEntityStore`, `IRepository`, etc. The recommended client-code pattern is `@Inject CountryRepository` — see [`dirigiblelabs/sample-java-entity-decorators`](https://github.com/dirigiblelabs/sample-java-entity-decorators); `JavaEntityDecoratorsSampleProjectIT` clones and exercises that repo end-to-end.

## Intent layer (`engine-intent` + `editor-intent`)

A single `app.intent` YAML file at a project root is the source of truth one altitude above the model files. **The intent is an authoring artifact, not a runtime artifact** — like the `.edm` it has an editor and an explicit Generate, and (like the `.edm`) it has **no synchronizer**. Double-clicking any `*.intent` file opens the Intent Editor (`components/ui/editor-intent`): editable YAML left, live read-only diagram right (mxGraph ER + per-process flowcharts, the same engine the EDM/schema/mapping modelers use), validation inline; the Generate button runs six generators that write `.edm`/`.model`, `.bpmn`, `.form`, `.report`, `.roles` and `.csvim`/`.csv` **into the developer's workspace project at the project root** (the layout of real-world Dirigible application projects) — nothing touches the registry until normal publish, after which the per-artefact synchronizers bring the runtime live as for any project. Services: `POST /services/ide/intent/parse` and `POST /services/ide/intent/generate`. A third editor pane is a **Claude AI assistant** (`POST /services/ide/intent/agent`): it proposes the complete updated `app.intent` via the Anthropic API (key server-side in `DirigibleConfig.INTENT_AI_*`, never sent to the browser), the editor shows a Monaco diff, and Accept merges it into the buffer — the agent never writes disk or runs Generate. Developed on PR [#6017](https://github.com/eclipse-dirigible/dirigible/pull/6017).

**Detailed guide:** [`components/engine/engine-intent/CLAUDE.md`](components/engine/engine-intent/CLAUDE.md). Read it before changing anything under that module — it covers the editor-first architecture and altitude contract (model files only, never code), the YAML schema and its semantics (integer-only primary keys, `composition: true` to-one = DEPENDENT master-detail while `required` alone is just a NOT NULL FK, PascalCase property names with UPPER_SNAKE columns, decision `then`/`else`, intent-prefixed table names via `IntentNaming`), the `writeModelFile`-only write surface with the stale-output scrub, the wrong turns already made (wrong altitude, template-output paths, registry-relative vs repository-absolute paths, the `JsonHelper` Gson pitfall, **and the synchronizer-based first incarnation — do not reintroduce it**), and the follow-up list (chaining model-to-code via `.gen` descriptors, `/custom/` escape hatch). Process triggers (`trigger: { onCreate: <Entity> }`) are wired: the EDM adds a `ProcessId` field + a `triggers` collection to the `.model`, and the `template-application-events-java` template generates a `gen/events/<Process>Trigger.java` listener that starts the process on create. `IntentEngineIT` is the HTTP-only end-to-end test (~1 minute, no sync cycles). The editor's diagram pane is **mxGraph** (replacing Mermaid, which had unfixable light/dark theming bugs) with a fixed brand-colour palette that reads on both themes — see the module guide's "Intent Editor diagram = mxGraph" section before touching `editor-intent/js/editor.js`.

**The general platform line this enshrines:** authoring artifacts (`.edm`, `.model`, `.form`, `.report`, `.intent`) get **workspace editors + an explicit Generate**; only runtime artifacts (`.roles`, `.bpmn`, `.csvim`, `.table`, jobs, listeners, …) get **synchronizers**. Applying the synchronizer hammer to an authoring artifact generates into the registry where no modeler, Projects view, or template can use it — that mistake was made once and reverted; the inventory of synchronizers (grep `extends BaseSynchronizer`) deliberately contains no authoring formats.

## Native applications (`engine-native-apps`)

User projects can declare a `*.nativeapp` JSON file that turns an external web server — local OS process or remote HTTP(S) endpoint — into a first-class Dirigible artefact, reverse-proxied under `/services/native-apps-proxy/v1/<basePath>/...` with optional Dirigible-managed authentication and role-based access. The whole feature lives in `components/engine/engine-native-apps`.

**Detailed guide:** [`components/engine/engine-native-apps/CLAUDE.md`](components/engine/engine-native-apps/CLAUDE.md). Read it before changing anything under that module — it covers the synchronizer model, kinds + start modes, port resolution, the stop / teardown contract (three layered guarantees), the dual-DELETE rehydrate requirement, PID + port logging, the proxy filter chain, placeholder expansion, the credentials field naming, the management endpoint's role policy, integration-test patterns including the no-manual-cleanup rule, and the macOS gotchas (Python 3.14 bind regression, wildcard-vs-loopback `ServerSocket` probe, `PollingWatchService` deadlock).

## Conventions worth knowing

- **Always write production-ready Java.** Every change must be production-grade and follow accepted Java best practices and clean-code principles — never prototype-quality, throwaway, or "good enough for now" code. This means: clear intention-revealing names; small, single-responsibility methods and classes; correct visibility and immutability (prefer `final`, avoid leaking mutable state); proper resource handling (try-with-resources); precise exception handling (no swallowed exceptions, no catch-and-ignore, fail at the right boundary); no dead code, no unused parameters/fields, no commented-out blocks; thread-safety where the runtime demands it; and defensive validation only at real system boundaries (trust internal invariants — see the global guidance against over-engineering). Favour the simplest correct design over cleverness, and keep behaviour and naming consistent with the surrounding module. Apply this standard by default to all Java in this repo without being reminded.
- **Constructor injection only.** Every Spring bean (`@Component` / `@Service` / `@Repository` / `@Configuration` / `@RestController` / synchronizer / filter / scheduler) receives its collaborators through the constructor and holds them in `private final` fields. No `@Autowired` (or `@Inject`) on fields, no setter injection. With a single constructor, omit the redundant `@Autowired` on it. `AuthenticationInjectorRegistry` in `engine-native-apps/auth/` is the canonical example — list-typed multi-bean intake plus an immutable derived map, all in the constructor. **Documented exception:** Quartz `Job` implementations. `AutoWiringSpringBeanJobFactory.createJobInstance` instantiates jobs via `clazz.getDeclaredConstructor().newInstance()` and then calls `AutowireCapableBeanFactory.autowireBean(job)`, which only handles property/field/setter autowiring — never constructor args. `JobHandler`, the `DirigibleJob` hierarchy, and `NativeAppMonitorJob` keep `@Autowired` on collaborator fields and document the Quartz instantiation contract in a class-level comment. Do the same when adding a new Quartz Job; don't try to rewire the factory.
- **Package-private by default.** Drop the `public` modifier from any class, interface, or enum used only within its own Java package — including its constructors and methods where the smaller surface is reachable. Cross-package use within the same Maven module still requires `public` (Java packages don't nest, so `org.eclipse.dirigible.x.auth` cannot see a package-private type from `org.eclipse.dirigible.x.proxy`). The SPI types other packages or modules extend stay `public`; the Spring-internal `@Component`s wired by other beans in the same package, helper classes, filter implementations referenced only by their router config, and DTOs consumed only by their endpoint become package-private. `engine-native-apps/proxy/*` is wholly package-private; `engine-native-apps/auth/` keeps the `AuthenticationInjector` SPI public but demotes `BasicAuthenticationInjector`. **Documented exceptions:** Quartz `Job` classes (Quartz's `Constructor.newInstance` call from `org.quartz.*` fails the language-access check on a package-private class) and Spring Data JPA repositories (Spring Data scans classpath-wide; package-private is brittle in practice).
- **Always log the throwable.** Every `catch` block's log call must pass the exception as the trailing SLF4J argument so the stack trace lands in the log — at every level, including DEBUG. Never log only `ex.getMessage()` and drop the rest. SLF4J's parameterised form `logger.X(format, …, throwable)` treats the trailing throwable as out-of-band; the `{}` placeholder count vs. format-arg count stays balanced. Even for cases where the exception "feels expected" (an `IOException` in a daemon log pump that just means the child closed its pipe), keep the throwable so an operator who bumps the level can investigate. Skip the throwable only when there is genuinely no exception in scope (validation failures detected via `if`, not `try`).
- **Code formatting is enforced — always run `mvn formatter:format` before every commit and push.** This is mandatory: the `code-style` CI job (`.github/workflows/build.yml`) runs `mvn formatter:validate` and fails the build on any violation, so unformatted code will not merge. Run `mvn formatter:format` (scoped to the changed module with `-pl` for speed) as the final step before staging, every time. The IDE setup (Eclipse / IntelliJ / VS Code) for importing `dirigible-formatter.xml` is documented in `CONTRIBUTING.md`.
- **Javadoc has to compile cleanly on every Maven module that participates in the release** (anything under `components/`, `modules/`, `dependencies/`, `build/`, `tests/tests-framework`, `cli/`). The release profile attaches the `maven-javadoc-plugin:jar` execution and treats any `error:` from javadoc as fatal; the `default` profile silently skips javadoc, so problems are invisible in normal `mvn install` and only surface in `.github/workflows/release.yml`. **Run `mvn -P release -Dgpg.skip=true -DskipTests -Dlicense.skip=true -Dformatter.skip=true install` on the modules you've touched before pushing anything that adds or changes javadoc.** The recurring failure modes:
  - **Unterminated inline tag.** `{@code` / `{@link` parse braces inside their argument as inline-tag delimiters. `{@code {{ }}}` — the inner `{{` opens a phantom tag and javadoc can't find the matching `}` until the next paragraph break. Escape with `<code>&#123;&#123; &#125;&#125;</code>` or rephrase to avoid braces inside braces.
  - **`@link` reference not found.** `{@link some.external.Type}` only resolves when that type is on the javadoc tool's classpath. JUnit Jupiter, Lombok, optional-test deps and friends are typically not. Use `{@code some.external.Type}` (plain text formatting, no resolution) when the reference is purely documentation; keep `{@link …}` only for types this module compiles against.
  - **`no comment` warning.** Add a one-liner Javadoc to every `public` member of an exported SDK class — javadoc 17+ promotes these to errors under `-Xdoclint:all`, and even today they look untidy in the published API docs.
  Default to writing concise Javadoc on every `public` SDK / API class and method (one sentence is fine), since these are the surfaces user projects link against and the only ones whose docs get published. Internal Spring beans, IDE plumbing, and tests don't need it — comments on intent over comments on shape, per the global rule.
- **License headers are checked.** Every Java/JS/properties file carries the EPL-2.0 header. New files should include it; `mvn license:format -P license` will add or refresh it. Most local-iteration profiles set `license.skip=true`, but the default `mvn install` does not.
- **Integration tests boot the full Spring app and drive Chrome via Selenide.** Pass `-D selenide.headless=true` to run them on CI/headless machines. Screenshots end up in `tests/tests-integrations/build/reports/tests`.
- **UI module resources are bundled into the `build/application` fat jar — a running instance does NOT hot-reload them.** After editing anything under `components/ui/*` or other `META-INF/dirigible/**` web resources, you must `mvn -P quick-build install -pl <module>` **and** `mvn -P quick-build package -pl build/application`, then restart `java -jar build/application/target/...jar`, before a locally running instance reflects the change. Integration tests repackage `tests-integrations` independently, so a **green IT never proves the user's running jar is current** — when a user reports "nothing changed," verify the served resource first (e.g. `curl -s -u admin:admin http://localhost:8080/services/web/<module>/...js | grep <new-token>`).
- **DB-specific behavior is covered by parametrized CI.** `build.yml` runs the integration suite three times — H2 (default), PostgreSQL 16, and MSSQL 2022 — by varying the `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars. When touching SQL or schema-emission code, replicate this locally for the affected DB rather than assuming H2 behavior generalizes.
- **WebJars / `dirigiblelabs` modules.** Some IDE-side modules (names starting with `ide-`, `api-`, `ext-`) historically lived as separate repos under [dirigiblelabs](https://github.com/dirigiblelabs); per `CONTRIBUTING.md` step 8 you may need `mvn clean install -Pcontent` to pull their latest content if working across them.
- **`*IT.java` vs `*Test.java` matters** — failsafe picks up the former, surefire the latter. Putting an integration test under a `*Test` name will silently run it during the wrong phase (and likely without the test app context).
- **Configuration goes through `DirigibleConfig` / `Configuration`** (`modules/commons/commons-config`). When introducing a new tunable, add the enum entry there with a `DIRIGIBLE_*` env-var key and a sensible default — don't read env vars or `System.getProperty` ad-hoc.
- **Spring beans glue everything together; `StaticObjects` is legacy.** Some code paths still grab dependencies via `StaticObjects.get(...)` (e.g. `RepositoryConfig` registers `IRepository` there explicitly) because parts of the runtime predate Spring. New code should rely on constructor injection; don't add new `StaticObjects` keys.
- **Bean-definition overriding is enabled** (`spring.main.allow-bean-definition-overriding=true` in `application-common.properties`), so a duplicate `@Bean` name will silently shadow another. Be deliberate about bean names.
- **JPA scan packages are `{ "org.eclipse.dirigible.components", "org.eclipse.dirigible.engine" }`** in `DataSourceSystemConfig` (both `@EnableJpaRepositories(basePackages = …)` and the `dirigible.scan.packages` property default). New artefact-bearing modules under either tree are picked up automatically; modules under other roots won't be.
- **Cross-module FK cascades use `@OnDelete(action = OnDeleteAction.CASCADE)`.** `core-tenants` depends on `engine-security` (for `Role`), not the reverse, so a JPA-level bidirectional cascade from `Role → UserRoleAssignment` would form a module cycle. `UserRoleAssignment.role` instead carries `@org.hibernate.annotations.OnDelete(action = OnDeleteAction.CASCADE)`, which makes Hibernate emit `ON DELETE CASCADE` on the FK at schema-generation time and the DB drops the assignment row when its role is deleted. Without this, removing a `.roles` artefact whose role was still assigned to a user tripped `FK… DIRIGIBLE_USER_ROLE_ASSIGNMENTS → DIRIGIBLE_SECURITY_ROLES` and the role row survived in the DB out of sync with its missing source file. `RoleSynchronizerCleanupIT` covers it end-to-end. **Caveat for migrations:** Hibernate only emits the cascade when the FK is *created*; existing deployments keep the old un-cascading constraint until the table is rebuilt. Apply the same pattern for any new artefact-bearing entity that owns an FK to a synchronizer-managed parent.
- **Spring Boot 3 fat-jar classpath is fragile.** Reading `BOOT-INF/lib/*.jar` resources via `ClassLoader.getResourceAsStream` — or via aggressive scanners like ClassGraph — closes pooled `NestedJarFile` handles inside `LaunchedClassLoader` and causes cascading `NoClassDefFoundError`s in unrelated platform code. If you need the platform classpath (e.g. as input to `javac --class-path`), follow `engine-java.runtime.ClassPathIndex`: open the outer fat jar with the standard `java.util.jar.JarFile`, extract `BOOT-INF/lib/*.jar` + `BOOT-INF/classes/` to a temp dir once, and use those on-disk paths. Don't introspect nested jars in-process.
- **HTTP-driven integration tests are an option.** Most existing ITs extend `UserInterfaceIntegrationTest` and drive the IDE via Selenide — heavy and slow. For a feature you can exercise purely over HTTP, extend `IntegrationTest` instead, write fixture files directly through `IRepository.createResource(...)`, call `SynchronizationProcessor.forceProcessSynchronizers()` to trigger reconciliation synchronously, and assert via `RestAssuredExecutor.execute(callable, timeoutSeconds)` (the retry-on-AssertionError overload absorbs the small async gap between sync-return and dispatch). See `JavaEngineIT` for the pattern; it runs headless without Chrome. `forceProcessSynchronizers()` is reliable since the engine-intent PR: it used to silently no-op when the scheduled `SynchronizationJob` was mid-run (the root cause of `LocalNativeAppLifecycleIT`-style flakes); it now retries until a full pass has actually consumed the force flag (bounded at 5 minutes).
- **Synchronizer artefact locations are registry-relative; `IRepository` paths are repository-absolute.** `SynchronizationWalker` strips the registry folder, so an artefact's `location` looks like `/myproject/file.ext` — but to read or write that file through `IRepository` you must prepend `IRepositoryStructure.PATH_REGISTRY_PUBLIC`. Code that confuses the two writes outside `/registry/public`, where no synchronizer, IT assertion, or Registry view will ever see the output (engine-intent shipped this bug once — see its CLAUDE.md "wrong turns"). The same convention shows up in `SynchronizationProcessor`'s cleanup pass and `CsvimProcessor.getCsvResource`.
- **`JsonHelper`/`GsonHelper` exclude fields without `@Expose` — and pretty-print.** Their Gson is built with `excludeFieldsWithoutExposeAnnotation()`, so `fromJson` into a POJO whose fields lack `@Expose` silently produces an all-null object (no exception — engine-intent's parser "worked" while generating nothing). Map-shaped data is unaffected (maps aren't field-reflected). For POJO mapping either annotate every field or use a private plain `Gson`; when asserting generated JSON content in tests remember the output is pretty-printed (`"key": "value"` with a space).
- **Stale H2 files break IT runs after a killed JVM.** The file-backed H2 under `tests/tests-integrations/target/dirigible` survives a killed test run and the next context fails to load with `missing artefact with name: [DefaultDB]` or `Table ... not found (this database is empty)` cascades. `rm -rf tests/tests-integrations/target/dirigible` before re-running locally.
- **Sample-project tests live in `tests/ui/tests/sample/`** — each extends `SampleProjectRepositoryIT` and overrides `getRepositoryURL()` + `verifyProject()`. `SampleProjectRepositoryIT` clones the repo through the IDE Git perspective, calls `Workbench.publishAll(true)`, and runs `forceProcessSynchronizers()` before delegating to `verifyProject()`. UI-based (Selenide → Chrome), slower than HTTP-only ITs. Inventory of sample repos under `dirigiblelabs/*`: `sample-entity-decorators`, `sample-java-entity-decorators`, `sample-roles-decorator`, `sample-job-decorator`, `sample-listener-decorator`, `sample-extension-decorator`, `sample-component-decorator`, `sample-websocket-decorator`, `sample-store-api`, `sample-intent-model`. When adding a new sample-project IT, drop the project in its own `dirigiblelabs/*` repo first, then reference the clone URL from the test. `IntentEditorLoadsIT` is not a `SampleProjectRepositoryIT` subclass (the intent generates in the workspace, not on publish) but uses the same clone-a-real-repo pattern: it clones `dirigiblelabs/sample-intent-model`, opens its `app.intent`, and drives the editor's Generate.
- **Spring Boot 4 strips `ResponseStatusException.getReason()`** from the default JSON error body even with `server.error.include-message=always` in `application-common.properties`. Status code reaches the client correctly; the reason text doesn't. ITs asserting 404 must check `statusCode` only, not body content (`JavaEngineIT.delete_unregisters_handler` and `compile_error_keeps_endpoint_unregistered` learned this the hard way — see commit `f13c8c219e`).
- **The client-Java effort is split across three repositories.** The platform code (engine-java, data-store-java, IT) ships in this repo via PR [#5923](https://github.com/eclipse-dirigible/dirigible/pull/5923). The sample project that `JavaEntityDecoratorsSampleProjectIT` clones lives in [`dirigiblelabs/sample-java-entity-decorators`](https://github.com/dirigiblelabs/sample-java-entity-decorators) (initial content from PR [#1](https://github.com/dirigiblelabs/sample-java-entity-decorators/pull/1)). The announcement blog "Return of the Java – Decorators Awaken in Eclipse Dirigible" (sister piece to the December 2025 TS decorators post) goes through the docs portal in PR [`dirigible-io/dirigible-io.github.io#123`](https://github.com/dirigible-io/dirigible-io.github.io/pull/123). Follow-up Java-runtime features generally touch the same three places.

## Browser UI — BlimpKit gotchas

The IDE shell and most editor perspectives render through **BlimpKit**, a thin AngularJS-on-Fundamental-Styles component library that lives in `components/ui/platform-core/src/main/resources/META-INF/dirigible/platform-core/ui/blimpkit/` (Angular module name **`blimpKit`** — camelCase, declared in `blimpkit.js`). The runnable artifact is the bundled `/webjars/blimpkit__blimpkit/dist/blimpkit.min.js` (~158 KB, currently webjar 2.1.6). Findings below are the ones that have already burned someone — read once, save hours later.

- **`<bk-checkbox>` is invisible without `<bk-checkbox-label>`.** `bk-checkbox` compiles to a bare `<input type="checkbox" class="fd-checkbox">`. Fundamental-Styles' `.fd-checkbox` rule hides the native input (`opacity:0; position:absolute`) on the assumption that a sibling `<bk-checkbox-label>` will draw the visible square via its `.fd-checkbox__checkmark` ::before pseudo. A lone `<bk-checkbox>` is therefore a working click target with zero visible chrome — easy to ship and never catch in code review. Pair it: `<bk-checkbox id="x" ng-model="…">` followed by `<bk-checkbox-label for="x" empty="true">…</bk-checkbox-label>` (the `empty="true"` attribute drops the inner text container so the label provides just the checkmark — use it when the surrounding markup already labels the row).
- **`<bk-dialog>` has an isolate scope.** You can't put `ng-controller="…PopupCtrl"` on the dialog element itself — Angular throws "Multiple directives [bkDialog, ngController] asking for new/isolated scope on: <bk-dialog>". Wrap with a thin `<div ng-controller="…">` and put `<bk-dialog visible="…">` inside.
- **`<bk-select>` doesn't support `ng-options`.** Use `<bk-option ng-repeat>` instead — text via the `text` attribute, model value via `value`. Example: `<bk-option ng-repeat="opt in items" text="{{opt.name}}" value="opt.id">`. When the select sits in a parent with `overflow:hidden` (a dialog, a sidebar), add `dropdown-fixed="true"` so the menu floats via `position:fixed` instead of being clipped.
- **`<bk-option>`'s `text` and `value` bind differently** — `text: '@'` is **interpolation** (use `text="{{ expr }}"` or a literal), `value: '<'` is a **one-way expression** (use `value="expr"`, never `value="{{ expr }}"`). Mixing them up is the canonical bug for this directive:
  - `value="{{s}}"` makes Angular try to parse `{{s}}` as a JS expression, the directive's link silently fails, and the dropdown shows raw `{{ text }}` from the unlinked template (one ghost item per ng-repeat iteration, not six). Fix: `value="s"`.
  - `value="user"` evaluates `$scope.user`, not the string `"user"` — every option ends up with the same `undefined` value and selection becomes a no-op. For string literals, quote inside: `value="'user'"`. For the empty default option, `value="''"`, not `value=""` (which is the undefined-expression).
  - Numeric literals (`value="2"`) and loop variables (`value="s"`) are already expressions — leave them unquoted. Numbers stay numbers, so `selectedValue === '2'` will fail; either store as numbers on the model or coerce in the controller (the refresh-interval dropdowns in `view-jvm-monitoring` / `view-jvm-threads` `parseInt` the model on read).
- **Perspective SVG icons inherit `fill` from CSS — don't hard-code `fill` on the path.** `blimpkit.css` styles `.fd-list__navigation-item i.bk-icon--svg svg` with `fill: var(--fdVerticalNav_Icon_Color, #303030)` (and `var(--sapSelectedColor)` on the active state). The CSS only takes effect on `<path>` elements with **no own `fill`** — adding `fill="#000000"` (the default when you paste an SVG from a web icon set) locks the icon to black and breaks dark-theme adaptability. Strip the fill attribute (jobs.svg / operations.svg pattern) or set `fill="currentColor"` (database.svg pattern). The container svg's other niceties (`width="512"` / `height="512"` / `stroke-width=".99999"`) don't affect rendering through this CSS but are the established style.
- **`<bk-input>` / `<bk-textarea>` / `<bk-button>` use `replace:true`.** The attributes you write on the directive element (ng-model, ng-blur, ng-keypress, ng-disabled, custom directives like `auto-focus` / `select-text`) end up on the underlying native `<input>` / `<textarea>` / `<button>`, so existing controller code keeps working unchanged after migrating native form controls to `bk-*`. ng-model binds against the parent scope — the isolate scope `bk-input` declares only owns `compact` / `state` / `glyph`.
- **Don't put `ng-class` on a `replace:true` directive element that already has its own `ng-class`.** `bk-table-header-cell`, `bk-table-cell`, and most layout-y BlimpKit directives template as `<th ng-class="getClasses()" …>` — Angular's attribute merge **string-concatenates** duplicate `ng-class` values, producing nonsense like `ng-class="{ sorted: sort.key === 'id' } getClasses()"`. The page then throws `$parse:syntax` at compile time and the row never renders. (`class` merges cleanly — only `ng-class` is broken — so `class="no-sort"` on a `<th bk-table-header-cell>` works fine.) The fix: push the conditional class onto a child element instead of the directive root: `<th bk-table-header-cell ng-click="…"><span class="sort-caret" ng-class="{ active: sort.key === 'id' }">{{ caret() }}</span></th>`. Same applies for anything else with a `replace:true` + `ng-class` template (audit `components/ui/platform-core/.../blimpkit/*.js` for the pattern before adding `ng-class` to a `bk-*` directive).
- **The `blimpKit` module's `.config()` block disables three `$compileProvider` flags.** `cssClassDirectivesEnabled(false)`, `commentDirectivesEnabled(false)`, and `debugInfoEnabled(false)` are flipped at module-load when debug info was on — saves per-element scope-tracking overhead in production. The last flag breaks Selenide-style debugging that calls `angular.element(node).scope()`: Angular stops attaching scope refs to DOM nodes, so the lookup returns `undefined`. If your app or its integration tests rely on that, re-enable the flags in a `.config(['$compileProvider', …])` block of your own — module config blocks run in dependency order, so `blimpKit`'s flips happen first and your override sticks.
- **SAP-icons + the "72" body font live in platform-core's `fonts.css`.** Every BlimpKit-using page needs `<link rel="stylesheet" href="/services/web/platform-core/ui/styles/fonts.css">`. Without it `.sap-icon--*` glyphs render as tofu squares because the `@font-face { font-family: "SAP-icons"; … }` declaration is missing. The IDE shell loads this automatically via the `platform-links` injection mechanism (see below); standalone iframes (editor-bpm, embedded views) have to add the link tag explicitly. Other `@font-face` rules in the same file declare the body font: `"72"` (Regular / Light / Bold), `"72-Light"`, `"72-Bold"`, `"72Mono-Regular"`, `"72Mono-Bold"`, plus `"BusinessSuiteInAppSymbols"` and `"SAP-icons-TNT"`.
- **`<meta name="platform-links" category="…">` auto-injects scripts + stylesheets.** Looking at any non-iframe perspective HTML you'll see a single `<meta name="platform-links" category="ng-view,ng-perspective">`-style tag in the `<head>`. `HtmlPlatformLinksInjector` (in `components/engine/engine-web/.../HtmlPlatformLinksInjector.java`) reads it at request time, walks the `category` list, and replaces the meta tag with the bundle of `<link>` and `<script>` tags registered for those categories. Categories are defined in `components/engine/engine-web/src/main/resources/platform-links.json` — `ng-view` is the heavyweight bundle (jQuery, AngularJS, all the platform hubs, BlimpKit, Fundamental-Styles, fonts.css), `ng-perspective` adds split + layout, `ng-editor` adds workspace + repository hubs, etc. Adding new shared platform code → add it to this JSON, not to every perspective HTML.
- **`<bk-dialog>` toggles visibility via the `visible` binding, not a `.modal('show')` plugin.** `<bk-dialog visible="modal.visible">` watches the expression and adds `fd-dialog--active` when true. No backdrop element is added (the dialog's own `.fd-dialog--active` overlay handles z-index + dimming). To dismiss programmatically: flip the bound flag (`scope.modal.visible = false`) inside an `$apply`; let the directive's digest cycle remove the `--active` class; then `$timeout` ~300ms later before tearing down the scope so the close animation completes.
- **Test selectors after a BlimpKit migration.** Native `<input class="form-control">` → `<input class="fd-input fd-input--compact">`. `<div class="modal in">` (Bootstrap-3 visible) → `<section class="fd-dialog fd-dialog--active">`. `body.modal-open` and `.modal-backdrop` are NOT set by `<bk-dialog>` — drop assertions on those, the active overlay handles its own dimming. When fixing Selenide tests that look at `.modal-header .close`, switch to `.fd-dialog__header .fd-button` (or scope to the dialog with `section.fd-dialog--active button.fd-button`).
- **A `<split>` splitter needs the `platformSplit` module in the app's dependency list — loading the script is not enough.** The `<split>`/`<split-pane>` resizable-pane directives are defined in Angular module `platformSplit` (`platform-core/ui/platform/split.js`). The script + `split.css` are already bundled by the `ng-perspective` (and `ng-split`) `platform-links` categories, so a perspective that declares `ng-perspective` does NOT also need `ng-split`. But every app must still list `'platformSplit'` in its `angular.module('app', [...])` deps, or the directives never register: `<split>`/`<split-pane>` stay inert unknown elements and the layout collapses (one pane fills everything, the others vanish — with no console error). Working examples: `editor-csvim`, `perspective-settings`, `resources-inbox`. Layout: `.bk-split` is `height:100%`, so under a persistent `<bk-toolbar>` in a `bk-vbox` body wrap the split in `<div class="bk-stretch">` (see `resources-documents`); wrap each pane's content in `<div class="bk-vbox bk-fill-parent">`; `split-pane size` values should sum to 100; the gutter replaces any manual `bk-border--*`.

## Java Debugger (`ide-java-debug` + `view-java-debug`)

Implemented in PR [#5948](https://github.com/eclipse-dirigible/dirigible/pull/5948). Bridges the browser IDE to a JDWP-enabled JVM via the Debug Adapter Protocol (DAP).

### Architecture

```
Browser (debug.js) ←WebSocket→ JavaDebugWebSocketHandler
                                  ↓ JavaDebugManager
                                  ↓ JavaDebugBridge (per workspace)
                                  ↓ DAP TCP socket
                                  JDT.LS (vscode.java.startDebugSession)
                                  ↓ JDWP TCP (port 8000 by default)
                                  Target JVM
```

**Key classes:**
- `JavaDebugManager` (`components/ide/ide-java-debug`) — singleton Spring bean; one `JavaDebugBridge` per `username/workspace` key. `getOrStart()` is the entry point: asks JDT.LS for a DAP TCP port via `workspace/executeCommand → vscode.java.startDebugSession`, then creates the bridge. `removeSession()` tears down the bridge when the last WebSocket session leaves.
- `JavaDebugBridge` — reads Content-Length-framed DAP messages from the TCP socket and broadcasts them as raw JSON to all WebSocket sessions. Translates `setBreakpoints` source paths from virtual workspace paths (`/workspace/proj/Foo.java`) to real filesystem paths before forwarding to the DAP server. **`destroy()` sends `disconnect` with `terminateDebuggee:false` before closing the socket** — without this the DAP adapter calls `VM.exit()` and kills the JVM, making the JDWP port refuse the next attach.
- `JavaDebugWebSocketHandler` — Spring WebSocket handler at `/websockets/ide/java-debug?workspace=<name>`. On open: `manager.getOrStart(username, workspace).addSession(session)`. On message: `bridge.sendToDap(json)`. On close: `manager.removeSession(sessionId)`.
- `JavaDebugConfigEndpoint` — `GET /services/ide/java-debug/config` → `{"jdwpPort": N}`. The default port is `DirigibleConfig.JAVA_DEBUG_JDWP_PORT` (`DIRIGIBLE_JAVA_DEBUG_JDWP_PORT`, default `8000`).

**OSGi requirement:** `com.microsoft.java.debug.plugin-*.jar` must be in the JDT.LS `plugins/` directory **and** listed in every `config_<platform>/config.ini` `osgi.bundles` entry. `JdtLsManager.installDebugPlugin()` handles both — it reads `Bundle-Version` from the jar's manifest and appends the entry. Without this, `vscode.java.startDebugSession` returns "unknown command".

**Path translation:** the browser sends virtual paths like `/workspace/proj/Foo.java`; the DAP server needs real paths like `/home/user/.dirigible/repository/root/users/admin/workspace/proj/Foo.java`. `JavaDebugBridge.translateSourcePaths()` rewrites `setBreakpoints` requests. In the reverse direction, `realToVirtualPath()` in `debug.js` uses `lastIndexOf('/' + workspaceName + '/')` to strip the server prefix from stack-frame source paths.

**Server-side LSP init:** `JdtLsInstance.ensureInitialized()` sends `initialize` + `initialized` to JDT.LS without a browser editor session so that `workspace/executeCommand` is not queued forever waiting for a client. Timeout: 60 s for `ensureInitialized`, 90 s for `startDebugSession` (CI runners are slow).

### Frontend (`components/ui/view-java-debug`)

- **Region:** `left` (sits alongside Projects/Import/Search in the left sidebar)
- **Config:** `configs/debug-view.js` — `region: 'left'`, `lazyLoad: true`
- **Layout:** compact toolbar (Attach, Disconnect, step controls) + status strip + three collapsible sections (Call Stack, Variables, Breakpoints)
- **Workspace:** read from `localStorage[${getBrandingInfo().prefix}.workspace.selected]` on load; kept in sync via `platform.workspace.changed` hub topic (same source as the Projects view)
- **Port:** fetched from `GET /services/ide/java-debug/config` on startup; falls back to 8000
- **Host:** always `localhost` — not configurable from the UI
- **Connecting animation:** `$interval` cycles through 18 descriptive phrases every 3 s while `status === 'connecting'`; cancelled on connected / error / disconnect / terminate / `$destroy`
- **Breakpoints persistence:** stored in `localStorage['dirigible.java.debug.breakpoints']` as `{ virtualPath → int[] }`. Restored on load; `refreshBpList()` must be called after restoring to populate `$scope.bpList`.

### Editor integration (`components/ui/editor-monaco`)

- **`java.debug.highlight` topic:** `debug.js` posts `{ filePath: realPath, lineNumber }` when paused; editors match via `filePath.endsWith(myPath)`. Post is repeated after 1500 ms for newly opened tabs. `clearPausedState()` in `dapContinue()` posts `{ filePath: null, lineNumber: 0 }` immediately (the DAP `continued` event is optional and often skipped by the Java adapter for long-running JVMs).
- **`java.debug.breakpoints` topic:** `debug.js` broadcasts the full `breakpoints` map; each editor restores glyph decorations for its own path. After registering its listener the editor immediately posts `java.debug.breakpoints.request` so the debug view re-broadcasts — this handles the race where the debug view's startup broadcast fires before the editor iframe is ready.
- **Glyph CSS** (`editor.css`): `.debug-breakpoint-glyph` — red circle (14 × 14 px, `border-radius:50%`). `.debug-current-line-glyph` — right-pointing yellow triangle (CSS border trick, `#ffcc00`, 12 × 14 px). `.debug-current-line` — faint yellow line background.
- **Glyph margin clicks:** `onMouseDown` with `isGlyphMarginLineNumber` toggles breakpoints and posts `java.debug.breakpoints.changed` with the updated lines array for that file.

## Messaging perspective (`ide-messaging-monitoring` + `perspective-messaging`)

A devops/debug surface for the embedded ActiveMQ broker. Three modules cooperate:

- **`components/ide/ide-messaging-monitoring`** — backend service + REST endpoint. Constructor-injects the existing `BrokerService` bean from `engine-listeners` (the broker is started with `useJmx=false` in `MessagingConfig`, so JMX is unavailable — read state directly off the bean instead). `RegionBroker.getDestinationMap()` enumerates queues and topics; `Destination.getDestinationStatistics()` exposes the counters; `Destination.browse()` returns the in-memory pending set without consuming. Endpoints live under `/services/ide/messaging-monitoring/` with `@RolesAllowed({ADMINISTRATOR, DEVELOPER, OPERATOR})`:
  - `GET /summary` — broker meta + every queue/topic with counters
  - `GET /queues/{name}/messages?limit=N` — non-destructive browse (capped at 200 by default, body preview at 64 KB)
  - `DELETE /queues/{name}/messages` — purge
  - `DELETE /queues/{name}/messages/{messageId}` — remove a single message
  - `DELETE /queues/{name}` and `DELETE /topics/{name}` — remove the destination (returns 409 if consumers are still attached — `IllegalStateException` translated by the endpoint)
- **`components/ui/perspective-messaging`** — perspective shell (id `messaging`, order 1010), icon, menu, extensions.
- **`components/ui/view-messaging-destinations`** (left) and **`components/ui/view-messaging-browser`** (center) — filterable list of destinations and master-detail message inspector. They communicate over `MessageHub` topics `messaging.destination.selected` (selection broadcast), `messaging.destination.selection.request` (browser asks destinations view to re-broadcast on init — fixes the iframe-startup race), and `messaging.broker.refresh` (browser asks destinations view to re-poll after a mutating action). Confirm-dialogs go through `DialogHub.showAlert({type:'confirmation', buttons:[...]})`.

**Topic limitation:** JMS topics do not retain non-persistent messages, so the browser shows live counters but no message list for topics. Message-body decoding prefers `ActiveMQTextMessage.getText()`; for `BytesMessage`-style payloads, the first 64 KB are inspected and rendered as UTF-8 when they look textual, otherwise base64-encoded.

## External documentation

The user-facing help portal at <https://www.dirigible.io/help/> documents the IDE perspectives (Workbench, Database, Git, Operations, Documents), artefact authoring (Jobs, CSVIM, Entity model, OData, Listeners, Camel routes, BPMN), JS/TS API reference (`/api/`), and deployment guides (Docker, Kubernetes, Cloud Foundry). Useful when reading integration-test fixtures under `tests/tests-integrations/src/main/resources/<TestName>/` or understanding what a given artefact extension is supposed to do.

The portal is **a separate repository** — MkDocs-built from <https://github.com/dirigible-io/dirigible-io.github.io> (Markdown sources under `docs-help/docs/`, nav in `docs-help/mkdocs.yml`). Doc fixes go there as PRs, not in this repo. Blog posts (`docs-blogs/docs/YYYY/MM/DD/*.md`) need three coordinated updates per post: the markdown source, `docs/blogs.json` (CI normally regenerates from the last 5 dated `.md` files via `.github/folders.sh`, but a manual edit lights the home page up immediately), and `docs-blogs/mkdocs.yml` nav (year heading + per-post entry). The `docs/` tree is otherwise CI-generated — see the repo's own `CLAUDE.md` and don't hand-edit it for routine changes; `ci skip` in a commit message *suppresses* the regeneration job.

The companion sample repos under `dirigiblelabs/*` are also separate. For end-to-end tests that clone such repos (`SampleProjectRepositoryIT` subclasses), the merge order follows the dependency direction, both ways: a dirigible-side IT that expects new sample content needs the sample-side PR merged first (else it clones an empty `master`), and a sample-side change that uses new platform APIs needs the platform PR merged first — the ITs clone the sample repo's HEAD, so merging the sample early breaks **every** PR's CI and master until the platform lands (this happened with the typed-handler interfaces: sample PR merged a day before platform PR [#6010](https://github.com/eclipse-dirigible/dirigible/pull/6010), and `JavaEntityDecoratorsSampleProjectIT` failed across the board until #6010 merged). Re-running a failed PR check does NOT pick up a newer master — re-runs build the original merge snapshot; use `gh pr update-branch` (or push) to get a fresh merge.

Treat `modules/commons/commons-config/src/main/java/org/eclipse/dirigible/commons/config/DirigibleConfig.java` (the enum) and `Configuration.java` (the allow-list) as the source of truth for env-var names and defaults, not the portal — the env-vars page lags the code by months. Specifically, the in-repo OAuth flow is Spring's `spring.security.oauth2.client.registration.github.*` activated by the `github` profile and configured via `DIRIGIBLE_GITHUB_CLIENT_ID` / `_CLIENT_SECRET` / `_SCOPE` (`build/application/src/main/resources/application-github.properties`), not the generic `DIRIGIBLE_OAUTH_*` entries some doc pages still describe. Current servlet mappings are rooted at `/services/...` and `/public/...` per `BaseEndpoint`; any `/services/v4/...` URL in a doc snippet is legacy.

## CI reference

`.github/workflows/build.yml` is the source of truth for "does this build pass":

- `code-style`: `mvn -T 1C formatter:validate`
- `tests` (ubuntu + windows matrix): `mvn clean install -P unit-tests`
- `integration-tests-h2` / `-postgresql` / `-mssql`: `mvn clean install -P integration-tests` with the matching `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars
- `build-deploy`: `mvn clean install -P quick-build` then Docker buildx multi-arch image push to `dirigiblelabs/dirigible`

`pull-request.yml`, `codeql.yml`, `release.yml` cover PR validation, CodeQL, and Maven Central release respectively.
