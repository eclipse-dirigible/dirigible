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

Mapping to the help portal: *Modeling* → `docs-help/docs/development/ide/modelers/` and *Tutorials > Modeling* → `docs-help/docs/tutorials/modeling/`.

### 2.5 Repository layout an artefact lives in

```
/registry/public/<project>/...          # published artefacts (reconciled)
/users/<user>/workspace/<project>/...   # IDE workspaces (drafts)
```

See `IRepositoryStructure`. Multi-tenancy is on by default (`DIRIGIBLE_MULTI_TENANT_MODE=true`), so artefact reconciliation runs per tenant unless the synchronizer is single-tenant by design.

### 2.6 Authoring an artefact (template inventory)

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

## 6. Java API reference (annotations, facades, interfaces)

Authoritative enumeration of the Java-side surface intended for code review and reference-doc generation. Three sub-sections, each grouped by purpose:

- §6.1 **Annotations** — what user code can be tagged with.
- §6.2 **Annotation-like SPI markers** — platform-side qualifiers / decorators that change how Spring or the runtime treats a Java element.
- §6.3 **Facades** — Java classes that back the `@aerokit/sdk/*` JS/TS modules. Each facade is one-shot bridging layer: static methods are what JS sees.
- §6.4 **Public interfaces (SPI + platform contracts)** — the load-bearing interfaces other code is expected to implement or depend on.
- §6.5 **Key concrete classes** — non-interface platform classes that downstream code reaches into often enough to deserve mention.

### 6.1 Client-Java annotations

User-facing annotations defined in `components/engine/engine-java/src/main/java/org/eclipse/dirigible/engine/java/annotations/**`. These are on the compile-time classpath of every client `.java` source.

#### Entity / persistence (`org.eclipse.dirigible.engine.java.annotations`)

| Annotation | Target | Purpose |
| ---------- | ------ | ------- |
| `@Entity` | `TYPE` | Marks a class as a persistent entity; claimed by `EntityClassConsumer`. |
| `@Table(name=…)` | `TYPE` | Optional override of the table name. |
| `@Id` | `FIELD` | Primary key field. |
| `@GeneratedValue(strategy=GenerationType.…)` | `FIELD` | Auto-generated PK strategy (see `GenerationType` enum). |
| `@Column(name=…, length=…, nullable=…)` | `FIELD` | Column mapping override. |
| `@Transient` | `FIELD` | Skip this field during persistence / `EntityBeanMapper`. |
| `@CreatedAt` | `FIELD` | Auto-populated on insert with the current timestamp. |
| `@UpdatedAt` | `FIELD` | Auto-populated on every update. |
| `@CreatedBy` | `FIELD` | Auto-populated with `UserFacade.getName()` on insert. |
| `@UpdatedBy` | `FIELD` | Auto-populated with `UserFacade.getName()` on update. |
| `@Documentation(value=…)` | `TYPE`, `FIELD`, `METHOD` | Free-text description; surfaces in the OpenAPI summary for controllers and entity fields. |
| `@Repository` | `TYPE` | Marks a class as a singleton repository; claimed by `RepositoryClassConsumer` and registered in `RepositoryRegistry`. |
| `@Inject` | `FIELD` | Inject a `@Repository`-managed singleton into a `@Controller`; resolved by the chain of `DependencyResolver`s. |

(`GenerationType` is an `enum`, not an annotation — values mirror JPA: `AUTO`, `IDENTITY`, `SEQUENCE`, `TABLE`, `UUID`, …)

#### REST / HTTP (`org.eclipse.dirigible.engine.java.annotations.http`)

| Annotation | Target | Purpose |
| ---------- | ------ | ------- |
| `@Controller` | `TYPE` | Marks a class as a REST controller; base path = FQN with `.` → `/`. Claimed by `ControllerClassConsumer`. |
| `@Get(value=…)` | `METHOD` | GET route; `value` is a path suffix, supports `{name}` placeholders. |
| `@Post(value=…)` | `METHOD` | POST route. |
| `@Put(value=…)` | `METHOD` | PUT route. |
| `@Patch(value=…)` | `METHOD` | PATCH route. |
| `@Delete(value=…)` | `METHOD` | DELETE route. |
| `@Body` | `PARAMETER` | Bind the request body (JSON via Jackson). |
| `@PathParam(value=…)` | `PARAMETER` | Bind a `{name}` placeholder; type coercion via `TypeCoercer` (String / int / long / UUID / enum / boolean). |
| `@QueryParam(value=…)` | `PARAMETER` | Bind a query-string parameter. |
| `@Context` | `PARAMETER` | Bind the raw `HttpServletRequest` / `HttpServletResponse`. |
| `@Roles(value={…})` | `TYPE`, `METHOD` | Any-of role check via `HttpServletRequest.isUserInRole`. Method-level overrides class-level for that method. Short-circuits on anonymous mode and on the `DEVELOPER` / `ADMINISTRATOR` super-roles. |

