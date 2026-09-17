## Project

Eclipse Dirigible — a high-productivity application platform (hpaPaaS). The runtime is a single Spring Boot fat jar that bundles an in-browser IDE plus execution engines (GraalJS, Flowable BPM, Camel, Quartz, Lucene, CMS, OData, etc.). It supports "In-System Programming": users develop and modify the running system through the browser. The shipped artifact is `build/application/target/dirigible-application-*-executable.jar`; the entry point is `org.eclipse.dirigible.DirigibleApplication` (`build/application/src/main/java/org/eclipse/dirigible/DirigibleApplication.java`).

## Prerequisites

- Java JDK 21 (project compiles to Java 21; CI builds/tests run on Corretto 24, bytecode target stays 21)
- Maven 3.8.x
- Node.js 22.x with global installs of `typescript` and `esbuild` (frontend WebJars are transpiled/bundled at build time)
- `ttyd` (only required at runtime for the in-IDE terminal on port 9000, and for integration tests)

## Build

The whole project is a multi-module Maven build at the root. The Maven coordinates are `org.eclipse.dirigible:dirigible-parent` (currently `14.0.0-SNAPSHOT`).

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

