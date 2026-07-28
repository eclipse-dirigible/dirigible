# Running Multitenant Eclipse Dirigible on AWS — Architecture Research

**Status:** research / design note. Nothing here is implemented.
**Date:** 2026-07-27
**Scope:** a production AWS architecture for a multitenant Dirigible deployment — one application instance today, scaling as tenant count grows.
**Companion:** [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) — the platform-side multitenancy reference this document is built on (how resolution, isolation, pooling and provisioning actually work). Read that first if you want the behaviour rather than the deployment.
**Sequel:** [`AWS_MULTITENANCY_TARGET_ARCHITECTURE.md`](AWS_MULTITENANCY_TARGET_ARCHITECTURE.md) — the *recommended* architecture, written without the "no code changes" constraint below. It adds cross-tenant login (one identity, many tenants, per-tenant roles) by making identity, membership and roles come from the IdP token instead of the platform's own tables. Read this document for what you can deploy today; read that one for where the model should go.

---

## 1. Purpose and scope

This document answers one question: *how do you run Eclipse Dirigible as a multitenant SaaS on AWS, in production, starting from a single instance and growing?*

It is grounded in what the platform actually does today, read out of this repository — not in what the documentation or the 2024 blog says. Where the two disagree, the code wins and the discrepancy is called out.

### Decisions taken as given

| Decision | Choice |
| --- | --- |
| Data isolation | Schema-per-tenant on a shared Aurora PostgreSQL cluster (the AWS "bridge" model) — this is what Dirigible's provisioner already does |
| Compute | ECS on Fargate |
| IDE in production | Production is **runtime-only**; authoring runs on a separate single instance |
| Tenant addressing | Wildcard subdomain `*.app.example.com` |
| Approach | **Infrastructure only.** No changes to Dirigible's code. The platform is treated as a fixed black box |
| Detail level | Architecture, component choices, rationale, scaling stages, risks. Not a runbook, not IaC |

### Explicitly out of scope

- Platform code changes. Several limitations below *could* be fixed upstream; this document works around them instead of proposing patches.
- IaC. No Terraform/CDK. The topology is described so it can be expressed in either.
- Exhaustive configuration reference. Config keys appear only where they are architectural decisions.

---

## 2. How Dirigible multitenancy actually works

> **Full reference:** [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) is the extracted, standalone description of the platform's multitenancy behaviour — resolution, the scope API, the isolation mechanism, pooling, provisioning, per-tenant synchronizer replay, security, the config-key table, the clustering status, the executable-spec ITs, and the footguns. This section is the condensed version needed to follow the AWS design; go there for detail.

One sentence: **a shared application instance, a shared platform database, a schema and a dedicated DB user per tenant for application data, and a single shared on-disk repository.**

### 2.1 Request to tenant

`TenantExtractor` matches both the `host` and `x-forwarded-host` headers against `DIRIGIBLE_TENANT_SUBDOMAIN_REGEX` (default `^([^\.]+)\..+$`, group 1 = subdomain), and `TenantContextInitFilter` wraps the request in `TenantContext.execute(tenant, ...)` — a `ThreadLocal`. Multi-tenant mode off, or no regex match (bare `localhost`, an IP), falls back to the default tenant. A match with **no matching tenant row is HTTP 404**. Lookups are cached in a static Caffeine cache, 10-minute TTL, `maximumSize(100)`, which also caches `Optional.empty()` — so a 404 for an unknown subdomain sticks for up to ten minutes.

The filter is inserted ahead of the authentication filters on every security chain, because authentication itself is tenant-scoped: `CustomUserDetailsService` looks the user up *within the current tenant*.

Two properties matter for the AWS design: **`x-forwarded-host` is honoured**, so the app works behind an ALB with no extra configuration; and the **404-on-unknown-subdomain** behaviour is what makes wildcard DNS safe — an unprovisioned subdomain is rejected rather than silently served as the default tenant.

### 2.2 The isolation mechanism is a naming indirection

`TenantDataSourceNameManager` rewrites the datasource *name* per request:

```java
public String getTenantDataSourceName(String dataSourceName) {
    if (isSystemDataSource(dataSourceName) || tenantContext.isNotInitialized()) { return dataSourceName; }
    return createName(tenantContext.getCurrentTenant(), dataSourceName);   // "<tenantId>_" + name
}
```

Every `getDefaultDataSource()` inside a tenant scope resolves to `<tenantId>_DefaultDB`. There is no routing DataSource, no per-request `SET SCHEMA`, and no discriminator column on application data. Consequences: `SystemDB` is never prefixed and is shared; the default tenant is never prefixed; and **outside a tenant scope you silently get the default tenant's datasource** — a forgotten `TenantContext.execute` wrapper writes to the wrong tenant without failing.

### 2.3 What is shared and what is isolated

| Concern | Isolation |
| --- | --- |
| Application tables/views/data | **Schema per tenant**, one dedicated DB user per tenant |
| Platform tables (tenants, roles, artefacts, Quartz, ActiveMQ store, Flowable) | **Shared** `SystemDB`, unpartitioned |
| Users | Discriminator column in the shared DB, unique on `(tenant, username)` |
| Roles | **Global** definitions; only the *assignments* are tenant-scoped, transitively via the user |
| Registry `/registry/public` and IDE workspaces | **Shared.** Workspaces are keyed by *user*, not tenant |
| Documents / CMS | **Tenant-prefixed** (`TenantPathResolver` -> `<tenantId>/...`) |
| Quartz jobs / JMS destinations | `<tenantId>###<name>` |
| BPMN | Single Flowable engine using Flowable's native `tenantId` discriminator |

### 2.4 Provisioning

`TenantStatus` has exactly two values — `INITIAL` and `PROVISIONED`. No `PROVISIONING`, no `FAILED`. Two triggers call the same `synchronized provision()`: a hard-coded 30 s delay after `ApplicationReadyEvent`, and a clustered Quartz job on `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` (**default 900 s**).

The single provisioning step (`DefaultDataSourceProvisioning`) does `CREATE USER <uuid>` with a generated password, `CREATE SCHEMA <TENANT_ID uppercased> AUTHORIZATION <that user>`, and clones the `DefaultDB` definition into a `<tenantId>_DefaultDB` row. **So the connecting credential needs `CREATE ROLE` and `CREATE SCHEMA` privileges.** A post-step then blanks the multitenant artefact checksums and forces a synchronization pass, which is how the new schema gets its tables, views and seed data.

Three gaps shape the AWS control plane (§6): **no user or role is created** (a new tenant has no admin user), **retry is not idempotent** (each attempt creates another DB user and schema, so failures leave orphans), and **there is no de-provisioning path** at all.

