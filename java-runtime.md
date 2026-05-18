# Java Runtime for User Code — Design Options

## Goal

Allow clients to author **Java** code inside their Dirigible projects (in addition to TS/JS), and have that code execute in the running platform with hot-reload, classloader isolation, and — ideally — full access to the Spring Boot container (DI, transactions, AOP, security, platform `api-*` beans).

Constraints:

- Open-source libraries only.
- Must compose with the existing **synchronizer** model (`/registry/public/<project>/...` → reconciled into runtime state by a `BaseSynchronizer`).
- Production-grade: no PoC-only patterns, no `SecurityManager` (removed in JDK 21).

## Options

### Option A — `JavaCompiler` + per-project `URLClassLoader`

Use the JDK's built-in `javax.tools.JavaCompiler` with an in-memory `JavaFileManager` to compile user `.java` files. Load each project under its own `URLClassLoader` whose parent exposes a curated set of API JARs (mirroring how `api-*` modules are exposed to GraalJS today).

**OSS pieces**

- `javax.tools` — JDK built-in.
- **Janino** (BSD-3) — optional, smaller compiler with snippet/expression support; used by Flink, Drools, Hibernate.
- **ByteBuddy** or **ASM** (Apache 2.0) — load-time bytecode rewriting to ban `System.exit`, `Runtime.exec`, reflection on platform internals.

**Hot reload**

Discard and replace the project's `ClassLoader` on file change, driven from a synchronizer's `UPDATE`/`STOP` phases.

**Tradeoff**

Simplest mental model, but we'd hand-roll all lifecycle plumbing (DI, lifecycle, extension points) ourselves, and classloader-leak hygiene is entirely on us.

---

### Option B — Plugin framework on top of A (**recommended**)

Adopt **PF4J** (Apache 2.0) for the plugin lifecycle, classloader isolation, and extension-point machinery. Each Dirigible project becomes a PF4J plugin; the platform publishes extension-point SPIs that user code implements.

**Why PF4J over OSGi (Felix/Equinox)**

OSGi gives more, but fights Spring Boot's flat classpath and adds operational weight (`MANIFEST.MF` headers, version ranges, the OSGi service registry). PF4J is the right size for this problem: a `PluginManager`, a per-plugin `ClassLoader`, an `@Extension` annotation, and a lifecycle. About one weekend to wire in.

**Tradeoff**

Slightly more rigid programming model (users implement `@Extension` classes against a published SPI rather than writing arbitrary entry points), which is arguably an improvement for maintainability.

---

### Option C — GraalVM Espresso (Java on Truffle)

We already ship GraalVM for `engine-javascript`. **Espresso** runs Java bytecode on the Truffle framework, giving native polyglot interop with the existing JS engine, real hot-swap, and per-context isolation tighter than `ClassLoader`-based isolation. Licensed UPL / GPL+CE.

**Tradeoff**

Still labeled experimental, runtime perf is noticeably below HotSpot, adds startup cost. Worth a spike for the sandboxing/polyglot story, but not the v1 choice.

---

## Recommendation

**Start with Option B (PF4J on top of the synchronizer model), integrated with Spring via `pf4j-spring`.** Lowest risk to production, well-trodden classloader-leak surface, and the loader implementation can be swapped for Espresso later if sandboxing requirements grow.

The synchronizer pattern maps cleanly:

| Synchronizer phase | Action                                                       |
| ------------------ | ------------------------------------------------------------ |
| `CREATE`           | Compile user sources → build plugin `ClassLoader` → `PluginManager.loadPlugin` → refresh child `ApplicationContext` |
| `UPDATE`           | `close()` child context, `unloadPlugin`, rebuild from scratch |
| `STOP` / `DELETE`  | `close()` child context, `unloadPlugin`, release CL          |

A `JavaPluginSynchronizer` watches a project directory (e.g. `<project>/java/` or a compiled `<project>/*.jar`), compiles via `JavaCompiler`, and hands the artifact to PF4J.

---

## Spring Boot integration

The pattern is **parent-child `ApplicationContext` per project**, with the user's `ClassLoader` attached to the child.

```
[Dirigible Spring Boot context]   ← all components/*, api-*, data sources
        ↑ parent
[Child context per project]       ← user @Component / @Service beans
        ↑
[user URLClassLoader]             ← compiled user .class files
```

The child sees all platform beans (datasources, tenant context, transaction manager, every `api-*` service); the parent never sees user beans, so user code cannot accidentally override platform internals.

**`pf4j-spring`** (Apache 2.0) does exactly this out of the box: each PF4J plugin gets its own `AnnotationConfigApplicationContext` whose parent is the main Spring context, with the plugin's `ClassLoader` set. ~300 lines of glue we'd otherwise hand-write.

### What works out of the box for user code

