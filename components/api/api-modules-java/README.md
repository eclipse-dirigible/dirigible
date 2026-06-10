# api-modules-java

Java mirror of the `@aerokit/sdk` TypeScript/JavaScript SDK shipped from
`components/api/api-modules-javascript`. Every class under `org.eclipse.dirigible.sdk.*` delegates
to the same `components/api/api-*` Java facades the GraalJS bridge uses, so client `.java` sources
dropped under `/registry/public/<project>/...` and compiled by `components/engine/engine-java`
import the SDK the same way TS/JS code imports `@aerokit/sdk/*`:

```ts
import { Logging } from "@aerokit/sdk/log";
import { Files }   from "@aerokit/sdk/io";

const log = Logging.getLogger("demo");
log.info("file size: {}", Files.size("/users/admin/workspace/proj/foo.txt"));
```

```java
import org.eclipse.dirigible.sdk.log.Logging;
import org.eclipse.dirigible.sdk.io.Files;

var log = Logging.getLogger("demo");
log.info("file size: {}", Files.size("/users/admin/workspace/proj/foo.txt"));
```

## Module map

| TS path                              | Java class                                                     | Notes |
|--------------------------------------|----------------------------------------------------------------|-------|
| `bpm/deployer`                       | `sdk.bpm.Deployer`                                             |       |
| `bpm/process`                        | `sdk.bpm.Process`                                              | Exposes the raw Flowable `BpmProviderFlowable` via `getEngine()` for advanced cases. |
| `bpm/tasks`                          | `sdk.bpm.Tasks`                                                |       |
| `cache`                              | `sdk.cache.Cache`                                              |       |
| `cms/cmis`                           | `sdk.cms.Cmis`                                                 | Returns the raw Apache Chemistry `Session`. |
| `core/configurations`                | `sdk.core.Configurations`                                      | Delegates to `Configuration` (no JS facade existed). |
| `core/context`                       | `sdk.core.Context`                                             |       |
| `core/env`                           | `sdk.core.Env`                                                 |       |
| `core/globals`                       | `sdk.core.Globals`                                             |       |
| `db/database` + `db/sequence` + `db/query` + `db/insert` + `db/update` + `db/sql` + `db/procedure` | `sdk.db.Database` | One static facade with the full `DatabaseFacade` surface. |
| `db/store`                           | `sdk.db.Store`                                                 | Dynamic-entity Hibernate store. For typed `@Entity` CRUD on client classes, resolve `JavaEntityStore` via `BeanProvider`. |
| `etcd/client`                        | `sdk.etcd.Client`                                              | Returns the raw `io.etcd.jetcd.KV`. |
| `extensions/extensions`              | `sdk.extensions.Extensions`                                    |       |
| `git/client`                         | `sdk.git.Git`                                                  |       |
| `http/client`                        | `sdk.http.HttpClient`                                          | Options passed as JSON, same shape as TS. |
| `http/request`                       | `sdk.http.Request`                                             |       |
| `http/response`                      | `sdk.http.Response`                                            |       |
| `http/session`                       | `sdk.http.Session`                                             |       |
| `http/upload`                        | `sdk.http.Upload`                                              |       |
| `http/utils`                         | `sdk.http.HttpUtils`                                           |       |
| `indexing/searcher`                  | `sdk.indexing.Searcher`                                        |       |
| `indexing/writer`                    | `sdk.indexing.Writer`                                          |       |
| `integrations/integrations`          | `sdk.integrations.Integrations`                                | Empty placeholder — Camel `Processor` users work with `Exchange` directly. |
| `io/bytes`                           | `sdk.io.Bytes`                                                 |       |
| `io/files`                           | `sdk.io.Files`                                                 |       |
| `io/image`                           | `sdk.io.Image`                                                 |       |
| `io/streams`                         | `sdk.io.Streams`                                               |       |
| `io/zip`                             | `sdk.io.Zip`                                                   |       |
| `job/scheduler`                      | `sdk.job.Scheduler`                                            | Job creation is annotation-driven; see "Decorators" below. |
| `junit`                              | `sdk.junit.Assert`                                             | Plain `AssertionError`s; use real JUnit 5 in test sources. |
| `kafka/producer`                     | `sdk.kafka.Producer`                                           |       |
| `kafka/consumer`                     | `sdk.kafka.Consumer`                                           |       |
| `log/logging`                        | `sdk.log.Logging` + `sdk.log.Logger`                           |       |
| `mail/client`                        | `sdk.mail.Mail`                                                |       |
| `messaging/producer`                 | `sdk.messaging.Producer`                                       |       |
| `messaging/consumer`                 | `sdk.messaging.Consumer`                                       |       |
| `mongodb/client`                     | `sdk.mongodb.Client`                                           | Returns the raw `com.mongodb.client.MongoClient`. |
| `net/websockets`                     | `sdk.net.Websockets`                                           |       |
| `net/soap`                           | `sdk.net.Soap`                                                 | Implemented directly against `jakarta.xml.soap`. |
| `pdf/pdf`                            | `sdk.pdf.Pdf`                                                  |       |
| `platform/command`                   | `sdk.platform.Command`                                         |       |
| `platform/engines`                   | `sdk.platform.Engine`                                          |       |
| `platform/lifecycle`                 | `sdk.platform.Lifecycle`                                       |       |
| `platform/os`                        | `sdk.platform.Os`                                              | Implemented against `Runtime` / system properties. |
| `platform/problems`                  | `sdk.platform.Problems`                                        |       |
| `platform/registry`                  | `sdk.platform.Registry`                                        |       |
| `platform/repository`                | `sdk.platform.Repository`                                      |       |
| `platform/workspace`                 | `sdk.platform.Workspace`                                       | Returns the platform `Workspace` / `File` domain objects directly. |
| `qldb`                               | `sdk.qldb.Qldb`                                                | `open(ledger, table)` returns the platform `QLDBRepository`. |
| `rabbitmq/producer`                  | `sdk.rabbitmq.Producer`                                        |       |
| `rabbitmq/consumer`                  | `sdk.rabbitmq.Consumer`                                        |       |
| `redis/client`                       | `sdk.redis.Client`                                             | Returns the raw `redis.clients.jedis.Jedis`. |
| `security/user`                      | `sdk.security.User`                                            |       |
| `template/engines`                   | `sdk.template.TemplateEngines`                                 |       |
| `utils/alphanumeric`                 | `sdk.utils.Alphanumeric`                                       | `SecureRandom`-backed. |
| `utils/base64`                       | `sdk.utils.Base64`                                             |       |
| `utils/converter`                    | `sdk.utils.Converter`                                          | Jackson-backed JSON helpers. |
| `utils/digest`                       | `sdk.utils.Digest`                                             |       |
| `utils/escape`                       | `sdk.utils.Escape`                                             |       |
| `utils/hex`                          | `sdk.utils.Hex`                                                |       |
| `utils/qrcode`                       | `sdk.utils.QrCode`                                             |       |
| `utils/url`                          | `sdk.utils.Url`                                                | Named `Url` to avoid clashing with `java.net.URL`. |
| `utils/utf8`                         | `sdk.utils.Utf8`                                               |       |
| `utils/uuid`                         | `sdk.utils.Uuid`                                               |       |
| `utils/xml`                          | `sdk.utils.Xml`                                                |       |

