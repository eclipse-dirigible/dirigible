# Multitenant Dirigible on AWS — Recommended Target Architecture

**Status:** design proposal. Nothing is implemented.
**Date:** 2026-07-27
**Constraint:** changes to Dirigible's multitenant model **are** permitted. This is a product fork; breaking changes are acceptable.

**Companion documents**
- [`AWS_MULTITENANCY_RESEARCH.md`](AWS_MULTITENANCY_RESEARCH.md) — what you can deploy on AWS *today* with zero code changes. Still valid; this document does not replace it.
- [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) — the behaviour reference for how multitenancy works now.

---

## 1. Purpose

The as-is document was written under a deliberate constraint: treat the platform as a fixed black box. That produced an honest but limited answer — cells, one instance per cell, and a list of things you simply have to live with.

That constraint is now lifted, and one new functional requirement drives most of what follows:

> **A user registered in one tenant must be able to log into another tenant, if they hold the necessary authorizations there.**

Plus a direction that changes the shape of the answer entirely:

> **The implementation need not use the local user and tenant tables. That data can come from the Cognito token.**

Taken together these move the design from *"add a membership table to the platform schema"* to *"make Dirigible a claims-driven authorization consumer and let the identity provider own identity."* That is a better architecture for a SaaS, and — as §6 shows — it is a **smaller** change than the alternative, because two thirds of it already exists by accident.

### Scope

| In scope | Out of scope |
| --- | --- |
| Identity, tenant membership, per-tenant authorization | The data layer (schema-per-tenant on Aurora stands unchanged) |
| The Cognito topology and the claims contract | Compute topology (ECS Fargate, cells — unchanged) |
| The platform changes those imply | The IDE/authoring tier's user-scoped state (§9.3 records it as a known constraint) |
| One runtime isolation gap that must be decided regardless (§6.11) | Upstream contribution / migration path — this is a fork |

---

## 2. The requirement, and why the current model cannot express it

Today a "user" is a row scoped to exactly one tenant — `components/core/core-tenants/.../domain/User.java`:

```java
@Entity
@Table(name = "DIRIGIBLE_USERS", uniqueConstraints = {@UniqueConstraint(columnNames = {"USER_TENANT_ID", "USER_USERNAME"})})
public class User extends Artefact {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "USER_TENANT_ID") private Tenant tenant;
    @Column(name = "USER_USERNAME", nullable = false) private String username;
```

`tenant` is `@ManyToOne` — strictly single-valued. There is **no `@ManyToMany` anywhere in the codebase** and no membership table; a repo-wide search finds only OData mapping-table machinery. So "the same person in two tenants" is two unrelated rows, two password hashes, two independent role-assignment sets, and no way for the platform to know they are the same person.

Login enforces it — `CustomUserDetailsService.loadUserByUsername`:

```java
Tenant tenant = tenantContext.getCurrentTenant();
User user = userService.findUserByUsernameAndTenantId(username, tenant.getId())
                       .orElseThrow(() -> new UsernameNotFoundException(
                               "Username [" + username + "] was not found in tenant [" + tenant + "]."));
```

Two things are worth noticing about that method, because they define the whole problem:

1. **The tenant is not chosen by the user.** It comes from `TenantContext.getCurrentTenant()`, i.e. from the `Host` header via `TenantExtractor`. There is no mechanism by which a user says "log me into tenant B."
2. **The tenant is resolved and then thrown away.** The method returns a plain `org.springframework.security.core.userdetails.User(username, password, authorities)` — no tenant field. From that point on the security context knows *the username*, the thread-local knows *the tenant*, and nothing ties them together. This is the seam any multi-tenant identity model has to close.

Role assignments have no tenant dimension either — `UserRoleAssignment` is `(USER_ID, ROLE_ID)`, unique on that pair, deriving its tenancy transitively from the user row. So "different roles in tenant A and tenant B" is expressible today *only* by virtue of being two different users.

### The irony: the membership model already exists, one layer up

`components/security/security-cognito/.../CognitoTenantFilter.java` already treats one identity as a member of many tenants:

```java
String tenantAttribute = oauthToken.getPrincipal().getAttribute("custom:tenant");
...
Set<String> userTenants = new HashSet<>(Arrays.asList(tenantAttribute.split(","))
                                              .stream().map(e -> e.trim()).collect(Collectors.toList()));
if (!userTenants.contains(currentTenant.get().getSubdomain())) {
    forbidden("User is not member of the [" + ... + "] tenant", response);
}
```

`custom:tenant` is a **comma-separated list of tenant subdomains**. The IdP therefore already models one-identity-in-many-tenants. The filter parses that set, answers one yes/no question with it, and **discards it** — nothing downstream can see the membership, and the roles the user gets are `cognito:groups`, a single flat pool-global list that is identical in every tenant.

So the gap is precise: **membership is already cross-tenant; authorization is not.** The design below closes exactly that gap.

