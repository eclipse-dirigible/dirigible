# CLAUDE.md — components/core/core-tenants (multitenancy reference)

The authoritative in-repo description of **how multitenancy actually works**. Multitenancy is not confined to this module — it is a contract between `core-base` (the SPI), `core-tenants` (resolution, entity, provisioning, users), `data-sources` (the isolation mechanism), `core-configurations` (per-tenant config) and a handful of engines that tenant-scope their own runtime names. This file covers all of it, because the pieces are only comprehensible together.

Read this before touching tenant resolution, provisioning, the tenant-aware datasource routing, or before adding a new artefact type that should be per-tenant.

> The user-facing announcement is the March 2024 blog ["Multitenant applications with zero effort"](https://www.dirigible.io/blogs/2024/03/26/multitenancy). It is still directionally correct but has drifted — see [Where the blog is out of date](#where-the-blog-is-out-of-date). For an AWS deployment architecture built on this model, see [`AWS_MULTITENANCY_RESEARCH.md`](../../../AWS_MULTITENANCY_RESEARCH.md) at the repo root.

## The model in one paragraph

A **shared application instance** serves all tenants. The platform's own tables live in a **shared `SystemDB`** with no tenant partitioning (except a discriminator column on users). Application data is isolated by **one schema plus one dedicated DB user per tenant**, all inside the same physical database as `DefaultDB`. The on-disk repository — registry *and* IDE workspaces — is **shared, not tenant-namespaced**. Documents (CMS), Quartz job names, JMS destination names and Flowable process instances are tenant-scoped by naming/discriminator conventions. Requests are mapped to a tenant by **subdomain**, and the tenant is carried through the call in a `ThreadLocal`.

There is no discriminator column on application data, and no database-per-tenant.

## Architecture

```
HTTP request
  │  Host: acme.app.example.com
  ▼
TenantContextInitFilter                       core-tenants/tenant/
  │  ├── TenantExtractor.determineTenantSubdomain(request)
  │  │     ├── multi-tenant mode off ─────────────────► default tenant
  │  │     ├── regex no match on host/x-forwarded-host ► default tenant
  │  │     ├── subdomain found in DIRIGIBLE_TENANTS ──► that tenant  (Caffeine cached)
  │  │     └── subdomain unknown ─────────────────────► Optional.empty() ► HTTP 404
  │  └── tenantContext.execute(tenant, () -> chain.doFilter(...))
  ▼
TenantContextImpl                     ThreadLocal<Tenant> currentTenantHolder
  │
  ├─► security: CustomUserDetailsService.loadUserByUsername
  │        findUserByUsernameAndTenantId(username, currentTenant.getId())
  │
  ├─► TenantConfigurationInitFilter  (core-configurations, @Order(LOWEST_PRECEDENCE))
  │        Configuration.setThreadConfiguration(allow-listed per-tenant values)
  │
  └─► data:  DataSourcesManager.getDefaultDataSource()
                 └── TenantDataSourceNameManager.getTenantDataSourceName("DefaultDB")
                         └── "<tenantId>_DefaultDB"   (non-default tenants only)
                                 └── DataSourceInitializer → dedicated Hikari pool,
                                     HikariConfig.setSchema(<TENANT_ID>)
```

## 1. Tenant resolution

`tenant/TenantExtractor.java`:

```java
private static final List<String> HOST_HEADERS = List.of("host", "x-forwarded-host");
private static final Pattern TENANT_SUBDOMAIN_PATTERN =
        Pattern.compile(DirigibleConfig.TENANT_SUBDOMAIN_REGEX.getStringValue());
public static final Cache<String, Optional<Tenant>> TENANT_CACHE = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(100).build();

public Optional<Tenant> determineTenantSubdomain(HttpServletRequest request)
```

- Both `host` **and** `x-forwarded-host` are tried; the first that resolves wins. This is what makes the app work behind a reverse proxy / load balancer without extra configuration.
- Default regex `^([^\.]+)\..+$` — capture group 1 is the subdomain. Configurable via `DIRIGIBLE_TENANT_SUBDOMAIN_REGEX`.
- **No regex match falls back to the default tenant.** `Host: localhost` and a bare IP both resolve to default. This is deliberate (local dev, health probes) and is covered by `EnabledMultitenantModeIT` / `TenantDeterminationIT`.
- **A match with no matching tenant row is HTTP 404**, not a fallback: `"There is no registered tenant for the current host"`.
- `TenantImpl` hard-codes the default tenant: id and name `default-tenant`, subdomain `default`. `DefaultTenantInitializer` persists a row for it with status `PROVISIONED`, so `default.localhost` also resolves through the DB path.

### Cache gotchas

`TENANT_CACHE` is **static, per-JVM**, and it caches `Optional.empty()` — a 404 for an unknown subdomain therefore sticks for up to ten minutes. `TenantService` mutates it on write (`save` puts, `delete` invalidates), but only in the local JVM. `maximumSize(100)` also means the cache stops being useful past ~100 tenants.

### Filter wiring

`TenantContextInitFilter extends OncePerRequestFilter`, `@Component`, `shouldNotFilter` skips `/webjars/`, `/css/`, `/js/`, `*.ico`.

It is explicitly `addFilterBefore(...)`-ed into every security chain, because **authentication itself needs the tenant scope** — `CustomUserDetailsService` calls `getCurrentTenant()` unguarded and throws outside a scope:

- `core-tenants/security/BasicSecurityConfig` — before `UsernamePasswordAuthenticationFilter`
- `security-keycloak/KeycloakSecurityConfiguration` — before `OAuth2LoginAuthenticationFilter`
- `security-snowflake/SnowflakeSecurityConfig`

Because it is also a plain `@Component`, Spring Boot auto-registers it in the raw servlet filter chain as well. `OncePerRequestFilter` makes the second invocation a no-op, so behaviour is correct — but the effective outer-chain ordering is a side effect of that double registration rather than an explicit design. Don't rely on it; if you need ordering guarantees, make them explicit.

## 2. The tenant scope API

`core-base/tenant/TenantContext.java`:

```java
<Result, Exc extends Throwable> Result execute(Tenant tenant, CallableResultAndException<Result, Exc> callable) throws Exc;
<Result, Exc extends Throwable> Result execute(String tenantId, CallableResultAndException<Result, Exc> callable)
        throws TenantNotFoundException, Exc;
<Result, Exc extends Throwable> List<TenantResult<Result>> executeForEachTenant(
        CallableResultAndException<Result, Exc> callable) throws Exc;
boolean isNotInitialized();
Tenant getCurrentTenant();     // throws IllegalStateException outside a scope
```

- `execute` saves and restores the previous tenant, so it nests correctly (including restoring `null`).
- `getCurrentTenant()` **throws** `IllegalStateException("Attempting to get current tenant but it is not initialized yet.")` outside a scope. That is why `isNotInitialized()` guards appear all over the codebase.
- `executeForEachTenant` iterates `tenantService.findByStatus(PROVISIONED)` **plus** the default tenant, hitting the DB on every call (no caching).
- The tenant is a **plain `ThreadLocal`, not inheritable.** Any thread hand-off must re-establish it explicitly. `JobHandler`, `AsynchronousMessageListener` and `ListenerClassConsumer` do exactly that — follow their pattern for any new async entry point.
- `Tenant extends Serializable` specifically so it can be stuffed into a Quartz `JobDataMap` (`JobHandler.TENANT_PARAMETER = "tenant-id"`).
- Logging: `core-base/logging/TenantConverter.java` renders the current tenant id into every log line, or the literal `background` when no scope is active. This makes per-tenant log filtering free — don't reinvent it.

## 3. The isolation mechanism — a naming indirection

**This is the single most important thing to understand.** There is no routing DataSource, no `SET SCHEMA` per request, no discriminator column. Isolation is a *name rewrite*.

`components/data/data-sources/.../manager/TenantDataSourceNameManager.java`:

```java
public String getTenantDataSourceName(String dataSourceName) {
    if (isSystemDataSource(dataSourceName) || tenantContext.isNotInitialized()) { return dataSourceName; }
    Tenant tenant = tenantContext.getCurrentTenant();
    return createName(tenant, dataSourceName);
}
public String createName(Tenant tenant, String dataSourceName) {
    if (isTenantDataSourceName(tenant, dataSourceName)) { return dataSourceName; }
    return tenant != null && !tenant.isDefault() ? tenant.getId() + "_" + dataSourceName : dataSourceName;
}
```

`DataSourcesManager.getDataSourceDefinition(name)` runs every lookup through it, so `getDefaultDataSource()` inside a tenant scope transparently returns `<tenantId>_DefaultDB`.

Consequences, all load-bearing:

- **`SystemDB` is never prefixed** → shared by all tenants.
- **The default tenant is never prefixed** → its data lives in the connecting JDBC user's default schema.
- **Outside a tenant scope you silently get the default tenant's datasource.** Forgetting a `TenantContext.execute(...)` wrapper on a background thread does not fail — it writes to the default tenant. This is the most common way to introduce a data-leak bug in this codebase.
- **Custom datasources are asymmetric.** `CustomDataSourcesService` registers `DIRIGIBLE_DATABASE_CUSTOM_DATASOURCES` entries *unprefixed*, but `getDataSourceDefinition` *will* prefix the lookup inside a tenant scope. So a custom datasource `FOO` is looked up as `<tenantId>_FOO` for non-default tenants and is not found unless a tenant-specific row exists. The blog documents the `{TENANT_ID}_MyDB` convention as the intended workaround. Treat this as sharp-edged rather than designed.

### Connection pools: one per (JVM, tenant)

`data-sources/.../manager/DataSourceInitializer.java`:

```java
Properties hikariProperties = getHikariProperties(name);      // <NAME>_HIKARI_* env overrides
HikariConfig config = new HikariConfig(hikariProperties);
...
config.setSchema(schema);
config.setPoolName(name);
config.setMaximumPoolSize(20);
config.setMinimumIdle(10);
config.setIdleTimeout(TimeUnit.MINUTES.toMillis(3));
config.setMaxLifetime(TimeUnit.MINUTES.toMillis(Configuration.getAsInt(name + "_MAX_LIFETIME_MINUTES", 15)));
config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(15));
```

**The explicit setters run *after* the `HikariConfig(Properties)` constructor, so `setMaximumPoolSize(20)` / `setMinimumIdle(10)` override any `<NAME>_HIKARI_maximumPoolSize` you set. Pool sizing is effectively hardcoded.** Only `_MAX_LIFETIME_MINUTES` and `_LEAK_DETECTION_THRESHOLD_MINUTES` are tunable per datasource.

Each pool also gets a dedicated `JdbcTransactionManager` **registered as a runtime Spring singleton** (`transactionManager_<name>`), and the datasource itself as a bean named `<name>`. So N tenants ⇒ N pools ⇒ up to `N × 20` connections with `N × 10` held idle, plus `2N` dynamically registered singletons. Pools are created **lazily**, on the tenant's first request. This is the platform's dominant scaling limit — see the AWS research doc for the arithmetic.

Schema pinning is `HikariConfig.setSchema` → JDBC `Connection.setSchema` on each new physical connection (on PostgreSQL, pgjdbc emits `SET search_path`). No per-request `SET` is issued. `DirigibleConnectionImpl.setSchema` exists but is only used ad hoc by CSVIM and data transfer/export.

### Hibernate's multi-tenancy in `data-store` is decorative

`components/data/data-store/.../config/MultiTenantConnectionProviderImpl.java`:

```java
@Override
public Connection getConnection(String tenantIdentifier) throws SQLException {
    return this.datasourcesManager.getDefaultDataSource().getConnection();   // tenantIdentifier IGNORED
}
```

The identifier is discarded; isolation already happened inside `getDefaultDataSource()`. `CurrentTenantIdentifierResolverImpl` returns the current tenant id or the string literal `"default-tenant"` (duplicated rather than referencing `TenantImpl`). Don't "fix" this by wiring the identifier through — it would double-apply the isolation.

## 4. What is shared vs isolated

| Concern | Isolation | Where |
| --- | --- | --- |
| Application tables / views / data | **Schema + DB user per tenant** | `DefaultDataSourceProvisioning` |
| Platform tables (tenants, roles, artefacts, `Definition` checksums, Quartz, ActiveMQ store, Flowable) | **Shared, unpartitioned** `SystemDB` | `core-database/DataSourceSystemConfig` |
| Users | Discriminator column, shared DB. `DIRIGIBLE_USERS` unique on `(USER_TENANT_ID, USER_USERNAME)`; `updateKey()` appends the tenant id | `domain/User.java` |
| Role *definitions* | **Global.** `DIRIGIBLE_SECURITY_ROLES` has no tenant column; `.roles` artefacts synchronize single-tenant | `engine-security/domain/Role.java` |
| Role *assignments* | Tenant-scoped transitively through the user; `DIRIGIBLE_USER_ROLE_ASSIGNMENTS` has no tenant column of its own | `domain/UserRoleAssignment.java` |
| Registry `/registry/public` | **Shared.** One copy | `core-repository/RepositoryConfig` |
| IDE workspaces `/users/<user>/…` | Keyed by **user**, not tenant — same-named users in different tenants collide on disk | `IRepositoryStructure` |
| Per-tenant configuration | `DIRIGIBLE_CONFIGURATIONS` table **inside each tenant schema** | `core-configurations/tenant/` |
| Documents / CMS | Path-prefixed `<tenantId>/…` | `engine-cms/TenantPathResolver`, `engine-cms-internal/CmsProviderInternalFactory` (appends the tenant id to its root folder) |
| Quartz jobs | `<tenantId>###<jobName>` | `engine-jobs/tenant/JobNameCreator` |
| JMS queues / topics | `<tenantId>###<destination>` | `engine-listeners/service/DestinationNameManager` |
| BPMN | Single Flowable engine, Flowable's native `tenantId` discriminator threaded into every query/deployment/start | `engine-bpm-flowable/config/BpmProviderFlowable` |

## 5. Provisioning

### Entity and lifecycle

`domain/Tenant.java` — `@Table(name = "DIRIGIBLE_TENANTS")`, `extends Artefact`, `String id` (caller-assigned UUID), `TENANT_SUBDOMAIN` unique, `TENANT_STATUS` enumerated as a string.

```java
public enum TenantStatus { INITIAL, PROVISIONED }
```

**There is no `PROVISIONING` and no `FAILED` state.** A failure logs `"Failed to provision tenant [{}]. Continue with the next one."` and leaves the row at `INITIAL`, so the next tick retries from scratch.

### Triggers

Two, both calling the same `synchronized void provision()`:

| Trigger | File | Timing |
| --- | --- | --- |
| Startup | `provisioning/TenantsInitializer.java` | `ApplicationListener<ApplicationReadyEvent>`, `@Order(TENANTS_INITIALIZER)` = 1000, i.e. last (orders live in `core-base/ApplicationListenersOrder.java`). Schedules on a private `ScheduledThreadPoolExecutor(1)` with a hard-coded **30 s** delay |
| Periodic | `provisioning/TenantProvisioningJob.java` | Clustered Quartz `SystemJob`, `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` (**default 900**), `withMisfireHandlingInstructionNextWithExistingCount()` |

### The flow

`provisioning/TenantsProvisioner.java`:

```java
synchronized void provision() {
    Set<Tenant> tenants = tenantService.findByStatus(TenantStatus.INITIAL);
    tenants.forEach(this::provisionTenant);
    if (!tenants.isEmpty()) { postProvisioningSteps.forEach(this::callPostProvisioningStep); }
}
```

Steps are injected as **unordered** `Set<TenantProvisioningStep>` / `Set<TenantPostProvisioningStep>`. Harmless with one of each today; adding a second introduces non-deterministic ordering. If you add a step that depends on another, add explicit ordering rather than relying on set iteration.

### The only provisioning step

`components/data/data-sources/.../provisioning/DefaultDataSourceProvisioning.java` — skipped entirely for the default tenant:

1. `CREATE USER <random UUID>` with a generated 20-char password.
2. `CREATE SCHEMA <tenant.getId().toUpperCase()> AUTHORIZATION <that user>`.
3. MSSQL only: `ALTER USER … WITH DEFAULT_SCHEMA` plus `GRANT CREATE TABLE/VIEW/PROCEDURE/TYPE`.
4. Clone the `DefaultDB` definition (same driver, same JDBC URL, copied properties), override username/password/schema, persist as `<tenantId>_DefaultDB` with `location = "TENANT_DEFAULT"`, `createdBy = "TENANT_PROVISIONING_JOB"`.

**The connecting `DefaultDB` credential therefore needs `CREATE ROLE` / `CREATE SCHEMA` privileges.** Without them, provisioning fails silently into the `INITIAL` retry loop.

**No users and no roles are created.** A freshly provisioned tenant has *no admin user*; one must be created explicitly via `POST /services/security/users`. `AdminUserInitializer` creates the `DIRIGIBLE_BASIC_USERNAME` admin **only in the default tenant**.

### The only post-provisioning step

`core-initializers/.../synchronizer/tenants/RetriggerSynchronizersTenantPostProvisioningStep.java`:

```java
definitionService.updateChecksums(StringUtils.EMPTY, multitenantArtifactTypes);  // force MODIFIED
synchronizationProcessor.forceProcessSynchronizers();
```

It blanks the stored checksums of every artefact type whose synchronizer reports `multitenantExecution() == true`, then forces a full pass — which is how the new schema gets its tables, views, CSVIM data, jobs and BPMN deployments.

### Known gaps

- **Retry is not idempotent.** Every attempt creates a *new* random DB user and a `CREATE SCHEMA`. A partial failure retried later leaves orphaned users/schemas behind.
- **Two nodes booting together can double-provision.** `provision()` is only `synchronized` (intra-JVM); `findByStatus(INITIAL)` → provision → `save(PROVISIONED)` is not under a DB lock or a single transaction. The *periodic* path is safe because clustered Quartz fires it on one node; the *startup* path runs on every node.
- **There is no de-provisioning.** `TenantEndpoint` refuses to delete anything not `INITIAL` (`"Deletion of already provisioned tenants is currently not supported"`). Nothing drops the schema, the DB user, the `DIRIGIBLE_DATA_SOURCES` row or the live pool — and `DataSourceInitializer.DATASOURCES` has no external removal path, so a stale pool survives until restart.

## 6. Per-tenant synchronizer replay

`core-base/synchronizer/MultitenantBaseSynchronizer.java` is three lines:

```java
@Override public boolean multitenantExecution() { return true; }
@Override protected boolean isMultitenantArtefact(A artefact) { return true; }
```

The behaviour is in `BaseSynchronizer.completeInternal` / `cleanupInternal`:

```java
if (!multitenantExecution() || !isMultitenantArtefact(artefact)) { return completeImpl(wrapper, flow); }
List<TenantResult<Boolean>> results = tenantContext.executeForEachTenant(() -> {
    artefact.setLifecycle(lifecycle);      // reset — completeImpl mutates and persists it
    return completeImpl(wrapper, flow);
});
return results.stream().map(TenantResult::getResult).allMatch(Boolean.TRUE::equals);
```

So: **the artefact file is read once from the shared registry, but `completeImpl` — the side-effecting part (DDL, CSV import, Quartz scheduling, BPMN deployment) — is replayed once per provisioned tenant plus the default.** The single `Artefact` row in `SystemDB` is re-saved on every iteration; note the explicit `setLifecycle(lifecycle)` reset. The row is effectively "last tenant wins" and records **no per-tenant state**.

Exactly eight synchronizers are multitenant:

| Synchronizer | Module |
| --- | --- |
| `TablesSynchronizer`, `ViewsSynchronizer`, `SchemasSynchronizer` | `data-structures` |
| `CsvimSynchronizer` | `data-csvim` |
| `JobSynchronizer` | `engine-jobs` |
| `BpmnSynchronizer` | `engine-bpm-flowable` |
| `ListenerSynchronizer` | `engine-listeners` |
| `CmsSeedSynchronizer` | `engine-document` |

Everything else is **global**: datasources, access, expose, extension points/extensions, roles, markdown, proxy, web/JS, Java, Camel, OData, websockets. In particular `DataSourcesSynchronizer extends BaseSynchronizer` — a user-authored `.datasource` is registered once, globally.

The one conditional override, in `CsvimSynchronizer`:

```java
@Override protected boolean isMultitenantArtefact(Csvim csvim) {
    return !Objects.equals(systemDataSourceName, csvim.getDatasource());
}
```

**When adding a new artefact type**, decide deliberately whether it is per-tenant, and extend `MultitenantBaseSynchronizer` if so. If its side effects register a runtime name (a scheduler key, a destination, an endpoint), that name must be tenant-qualified too — follow `JobNameCreator` / `DestinationNameManager`.

## 7. Security and users

- `security/CustomUserDetailsService.java` — login is tenant-scoped:

  ```java
  Tenant tenant = tenantContext.getCurrentTenant();
  User user = userService.findUserByUsernameAndTenantId(username, tenant.getId())
      .orElseThrow(() -> new UsernameNotFoundException("Username [" + username + "] was not found in tenant [" + tenant + "]."));
  ```

  `getCurrentTenant()` is unguarded, which is why the tenant filter must run before the auth filters.
- `init/DefaultTenantInitializer.java` — `@Order(DEFAULT_TENANT_INITIALIZER)` = 20, idempotently persists the default tenant as `PROVISIONED`, `location = "-"`.
- `init/AdminUserInitializer.java` — `@ConditionalOnProperty("basic.enabled")`, `@Order(ADMIN_USER_INITIALIZER)` = 30. Default tenant only; assigns **every** `Roles` enum value; credentials from `DIRIGIBLE_BASIC_USERNAME` / `_PASSWORD` (base64, default `admin`/`admin`).
- **Cross-tenant admin surface — a real authorization gap.** `TenantEndpoint` and `UsersEndpoint` are ordinary `@RolesAllowed({"ADMINISTRATOR","OPERATOR"})` endpoints with **no tenant scoping on read**. `GET /services/security/tenants` lists every tenant; `GET /services/security/users` lists every user of every tenant; `createUser` accepts an arbitrary `tenant` id. So an `ADMINISTRATOR` in *any* tenant can enumerate and manipulate every other tenant's users. Nothing upstream restricts this. If you are running real multi-tenant workloads, do not grant `ADMINISTRATOR` to tenant users — reserve it for the platform operator.
- **Tenant DB passwords are stored in `DIRIGIBLE_DATA_SOURCES`** in `SystemDB`. Read access to that table is read access to every tenant's database credentials.

### IdP flavours

`security-cognito/CognitoTenantFilter` and `security-keycloak/KeycloakTenantFilter` are the same logic, gated on `MULTI_TENANT_MODE_ENABLED && <single user pool | single realm>`:

1. Resolve the tenant from the host (404 if unknown).
2. Read the `custom:tenant` claim — a **comma-separated list of tenant subdomains**.
3. 403 unless the list contains the resolved tenant's subdomain.

Without the single-pool/single-realm flag these filters are pass-through, and isolation is expected to come from separate pools/realms.

`CognitoSecurityConfiguration` maps `cognito:groups` straight to Dirigible role authorities (so **Cognito group names must equal Dirigible role names**), and derives roles from the `scope` claim for client-credentials tokens via `ScopeRoleJwtAuthoritiesConverter`. `SessionCreationPolicy.ALWAYS`.

`CognitoLoginController` reveals the intended per-tenant OAuth wiring — **one client registration per tenant, whose registration id is the tenant's subdomain**:

```java
@GetMapping("/{registrationId}")
public String login(@PathVariable String registrationId, HttpServletRequest request) {
    Set<String> provisionedClients = tenantService.findByStatus(TenantStatus.PROVISIONED)
                                                  .stream().map(Tenant::getSubdomain).collect(toSet());
    if (!provisionedClients.contains(registrationId)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid OAuth2 client");
    }
    return "redirect:/oauth2/authorization/" + registrationId;
}
```

Registrations live in `security-client-registration` (`DynamicClientRegistrationRepository`, DB-persisted, CRUD via `POST /services/security/client-registrations`, or seeded from `DIRIGIBLE_OAUTH_CUSTOM_CLIENTS=<name>,…` plus `<NAME>_CLIENT_ID`/`_REDIRECT_URI`/…). Each carries its own `redirectUri`, which is what lets per-tenant subdomains work despite `DIRIGIBLE_HOST` being a single value.

## 8. Per-tenant configuration (`core-configurations`)

Layering is load-bearing and documented in the root `CLAUDE.md`: `commons-config` carries only the neutral `ThreadLocal<Map<String,String>>` mechanism and the `get()` precedence branch; everything tenant/DB/policy-related lives in `core-configurations`. **Never add a tenant or DB dependency to `commons-config`.**

- `TenantConfigurationStore` — raw `SqlFactory` CRUD against `DataSourcesManager.getDefaultDataSource()`, which inside a tenant scope is already that tenant's schema. So a distinct `DIRIGIBLE_CONFIGURATIONS` table (`CONFIGURATION_KEY` / `CONFIGURATION_VALUE`) exists **per tenant schema**, created create-if-absent.
- `TenantConfigurationCache` — per-tenant map, invalidated on local write. Its Javadoc states plainly that a multi-node deployment needs an external invalidation signal and that this is out of scope.
- `TenantConfigurationKeyPolicy` — an **explicit allow-list of full keys, exact match, no wildcards**, default-deny. Currently the eight `DIRIGIBLE_BRANDING_*` keys. A non-allow-listed key can be stored but is never injected. Do not reintroduce prefix matching or a config-driven allow-list — both were explicitly rejected.
- `TenantConfigurationInitFilter`, `@Order(LOWEST_PRECEDENCE)` so it runs *inside* the tenant scope established by `TenantContextInitFilter`. **Do not move this into `core-tenants`** — that would make `core-tenants` depend on `core-configurations` and create a cycle.

Resolution precedence in `Configuration.get`: `RUNTIME (Configuration.set)` → **tenant thread map** → `ENVIRONMENT` → `DEPLOYMENT (.properties)` → `MODULE` → enum default. A tenant override beats env/properties; a programmatic `set` still wins.

The recipe for making a new key tenant-overridable is in the root `CLAUDE.md` ("Making a new property tenant-overridable"). The short version: add the exact key to `ALLOWED_KEYS`, and make sure the consuming code reads it **lazily, per request, inside the tenant scope** — a value cached in a bean or a `static` field will never reflect an override.

## 9. Configuration keys

All in `modules/commons/commons-config/.../DirigibleConfig.java` unless noted.

| Key | Default | Notes |
| --- | --- | --- |
| `DIRIGIBLE_MULTI_TENANT_MODE` | enum says `false`, but `commons-resources/dirigible-commons.properties:22` sets `true` | **Effectively `true`.** An env var beats the module properties, so `=false` in the environment works |
| `DIRIGIBLE_TENANT_SUBDOMAIN_REGEX` | `^([^\.]+)\..+$` | Capture group 1 is the subdomain |
| `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` | `900` | Onboarding latency knob |
| `DIRIGIBLE_MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL` | `false` | Enables `CognitoTenantFilter`'s `custom:tenant` check |
| `DIRIGIBLE_MULTI_TENANT_MODE_KEYCLOAK_SINGLE_REALM` | `false` | Keycloak equivalent |
| `DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT` | `DefaultDB` | The name that gets tenant-prefixed |
| `DIRIGIBLE_DATABASE_DATASOURCE_NAME_SYSTEM` | `SystemDB` | Never tenant-prefixed |
| `DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER` | `target/dirigible/cms` | Tenant id is appended per tenant |
| `DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER` | `target` | Single shared repository root — **not** tenant-aware |
| `DIRIGIBLE_BASIC_USERNAME` / `_PASSWORD` | base64 `admin` | Default-tenant admin only |

Two traps: `DIRIGIBLE_MULTI_TENANT_MODE_SINGLE_USER_POOL` appears in `Configuration.getKeys()` but **nothing reads it** — the live key is `..._COGNITO_SINGLE_USER_POOL`. And `DIRIGIBLE_MS_SHAREPOINT_TENANT_ID` is unrelated (an Azure AD tenant).

`MULTI_TENANT_MODE_ENABLED` is read **once in the constructor** of `TenantExtractor`, `CognitoTenantFilter` and `KeycloakTenantFilter` — flipping it at runtime has no effect on those beans. The ITs flip it in `@BeforeAll`, before the context boots.

## 10. Multi-node / clustering status

Cluster-ready: Quartz (`quartz.properties` → `JobStoreTX`, `isClustered=true`, `instanceId=AUTO`, on `SystemDB`), so `SynchronizationJob` and `TenantProvisioningJob` fire once cluster-wide; ActiveMQ persistence is a shared JDBC store; application data is schema-isolated in a shared DB.

Not cluster-ready — the reasons a Dirigible instance is effectively **single-writer**:

1. **Embedded ActiveMQ broker.** `engine-listeners/config/MessagingConfig` uses `vm://localhost` with `setPersistent(true)` and `JDBCPersistenceAdapter(SystemDB)`, and never disables the database locker. ActiveMQ's default locker takes an exclusive row lock on `ACTIVEMQ_LOCK` and `broker.start()` blocks until it wins, so the second instance's Spring context never finishes refreshing. The connector URL is a hardcoded constant.
2. **`SynchronizationJob` is a cluster-singleton whose effects are node-local.** It fires on one node per interval and reconciles *that JVM's* runtime — Camel routes, JMS listeners, Quartz registrations, compiled client-Java classloaders, native-app processes. Other instances never reconcile and serve stale runtime.
3. **Local filesystem repository.** `LocalRepository` is the only `IRepository`; `IRepository.DIRIGIBLE_REPOSITORY_PROVIDER_DATABASE = "database"` is a dead constant (the module was deleted). Change detection uses `WatchService`, unreliable on NFS/EFS.
4. **Per-JVM caches with no invalidation channel** — `TENANT_CACHE`, `TenantConfigurationCache`, `DataSourceInitializer.DATASOURCES`, `AccessVerifier`'s ACL cache, `Configuration.RUNTIME_VARIABLES`, `CmsProviderInternalFactory.PROVIDERS`.
5. **Boot races** — `spring.quartz.jdbc.initialize-schema=always` runs a script that opens with `DROP TABLE IF EXISTS QRTZ_*`; `JobsInitializer` does unschedule→delete→schedule and throws on collision; `SynchronizationProcessor.prepareSynchronizers()` calls `setRunningToAll(false)` on shared rows; `DefaultTenantInitializer`/`AdminUserInitializer` are check-then-insert; the 30 s startup provisioning runs on every node. Liquibase (`core-liquibase`) *is* safe — it serialises on `DATABASECHANGELOGLOCK`.
6. **In-memory HTTP sessions** — no `spring-session` on the classpath; 8 h timeout; `SessionCreationPolicy.ALWAYS` under the cognito profile.

Deploy accordingly: one instance, stop-then-start. `build/helm-charts/dirigible/values.yaml` (`replicaCount: 1`, `strategyType: Recreate`, `ReadWriteOnce`) is the upstream statement of the same conclusion — though the chart is otherwise stale (Tomcat paths, a v4-era health path) and should not be used as a deployment reference.

## Where the blog is out of date

The [2024 blog](https://www.dirigible.io/blogs/2024/03/26/multitenancy) still describes subdomain resolution, schema-per-tenant, the `{TENANT_ID}_` datasource prefix and the `{TENANT_ID}###` naming for jobs and destinations correctly. Two things have moved:

- **Onboarding.** The blog shows raw `INSERT`s into `DIRIGIBLE_TENANTS` and `DIRIGIBLE_USERS`. There are now role-guarded REST endpoints — `POST /services/security/tenants` and `POST /services/security/users` — which is what automation should call.
- **Per-tenant configuration did not exist then** (§8).

Also note the blog's provisioning description ("30 seconds post-startup, then 15-minute intervals") is exactly right and still matches `TenantsInitializer` + `TenantProvisioningJob`.

## Executable spec

These ITs are the behavioural contract — read them before changing resolution or provisioning:

| Test | Covers |
| --- | --- |
| `EnabledMultitenantModeIT` / `DisabledMultitenantModeIT` | Resolution with the mode on/off, including the `localhost`-falls-back-to-default behaviour |
| `TenantDeterminationIT` | Host / `x-forwarded-host` matching, unknown-subdomain 404 |
| `MultitenancyIT`, `MultitenancyHarmoniaIT` | End-to-end per-tenant data isolation |
| `BpmnMultitenancyIT` | Flowable discriminator-based isolation |
| `TenantConfigurationIT` | Per-tenant config set → resolve with precedence → clear |
| `RestTransactionsIT` | Tenant-scoped transaction manager behaviour |

All live under `tests/tests-integrations/src/main/java/.../tests/`.

## Wrong turns and footguns

- **Forgetting `TenantContext.execute(...)` on a background thread does not fail — it writes to the default tenant.** Always wrap async entry points; copy `JobHandler` / `AsynchronousMessageListener`.
- **Don't wire the `tenantIdentifier` through `MultiTenantConnectionProviderImpl`** — isolation already happened in `getDefaultDataSource()`; doing both double-applies it.
- **Don't add a tenant/DB dependency to `commons-config`**, and don't move `TenantConfigurationInitFilter` into `core-tenants` (module cycle).
- **Don't widen `TenantConfigurationKeyPolicy` with prefixes or a config-driven list** — both were rejected; a tenant must never be able to shadow an infrastructure key.
- **Synchronizer artefact `location`s are registry-relative; `IRepository` paths are repository-absolute.** Prepend `IRepositoryStructure.PATH_REGISTRY_PUBLIC` when reading/writing an artefact's file.
- **`SqlFactory` quoting**: `create().table()` takes **pre-quoted** identifiers, while `select/insert/update/delete` and `where(...)` **auto-quote** — pass unquoted names there. A VARCHAR length must include its parens: `column(..., "(255)")`.
- **A new provisioning step joins an unordered `Set`.** If order matters, make it explicit.