## Annotations

The annotations that used to live in `org.eclipse.dirigible.engine.java.annotations.*` now ship
from this module so the SDK surface — facades **and** decorators — has a single root
(`org.eclipse.dirigible.sdk.*`). The mapping:

| TS decorator                              | Java annotation                                                                  |
|-------------------------------------------|----------------------------------------------------------------------------------|
| `http/decorators#Controller`              | `org.eclipse.dirigible.sdk.http.Controller`                                      |
| `http/decorators#Get / Post / Put / Patch / Delete` | `org.eclipse.dirigible.sdk.http.{Get, Post, Put, Patch, Delete}`       |
| `http/decorators#Body / PathParam / QueryParam / Context` | `org.eclipse.dirigible.sdk.http.{Body, PathParam, QueryParam, Context}` |
| `security/decorators#Roles`               | `org.eclipse.dirigible.sdk.security.Roles`                                       |
| `db/decorators#Entity / Table / Column / Id` | `org.eclipse.dirigible.sdk.db.{Entity, Table, Column, Id}`                    |
| `db/decorators#Generated / GenerationType` | `org.eclipse.dirigible.sdk.db.{GeneratedValue, GenerationType}`                 |
| `db/decorators#CreatedAt / UpdatedAt / CreatedBy / UpdatedBy / Transient` | `org.eclipse.dirigible.sdk.db.{CreatedAt, UpdatedAt, CreatedBy, UpdatedBy, Transient}` |
| `Documentation` (cross-cutting)            | `org.eclipse.dirigible.sdk.platform.Documentation`                              |
| `component/decorators#Inject / Repository` | `org.eclipse.dirigible.sdk.component.{Inject, Repository}`                      |
| `job/decorators#Scheduled`                 | `org.eclipse.dirigible.sdk.job.Scheduled`                                       |
| `net/decorators#Websocket`                 | `org.eclipse.dirigible.sdk.net.Websocket`                                       |
| `extensions/decorators#Extension`          | `org.eclipse.dirigible.sdk.extensions.Extension`                                |
| Message listeners                          | `org.eclipse.dirigible.sdk.messaging.{Listener, ListenerKind}`                  |

