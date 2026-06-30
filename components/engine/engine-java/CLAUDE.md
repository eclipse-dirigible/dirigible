# Client Java code (`engine-java` + `data-store-java`)

Deep guide to the **client-Java development model** — the `.java` files a user drops under
`/registry/public/<project>/...`, compiled and run in-process. The model deliberately follows
**Spring Boot idioms**: a managed bean container, constructor injection, and annotation/interface
component shapes. Read this before changing anything under `engine-java`, `data-store-java`, the
`org.eclipse.dirigible.sdk.*` annotations in `api-modules-java`, or the `*-java` templates.

> The big realignment (PR [#6051](https://github.com/eclipse-dirigible/dirigible/pull/6051)) replaced
> the old service-locator model. **Removed: `RepositoryRegistry`, `RepositoryClassConsumer`, the
> `DependencyResolver` SPI, the reflective by-name handler fallback, the annotation+interface hybrid,
> and the `@Extension`/`@ExtensionPoint` annotations.** If you see those names anywhere, the doc/code
> is stale.

## Compile + load lifecycle (`JavaSynchronizer` → `JavaLoader`)

- `.java` sources ARE synchronized. `JavaSynchronizer.parseImpl` only parses + persists the `JavaFile`
  artefact (and enforces global FQN uniqueness); `finishing()` does the real work via
  `JavaLoader.rebuild()`: one `javac` task over **all** client sources, one fresh `ClientClassLoader`
  (parent = platform CL, so user code sees the SDK, Spring, Hibernate), then the bean container, then
  the behaviour consumers. The previous generation's CL becomes unreachable on swap → GC reclaims its
  Metaspace.
- `JavaLoader.rebuild()` order each generation: compile → load classes → unload-notify consumers for
  removed/replaced FQNs → swap the loader → **`componentContainer.rebuild(...)`** → load-notify
  consumers. So when a consumer runs, every bean is already built and injected.
- Platform classpath for `javac` comes from `ClassPathIndex` — it extracts `BOOT-INF/lib/*.jar` once
  to disk; **never** introspect nested fat-jar entries in-process (closes pooled `NestedJarFile`
  handles → cascading `NoClassDefFoundError`).
- **One `javac` batch = all-or-nothing.** Since every client `.java` across every project compiles in a
  single `javac` task, ONE uncompilable file fails the **whole** batch — a syntax error in one project
  silently unregisters EVERY project's `@Controller`/bean, so all `/services/java/...` endpoints 404
  (not just the offending project's). When endpoints vanish wholesale, find the first `javac error at …`
  in the `JavaSourceCompiler` log (or the IDE Problems view) — that one file is the cause.
- **Publish copies, it does not unpublish.** Deleting a `.java` from the workspace does NOT remove it
  from `/registry/public` on the next publish (publish is copy/overwrite, not sync-with-delete); the
  stale file lingers in the registry and keeps failing the batch. Remove it from the registry too
  (delete in the Projects/registry view, or the file under `/registry/public/...`), or it never goes
  away.

## The bean container (`ComponentContainer`, `engine-java`)

One Spring-singleton container, rebuilt per `ClientClassLoader` generation.

- A bean is any class (meta-)annotated `org.eclipse.dirigible.sdk.component.Component`. `@Repository`,
  `@Controller` and `@Websocket` are meta-`@Component` (beans without extra annotation). `@Scheduled`
  and `@Listener` are **method-level only** and are **not** meta-`@Component` — their host class must
  be a `@Component`.
- Bean name = `@Component("value")` or the decapitalized simple class name (Spring convention).
- Injection (resolved by type, order-independent, within the generation): **constructor** (preferred;
  single ctor auto-selected, else the `@Inject` one), **field** `@Inject`, and **collection** — a
  `List<T>`/`Set<T>`/`Collection<T>` injection point gets every bean assignable to `T`.
- Eager singletons; `@PostConstruct`/`@PreDestroy` (`jakarta.annotation`) run on build/teardown;
  construction cycles are detected and reported.
- `instanceOf(Class)` is an O(1) type-indexed lookup the consumers use to fetch the ready bean.
- Published to `ClientBeansHolder` (a `core-java` bean, package `org.eclipse.dirigible.engine.java.runtime`,
  alongside `ClientClassLoader`) so the SDK facade reaches client beans without a module cycle
  (`engine-java` → `api-modules-java` → `core-java`).
- **`Beans` facade** (`sdk.component.Beans`: `get(Class)`, `get(name, Class)`, `getAll(Class)`) is the
  client-facing lookup — resolves client beans first, then platform beans. Client code must **not**
  use the platform-internal `BeanProvider` (that's core-only; `JavaRepository.store()` uses it because
  it is platform code).

## Behaviour consumers (`JavaClassConsumer` SPI)

Consumers are pure **behaviour wirers** now — they fetch the already-built instance from the container
(`componentContainer.instanceOf(type)`) and register routes/schedules/subscriptions. They no longer
instantiate client classes.

- `EntityClassConsumer` (data-store-java) — `@Entity` → `JavaEntityManager` (Hibernate dynamic-map).
- `ControllerClassConsumer` — `@Controller` → `ControllerRouter` + OpenAPI via
  `JavaControllerOpenApiPublisher`. (A `@Controller` must not also implement `JavaHandler`.)
- `ScheduledClassConsumer` — jobs (see two styles below) → cron via a dedicated `ThreadPoolTaskScheduler`.
- `ListenerClassConsumer` — listeners → ActiveMQ; re-establishes the message's tenant context.
- `WebsocketClassConsumer` + `JavaWebsocketRegistry` — websockets; `WebsocketProcessor`
  (`engine-websockets`) calls `JavaWebsocketRegistry.dispatch(...)` reflectively (keeps that module free
  of an `engine-java` dependency).
- `HandlerClassConsumer` — `JavaHandler` (see below).

## Two handler styles — never mixed (jobs, listeners, websockets)

A `@Component` class uses **exactly one** style; the engine rejects (error-logs + skips) a class that
mixes them. There is **no** reflective by-name fallback.

| Component | Self-describing interface (no class annotation) | Method-level annotation |
|---|---|---|
| Job | `@Component implements JobHandler` → `String cron()` + `void run()` (like `org.quartz.Job`) | `@Scheduled(expression=…)` on a `@Component` method |
| Listener | `@Component implements MessageHandler` → `String destination()`, default `ListenerKind kind()`, `onMessage(String)`, default `onError` (like `jakarta.jms.MessageListener`) | `@Listener(name=…, kind=…)` on a `@Component` `void m(String)` method |
| WebSocket | `@Component implements WebsocketHandler` → `String endpoint()` + default lifecycle callbacks (like `TextWebSocketHandler`) | `@Websocket(endpoint=…)` class + `@OnOpen`/`@OnMessage`/`@OnError`/`@OnClose` methods (like Jakarta `@ServerEndpoint`; the endpoint has no method-level home) |

## `JavaHandler` (low-level REST)

`JavaEndpoint` (`/services/java/{project}/{*classPath}` + `/public/...`) tries `ControllerRouter` first,
then `JavaClassRegistry` + `JavaHandler.handle`. A `JavaHandler` that is also `@Component` is dispatched
as the container-built (injected) singleton; a plain `JavaHandler` (no `@Component`) is instantiated per
request via its no-arg constructor.

## Extension points (no annotation)

An extension point is a **plain Java interface**; a contribution is a `@Component` implementing it (its
`@Component` name is the contribution name). Consume via `List<Interface>` collection injection
(preferred) or `Extensions.find(Class)` (`sdk.extensions.Extensions`, which resolves the same beans).
`Extensions.getExtensions(String)` stays for cross-runtime TypeScript/JavaScript contributions.

## SDK annotations (`api-modules-java`, `org.eclipse.dirigible.sdk.*`)

All client annotations/facades live here (NOT the old `engine.java.annotations.*`): `component.{Component,
Inject, Repository, Beans}`, `http.{Controller, Get, Post, Put, Patch, Delete, Body, PathParam,
QueryParam, Context}`, `db.{Entity, Table, Id, GeneratedValue, GenerationType, Column, Transient,
CreatedAt/UpdatedAt/CreatedBy/UpdatedBy}`, `job.{Scheduled, JobHandler}`, `messaging.{Listener,
ListenerKind, MessageHandler}`, `net.{Websocket, WebsocketHandler, OnOpen, OnMessage, OnError, OnClose}`,
`extensions.Extensions`, `security.{Roles, User}`, `platform.Documentation`. `engine-java` has
`api-modules-java` on the compile classpath so client `.java` resolves them. The mirror of the TS
`@aerokit/sdk` surface is documented in `api-modules-java/README.md`.

## data-store-java

Hibernate **dynamic-map mode** — `session.save(entityName, Map<String,Object>)`, never the user's
`Class<?>` (sidesteps cross-classloader issues). `JavaEntityStore` is the typed CRUD API; `@Repository
extends JavaRepository<T>` is the recommended client pattern (`super(Entity.class)`; resolves the store
lazily). `EntityBeanMapper` does bean↔map; `JavaEntityToHbmMapper` reflects annotations → HBM XML
(shares `HbmXmlDescriptor` with `data-store` — audit both if you change either). SessionFactory roots at
the default user-data datasource, not SystemDB.

## Errors are surfaced to developers

Both **compile errors** (per line/column) and **bean-wiring errors** (unsatisfied/ambiguous dependency,
construction cycle, duplicate bean name, throwing constructor) are projected onto the IDE **Problems**
view and mark the `JavaFile` artefact `FAILED` (see `JavaSynchronizer.recordCompilationProblems` and
`ComponentContainer.wiringErrors()` carried on `RebuildResult`). Don't regress this — it's how a
browser-IDE developer sees what's wrong without reading the server log.

## Conventions / gotchas

- `@Roles` mirrors `UserFacade.isInRole` without pulling `api-security` (which would drag
  `engine-javascript`). Short-circuits on anonymous mode + `DEVELOPER`/`ADMINISTRATOR` super-roles.
- Controller routing: base path = class FQN with slashes; longest base path wins, literal beats
  `{placeholder}`; `TypeCoercer` → `400` on parse failure; `@Body` via Spring's primary `ObjectMapper`;
  return `void`/`String`/other → write-yourself / `text/plain` / JSON.
- Spring Boot strips `ResponseStatusException.getReason()` from the JSON body — ITs assert status code
  only, not body text.
- **Client code must be in a named package.** A type in the default (unnamed) package can't be imported
  or referenced from packaged code, so a default-package class is unusable from generated
  repositories/controllers — e.g. a calculated-field `org.eclipse.dirigible.sdk.db.CalculatedField`
  action must live in `custom.<x>` (not the project root). `import Foo;` (no dots) is illegal Java and,
  per the all-or-nothing batch above, fails every project's compile.

## Tests

- Unit (`engine-java/src/test`): `ComponentContainerTest`, `ControllerClassConsumer*Test`,
  `ControllerInvoker*Test`, `ControllerRouterTest`, `JavaLoaderTest`; (`data-store-java`)
  `JavaEntityToHbmMapperTest`, `EntityBeanMapperTest`, `CriteriaTest`.
- HTTP ITs (extend `IntegrationTest`, no Selenide): `JavaEngineIT` (handler lifecycle), `JavaComponentIT`
  (constructor + collection injection, and a `@Component` `JavaHandler`), `JavaNoMixingIT` (the
  no-mixing rejection), `JavaTemplateIT` (generated DAO/REST shape), `IntentEngineIT` (intent glue).

## Cross-repo effort (three repos)

- Platform: this repo, PR #6051.
- Samples: `dirigiblelabs/sample-java-{entity,listener,job,websocket,extension}-decorator` — each shows the
  styles above; the entity sample is the kitchen-sink. The `Java*DecoratorsSampleProjectIT` /
  `Java*DecoratorSampleProjectIT` clone these repos' HEAD, so **merge order is load-bearing**: the
  platform PR merges first; the sample-clone ITs are temporarily `@Disabled` until the sample PRs land
  (the samples' old API doesn't compile against the new engine). Re-enable them after.
- Docs: `dirigible-io/dirigible-io.github.io` — `/help/develop` (incl. a "Coming from Spring Boot"
  guide) and `/sdk`.
