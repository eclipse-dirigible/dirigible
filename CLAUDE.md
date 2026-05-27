# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Eclipse Dirigible — a high-productivity application platform (hpaPaaS). The runtime is a single Spring Boot fat jar that bundles an in-browser IDE plus execution engines (GraalJS, Flowable BPM, Camel, Quartz, Lucene, CMS, OData, etc.). It supports "In-System Programming": users develop and modify the running system through the browser. The shipped artifact is `build/application/target/dirigible-application-*-executable.jar`; the entry point is `org.eclipse.dirigible.DirigibleApplication` (`build/application/src/main/java/org/eclipse/dirigible/DirigibleApplication.java`).

## Prerequisites

- Java JDK 21 (project compiles to Java 21; CI uses Corretto 21)
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

## Native applications (`engine-native-apps`)

User projects can declare a `*.native-app` JSON file that turns an external web server — local OS process or remote HTTP(S) endpoint — into a first-class Dirigible artefact, reverse-proxied under `/services/native-apps-proxy/v1/<basePath>/...` with optional Dirigible-managed authentication and role-based access. The whole feature lives in `components/engine/engine-native-apps` (`org.eclipse.dirigible.components.engine.nativeapps`); the management surface lives in `org.eclipse.dirigible.components.engine.nativeapps.endpoint`.