Existing import statements that used the old `org.eclipse.dirigible.engine.java.annotations.*`
paths have been migrated across the codebase (engine-java consumers, data-store-java consumers,
IT fixtures, IDE snippets, `EntityController.java.template` and the DAO templates).

## DAO / ORM / Repository builders

The TS modules `db/dao`, `db/orm`, `db/repository`, `db/translator`, `mongodb/dao` are pure-TS
sugar over decorators. The Java equivalent is `@Repository` on a client class plus
`JavaEntityStore` (see `components/data/data-store-java/README` and the
`dirigiblelabs/sample-java-entity-decorators` sample project). The Dirigible client-Java
convention is `@Inject CountryRepository` inside a `@Controller`.

## What is intentionally not mirrored

* `http/client-async` — the TS module is callback-driven via the GraalJS bridge. Java callers
  should use a `java.util.concurrent.CompletableFuture<>`-style wrapper over the synchronous
  `HttpClient`, or `org.apache.hc.client5.http.impl.async.HttpAsyncClients` directly.
* `http/rs/resource` — the JAX-RS-like builder is replaced by `@Controller` + `@Get/@Post/...`
  routing in `engine-java`.
* `bpm/tracer`, `bpm/values` — `Tracer` is sugar over `Logger`; in Java just use
  `sdk.log.Logging` directly. `Values` is a JSON encoder for variable passing — `Converter` and
  `Bpm.set/getVariable` cover the same ground.
* `component/decorators` — see the decorators table above.
* `utils/jsonpath` — bring in `com.jayway.jsonpath:json-path` directly when needed (the dirigible
  fat jar already exposes it).

## Module wiring

Pulled into the runnable jar transitively from `components/group/group-api` (so any deployment
that uses the standard API group gets the Java SDK without extra configuration). `engine-java`
declares an explicit compile dependency so the SDK classes — both facades and annotations — are
on the classpath that `javac` sees when synchronizing client `.java` files. See
`JavaSynchronizer#finishing` and `ClassPathIndex` for how the platform classpath is wired into
the in-process compiler.

### Why `components/core/core-java` exists

The SDK module depends on `api-bpm` (so `sdk.bpm.*` calls `BpmFacade` directly), and `api-bpm`
depends on `engine-bpm-flowable`. If `engine-bpm-flowable` also depended on the full
`engine-java`, the graph would close into a cycle the moment `engine-java` started using SDK
annotations: `engine-java → api-modules-java → api-bpm → engine-bpm-flowable → engine-java`.

The fix is to extract the only pieces `engine-bpm-flowable` actually needs from
`engine-java`'s runtime package — `ClientClassLoader` and `ClientClassLoaderHolder` — into a
small leaf module, `components/core/core-java`. `engine-bpm-flowable` now depends on that
module instead of the full engine. `engine-java` also depends on `core-java`, but the two
no longer share a dependency cycle.