Most longest-suffix / specificity rules live in `ControllerRouter.PathPattern.specificity`.

### 6.2 Platform-side annotation-like markers

Additional `@interface`s defined under `components/` that affect platform behavior. Most are Spring qualifiers.

| Annotation | Target | Purpose | Source |
| ---------- | ------ | ------- | ------ |
| `@DefaultTenant` | field / param / type / method | Spring `@Qualifier` for injecting the default-tenant bean. | `components/core/core-base/.../tenant/DefaultTenant.java` |
| `@Encrypted` | field | Marks a JPA field that should be encrypted-at-rest. | `components/core/core-base/.../encryption/Encrypted.java` |
| `@DefaultDataSourceName` | field / param / type / method | Spring `@Qualifier` for injecting the default datasource name string. | `components/data/data-sources/.../config/DefaultDataSourceName.java` |
| `@SystemDataSourceName` | field / param / type / method | Same shape, for the SystemDB datasource name. | `components/data/data-sources/.../config/SystemDataSourceName.java` |
| `@CalledFromJS` | any | Documentation marker on Java methods invoked from GraalJS user code — flags the static call surface so refactors don't break the JS bridge silently. | `modules/engines/engine-graalium/.../javascript/CalledFromJS.java` |

### 6.3 Facades (`*Facade`) — Java backings for `@aerokit/sdk/*`

Each facade is a Spring component or a static utility that exposes platform capabilities to GraalJS. The class name follows the `<Area>Facade` convention; the JS module path is `@aerokit/sdk/<area>/<submodule>`.