- `@Autowired` of any platform bean (`DataSource`, `JdbcTemplate`, `ITenantContext`, `IRepository`, …) — resolved through the parent.
- `@Transactional` — proxied at child-context creation, but the `PlatformTransactionManager` is the parent's, so user code participates in the same transactions as platform code.
- `@Scheduled`, `@EventListener`, `@Async` — work if the corresponding post-processors are enabled in the child context (or we share the parent's `TaskScheduler` / `ApplicationEventMulticaster`).
- Spring Security context — thread-local, propagates into user code automatically.
- Spring `ApplicationEvent`s — propagate child → parent by default. `pf4j-spring` can patch this for cross-plugin events if needed.

### The one real gotcha: `@RestController` from user code

`RequestMappingHandlerMapping` lives in the parent context and only scans its own beans. A `@RestController` declared in a child context is invisible to the dispatcher servlet by default. Two production-grade fixes:

1. **Re-register at runtime.** After each child `refresh()`, iterate user beans annotated `@RequestMapping` / `@RestController` and call `RequestMappingHandlerMapping.registerMapping(...)` on the parent. On `close()`, call `unregisterMapping(...)`. Standard hot-reload-Spring pattern. Users write idiomatic Spring controllers.
2. **Keep using the `Expose` artefact.** Users declare the endpoint via the existing Dirigible synchronizer (consistent with how JS/TS endpoints are exposed today) and implement a Java class behind it (e.g. a published `JavaServiceHandler` SPI). Less Spring-magical, single source of truth for URL routing.

**Suggested choice: option 2.** The `Expose` synchronizer is already where URL routing happens; bolting parallel route discovery into the parent dispatcher creates two sources of truth and complicates audit, security, and tenant scoping. We can revisit (1) later if users explicitly demand `@RestController`.

---

## Production hazards

These apply to all options; they bite hardest in Option B because Spring expands the surface.

- **ClassLoader leaks are the #1 risk.** Anything holding a strong ref to a stale plugin CL — `ApplicationEventMulticaster`, `@Async` executor thread-locals, AOP proxy caches, `ConversionService`, JDBC `DriverManager`, log4j thread-locals, MBean registries, GraalJS host bindings — prevents GC and bleeds Metaspace. Mitigations:
  - Call `ctx.close()` deterministically on every synchronizer `STOP` / `UPDATE`.
  - CI test that hot-reloads N times and asserts Metaspace returns to baseline.
  - Heap-dump diff after reload cycles to spot retained loaders.
- **TCCL discipline.** Wrap every entry into user code with `Thread.currentThread().setContextClassLoader(pluginCL)` / restore in a `finally`. Spring, JPA, Jackson, logging all consult the thread-context classloader.
- **No `SpringApplication` for the child context.** Use `AnnotationConfigApplicationContext` directly. Letting Boot auto-configuration re-run would re-bind ports, re-init datasources, and double-instantiate framework beans.
- **No user `@Entity` in v1.** JPA's `EntityManagerFactory` is built at platform startup with a fixed entity scan. Letting users contribute entities means rebuilding the EMF, which cascades into Hibernate's session factory and rarely composes with multi-tenancy. Force user code through `JdbcTemplate` or `IDataSourcesManager`.
- **Bean-definition overriding is globally enabled** (`spring.main.allow-bean-definition-overriding=true` in `application-common.properties`). Child contexts have their own `BeanFactory`, so a user bean named `dataSource` only shadows resolution *inside* the child — not globally. Worth documenting precedence rules explicitly.
- **Sandboxing without `SecurityManager`.** JDK 21 removed it. Realistic substitutes: bytecode allow/deny lists enforced at load time via ByteBuddy/ASM, JPMS module-layer restrictions, or eventually Espresso contexts. None is as comprehensive as the old `SecurityManager`; pick a threat model first.
- **API surface curation.** The plugin CL's parent must expose *only* the JARs we sanction. Anything reachable becomes de-facto public API and constrains future refactors. Treat the published SPI like a versioned product surface.

---

## Open questions before implementation

- **Source layout in user projects.** `<project>/java/**/*.java` compiled in-place, or a pre-built `<project>/*.jar` uploaded? (Recommend: support both, start with sources for parity with the JS experience.)
- **Build target.** Compile to Java 21 bytecode matching the platform, or pin user code to an older target for forward compatibility?
- **Multi-tenancy.** One child context per project regardless of tenant (use `ITenantContext` for tenant resolution inside user code), or one per `(project, tenant)` pair? Strongly prefer the former.
- **Published SPI.** Which extension points do we open first? Candidates: HTTP handlers (via `Expose`), BPM task delegates, Camel processors, scheduled jobs, event listeners.
- **Dependency declaration.** How do users add third-party libraries? PF4J supports per-plugin `lib/` directories; we'd publish a constrained allow-list or a BOM.
- **Hot reload signal.** File-watcher (consistent with synchronizers) vs. explicit "redeploy" action in the IDE.

---

## OSS components summary

| Component             | License    | Role                                                 |
| --------------------- | ---------- | ---------------------------------------------------- |
| `javax.tools`         | JDK        | Compile user `.java` sources in-process              |
| PF4J                  | Apache 2.0 | Plugin lifecycle, per-plugin `ClassLoader`           |
| `pf4j-spring`         | Apache 2.0 | Parent-child Spring `ApplicationContext` per plugin  |
| ByteBuddy / ASM       | Apache 2.0 | Load-time bytecode verification / API safety gates   |
| Janino                | BSD-3      | (Optional) Smaller compiler for snippets/expressions |
| GraalVM Espresso      | UPL / GPL+CE | (Optional, future) True isolation + polyglot interop |

---

## Suggested next step

A time-boxed spike (~1–2 weeks):

1. New module `components/engine/engine-java` with a `JavaPluginSynchronizer` watching `<project>/java/`.
2. Compile via `JavaCompiler` into an in-memory `JavaFileManager`.
3. Hand the resulting class bytes to PF4J; wire `pf4j-spring` so each plugin gets a child `ApplicationContext` parented to the main Dirigible context.
4. Expose a single SPI — e.g. `JavaServiceHandler` invoked via the existing `Expose` synchronizer — and ship one sample project under `tests/tests-integrations/src/main/resources/JavaPluginIT/`.
5. Add a hot-reload IT and a Metaspace-leak test that reloads a plugin 100× and asserts Metaspace returns to baseline.

If that lands cleanly, broaden the SPI (jobs, Camel processors, BPM delegates) in follow-ups.
