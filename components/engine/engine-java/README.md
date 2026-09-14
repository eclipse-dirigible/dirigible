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

### AOT compiled modules and the `/modules` drop-in directory

A module can also ship **already compiled**, with no runtime `javac` at all. Such a module jar carries
its compiled classes (packages `gen.*` / `custom.*`), a marker at
`META-INF/dirigible/<project>/.compiled` listing the module's top-level class binary names (one per
line, `#` comments allowed), and the module's declarative registry payload under the same
`META-INF/dirigible/<project>/` folder.

On `ApplicationReadyEvent`, `CompiledModuleClassProvider` scans the classpath for those markers, loads
each listed class through the application classloader and installs them via
`JavaLoader.installCompiledModules(...)` — the same install path a registry rebuild uses, so the standard
consumers register the module's controllers, entities and handlers (`Registered [N] class(es) from AOT
compiled module(s) on the classpath`). `ClasspathExpander` lays the payload into
`registry/public/<project>/` in parallel, so a single jar delivers both halves of the module.

The runtime image (`build/application/Dockerfile`) makes such jars deployable without touching the
platform artifact: it launches through Spring Boot's `PropertiesLauncher` with `-Dloader.path=/modules`
and ships an empty `/modules` directory. Drop module jars in — by `COPY` in a downstream image, or by
mounting a volume — and they are on the application classpath; there is no need to explode the fat jar.

- Empty or missing `/modules` is a **no-op**: `PropertiesLauncher` reads `Start-Class` from the jar's
  manifest, so the boot sequence and startup time match a plain `java -jar` launch.
- `loader.path` entries precede the fat jar's own `BOOT-INF/classes` + `BOOT-INF/lib`, so a drop-in jar
  could in principle shadow a platform class — in practice it cannot happen by accident, because module
  packages are `gen.*` / `custom.*`, which the platform does not use.
- **`LOADER_PATH`** (comma-separated, honored natively by `PropertiesLauncher`) overrides the location;
  no Dirigible configuration property is involved.
- `ClassPathIndex` appends the same entries to the compile classpath, so registry sources can be
  compiled against a drop-in module's classes.

### Hot reload + classloader hygiene

Each source unit gets a **fresh** `BytecodeClassLoader`. On update, the registry's `put` atomically
replaces the prior `LoadedHandler` — its `ClassLoader` (and the `Class` it defined) become
unreachable as soon as no in-flight request still holds them, and the JVM reclaims the class
metadata at the next GC.

The endpoint switches the **thread-context classloader** to the user code's loader during dispatch
and restores it in a `finally` block. This is essential for frameworks consulted from within user
code (Jackson, JPA, logging) to resolve user types correctly.

## Building a compiled module jar (AOT) - step by step

The previous section describes what the runtime does with a compiled module. This one describes how
to **produce** one from an ordinary Dirigible project, with nothing but a JDK, the platform's own
runtime image, and `jar`. Nothing here is specific to any vendor or suite: the contract is three
things in one jar, and any build tool can emit them.

### What goes into the jar

```
<project>-<version>.jar
├── META-INF/dirigible/<project>/.compiled        the marker: top-level class binary names, one per line
├── META-INF/dirigible/<project>/**               the registry payload: every NON-.java file of the project
│                                                  (.model/.edm/.csvim + CSVs, .bpmn, .form, .report, .extension,
│                                                   generated UI, i18n, ...) - laid into registry/public/<project>/
└── <package path>/*.class                        the compiled Java, at ordinary package paths
```

Rules:

- **`<project>` is the registry project folder name** (the folder under `registry/public/`), and it
  must be the same in the marker path, the payload path and the class packages the project's Java
  uses. It is also the natural Maven `artifactId`.
- **Ship no `.java`.** Source next to classes would make the registry synchronizer compile it again
  and the two generations would clash on every FQN.
- **A project with no Java ships without a marker**, not with an empty one. `ClasspathExpander`
  lays its payload down regardless; `CompiledModuleClassProvider` only scans jars that carry a marker.
- **Leave out build metadata** that is not runtime content (`package.json`, `project.json`, tests,
  `node_modules`).
- **The platform is not a dependency of the jar.** The runtime image provides every platform class;
  the jar declares only its dependencies on OTHER compiled modules (below).

### 1. Get the compile classpath from the runtime image

Compile against the very jar the classes will load into - the pinned `dirigiblelabs/dirigible:<version>`
image - never against a separately downloaded build. The Spring Boot fat jar is exploded once;
`BOOT-INF/classes` plus `BOOT-INF/lib/*` is the SDK classpath.

```bash
VERSION=14.56.0
cid=$(docker create dirigiblelabs/dirigible:$VERSION)
docker cp "$cid:/dirigible.jar" platform.jar && docker rm -f "$cid"
mkdir sdk && (cd sdk && jar xf ../platform.jar BOOT-INF/classes BOOT-INF/lib)
SDK="$PWD/sdk/BOOT-INF/classes:$PWD/sdk/BOOT-INF/lib/*"
```

Keep `BOOT-INF/lib/*` as a **wildcard classpath entry**. The platform ships on the order of a
thousand jars; spelled out one per entry the classpath exceeds the Linux per-argument limit
(`MAX_ARG_STRLEN`, 128 KB) and `javac` fails with "Argument list too long". Use JDK 21, the platform's
own compile level.

### 2. Stage the marker and the payload

Given a project checkout at `./myproject` (the folder that contains the `.model`, the generated
`gen/` and hand-written `custom/` Java, and so on):

```bash
PROJECT=myproject
OUT=build/$PROJECT
PAYLOAD=$OUT/META-INF/dirigible/$PROJECT
mkdir -p "$PAYLOAD"

# the payload: everything that is not Java or build metadata
rsync -a --exclude '*.java' --exclude test/ --exclude node_modules/ \
      --exclude package.json --exclude project.json --exclude .npmrc "$PROJECT/" "$PAYLOAD/"

# the marker: <package>.<file stem> for every top-level Java type (a public top-level type == its file)
find "$PROJECT" -name '*.java' -not -path '*/test/*' | while read -r f; do
  pkg=$(sed -n 's/^package \(.*\);/\1/p' "$f" | head -1)
  echo "${pkg:+$pkg.}$(basename "$f" .java)"
done | sort > "$PAYLOAD/.compiled"
```

Only write the marker when the `find` produced at least one class; a payload-only project skips it.

### 3. Compile and assemble

```bash
# other compiled modules this project's Java imports go on the classpath too, one jar each
javac -cp "$SDK:build/othermodule.jar" -d "$OUT" $(find "$PROJECT" -name '*.java' -not -path '*/test/*')

jar cf "build/$PROJECT.jar" -C "$OUT" .

# sanity: classes present, exactly one marker, no source
jar tf "build/$PROJECT.jar" | grep -c '\.class$'
jar tf "build/$PROJECT.jar" | grep -c "/$PROJECT/\.compiled$"      # 1
jar tf "build/$PROJECT.jar" | grep -c '\.java$'                     # 0
```

Optionally stamp the platform version into the manifest (`jar cfm ... MANIFEST.MF`) so a published
jar's compile target stays auditable - a jar and the runtime it loads into must be the same platform
line.

### 4. Modules that depend on each other

When project B's Java imports project A's classes (a cross-model relation, a delegate reading
another module's entities, a generated print feeder), B compiles against A's **compiled jar**, so:

- **the dependency graph must be a DAG.** Two projects importing each other compile fine as registry
  sources (one flat `javac` over all projects) and cannot be built at all as separate jars - neither
  can be first. Break the cycle at the model level before adopting AOT.
- **build leaf-first**: owners before consumers, each consumer seeing the platform plus only the jars
  it actually references. A shared "all jars" directory on every module's classpath hides an
  undeclared dependency until an image without that module fails to boot.
- **derive the dependency list from the source**, not from the model alone: grep the project's Java
  for `import <otherproject's package>` and union it with the model's declared cross-model
  references. Hand-written code can import an owner the model never mentions.
- **pin exact versions** when publishing to a Maven repository: a POM `<dependency>` per referenced
  module at the version compiled against, never a range. The platform itself is NOT declared.

A minimal POM for `mvn deploy:deploy-file`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example.modules</groupId>
  <artifactId>myproject</artifactId>
  <version>1.4.0</version>
  <packaging>jar</packaging>
  <dependencies>
    <dependency>
      <groupId>com.example.modules</groupId>
      <artifactId>othermodule</artifactId>
      <version>2.1.0</version>
    </dependency>
  </dependencies>
</project>
```

### 5. Put the jars on the classpath of an image

The shipped image already launches through `PropertiesLauncher` with `-Dloader.path=/modules` and
an empty `/modules`. A downstream image only copies jars in:

```dockerfile
FROM dirigiblelabs/dirigible:14.56.0
COPY build/*.jar /modules/
```

Or mount them at run time: `docker run -v "$PWD/build:/modules" ... dirigiblelabs/dirigible:14.56.0`.
The platform jar is consumed verbatim - do not explode it, do not add jars under `BOOT-INF/lib`.
`LOADER_PATH` (comma-separated) relocates the directory if `/modules` is unsuitable.

For a Maven-hosted set of modules, a two-stage Dockerfile resolves them first (`mvn
dependency:copy-dependencies -DoutputDirectory=/modules` from an aggregator POM listing the module
coordinates) and copies the result into the runtime stage. Resolve each version from the
repository's `maven-metadata.xml` and pin it; GitHub Packages, for one, emits `<latest>` and no
`<release>` element.

Every project the image bundles must be delivered one way only: a project both dropped in as a jar
and published from source produces the `FQN ... provided by BOTH` warning and the registry copy wins.
Never put `DIRIGIBLE_DEPENDENCIES_DIR` (the resolved `project.json` Maven dependencies) on
`loader.path` - see the previous section.

### 6. Verify the boot

Readiness is **functional**, never a log line. `Started ...Application` means the port is open, and
`Processing synchronizers completed` fires **before** `ApplicationReadyEvent`, when the compiled
classes register. Measured at either point the instance shows 0 registered classes and looks exactly
like "the jars were ignored". Poll a REST endpoint one of the modules serves until it answers, then
read the log:

```bash
until curl -sf -u admin:admin http://localhost:8080/services/java/myproject/<package path>/<Controller> >/dev/null; do sleep 5; done
docker logs app > boot.log 2>&1

grep -oE 'Registered \[[0-9]+\] class\(es\) from AOT' boot.log   # N must equal the SUM of all markers
grep -ci 'JavaSourceCompiler' boot.log                           # 0 - nothing was compiled at boot
grep -c 'Registered controller' boot.log                          # the REST surface is up
grep -ciE 'CsvimProcessingException|Failed to import|Incompatible change|Table metadata was not found' boot.log   # 0
```

Assert the **exact** class count, the sum of every `.compiled` marker's non-comment lines across
`/modules`, rather than a floor: a floor waves through an image where one jar was silently skipped
(a class that fails `Class.forName` is logged as an ERROR naming its FQN - usually an owner jar the
image does not bundle). A jar that builds is not evidence that it loads; the boot is.

This flow was run end to end on `dirigiblelabs/dirigible:14.56.0` with real generated modules: a
leaf module, then a consumer compiled against the leaf's jar (and failing with
`package ... does not exist` when the leaf's jar was omitted), zero `javac` at boot, and the
controllers served 200.

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