One further asymmetry that makes the "token as source of truth" direction natural: under the Cognito and Keycloak profiles the platform's own user tables are **already bypassed**. `CognitoSecurityConfiguration.userAuthoritiesMapper()` derives authorities solely from the `cognito:groups` claim, no `DIRIGIBLE_USERS` row is consulted, and none is created. Half of the recommended model is already the shipped behaviour — it is just tenant-blind.

---

## 3. Design principles

1. **The IdP is the system of record for identity.** The platform stores no passwords, no user directory, no role assignments.
2. **The platform authorizes from claims, per request.** Roles arrive in the token; Dirigible maps them to authorities and enforces them.
3. **The tenant is resolved from the host and *proven* by the token.** Host-based resolution stays (it is good, and it already reads `x-forwarded-host`). The token must independently assert which tenant it is for, so a token minted for tenant A cannot be replayed against tenant B.
4. **A token is scoped to one tenant.** Not "here are all your tenants and all your roles everywhere" — that bloats tokens and makes every authorization decision a filtering exercise.
5. **The control plane owns the membership graph.** Cognito authenticates and mints tokens; a control-plane store owns tenant ↔ user ↔ roles; the platform consumes the result. Three components, three jobs.
6. **`.roles` artefacts remain the role vocabulary.** The registry defines what roles exist; the control plane assigns them. That keeps role definitions where developers author them.

---

## 4. Recommended identity architecture

### 4.1 One Cognito user pool, one app client per tenant

```
                        ┌───────────────────────────────────────────────┐
                        │  ONE Cognito user pool                        │
                        │  • one identity per person (sub = global id)  │
                        │  • managed-login session cookie shared        │
                        │    across ALL app clients in the pool (1h)    │
                        │                                               │
                        │  app client "acme"    app client "globex"  …  │
                        │    callback:            callback:             │
                        │    acme.app.ex.com/…    globex.app.ex.com/…   │
                        └───────┬───────────────────────┬───────────────┘
                                │                       │
                     ┌──────────▼───────────────────────▼──────────┐
                     │  Pre-token-generation Lambda (V2_0 / V3_0)  │
                     │  clientId → tenant                          │
                     │  (sub, tenant) → roles, from the control     │
                     │                  plane store                │
                     │  emits: groupsToOverride = roles HERE        │
                     │         dirigible:tenant  = this tenant      │
                     │         dirigible:tenants = membership list  │
                     │  denies issuance if not a member             │
                     └──────────┬──────────────────────────────────┘
                                │  tenant-scoped token
                     ┌──────────▼──────────────────────────────────┐
                     │  Dirigible                                   │
                     │  host → tenant  (TenantExtractor)            │
                     │  assert dirigible:tenant == host tenant      │
                     │  cognito:groups → authorities (per tenant)   │
                     └──────────────────────────────────────────────┘
```

