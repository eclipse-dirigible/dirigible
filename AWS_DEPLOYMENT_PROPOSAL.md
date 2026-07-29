# Multitenant Dirigible on AWS — Deployment Proposal

**Status:** architecture proposal. Nothing here is deployed or implemented.
**Date:** 2026-07-28
**Scope:** one production deployment ("unit") of a multitenant Dirigible application on ECS, sized for ~50–100 tenants, with subdomain-per-tenant addressing and cross-tenant login (one identity, different roles per tenant).
**Independence note:** this document was designed independently from the earlier research on branch `docs/aws-multitenancy-research`, from the code and AWS constraints alone; §10 records where the two efforts agree and differ.

**Contents**

- [0. Decisions at a glance](#0-decisions-at-a-glance)
- [1. Requirements](#1-requirements)
- [2. What the platform gives you and what it imposes](#2-what-the-platform-gives-you-and-what-it-imposes)
- [3. Topology — one production unit](#3-topology--one-production-unit)
- [4. Identity — one pool, per-tenant app clients, roles as prefixed groups](#4-identity--one-pool-per-tenant-app-clients-roles-as-prefixed-groups)
  - [4.1 Why Cognito (and not Keycloak on ECS)](#41-why-cognito-and-not-keycloak-on-ecs)
  - [4.2 One app client for all tenants, or one per tenant? — the decision](#42-one-app-client-for-all-tenants-or-one-per-tenant--the-decision)
  - [4.3 The topology](#43-the-topology)
  - [4.4 Membership and roles: prefixed Cognito groups](#44-membership-and-roles-prefixed-cognito-groups)
  - [4.5 The pre-token-generation Lambda (V2_0) — the per-tenant lens](#45-the-pre-token-generation-lambda-v2_0--the-per-tenant-lens)
  - [4.6 Flows](#46-flows)
- [5. Isolation summary](#5-isolation-summary)
- [6. Fork changes (minimal set)](#6-fork-changes-minimal-set)
- [7. Tenant onboarding (automation runbook)](#7-tenant-onboarding-automation-runbook)
- [8. Security hardening checklist](#8-security-hardening-checklist)
- [9. Operations](#9-operations)
- [10. Where this agrees / differs with `docs/aws-multitenancy-research`](#10-where-this-agrees--differs-with-docsaws-multitenancy-research)
- [11. How it grows](#11-how-it-grows)
- [12. Hypotheses to verify before go-live](#12-hypotheses-to-verify-before-go-live)

---

## 0. Decisions at a glance

| Concern | Decision | Why (short) |
| --- | --- | --- |
| Tenant addressing | Wildcard subdomain `*.app.com` → one ALB | `TenantExtractor` already resolves tenants from `host`/`x-forwarded-host`; onboarding needs no DNS change |
| Compute | ECS **Fargate**, one service, **desiredCount = 1**, stop-then-start deploys | The runtime is single-writer by construction (§2.2) — this is a property of the platform, not a choice |
| Database | **RDS PostgreSQL, Multi-AZ** (e.g. `db.r6g.large`); SystemDB + DefaultDB with one schema + DB user per tenant | Matches what Dirigible's provisioner does; Multi-AZ compensates for single-task compute |
| Documents/CMS | **S3** via `engine-cms-s3`, tenant-prefixed keys | Removes the biggest stateful-filesystem need; isolation ships in `TenantPathResolver` |
| Registry/content | **Baked into the image** (`META-INF/dirigible/**`), `DIRIGIBLE_PUBLISH_DISABLED=true` | Production is runtime-only and content-immutable; no EFS |
| Identity | **Amazon Cognito**: ONE user pool | One pool = one identity per person = same credentials everywhere |
| OAuth clients | **ONE confidential app client per tenant** — decided on merit in §4.2, single-client alternative analysed there | Tenant-scoped tokens at issuance, deny-at-IdP, no 100-callback ceiling, per-tenant federation |
| Client resolution in the app | **Fork-built host-keyed `ClientRegistrationRepository`** (secrets from Secrets Manager) | Specified fresh; the shipped `security-client-registration` module is deliberately **not** assumed (§2.5) |
| Membership & roles | **Cognito groups named `t:<subdomain>:<role>`**, projected per token by a pre-token-generation Lambda | Single source of truth in the IdP, no extra membership database at this scale |
| Local user tables | **Not used in production** (`basic.enabled=false` under the cognito profile) | The requirement said they need not stay; under OIDC profiles they are already bypassed |
| Fork changes | Six small, targeted changes (§6) — no redesign of the multitenancy model | The shipped cognito profile is 80 % of the design already |

---

## 1. Requirements

1. One Dirigible application, multiple tenants, isolated from each other.
2. Tenant access by subdomain: `tenant1.app.com`, `tenant2.app.com`, …
3. Compute on ECS.
4. **A user logs into several tenants with the same credentials and holds different roles in each.**
5. User/role assignments need not live in the local `DIRIGIBLE_USERS` / `DIRIGIBLE_USER_ROLE_ASSIGNMENTS` tables — tokens may carry them.
6. Changing the multitenancy implementation is allowed (this is a fork).
7. Production quality: hardened, observable, operable.

---

## 2. What the platform gives you and what it imposes

Everything below is read out of this repository, with file references, because these facts dictate the architecture.

### 2.1 Multitenancy that already works

- **Subdomain → tenant.** `TenantExtractor` (`components/core/core-tenants/.../tenant/TenantExtractor.java`) matches `host` and `x-forwarded-host` against `DIRIGIBLE_TENANT_SUBDOMAIN_REGEX` (default `^([^\.]+)\..+$`); `TenantContextInitFilter` wraps every request in a `ThreadLocal` tenant scope. An unknown subdomain is a hard **404** — wildcard DNS is safe because unprovisioned hosts are rejected, not silently served. The `x-forwarded-host` support means it works behind an ALB unmodified. Lookups sit in a static Caffeine cache: 10-minute TTL, `maximumSize(100)` (`TenantExtractor.java:44`) — note that 100 is exactly our target tenant count (§6, fix 6).
- **Data isolation = schema per tenant.** Inside a tenant scope every `getDefaultDataSource()` is name-rewritten to `<tenantId>_DefaultDB` (`components/data/data-sources/.../manager/TenantDataSourceNameManager.java`). Provisioning (`DefaultDataSourceProvisioning.java`) executes `CREATE USER <uuid>` + `CREATE SCHEMA <TENANT> AUTHORIZATION <user>` and clones the datasource row — so **the connecting DB credential must hold `CREATEROLE` and `CREATE SCHEMA`**. The platform's own tables (tenants, users, roles, Quartz, ActiveMQ store, Flowable) live in a shared, never-prefixed **SystemDB**.
- **Per-tenant runtime replay.** The registry (`/registry/public`) is shared; eight multitenant synchronizers (tables, views, schemas, csvim, jobs, listeners, bpmn, cms-seed) replay their side effects once per provisioned tenant, and a post-provisioning step retriggers them for new tenants (`RetriggerSynchronizersTenantPostProvisioningStep`). Quartz jobs and JMS destinations are `<tenantId>###<name>`-prefixed; Flowable uses its native `tenantId`; CMS paths get a `<tenantId>/` prefix (`engine-cms/.../TenantPathResolver.java`).
- **Provisioning cadence.** Tenants are created `INITIAL` and flipped to `PROVISIONED` by a job that runs 30 s after boot and then every `DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS` (**default 900 s** — lower it, §7). There is no `FAILED` state and no de-provisioning: a failing step is retried whole (leaving orphan DB users/schemas), and `DELETE` on a `PROVISIONED` tenant returns 400.
- **Tenant-tagged logs for free.** `TenantConverter` (`core-base/.../logging/TenantConverter.java`) stamps the tenant id into every log line — per-tenant observability costs nothing (§9).

### 2.2 The single-writer constraint (dictates desiredCount = 1)

You cannot run two Dirigible tasks against one database/filesystem. Four independent, code-verified reasons:

1. **Embedded ActiveMQ.** `engine-listeners/.../config/MessagingConfig.java` builds the broker on the hardcoded `vm://localhost` transport with `setPersistent(true)` and a `JDBCPersistenceAdapter` on SystemDB. ActiveMQ's JDBC locker takes an exclusive lock and `broker.start()` blocks until it wins — a second instance never finishes booting.
2. **Node-local synchronization.** The Quartz `SynchronizationJob` is a cluster singleton (clustered job store), but what it does — reconciling the registry into Camel routes, listeners, scheduled jobs, compiled classes — is state **inside one JVM**. A second instance would simply never reconcile.
3. **Filesystem repository.** `RepositoryConfig` unconditionally constructs a `LocalRepository`; the registry, workspaces and Lucene indexes are local files with `WatchService`-based change detection (which degrades on NFS/EFS).
4. **Boot-time DDL.** Quartz `initialize-schema=always` drops and recreates the `QRTZ_*` tables at startup; several initializers are check-then-insert. A rolling deploy where old and new overlap corrupts shared state.

**Consequences, stated honestly:** ECS `desiredCount=1`, `minimumHealthyPercent=0`, `maximumPercent=100` (stop-then-start). Every deploy and every task crash is an outage of boot time + the first synchronization pass — minutes, not seconds. Fargate restarts the task in another AZ on failure and RDS Multi-AZ fails over independently, but the realistic availability target is **~99.5 %**, not 99.99 %. If that is unacceptable, the answer is a second unit (§11), not a replica.

### 2.3 The capacity ceiling is database connections

`DataSourceInitializer.java:155-159` calls `config.setSchema(schema)`, `config.setMaximumPoolSize(20)`, `config.setMinimumIdle(10)` **after** the properties-based constructor — so the `*_HIKARI_*` env overrides are silently discarded and every tenant that has served one request holds **10 idle connections forever**. 100 active tenants ≈ 1,000+ idle connections, which no reasonable RDS class tolerates. This is the single most important fork fix (§6, fix 1). RDS Proxy is **not** an alternative: `setSchema` makes pgjdbc emit `SET search_path`, which pins proxy sessions and disables multiplexing — the proxy's entire value.

### 2.4 In-System Programming is a security boundary

A user holding the `DEVELOPER` role can execute arbitrary JavaScript/Java in the JVM and spawn OS processes. On ECS that means "whoever can author code holds the task role." Production must therefore be **runtime-only**: no DEVELOPER role is grantable, publishing is disabled, and content arrives baked into the image. Authoring (the IDE) runs on a separate, internal, IP-restricted instance with its own small database. This split is a security control, not an operational preference.

### 2.5 What the cognito profile contributes — and what this proposal deliberately does not build on

Shipped in `components/security/security-cognito/` and used by this design:

- `CognitoSecurityConfiguration.userAuthoritiesMapper()` maps the **`cognito:groups`** claim to `ROLE_*` authorities.
- `CognitoLoginController` exposes `GET /login/{registrationId}`, validating the id against **provisioned tenant subdomains** and redirecting to `/oauth2/authorization/{registrationId}` — a useful per-tenant entry point, independent of how registrations are stored.
- `ScopeRoleJwtAuthoritiesConverter` maps M2M `scope` claims (segment after the last `/`) to roles.
- `application-cognito.properties` sets `basic.enabled=false` — the local user tables are already bypassed under this profile; no `DIRIGIBLE_USERS` row is consulted or created.

**Deliberately not assumed:** the `security-client-registration` module (`DynamicClientRegistrationRepository`, its REST CRUD and env seeding). The single-vs-per-tenant OAuth client decision in §4.2 is made on its own merits, and the component that resolves per-tenant registrations — if the per-tenant answer wins — is specified fresh in §6 (fix 7). Whether its implementation reworks or replaces the existing module is an implementation detail, not a design input.

What the profile does **not** yet do correctly is tenant-scope the authorization — the gaps are precisely the fork changes in §6.

---

## 3. Topology — one production unit

```
                  Route 53   *.app.com  ──alias──┐
                                                  │
             ┌────────────────────────────────────▼─────────────────────────────┐
             │  AWS WAF  →  ALB  (HTTPS, ACM *.app.com)                          │
             │  health check: /services/core/healthcheck                         │
             └──────────────┬───────────────────────────────┬────────────────────┘
                            │ *.app.com                     │ authoring.internal (internal ALB,
     ═══ private subnets ═══╪═════════════════════════════  │  office CIDR / VPN only)
                            ▼                               ▼
             ┌───────────────────────────┐      ┌───────────────────────────┐
             │ ECS Fargate: dirigible-   │      │ ECS Fargate: dirigible-   │
             │ runtime  (desired = 1,    │      │ authoring (IDE, DEVELOPER,│
             │ stop-then-start deploys)  │      │ EFS workspace, own DB)    │
             │ content baked into image  │      └───────────────────────────┘
             │ publish disabled          │
             └────┬──────────┬───────────┘
     ═══ isolated ▼ subnets ═╪══════════════════════════════════════════════════
             ┌───────────────────────────┐      ┌───────────────────────────┐
             │ RDS PostgreSQL, Multi-AZ  │      │ S3: documents/CMS         │
             │  SystemDB (platform)      │      │  keys <tenantId>/…        │
             │  DefaultDB:               │      │  SSE-KMS, versioning      │
             │   default schema          │      └───────────────────────────┘
             │   TENANT1, TENANT2, …     │
             │   (schema + DB user each) │      Secrets Manager · CloudWatch
             └───────────────────────────┘      VPC endpoints (S3/ECR/Logs/…)

             ┌───────────────────────────────────────────────────────────────┐
             │ Amazon Cognito — ONE user pool                                │
             │   app clients: tenant1, tenant2, …  (callback per subdomain)  │
             │   groups: t:tenant1:manager, t:tenant2:viewer, …              │
             │   resource servers per tenant (M2M scopes)                    │
             │   pre-token-generation Lambda ──reads──▶ DynamoDB             │
             │     clientId → tenant                    (clientId→tenant map)│
             └───────────────────────────────────────────────────────────────┘
```

### Network, DNS, TLS, edge

- One VPC, two AZs. Public subnets: ALB only. Private: Fargate tasks. Isolated (no route out): RDS.
- **Route 53 wildcard alias** `*.app.com` → ALB, one **ACM wildcard certificate**. Tenant onboarding touches no DNS.
- **AWS WAF** on the ALB: managed common rules, per-IP rate limiting, and a block on `/actuator/**` and `/spring-admin/**` from non-admin CIDRs.
- **No CloudFront.** Tenant resolution reads the `Host` header; CloudFront rewrites it to the origin by default. Add it later only for static-asset offload, with an origin request policy forwarding the viewer `Host`.
- ALB specifics: stickiness on (heap-bound HTTP sessions, WebSockets), idle timeout above the app's long-request ceiling, health check target **`/services/core/healthcheck`** — the actuator readiness probe flips UP before the first synchronization pass, while `HealthCheckFilter` still 302-redirects everything to `/index-busy.html`. Pointing the ALB at actuator readiness routes live traffic to a busy instance.

### Compute

`dirigible-runtime` (Fargate, ARM64 — the CI images are multi-arch):

| Setting | Value | Why |
| --- | --- | --- |
| desiredCount / deploy | `1`, `minimumHealthyPercent=0`, `maximumPercent=100` | §2.2 |
| Size | 2 vCPU / 8 GiB to start; measure | GraalJS is interpreted → CPU-hungry |
| JVM | `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError` | Image ships no heap flags (defaults to 25 % of task memory) |
| Ports | 8080 only, security group ALB→task | ttyd 9000 / debug 8081 / SFTP 8022 unmapped **and** disabled by env (§8) |
| Health grace | ALB `healthCheckGracePeriodSeconds` ≈ 600 | Boot includes classpath expansion + a full sync pass |
| Shutdown | `server.shutdown=graceful` via env; stopTimeout > deregistration delay | Not set anywhere in the shipped config |
| Logs | stdout → awslogs (JSON) | The three file appenders don't rotate — never rely on them |
| Ephemeral storage | ≥ 30 GiB | fat-jar lib extraction, multipart tmp, Graal caches |

`dirigible-authoring` (optional but recommended): desired 1, internal ALB, WAF/SG IP allow-list, EFS for workspaces, its own small RDS. This is the only place the IDE, ttyd and the DEVELOPER role exist.

### Database

**RDS PostgreSQL, Multi-AZ**, `db.r6g.large` class to start. Two logical databases matching Dirigible's two datasources:

- `SystemDB` — platform schema (Liquibase-managed, cluster-safe). Own credentials.
- `DefaultDB` — application data; default tenant in the connecting user's default schema, every other tenant in its own schema owned by its own DB user, created by the provisioner. **Verify before go-live that the connecting credential can `CREATE ROLE` / `CREATE SCHEMA` on RDS** — on RDS the master user can; a least-privilege role cannot, and provisioning fails silently into the retry loop.

Plain provisioned RDS rather than Aurora Serverless v2: the load profile of one unit with a bounded tenant count is steady, and a fixed Multi-AZ instance is simpler and cheaper than a serverless floor. Revisit if tenant load proves spiky.

With the pool fix (§6, fix 1: max 5 / minIdle 0 per tenant datasource), 100 tenants peak below ~500 connections with near-zero idle — comfortable for the class (~1,700 `max_connections`). **Without the fix this architecture does not hold at 100 tenants.** Alarm on `DatabaseConnections` ÷ `max_connections` (§9).

### Storage

- **Documents/CMS → S3**: `DIRIGIBLE_CMS_PROVIDER=cms-provider-s3`, `DIRIGIBLE_S3_BUCKET`, task-role auth (leave the static key env vars unset so the default credentials chain is used). Tenant isolation comes from `TenantPathResolver`'s key prefix. SSE-KMS, versioning, lifecycle rules.
- **Registry → the image.** Bake published projects as a jar layer under `META-INF/dirigible/**`; `ClasspathExpander` writes them into the registry at boot. Set `DIRIGIBLE_PUBLISH_DISABLED=true`. The runtime task then needs **no persistent volume at all**: task replacement rebuilds identical state. Do not put the registry on EFS (WatchService + Lucene + thousands of small files).

---

## 4. Identity — one pool, per-tenant app clients, roles as prefixed groups

### 4.1 Why Cognito (and not Keycloak on ECS)

Keycloak's realm–client–role model fits this problem naturally, but it is a second stateful, always-on service to run, patch, back up and upgrade — on an architecture already constrained to careful single-writer operations, adding another one is real cost. Cognito is managed, priced per MAU, and its two relevant primitives (pool-wide SSO session, pre-token-generation trigger) cover the requirement. That the fork ships a cognito Spring profile (the groups→authorities mapper, the per-tenant login entry point) lowers integration effort further, but the decision stands on managed-vs-operated alone. Cognito on the **Essentials** tier (required for pre-token-generation trigger V2_0).

### 4.2 One app client for all tenants, or one per tenant? — the decision

Both are workable at this scale; this is the one identity decision that deserves a real trade-off analysis rather than inheritance from either the platform's shipped machinery or AWS blog patterns.

**Option A — one shared app client.**
The app side is genuinely simple: **one** Spring client registration whose redirect uses the standard `{baseUrl}` template (`{baseUrl}/login/oauth2/code/cognito` — Spring resolves it against the request host; the shipped config pins `${DIRIGIBLE_HOST}` instead, a one-line change), one client secret, no host→registration resolution at all. Onboarding a tenant = add `https://<sub>.app.com/login/oauth2/code/cognito` to the client's callback list + create the tenant's groups. But:

- **Hard ceiling at ~100 tenants:** Cognito allows **100 callback URLs per app client, no wildcards** — the ceiling lands exactly on this unit's design capacity, and the only escape is per-tenant clients, i.e. a migration at the design point.
- **The token cannot be tenant-scoped.** The pre-token Lambda's only reliable tenant signal in managed-login flows is `callerContext.clientId` (`clientMetadata` is not passed on hosted-UI/`InitiateAuth` flows). With one client, every token must carry **all** the user's `t:*:*` groups, and the application filters by the host-resolved tenant at login. Denial of non-members happens only app-side; the IdP happily issues tokens for tenants the user doesn't belong to.
- **A stolen bearer token is valid on every tenant the victim belongs to** — the membership check passes wherever they are a member. There is no audience binding to assert.
- **Per-tenant federation is not expressible** — which external IdPs a client offers is per-app-client configuration.
- One leaked client secret affects all tenants.

**Option B — one confidential app client per tenant** (name = subdomain, callback `https://<sub>.app.com/login/oauth2/code/<sub>`, sign-out `https://<sub>.app.com`).

- **The token is tenant-scoped at issuance:** the Lambda keys on `clientId`, emits only that tenant's roles into `cognito:groups`, and stamps `dirigible:tenant` — least privilege in the token itself, an audience binding the app can assert (§6, fix 2), and a stolen token that is useless on every other tenant.
- **Deny at the IdP:** a non-member never receives a token at all.
- No callback ceiling (one URL per client; 1,000 clients/pool default, raisable to 10,000); per-tenant enterprise federation later is a per-client setting; a leaked secret is one tenant's problem.
- **The honest cost:** N clients and N secrets to create and rotate (onboarding automation), a clientId→tenant map for the Lambda, and — since this proposal does not assume the shipped registration module — a fork-built **host-keyed `ClientRegistrationRepository`** that resolves the request's subdomain to that tenant's client id + secret (§6, fix 7). That is a real component, not free.

**Recommendation: Option B, one app client per tenant.** The shared client's 100-callback limit coincides with the unit's target capacity, and the tokens it produces are strictly weaker — multi-tenant bearer tokens with app-side-only denial. Option B buys issuance-time least privilege, IdP-side denial and federation headroom for the price of one well-understood component plus onboarding automation that has to exist anyway. Choose Option A only for a small, fixed tenant fleet (≲ 20) where operational minimalism outweighs headroom — and note that the groups model, claims contract and login flows are identical in both options, so a later A→B migration is contained (create clients, extend the Lambda, add the resolver), but it *is* a migration.

### 4.3 The topology

- **One user pool.** This is what satisfies requirement 4: one identity (email sign-in) per person, one password, one MFA enrolment. Cognito's managed-login session cookie is shared across all app clients in a pool (~1 h), so signing into a second tenant is **silent** — no credential prompt.
- **One confidential app client per tenant** (§4.2). Each tenant's client id + secret live in Secrets Manager and are resolved per request host by the fork's registration resolver (§6, fix 7) — that is what makes `https://<sub>.app.com/login/<sub>` reach the right client.
- **One Cognito resource server per tenant** (identifier = subdomain, custom scopes = role names) for machine-to-machine access, plus an M2M app client per tenant that needs it.

### 4.4 Membership and roles: prefixed Cognito groups

Roles live in Cognito as groups named **`t:<subdomain>:<roleName>`** — e.g. `t:tenant1:ADMINISTRATOR`, `t:tenant2:employee-manager`. Membership in a tenant *is* holding at least one of its groups. No separate membership database: the IdP is the single source of truth, administered with plain Cognito APIs/console.

Arithmetic at target scale: 100 tenants × ~5 roles = ~500 groups (pool limit 10,000). The **non-adjustable 100-groups-per-user** limit caps one person at ~100 tenant-role pairs — a non-issue for a person in a handful of tenants; flagged as a growth watchpoint (§11).

The `custom:tenant` user attribute that the shipped `CognitoTenantFilter` reads is **retired** — a user-global comma-separated attribute is a second source of truth that drifts, and it carries no per-tenant roles.

### 4.5 The pre-token-generation Lambda (V2_0) — the per-tenant lens

Configured on the pool; runs on every token issuance (including refresh):

1. Map `event.callerContext.clientId` → tenant subdomain. The map is a two-column DynamoDB table written at onboarding (clientId → subdomain), cached in the Lambda.
2. Take the user's groups, keep those with prefix `t:<subdomain>:`, strip the prefix.
3. **Not a member (no matching group)?** Throw — Cognito fails the sign-in and never issues a token. Deny at the IdP; Dirigible never sees the session.
4. **Member:** emit `groupsToOverride = [roles-here]` (replaces `cognito:groups` in **both** ID and access tokens) and add the claim **`dirigible:tenant = <subdomain>`** to both tokens.

The resulting claims contract:

| Claim | Tokens | Meaning |
| --- | --- | --- |
| `sub` | both | Global identity of the person (immutable; the Lambda cannot alter it) |
| `cognito:groups` | both | The caller's roles **in this token's tenant only** — must equal Dirigible role names (`.roles` artefacts + the built-ins) exactly |
| `dirigible:tenant` | both | The tenant this token was minted for; Dirigible asserts it equals the host-resolved tenant (§6, fix 2) |
| `scope` (M2M) | access | `<subdomain>/<role>`; the shipped converter takes the segment after the last `/` |

Because the token is tenant-scoped at issuance, the shipped `cognito:groups` → authorities mapper is **correct per tenant with zero changes**. And because `JSESSIONID` is host-scoped, each subdomain gets its own Spring session holding its own authority snapshot — the "authorities are snapshotted at login" behaviour becomes per-tenant automatically.

### 4.6 Flows

**Login:** `https://tenant1.app.com/` → unauthenticated → `/login/tenant1` (validated against provisioned subdomains) → `/oauth2/authorization/tenant1` → Cognito managed login (client `tenant1`) → Lambda: member, `cognito:groups=[manager]`, `dirigible:tenant=tenant1` → callback → session for `tenant1.app.com` with `ROLE_manager`.

**Cross-tenant hop (requirement 4):** same browser opens `https://tenant2.app.com/` → no session for this host → `/login/tenant2` → Cognito: pool-wide session cookie still valid ⇒ **no credential prompt** → Lambda mints through client `tenant2`: `cognito:groups=[viewer]` → independent session for `tenant2.app.com` with `ROLE_viewer`. Same credentials, different roles, one interactive sign-in.

**Logout:** clear the local session, redirect to Cognito `/logout` with the **current tenant's** client id and `logout_uri=https://<sub>.app.com` (fork fix 5 — the shipped handler uses the single default client and host). Honest caveat: Cognito logout ends the pool-wide managed-login session (all tenants' SSO), while live Dirigible sessions on other subdomains survive until they expire — keep the session timeout ≤ 30 min.

**M2M:** client-credentials against the tenant's M2M client, scopes from the tenant's resource server (`tenant1/data-reader` → `ROLE_data-reader` via the shipped converter). The hardened membership filter (fix 2) asserts the token's tenant against the host, closing today's bypass where Bearer requests skip the check entirely.

**Role-change propagation is refresh-bound:** the Lambda also fires on token refresh, so a group change lands within one access-token lifetime (set 60 min) plus the Dirigible session. An immediate revoke = remove groups + `AdminUserGlobalSignOut` + invalidate the Dirigible session (an explicit "eject" runbook, not an assumption).

---

## 5. Isolation summary

| Layer | Mechanism | Status |
| --- | --- | --- |
| HTTP request → tenant | Host/`x-forwarded-host` subdomain, 404 on unknown | Ships today |
| Application data | Schema + dedicated DB user per tenant | Ships today (provisioner) |
| Documents | S3 key prefix per tenant | Ships today (`TenantPathResolver`) |
| Jobs / messaging / BPM | Name-prefixing / Flowable tenantId | Ships today |
| Identity | One identity per person (pool), membership = prefixed groups | Cognito config |
| Authorization | Tenant-scoped token (`cognito:groups` filtered per client) | Lambda + fix 2/3 |
| Token replay across tenants | `dirigible:tenant` claim == host tenant asserted per request | **Fork fix 2 — required** |
| Platform tables | Shared SystemDB — not partitioned | Accepted; mitigations in §8 |
| Registry / published code | Shared across tenants by design | Accepted: one application, all tenants run the same code |

---

## 6. Fork changes (minimal set)

The design deliberately needs **no change** to tenant resolution, datasource routing, provisioning, the synchronizer replay, or the authorities mapper. What it needs:

### Must fix before production

1. **Make Hikari pool sizing configurable** — `components/data/data-sources/.../manager/DataSourceInitializer.java:155-159`. The hardcoded `setMaximumPoolSize(20)` / `setMinimumIdle(10)` run after the properties constructor and override any env setting. Add `DirigibleConfig` keys (defaults preserving today's values); production sets max 5 / minIdle 0 / `idleTimeout≈120s` per tenant datasource. *This is the change that makes 100 tenants fit on one RDS instance.*
2. **Tenant assertion for all principal types** — `components/security/security-cognito/.../CognitoTenantFilter.java`. Replace the `custom:tenant` attribute check with: for `OAuth2AuthenticationToken` **and** `JwtAuthenticationToken`, require token claim `dirigible:tenant == currentTenant.subdomain` (M2M fallback: a `scope` whose resource-server prefix equals the subdomain); 403 otherwise. Closes two real holes: M2M/Bearer requests currently skip the membership check entirely (the filter guards on `instanceof OAuth2AuthenticationToken`), and nothing today prevents a token minted for tenant1 being used on tenant2.
3. **Enable method security under the cognito profile** — `@EnableMethodSecurity(jsr250Enabled = true)` exists only on `BasicSecurityConfig` (conditional on `basic.enabled=true`) and, in default-off form, on the Keycloak config. Read literally, every `@RolesAllowed` in `components/` is **inert** under the cognito profile. *Hypothesis — verify first* with an integration test (cognito profile, token without the required group, expect 403), then add the annotation to `CognitoSecurityConfiguration`.
4. **Deterministic filter ordering in the cognito chain** — `CognitoSecurityConfiguration` never adds `TenantContextInitFilter` (Basic/Keycloak/Snowflake configs all do); it and `CognitoTenantFilter` run only via Boot's servlet auto-registration, unordered relative to authentication. Add both explicitly to the chain, membership filter after tenant-context filter.
5. **Per-tenant logout** — `CognitoLogoutSuccessHandler` hardcodes the default client id and `DIRIGIBLE_HOST`. Resolve the current tenant's registration and build `logout_uri` from the request host.
6. **Tenant cache sizing** — `TenantExtractor.java:44` caps the cache at 100 entries, our exact target count; at 100+ it thrashes and every miss hits the DB. Make it configurable, default 1,000.
7. **Host-keyed OAuth client-registration resolver** — a `ClientRegistrationRepository` that resolves the request's tenant subdomain to that tenant's client id + secret, reading from AWS Secrets Manager (secret per tenant, cached with a short TTL) or the tenant registry. This is the app-side component the per-tenant-client decision (§4.2) pays for. Specified fresh: the shipped `security-client-registration` module is **not** a design input — whether the implementation reworks it or replaces it is decided at coding time against production criteria (no plaintext secret reads, no process-global mutable statics, tenant-linked rows).

### Worth doing soon after

8. **Fail-fast guards:** refuse to boot a production-profile task with `DIRIGIBLE_TRIAL_ENABLED=true` (it grants every role to every principal, tenant-blind) or with publishing enabled.
9. **Provisioning idempotency + `FAILED` status** — `DefaultDataSourceProvisioning` re-runs whole on failure and mints a new DB user/schema each retry. Check-before-create makes the 60 s retry loop safe; a `FAILED` status makes stuck tenants observable.
10. **Close the cross-tenant admin surface** — `TenantEndpoint`/`UsersEndpoint` `getAll()` are not tenant-scoped (any tenant's ADMINISTRATOR can enumerate everything). Under cognito the users endpoint is unused — gate both on `basic.enabled` or a platform-operator role. Interim mitigation is operational: never grant `ADMINISTRATOR`/`OPERATOR` groups to tenant users.
11. **Dockerfile hygiene:** non-root user; keep heap flags in env.

---

## 7. Tenant onboarding (automation runbook)

Idempotent script or Step Functions; order matters (the Lambda must be able to map the client before anyone signs in). No DNS or TLS step — wildcard covers it.

```
 1. Cognito: create resource server "<sub>" (scopes = role catalog)      [idempotent]
 2. Cognito: create confidential app client "<sub>"
       callback https://<sub>.app.com/login/oauth2/code/<sub>
       (+ optional M2M client with the tenant's scopes)
 3. DynamoDB: put clientId → "<sub>"                                     [Lambda's map]
 4. Secrets Manager: put dirigible/tenants/<sub>/oauth = { clientId, clientSecret }
       — the fork's host-keyed registration resolver (fix 7) reads it per request
 5. Dirigible: POST /services/core/security/tenants { name, subdomain }  → INITIAL
 6. Poll tenant status until PROVISIONED
       (set DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS=60;
        alarm if INITIAL > 15 min — there is no FAILED state, and each retry
        can orphan a DB user/schema until fork fix 9 lands)
 7. Cognito: create groups t:<sub>:<role>; create/invite the tenant-admin user
       and add them to t:<sub>:<their-role>
       — MANDATORY: the platform creates no user for a new tenant
       — the tenant-admin role is a business role, NOT Dirigible ADMINISTRATOR
 8. Smoke test: GET https://<sub>.app.com/login/<sub> → 302 to Cognito
```

Adding a person to another tenant later = add them to that tenant's groups. Nothing else.

**Offboarding** (no platform de-provisioning exists): disable the app client → delete the tenant's groups → delete the client registration (tenant now unreachable and un-login-able) → `pg_dump -n <TENANT_SCHEMA>` + S3 prefix copy to archive → manual `DROP SCHEMA … CASCADE` / `DROP USER`, datasource-row and tenant-row cleanup per runbook → restart the task (the tenant's pool and cache entries have no external eviction path).

---

## 8. Security hardening checklist

Shipped defaults that must be closed, and the closing move:

| Risk (default) | Action |
| --- | --- |
| ttyd shell, port 9000, **no auth** (`DIRIGIBLE_TERMINAL_ENABLED=true`) | `DIRIGIBLE_TERMINAL_ENABLED=false`; port unmapped |
| Graalium debugger, port 8081 (`DIRIGIBLE_GRAALIUM_ENABLE_DEBUG=true` shipped) | `DIRIGIBLE_GRAALIUM_ENABLE_DEBUG=false`; port unmapped |
| SFTP, port 8022, admin/admin | disabled/unmapped; SG exposes 8080 only |
| Actuator `management.endpoints.web.exposure.include=*` | `include=health`; WAF blocks `/actuator/**` from non-admin CIDRs |
| `DIRIGIBLE_TRIAL_ENABLED` grants all roles | `false` + fail-fast guard (fix 8) |
| Default basic admin/admin | cognito profile sets `basic.enabled=false`; leave `DIRIGIBLE_BASIC_*` unset |
| DEVELOPER role = in-JVM code execution | runtime-only: no `t:*:DEVELOPER` groups; `DIRIGIBLE_PUBLISH_DISABLED=true`; content baked in; authoring on the internal instance |
| Cross-tenant token/session replay | fork fix 2 (`dirigible:tenant` assertion) |
| Cross-tenant admin enumeration | fix 10 + never grant ADMINISTRATOR to tenant users |
| Tenant DB passwords stored in SystemDB (`DIRIGIBLE_DATA_SOURCES`) | accept + encrypt at rest; restrict SystemDB credentials; treat SystemDB compromise as full-unit compromise |
| CSRF disabled, frame options disabled | compensate at the edge (WAF, `SameSite=Lax`, short sessions); re-enabling CSRF needs UI compatibility testing (hypothesis §12) |
| Root container, writable FS | non-root user (fix 11); minimal task role (S3 prefix, KMS, Logs — nothing else) |
| Secrets | Secrets Manager → ECS `secrets` for DB creds + Cognito client secrets; nothing in plain env |
| Mode flags | `SPRING_PROFILES_ACTIVE=cognito`, `DIRIGIBLE_MULTI_TENANT_MODE=true`, `DIRIGIBLE_MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL=true` |

---

## 9. Operations

**Deploy.** New image (content baked in) → stop-then-start ECS deployment with circuit breaker + rollback. Planned downtime ≈ boot + first sync pass; deploy in a quiet window, optionally with an ALB fixed-response maintenance rule. Never run two versions concurrently (§2.2).

**Backup / restore.** RDS automated backups + PITR, **plus a nightly per-tenant `pg_dump -n <SCHEMA>` to S3** — that is the practical per-tenant restore path (restore a snapshot to a side instance and extract one schema is the slow fallback). S3 CMS bucket versioned. Cognito has no native backup: a nightly Lambda exports users/groups/clients to S3. Rehearse the single-tenant restore before a customer asks for it.

**Observability.** JSON logs to CloudWatch with the built-in tenant id in every line → per-tenant Logs Insights queries and error-rate metric filters are one-liners. `engine-open-telemetry` + ADOT sidecar for traces/metrics (JVM, per-tenant Hikari pools, Quartz). ALB access logs → Athena for per-Host traffic. **Alarms:** `DatabaseConnections`/`max_connections` > 70 %, tenant INITIAL > 15 min, health check failing > 5 min, task restart, p95 latency, heap after GC.

**Cost ballpark** (monthly, eu-central-1, on-demand): Fargate runtime ≈ $95 · RDS `db.r6g.large` Multi-AZ + 100 GB ≈ $450 · ALB ≈ $30 · NAT ≈ $40 · WAF ≈ $20 · Cognito Essentials ≈ $0–50 at modest MAU · S3/Secrets/CloudWatch/Route 53 ≈ $40 → **≈ $700/month**, + ~$150 for the authoring instance if always on (schedule it to business hours).

---

## 10. Where this agrees / differs with `docs/aws-multitenancy-research`

Designed independently, the two efforts **converge** on: one Cognito pool + one app client per tenant; a pre-token-generation Lambda minting tenant-scoped tokens; a `dirigible:tenant` claim asserted against the host tenant; the M2M bypass and `@RolesAllowed`-inert-under-cognito findings; single-writer ⇒ one task + stop-then-start; S3 for CMS; registry baked into the image; the runtime/authoring split as a security boundary; `/services/core/healthcheck` as the ALB target; RDS Proxy rejected for `SET search_path` pinning. That convergence is strong evidence both designs sit on the platform's real constraints.

**Differences (deliberate):**

| Topic | This proposal | Research branch | Why differ |
| --- | --- | --- | --- |
| Where roles live | **Prefixed Cognito groups** (`t:<sub>:<role>`); Lambda filters by prefix | DynamoDB membership store; Lambda reads it; groups synthesized | One fewer authoritative store and admin surface at ≤100 tenants; accepts the 100-groups-per-user ceiling they designed around. Migrating to their model later is only a Lambda + data change — the token contract is identical |
| Client-registration plumbing | **Fork-built host-keyed `ClientRegistrationRepository`** reading Secrets Manager; per-tenant clients re-derived from first principles (§4.2) | Reuses the shipped `security-client-registration` module (`POST /services/security/client-registrations`) | Their own review flags that module as defective (process-global static map, plaintext secret reads, no tenant column). This proposal removes it as a design input entirely; the client topology must stand without it — and does |
| Database | RDS PostgreSQL Multi-AZ (provisioned) | Aurora Serverless v2 | One right-sized unit with steady load; provisioned is simpler and cheaper than a serverless floor |
| Pool sizing | **Fork fix** making Hikari sizing configurable | Size the DB around the hardcoded pools | Their as-is doc was written under a no-code-changes constraint; with the fork allowed, fixing the root cause is strictly better |
| Scope | One unit + short growth note | Multi-cell control plane, tenant→cell registry, lifecycle automation | Right-sizing per the actual ask; their cell model remains the correct end-state at larger scale |

---

## 11. How it grows

Past what one unit sustains (connection budget, CPU, or blast-radius tolerance):

- **Add a second unit** = one more {ECS service + RDS instance + ALB}. Tenants are pinned to a unit.
- **DNS changes shape once:** wildcard-to-one-ALB becomes a per-tenant Route 53 alias created at onboarding, pointing at the tenant's unit. (Keep the wildcard as a catch-all to a "no such tenant" page.)
- **What stays shared:** the Cognito user pool (identity, SSO and per-tenant roles are unit-agnostic), the pre-token Lambda + client map, the image pipeline, WAF rules, the wildcard certificate.
- **Watchpoints on the way:** the 100-groups-per-user Cognito hard limit (a person in ~30+ tenants — that is the trigger to move the membership graph to a store, per the research branch's model); per-unit connection budget; and only if a *single tenant* outgrows a unit does the deep fork surgery (external broker, non-local repository) become worth discussing.
- **True horizontal scaling** (more than one task per unit) is blocked by the platform, not by this model — the layer-by-layer analysis of what blocks it today and what enabling it would take (external broker, per-node synchronizer replay, boot-race fixes, cache invalidation, external sessions) lives in [`AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md`](AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md) §10; items 1–5 there apply to both models verbatim.

---

## 12. Hypotheses to verify before go-live

Flagged, not asserted:

1. **`@RolesAllowed` inert under the cognito profile** — annotation absence is confirmed by inspection; behaviour needs the integration test in fix 3 before relying on (or adding) method security.
2. **`groupsToOverride` → `cognito:groups` as seen by `userAuthoritiesMapper`** — the mapper reads the OIDC principal's attributes (ID token / userinfo); confirm with one real pool that the overridden groups arrive there, before building onboarding automation.
3. **RDS master credential can execute the provisioner's `CREATE USER` / `CREATE SCHEMA … AUTHORIZATION`** — provision one throwaway tenant end-to-end on RDS first.
4. **CSRF re-enablement compatibility** with the shipped UI (§8).
5. **Boot + first-sync duration at ~100 provisioned tenants** — drives the deploy window and the ALB health-check grace period.
6. **Federated-IdP SSO behaviour** (if a tenant later brings its own IdP): the pool-wide session-cookie SSO statement is worded for local users; test the cross-tenant hop for federated users before promising it.