- **Artefact type string `native-app`, file extension `.native-app`, JPA table `DIRIGIBLE_NATIVE_APPS`.** Synchronizer order is `SynchronizersOrder.NATIVE_APP = 440` (after `PROXY = 430`, before `OPENAPI = 510`). `NativeAppSynchronizer` extends `BaseSynchronizer` (single-tenant — apps are platform-global).
- **Two kinds: `local` and `remote`.** Remote = third-party HTTP(S) URL Dirigible doesn't own; the artefact only declares `config.url`. Local = OS process Dirigible spawns and supervises. Local apps carry a `config.lifecycle.start.commands[]` and optional `config.lifecycle.stop.commands[]`, each entry tagged with `os` ∈ `mac`/`linux`/`windows`. `OsCommandResolver` (uses `org.apache.commons.lang3.SystemUtils.IS_OS_*`) picks the entry for the current platform.
- **Start modes (local only): `lazy` vs `always`.** ALWAYS apps are spawned by `NativeAppBootstrap` on `ApplicationReadyEvent` and kept alive by `NativeAppMonitorJob` — a Quartz job scheduled by `NativeAppMonitorScheduler` (cron `DIRIGIBLE_NATIVE_APP_MONITOR_CRON`, default `0/30 * * * * ?`) that iterates LOCAL apps in ALWAYS mode and restarts dead ones. LAZY apps start on the first proxy request via `LazyStartFilter.startAndAwaitReady(app)`. The synchronizer's `START` phase only acts on ALWAYS apps — LAZY must not be pre-started.
- **Port resolution: prefer-then-allocate.** `PortResolver.resolve(preferredPort)` first probes the declared `config.defaultPort` (loopback `ServerSocket` open/close). On `BindException` (or if `defaultPort` is null) it allocates an OS-chosen ephemeral port via `ServerSocket(0)`. The resolved port is exported to the child as `DIRIGIBLE_NATIVE_APP_PORT` — **the spawned process must read that env var** rather than hardcode a port. See [`dirigiblelabs/sample-library-native-app-nodejs`](https://github.com/dirigiblelabs/sample-library-native-app-nodejs)'s `src/config.ts` for the canonical pattern (`env.DIRIGIBLE_NATIVE_APP_PORT ?? env.PORT`).
- **Readiness probe.** After spawn, `NativeAppProcessManager.awaitReady` polls `isLoopbackPortOpen(port)` every 200 ms up to `DirigibleConfig.NATIVE_APP_READY_TIMEOUT_MS` (default 30 s). Bump it via that enum if the spawn needs first-time `npm install` etc — `SampleLibraryNativeAppNodejsIT` sets it to 5 min for that reason.
- **Proxy chain — Spring Cloud Gateway WebMvc, modelled on `engine-proxy`.** `NativeAppProxyRouterConfig` registers `/services/native-apps-proxy/v1/**` with the filter order:
    1. `rewritePath` strips the absolute base.
    2. `NativeAppLookupFilter` resolves `basePath → NativeApp`, rewrites the path to the upstream-relative form, attaches the app as a request attribute. Supports an empty-`basePath` catch-all that matches when no named app catches first.
    3. `ExposedPathFilter` enforces `security.exposedPaths` (404 on whitelist miss) and `scopes` (403 on miss). **Important:** native-app scope semantics are intentionally strict — DEVELOPER/ADMINISTRATOR super-roles do NOT grant implicit access. Authors define their app's audience explicitly. If `scopes` is empty / `security` is null, any authenticated caller passes (auth itself is enforced upstream by Spring Security on `/services/**`).
    4. `LazyStartFilter` spawns LOCAL+LAZY apps on demand.
    5. `NativeAppDispatcher` sets the downstream scheme/host/port: LOCAL → `http://127.0.0.1:<resolvedPort>`, REMOTE → `app.remoteUrl`. The current request path (already rewritten in step 2) is reused by SCG's `http()` handler.
    6. `AuthInjectionFilter` consults `AuthenticationInjectorRegistry` and lets the matching `AuthenticationInjector` mutate the outbound request. `BasicAuthenticationInjector` uses `.headers(h -> h.set(AUTHORIZATION, ...))` because `builder.header(...)` appends rather than replaces — without `.set` the inbound `Authorization` would shadow the injected one.
    7. `removeRequestHeader("Cookie")` + `http()` forward.
- **Placeholder expansion via `Configuration.configureObject`.** `NativeAppParser` walks the typed POJO tree (`NativeAppFile` → `NativeAppConfig` → `Lifecycle`/`Security`/`Authentication`/`BasicAuthCredentials`/each `Command` and `ExposedPath`) and invokes `configureObject` on each owned node — same `${KEY}` / `${KEY}.{DEFAULT}` semantics as Jobs and other artefacts, just done in a recursive walk because `configureObject` only handles top-level fields. The typed config tree is **transient** on the entity; the raw post-expansion JSON is persisted in `NATIVE_APP_CONFIG_JSON` and rehydrated lazily by `NativeAppParser.rehydrateConfig` (used by the registry on cache miss, the synchronizer's lifecycle hooks, and the management endpoint).
- **JSON schema gotcha.** The user-facing `type` field (LOCAL/REMOTE) collides with `Artefact#type` (the artefact-type string) at the Gson layer (`Class declares multiple JSON fields named 'type'`). Solved by a dedicated DTO `NativeAppFile` for deserialization that mirrors the file structure 1:1; `NativeAppParser.parse` then maps the DTO onto the JPA entity. Don't try to deserialize into `NativeApp` directly.
- **Credentials field naming.** `Authentication.credentials.user` and `.password` are the literal username / password to send. Placeholders like `${SAMPLE_APP_USER}.{admin}` are resolved at parse time and persisted. The runtime injector reads the already-resolved values; no per-request env lookup. The management endpoint omits the credentials block from `GET` responses.
- **Management REST** at `/services/native-apps[/<id>][/start|/stop]` — list / get / start / stop / delete. Restricted by `HttpSecurityURIConfigurator.NATIVE_APPS_MANAGEMENT_PATTERNS` to **any of DEVELOPER, ADMINISTRATOR, OPERATOR** — distinct from the proxy itself which uses per-app scope semantics.
- **Three integration-test patterns.** All three live alongside other dirigible-fork ITs and complement each other:
  - `RemoteNativeAppIT` + `RemoteNativeAppTestProject` (`tests/ui/tests/...`, fixture under `tests/tests-integrations/src/main/resources/RemoteNativeAppIT/`) — `BaseTestProject`/`PredefinedProjectIT` pattern. Boots an in-JVM `HttpServer` for the upstream and uses ProjectUtil's `__UPSTREAM_URL__` build-time placeholder (deliberately NOT `${...}` to avoid colliding with the synchronizer's runtime placeholders). Exercises whitelist hit, whitelist miss, empty-base catch-all, basic-auth header injection, and delete-unregisters.
  - `LocalNativeAppLifecycleIT` (`tests/api/...`, fast HTTP-only) — spawns a tiny Java single-file source HTTP server via `java Server.java` (single-source execution since Java 11). Cross-platform, no Python or Node dependency. Verifies lazy/always start, monitor restart, basic-auth injection, clean shutdown.
  - `SampleLibraryNativeAppNodejsIT` (`tests/ui/tests/sample/...`, slow Selenide+Git-clone) — clones [`dirigiblelabs/sample-library-native-app-nodejs`](https://github.com/dirigiblelabs/sample-library-native-app-nodejs) (which holds `roles.roles`, `project.json`, and `sample-library-native-app-nodejs.native-app`), publishes via the IDE, creates a `library-reader` user, asserts 403, assigns `library-admin` via `SecurityUtil.assignRoleToUser`, asserts 200/201 for CRUD. Bumps `DirigibleConfig.NATIVE_APP_READY_TIMEOUT_MS` to 5 min to absorb the first-spawn `npm install`.
- **Gotchas worth remembering.**
  - `Python 3.14` on macOS has a bind regression in `http.server.HTTPServer` — don't use Python for embedded test servers; use `java Server.java`.
  - `ServerSocket(port, 0, InetAddress.getLoopbackAddress())` can succeed on loopback even when another listener is bound to `0.0.0.0:port` on macOS. If you ever extend `PortResolver`, probe with the wildcard interface to be safe. Until then, this is the reason a stale orphan node app on `0.0.0.0:8080` was caught silently in one test debugging session.
  - `Spring Boot's DevTools` (or whatever `FileSystemWatcher` wires up) can deadlock against `sun.nio.fs.PollingWatchService$PollingWatchKey` on macOS during full IT runs (last seen on `JavaLspIT`, `CreateNewFileIT`, `JavaEntityDecoratorsSampleProjectIT`). This is NOT a native-apps regression. When the full reactor IT run hangs, jstack the surefire JVM and check for `Found one Java-level deadlock` between `main` and `FileSystemWatcher`.

## Conventions worth knowing

- **Always write production-ready Java.** Every change must be production-grade and follow accepted Java best practices and clean-code principles — never prototype-quality, throwaway, or "good enough for now" code. This means: clear intention-revealing names; small, single-responsibility methods and classes; correct visibility and immutability (prefer `final`, avoid leaking mutable state); proper resource handling (try-with-resources); precise exception handling (no swallowed exceptions, no catch-and-ignore, fail at the right boundary); no dead code, no unused parameters/fields, no commented-out blocks; thread-safety where the runtime demands it; and defensive validation only at real system boundaries (trust internal invariants — see the global guidance against over-engineering). Favour the simplest correct design over cleverness, and keep behaviour and naming consistent with the surrounding module. Apply this standard by default to all Java in this repo without being reminded.
- **Code formatting is enforced — always run `mvn formatter:format` before every commit and push.** This is mandatory: the `code-style` CI job (`.github/workflows/build.yml`) runs `mvn formatter:validate` and fails the build on any violation, so unformatted code will not merge. Run `mvn formatter:format` (scoped to the changed module with `-pl` for speed) as the final step before staging, every time. The IDE setup (Eclipse / IntelliJ / VS Code) for importing `dirigible-formatter.xml` is documented in `CONTRIBUTING.md`.
- **License headers are checked.** Every Java/JS/properties file carries the EPL-2.0 header. New files should include it; `mvn license:format -P license` will add or refresh it. Most local-iteration profiles set `license.skip=true`, but the default `mvn install` does not.
- **Integration tests boot the full Spring app and drive Chrome via Selenide.** Pass `-D selenide.headless=true` to run them on CI/headless machines. Screenshots end up in `tests/tests-integrations/build/reports/tests`.
- **DB-specific behavior is covered by parametrized CI.** `build.yml` runs the integration suite three times — H2 (default), PostgreSQL 16, and MSSQL 2022 — by varying the `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars. When touching SQL or schema-emission code, replicate this locally for the affected DB rather than assuming H2 behavior generalizes.
- **WebJars / `dirigiblelabs` modules.** Some IDE-side modules (names starting with `ide-`, `api-`, `ext-`) historically lived as separate repos under [dirigiblelabs](https://github.com/dirigiblelabs); per `CONTRIBUTING.md` step 8 you may need `mvn clean install -Pcontent` to pull their latest content if working across them.
- **`*IT.java` vs `*Test.java` matters** — failsafe picks up the former, surefire the latter. Putting an integration test under a `*Test` name will silently run it during the wrong phase (and likely without the test app context).
- **Configuration goes through `DirigibleConfig` / `Configuration`** (`modules/commons/commons-config`). When introducing a new tunable, add the enum entry there with a `DIRIGIBLE_*` env-var key and a sensible default — don't read env vars or `System.getProperty` ad-hoc.
- **Spring beans glue everything together; `StaticObjects` is legacy.** Some code paths still grab dependencies via `StaticObjects.get(...)` (e.g. `RepositoryConfig` registers `IRepository` there explicitly) because parts of the runtime predate Spring. New code should rely on constructor injection; don't add new `StaticObjects` keys.
- **Bean-definition overriding is enabled** (`spring.main.allow-bean-definition-overriding=true` in `application-common.properties`), so a duplicate `@Bean` name will silently shadow another. Be deliberate about bean names.
- **JPA scan packages are `{ "org.eclipse.dirigible.components", "org.eclipse.dirigible.engine" }`** in `DataSourceSystemConfig` (both `@EnableJpaRepositories(basePackages = …)` and the `dirigible.scan.packages` property default). New artefact-bearing modules under either tree are picked up automatically; modules under other roots won't be.
- **Spring Boot 3 fat-jar classpath is fragile.** Reading `BOOT-INF/lib/*.jar` resources via `ClassLoader.getResourceAsStream` — or via aggressive scanners like ClassGraph — closes pooled `NestedJarFile` handles inside `LaunchedClassLoader` and causes cascading `NoClassDefFoundError`s in unrelated platform code. If you need the platform classpath (e.g. as input to `javac --class-path`), follow `engine-java.runtime.ClassPathIndex`: open the outer fat jar with the standard `java.util.jar.JarFile`, extract `BOOT-INF/lib/*.jar` + `BOOT-INF/classes/` to a temp dir once, and use those on-disk paths. Don't introspect nested jars in-process.
- **HTTP-driven integration tests are an option.** Most existing ITs extend `UserInterfaceIntegrationTest` and drive the IDE via Selenide — heavy and slow. For a feature you can exercise purely over HTTP, extend `IntegrationTest` instead, write fixture files directly through `IRepository.createResource(...)`, call `SynchronizationProcessor.forceProcessSynchronizers()` to trigger reconciliation synchronously, and assert via `RestAssuredExecutor.execute(callable, timeoutSeconds)` (the retry-on-AssertionError overload absorbs the small async gap between sync-return and dispatch). See `JavaEngineIT` for the pattern; it runs headless without Chrome.
- **Sample-project tests live in `tests/ui/tests/sample/`** — each extends `SampleProjectRepositoryIT` and overrides `getRepositoryURL()` + `verifyProject()`. `SampleProjectRepositoryIT` clones the repo through the IDE Git perspective, calls `Workbench.publishAll(true)`, and runs `forceProcessSynchronizers()` before delegating to `verifyProject()`. UI-based (Selenide → Chrome), slower than HTTP-only ITs. Inventory of sample repos under `dirigiblelabs/*`: `sample-entity-decorators`, `sample-java-entity-decorators`, `sample-roles-decorator`, `sample-job-decorator`, `sample-listener-decorator`, `sample-extension-decorator`, `sample-component-decorator`, `sample-websocket-decorator`, `sample-store-api`. When adding a new sample-project IT, drop the project in its own `dirigiblelabs/*` repo first, then reference the clone URL from the test.
- **Spring Boot 4 strips `ResponseStatusException.getReason()`** from the default JSON error body even with `server.error.include-message=always` in `application-common.properties`. Status code reaches the client correctly; the reason text doesn't. ITs asserting 404 must check `statusCode` only, not body content (`JavaEngineIT.delete_unregisters_handler` and `compile_error_keeps_endpoint_unregistered` learned this the hard way — see commit `f13c8c219e`).
- **The client-Java effort is split across three repositories.** The platform code (engine-java, data-store-java, IT) ships in this repo via PR [#5923](https://github.com/eclipse-dirigible/dirigible/pull/5923). The sample project that `JavaEntityDecoratorsSampleProjectIT` clones lives in [`dirigiblelabs/sample-java-entity-decorators`](https://github.com/dirigiblelabs/sample-java-entity-decorators) (initial content from PR [#1](https://github.com/dirigiblelabs/sample-java-entity-decorators/pull/1)). The announcement blog "Return of the Java – Decorators Awaken in Eclipse Dirigible" (sister piece to the December 2025 TS decorators post) goes through the docs portal in PR [`dirigible-io/dirigible-io.github.io#123`](https://github.com/dirigible-io/dirigible-io.github.io/pull/123). Follow-up Java-runtime features generally touch the same three places.

## Browser UI — BlimpKit gotchas

The IDE shell and most editor perspectives render through **BlimpKit**, a thin AngularJS-on-Fundamental-Styles component library that lives in `components/ui/platform-core/src/main/resources/META-INF/dirigible/platform-core/ui/blimpkit/` (Angular module name **`blimpKit`** — camelCase, declared in `blimpkit.js`). The runnable artifact is the bundled `/webjars/blimpkit__blimpkit/dist/blimpkit.min.js` (~158 KB, currently webjar 2.1.6). Findings below are the ones that have already burned someone — read once, save hours later.

- **`<bk-checkbox>` is invisible without `<bk-checkbox-label>`.** `bk-checkbox` compiles to a bare `<input type="checkbox" class="fd-checkbox">`. Fundamental-Styles' `.fd-checkbox` rule hides the native input (`opacity:0; position:absolute`) on the assumption that a sibling `<bk-checkbox-label>` will draw the visible square via its `.fd-checkbox__checkmark` ::before pseudo. A lone `<bk-checkbox>` is therefore a working click target with zero visible chrome — easy to ship and never catch in code review. Pair it: `<bk-checkbox id="x" ng-model="…">` followed by `<bk-checkbox-label for="x" empty="true">…</bk-checkbox-label>` (the `empty="true"` attribute drops the inner text container so the label provides just the checkmark — use it when the surrounding markup already labels the row).
- **`<bk-dialog>` has an isolate scope.** You can't put `ng-controller="…PopupCtrl"` on the dialog element itself — Angular throws "Multiple directives [bkDialog, ngController] asking for new/isolated scope on: <bk-dialog>". Wrap with a thin `<div ng-controller="…">` and put `<bk-dialog visible="…">` inside.
- **`<bk-select>` doesn't support `ng-options`.** Use `<bk-option ng-repeat>` instead — text via the `text` attribute, model value via `value`. Example: `<bk-option ng-repeat="opt in items" text="{{opt.name}}" value="opt.id">`. When the select sits in a parent with `overflow:hidden` (a dialog, a sidebar), add `dropdown-fixed="true"` so the menu floats via `position:fixed` instead of being clipped.
- **`<bk-input>` / `<bk-textarea>` / `<bk-button>` use `replace:true`.** The attributes you write on the directive element (ng-model, ng-blur, ng-keypress, ng-disabled, custom directives like `auto-focus` / `select-text`) end up on the underlying native `<input>` / `<textarea>` / `<button>`, so existing controller code keeps working unchanged after migrating native form controls to `bk-*`. ng-model binds against the parent scope — the isolate scope `bk-input` declares only owns `compact` / `state` / `glyph`.
- **The `blimpKit` module's `.config()` block disables three `$compileProvider` flags.** `cssClassDirectivesEnabled(false)`, `commentDirectivesEnabled(false)`, and `debugInfoEnabled(false)` are flipped at module-load when debug info was on — saves per-element scope-tracking overhead in production. The last flag breaks Selenide-style debugging that calls `angular.element(node).scope()`: Angular stops attaching scope refs to DOM nodes, so the lookup returns `undefined`. If your app or its integration tests rely on that, re-enable the flags in a `.config(['$compileProvider', …])` block of your own — module config blocks run in dependency order, so `blimpKit`'s flips happen first and your override sticks.
- **SAP-icons + the "72" body font live in platform-core's `fonts.css`.** Every BlimpKit-using page needs `<link rel="stylesheet" href="/services/web/platform-core/ui/styles/fonts.css">`. Without it `.sap-icon--*` glyphs render as tofu squares because the `@font-face { font-family: "SAP-icons"; … }` declaration is missing. The IDE shell loads this automatically via the `platform-links` injection mechanism (see below); standalone iframes (editor-bpm, embedded views) have to add the link tag explicitly. Other `@font-face` rules in the same file declare the body font: `"72"` (Regular / Light / Bold), `"72-Light"`, `"72-Bold"`, `"72Mono-Regular"`, `"72Mono-Bold"`, plus `"BusinessSuiteInAppSymbols"` and `"SAP-icons-TNT"`.
- **`<meta name="platform-links" category="…">` auto-injects scripts + stylesheets.** Looking at any non-iframe perspective HTML you'll see a single `<meta name="platform-links" category="ng-view,ng-perspective">`-style tag in the `<head>`. `HtmlPlatformLinksInjector` (in `components/engine/engine-web/.../HtmlPlatformLinksInjector.java`) reads it at request time, walks the `category` list, and replaces the meta tag with the bundle of `<link>` and `<script>` tags registered for those categories. Categories are defined in `components/engine/engine-web/src/main/resources/platform-links.json` — `ng-view` is the heavyweight bundle (jQuery, AngularJS, all the platform hubs, BlimpKit, Fundamental-Styles, fonts.css), `ng-perspective` adds split + layout, `ng-editor` adds workspace + repository hubs, etc. Adding new shared platform code → add it to this JSON, not to every perspective HTML.
- **`<bk-dialog>` toggles visibility via the `visible` binding, not a `.modal('show')` plugin.** `<bk-dialog visible="modal.visible">` watches the expression and adds `fd-dialog--active` when true. No backdrop element is added (the dialog's own `.fd-dialog--active` overlay handles z-index + dimming). To dismiss programmatically: flip the bound flag (`scope.modal.visible = false`) inside an `$apply`; let the directive's digest cycle remove the `--active` class; then `$timeout` ~300ms later before tearing down the scope so the close animation completes.
- **Test selectors after a BlimpKit migration.** Native `<input class="form-control">` → `<input class="fd-input fd-input--compact">`. `<div class="modal in">` (Bootstrap-3 visible) → `<section class="fd-dialog fd-dialog--active">`. `body.modal-open` and `.modal-backdrop` are NOT set by `<bk-dialog>` — drop assertions on those, the active overlay handles its own dimming. When fixing Selenide tests that look at `.modal-header .close`, switch to `.fd-dialog__header .fd-button` (or scope to the dialog with `section.fd-dialog--active button.fd-button`).

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

## External documentation

The user-facing help portal at <https://www.dirigible.io/help/> documents the IDE perspectives (Workbench, Database, Git, Operations, Documents), artefact authoring (Jobs, CSVIM, Entity model, OData, Listeners, Camel routes, BPMN), JS/TS API reference (`/api/`), and deployment guides (Docker, Kubernetes, Cloud Foundry). Useful when reading integration-test fixtures under `tests/tests-integrations/src/main/resources/<TestName>/` or understanding what a given artefact extension is supposed to do.

The portal is **a separate repository** — MkDocs-built from <https://github.com/dirigible-io/dirigible-io.github.io> (Markdown sources under `docs-help/docs/`, nav in `docs-help/mkdocs.yml`). Doc fixes go there as PRs, not in this repo. Blog posts (`docs-blogs/docs/YYYY/MM/DD/*.md`) need three coordinated updates per post: the markdown source, `docs/blogs.json` (CI normally regenerates from the last 5 dated `.md` files via `.github/folders.sh`, but a manual edit lights the home page up immediately), and `docs-blogs/mkdocs.yml` nav (year heading + per-post entry). The `docs/` tree is otherwise CI-generated — see the repo's own `CLAUDE.md` and don't hand-edit it for routine changes; `ci skip` in a commit message *suppresses* the regeneration job.

The companion sample repos under `dirigiblelabs/*` are also separate. For end-to-end tests that clone such repos (`SampleProjectRepositoryIT` subclasses), the sample-side PR has to merge first; the dirigible-side IT will otherwise fail CI when it clones an empty `master`.

Treat `modules/commons/commons-config/src/main/java/org/eclipse/dirigible/commons/config/DirigibleConfig.java` (the enum) and `Configuration.java` (the allow-list) as the source of truth for env-var names and defaults, not the portal — the env-vars page lags the code by months. Specifically, the in-repo OAuth flow is Spring's `spring.security.oauth2.client.registration.github.*` activated by the `github` profile and configured via `DIRIGIBLE_GITHUB_CLIENT_ID` / `_CLIENT_SECRET` / `_SCOPE` (`build/application/src/main/resources/application-github.properties`), not the generic `DIRIGIBLE_OAUTH_*` entries some doc pages still describe. Current servlet mappings are rooted at `/services/...` and `/public/...` per `BaseEndpoint`; any `/services/v4/...` URL in a doc snippet is legacy.

## CI reference

`.github/workflows/build.yml` is the source of truth for "does this build pass":

- `code-style`: `mvn -T 1C formatter:validate`
- `tests` (ubuntu + windows matrix): `mvn clean install -P unit-tests`
- `integration-tests-h2` / `-postgresql` / `-mssql`: `mvn clean install -P integration-tests` with the matching `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars
- `build-deploy`: `mvn clean install -P quick-build` then Docker buildx multi-arch image push to `dirigiblelabs/dirigible`

`pull-request.yml`, `codeql.yml`, `release.yml` cover PR validation, CodeQL, and Maven Central release respectively.
