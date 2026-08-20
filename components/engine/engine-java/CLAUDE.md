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
  handles → cascading `NoClassDefFoundError`). It also appends the drop-in module jars (next section).

## AOT compiled modules + the `/modules` drop-in directory

A module can ship **already compiled** instead of as registry sources — no runtime `javac` at all
(PR [#6400](https://github.com/eclipse-dirigible/dirigible/pull/6400)). Such a module jar carries:

- its compiled classes (`gen.*` / `custom.*` packages),
- a marker at `META-INF/dirigible/<project>/.compiled` — a UTF-8 list of the module's top-level class
  binary names, one per line (`#` comments allowed),
- the module's declarative registry payload under the same `META-INF/dirigible/<project>/` folder.

On `ApplicationReadyEvent`, `CompiledModuleClassProvider` scans `classpath*:META-INF/dirigible/*/.compiled`,
`Class.forName`s every listed class through the **application** classloader and installs them via
`JavaLoader.installCompiledModules(...)` — the same install path a registry rebuild uses, so the standard
consumers register the controllers / entities / handlers (`Registered [N] class(es) from AOT compiled
module(s) on the classpath`). The installed generation is the **union** of the registry-compiled and the
classpath-compiled sub-generations: a later registry rebuild does not unload compiled modules. In
parallel, the existing `ClasspathExpander` lays the payload into `registry/public/<project>/`, so one jar
delivers both halves of the module.

**Getting such a jar onto the classpath of the shipped image (issue [#6592](https://github.com/eclipse-dirigible/dirigible/issues/6592)):**
`build/application/Dockerfile` launches through Spring Boot's **`PropertiesLauncher`** with
`-Dloader.path=/modules,...`, and creates an empty `/modules`. Since #6779 the launch is a plain
`java -jar` — the executable jar's **ZIP layout** makes `PropertiesLauncher` the manifest
`Main-Class` (so `loader.path` keeps working), and `-jar` is what makes the JVM honor the
`Launcher-Agent-Class` entry that powers `scope: "platform"` dependencies. A downstream image
`COPY`s module jars there, or they are volume-mounted at run time — **the platform jar
is consumed verbatim**; never explode the fat jar to add jars to `BOOT-INF/lib` (the launcher-agent
classes at the jar ROOT are injected by the build itself, with the nested jars kept STORED — see
`build/application/pom.xml`'s `inject-launcher-agent` execution and `LauncherAgentDeliveryIT`).

- **Empty or missing `/modules` is a no-op** — `PropertiesLauncher` reads `Start-Class` from the jar's
  manifest, so boot is identical to the pre-ZIP-layout launch (verified: same startup lines, same
  startup time).
- `loader.path` entries are **prepended** to `BOOT-INF/classes` + `BOOT-INF/lib`, so in principle a
  drop-in jar could shadow a platform class. In practice it cannot happen by accident: module packages
  are `gen.*` / `custom.*`, which the platform does not use.
- **`LOADER_PATH`** (env var, comma-separated) is the override for non-default locations — Spring Boot
  honors it natively, so there is no `DIRIGIBLE_*` property for this.
- `ClassPathIndex` appends the same `loader.path` / `LOADER_PATH` jars to the **compile** classpath, so
  registry sources can still be compiled against a drop-in module's classes.

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
- `ScheduledClassConsumer` — jobs (see two styles below) → a real `Job` row per tenant on the platform's
  **shared Quartz scheduler** (#6375), under the synthetic `RUNTIME_LOCATION_PREFIX` location so the job
  synchronizer does not reap it as a registry orphan. So a client-Java job is listed, enable/disable-able,
  trigger-now-able and job-logged in the Jobs perspective like any `.job`, and fires **once cluster-wide**
  — not once per JVM, as the private `ThreadPoolTaskScheduler` this replaced did. At fire time the jobs
  engine dispatches back through the `JavaJobExecutor` SPI (engine `java`); both that path and the manual
  trigger go through `JobHandlerRunner`, which is the ONLY place the engine→runner dispatch lives —
  trigger-now was written out separately once and stayed JavaScript-only, so triggering a client-Java job
  ran its class name as a JS path and 500'd (#6305).
- `ListenerClassConsumer` — listeners → ActiveMQ; re-establishes the message's tenant context.
  **A topic subscription is DURABLE**, because this consumer tears every subscription down and
  re-registers it on each republish, and a plain topic subscriber receives only what is published
  while it is connected — so an event raised in that window was dropped, and a record created
  mid-republish silently never started its process or ran its glue. The broker therefore holds a
  topic's messages against a `clientId` + subscription name derived from the handler's label and the
  tenant-resolved destination; that id must stay **stable**, since a varying one would open an empty
  subscription on every reload and strand the messages held for the previous one. Queues are left as
  plain consumers — a queue already retains messages for an absent consumer. Two consequences: a
  topic's messages now accumulate in the broker's store (SystemDB) while a handler is down instead of
  being discarded, and a subscription nobody reconnects to is reclaimed only by the broker's
  `offlineDurableSubscriberTimeout` (7 days, set in `MessagingConfig`) — the consumer cannot
  unsubscribe on its own, because `onClassUnloaded` fires for a *replaced* class as well as a deleted
  one, and a class deleted while the server was down is never reported at all.
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

**Throwing means different things in a job and in a listener — the idiom is identical, the outcome is
not.** A `JobHandler` that throws is caught by `JobExecutionService`, recorded as a **FAILED job-log
row** and rethrown to Quartz, so the failure is a first-class operational record: it shows up in the
Jobs perspective and in the Monitoring shell's failed-jobs tile, and the run can be triggered again.
A `MessageHandler` that throws is **logged with its stack trace** by `ListenerClassConsumer.dispatch`,
which then **rethrows so the failure reaches the broker**: the delivery is not acknowledged, and the
bounded redelivery policy the subscription configures (1s initial, 5s, exponential, 3 attempts — the
same budget the JavaScript listener path uses) retries it before the broker dead-letters it. So the
work is retried, but there is still **no job-log row and nothing to trigger by hand** — the log and
the dead-letter queue are the whole operational record. (Before that log line existed, a throwing
listener produced no output at all — the handler's own `onError` defaults to a no-op — and before the
rethrow, the message was acknowledged and the event lost for good.)

Two consequences worth internalizing before writing either kind of handler:

- **A listener throw is retried, not escalated.** The generated templates use the same
  `throw new RuntimeException(…)` idiom in `Job.java.template` and in
  `Notification`/`Integration.java.template`; in the job it becomes a re-runnable failed row, in the
  listener it becomes up to three more attempts and then a dead letter nobody is paged about. Neither
  one is a substitute for noticing.
- **A handler must therefore be safe to run twice.** Redelivery means the same message can arrive
  again after a partial write, so an event-sourced write in generated glue is written to be
  replayable — keyed on something durable, like the posting glue's back-reference — rather than
  transactional. Work that must not be lost still wants a reconciliation job that finds records left
  in a pre-handler state, because the dead-letter queue is where a poisonous message stops.

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
QueryParam, Context}`, `db.{Entity, Table, Id, GeneratedValue, GenerationType, Column, Lob, Transient,
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

**An absent id reads back as `null`, it does not throw.** `findById(id)` answers `null` when there is no such row, and `findOne(id)` is its `Optional` sibling — both are ordinary lookups, and the caller owns what absence means: a `404` at a controller boundary, a skip in an event handler. It used to throw `IllegalArgumentException` while its javadoc promised `null`, which made every documented `findById` + null-guard dead code — a controller looking a record up by a path parameter answered `500` for an unknown id, and a handler meant to skip a dangling FK failed its whole run instead (issue #6420; the generated glue and DAO templates were all written against the documented contract). Do NOT reintroduce a throwing lookup: a caller who needs "must exist" writes `findOne(id).orElseThrow(...)` and chooses its own failure. `JavaRepositoryFindByIdIT` covers it end-to-end.

**A large-text column needs `@Lob` — the mapping resizes the column to whatever it claims.** Entity registration runs Hibernate's `hbm2ddl.auto = update`, which does not only create missing tables: it ALTERS an existing column to match the mapping. A plain `String` property claims `@Column(length = ...)`, whose default is **255**, so a `CLOB` / `TEXT` column declared by the project's `.table` silently became a `VARCHAR(255)` on every deploy (issue #6346's recurring "Incompatible change ... VARCHAR to be changed to CLOB" was the schema layer noticing). Annotate the property `@Lob` and it is mapped past the dialect's maximum `VARCHAR`, which resolves to the database's own large-text type (`CLOB` on H2, `TEXT` on PostgreSQL) and leaves the column alone. Do NOT try to pin the type with `@Column(columnDefinition = ...)` — the mapper ignores it, and a raw SQL type name is not portable across dialects anyway. Generated entities don't need `@Lob`: an intent `type: text` field is a `VARCHAR(4000)` whose length the generated `@Column` declares. `JavaEntityLobColumnIT` covers the contract end-to-end.

**A write and the event announcing it commit together — the transactional outbox.** A repository that publishes an entity event does not commit the row and then call the broker: it hands the topic to the write itself (`save(entity, topic)`, `update(entity, topic[, extraEvents])`, `updateProperties(id, values, topic)`, `delete(entity, topic)`, `deleteById(id, topic)`), which records the event in the tenant's `DIRIGIBLE_EVENT_OUTBOX` **on the write's own connection, inside its transaction**, and only then — after the commit — hands it to the broker in-process. Two failures die with this: an event lost for good because the broker was briefly down while its row committed anyway (nothing retried it), and a `500` raised to a REST caller whose write had actually succeeded, inviting a retry that duplicated the record (issue #6816). What the in-process dispatch cannot deliver simply stays in the table, and `EventOutboxRelayJob` retries it per tenant every `DIRIGIBLE_EVENT_OUTBOX_RELAY_INTERVAL_SECONDS` (30) for entries idle longer than `DIRIGIBLE_EVENT_OUTBOX_RELAY_GRACE_SECONDS` (60). Consequences to keep in mind:

- **Delivery is at-least-once, not exactly-once.** "Sent" is only known once the entry is gone, so an entry published just before the node died is published again. Handlers must tolerate a repeat — which the generated glue already does, since it recomputes from the store rather than accumulating.
- **`Producer.sendToTopic` in hand-written client code is still a bare publish** with none of this. It is the raw messaging API; the outbox is reached only by giving a *write* its topic. Announce an entity change through its repository, not by publishing next to it. For an announcement that is deliberately DECOUPLED from any single write - deferred past a workflow chain's commit, or ordered after several transactions - use **`Producer.sendToTopicDurable`**: the message is recorded in the outbox in its own short transaction and the relay retries whatever the broker refuses, so an outage delays it instead of losing it (at-least-once; the generated deferred publishes - setField, Writer, Numbering, step events, the create-from completion announce - all use it).
- **The event's payload is the row as the transaction left it** — read back on the write's own connection for the targeted path, never a re-read afterwards that a concurrent write could have moved on. On a `multilingual: true` entity that means the untranslated row: an event carries canonical data, not the writer's `Accept-Language`.
- **A repository that overrides targeted writes must override the event-carrying form.** `updateProperties(id, values, topic)` is where a generated repository hangs its declarative checks, stored label and document resum; the plain two-argument form delegates to it. The base two-argument form deliberately does NOT re-dispatch, because `recalculate` reaches it through `super` precisely to bypass those semantics.
- **No outbox, no write.** If the entry cannot be recorded the transaction fails, which is the whole contract: a row whose event was never recorded is exactly the state this replaces. `JavaEventOutboxIT` covers both halves — an ordinary create reaching its listener, and an entry only the relay can deliver.

**Manage entities ONLY through their generated `<Entity>Repository` — NEVER the generic `Store`/`Database` for entity CRUD.** The generated repository (`@Repository extends JavaRepository<T>`) is the *only* sanctioned way to load/save/update/delete a managed entity, because it carries validations, **event publishing** (the create/`-updated`/`-deleted` topics that intent triggers/reactions/rollups/notifications listen on, recorded through the transactional outbox above), the multilingual read-overlay (a `multilingual: true` entity's finds translate string properties from its `<TABLE>_LANG` table for the caller's `Accept-Language` via `org.eclipse.dirigible.sdk.db.Translator`), and other per-entity behaviour. The generic `org.eclipse.dirigible.sdk.db.Store` (name-keyed dynamic map) and raw `Database` SQL **bypass all of that silently** and MUST NOT be used to read or mutate a managed entity. (`updateWithoutEvent` is fine — it's a deliberate repository method that keeps `super.update`'s validations/i18n and only omits the event, for workflow-driven system writes: intent SetField/Writer/trigger delegates.) The **targeted** writes are the write-back primitives a workflow should reach for instead of a full-row merge: `updateProperty`/`updateProperties` persist only the named columns (still gated by the entity's `checks:`, still refreshing a `label:`), `updateDerived` adds back the `-updated` event for recomputed totals. A generated repository routes those through its own bookkeeping (the `history:` trail as SYSTEM, the stored `label:` Name) and its declarative gates — except that `checks:` is skipped for a write touching only platform-owned columns (`ProcessId`), because recording WHICH process handles a record must not be refusable by a business gate. Consequence for a *reusable* delegate/service: it can't statically import a foreign `<Entity>Entity`, so the code that touches a specific entity must live **in that entity's project** (where it imports that project's repository); keep only entity-agnostic helpers (e.g. a number generator over its own `NumberRepository`) in a shared project. Don't make code "general" by reaching into arbitrary entities through `Store`.

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
  styles above; the entity sample is the kitchen-sink. `JavaSampleProjectsIT` clones all five repos'
  HEAD into one workspace and publishes them together, so **merge order is load-bearing**: the
  platform PR merges first; that IT is temporarily `@Disabled` until the sample PRs land (the samples'
  old API doesn't compile against the new engine). Re-enable it after.
- Docs: `dirigible-io/dirigible-io.github.io` — `/help/develop` (incl. a "Coming from Spring Boot"
  guide) and `/sdk`.
