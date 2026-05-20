# Engine - Java

Runtime engine for **client-supplied Java sources** in Eclipse Dirigible projects.

## What it does

Discovers `*.java` files placed under the registry, compiles them in-process via the JDK
`JavaCompiler` API, loads the resulting bytecode under per-source isolated `ClassLoader`s, and
exposes them as REST endpoints with **hot reload** on every source change.

The engine follows the standard Dirigible synchronizer pattern (see
[BaseSynchronizer](../../../components/core/core-base/src/main/java/org/eclipse/dirigible/components/base/synchronizer/BaseSynchronizer.java))
and persists discovered sources as `JavaFile` artefacts in `DIRIGIBLE_JAVA_FILES`.

## Authoring a Java endpoint

1. In any Dirigible project under the registry, drop a `.java` file. Example —
   `myproject/com/example/Hello.java`:

   ```java
   package com.example;

   import jakarta.servlet.http.HttpServletRequest;
   import jakarta.servlet.http.HttpServletResponse;
   import org.eclipse.dirigible.engine.java.handler.JavaHandler;

   public class Hello implements JavaHandler {
       @Override
       public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
           response.setContentType("application/json");
           response.getWriter().write("{\"message\": \"hello from java\"}");
       }
   }
   ```

2. Publish the project. The synchronizer picks up the file, compiles it, and registers the class.

3. Invoke it:

   ```
   GET http://localhost:8080/services/java/myproject/com/example/Hello
   ```

   The unauthenticated variant lives at `/public/java/myproject/com/example/Hello`.

4. Edit the file, republish — the next request hits the new code with no restart.

## URL convention

| Registry path                                             | URL                                                          |
| --------------------------------------------------------- | ------------------------------------------------------------ |
| `/registry/public/<project>/<package-path>/<Class>.java`  | `/services/java/<project>/<package-path>/<Class>`            |

The `package` declaration in the source must match the directory layout (standard Java convention).

## End-to-end verification

The engine is exercised against the running fat jar — drop a file, hit the endpoint, modify it, hit again, then delete it:

```bash
# 1. write v1
mkdir -p ./target/dirigible/repository/root/registry/public/sample-java/demo
cat > .../sample-java/demo/Hello.java <<'EOF'
package demo;
import jakarta.servlet.http.*;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
public class Hello implements JavaHandler {
    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("application/json");
        resp.getWriter().write("{\"message\": \"hello from java v1\"}");
    }
}
EOF

# 2. (wait for sync ~10s)
curl -u admin:admin http://localhost:8080/services/java/sample-java/demo/Hello
# → {"message": "hello from java v1"}

# 3. modify file in place, sync picks up the change, classloader is replaced
# 4. delete the file, sync detects absence, calls cleanup, classloader is unloaded
curl -u admin:admin http://localhost:8080/services/java/sample-java/demo/Hello
# → 404 with "No Java handler registered for [sample-java/demo.Hello]"
```

This exact flow was validated with the bundled Spring Boot fat jar build.

## Architecture

```
┌──────────────────────┐
│   *.java in registry │
└──────────┬───────────┘
           │ scanned every cycle
           ▼
┌──────────────────────┐    parseImpl   ┌────────────────────┐
│   JavaSynchronizer   │───────────────▶│  JavaFile artefact │ (JPA)
│  @Order(65)          │                └────────────────────┘
└──────────┬───────────┘
           │ completeImpl(CREATE / UPDATE / DELETE)
           ▼
┌──────────────────────┐
│      JavaLoader      │   ── compile ──▶  JavaSourceCompiler   (javax.tools)
│                      │   ── define ──▶  BytecodeClassLoader   (per-source, fresh)
└──────────┬───────────┘
           │ register
           ▼
┌──────────────────────┐                    ┌────────────────────┐
│  JavaClassRegistry   │◀───────────────────│    JavaEndpoint    │  /services/java/**
└──────────────────────┘   find at request  │  REST controller   │  /public/java/**
                                            └────────────────────┘
```

### Compile-time classpath in a fat-jar runtime

A Spring Boot 3 fat jar uses a custom `LaunchedClassLoader` that resolves classes from
nested `BOOT-INF/lib/*.jar` entries via pooled `NestedJarFile` handles. The JDK
`javax.tools.JavaCompiler` cannot see those nested jars through `java.class.path`, and
reading them in-process (via `getResourceAsStream`, or via aggressive classpath scanners
like ClassGraph) closes the pooled handles and breaks the running application's class loading.

The engine sidesteps this by cracking the outer fat jar with the standard
`java.util.jar.JarFile` — bypassing Spring Boot's loader entirely — and extracting every
`BOOT-INF/lib/*.jar` plus the `BOOT-INF/classes/` tree to a temp directory under
`$TMPDIR/dirigible-engine-java-*` once on first compile. Those on-disk paths are bound to
`javac`'s `--class-path` via `setLocationFromPaths(CLASS_PATH, ...)`. The extraction is
cleaned up on shutdown via `DisposableBean#destroy()`.

### Hot reload + classloader hygiene

Each source unit gets a **fresh** `BytecodeClassLoader`. On update, the registry's `put` atomically
replaces the prior `LoadedHandler` — its `ClassLoader` (and the `Class` it defined) become
unreachable as soon as no in-flight request still holds them, and the JVM reclaims the class
metadata at the next GC.

The endpoint switches the **thread-context classloader** to the user code's loader during dispatch
and restores it in a `finally` block. This is essential for frameworks consulted from within user
code (Jackson, JPA, logging) to resolve user types correctly.

## Caveats / current limitations

- **Sandboxing.** Loaded user code runs with the same JVM permissions as the platform — there is
  no `SecurityManager` shim (deprecated/removed in JDK 21). Bytecode allow-listing via ByteBuddy/ASM
  is the recommended next step before exposing the engine to untrusted operators.
- **JPA `@Entity` from user code.** Not supported: the `EntityManagerFactory` is built at platform
  startup with a fixed entity scan. Use `JdbcTemplate` / `IDataSourcesManager` for now.
- **No Spring DI inside user classes.** Constructor-injected dependencies are not wired by the
  framework (handlers are instantiated reflectively via the no-arg constructor). To resolve a
  platform bean, use `BeanProvider.getBean(...)` explicitly. A parent-child `ApplicationContext`
  model (`pf4j-spring` style) is the natural follow-up if Spring-native authoring is required.
- **No annotation processors at compile time** (`-proc:none`).
- **Single top-level class per file** is assumed (standard Java rule).

## Modules and references

- Synchronizer base: `components/core/core-base/.../synchronizer/BaseSynchronizer.java`
- Existing parallel implementation: `components/engine/engine-web/.../synchronizer/ExposesSynchronizer.java`
- Endpoint patterns: `components/engine/engine-javascript/.../endpoint/JavascriptEndpoint.java`

See the parent design document at `java-runtime.md` in the repository root for the broader
options analysis and recommendation context.