### 2.5 Per-tenant synchronizer replay

`BaseSynchronizer.completeInternal` replays `completeImpl` once per provisioned tenant plus the default, for the eight synchronizers that opt in (tables, views, schemas, csvim, jobs, bpmn, listeners, cms-seed). The artefact *file* is read once from the shared registry; the *side effects* — DDL, CSV import, Quartz scheduling, BPMN deployment — happen per tenant. Everything else (datasources, access, expose, extensions, roles, web, JS, Java, Camel) is global.

Two AWS-relevant implications: synchronization cost grows with tenant count, so pass duration is a signal to watch (§4.8); and a single `.datasource` artefact is global, so per-tenant custom datasources need per-tenant rows.

### 2.6 What the 2024 blog no longer reflects

The blog ["Multitenant applications with zero effort"](https://www.dirigible.io/blogs/2024/03/26/multitenancy) is still directionally right about subdomain resolution, schema-per-tenant, the `{TENANT_ID}_` datasource prefix, the `###` naming for queues and jobs, and the 30 s / 15 min provisioning cadence. Two things have moved:

- **Onboarding.** The blog shows raw `INSERT`s into `DIRIGIBLE_TENANTS` and `DIRIGIBLE_USERS`. There are now role-guarded REST endpoints (`POST /services/security/tenants`, `POST /services/security/users`), which is what automation should call.
- **Tenant-scoped configuration** did not exist then. There is now a per-tenant `DIRIGIBLE_CONFIGURATIONS` table *inside each tenant schema*, resolved per request into a thread-local config map, gated by an explicit key allow-list (currently the branding keys only).

One documentation-vs-code note: the root `CLAUDE.md` says multi-tenancy is on by default. `DirigibleConfig` declares `DIRIGIBLE_MULTI_TENANT_MODE` default `false`, but `dirigible-commons.properties` sets it to `true`, and module properties lose to environment variables — so it is effectively on, and an env var can turn it off.

---

## 3. The constraints that dictate the architecture

### 3.1 A Dirigible instance is a single-writer runtime

Four independent, verified reasons why you cannot simply raise the replica count.

**(a) The embedded ActiveMQ broker takes an exclusive database lock.**
`components/engine/engine-listeners/.../config/MessagingConfig.java` builds a `BrokerService` with `setPersistent(true)` and a `JDBCPersistenceAdapter` on `SystemDB`, on the `vm://localhost` transport, and never disables the database locker. ActiveMQ's default locker takes an exclusive row lock on `ACTIVEMQ_LOCK` and `broker.start()` **blocks until it wins**. The bean is therefore an indefinite block on the second instance — its Spring context never finishes refreshing and it never becomes ready. Classic ActiveMQ master/slave. The connector URL is a hardcoded constant, so there is no configuration path to a shared external broker.

**(b) `SynchronizationJob` is a cluster-singleton whose effects are node-local.**
Because the Quartz job store is clustered (`org.quartz.jobStore.isClustered=true`), the synchronization trigger fires on exactly *one* instance per interval (`DIRIGIBLE_SYNCHRONIZER_FREQUENCY`, default 10 s). What it does is reconcile that instance's local `/registry/public` into *that JVM's* runtime state: Camel routes, JMS listeners, OData services, Quartz job registrations, compiled client-Java classloaders, native-app processes. Any other instance never reconciles and serves stale runtime until it restarts. This is architectural, not configurable — the synchronizer model assumes the node that owns the disk owns the runtime.

**(c) The repository is a POSIX filesystem, and it is the only implementation.**
`RepositoryConfig` unconditionally constructs a `LocalRepository`. `IRepository.DIRIGIBLE_REPOSITORY_PROVIDER_DATABASE = "database"` is a dead constant — the `repository-database` module was deleted. Living on that filesystem: the registry, IDE workspaces, git working trees, GraalJS module-proxy caches, compiled client `.java` bytecode, two Lucene indexes, and the internal CMS. Change detection is `java.nio.file.WatchService`, which degrades or silently misses events on NFS/EFS; Lucene's `write.lock` makes a shared index a corruption risk.

**(d) Per-JVM caches with no invalidation channel.**
`TenantConfigurationCache` is an unbounded `ConcurrentHashMap` with no TTL, invalidated only by a local write — its own Javadoc states that multi-node deployments need an external invalidation signal and that this is out of scope. `TenantExtractor.TENANT_CACHE` gives ten minutes of staleness. `AccessVerifier`'s security-ACL cache refreshes only when the *local* file watcher flags a change, so on an instance that never sees local writes it effectively never refreshes. `DataSourceInitializer.DATASOURCES` and `Configuration.RUNTIME_VARIABLES` are static maps mutated locally.

### 3.2 Even one instance cannot be rolled in place

Several boot-time actions are not safe if a new instance starts while the old one is still running:

- `spring.quartz.jdbc.initialize-schema=always` runs `org/quartz/impl/jdbcjobstore/tables_postgres.sql`, whose first eleven statements are `DROP TABLE IF EXISTS QRTZ_*`. A booting instance recreates the scheduler tables from scratch.
- `JobsInitializer` does unschedule → delete → schedule for every system job, and throws `IllegalStateException` (failing the boot) on a collision.
- Flowable runs `DB_SCHEMA_UPDATE_TRUE` by default; its `ACT_GE_PROPERTY` versioning is not first-boot-concurrent-safe.
- The ActiveMQ JDBC adapter creates its own tables on first start.
- `DefaultTenantInitializer` and `AdminUserInitializer` are check-then-insert.
- `SynchronizationProcessor.prepareSynchronizers()` calls `setRunningToAll(false)` across shared artefact rows, clobbering another instance's in-flight pass.

Liquibase (`components/core/core-liquibase`) *is* cluster-safe — it serialises on `DATABASECHANGELOGLOCK`. It is everything layered on top that is not.

**Conclusion: deployments must be stop-then-start** (ECS `minimumHealthyPercent=0`, `maximumPercent=100`). The shipped Helm chart reaching the same conclusion independently — `replicaCount: 1`, `strategyType: Recreate`, a `ReadWriteOnce` PVC — is the upstream statement of intent.

### 3.3 The capacity limit is database connections

`components/data/data-sources/.../manager/DataSourceInitializer.java`:

```java
Properties hikariProperties = getHikariProperties(name);   // <NAME>_HIKARI_* env overrides
HikariConfig config = new HikariConfig(hikariProperties);
...
config.setSchema(schema);
config.setMaximumPoolSize(20);
config.setMinimumIdle(10);
```

The explicit setters run *after* the properties constructor, so they override any `<NAME>_HIKARI_maximumPoolSize` you set. **Pool sizing is effectively hardcoded.** One pool per `(instance, tenant)`, created lazily on that tenant's first request, each with a dedicated `JdbcTransactionManager` registered as a runtime Spring singleton.

```
idle connections per instance  ≈  10 × (tenants that have made a request)
peak connections per instance  ≈  20 × (concurrently busy tenants)
```

Aurora PostgreSQL's default is `max_connections = LEAST({DBInstanceClassMemory/9531392}, 5000)` — roughly 1,800 on a 16 GiB instance, hitting the 5,000 ceiling around 64 GiB. So ~100 active tenants on one instance is ~1,000 idle connections held open, and a few hundred tenants saturates any class.

This is the number that sizes a cell, and connection count is the leading indicator to alarm on.

### 3.4 Why RDS Proxy does not help here

It looks like the obvious fix for the connection problem. It is not, for two independent reasons:

1. **Pinning.** Schema selection happens via `HikariConfig.setSchema` → JDBC `Connection.setSchema` → pgjdbc emits `SET search_path`. [AWS documents](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/rds-proxy-pinning.html) that for PostgreSQL, "using `SET` commands" pins the client connection to a database connection for the life of the session, and unlike MySQL there is no session-pinning filter to opt out. Multiplexing — the entire point of the proxy — would be off for every tenant connection.
2. **Dynamic credentials.** Each tenant gets a freshly generated DB user at provisioning time. RDS Proxy authenticates against Secrets Manager secrets registered on the proxy, so every tenant onboarding would have to create a secret and modify the proxy.

Size Aurora for the real connection count instead. (A conventional PgBouncer sidecar in transaction mode has the same `SET search_path` problem; session mode adds little over Hikari.)

### 3.5 In-System Programming is a security boundary, not a feature toggle

Dirigible's premise is that authenticated developers modify the running system through the browser. Concretely, a user with the DEVELOPER role can execute arbitrary JavaScript and arbitrary Java **in the application's own JVM**, and — via `.nativeapp` artefacts — spawn arbitrary OS processes in the container.

On AWS that means: **the ECS task role is effectively delegated to anyone who can author code on that instance.** So does the task's network position, its instance metadata, and its database credentials.

This is the single strongest argument for the runtime/authoring split, and it makes the split a security control rather than an operational convenience:

- The **runtime** service grants no DEVELOPER role, carries a minimal task role (S3 documents prefix, KMS key, Logs, nothing else), and has content baked in rather than authored live.
- The **authoring** service is not internet-facing in the same way, is IP-restricted, and its task role is scoped to the dev environment's resources only.

### 3.6 Surfaces that are on by default and must be closed

Discovered while inventorying ports. All of these are shipped defaults:

| Surface | Default | Why it matters |
| --- | --- | --- |
| ttyd terminal, port **9000** | `DIRIGIBLE_TERMINAL_ENABLED=true` | `ttyd -p 9000 --writable sh` — a writable root shell with **no authentication**. Started from a static initializer. Port is not configurable |
| Graalium inspector, port **8081** | `DIRIGIBLE_GRAALIUM_ENABLE_DEBUG=true` shipped in `dirigible-commons.properties`; `inspect.Secure=false`, `inspect.Suspend=true` | A Chrome DevTools inspector; `Suspend=true` means JS execution blocks waiting for a debugger. Exposed by the Dockerfile |
| SFTP server, port **8022** | `engine-sftp` is on the classpath; `DIRIGIBLE_SFTP_USERNAME`/`_PASSWORD` default to `admin`/`admin` | Live SSH/SFTP into the CMS tree. Host key is regenerated per container, so clients see key mismatches |
| Actuator | `management.endpoints.web.exposure.include=*` | Exposes `heapdump`, `threaddump`, `env`, `loggers`. Non-health endpoints are OPERATOR-gated, but the gate is the only thing standing between the internet and a heap dump |
| Basic auth | `DIRIGIBLE_BASIC_USERNAME`/`_PASSWORD` = base64(`admin`)/base64(`admin`) | Default credentials |
| Spring Boot Admin | `admin`/`admin` when the profile is enabled | Default credentials |
| Logback | three non-rotating `FileAppender`s under `/logs` | Grows without bound; will fill the volume |
| CSRF / frame options | both disabled in `BasicSecurityConfig` | Accepted platform design; compensate at the edge |

The Docker image also runs as **root**, sets **no JVM heap flags** (so the JVM takes the default 25 % of task memory), declares `VOLUME /tmp`, and has no `WORKDIR` — meaning the working directory is `/`, and the repository lands at `/target/`, logs at `/logs/`, plus `./ttyd.sh` and `./host.ser` written into `/`.

---

## 4. Target architecture — one cell

```
                              Route 53
                     *.app.example.com  ── A/ALIAS ──┐
                                                     │
                             ┌───────────────────────▼───────────────────────┐
                             │  AWS WAF  →  ALB (HTTPS, ACM *.app.example.com)│
                             │  stickiness on · idle timeout ≥ longest async  │
                             │  health check → /services/core/healthcheck     │
                             └───────┬──────────────────────────┬─────────────┘
                                     │ host: *.app.example.com  │ host: ide.example.com
                                     │                          │ (+ WAF IP allow-list)
  ══════════════ private subnets ════╪══════════════════════════╪═════════════════════
                                     ▼                          ▼
                        ┌──────────────────────┐   ┌──────────────────────────┐
                        │ ECS service RUNTIME  │   │ ECS service AUTHORING    │
                        │ Fargate ARM64, N = 1 │   │ Fargate, N = 1           │
                        │ stop-then-start      │   │ IDE · ttyd · LSP · git   │
                        │ IDE off, publish off │   │ EFS/EBS repository       │
                        │ content baked in     │   │ minimal dev task role    │
                        │ + ADOT sidecar       │   │                          │
                        └───┬────────┬─────────┘   └────────┬─────────────────┘
                            │        │                      │
  ══════ isolated subnets ══╪════════╪══════════════════════╪══════════════════════
                            ▼        │                      ▼
        ┌────────────────────────┐   │        ┌──────────────────────────┐
        │ Aurora PostgreSQL      │   │        │ Aurora (dev)             │
        │ Serverless v2, Multi-AZ│   │        └──────────────────────────┘
        │ SystemDB   (platform)  │   │
        │ DefaultDB  (default    │   │        VPC endpoints (no NAT):
        │   tenant + one schema  │   │          S3 (gateway) · ECR api/dkr
        │   and DB user per      │   │          Logs · Secrets Manager · KMS
        │   tenant)              │   │
        └────────────────────────┘   │
                                     ▼
                      ┌──────────────────────────────┐        ┌──────────────────┐
                      │ S3 — documents / CMS         │        │ Amazon Cognito   │
                      │ cms-provider-s3              │        │ ONE user pool    │
                      │ key prefix = <tenantId>/…    │        │ custom:tenant    │
                      │ SSE-KMS · versioned          │        │ cognito:groups   │
                      └──────────────────────────────┘        │ 1 app client per │
                                                              │ tenant subdomain │
                                                              └──────────────────┘
```

### 4.1 Network

Three AZs. Public subnets carry only the ALB. Fargate tasks sit in private subnets; Aurora in isolated subnets with no route out. Use VPC endpoints — S3 gateway, ECR api + dkr, CloudWatch Logs, Secrets Manager, KMS — rather than a NAT gateway; the task's only genuine internet need is outbound HTTPS to Cognito's JWKS endpoint and whatever user code calls, so scope a single NAT (or an egress proxy) deliberately rather than by default. Egress restriction matters more here than in a normal app, because user code runs in-process (§3.5).

### 4.2 Edge, DNS and TLS

One wildcard ACM certificate for `*.app.example.com` plus the apex, one Route 53 wildcard alias record to the ALB. **Onboarding a tenant requires no DNS change** — this is the main reason to prefer subdomain addressing.

AWS WAF on the ALB: the managed common rule set, a rate-based rule per IP, and a rule blocking `/actuator/**` and `/spring-admin/**` from anything but the admin CIDRs (defence in depth behind the OPERATOR role gate).

ALB details that matter for this application:
- **Stickiness on.** Two independent reasons: HTTP sessions are in Tomcat's heap with no `spring-session` on the classpath (8 h timeout), and the four platform WebSocket handlers (`terminal`, `java-lsp`, `java-debug`, `data/transfer`) bind to instance-local OS processes.
- **Idle timeout** must exceed the longest legitimate request; the app ships `spring.mvc.async.request-timeout=3600000` (1 h) and allows 1 GB multipart uploads.
- **Health check target must be `/services/core/healthcheck`, not `/actuator/health/readiness`.** Spring's readiness probe flips UP as soon as the context refreshes, but `ApplicationReadyEvent` then kicks off classpath expansion and a full synchronization pass that can take minutes — during which `HealthCheckFilter` 302-redirects every request to `/index-busy.html`. Pointing the ALB at the actuator probe means routing live traffic to an instance that redirects everything.

**CloudFront is deliberately deferred.** It would be valuable later for caching the UI's static assets (`/webjars/**`, `/services/web/**`), but tenant resolution reads the `Host` header, and CloudFront rewrites `Host` to the origin domain by default and does not send `X-Forwarded-Host`. Adding it requires an origin request policy that forwards the viewer `Host` (or a CloudFront Function copying it into `X-Forwarded-Host`) — a known-good pattern, but one more thing to get right, and worth adding only when static-asset egress actually costs something.

### 4.3 Compute

Two ECS services in one cluster, both Fargate on ARM64 (the CI pipeline already publishes multi-arch images, so Graviton is free money).

**`dirigible-runtime`** — desired count **1**. Deployment configuration `minimumHealthyPercent=0`, `maximumPercent=100`, i.e. stop-then-start, for the reasons in §3.2. This accepts a short outage per deploy; §8 discusses that trade-off.

**`dirigible-authoring`** — desired count **1**, its own hostname, WAF IP allow-list, its own Aurora and repository volume. This is where the IDE, ttyd, JDT.LS and git live.

Task configuration worth calling out:

- **JVM memory.** The image sets no heap flags, so the JVM caps itself at 25 % of task memory — on an 8 GiB task that is ~2 GiB of heap. Raise it via `JAVA_TOOL_OPTIONS` (`-XX:MaxRAMPercentage`, `-XX:+ExitOnOutOfMemoryError`, `-XX:+HeapDumpOnOutOfMemoryError`). But do not push it to 80 %: this container has substantial *out-of-JVM* residents — a `tsc --watch` node process, one JDT.LS child at `-Xmx2g` **per user/workspace**, and any `.nativeapp` processes. On a runtime task those should all be off (`DIRIGIBLE_JAVA_LSP_ENABLED=false`, `DIRIGIBLE_TERMINAL_ENABLED=false`), which is what makes the runtime task far leaner than the authoring task.
- **CPU.** GraalJS and GraalPython run as Truffle interpreters on stock Corretto 21 — the image is *not* GraalVM, and does not need to be, but JS/TS execution is interpreted and therefore CPU-hungry. Size on CPU, not just memory, and measure with real tenant workloads.
- **Ephemeral storage.** Raise it above the 20 GiB Fargate default. `engine-java`'s `ClassPathIndex` extracts `BOOT-INF/lib/*.jar` to a temp directory at startup, multipart uploads land in `java.io.tmpdir` at up to 1 GiB each, and the ActiveMQ temp store and Graal caches are on disk.
- **Startup grace.** A generous ECS health-check `startPeriod` and ALB `deregistration_delay`; boot includes classpath expansion plus a synchronization pass over the whole registry.
- **Shutdown.** `server.shutdown=graceful` is not set anywhere, and Quartz is configured with `waitForJobsToCompleteOnShutdown(false)`. A SIGTERM drops in-flight requests and abandons running jobs (Quartz recovers them via `requestRecovery`). Set the Spring properties as task environment overrides and give the container a stop timeout that exceeds the ALB deregistration delay.
- **Logging.** Redirect the three non-rotating file appenders; log to stdout and let awslogs/FireLens carry it to CloudWatch. The console appender is already wired.
- Non-root user and a read-only root filesystem are *not* achievable without relocating a set of hardcoded relative paths (`./target/dirigible/kahadb`, `./ttyd.sh`, `./host.ser`, the SFTP root). Treat "runs as root with a writable root filesystem" as a known property and compensate with a minimal task role and network isolation.

### 4.4 Data

One Aurora PostgreSQL cluster per cell — Serverless v2 is the right default (the load is bursty and tenant-count-driven), Multi-AZ with a reader.

Two logical databases in the cluster, matching Dirigible's two datasources:

- **`SystemDB`** — the platform's own schema: tenants, users, roles, artefact registry, Quartz, the ActiveMQ message store, Flowable. Its own credentials. Managed by Liquibase (`core-liquibase`, changelog `db/changelog/dirigible-system.json`), which is cluster-safe. Set `DIRIGIBLE_DATABASE_SYSTEM_DDL_AUTO=validate` so Hibernate never emits DDL behind Liquibase's back, and the Postgres Quartz delegate (`DIRIGIBLE_SCHEDULER_DATABASE_DELEGATE=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate`).
- **`DefaultDB`** — application data. The default tenant lives in the connecting user's default schema; every other tenant gets its own schema and its own DB user, created by the provisioner. **The credential Dirigible connects with therefore needs `CREATE ROLE` and `CREATE SCHEMA` privileges** — on RDS/Aurora the master user (an `rds_superuser` member) has these; a least-privilege application role does not, and provisioning will fail silently into the `INITIAL` retry loop if you get this wrong.

Keeping them as separate databases with separate credentials means a tenant's DB user cannot see the platform's tables. Note that tenant DB passwords are generated by Dirigible and stored in `DIRIGIBLE_DATA_SOURCES` in `SystemDB` — encryption at rest covers the storage, but anything with read access to that table holds every tenant's credentials.

Capacity: size from §3.3, alarm on `DatabaseConnections` against the class's `max_connections`, and treat that ratio as the trigger for a new cell. Skip RDS Proxy (§3.4).

Backup: Aurora automated backups plus PITR covers the cluster. **Per-tenant restore is the operational cost of the bridge model** — restoring one tenant means cloning the cluster to a point in time, `pg_dump -n <TENANT_SCHEMA>` out of the clone, and restoring into the live database. Write and rehearse that runbook before you need it; it is the thing customers ask about.

### 4.5 Storage

**Documents go to S3.** Set `DIRIGIBLE_CMS_PROVIDER=cms-provider-s3` with `DIRIGIBLE_S3_BUCKET`; `TenantPathResolver` already prefixes every key with the tenant id, so isolation comes for free. Authenticate with the task role, not `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` — `S3Facade` falls back to the default credentials chain when those are unset. Bucket policy scoped per environment, SSE-KMS, versioning on, lifecycle rules for old versions.

**The repository is where the runtime/authoring split earns its keep.**

- *Authoring instance*: a persistent volume (EFS with an access point, or EBS if you accept single-AZ). It holds IDE workspaces, git working trees and the Lucene indexes, none of which survive task replacement otherwise. Point `DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER` at an explicit absolute path rather than relying on the relative default resolving against `/`.
- *Runtime instances*: **no persistent volume.** Content arrives immutably at boot, by one of two mechanisms that already exist:
  - `ClasspathExpander` walks every `META-INF/dirigible/**` on the classpath at `ApplicationReadyEvent` and writes it into `/registry/public`. Bake the published projects into a jar layer and the runtime task is content-immutable by construction. A jar containing `META-INF/dirigible/.skip` is skipped.
  - `DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER` replicates an external folder into the registry and keeps watching it — useful if you prefer to sync content from S3 into an ephemeral local directory at task start.
  Then set `DIRIGIBLE_PUBLISH_DISABLED=true` so nothing can write to the registry at runtime.

Two traps worth knowing: `DIRIGIBLE_MASTER_REPOSITORY_PROVIDER`/`_ZIP_LOCATION`/`_JAR_PATH` look like exactly the right mechanism for immutable seeding but are **dead code** — nothing in `components/` ever creates an `IMasterRepository`. And EFS is a poor fit for the registry even where it works: `WatchService` change detection degrades on NFS, Lucene explicitly should not live on it, and the registry is thousands of small files.

### 4.6 Identity

Dirigible already implements the wildcard-subdomain SaaS pattern against Cognito. No code change is needed.

Activate the profile (`spring_profiles_active=cognito`, which also sets `basic.enabled=false`) with `DIRIGIBLE_MULTI_TENANT_MODE=true` and `DIRIGIBLE_MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL=true`.

**One Cognito user pool for all tenants.** Two claims carry the tenancy:

- `custom:tenant` — a comma-separated list of tenant **subdomains** the user belongs to. `CognitoTenantFilter` resolves the tenant from the request host and returns 403 unless the list contains that subdomain. A user with no value gets 403.
- `cognito:groups` — mapped straight to Dirigible role authorities by `CognitoSecurityConfiguration.userAuthoritiesMapper()`. **Cognito group names must equal Dirigible role names** (`ADMINISTRATOR`, `OPERATOR`, and the application's own `.roles`). Note that on a *runtime* cell you simply do not create a `DEVELOPER` group (§3.5).

**One OAuth client registration per tenant, whose registration id is the tenant's subdomain.** This is the piece that makes per-tenant callbacks work despite `DIRIGIBLE_HOST` being a single value. `CognitoLoginController` exposes `GET /login/{registrationId}`, validates the id against the set of `PROVISIONED` tenant subdomains, and redirects to `/oauth2/authorization/{registrationId}`:

```java
if (!provisionedClients.contains(registrationId)) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid OAuth2 client");
}
return "redirect:/oauth2/authorization/" + registrationId;
```

Registrations are held by `DynamicClientRegistrationRepository` — DB-persisted, resolvable at runtime, and each carries its own `redirectUri`. They can be created two ways: through `ClientRegistrationEndpoint` (REST, the automation path), or seeded from the environment via `DIRIGIBLE_OAUTH_CUSTOM_CLIENTS=<name1>,<name2>` plus `<NAME>_CLIENT_ID`, `<NAME>_REDIRECT_URI`, `<NAME>_TOKEN_URI`, … per name.

So a tenant's login entry point is `https://acme.app.example.com/login/acme`, with `redirectUri` `https://acme.app.example.com/login/oauth2/code/acme`. Onboarding adds one Cognito app client (or reuses one) and one client registration — no DNS, no infrastructure change.

Machine-to-machine access works through the same pool: client-credentials tokens are validated as resource-server JWTs against the pool's JWKS, and `ScopeRoleJwtAuthoritiesConverter` derives roles from the `scope` claim.

### 4.7 Messaging

The embedded ActiveMQ broker opens **no network port** — it is `vm://` in-JVM only. Within a cell that is fine: a single instance produces and consumes its own queues, persistence is in `SystemDB` so messages survive a restart, and destinations are tenant-prefixed. There is nothing to provision on AWS.

What you are accepting: `.listener` artefacts are an in-process async mechanism, not a cross-instance bus. Cross-cell messaging does not exist and cannot be configured into existence. If a workload genuinely needs a shared broker, that workload should talk to SQS/SNS/Amazon MQ directly from user code (the `api-*` modules make that straightforward) rather than through `.listener`.

### 4.8 Observability

`engine-open-telemetry` is already in the tree and Camel OTel is wired in `DirigibleApplication`. Run an **ADOT collector sidecar** in the task and point `otel.exporter.otlp.endpoint` at it; fan out to CloudWatch/AMP and X-Ray.

The one genuinely nice property: **per-tenant log correlation is free.** `components/core/core-base/.../logging/TenantConverter.java` already injects the current tenant id into every log line (and the literal `background` for system threads). Structure the log output as JSON and every CloudWatch Logs Insights query, metric filter and per-tenant dashboard becomes a one-liner — this is usually the hardest part of SaaS observability and it is already done.

Metrics: scrape `/actuator/prometheus` into AMP. Watch, per cell: `DatabaseConnections` against the class limit, active tenant count, JVM heap and GC, synchronization pass duration, tenant-provisioning lag (`INITIAL` rows older than one job interval), and ALB 5xx/target response time.

---

## 5. Scaling out: cells

### 5.1 Why cells rather than replicas

§3.1 establishes that the unit of Dirigible that can be replicated safely is *the whole instance with its own database and its own filesystem*. That is precisely AWS's definition of a **cell** in cell-based architecture. The platform's constraint and the AWS pattern land on the same boundary, which is why this is the recommended shape rather than a workaround.

A cell is:

```
cell-01 ─┬─ ALB listener rule / dedicated ALB
         ├─ ECS service "runtime", desired count 1
         ├─ Aurora PostgreSQL cluster (SystemDB + DefaultDB with K tenant schemas)
         ├─ S3 prefix (or bucket)
         └─ its own CloudWatch dashboard + alarms
```

Global, shared by all cells: the Cognito user pool, the ECR image, the IaC pipeline, the observability account/workspace, and the tenant→cell registry.

### 5.2 Routing tenants to cells

Three options, in order of preference:

1. **Route 53 per-tenant CNAME to the cell's ALB.** `acme.app.example.com` → `cell-03-alb...elb.amazonaws.com`. No rule limits, no shared listener to grow, per-tenant moves are a DNS change (with a TTL-bounded cutover). The cost is that onboarding now *does* touch DNS — one API call, easily automated. **This is the recommendation.**
2. **One shared ALB with host-header listener rules** per cell. Simplest mental model and keeps wildcard DNS, but ALB listener rules are limited (on the order of 100 per listener, with a handful of host conditions each), so this caps total tenants unless you group cells behind wildcard-ish patterns.
3. **CloudFront with a Function selecting the origin** from the `Host` header. Most flexible and adds edge caching, but it puts the tenant→cell map in a Function (and its own deployment cycle) and reintroduces the `Host`-forwarding concern from §4.2.

### 5.3 Sizing a cell

Three ceilings, take the lowest:

- **Connections** — from §3.3, `10 × activeTenants` idle. This is usually the binding constraint.
- **Memory/CPU** — measured, not derived. Per-tenant cost depends entirely on the applications those tenants run.
- **Blast radius** — a business decision. A cell failure takes down every tenant in it, and a deploy stops the instance briefly. Smaller cells cost more per tenant and fail less broadly.

Open a new cell when any of these crosses its threshold. Keep the number a published, monitored figure rather than an emergent one; "how many tenants fit in a cell" should be a known constant, revisited when the workload mix changes.

### 5.4 Operating multiple cells

- **Deploy in waves.** A canary cell (internal tenants only) takes every release first, then a small wave, then the rest. Because each cell stops briefly during a deploy, schedule waves against each cell's own quiet hours where tenant geography allows.
- **Blast radius is the point.** Cell-level alarms, cell-level dashboards, and an explicit decision about whether a cell failure degrades or hard-fails its tenants.
- **Cell rebalancing** is a tenant migration: `pg_dump -n <SCHEMA>` from the source cell, restore into the target, copy the S3 prefix, re-point DNS, delete the source schema. Automate it before you need it, because tenants grow unevenly and the first cell will fill with the wrong mix.
- **Cell provisioning should be one IaC module instantiated N times**, with a cell index as the only meaningful parameter. If standing up cell #4 is bespoke work, the model has failed.

---

## 6. Tenant lifecycle

### 6.1 Onboarding

```
signup
  │
  ├─ 1. control plane: allocate a cell, reserve the subdomain,
  │       write tenant → cell into the registry (DynamoDB)
  │
  ├─ 2. Cognito: create/attach the user, set custom:tenant = <subdomain>,
  │       add role groups, ensure an app client with callback
  │       https://<subdomain>.app.example.com/login/oauth2/code/<subdomain>
  │
  ├─ 3. Route 53: CNAME <subdomain>.app.example.com → cell ALB
  │
  ├─ 4. cell: POST /services/security/tenants   { name, subdomain }  → row INITIAL
  │
  ├─ 5. cell: POST /services/security/client-registrations
  │            { name/id = <subdomain>, clientId, clientSecret,
  │              redirectUri = https://<subdomain>…/login/oauth2/code/<subdomain>, … }
  │
  ├─ 6. cell: POST /services/security/users     { username, tenant } → tenant admin
  │
  ├─ 7. wait for the provisioning tick:
  │       CREATE USER + CREATE SCHEMA + datasource row + status PROVISIONED
  │       + forced synchronization pass creates tables/views/seed data
  │
  └─ 8. poll until PROVISIONED, then mark the tenant live
```

Steps 4–6 are role-guarded REST calls under `services/security/` (`ADMINISTRATOR`/`OPERATOR`). Note step 7's latency: the provisioning job defaults to **900 s**. Lower `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` to ~60 s for a responsive signup flow, and remember the job is a *clustered Quartz* job so a single cell instance runs it exactly once per tick.

Implement the control plane as a small service or a Step Functions state machine with Lambda tasks, backed by a DynamoDB table that is the authority on tenant → cell, subdomain reservation, and onboarding state. Dirigible's `DIRIGIBLE_TENANTS` table remains the authority *within* a cell; the two must be reconciled by the control plane, not assumed consistent.

### 6.2 Failure handling

Provisioning has no failure state — a failed attempt leaves the row at `INITIAL` and the next tick retries from scratch, creating another DB user and schema. So the control plane must:

- Alarm on `INITIAL` rows older than a small multiple of the job interval, rather than waiting for the tenant to complain.
- Treat repeated retries as an incident, and include orphaned-user/orphaned-schema cleanup in the remediation.

### 6.3 Offboarding

**The platform has no de-provisioning path.** The tenant API refuses to delete a `PROVISIONED` tenant, and nothing drops the schema, the DB user, the datasource row or the live connection pool. Offboarding is therefore a scripted runbook the control plane owns:

1. Suspend access — remove the tenant's subdomain from every user's `custom:tenant`, delete the Cognito app client.
2. Export — `pg_dump -n <TENANT_SCHEMA>` and copy the S3 prefix to the retention location; hold for the contractual period.
3. Remove Route 53 record.
4. In the cell's `SystemDB`: delete the tenant's users and role assignments, delete the `<tenantId>_DefaultDB` row from `DIRIGIBLE_DATA_SOURCES`, delete the client registration, delete the tenant row.
5. In `DefaultDB`: `DROP SCHEMA <TENANT_SCHEMA> CASCADE`, `DROP USER <tenant user>`.
6. Delete the S3 prefix.
7. Restart the cell's runtime task so the stale connection pool and the cached tenant lookup are discarded — the pool is a static map with no removal path from outside, and `TENANT_CACHE` holds the subdomain for up to ten minutes regardless.

Rehearse it. Step 4 before step 5 matters (the datasource row is what keeps the pool alive), and step 7 is easy to forget.

---

## 7. Scaling stages

| Stage | Tenants | Shape | What changes |
| --- | --- | --- | --- |
| **1** | 0 – ~50 | One cell. Runtime + authoring split from day one. Aurora Serverless v2 with a low floor | Get the hardening (§3.6), the health-check target, the JVM flags and the stop-then-start deploy right. Establish the connection-count dashboard |
| **2** | ~50 – 150 | Same one cell, vertically scaled | Move documents to S3. Make runtime content immutable (`ClasspathExpander` + `DIRIGIBLE_PUBLISH_DISABLED`). Tune the provisioning interval. Measure the real per-tenant connection and memory cost and *publish the cell capacity number* |
| **3** | 150+ | Multiple cells, Route 53 per-tenant CNAME, DynamoDB tenant→cell registry | Cell IaC module. Onboarding automation gains cell allocation. Deploy waves with a canary cell. Tenant-migration tooling |
| **4** | many cells | Real control plane | Cell health/capacity as a first-class service, automatic cell provisioning, rebalancing on a schedule, per-cell cost attribution |

Leading indicators, per cell:

- `DatabaseConnections` ÷ `max_connections` — **the primary signal**; open a new cell well before saturation.
- Active tenants in the cell vs. the published capacity number.
- JVM heap after GC, and GC pause time (user code shares the heap).
- Synchronization pass duration — it grows with registry size and tenant count, since side effects replay per tenant.
- Tenants stuck in `INITIAL`.
- ALB target response time p99 and 5xx rate.

---

## 8. Risks and accepted limitations

These are consequences of the "no code changes" constraint. They are deliberate, and each should be an explicit conversation with whoever owns the SLA.

| Limitation | Consequence | Mitigation available today |
| --- | --- | --- |
| **One runtime task per cell** | No intra-cell HA. A task failure or an AZ event takes the cell down until ECS replaces the task (~1–3 min) | Fast task replacement, cell-level alarms, tenants spread across cells so no single failure is total. Communicate the SLA honestly |
| **Stop-then-start deploys** | A short planned outage per cell per release | Deploy waves in each cell's quiet window; keep releases small and frequent so the window is short and routine |
| **Cross-tenant admin visibility** | `TenantEndpoint` and `UsersEndpoint` do not tenant-scope reads. An `ADMINISTRATOR` in *any* tenant can list and modify *every* tenant's users | Do not grant `ADMINISTRATOR` to tenant users. Keep it to the platform operator; give tenant admins an application-level role instead. This is the most important item in this table |
| **Tenant DB credentials in the platform DB** | `DIRIGIBLE_DATA_SOURCES` holds every tenant's generated DB password | Encryption at rest; tightly restrict who can read `SystemDB`; treat a `SystemDB` compromise as a full-cell compromise |
| **Tenant cache: 10 min TTL, `maximumSize(100)`** | Tenant create/disable takes up to 10 min to take effect; above 100 tenants per cell the cache thrashes and every miss hits the DB | Keep cells at or below ~100 tenants (which the connection budget suggests anyway); accept the onboarding delay or restart after onboarding |
| **No de-provisioning** | Offboarding is manual (§6.3) | Scripted, rehearsed runbook owned by the control plane |
| **Per-tenant restore is a clone + `pg_dump`** | Restoring one tenant is slow and operationally involved | Documented, rehearsed runbook; set customer expectations accordingly |
| **Lucene indexes are instance-local** | Search results are whatever that instance indexed; the authoring and runtime instances diverge | Acceptable with one instance per cell. Do not put the index on EFS |
| **GraalJS/Python run interpreted** | JS/TS-heavy tenants are CPU-expensive | Size on CPU; watch per-tenant CPU; consider it in cell capacity |
| **Runs as root, writable root filesystem** | Weaker container hardening than a normal Java service | Minimal task role, private subnets, tight egress, no DEVELOPER role in production |
| **In-System Programming = code execution** | Anyone who can author code on an instance effectively holds its task role | Runtime/authoring split as a security boundary (§3.5). Non-negotiable |

---

## 9. Cost model

Order-of-magnitude only, one region, on-demand, before any discounting. Verify against the current price list.

**One cell, steady state:**

| Component | Assumption | Rough monthly |
| --- | --- | --- |
| Fargate runtime task | 2 vCPU / 8 GiB, ARM64, 24×7 | $60 – 80 |
| Fargate authoring task | 2 vCPU / 8 GiB, business hours only | $25 – 80 |
| ALB | 1 ALB + modest LCU | $20 – 35 |
| Aurora Serverless v2 | floor 1 ACU, average 2–4 ACU, + reader | $90 – 300 |
| S3 + KMS | documents, modest volume | $5 – 25 |
| EFS (authoring only) | small, elastic throughput | $10 – 30 |
| CloudWatch Logs + metrics | with sane retention | $20 – 60 |
| Cognito | scales with monthly active users | verify current pricing |
| **Total** | | **~$250 – 600** |

**Marginal cell:** roughly the runtime task + ALB (or listener rule) + Aurora + S3 — call it $200–450, since the authoring instance, the Cognito pool and the pipeline are shared.

Main levers, in order of impact:

1. **Aurora floor.** The Serverless v2 minimum ACU dominates a lightly loaded cell. Tune it per cell rather than globally.
2. **Graviton** for both tasks — the image is already multi-arch.
3. **Avoid NAT.** VPC endpoints instead; a NAT gateway per AZ is $32+/month each before data processing.
4. **Log retention and volume.** With per-tenant tenant ids in every line, log volume grows with tenants; set retention deliberately and consider a cheaper tier for older data.
5. **Authoring task schedule.** Scale it to zero outside working hours.
6. Compute Savings Plans once the baseline is stable.

---

## 10. Verification checklist

Confirm each of these in your own account and against your own build before committing to the design.

**AWS-side**
- [ ] Aurora PostgreSQL `max_connections` for the instance class you pick — the documented default is `LEAST({DBInstanceClassMemory/9531392}, 5000)`; measure the actual value.
- [ ] Cognito limits and current pricing: app clients per user pool, callback URLs per app client, custom attributes, and the monthly-active-user price for your tier.
- [ ] ALB listener-rule and target limits, if you choose the shared-ALB routing option.
- [ ] EFS throughput and latency for the registry's file-count and size profile (thousands of small files) on the authoring instance.
- [ ] CloudFront `Host`-header forwarding behaviour with the origin request policy you choose, if and when you add CloudFront.

**Dirigible-side**
- [ ] Whether `spring.quartz.jdbc.initialize-schema=always` really recreates the `QRTZ_*` tables on your engine and version. The referenced script (`org/quartz/impl/jdbcjobstore/tables_postgres.sql` in quartz 2.5.2) opens with `DROP TABLE IF EXISTS` for all eleven tables, but confirm the initializer runs it on every boot in your build.
- [ ] The `default-tenant` Cognito registration: `application-cognito.properties` sets `redirect-uri=${DIRIGIBLE_HOST}/login/oauth2/code/cognito` while `client-name=default-tenant` becomes the registration id. Confirm the callback path and the registration id line up, or you will get a redirect-URI mismatch on the default tenant specifically.
- [ ] That the connecting `DefaultDB` credential really can `CREATE ROLE` and `CREATE SCHEMA` on Aurora — provision one throwaway tenant end to end before anything real depends on it.
- [ ] Actual per-tenant connection and memory cost, measured with a representative application, to fix the cell capacity number.
- [ ] That `DIRIGIBLE_TERMINAL_ENABLED=false`, `DIRIGIBLE_GRAALIUM_ENABLE_DEBUG=false`, SFTP disabled or credentialled, and restricted actuator exposure are all actually in effect on the running task — verify by probing ports 9000, 8081 and 8022 from inside the VPC, not by reading the task definition.
- [ ] Behaviour of a tenant whose subdomain is requested before provisioning completes (expect a cached 404 for up to ten minutes) — decide what the signup UX does about it.

---

## 11. References

### In-repo

| Concern | Path |
| --- | --- |
| **Multitenancy behaviour reference — start here** | [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) |
| Tenant SPI (`Tenant`, `TenantContext`, provisioning steps) | `components/core/core-base/src/main/java/org/eclipse/dirigible/components/base/tenant/` |
| Host → tenant resolution + cache | `components/core/core-tenants/.../tenant/TenantExtractor.java`, `TenantContextInitFilter.java` |
| Tenant entity, status, CRUD API | `components/core/core-tenants/.../domain/Tenant.java`, `domain/TenantStatus.java`, `endpoint/TenantEndpoint.java`, `endpoint/UsersEndpoint.java` |
| Provisioning | `components/core/core-tenants/.../provisioning/`, `components/data/data-sources/.../provisioning/DefaultDataSourceProvisioning.java` |
| **The isolation switch** | `components/data/data-sources/.../manager/TenantDataSourceNameManager.java` |
| Per-tenant pools (hardcoded sizing, `setSchema`) | `components/data/data-sources/.../manager/DataSourceInitializer.java` |
| Shared platform datasource + JPA | `components/core/core-database/.../DataSourceSystemConfig.java` |
| Platform schema (Liquibase) | `components/core/core-liquibase/` |
| Per-tenant synchronizer replay | `components/core/core-base/.../synchronizer/BaseSynchronizer.java`, `MultitenantBaseSynchronizer.java` |
| Synchronization loop | `components/core/core-initializers/.../synchronizer/` |
| Single local repository | `components/core/core-repository/.../RepositoryConfig.java`, `modules/repository/repository-local/` |
| Immutable content seeding | `components/core/core-initializers/.../classpath/ClasspathExpander.java`, `components/core/core-registry/.../watcher/ExternalRegistryWatcher.java` |
| Embedded broker (`vm://`, JDBC store) | `components/engine/engine-listeners/.../config/MessagingConfig.java` |
| Quartz configuration | `modules/commons/commons-resources/src/main/resources/quartz.properties`, `components/engine/engine-jobs/.../config/QuartzConfig.java` |
| Cognito profile, tenant filter, login controller | `components/security/security-cognito/` |
| Per-tenant OAuth client registrations | `components/security/security-client-registration/` |
| CMS tenant path prefixing, S3 provider | `components/engine/engine-cms/.../TenantPathResolver.java`, `components/engine/engine-cms-s3/`, `components/api/api-s3/` |
| Tenant id in every log line | `components/core/core-base/.../logging/TenantConverter.java` |
| Tenant-scoped configuration | `components/core/core-configurations/.../tenant/` |
| Config keys and defaults (authoritative) | `modules/commons/commons-config/.../DirigibleConfig.java`, `Configuration.java` |
| Shipped properties | `modules/commons/commons-resources/src/main/resources/application-common.properties`, `dirigible-commons.properties` |
| Container image | `build/application/Dockerfile` |

`build/helm-charts/` is **stale** and should not be used as a deployment reference — it targets a Tomcat-era layout (`/usr/local/tomcat/...`, `tomcat-users.xml`), probes a v4-era health path (`/services/v4/healthcheck`, now `/services/core/healthcheck`), and sets keys that no longer do anything (`DIRIGIBLE_DATABASE_PROVIDER=custom`, `DIRIGIBLE_SCHEDULER_MEMORY_STORE`). Its `replicaCount: 1` + `strategyType: Recreate` + `ReadWriteOnce` PVC remain accurate as a statement of the single-writer model.

### External

- ["Multitenant applications with zero effort"](https://www.dirigible.io/blogs/2024/03/26/multitenancy) — the original feature announcement (March 2024); see §2.6 for what has since changed.
- [Avoiding pinning an RDS Proxy](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/rds-proxy-pinning.html) — the PostgreSQL `SET` pinning behaviour behind §3.4.
- [Amazon RDS Proxy for Aurora](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/rds-proxy.html)
- [AWS Prescriptive Guidance: multi-tenant SaaS on managed PostgreSQL](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/) — the silo / bridge / pool taxonomy this document's "bridge" choice comes from.
- [Amazon Aurora PostgreSQL parameters](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/AuroraPostgreSQL.Reference.ParameterGroups.html)
