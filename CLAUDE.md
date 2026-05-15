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
  - `components/engine/` — runtime execution engines: `engine-javascript` (GraalVM polyglot), `engine-typescript`, `engine-bpm-flowable` (workflows), `engine-camel` (integration routes), `engine-jobs` (Quartz), `engine-listeners` (message listeners), `engine-cms-*` (internal / S3 / SharePoint), `engine-odata`, `engine-openapi`, `engine-web`, `engine-websockets`, `engine-template-*`, `engine-open-telemetry`, etc. Each engine registers itself as a Spring component and contributes routes/processors/scheduled jobs to the running app.
  - `components/api/` — Java implementations of JS/TS APIs that user code in the IDE can `import` (e.g. `api-database`, `api-http`, `api-mail`, `api-git`, `api-bpm`, `api-cms`, `api-s3`, `api-kafka`, `api-rabbitmq`, `api-mongodb`, `api-pdf`, ...). These are bridged into GraalJS by `engine-javascript`.
  - `components/data/` — data-layer tooling: `data-sources`, `data-structures` (HDB-style table definitions), `data-management`, `data-import`/`-export`, `data-csvim` (CSV import model), `data-store`, `data-transfer`, `data-processes`.
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

## Conventions worth knowing

- **Code formatting is enforced.** Build job `code-style` in `.github/workflows/build.yml` runs `mvn formatter:validate`. The IDE setup (Eclipse / IntelliJ / VS Code) for importing `dirigible-formatter.xml` is documented in `CONTRIBUTING.md`.
- **License headers are checked.** Every Java/JS/properties file carries the EPL-2.0 header. New files should include it; `mvn license:format -P license` will add or refresh it. Most local-iteration profiles set `license.skip=true`, but the default `mvn install` does not.
- **Integration tests boot the full Spring app and drive Chrome via Selenide.** `HEADLESS=true` + `-D selenide.headless=$HEADLESS` is the way to run them on CI/headless machines. Screenshots end up in `tests/tests-integrations/build/reports/tests`.
- **DB-specific behavior is covered by parametrized CI.** `build.yml` runs the integration suite three times — H2 (default), PostgreSQL 16, and MSSQL 2022 — by varying the `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars. When touching SQL or schema-emission code, replicate this locally for the affected DB rather than assuming H2 behavior generalizes.
- **WebJars / `dirigiblelabs` modules.** Some IDE-side modules (names starting with `ide-`, `api-`, `ext-`) historically lived as separate repos under [dirigiblelabs](https://github.com/dirigiblelabs); per `CONTRIBUTING.md` step 8 you may need `mvn clean install -Pcontent` to pull their latest content if working across them.
- **`*IT.java` vs `*Test.java` matters** — failsafe picks up the former, surefire the latter. Putting an integration test under a `*Test` name will silently run it during the wrong phase (and likely without the test app context).
- **Configuration goes through `DirigibleConfig` / `Configuration`** (`modules/commons/commons-config`). When introducing a new tunable, add the enum entry there with a `DIRIGIBLE_*` env-var key and a sensible default — don't read env vars or `System.getProperty` ad-hoc.
- **Spring beans glue everything together; `StaticObjects` is legacy.** Some code paths still grab dependencies via `StaticObjects.get(...)` (e.g. `RepositoryConfig` registers `IRepository` there explicitly) because parts of the runtime predate Spring. New code should rely on constructor injection; don't add new `StaticObjects` keys.
- **Bean-definition overriding is enabled** (`spring.main.allow-bean-definition-overriding=true` in `application-common.properties`), so a duplicate `@Bean` name will silently shadow another. Be deliberate about bean names.

## External documentation

The user-facing help portal at <https://www.dirigible.io/help/> documents the IDE perspectives (Workbench, Database, Git, Operations, Documents), artefact authoring (Jobs, CSVIM, Entity model, OData, Listeners, Camel routes, BPMN), JS/TS API reference (`/api/`), and deployment guides (Docker, Kubernetes, Cloud Foundry). Useful when reading integration-test fixtures under `tests/tests-integrations/src/main/resources/<TestName>/` or understanding what a given artefact extension is supposed to do.

The portal is **a separate repository** — MkDocs-built from <https://github.com/dirigible-io/dirigible-io.github.io> (Markdown sources under `docs-help/docs/`, nav in `docs-help/mkdocs.yml`). Doc fixes go there as PRs, not in this repo.

Caveats — parts of the portal are out of date and should not be trusted over the code (verified against `dirigible-io.github.io@master`):

- URL paths often use legacy `/services/v4/...` prefixes; the current endpoints are rooted at `/services/...` and `/public/...` per `BaseEndpoint` (see Run section above).
- The "Environment Variables" page lists `DIRIGIBLE_HOME_URL` default as `/services/v4/web/ide/index.html`, but the live default in `DirigibleConfig.HOME_URL` is `services/web/shell-ide/`. Treat `modules/commons/commons-config/src/main/java/org/eclipse/dirigible/commons/config/DirigibleConfig.java` (the enum) and `Configuration.java` (the allow-list) as the source of truth for env-var names and defaults.
- OAuth wiring described as `DIRIGIBLE_OAUTH_*` in the portal doesn't match the in-repo GitHub flow, which uses Spring's `spring.security.oauth2.client.registration.github.*` driven by `DIRIGIBLE_GITHUB_CLIENT_ID` / `DIRIGIBLE_GITHUB_CLIENT_SECRET` / `DIRIGIBLE_GITHUB_SCOPE` (see `build/application/src/main/resources/application-github.properties`).

## CI reference

`.github/workflows/build.yml` is the source of truth for "does this build pass":

- `code-style`: `mvn -T 1C formatter:validate`
- `tests` (ubuntu + windows matrix): `mvn clean install -P unit-tests`
- `integration-tests-h2` / `-postgresql` / `-mssql`: `mvn clean install -P integration-tests` with the matching `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars
- `build-deploy`: `mvn clean install -P quick-build` then Docker buildx multi-arch image push to `dirigiblelabs/dirigible`

`pull-request.yml`, `codeql.yml`, `release.yml` cover PR validation, CodeQL, and Maven Central release respectively.
