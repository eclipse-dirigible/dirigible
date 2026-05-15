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

UI at `http://localhost:8080`, default credentials `admin`/`admin`. The default DB is in-memory H2; switch to PostgreSQL/MSSQL/etc. by exporting `DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER/URL/USERNAME/PASSWORD` (and `mvn clean` if you previously ran with a different DB so leftover H2 files don't poison startup). The server port is overridable via `DIRIGIBLE_SERVER_PORT`. The in-IDE terminal launches `ttyd` on port 9000.

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

## Conventions worth knowing

- **Code formatting is enforced.** Build job `code-style` in `.github/workflows/build.yml` runs `mvn formatter:validate`. The IDE setup (Eclipse / IntelliJ / VS Code) for importing `dirigible-formatter.xml` is documented in `CONTRIBUTING.md`.
- **License headers are checked.** Every Java/JS/properties file carries the EPL-2.0 header. New files should include it; `mvn license:format -P license` will add or refresh it. Most local-iteration profiles set `license.skip=true`, but the default `mvn install` does not.
- **Integration tests boot the full Spring app and drive Chrome via Selenide.** `HEADLESS=true` + `-D selenide.headless=$HEADLESS` is the way to run them on CI/headless machines. Screenshots end up in `tests/tests-integrations/build/reports/tests`.
- **DB-specific behavior is covered by parametrized CI.** `build.yml` runs the integration suite three times — H2 (default), PostgreSQL 16, and MSSQL 2022 — by varying the `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars. When touching SQL or schema-emission code, replicate this locally for the affected DB rather than assuming H2 behavior generalizes.
- **WebJars / `dirigiblelabs` modules.** Some IDE-side modules (names starting with `ide-`, `api-`, `ext-`) historically lived as separate repos under [dirigiblelabs](https://github.com/dirigiblelabs); per `CONTRIBUTING.md` step 8 you may need `mvn clean install -Pcontent` to pull their latest content if working across them.
- **`*IT.java` vs `*Test.java` matters** — failsafe picks up the former, surefire the latter. Putting an integration test under a `*Test` name will silently run it during the wrong phase (and likely without the test app context).

## CI reference

`.github/workflows/build.yml` is the source of truth for "does this build pass":

- `code-style`: `mvn -T 1C formatter:validate`
- `tests` (ubuntu + windows matrix): `mvn clean install -P unit-tests`
- `integration-tests-h2` / `-postgresql` / `-mssql`: `mvn clean install -P integration-tests` with the matching `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars
- `build-deploy`: `mvn clean install -P quick-build` then Docker buildx multi-arch image push to `dirigiblelabs/dirigible`

`pull-request.yml`, `codeql.yml`, `release.yml` cover PR validation, CodeQL, and Maven Central release respectively.