| Facade | Module | Area exposed |
| ------ | ------ | ------------ |
| `BpmFacade` | api-bpm | `@aerokit/sdk/bpm` — process start / task complete / variables. |
| `CacheFacade` | api-cache | `@aerokit/sdk/cache` — in-process / shared key-value cache. |
| `CmisFacade` | api-cms | `@aerokit/sdk/cms/cmis` — CMIS session, folders, documents. |
| `ComponentFacade` | api-component | `@aerokit/sdk/component` — DI lookups for `*Component.ts`. |
| `ContextFacade` | api-core | `@aerokit/sdk/core/context` — request-scoped key-value bag. |
| `DestinationsFacade` | api-core | Outbound HTTP destinations registry. |
| `EnvFacade` | api-core | `@aerokit/sdk/core/env` — environment variables. |
| `GlobalsFacade` | api-core | `@aerokit/sdk/core/globals` — process-wide variables. |
| `DatabaseFacade` | api-database | `@aerokit/sdk/db/database` — connections / queries / updates. |
| `DataStoreFacade` | api-database | `@aerokit/sdk/db/store` — Hibernate-backed entity store (dynamic map mode). |
| `EtcdFacade` | api-etcd | `@aerokit/sdk/etcd/client` — etcd K/V client. |
| `ExtensionsFacade` | api-extensions | `@aerokit/sdk/extensions/*` — discover and call `.extension` providers. |
| `GitFacade` | api-git | `@aerokit/sdk/git/client` — clone / pull / push / commit / log. |
| `HttpClientFacade`, `HttpClientAsyncFacade` | api-http | `@aerokit/sdk/http/client` and `client-async`. |
| `HttpRequestFacade`, `HttpResponseFacade`, `HttpSessionFacade`, `HttpUploadFacade` | api-http | Request / response / session / multipart upload helpers exposed to JS handlers. |
| `IndexingFacade` | api-indexing | `@aerokit/sdk/indexing/{searcher,writer}` — Lucene index ops. |
| `BytesFacade`, `FilesFacade`, `FTPFacade`, `ImageFacade`, `StreamsFacade`, `ZipFacade` | api-io | `@aerokit/sdk/io/{bytes,files,ftp,image,streams,zip}`. |
| `JobFacade` | api-job | `@aerokit/sdk/job/scheduler` — Quartz job control. |
| `KafkaFacade` | api-kafka | Kafka producer / consumer. |
| `LogFacade` | api-log | `@aerokit/sdk/log/logging` — SLF4J bridge. |
| `MailFacade` | api-mail | `@aerokit/sdk/mail/client`. |
| `MessagingFacade` | api-messaging | `@aerokit/sdk/messaging/{consumer,producer}`. |
| `MongoDBFacade` | api-mongodb | `@aerokit/sdk/mongodb/{client,dao}`. |
| `WebsocketsFacade` | api-net | `@aerokit/sdk/net/websocket`. |
| `PDFFacade` | api-pdf | `@aerokit/sdk/pdf/pdf` — PDF generation. |
| `CommandFacade` | api-platform | `@aerokit/sdk/platform/command` — shell command execution. |
| `EnginesFacade` | api-platform | `@aerokit/sdk/platform/engines` — list/configure platform engines. |
| `LifecycleFacade` | api-platform | `@aerokit/sdk/platform/lifecycle` — start/stop, restart, system shutdown. |
| `ProblemsFacade` | api-platform | `@aerokit/sdk/platform/problems` — register / clear validation problems. |
| `RegistryFacade` | api-platform | `@aerokit/sdk/platform/registry` — registry browsing. |
| `RepositoryFacade` | api-platform | `@aerokit/sdk/platform/repository-*` — low-level repository CRUD. |
| `WorkspaceFacade` | api-platform | `@aerokit/sdk/platform/workspace-*` — workspace/project ops. |
| `RabbitMQFacade` | api-rabbitmq | RabbitMQ producer / consumer. |
| `RedisFacade` | api-redis | `@aerokit/sdk/redis/client`. |
| `S3Facade` | api-s3 | AWS S3 client (CMS-S3 backing). |
| `UserFacade` | api-security | `@aerokit/sdk/security/user` — current user, roles, anonymous-mode checks (canonical source of role semantics, mirrored by `@Roles`). |
| `SharepointFacade` | api-sharepoint | MS SharePoint CMS backing. |
| `TemplateEnginesFacade` | api-template | `@aerokit/sdk/template/engines` — Mustache / Velocity / JS templating. |
| `Base64Facade`, `DigestFacade`, `EscapeFacade`, `HexFacade`, `QRCodeFacade`, `UTF8Facade`, `UrlFacade`, `UuidFacade`, `Xml2JsonFacade` | api-utils | `@aerokit/sdk/utils/*`. |
| `ThreadContextFacade` | core-base + commons-helpers | Internal: per-request thread-context propagation. |
| `TracingFacade` | core-tracing | Internal: OpenTelemetry span helper. |
| `TestResultsFacade` | ide-junit-results | IDE-side façade for JUnit test results. |

### 6.4 Public interfaces (SPI + platform contracts)

Interfaces that are either explicit SPIs (intended for extension), or load-bearing platform contracts that consumers depend on. Grouped by area; the list is not exhaustive — full coverage lives in the source.

#### Synchronization / artefact reconciliation (`components/core/core-base/.../artefact/`, `.../synchronizer/`)

| Interface | Role |
| --------- | ---- |
| `Synchronizer<A, ID>` | Top-level synchronizer contract. Extended by `BaseSynchronizer` (single-tenant) and `MultitenantBaseSynchronizer`. |
| `SynchronizerCallback` | Callback for synchronization lifecycle events (per artefact). |
| `SynchronizersOrder` | Constants for the fixed cross-synchronizer ordering. |
| `ArtefactService<A, ID>` | CRUD façade over an artefact's JPA repository (extended by every `*Service`). |
| `ArtefactRepository<A, ID>` | Spring-Data repository contract for artefacts. |
| `Engine` | Identifier-and-metadata contract for runtime engines (`getName()`, `getProvider()`). |
| `Auditable` | Mapped-superclass marker for created/updated audit fields. |
| `TopologicallySortable` / `TopologicallyDepletable` | Helpers for ordering artefacts by dependency. |
| `PublisherHandler` | Hook for the project-publish pipeline. |

#### Engine-Java SPI (`components/engine/engine-java/src/main/java/org/eclipse/dirigible/engine/java/spi/` and `.../handler/`)

| Interface / record | Role |
| ------------------ | ---- |
| `JavaClassConsumer` | Pluggable consumer of compiled client classes; `accepts` + `onClassLoaded` + `onClassUnloaded`. Built-ins: `EntityClassConsumer`, `RepositoryClassConsumer`, `ControllerClassConsumer`, `HandlerClassConsumer`. |
| `DependencyResolver` | Resolves `@Inject` field types from a chain of resolvers; primary impl is `RepositoryRegistry`. |
| `JavaHandler` | Catch-all handler shape: `handle(HttpServletRequest, HttpServletResponse)` mapped to a single URL. |
| `LoadedClass` (record) | Descriptor passed to consumers — `{project, fqn, type, loader}`. |

