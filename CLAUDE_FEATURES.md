# CLAUDE_FEATURES.md

A structured inventory of Eclipse Dirigible's functionality, written to drive later generation of human-readable documentation. The shape mirrors how an end user thinks about the platform: what *artefacts* they can author, what *engines* execute them, what *APIs* their code can call, what *tooling* surfaces the experience, and what *operations* sit underneath.

Source-of-truth pointers are included in each section so generated docs can cite (or update from) the code directly.

---

## 1. Platform overview

- **Category.** High-productivity application platform as a service (hpaPaaS). Single Spring Boot fat jar that bundles an in-browser IDE plus runtime engines (GraalJS / GraalVM polyglot, Flowable BPM, Camel, Quartz, Lucene, CMS, OData, JDBC, …).
- **Development model.** "In-System Programming": end users develop and modify the running system through the browser. There is no separate deploy step — artefacts are reconciled from the on-disk repository into runtime state by *synchronizers* on every change.
- **Shipping artifact.** `build/application/target/dirigible-application-*-executable.jar`.
- **Entry point.** `org.eclipse.dirigible.DirigibleApplication` (`build/application/src/main/java/org/eclipse/dirigible/DirigibleApplication.java`).
- **Languages users can author in.**
  - **JavaScript** — ES6+ syntax over GraalJS; synchronous programming model (in contrast to Node.js); CommonJS + ESM supported.
  - **TypeScript** — transpiled at the platform; full strong typing via tsconfig at the project root.
  - **Java** — client `.java` compiled in-process by `engine-java`.
  - **Python** — server-side modules via `engine-python` (subset).
  - **ABAP** — supported in downstream products via [open-abap](https://github.com/open-abap) + the [abaplint transpiler](https://github.com/abaplint/transpiler); transpiles to JS for execution. Not part of the upstream dirigible codebase.
  - **SAP HANA XSC (XSK)** — XS Classic compatibility (XSJS, XSODATA, HDB artefacts) lives in the separate [XSK project](https://xsk.io); consumed by downstream products (e.g. codbex `Kronos`).
  - **Declarative artefacts** — XML / JSON / YAML / Markdown / Confluence wiki (see §2).
- **Default UI.** `http://localhost:8080`, login `admin` / `admin`.
- **License.** Eclipse Public License 2.0.

---

## 2. Artefacts

Artefacts are user-authored files placed under a project. The platform reconciles them from `/registry/public/<project>/...` to runtime state. Each artefact type has a synchronizer (`extends BaseSynchronizer` / `MultitenantBaseSynchronizer`), a JPA entity (`extends Artefact`), and usually a serving engine or service.

Use this table as the canonical list of file extensions and the runtime behavior each one enables.

### 2.1 Runtime / execution artefacts

| Extension / Pattern | Artefact | Engine that consumes it | Source-of-truth synchronizer |
| ------------------- | -------- | ----------------------- | ---------------------------- |
| `*.js`, `*.mjs`, `*.ts` | JavaScript / TypeScript module | `engine-javascript` (Graalium / GraalVM polyglot) — served at `/services/js/...` and `/public/js/...` | Not synchronized; loaded on demand by `JavascriptEndpoint` / `TypeScriptEndpoint` |
| `*.java` | Client Java source | `engine-java` — single `javac` + single `ClientClassLoader` per cycle; served at `/services/java/{project}/{*classPath}` and `/public/java/...` | `JavaSynchronizer` |
| `*.py` | Python module | `engine-python` (limited) | `PythonEndpoint` |
| `*.bpmn` | BPMN 2.0 process | `engine-bpm-flowable` (Flowable) | `BpmnSynchronizer` |
| `*.camel` | Apache Camel route (YAML / XML route definition) | `engine-camel` | `CamelSynchronizer` |
| `*.job` | Scheduled job (Quartz, cron) | `engine-jobs` | `JobSynchronizer` |
| `*.listener` | Message listener (JMS / ActiveMQ-style) | `engine-listeners` | `ListenerSynchronizer` |
| `*.websocket` | WebSocket endpoint binding | `engine-websockets` | `WebsocketsSynchronizer` |
| `*.odata` | OData V2 service definition | `engine-odata` (Apache CXF) | `ODataSynchronizer` |
| `*.proxy` | HTTP reverse-proxy route | `engine-proxy` | `ProxySynchronizer` |
| `*.extensionpoint`, `*.extension` | Pluggable extension hooks (declared point + provider) | `core-extensions` | `ExtensionPointsSynchronizer`, `ExtensionsSynchronizer` |
| `*.access`, `*.roles` | Declarative URL access and role definitions | `engine-security` | `AccessSynchronizer`, `RolesSynchronizer` |
| `expose` (project-level) | Expose project resources as static URLs | `engine-web` | `ExposesSynchronizer` |
| `*Component.ts` | Spring-style DI component declared in TS | `engine-di` | `ComponentSynchronizer` |
| `*Entity.ts` | TS `@Entity` decorator → Hibernate (dynamic HBM XML) | `data-store` | `EntitySynchronizer` |
| `*.java` annotated with `@Entity` | Java entity decorators → same Hibernate machinery | `data-store-java` (consumes `engine-java` SPI) | (`EntityClassConsumer` plugged into `JavaSynchronizer.finishing()`) |
| `*Controller.ts` (and Java `@Controller`) | Auto-published OpenAPI fragment | `engine-openapi` | `OpenAPISynchronizer` |

### 2.2 Data artefacts

| Extension | Purpose | Synchronizer / engine |
| --------- | ------- | --------------------- |
| `*.datasource` | JDBC data-source registration (driver, URL, credentials, tenant binding) | `DataSourcesSynchronizer` (data-sources) |
| `*.schema` | Database schema definition (multi-table grouping) | `SchemasSynchronizer` (data-structures) |
| `*.table` | Single-table DDL definition | `TablesSynchronizer` (data-structures) |
| `*.view` | Database view DDL | `ViewsSynchronizer` (data-structures) |
| `*.csvim`, `*.csv` | CSV import model + data files | `CsvimSynchronizer` (data-csvim) |

### 2.3 Documentation / wiki artefacts

| Extension | Purpose | Synchronizer |
| --------- | ------- | ------------ |
| `*.md` | Markdown page rendered by the runtime | `MarkdownSynchronizer` |
| `*.confluence` | Confluence-wiki page rendered by the runtime | `ConfluenceSynchronizer` |

### 2.4 Design-time modeler artefacts

Files produced by IDE modelers and consumed by template-based code generation. They typically generate the runtime artefacts in §2.1–2.3 rather than being reconciled directly by a synchronizer.

| Extension | Modeler | Purpose |
| --------- | ------- | ------- |
| `*.dsm` | Database Schema Modeler | XML database-schema model used to generate `.schema` / `.table` / `.view` artefacts. |
| `*.edm` | Entity Data Modeler | XML domain model used to generate `.model` plus full CRUD applications via the application templates. |
| `*.model` | Entity Data Modeler (JSON projection) | JSON projection of an EDM consumed by application generation templates (entities + perspectives + navigations). |
| `*.form` | Form Designer | Layout of a web form. |
| `*.command` | (legacy) | Shell-command descriptor; superseded by `engine-command` + `@aerokit/sdk/platform/command`. |
| `*.entity` | (codbex-specific) | JSON entity descriptor in downstream codbex products; rough equivalent of TS `*Entity.ts` in upstream dirigible. |

Mapping to the help portal: *Modeling* → `docs-help/docs/development/ide/modelers/` and *Tutorials > Modeling* → `docs-help/docs/tutorials/modeling/`.

### 2.4 Repository layout an artefact lives in

```
/registry/public/<project>/...          # published artefacts (reconciled)
/users/<user>/workspace/<project>/...   # IDE workspaces (drafts)
```

See `IRepositoryStructure`. Multi-tenancy is on by default (`DIRIGIBLE_MULTI_TENANT_MODE=true`), so artefact reconciliation runs per tenant unless the synchronizer is single-tenant by design.

### 2.5 Authoring an artefact (template inventory)

Each modeler in §2.4 has an in-IDE editor under `components/ui/editor-*` and a templating pipeline under `components/template/`.



Project templates available via the IDE's *Generate* action. Source: `components/template/template-*` (also reachable through `GenerationEndpoint`).

- **Application templates.** `template-application-angular`, `…-angular-java`, `…-angular-v2`, `template-application-dao` (+ `-java`, `-v2`), `template-application-data` (+ `-v2`), `template-application-feed` (+ `-v2`), `template-application-odata`, `template-application-rest` (+ `-java`, `-v2`), `template-application-schema`, `template-application-ui-angular` (+ `-java`, `-v2`).
- **Sample applications.** `template-bookstore`, `template-hello-world`.
- **Single-artefact templates.** `template-bpm`, `template-camel`, `template-camel-cron-route`, `template-camel-http-route`, `template-database-access`, `template-database-table`, `template-database-view`, `template-editor`, `template-extension-perspective`, `template-extension-view`, `template-form`, `template-form-builder-angularjs`, `template-html`, `template-http-client`, `template-job`, `template-listener`, `template-mapping-javascript`, `template-perspective`, `template-react`, `template-typescript`, `template-view`, `template-websocket`.

---

## 3. Execution engines

The platform is composed by what lands on the classpath — `build/application` Spring-Boots everything together. Each engine lives under `components/engine/<name>/` and registers itself via Spring auto-configuration.

| Engine module | Capability |
| ------------- | ---------- |
| `engine-javascript` | Runs `.js`/`.mjs`/`.ts` over GraalJS via the Graalium runner (`DirigibleJavascriptCodeRunner`). Bridges Java APIs into JS contexts. |
| `engine-typescript` | Serves `.ts` modules; transpiles on demand. |
| `engine-java` | Compiles, loads, and dispatches client `.java`: one `javac` task, one `ClientClassLoader` per cycle. Hosts the `JavaClassConsumer` SPI (entity, repository, controller, handler consumers). Routes through `JavaEndpoint`. |
| `engine-bpm`, `engine-bpm-flowable` | Flowable workflow engine (process / task / signal / message). REST surface at `BpmFlowableEndpoint`, inbox at `BpmInboxEndpoint`. |
| `engine-camel` | Apache Camel routes (HTTP, cron, file, …). |
| `engine-jobs` | Quartz scheduler. |
| `engine-listeners` | Message-bus listeners (ActiveMQ-style). |
| `engine-websockets` | Server WebSocket endpoints. |
| `engine-odata` | OData v2 (CXF) at `/odata/v2/...`. |
| `engine-openapi` | Aggregates OpenAPI fragments published by TS and Java controllers; served at `/services/openapi`. |
| `engine-web` | `expose`-driven static / project-resource serving. |
| `engine-cms`, `engine-cms-internal`, `engine-cms-s3`, `engine-cms-sharepoint` | Content management — internal CMIS, AWS S3 backend, MS SharePoint backend. |
| `engine-command` | Run shell commands from user code. |
| `engine-di` | Dependency injection for TS components (`*Component.ts`). |
| `engine-ftp`, `engine-sftp` | (S)FTP server endpoints. |
| `engine-proxy` | HTTP reverse-proxy routes. |
| `engine-python` | Python module execution. |
| `engine-security` | Declarative `.access` / `.roles` enforcement; integrates with Spring Security. |
| `engine-template`, `engine-template-javascript`, `engine-template-mustache`, `engine-template-velocity` | Template-language runtimes for generation. |
| `engine-wiki` | Markdown and Confluence rendering. |
| `engine-open-telemetry` | OpenTelemetry trace/metric export (Camel-aware). |

---

## 4. JS / TS API surface (`@aerokit/sdk/*`)

Java implementations of platform APIs that JS/TS user code can `import`. They are bridged into the GraalJS context by `engine-javascript`. Java surface lives under `components/api/api-*/`; pre-built TS/JS bundles ship from `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/`.

The canonical package is `@aerokit/sdk` — submodules are imported as `@aerokit/sdk/<area>` (e.g. `import { ... } from "@aerokit/sdk/db"`). The legacy `@dirigible/*` aliases still resolve.

### 4.1 Data and storage

- `@aerokit/sdk/db` — JDBC, DAO/ORM (`dao`, `database`, `orm`, `procedure`, `query`, `insert`, `update`, `sequence`, `sql`, `repository`, `store`, `translator`, `decorators`).
- `@aerokit/sdk/cms` — Content management (CMIS + S3 + SharePoint).
- `@aerokit/sdk/s3` — Amazon S3 client.
- `@aerokit/sdk/mongodb` — MongoDB driver.
- `@aerokit/sdk/qldb` — Amazon QLDB.
- `@aerokit/sdk/etcd` — etcd client.
- `@aerokit/sdk/redis` — Redis client.
- `@aerokit/sdk/cache` — In-process / shared cache.

### 4.2 Messaging and integration

- `@aerokit/sdk/http` — HTTP client + JAX-RS-style server helpers (`client`, `client-async`, `request`, `response`, `session`, `upload`, `rs`, `decorators`).
- `@aerokit/sdk/messaging` — Generic message bus.
- `@aerokit/sdk/kafka` — Kafka producer / consumer.
- `@aerokit/sdk/rabbitmq` — RabbitMQ.
- `@aerokit/sdk/mail` — SMTP send.
- `@aerokit/sdk/net` — Low-level networking.

### 4.3 Process and platform

- `@aerokit/sdk/bpm` — Flowable interaction (start process, complete task, …).
- `@aerokit/sdk/job` — Quartz job control.
- `@aerokit/sdk/extensions` — Discover & invoke extensions declared at `.extensionpoint` / `.extension`.
- `@aerokit/sdk/component` — DI lookups (TS `*Component.ts`).
- `@aerokit/sdk/platform` — Generic platform info.

### 4.4 Tooling

- `@aerokit/sdk/io` — `bytes`, `files`, `image`, `streams`, `zip`.
- `@aerokit/sdk/indexing` — Lucene index API.
- `@aerokit/sdk/log` — Logging facade.
- `@aerokit/sdk/utils` — Hashing, encoding, UUIDs, etc.
- `@aerokit/sdk/git` — Git operations (mirrors the IDE Git perspective).
- `@aerokit/sdk/pdf` — PDF generation.
- `@aerokit/sdk/template` — Mustache / Velocity / JS-templating.
- `@aerokit/sdk/security` — `UserFacade` (current user, roles, anonymous mode).
- `@aerokit/sdk/core` — Configuration access, environment, base utilities.

### 4.5 Testing helpers

- `@aerokit/sdk/junit` — JUnit runner bridge.
- `@aerokit/sdk/qunit` — QUnit runner.
- `@aerokit/sdk/test` — Generic test helpers.

### 4.6 Submodule index (for reference generation)

The published portal organises the API into ~100 reference pages. Each row below is a stable target for a docs page; the SDK module name (`@aerokit/sdk/<area>`) maps 1:1 to the help portal path.

| Area | Submodules |
| ---- | ---------- |
| `bpm` | `deployer`, `process`, `tasks`, `values` |
| `cache` | `cache` |
| `cms` | `cmis` |
| `core` | `configurations`, `context`, `env`, `globals` |
| `db` | `dao`, `database`, `decorators`, `insert`, `orm`, `ormstatements`, `procedure`, `query`, `repository`, `sequence`, `sql`, `store`, `translator`, `update` |
| `etcd` | `client` |
| `extensions` | `extension-point`, `extensions-client`, `extensions-server` |
| `git` | `client` |
| `http` | `client`, `client-async`, `decorators`, `errors`, `request`, `response`, `rs`, `session`, `upload`, `utils` |
| `indexing` | `searcher`, `writer` |
| `io` | `bytes`, `files`, `ftp`, `image`, `streams`, `zip` |
| `job` | `scheduler` |
| `kafka` | `consumer`, `producer` |
| `log` | `logging` |
| `mail` | `client` |
| `messaging` | `consumer`, `producer` |
| `mongodb` | `client`, `dao` |
| `net` | `soap`, `websocket` |
| `pdf` | `pdf` |
| `platform` | `command`, `engines`, `lifecycle`, `os`, `problems`, `registry`, `repository-client`, `repository-hub`, `repository-server`, `workspace-client`, `workspace-hub`, `workspace-server` |
| `qldb` | `qldb` |
| `qunit` | `qunit` |
| `rabbitmq` | `consumer`, `producer` |
| `redis` | `client` |
| `security` | `decorators`, `oauth`, `user` |
| `template` | `engines` |
| `test` | `assert`, `runner` |
| `utils` | `alphanumeric`, `base64`, `converter`, `digest`, `escape`, `hex`, `jsonpath`, `qrcode`, `url`, `utf8`, `uuid`, `xml` |

There is also a parallel **User-Interface** API documented in the portal (`docs-api/docs/user-interface/`) covering `branding`, `context-menu`, `dialog`, `editor`, `internationalization`, `layout`, `menu`, `message-hub`, `notification`, `perspective`, `shell`, `shell-hub`, `shortcuts`, `status-bar`, `subview`, `themes`, `ui-components`, `view`, `viewParameters`, `window`. These are the browser-side helpers consumed by IDE extensions and custom perspectives.

---

## 5. Client Java surface

Detailed in CLAUDE.md §"Client Java code". For doc-generation purposes, the user-visible surface is:

### 5.1 Entity / persistence annotations (`org.eclipse.dirigible.engine.java.annotations.*`)
`@Entity`, `@Table`, `@Id`, `@GeneratedValue` (`+ GenerationType`), `@Column`, `@Transient`, `@CreatedAt`, `@UpdatedAt`, `@CreatedBy`, `@UpdatedBy`, `@Documentation` (valid on methods → OpenAPI summary).

### 5.2 REST annotations (`org.eclipse.dirigible.engine.java.annotations.http.*`)
Class-level: `@Controller`, `@Roles`. Method-level verbs: `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete` (each takes a path suffix). Parameter binding: `@Body`, `@PathParam`, `@QueryParam`, `@Context`. Method-level `@Roles` overrides class-level for that method.

### 5.3 Client SPI / helpers
- `JavaClassConsumer` SPI — pluggable consumers for compiled classes (`Entity`, `Repository`, `Controller`, `Handler`); ordered via Spring `@Order`.
- `JavaEntityStore` — typed CRUD façade over Hibernate dynamic-map mode.
- `EntityBeanMapper` — bean ↔ Map conversion respecting `@Column` / `@Transient`.
- `BeanProvider.getBean(...)` — pull platform beans (`JavaEntityStore`, `IRepository`, …) from client classes (no Spring scan).
- OpenAPI auto-generation — `JavaControllerOpenApiPublisher` writes a fragment per controller at `java-controller://<project>::<fqn>`; aggregated under `OpenAPIEndpoint`.

---

## 6. In-browser IDE

The IDE is composed of WebJar UI modules under `components/ui/` plus backend services under `components/ide/`. It is the user's primary entry point.

### 6.1 Perspectives (`components/ui/perspective-*`)
- **Workbench** — editor + project tree (default perspective).
- **Database** — schemas, tables, views, SQL console.
- **Git** — clone, pull, push, branch, diff, log (driven by `ide-git`).
- **Operations** — runtime introspection: jobs, listeners, extensions, websockets, transport, configurations, loggers, logs, registry, repository, problems, search.
- **Processes** — running BPMN processes + inbox.
- **Documents** — CMS browser (internal / S3 / SharePoint).
- **Security** — roles, access rules, client registrations.
- **Jobs** — scheduled job control.
- **Settings** — branding, locale, preferences.
- **Tracing** — OpenTelemetry-backed view.

### 6.2 Editors (`components/ui/editor-*`)
- `editor-monaco` (and `editor-monaco-extensions`) — Monaco-based code editing for JS/TS/Java/CSS/HTML/etc., with breakpoint glyphs (`debug-breakpoint-glyph`, `debug-current-line-glyph`). Powers JS/TS/Java/Python/HTML/CSS/JSON authoring.
- Visual / form editors: `editor-bpm` (BPMN), `editor-csv`, `editor-csvim`, `editor-data-structures` (schema / table / view), `editor-entity`, `editor-extensions`, `editor-form-builder`, `editor-image`, `editor-integrations`, `editor-jobs`, `editor-listeners`, `editor-mapping`, `editor-report`, `editor-schema`, `editor-security`, `editor-websockets`.

#### Modelers (visual designers)
First-class designers, each writing one of the modeler artefacts from §2.4:

| Modeler | Authors | Generates |
| ------- | ------- | --------- |
| Entity Data Modeler | `*.edm` / `*.model` | Full CRUD application (tables, OData, REST, UI) via the `template-application-*` family. |
| Database Schema Modeler | `*.dsm` / `*.schema` | `.table` + `.view` artefacts + constraints. |
| BPMN Modeler | `*.bpmn` | Flowable process definition. |
| Form Designer | `*.form` | HTML form layout. |
| Integrations Modeler (Karavan) | `*.camel` | Apache Camel route (`resources-karavan-libs`). |

#### Underlying libraries
Monaco (editor), mxGraph (Schema/EDM diagrams), bpmn-visualization-js (BPMN viewer), Flowable BPMN (modeler), Karavan (Camel route designer), AngularJS + GoldenLayout (legacy layout), AG Grid (tables), Chart.js (charts), jsTree (trees), Xterm.js (terminal).

### 6.3 Views (`components/ui/view-*`)
Side / bottom panels: `view-artefacts`, `view-configurations`, `view-console`, `view-databases`, `view-data-structures`, `view-debugger` (JS), `view-java-debug`, `view-extensions`, `view-git`, `view-import`, `view-jobs`, `view-listeners`, `view-loggers`, `view-logs`, `view-preview`, `view-problems`, `view-projects`, `view-properties`, `view-registry`, `view-repository`, `view-search`, `view-security`, `view-sql`, `view-swagger`, `view-terminal`, `view-transfer`, `view-translation`, `view-websockets`, `view-welcome`.

### 6.4 Menus (`components/ui/menu-*`)
Per-perspective top-bar menus: `menu-bpm`, `menu-camel`, `menu-csv`, `menu-database`, `menu-entity`, `menu-extensions`, `menu-form-builder`, `menu-help`, `menu-jobs`, `menu-listeners`, `menu-mapping`, `menu-projects`, `menu-schema`, `menu-security`, `menu-websockets`.

### 6.5 IDE backends (`components/ide/`)
- `ide-workspace` — workspace + transport REST API (`WorkspaceEndpoint`, `WorkspacesEndpoint`, `WorkspaceSearchEndpoint`, `WorkspaceFindEndpoint`, `WorkspaceActionsEndpoint`, `TransportEndpoint`, `PublisherEndpoint`).
- `ide-git` — Git over HTTP (`GitEndpoint`).
- `ide-terminal` — Browser terminal backed by `ttyd` on port 9000 (`TerminalWebsocketClientEndpoint`).
- `ide-logs` — Live log streaming and logger configuration.
- `ide-problems` — Compilation/validation problems list.
- `ide-template` — Project generation from templates (`GenerationEndpoint`).
- `ide-java-lsp` — JDT.LS based Java language server (completion, navigation, refactor).
- `ide-java-debug` — DAP bridge from the browser to JDT.LS to a JDWP target (see CLAUDE.md §"Java Debugger" for full architecture; default JDWP port `DIRIGIBLE_JAVA_DEBUG_JDWP_PORT=8000`).
- `ide-junit-results` — JUnit test report viewer.

### 6.6 Shell, branding, locale
- `shell-ide` — top-level frame composing the perspectives and the global toolbar (default entrypoint, configurable via `DIRIGIBLE_HOME_URL`).
- `platform-branding` — logos, titles, prefixes.
- `resources-theme-blimpkit` / `-classic` / `-high-contrast` / `-mystic` — themes.
- `settings-locale`, `resources-locale` — i18n.

### 6.7 BlimpKit UI component library
The IDE chrome (sidebars, dialogs, tabs, menus, toolbars, forms) is built on [BlimpKit](https://blimpkit.dev/) — a SAP-Fundamental-derived Angular component library. Custom perspectives / views should consume BlimpKit components for consistent look and feel; the catalogue is the canonical reference (do not invent ad-hoc styling).

---

## 7. Data layer

### 7.1 Supported databases (`modules/database/`)
- `database-h2` (default, file-backed at `./target/dirigible/h2/DefaultDB`)
- `database-sql-postgres`
- `database-sql-mssql`
- `database-sql-hana`
- `database-sql-mariadb`
- `database-sql-mysql`
- `database-sql-snowflake`
- `database-sql-h2`
- `database-sql-mongodb` (NoSQL)
- `database-mongodb-jdbc`
- `database-persistence` — JDBC persistence abstraction.

CI runs the integration suite three times (H2, PostgreSQL 16, MSSQL 2022) — when changing SQL/DDL emission, replicate locally on the affected DB.

### 7.2 Data tooling components
- `data-sources` — JDBC pool registry + `.datasource` synchronizer.
- `data-structures` — `.schema`, `.table`, `.view` DDL emission.
- `data-csvim` — CSV import model.
- `data-store` — TS `@Entity` decorators → Hibernate via HBM XML (`HbmXmlDescriptor`, `HbmPropertyDescriptor`).
- `data-store-java` — Java `@Entity` annotations → same Hibernate machinery (dynamic-map mode, sidesteps cross-classloader gymnastics).
- `data-management` — schema introspection / DML through the IDE.
- `data-import`, `data-export` — bulk import/export over REST.
- `data-transfer` — copy between data sources.
- `data-anonymize` — column-level anonymization endpoint.
- `data-processes` — process-data helpers.
- `data-core` — shared helpers.
- `data-source-snowpark` — Snowflake Snowpark integration.

### 7.3 Repository (`modules/repository/`)
- `repository-api`, `repository-local`, `repository-master`, `repository-cache`, `repository-search`, `repository-zip` — pluggable storage abstraction backing the registry (file-system local is the default).

---

## 8. Security

- **Authentication backends (`components/security/`).** `security-basic` (form/basic login, default `admin`/`admin`), `security-oauth2`, `security-keycloak`, `security-cognito`, `security-snowflake`, `security-client-registration` (UI for registering OAuth clients at runtime).
- **GitHub OAuth profile.** Activated by the `github` Spring profile via `DIRIGIBLE_GITHUB_CLIENT_ID` / `_CLIENT_SECRET` / `_SCOPE`. Configured in `application-github.properties`.
- **Authorization model.** Declarative `.access` rules + `.roles` definitions, enforced by `engine-security` (URL patterns) and by `@Roles` in client Java (`ControllerInvoker.checkRoles`). Built-in super-roles: `DEVELOPER`, `ADMINISTRATOR`. Anonymous mode toggled by `Configuration.isAnonymousModeEnabled()` / `isAnonymousUserEnabled()`.
- **Multi-tenancy.** On by default (`DIRIGIBLE_MULTI_TENANT_MODE=true`). Tenant resolution is subdomain-based (`DIRIGIBLE_TENANT_SUBDOMAIN_REGEX`); single-realm options exist for Keycloak / Cognito.
  - **Tenant-isolated:** DataSources, CSV import/export, OData services, Documents (CMS storage), scheduled Jobs, Listeners. Each artefact reconciliation runs per tenant via `MultitenantBaseSynchronizer`.
  - **System-level (NOT tenant-isolated):** BPMN process instances, Camel integration flows, declared Extensions, the Git perspective, development workspaces (`/users/<user>/workspace/...`).

---

## 9. HTTP surface

Stable URL roots (`BaseEndpoint.PREFIX_ENDPOINT_*`):

| Path root | Purpose |
| --------- | ------- |
| `/services/...` | Authenticated REST endpoints (workspace, git, jobs, listeners, websockets, security, openapi, …). |
| `/public/...` | Same endpoints without authentication where exposed. |
| `/services/js/<project>/<file>` | Execute a JS/TS user module. `/public/js/...` is the anonymous variant. |
| `/services/java/{project}/{*classPath}` | Dispatch into a client-Java controller or `JavaHandler`. `/public/java/...` is the anonymous variant. |
| `/odata/v2/...` | OData services. |
| `/websockets/...` | WebSocket endpoints (incl. `/websockets/ide/java-debug?workspace=<name>`). |
| `/swagger-ui/index.html`, `/api-docs` | Swagger UI + OpenAPI document. |
| `/spring-admin/` | Spring Boot Admin (server profile enabled). |
| `/actuator/health/readiness`, `/actuator/health/liveness` | Health probes. |
| `/` | Redirects to `DIRIGIBLE_HOME_URL` (default `services/web/shell-ide/`). |

For doc generation: the full list of `@RestController`-annotated classes is in `components/**/endpoint/*Endpoint.java` (~65 endpoint classes; subject to change).

---

## 10. Tooling around the code

### 10.1 Maven build
- Multi-module reactor with five aggregators: `modules/`, `components/`, `build/`, `dependencies/`, `tests/`, `cli/`.
- Profiles: `tests`, `unit-tests`, `integration-tests`, `quick-build`, `format`, `license`, `spotbugs`, `coverage`, `content`.
- Formatter: `dirigible-formatter.xml` enforced by `mvn formatter:validate`.
- License headers: enforced by `mvn license:format -P license`.
- Custom Maven plugin: `maven-plugins/flowable-bpmn-docs-maven-plugin` — generates BPMN docs.

### 10.2 Frontend toolchain
- Node 22.x with global `typescript` and `esbuild`. WebJar modules under `components/ide/` and `components/ui/` are transpiled / bundled at Maven build time.
- Monaco editor, mxGraph, BlimpKit theme, BPMN visualization, AG Grid, Chart.js, Xterm.js, jsTree.

### 10.3 CLI (`cli/`)
Standalone helper that starts the Dirigible jar against a given user project path; produces `cli/target/dirigible-cli-*-executable.jar`. See `cli/README.md`.

### 10.4 Docker
- `build/application/Dockerfile` — produces `dirigiblelabs/dirigible:latest` on the GitHub Container Registry. Ports `8080` (UI) and `8081` (Graalium debugger) exposed.
- `mvn clean install -P quick-build` → `docker build` is the official image pipeline.

### 10.5 Native image
GraalVM native-image build is supported (see `README.md`):
```
native-image -jar build/application/target/dirigible-application-*-executable.jar -o dirigible
```

### 10.6 Tests
- `tests/tests-framework/` — Selenide / Spring test base classes.
- `tests/tests-integrations/` — `*IT.java` Selenide-driven UI integration tests with per-test fixture projects under `src/main/resources/<TestName>/`.
- `tests/ui/tests/sample/` — `SampleProjectRepositoryIT` subclasses that clone `dirigiblelabs/sample-*` repos, publish them, and call `verifyProject()`.
- HTTP-only ITs (faster, headless) extend `IntegrationTest` and call `SynchronizationProcessor.forceProcessSynchronizers()` directly — see `JavaEngineIT`.
- Surefire (`*Test.java`) = unit. Failsafe (`*IT.java`) = integration. Naming matters.
- `-D selenide.headless=true` is the headless flag.

### 10.7 CI (`.github/workflows/`)
- `build.yml` — code-style, ubuntu + windows unit tests, parametrized integration tests (H2 / PostgreSQL 16 / MSSQL 2022), Docker buildx multi-arch image push.
- `pull-request.yml` — PR validation.
- `codeql.yml` — CodeQL scan.
- `release.yml` — Maven Central release.

---

## 11. Configuration

The source of truth for env-vars and defaults is `modules/commons/commons-config/src/main/java/org/eclipse/dirigible/commons/config/DirigibleConfig.java` (the enum) and `Configuration.java` (the allow-list). New tunables go there, not into ad-hoc `System.getProperty` reads.

Notable env-vars (non-exhaustive — full list lives in the source):

| Env var | Default | Purpose |
| ------- | ------- | ------- |
| `DIRIGIBLE_SERVER_PORT` | `8080` | HTTP listen port. |
| `DIRIGIBLE_HOME_URL` | `services/web/shell-ide/` | Landing URL after `/`. |
| `DIRIGIBLE_BASIC_USERNAME` / `_PASSWORD` | `admin` / `admin` | Default basic-auth credentials. |
| `DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER` | `./target/` | On-disk root for the registry. |
| `DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER` / `_URL` / `_USERNAME` / `_PASSWORD` | H2 file-backed | Default JDBC datasource. |
| `DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT` / `_SYSTEM` | `DefaultDB` / `SystemDB` | Logical names. |
| `DIRIGIBLE_MULTI_TENANT_MODE` | `true` | Multi-tenant switch. |
| `DIRIGIBLE_TENANT_SUBDOMAIN_REGEX` | — | Tenant resolution. |
| `DIRIGIBLE_JAVASCRIPT_GRAALVM_DEBUGGER_PORT` | `8081` | Graalium JS debugger port. |
| `DIRIGIBLE_GRAALIUM_ENABLE_DEBUG` | `true` (in Docker) | Toggle JS debug. |
| `DIRIGIBLE_JAVA_DEBUG_JDWP_PORT` | `8000` | JDWP port used by the Java debugger view. |
| `DIRIGIBLE_JAVA_LSP_ENABLED` / `_INSTALL_DIR` | — | JDT.LS install controls. |
| `DIRIGIBLE_MAIL_*` | — | SMTP defaults for the Mail API. |
| `DIRIGIBLE_FLOWABLE_*` | — | Flowable engine datasource + mail settings. |
| `DIRIGIBLE_MS_SHAREPOINT_*` | — | SharePoint CMS credentials. |
| `DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER` | — | Internal CMIS root. |
| `DIRIGIBLE_SYNCHRONIZER_FREQUENCY` | — | Reconciliation cadence. |
| `DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT` / `_INTERVAL_MILLIS` | — | Cross-synchronizer retry tuning. |
| `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` | — | Tenant provisioning cadence. |
| `DIRIGIBLE_LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS` / `_MAX_IN_USE_SECONDS` | — | JDBC leak detection. |
| `DIRIGIBLE_EXEC_COMMAND_LOGGING_ENABLED` | — | Audit shell-exec calls. |
| `DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER` (`+ _AS_SUBFOLDER`, `+ _IGNORED_FOLDERS`) | — | Mount external registry. |
| `DIRIGIBLE_GITHUB_CLIENT_ID` / `_CLIENT_SECRET` / `_SCOPE` | — | GitHub OAuth (via `github` profile). |
| `DIRIGIBLE_S3_PROVIDER` | `aws` | S3 backend (`aws` / `localstack`). |
| `DIRIGIBLE_TRIAL_ENABLED` | — | Trial-mode flag. |

---

## 12. Observability

- **Spring Boot Admin** at `/spring-admin/`.
- **Actuator probes** at `/actuator/health/readiness`, `/actuator/health/liveness`.
- **OpenTelemetry** via `engine-open-telemetry` + Camel OpenTelemetry; configurable per the OTLP convention. Companion config under `open-telemetry/`.
- **Logs** — live in `components/ide/ide-logs` (REST: `LogsEndpoint`, `LogsConfigurationsEndpoint`; UI: `view-logs`, `view-loggers`).
- **Tracing UI** — `perspective-tracing`.

---

## 13. Extensibility

- **Extension points** — declarative (`*.extensionpoint` / `*.extension`) → discovered at runtime via `@dirigible/extensions`.
- **TS components** — `*Component.ts` + `engine-di` for Spring-style DI inside user code.
- **Java SPI** — `JavaClassConsumer` (and its `@Order`-driven chain) is the official hook for "react to compiled client classes." Future Java-runtime features plug in here rather than inventing a second synchronizer.
- **Custom synchronizers** — a new artefact type means *(entity + synchronizer + service/engine)*, then registration in the matching `components/group/group-*/pom.xml` aggregator.
- **Custom JS/TS APIs** — drop a Java module under `components/api/`, expose Java methods, ship the matching TS bundle under `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/` (published under `@aerokit/sdk/<area>`).
- **WebJar UI modules** — perspectives / views / editors / menus / templates are pluggable through `components/ui/` and `components/template/`.

---

## 14. Sample projects (under `dirigiblelabs/*`)

End-to-end tests that exercise the platform clone these:
`sample-entity-decorators`, `sample-java-entity-decorators`, `sample-roles-decorator`, `sample-job-decorator`, `sample-listener-decorator`, `sample-extension-decorator`, `sample-component-decorator`, `sample-websocket-decorator`, `sample-store-api`.

Each sample is a self-contained Dirigible project demonstrating one feature area; useful as documentation source material.

---

## 15. Downstream products (codbex family)

[codbex](https://www.codbex.com) is a commercially-curated product family built on top of Eclipse Dirigible. It bundles the upstream platform with additional content (industry modules, migration tools, branded shells) and ships as a set of editions, each tailored to a use case:

| Edition | Focus | Key inclusions |
| ------- | ----- | -------------- |
| **Atlas** | All-in-one platform | Every standard engine + tooling; the full-fat distribution. |
| **Helios** | Enterprise JS development | JS API + UI + Debugger + Git + Themes + Databases + Jobs + Listeners + WebSockets + Security + Extensions. |
| **Hades** | Database management | DB Explorer, SQL Console, Data Transfer, Import/Export, Anonymization (PostgreSQL, MySQL, HANA, H2, …). |
| **Oceanus** | Document management | CMIS-compliant docs browser + viewer + import/export + ZIP. |
| **Hyperion** | BPMN-first | BPMN Modeler, process definitions, instances, variables, inbox, dead-letter jobs. |
| **Iapetus** | Integrations | Integrations Modeler + Apache Camel flows. |
| **Rhea** | Model-driven | Entity, Forms and Reports modeling; the MDA stack. |
| **Kronos** | XSC compatibility | XS Classic + ABAP compatibility plugins (XSK, open-abap, abaplint). |
| **Phoebe** | Data-centric workflows | Apache Airflow integration. |
| **Gaia** | Reference modules | Master data + reference data starter content. |

### codbex-specific additions worth knowing

- **`.entity`** — JSON entity descriptor; rough equivalent of upstream `*Entity.ts`.
- **Industry modules** — `accounting`, `billing`, `e-commerce`, `employees`, `hotels`, `inventory`, `manufactoring`, `master-data`, `payments`, `payroll`, `projects`, `purchasing`, `reference-data`, `restaurants`, `salaries`, `sales`, `services`, `shipping`, `stores`, `timesheets`, `vacations` — ready-made domain projects layered on the platform.
- **Migration tooling** — XSC → JS (XSK), ABAP → JS (abaplint transpiler).
- **codbex SDK** — same shape as upstream `@aerokit/sdk/*`; alias `@dirigible/*` still resolves.

For doc generation: when codbex content meaningfully diverges from upstream (e.g. `.entity` vs `*Entity.ts`), flag it. Most pages can be shared verbatim.

---

## 16. External documentation pointers

### Dirigible (upstream)
- User-facing help portal: <https://www.dirigible.io/help/> — separate repo at <https://github.com/dirigible-io/dirigible-io.github.io>. MkDocs sources under `docs-help/docs/`, nav in `docs-help/mkdocs.yml`. Local checkout: `/Users/delchev/Data/GitHub/dirigible.io/dirigible-io.github.io`.
- API reference portal: same repo, `docs-api/docs/` (per-submodule pages — see §4.6).
- Blog posts: `docs-blogs/`; three coordinated updates per post (markdown source, `docs/blogs.json`, `docs-blogs/mkdocs.yml`).
- Samples portal: <https://samples.dirigible.io>.
- Trial: <https://trial.dirigible.io>.
- Slack: <https://slack.dirigible.io>.
- Issues: <https://github.com/eclipse-dirigible/dirigible/issues>.

### codbex (downstream)
- Product site & docs: <https://www.codbex.com>. VitePress-based; local checkout: `/Users/delchev/Data/GitHub/codbex/codbex.github.io`.
- Per-product GitHub repos under <https://github.com/codbex> (e.g. `codbex-atlas`, `codbex-helios`).

### Cross-reference map for doc generation

| This file's section | Help portal source |
| ------------------- | ------------------ |
| §1 Platform overview | `docs-help/docs/overview/index.md`, `architecture.md`, `features.md` |
| §2 Artefacts | `docs-help/docs/development/artifacts/`, `codbex.github.io/docs/documentation/platform/artefacts/` |
| §2.4 Modeler artefacts | `docs-help/docs/development/ide/modelers/`, `codbex.github.io/docs/documentation/tooling/modeling/` |
| §3 Engines | `docs-help/docs/overview/engines.md`, `codbex.github.io/docs/documentation/platform/engines/` |
| §4 JS/TS API | `docs-api/docs/<area>/<submodule>.md`, `codbex.github.io/docs/documentation/platform/sdk/<area>/` |
| §5 Client Java | (no portal section yet — emerging; the announcement blog `dirigible-io.github.io#123` will seed it) |
| §6 IDE | `docs-help/docs/development/ide/`, `codbex.github.io/docs/documentation/tooling/` |
| §7 Data layer | `codbex.github.io/docs/documentation/tooling/databases/` |
| §8 Security | `codbex.github.io/docs/documentation/configurations/basic-auth.md`, `client-registration.md`, `cognito-auth.md`, `keycloak-auth.md` |
| §11 Configuration | `docs-help/docs/setup/setup-environment-variables/` (note: lags `DirigibleConfig.java`) |
| §15 codbex products | `codbex.github.io/docs/products/` |

---

## 17. Pointers for doc generation

When generating human-readable documentation from this file:

1. **Treat §2 (Artefacts) as the table of contents.** Each artefact type deserves its own page: *Authoring* (file format + IDE editor) / *Runtime semantics* (engine that consumes it) / *APIs available to it* / *Examples* (link to the matching `dirigiblelabs/sample-*` repo or `tests/tests-integrations/src/main/resources/<TestName>/`).
2. **§4 (JS/TS APIs) is the canonical reference.** Group pages by domain (Data, Messaging, Process, Tooling, Testing); link each module to its TS `.d.ts` under `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/dist/dts/`.
3. **§5 (Client Java surface) deserves its own reference page** mirroring §4 for symmetry: annotation list, controller routing rules, `@Roles` semantics, `BeanProvider`, auto-OpenAPI.
4. **§6 (IDE)** maps 1:1 to the existing portal sections (Workbench, Database, Git, Operations, Documents, Settings, Tracing, Processes, Security, Jobs). Cross-link perspectives to the artefact types they edit.
5. **§7 (Data) and §8 (Security) → "Configuration & Operations"** in user docs; §11 (env-vars) belongs as an appendix and should be regenerated from `DirigibleConfig.java` rather than maintained by hand (the portal's env-vars page is known to lag the code by months).
6. **§9 (HTTP surface)** + the auto-generated OpenAPI document at `/services/openapi` cover the API reference; favour generating from OpenAPI over hand-writing tables.
7. **Refresh trigger.** Add a new artefact type → update §2; add a new engine → update §3; add a new `api-*` → update §4 (+ §4.6 submodule index); add new env-var → update §11 (or trust the regeneration story); add a new codbex edition → update §15.
8. **Reusable upstream/downstream content.** Most pages can be generated once and shared between dirigible help and codbex docs. Pivot on §15 to flag genuine divergence (codbex `.entity` vs upstream `*Entity.ts`, codbex edition matrix, codbex industry modules, ABAP/XSK support).
9. **Editor / modeler pages** should pair each §2.4 modeler artefact with the matching §6.2 editor and its generation pipeline under `components/template/template-application-*`. Screenshots live in the portal under `docs-help/docs/images/` and `docs-help/docs/development/ide/img/`.