**Why a single pool.** The requirement forces it: cross-tenant login means one identity per person, and pool-per-tenant means N accounts per person. AWS's guidance is explicit that a shared pool "supports models where customers have accounts in more than one application," and — decisively — that when you authenticate local users in a pool, "their session cookie authenticates them for all app clients in the same user pool." AWS then names the only two ways to *prevent* that cross-app-client SSO: separate per-tenant pools, or replacing hosted-UI sign-in with the API. **We want the behaviour AWS describes as the thing you'd have to work to avoid.** ([multi-tenant application best practices](https://docs.aws.amazon.com/cognito/latest/developerguide/multi-tenant-application-best-practices.html))

**Why one app client per tenant.** This is the move that makes the platform change small. The pre-token-generation Lambda receives `event.callerContext.clientId`, so it knows which tenant the token is being minted for, and can emit *only that tenant's roles*. Consequences:

- **The token is tenant-scoped at issuance.** Dirigible's existing `cognito:groups` → authorities mapping becomes correct per tenant **with no change to that mapper**.
- **Roles need not be real Cognito groups.** `groupsToOverride` replaces the `cognito:groups` claim outright, so the Lambda can synthesize it from the control-plane store. That sidesteps the non-adjustable quota **"Groups to which each user can belong: 100"** — a user in 30 tenants with 4 roles each would otherwise blow through it.
- **Per-tenant callback URLs**, which Dirigible's per-tenant client registrations already support (`redirectUri` is a per-registration column).
- **Per-tenant federated IdPs later.** An app client selects which identity providers it offers, so "tenant X federates its own Entra ID" is an app-client setting — the enterprise-SSO extension point, with no redesign. (Identity providers per user pool: 300, adjustable to 1,000.)
- **The tenants-per-pool ceiling is the app-client quota**: 1,000 by default, adjustable to 10,000. Well past the point where you would shard pools by cell anyway.

**Why host-scoped sessions make this work cleanly.** `JSESSIONID` carries no `Domain` attribute, so it is host-only. Each tenant subdomain therefore gets its own Spring session, holding its own tenant-scoped authorities. This neutralises what would otherwise be a serious problem: **authorities are snapshotted at login** and never re-resolved per request in session-based flows. Because the snapshot is per (host, session), and host ⇒ tenant, the snapshot *is* per-tenant. No per-request re-derivation machinery is needed.

### 4.2 The claims contract

| Claim | Source | Meaning | Notes |
| --- | --- | --- | --- |
| `sub` | Cognito | The global, immutable identity of the person | The Lambda **cannot** modify `sub` or `cognito:username`; use `sub` as the identity key |
| `cognito:groups` | Lambda `groupsToOverride` | The caller's roles **in this token's tenant** | Must match `.roles` artefact names (and the three platform roles) exactly |
| `dirigible:tenant` | Lambda `claimsToAddOrOverride` | The tenant this token was minted for | Dirigible asserts it equals the host-resolved tenant. Defence in depth |
| `dirigible:tenants` | Lambda `claimsToAddOrOverride` | The caller's full membership list | For the UI tenant switcher only — never an authorization input |
| `email` | Cognito | Display identity | Already the configured `user-name-attribute` |
| `scope` | Cognito / Lambda | M2M authorization | Maps to roles via `ScopeRoleJwtAuthoritiesConverter` and `.scopes` artefacts |

Trigger version **V2_0** is required for access-token customization and complex (array/JSON) claim values; it needs the **Essentials or Plus** feature plan. Use **V3_0** if M2M client-credentials tokens need the same treatment — that is the only version that fires for `TokenGeneration_ClientCredentials`. ([pre token generation Lambda trigger](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html))

### 4.3 Flows

**First login to a tenant.**

```
user → https://acme.app.example.com/
  → Dirigible: host → tenant "acme"; unauthenticated
  → redirect /login/acme → /oauth2/authorization/acme   (registration id = subdomain, already implemented)
  → Cognito managed login (app client "acme"): credentials + MFA
  → Pre-token Lambda: clientId "acme" → tenant "acme"
        member? no  → deny, no token issued
        member? yes → groups = roles(sub, acme), dirigible:tenant = acme,
                      dirigible:tenants = [acme, globex]
  → callback https://acme.app.example.com/login/oauth2/code/acme
  → Dirigible: assert dirigible:tenant == "acme"; authorities = cognito:groups
  → JSESSIONID for acme.app.example.com
```

**Cross-tenant navigation — the requirement.**

```
same browser → https://globex.app.example.com/
  → no JSESSIONID for this host → unauthenticated
  → /login/globex → /oauth2/authorization/globex
  → Cognito: managed-login session cookie is still valid (pool-wide, ≤1h)
              ⇒ NO credential prompt
  → Pre-token Lambda: clientId "globex" → tenant "globex"
              ⇒ groups = roles(sub, globex)      ← DIFFERENT roles
  → separate JSESSIONID for globex.app.example.com
```

One sign-in, two tenants, different roles in each, and a user who is not a member of `globex` never gets a token for it. That is the requirement satisfied, using Cognito's documented default behaviour rather than fighting it.

**Role change propagation — state this as an SLA, do not hand-wave it.** The trigger also fires on `TokenGeneration_RefreshTokens`, so a membership or role change takes effect on the next token refresh, not instantly. With a 1-hour access token the worst case is ~1 hour, plus the life of the Dirigible session. If a revocation must be immediate, the control plane has to call Cognito's global sign-out for that user *and* the platform has to invalidate the session — design that as an explicit "eject user" operation rather than assuming propagation is instant.

---

## 5. Why not the alternatives

| Alternative | Why not |
| --- | --- |
| **One user pool per tenant** | Directly contradicts the requirement — a person needs one account per tenant, with separate passwords and MFA enrolments, and there is no cross-tenant sign-in. It is the right answer only when you want to *prevent* cross-tenant access |
| **One shared app client for all tenants** | Cognito callback URLs do not support wildcards and cap at 100 per app client, so per-tenant subdomain callbacks stop working past 100 tenants. The Lambda also loses its tenant signal (`clientId` is the only reliable one — `clientMetadata` is not passed on `InitiateAuth`/hosted-UI flows), so the token must carry *every* tenant's roles and the app must filter per request. Bigger tokens, more platform code, no per-tenant federation |
| **Per-tenant roles as real Cognito groups** (`acme:ADMINISTRATOR`) | Hits the non-adjustable **100 groups per user** quota for anyone in many tenants, and makes Cognito the authority for a graph that belongs to the control plane. Synthesizing the claim in the Lambda has none of these problems |
| **A local membership table in the platform DB** | Duplicates the control plane, requires a migration, and keeps the cross-tenant admin CRUD surface (§9.1) that the claims model deletes outright. It also keeps the platform in the business of storing credentials |
| **Custom attribute holding a role map** (`custom:tenant_roles` as JSON) | Custom attributes cap at 2,048 bytes and 50 per pool, and are user-global — so the token would again carry every tenant's roles. The Lambda + `groupsToOverride` path has no size problem because the claim is per-token |

---

## 6. Platform changes

Ordered so that each step is verifiable before the next depends on it.

### 6.1 Phase 0 — verify method security is actually on (prerequisite)

`@EnableMethodSecurity(securedEnabled = false, jsr250Enabled = true, prePostEnabled = false)` appears on exactly one class — `core-tenants/.../security/BasicSecurityConfig.java:36` — which is `@ConditionalOnProperty(name = "basic.enabled", havingValue = "true")`. The only other occurrence is `KeycloakSecurityConfiguration.java:50`, a bare `@EnableMethodSecurity` whose default is `jsr250Enabled = false`. Every OIDC profile properties file sets `basic.enabled=false`.

Read literally, **all 52 `@RolesAllowed` annotations in `components/` are inert under the cognito, keycloak, snowflake and github profiles**, leaving only the URL-pattern layer and the `.access` filter. If that is true it is a live authorization bug independent of this design, and it changes which layer must be made tenant-aware. **Confirm by experiment before designing anything on top of it** (§10).

### 6.2 Introduce a `TenantIdentityResolver` SPI

The one new abstraction. It answers three questions from whatever the request carries:

- *Who is this?* → a stable identity key (`sub`), plus a display name.
- *May they act in this tenant?* → yes/no, for the host-resolved tenant.
- *What are their roles here?* → a role-name set.

Implementations: **cognito-claims** (production), **keycloak-claims**, **local-tables** (dev / single-tenant). Everything else consumes its output, so provider differences stop leaking into the platform. Two unused SPI seams already exist and are the natural mounting points — `core-base/.../http/access/CustomSecurityConfigurator.java` (consumed by `HttpSecurityURIConfigurator`, zero implementations) and `core-base/.../http/access/UserAccessVerifier.java` (loaded by `UserRequestVerifier` via `ServiceLoader`, zero implementations).

### 6.3 Demote the local user tables

`DIRIGIBLE_USERS`, `DIRIGIBLE_USER_ROLE_ASSIGNMENTS`, `UsersEndpoint`, `AdminUserInitializer` and the `view-security` users UI stop being the authorization source. They survive only behind the dev SPI implementation.

This is worth doing for the security posture alone. It **closes the cross-tenant admin gap by construction** (§9.1) and retires four defects found while mapping the code, all in `core-tenants`:

- `UserService.updateUser` stores the password **un-encoded** (`user.setPassword(password)` at line 115) while `createNewUser` BCrypts it — so a `PUT` on a user destroys the hash.
- `UserParameter` is annotated `@Valid` at both call sites but carries **no constraint annotations at all**, so username/password/tenant may all be null.
- `UsersEndpoint.get` and the username search call `.get()` on an `Optional` → `NoSuchElementException` → HTTP 500 instead of 404.
- `User.updateKey()` accepts a null tenant and silently produces the key `user|-|admin|null`.

### 6.4 Unify and universalise the tenant membership check

Replace the two byte-identical `CognitoTenantFilter` / `KeycloakTenantFilter` clones with one provider-agnostic `TenantMembershipFilter` that reads the normalized SPI output, and add it **inside** the security chain. It must assert `dirigible:tenant == host-resolved tenant` and reject on mismatch. Three concrete holes close:

- **Cognito's chain never adds `TenantContextInitFilter` at all.** `BasicSecurityConfig`, `KeycloakSecurityConfiguration` and `SnowflakeSecurityConfig` all `addFilterBefore(...)` it; `CognitoSecurityConfiguration` does not. It still runs, but only because Spring Boot auto-registers plain `OncePerRequestFilter` `@Component`s as servlet filters at lowest precedence — so its ordering relative to authentication is an accident, not a design.
- **M2M requests bypass the membership check entirely.** Both filters guard on `principal instanceof OAuth2AuthenticationToken`; a Bearer-token request produces a `JwtAuthenticationToken`, which is not one, so the whole body is skipped.
- **Nothing rejects a session minted on `t1.example.com` replayed against `t2.example.com`** in the basic and snowflake profiles. Authorities are snapshotted; the tenant is re-derived per request from the host; no filter checks that the two agree. Under the recommended design the host-scoped-cookie + `dirigible:tenant` assertion makes this structurally impossible, but the assertion has to be written.

Also note both filters are gated on flags read **once in the constructor**, so they are not runtime-flippable, and when either flag is off **the entire membership check is skipped**.

### 6.5 Carry the tenant in the principal

Close the seam at `CustomUserDetailsService:76`. A principal holding `(sub, username, tenant, roles)` means the tenant is available wherever the identity is, instead of living in a parallel thread-local. Then:

- Add `UserFacade.getTenant()`. `UserFacade` today has no reference to `TenantContext` at all, and all its methods are `static`, so this needs a small restructure.
- Add a tenant accessor and `getRoles()` to the TypeScript facade (`api-modules-javascript/.../modules/src/security/user.ts`) — it currently has neither, while the Java SDK has `getRoles()`. TS user code cannot presently discover which tenant it is running in.
- Guard `UserFacade.getUserRoles()`, which reads `SecurityContextHolder.getContext().getAuthentication().getAuthorities()` unguarded and NPEs on job/anonymous threads.

### 6.6 Decide super-role semantics explicitly

The DEVELOPER/ADMINISTRATOR "bypass everything" short-circuit is implemented **three times**: `api-security/.../UserFacade.isInRole`, `engine-java/.../controller/ControllerInvoker.checkRoles`, and `core-extensions/.../ExtensionService.validateRoles`. Meanwhile `engine-native-apps/.../proxy/ExposedPathFilter` deliberately rejects it and documents why. Today these super-roles are neither per-tenant nor platform-global — they are "whatever tenant you logged into, applied everywhere." A tenant-aware model has to state a position and collapse the three copies onto it.

### 6.7 Rework or delete `security-client-registration`

Derive OAuth client registrations from the tenant registry rather than maintaining a parallel CRUD surface. As it stands:

- **No tenant column.** The tenant linkage is convention only (`registrationId` should equal `Tenant.subdomain`).
- **`registrationId` is the entity `name`, while the REST path uses `id`** — `withRegistrationId(registration.getName())`. The seeded Cognito default is constructed as `new ClientRegistration(clientName, "cognito", ...)` then `setId("default-tenant")`, so its name is `"cognito"`. But `CognitoLoginController` accepts only provisioned tenant **subdomains**, and the default tenant's subdomain is `default`. So `/login/cognito` 404s and `/login/default` finds no registration — the default registration is reachable only via the `permitAll` `/oauth2/authorization/cognito`. (This confirms, as a real inconsistency, what the as-is document listed as a verification item.)
- **`REGISTRATIONS` is a `private static` non-thread-safe `HashMap`**, process-global, never invalidated, and populated *only* as a side effect of `iterator()` — so `findByRegistrationId` (what Spring actually calls) returns `null` until something iterates.
- **`GET` returns `clientSecret` in plaintext** to any tenant's ADMINISTRATOR / DEVELOPER / OPERATOR, with no tenant predicate on any operation.
- The module's beans are unconditional, so its `ClientRegistrationRepository` replaces Spring Boot's in **every** profile, including basic and snowflake.

### 6.8 Remove the unused authorization-server starter

`components/security/security-client-registration/pom.xml:55` pulls in `spring-boot-starter-oauth2-authorization-server`. Nothing configures it — there are no `spring.security.oauth2.authorizationserver.client` properties anywhere in the repo, so no `RegisteredClientRepository`, no token endpoint, no issuer. Its only observable effect is that Boot's auto-configuration publishes a `JwtDecoder` backed by a generated in-memory RSA key, which is why all three resource-server configs must defensively pin their own decoder on the configurer. Removing the starter lets those workarounds go. (The regression test `CognitoResourceServerJwtDecodeTest` guards the current behaviour and documents the failure mode: "no matching key(s) found".)

### 6.9 Kill or scope `DIRIGIBLE_TRIAL_ENABLED`

It grants **every platform role to every authenticated principal**, tenant-blind, in three places (both OIDC authority mappers and `ScopeRoleJwtAuthoritiesConverter`). In a multi-tenant deployment that is a full cross-tenant privilege escalation switch. Either delete it or restrict it to the default tenant in a non-production profile.

### 6.10 Keep the tenant registry, as a projection

`DIRIGIBLE_TENANTS` must stay: provisioning needs it, and `TenantContext.executeForEachTenant` — which drives the per-tenant replay of the eight multitenant synchronizers — needs to enumerate all tenants, which a per-user token can never do. But it becomes a **projection of the control plane**, reconciled by the onboarding automation rather than hand-maintained. Provisioning should be triggered by the control plane on demand instead of discovered by a 900-second poll.

### 6.11 Decide the client-Java isolation gap (runtime, not authoring)

`engine-java/.../runtime/JavaCompiledOutputDirectory.java:42` resolves to a single global directory for **all** tenants:

```java
this.directory = Paths.get(repoRoot, "dirigible", "java-compiled", "bin")
```

loaded by one `ClientClassLoader`. Unlike the workspace/git/LSP collisions in §9.3, this is a **runtime** concern: client Java code is not tenant-isolated at all. Two tenants publishing a same-FQN class share one compiled artefact and one classloader. The same applies to `PublisherService`, which publishes into the **shared** `/registry/public` — so two same-named users in two tenants can overwrite each other's running application code.

Neither is caused by the identity change, and both need a decision regardless. The honest options are: accept it (a single shared registry is the platform's model, and per-tenant code is not currently a feature), or partition the compiled output and classloader per tenant, which is a substantial change to `engine-java`.

---

## 7. The authorization model after the change

**Where authorities come from.** The `TenantIdentityResolver` implementation for the active profile. In production: `cognito:groups` from a tenant-scoped token, mapped by `AuthoritiesUtil.toAuthorities` (unchanged — a flat `ROLE_<NAME>` string). No composite `ROLE_<TENANT>__<ROLE>` encoding is needed, because the token is already tenant-scoped; that matters, since `AuthoritiesUtil.toRoleNames` strips the prefix with a regex `replaceAll` and would mangle composite names.

**What stays global.** The role *vocabulary* and the ACL definitions: `.roles`, `.access` and `.scopes` artefacts, and their entities (`Role`, `Access`, `Scope` — none has a tenant column). This is correct and should be stated as a decision, not an omission: the registry is shared, so the definitions are shared. Note `Role.name` is **de-facto globally unique** because `BaseArtefactService.findByName` and `RoleService.roleExistsByName` are single-result queries that throw `IncorrectResultSizeDataAccessException` on duplicates — so per-tenant role *names* are not an option without reworking those.

**What becomes per-tenant.** Assignments only, and they live in the token.

**Where enforcement happens** — unchanged mechanisms, now fed correct per-tenant authorities:

| Layer | File | Note |
| --- | --- | --- |
| URL patterns | `core-base/.../http/access/HttpSecurityURIConfigurator.java` | `PUBLIC` / `DEVELOPER` / `OPERATOR` / `AUTHENTICATED` groups, then `anyRequest().denyAll()` |
| Platform endpoints | 52 × class-level `@RolesAllowed` | Only three distinct sets, all built-in roles. **See §6.1 first** |
| `.access` constraints | `engine-security/.../filter/SecurityFilter.java` + `verifier/AccessVerifier.java` | Longest-path-wins; magic role `Public`. The ACL cache is a per-JVM `volatile Map`, not tenant-keyed — fine while the definitions are global |
| Client Java | `engine-java/.../controller/ControllerInvoker.checkRoles` | Duplicates the super-role logic (§6.6) |
| UI visibility | `core-extensions/.../ExtensionService.validateRoles` | Third copy; returns `true` when there is no request context |
| CMIS | `api-cms/.../CmisFacade` | `.access` with `scope = "CMIS"`, walked per path prefix |
| Native apps | `engine-native-apps/.../proxy/ExposedPathFilter` | Deliberately strict; no super-role bypass |
| M2M | `engine-security/.../oauth/ScopeRoleJwtAuthoritiesConverter` | `scope` → role via `.scopes`; **must gain the membership check** (§6.4) |

---

## 8. What this changes in the AWS architecture

Everything in the as-is document's data, compute, storage and cell design **stands unchanged**: schema-per-tenant on Aurora, ECS Fargate, one instance per cell, wildcard subdomains, S3-backed CMS, connection count as the cell-sizing signal. The deltas are all in identity and the control plane.

**The control plane grows into the authority for the membership graph.** It owns tenant ↔ user ↔ roles (DynamoDB is the natural fit), and it is what the pre-token Lambda reads. It also owns the mirror of `.roles` artefacts, so it can validate that a role being assigned actually exists in the target tenant's application.

**Onboarding a tenant additionally provisions a Cognito app client.**

```
1. control plane: allocate cell, reserve subdomain, write tenant → cell
2. Cognito:       create app client "<subdomain>" with callback
                  https://<subdomain>.app.example.com/login/oauth2/code/<subdomain>
                  (+ attach federated IdPs later, per tenant)
3. Route 53:      CNAME <subdomain>.app.example.com → cell ALB
4. cell:          POST /services/security/tenants  { name, subdomain }   → INITIAL
5. cell:          register the OAuth client (or derive it — §6.7)
6. control plane: write the first membership row (tenant admin) — no local user is created
7. wait for provisioning: CREATE USER + CREATE SCHEMA + synchronizer pass → PROVISIONED
```

Step 6 replaces the old `POST /services/security/users`. **Nothing writes a user row into the platform database.**

**Adding a person to a second tenant** is one control-plane write plus, at most, a token refresh. No DNS, no infrastructure, no platform call. That is the payoff.

**Offboarding gets simpler and safer.** Revoking membership is a control-plane delete, and it takes effect on the next token refresh (or immediately via a global sign-out). The tenant-deletion runbook still has to drop the schema, the DB user, the datasource row and restart the cell — but it no longer has to hunt down user rows and role assignments across the platform DB.

**The tenant switcher becomes possible.** `dirigible:tenants` gives the UI the membership list, so the shell can offer "switch to Globex" as a link to the other subdomain. The Harmonia application shell (`components/resources/resources-application`) is the natural place for it.

---

## 9. Multi-tenancy security review

### 9.1 Closed by construction

- **The cross-tenant admin gap.** `TenantEndpoint` and `UsersEndpoint` do not tenant-scope reads: `getAll()` returns every user of every tenant, and `createUser` / `updateUser` accept an arbitrary `tenant` id, so an `ADMINISTRATOR` in *any* tenant can enumerate and modify *every* tenant's users. Removing the local user directory removes the surface. (`RepositoryEndpoint`'s unrestricted `/{*path}` CRUD is the same class of problem and is **not** closed by this change — it needs its own fix.)
- **Tenant DB passwords in the platform DB.** Still stored in `DIRIGIBLE_DATA_SOURCES`, but the platform no longer *also* holds every user's password hash.
- **Duplicate user administration.** One membership graph instead of N per-tenant directories.

### 9.2 Must be closed deliberately

| Item | Where |
| --- | --- |
| M2M tokens bypass the tenant membership check | §6.4 |
| `@RolesAllowed` possibly inert outside the basic profile | §6.1 |
| `DIRIGIBLE_TRIAL_ENABLED` grants all roles, tenant-blind | §6.9 |
| Client-registration secrets readable cross-tenant | §6.7 |
| `RepositoryEndpoint` `/{*path}` CRUD, no tenant scoping, no ownership check | not addressed here |
| ttyd (9000), Graalium inspector (8081), SFTP (8022) with `admin`/`admin`, actuator `include=*` | as-is document §3.6 — unchanged |

### 9.3 Explicitly left open (out of scope per the runtime-only decision)

Production is runtime-only and authoring runs on a single instance per environment, so these cannot occur in production — but they are real, they exist today, and they must be recorded:

- **`/users/<user>/<workspace>` is keyed by bare username, hardcoded five times independently**: `ide-workspace/.../WorkspaceService.generateWorkspacePath`, `ide-workspace/.../PublisherService.generateWorkspacePath`, `ide-git/.../GitFileUtils` (with a **duplicate** of the git-root logic in `commons-helpers/.../FileSystemUtils`), `ide-java-lsp/.../JdtLsManager.workspaceRoot()`, and `ide-java-debug/.../JavaDebugManager`. The two `IRepositoryStructure.PATTERN_USERS_WORKSPACE_DEFAULT` / `_NAMED` constants that ought to be the single definition are **dead code** — the only references to them are in the interface that declares them. (`GitFileUtils.PATTERN_USERS_WORKSPACE` is a separate, similarly-named constant that *is* used — by `GitFileUtils` and `CloneCommand` — which is precisely the duplication.) So there is no single place to change.
- **`GitFileUtils.USER_WORKSPACE_SEGMENTS_COUNT = 3` / `USER_WORKSPACE_PROJECT_SEGMENTS_COUNT = 4`** (lines 53, 56). Inserting a tenant path segment makes these off by one, and the failure mode is **silent file mangling in `ShareCommand`**, not an exception. This is the easiest thing on the list to miss.
- **The ttyd terminal proxies every user of every tenant to one shared shell** (`ws://localhost:9000/ws`), with no per-user or per-tenant working directory.
- **`Problem.createdBy` has no tenant column**, and `ProblemEndpoint`'s `deleteAll` / `deleteAllByStatus` / `deleteAllByIds` are global un-scoped mass deletes in the shared `SystemDB`.
- **`AuditorAwareHandler.getCurrentAuditor()` returns the literal `"SYSTEM"`** with a `// TODO`. So `Artefact.createdBy` is not a username today — but "fixing" it with `UserFacade.getName()` would instantly create the ambiguity across ~35 tables.
- **`HanaConnectionEnhancer`** sets `APPLICATIONUSER` / `XS_APPLICATIONUSER` to a bare username, so two same-named users in different tenants are indistinguishable to HANA-side row-level security and auditing.
- **`TaskServiceImpl.getTasks()` is tenant-filtered but not user-filtered**, while `sdk/bpm/Tasks.list()`'s Javadoc claims "the platform pre-filters by candidate user / group." The Javadoc is wrong; the method returns every task in the tenant. An intra-tenant leak, not a cross-tenant one.

BPM is worth a note on the positive side: `TaskServiceImpl.prepareQuery` applies `taskTenantId(getTenantId())` **and** `taskAssignee(UserFacade.getName())` — the one place in the codebase that combines tenant and user correctly. Under the new model its semantics become "my tasks in the tenant I am currently in," which is almost certainly the desired reading, but it should be confirmed rather than inherited by accident. Note also that a `.bpmn` with a design-time `assignee="admin"` deploys into every tenant via `BpmnSynchronizer` and resolves to a different person in each.

---

## 10. Verification and first steps

The cheapest possible first move costs nothing and settles the most important ambiguity:

1. **Write a failing integration test** asserting that `admin` in tenant A and `admin` in tenant B get distinct workspaces and cannot see each other's data. None of the existing multitenancy ITs (`EnabledMultitenantModeIT`, `TenantDeterminationIT`, `MultitenancyIT`, `MultitenancyHarmoniaIT`, `BpmnMultitenancyIT`) exercises a same-username-in-two-tenants scenario, which is why the §9.3 collisions have gone unnoticed.
2. **Pin down whether `@RolesAllowed` is enforced under the cognito profile** (§6.1). Boot with `spring_profiles_active=cognito` and call a `@RolesAllowed`-only endpoint with a token holding no matching group. This gates the rest of the design.
3. **Probe the cross-subdomain session replay** — authenticate on `t1.localhost`, replay `JSESSIONID` against `t2.localhost` under the basic profile, and see whether t1's authorities are honoured in t2's tenant context.

Four findings in this document are **hypotheses, not confirmed facts**, and are not asserted anywhere else:

- That all 52 `@RolesAllowed` are inert outside the basic profile (§6.1) — the wiring reads that way; not executed.
- Whether HTTP Basic re-invokes `loadUserByUsername` per request in this configuration, or restores a session context. No `SecurityContextRepository` is configured explicitly and `formLogin` is also enabled, so a `JSESSIONID` normally exists.
- Whether the cross-subdomain session replay above is actually exploitable.
- Whether `Role.name` global uniqueness is enforced anywhere, or merely implied by single-result queries.

AWS-side items to confirm in your own account:

- The **Essentials or Plus** feature plan requirement for pre-token-generation trigger V2_0/V3_0, and its MAU pricing impact.
- **App clients per user pool** — 1,000 default, 10,000 maximum — as the tenants-per-pool ceiling; and **identity providers per user pool** (300 / 1,000) as the federated-tenant ceiling.
- **Federated-user session-cookie behaviour.** AWS's cross-app-client SSO statement is worded for *local* users; a user who signs in through an external IdP has their session at that IdP, so cross-tenant navigation may behave differently. Test it before promising seamless switching to federated tenants.
- Cognito pricing at your expected MAU, including the separate M2M pricing for V3_0.
- The **"total combined changes in pre token generation Lambda trigger"** quota (5,000 claims + scopes per transaction) — not a practical constraint here, but worth knowing it exists.

---

## 11. References

### In-repo

| Concern | Path |
| --- | --- |
| Multitenancy behaviour reference | [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) |
| As-is AWS deployment analysis | [`AWS_MULTITENANCY_RESEARCH.md`](AWS_MULTITENANCY_RESEARCH.md) |
| The identity model and its constraint | `components/core/core-tenants/.../domain/User.java`, `domain/UserRoleAssignment.java` |
| The seam where the tenant is discarded | `components/core/core-tenants/.../security/CustomUserDetailsService.java` |
| Basic auth chain + the only `jsr250Enabled` | `components/core/core-tenants/.../security/BasicSecurityConfig.java` |
| Cognito chain, authority mapper, membership filter, login controller | `components/security/security-cognito/` |
| Keycloak equivalents (byte-identical filter) | `components/security/security-keycloak/` |
| Per-tenant OAuth client registrations | `components/security/security-client-registration/` |
| M2M scope → role | `components/engine/engine-security/.../oauth/ScopeRoleJwtAuthoritiesConverter.java` |
| Authority string format (single choke point) | `components/core/core-base/.../util/AuthoritiesUtil.java` |
| URL-pattern enforcement + unused SPI seam | `components/core/core-base/.../http/access/HttpSecurityURIConfigurator.java`, `CustomSecurityConfigurator.java` |
| Role check bottom of stack + unused SPI seam | `components/core/core-base/.../http/access/UserRequestVerifier.java`, `UserAccessVerifier.java` |
| `.access` model, cache and filter | `components/engine/engine-security/.../domain/Access.java`, `verifier/AccessVerifier.java`, `filter/SecurityFilter.java` |
| Current-user API (no tenant) | `components/api/api-security/.../UserFacade.java`, `api-modules-javascript/.../modules/src/security/user.ts` |
| The client-Java isolation gap | `components/engine/engine-java/.../runtime/JavaCompiledOutputDirectory.java` |
| Audit stub | `components/core/core-base/.../artefact/AuditorAwareHandler.java` |
| The one correct tenant+user pattern | `components/engine/engine-bpm-flowable/.../config/TaskServiceImpl.java` |

### AWS documentation

- [Multi-tenant application best practices](https://docs.aws.amazon.com/cognito/latest/developerguide/multi-tenant-application-best-practices.html) — the shared-pool advantages and the cross-app-client session cookie.
- [User-pool multi-tenancy](https://docs.aws.amazon.com/cognito/latest/developerguide/bp_user-pool-based-multi-tenancy.html), [app-client](https://docs.aws.amazon.com/cognito/latest/developerguide/application-client-based-multi-tenancy.html), [user group](https://docs.aws.amazon.com/cognito/latest/developerguide/group-based-multi-tenancy.html), [custom attribute](https://docs.aws.amazon.com/cognito/latest/developerguide/custom-attribute-based-multi-tenancy.html), [custom scope](https://docs.aws.amazon.com/cognito/latest/developerguide/scope-based-multi-tenancy.html) — the five models and their trade-offs.
- [Pre token generation Lambda trigger](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html) — trigger versions, feature-plan requirements, the modifiable-claims table, `groupsToOverride`.
- [Quotas in Amazon Cognito](https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html) — app clients per pool, groups per user, identity providers per pool, token validity.
- [Multi-tenancy security recommendations](https://docs.aws.amazon.com/cognito/latest/developerguide/multi-tenancy-security-recommendations.html)