#### Tenant / multitenancy (`components/core/core-base/.../tenant/`)

| Interface | Role |
| --------- | ---- |
| `Tenant` | Tenant identity (id, name, subdomain, status). |
| `TenantContext` | Per-request tenant context provider. |
| `TenantResult<T>` | Tenant-scoped result wrapper. |
| `TenantProvisioningStep` / `TenantPostProvisioningStep` | Hooks for tenant onboarding and post-provisioning. |

#### Repository (`modules/repository/repository-api/`)

| Interface | Role |
| --------- | ---- |
| `IRepository` | Top-level repository contract. The "registry" abstraction the synchronizer reconciles from. |
| `IRepositoryStructure` | Canonical path constants (`/registry/public/...`, `/users/<user>/workspace/...`). |
| `IMasterRepository` | Read-only mirror of a master/upstream repository. |
| `IRepositoryReader` / `IRepositoryWriter` | Split read/write halves of `IRepository`. |
| `IRepositoryImporter` / `IRepositoryExporter` | Zip/import/export contract. |
| `IRepositorySearch` | Lucene-backed search. |
| `IRepositoryCache` | Optional caching layer. |
| `ICollection` / `IResource` / `IEntity` / `IEntityInformation` | Per-node interfaces (folder, file, common, metadata). |
| `IResourceVersion` | Version history for a resource. |

#### Database SQL (`modules/database/database-sql/`)

| Interface | Role |
| --------- | ---- |
| `ISqlFactory` | Entry point to obtain `ISqlDialect` / `ISqlBuilder` for a connection. |
| `ISqlDialect` | Per-RDBMS dialect (column types, escaping, identifier quoting, etc.). |
| `ISqlDialectProvider` | SPI for registering a new dialect. |
| `ISqlBuilder` | Fluent DDL/DML builder. |
| `ISqlKeywords` | Reserved-keyword constants per dialect. |
| `DatabaseArtifactTypes` | Constants for HDB-style artefact types. |
| `IEntityManagerInterceptor`, `IPersistenceProcessor` | Lower-level persistence hooks (`database-persistence`). |

#### Core platform (`components/core/core-database/`, `core-base/spring/`)

| Interface | Role |
| --------- | ---- |
| `DirigibleDataSource` | Marker for the platform-managed `DataSource`. |
| `DirigibleConnection` | Wrapped JDBC `Connection`. |
| `DatabaseConfigurator` | Hook for per-RDBMS connection configuration. |
| `DatabaseSystemAware` | Implemented by beans that need to know the underlying DB system. |
| `ConnectionEnhancer` | Adapt a `Connection` (e.g. for tracing / multi-tenancy). |
| `DatabaseParameters` | Constants used by datasource wiring. |
| `ParameterizedByName` / `ParameterizedByIndex` | Strategy for named vs positional JDBC parameters. |
| `UserAccessVerifier` | Plug-in role / access verification. |
| `CustomSecurityConfigurator` | Add Spring Security configuration without forking `BasicSecurityConfig`. |
| `SynchronizationWalkerCallback` | Callback used by the initializer / registry walker. |
| `LocalRegistryWatcherHandler` | Hook into local filesystem changes under the registry. |
| `DataSourceLifecycleListener` | React to datasource registration / lifecycle. |

#### CMS (`components/engine/engine-cms/`)

| Interface | Role |
| --------- | ---- |
| `CmsProvider` | Pluggable CMS backend (internal / S3 / SharePoint). |
| `CmsProviderFactory` | Factory selecting a `CmsProvider` based on configuration. |
| `CmisSession`, `CmisObject`, `CmisObjectFactory`, `CmisFolder`, `CmisDocument`, `CmisContentStream`, `CmisConstants` | CMIS-style object model exposed to user code. |

#### BPM (`components/engine/engine-bpm/`, `engine-bpm-flowable/`)

| Interface | Role |
| --------- | ---- |
| `BpmProvider` | Pluggable BPMN engine provider (Flowable is the default). |
| `SystemBpmProcess` | System-internal process descriptor. |
| `TaskService` | Inbox / task operations. |

#### OData (`components/engine/engine-odata/`, `modules/odata/odata-core/`)

| Interface | Role |
| --------- | ---- |
| `TableMetadataProvider` | Supplies OData metadata for a table/view. |
| `ODataPropertyNameEscaper` | Per-RDBMS identifier escaping. |
| `OData2EventHandler` | Lifecycle events around OData requests. |
| `SQLStatementBuilder`, `SQLClause`, `SQLProcessor`, `SQLStatement`, `SQLInterceptor` | Internals of the OData-to-SQL pipeline. |

#### Camel / IDE / misc

| Interface | Role |
| --------- | ---- |
| `DirigibleJavaScriptInvoker` | Camel processor hook into GraalJS. |
| `IGitConnector` | IDE Git operations façade. |
| `TemplateEngine` | Plug-in template engine (Mustache / Velocity / JS). |
| `MailConfigurationProvider` | Override SMTP settings per tenant / context. |
| `ProjectStatusProvider` | Compute workspace project status (clean / modified / errors). |
| `GenerationParameters` | Parameter contract for project generation templates. |
| `ParamSetter`, `HeaderFormatter`, `ResultSetWriter`, `RowFormatter` | Database-tooling SPIs (data-core). |
| `DataTransferCallbackHandler` | Hooks for the data-transfer process. |
| `ApplicationListenersOrder` | Constants for ordering `ApplicationListener` beans. |
| `CallableNoResultAndNoException`, `CallableResultAndNoException`, `CallableNoResultAndException`, `CallableResultAndException` | Typed `Runnable`/`Callable` variants used across the codebase. |

#### Graalium / JS runtime (`modules/engines/engine-graalium/`)

| Interface | Role |
| --------- | ---- |
| `CodeRunner` | Top-level runner contract; specialized by `JavascriptCodeRunner`, `PythonCodeRunner`. |
| `PythonCodeRunner` | Python runtime hook. |
| `DirigibleJavascriptHooksProvider` | Plug into the JS context lifecycle (before/after eval). |
| `JavascriptSourceProvider` | Resolve source modules; default is repository-backed. |
| `ExternalModuleResolver` | Resolve external (non-`@aerokit/sdk/*`) imports. |
| `ModuleResolver` | Internal resolver chain. |
| `GraalJSInterceptor` | Per-execution interception (timing, tracing, sandboxing). |
| `JavascriptPolyfill` | Browser-like polyfill registration. |
| `GlobalObject`, `GlobalFunction` | Inject globals into the JS context. |

### 6.5 Key concrete classes worth knowing

Not interfaces, but used widely enough that reviewers and doc writers should treat them as canonical reference points.

| Class | Where | What it does |
| ----- | ----- | ------------ |
| `BeanProvider` | `components/core/core-base/.../spring/BeanProvider.java` | Static `getBean(Class)` lookup — the only way to fetch platform beans from a client `.java` class (those are not Spring-scanned). |
| `Configuration` / `DirigibleConfig` | `modules/commons/commons-config/.../config/` | Source of truth for `DIRIGIBLE_*` env-vars; new tunables must be added here, not read ad-hoc. |
| `StaticObjects` | `modules/commons/commons-config/` | Legacy lookup registry — predates Spring. New code must use injection instead. |
| `Artefact` | `components/core/core-base/.../artefact/Artefact.java` | JPA mapped-superclass extended by every artefact entity. |
| `BaseSynchronizer<A, ID>` / `MultitenantBaseSynchronizer<A, ID>` | `components/core/core-base/.../synchronizer/` | Abstract base classes extended by every concrete synchronizer. |
| `BaseEndpoint` | `components/core/core-base/.../endpoint/BaseEndpoint.java` | URL constants (`PREFIX_ENDPOINT_*`) — `/services/...` and `/public/...` roots. |
| `JavaEntityStore` | `components/data/data-store-java/.../store/JavaEntityStore.java` | Typed CRUD over Hibernate dynamic-map mode for client `@Entity` classes. |
| `EntityBeanMapper` | `components/data/data-store-java/.../store/EntityBeanMapper.java` | Bean ↔ `Map<String,Object>` conversion respecting `@Column` / `@Transient`. |
| `JavaEntityManager` | `components/data/data-store-java/.../manager/JavaEntityManager.java` | Per-class registration into Hibernate. |
| `RepositoryRegistry` | `components/data/data-store-java/.../repository/RepositoryRegistry.java` | Singleton store of `@Repository` instances; primary `DependencyResolver`. |
| `JavaEntityToHbmMapper` | `components/data/data-store-java/.../hbm/JavaEntityToHbmMapper.java` | Reflects `@Entity` classes into HBM XML descriptors (`HbmXmlDescriptor`, `HbmPropertyDescriptor` from data-store). |
| `JavaSynchronizer` | `components/engine/engine-java/.../synchronizer/JavaSynchronizer.java` | The single Java compilation + classloading cycle; calls every `JavaClassConsumer` in `@Order`. |
| `JavaEndpoint` | `components/engine/engine-java/.../endpoint/JavaEndpoint.java` | URL pattern `/services/java/{project}/{*classPath}`; controllers first, then handlers, then 404. |
| `ControllerRouter` | engine-java | Spring-style routing: longest-basePath + most-specific-path wins. |
| `ControllerInvoker` | engine-java | Argument binding (`@Body`/`@PathParam`/`@QueryParam`/`@Context`), `@Roles` check, return-value writing. |
| `TypeCoercer` | engine-java | Path-parameter / query-parameter coercion (String / int / long / UUID / enum / boolean). |
| `JavaControllerOpenApiPublisher` | engine-java | Reflects each `@Controller` into an OpenAPI 3 fragment at `java-controller://<project>::<fqn>`. |
| `ClientClassLoader` / `ClassPathIndex` | engine-java | Custom classloader for client classes + safe `BOOT-INF/lib` extraction (see CLAUDE.md "Spring Boot 3 fat-jar classpath is fragile"). |
| `OpenAPIService` | engine-openapi | Stores OpenAPI fragments; consumed by `OpenAPIEndpoint`. |
| `DataSourcesManager` | components/data/data-sources | Resolves named datasources (`DefaultDB`, `SystemDB`, user-defined). |
| `SynchronizationProcessor` | core-initializers | Force synchronous reconciliation (used in HTTP-only ITs via `forceProcessSynchronizers()`). |

---

## 7. In-browser IDE

The IDE is composed of WebJar UI modules under `components/ui/` plus backend services under `components/ide/`. It is the user's primary entry point.

### 7.1 Perspectives (`components/ui/perspective-*`)
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

### 7.2 Editors (`components/ui/editor-*`)
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

### 7.3 Views (`components/ui/view-*`)
Side / bottom panels: `view-artefacts`, `view-configurations`, `view-console`, `view-databases`, `view-data-structures`, `view-debugger` (JS), `view-java-debug`, `view-extensions`, `view-git`, `view-import`, `view-jobs`, `view-listeners`, `view-loggers`, `view-logs`, `view-preview`, `view-problems`, `view-projects`, `view-properties`, `view-registry`, `view-repository`, `view-search`, `view-security`, `view-sql`, `view-swagger`, `view-terminal`, `view-transfer`, `view-translation`, `view-websockets`, `view-welcome`.

### 7.4 Menus (`components/ui/menu-*`)
Per-perspective top-bar menus: `menu-bpm`, `menu-camel`, `menu-csv`, `menu-database`, `menu-entity`, `menu-extensions`, `menu-form-builder`, `menu-help`, `menu-jobs`, `menu-listeners`, `menu-mapping`, `menu-projects`, `menu-schema`, `menu-security`, `menu-websockets`.

### 7.5 IDE backends (`components/ide/`)
- `ide-workspace` — workspace + transport REST API (`WorkspaceEndpoint`, `WorkspacesEndpoint`, `WorkspaceSearchEndpoint`, `WorkspaceFindEndpoint`, `WorkspaceActionsEndpoint`, `TransportEndpoint`, `PublisherEndpoint`).
- `ide-git` — Git over HTTP (`GitEndpoint`).
- `ide-terminal` — Browser terminal backed by `ttyd` on port 9000 (`TerminalWebsocketClientEndpoint`).
- `ide-logs` — Live log streaming and logger configuration.
- `ide-problems` — Compilation/validation problems list.
- `ide-template` — Project generation from templates (`GenerationEndpoint`).
- `ide-java-lsp` — JDT.LS based Java language server (completion, navigation, refactor).
- `ide-java-debug` — DAP bridge from the browser to JDT.LS to a JDWP target (see CLAUDE.md §"Java Debugger" for full architecture; default JDWP port `DIRIGIBLE_JAVA_DEBUG_JDWP_PORT=8000`).
- `ide-junit-results` — JUnit test report viewer.

### 7.6 Shell, branding, locale
- `shell-ide` — top-level frame composing the perspectives and the global toolbar (default entrypoint, configurable via `DIRIGIBLE_HOME_URL`).
- `platform-branding` — logos, titles, prefixes.
- `resources-theme-blimpkit` / `-classic` / `-high-contrast` / `-mystic` — themes.
- `settings-locale`, `resources-locale` — i18n.

### 7.7 BlimpKit UI component library
The IDE chrome (sidebars, dialogs, tabs, menus, toolbars, forms) is built on [BlimpKit](https://blimpkit.dev/) — a SAP-Fundamental-derived Angular component library. Custom perspectives / views should consume BlimpKit components for consistent look and feel; the catalogue is the canonical reference (do not invent ad-hoc styling).

---

## 8. Data layer

### 8.1 Supported databases (`modules/database/`)
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

### 8.2 Data tooling components
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

### 8.3 Repository (`modules/repository/`)
- `repository-api`, `repository-local`, `repository-master`, `repository-cache`, `repository-search`, `repository-zip` — pluggable storage abstraction backing the registry (file-system local is the default).

---

## 9. Security

- **Authentication backends (`components/security/`).** `security-basic` (form/basic login, default `admin`/`admin`), `security-oauth2`, `security-keycloak`, `security-cognito`, `security-snowflake`, `security-client-registration` (UI for registering OAuth clients at runtime).
- **GitHub OAuth profile.** Activated by the `github` Spring profile via `DIRIGIBLE_GITHUB_CLIENT_ID` / `_CLIENT_SECRET` / `_SCOPE`. Configured in `application-github.properties`.
- **Authorization model.** Declarative `.access` rules + `.roles` definitions, enforced by `engine-security` (URL patterns) and by `@Roles` in client Java (`ControllerInvoker.checkRoles`). Built-in super-roles: `DEVELOPER`, `ADMINISTRATOR`. Anonymous mode toggled by `Configuration.isAnonymousModeEnabled()` / `isAnonymousUserEnabled()`.
- **Multi-tenancy.** On by default (`DIRIGIBLE_MULTI_TENANT_MODE=true`). Tenant resolution is subdomain-based (`DIRIGIBLE_TENANT_SUBDOMAIN_REGEX`); single-realm options exist for Keycloak / Cognito.
  - **Tenant-isolated:** DataSources, CSV import/export, OData services, Documents (CMS storage), scheduled Jobs, Listeners. Each artefact reconciliation runs per tenant via `MultitenantBaseSynchronizer`.
  - **System-level (NOT tenant-isolated):** BPMN process instances, Camel integration flows, declared Extensions, the Git perspective, development workspaces (`/users/<user>/workspace/...`).

---

## 10. HTTP surface

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

## 11. Tooling around the code

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

## 12. Configuration

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

## 13. Observability

- **Spring Boot Admin** at `/spring-admin/`.
- **Actuator probes** at `/actuator/health/readiness`, `/actuator/health/liveness`.
- **OpenTelemetry** via `engine-open-telemetry` + Camel OpenTelemetry; configurable per the OTLP convention. Companion config under `open-telemetry/`.
- **Logs** — live in `components/ide/ide-logs` (REST: `LogsEndpoint`, `LogsConfigurationsEndpoint`; UI: `view-logs`, `view-loggers`).
- **Tracing UI** — `perspective-tracing`.

---

## 14. Extensibility

- **Extension points** — declarative (`*.extensionpoint` / `*.extension`) → discovered at runtime via `@dirigible/extensions`.
- **TS components** — `*Component.ts` + `engine-di` for Spring-style DI inside user code.
- **Java SPI** — `JavaClassConsumer` (and its `@Order`-driven chain) is the official hook for "react to compiled client classes." Future Java-runtime features plug in here rather than inventing a second synchronizer.
- **Custom synchronizers** — a new artefact type means *(entity + synchronizer + service/engine)*, then registration in the matching `components/group/group-*/pom.xml` aggregator.
- **Custom JS/TS APIs** — drop a Java module under `components/api/`, expose Java methods, ship the matching TS bundle under `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/` (published under `@aerokit/sdk/<area>`).
- **WebJar UI modules** — perspectives / views / editors / menus / templates are pluggable through `components/ui/` and `components/template/`.

---

## 15. Sample projects (under `dirigiblelabs/*`)

End-to-end tests that exercise the platform clone these:
`sample-entity-decorators`, `sample-java-entity-decorators`, `sample-roles-decorator`, `sample-job-decorator`, `sample-listener-decorator`, `sample-extension-decorator`, `sample-component-decorator`, `sample-websocket-decorator`, `sample-store-api`.

Each sample is a self-contained Dirigible project demonstrating one feature area; useful as documentation source material.

---

## 16. External documentation pointers

- User-facing help portal: <https://www.dirigible.io/help/> — separate repo at <https://github.com/dirigible-io/dirigible-io.github.io>. MkDocs sources under `docs-help/docs/`, nav in `docs-help/mkdocs.yml`. Local checkout: `/Users/delchev/Data/GitHub/dirigible.io/dirigible-io.github.io`.
- API reference portal: same repo, `docs-api/docs/` (per-submodule pages — see §4.6).
- Blog posts: `docs-blogs/`; three coordinated updates per post (markdown source, `docs/blogs.json`, `docs-blogs/mkdocs.yml`).
- Samples portal: <https://samples.dirigible.io>.
- Trial: <https://trial.dirigible.io>.
- Slack: <https://slack.dirigible.io>.
- Issues: <https://github.com/eclipse-dirigible/dirigible/issues>.

### Cross-reference map for doc generation

| This file's section | Help portal source |
| ------------------- | ------------------ |
| §1 Platform overview | `docs-help/docs/overview/index.md`, `architecture.md`, `features.md` |
| §2 Artefacts | `docs-help/docs/development/artifacts/` |
| §2.4 Modeler artefacts | `docs-help/docs/development/ide/modelers/` |
| §3 Engines | `docs-help/docs/overview/engines.md` |
| §4 JS/TS API | `docs-api/docs/<area>/<submodule>.md` |
| §5 Client Java | (no portal section yet — emerging; the announcement blog `dirigible-io.github.io#123` will seed it) |
| §6 Java API reference | Source files under `components/api/api-*/`, `components/engine/engine-java/.../annotations/`, `components/core/core-base/`, `modules/repository/repository-api/`, `modules/database/database-sql/` |
| §7 IDE | `docs-help/docs/development/ide/` |
| §8 Data layer | `docs-help/docs/development/concepts/`, `docs-help/docs/development/ide/perspectives/database.md` |
| §9 Security | `docs-help/docs/setup/`, `docs-help/docs/development/ide/views/access.md`, `roles.md` |
| §12 Configuration | `docs-help/docs/setup/setup-environment-variables/` (note: lags `DirigibleConfig.java`) |

---

## 17. Pointers for doc generation

When generating human-readable documentation from this file:

1. **Treat §2 (Artefacts) as the table of contents.** Each artefact type deserves its own page: *Authoring* (file format + IDE editor) / *Runtime semantics* (engine that consumes it) / *APIs available to it* / *Examples* (link to the matching `dirigiblelabs/sample-*` repo or `tests/tests-integrations/src/main/resources/<TestName>/`).
2. **§4 (JS/TS APIs) is the canonical reference.** Group pages by domain (Data, Messaging, Process, Tooling, Testing); link each module to its TS `.d.ts` under `components/api/api-modules-javascript/src/main/resources/META-INF/dirigible/modules/dist/dts/`.
3. **§5 (Client Java surface) deserves its own reference page** mirroring §4 for symmetry: annotation list, controller routing rules, `@Roles` semantics, `BeanProvider`, auto-OpenAPI.
4. **§6 (Java API reference)** is the canonical Java-side surface for review and reference generation. Treat each annotation / facade / interface row as a docs entry; pair the user-visible client annotations (§6.1–6.2) with the §5 narrative, and reserve §6.3 (facades) for the JS/TS-bridge appendix.
5. **§7 (IDE)** maps 1:1 to the existing portal sections (Workbench, Database, Git, Operations, Documents, Settings, Tracing, Processes, Security, Jobs). Cross-link perspectives to the artefact types they edit.
6. **§8 (Data) and §9 (Security) → "Configuration & Operations"** in user docs; §12 (env-vars) belongs as an appendix and should be regenerated from `DirigibleConfig.java` rather than maintained by hand (the portal's env-vars page is known to lag the code by months).
7. **§10 (HTTP surface)** + the auto-generated OpenAPI document at `/services/openapi` cover the API reference; favour generating from OpenAPI over hand-writing tables.
8. **Refresh trigger.** Add a new artefact type → update §2; add a new engine → update §3; add a new `api-*` → update §4 (+ §4.6 submodule index) and §6.3; add a new annotation → update §6.1; add a new env-var → update §12 (or trust the regeneration story).
9. **Editor / modeler pages** should pair each §2.4 modeler artefact with the matching §7.2 editor and its generation pipeline under `components/template/template-application-*`. Screenshots live in the portal under `docs-help/docs/images/` and `docs-help/docs/development/ide/img/`.
