# Horizontal Scaling for the Tenant-Picker Model — Spring Session on Redis

**Status:** design note. Nothing here is implemented.
**Date:** 2026-07-29
**Scope:** how the single-host tenant-picker model behaves on more than one runtime instance, and what externalising the HTTP session into Redis does and does not buy.
**Companion:** [`AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md`](AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md) — the picker design itself; its §10 is the summary this document expands. See also [`AWS_DEPLOYMENT_PROPOSAL.md`](AWS_DEPLOYMENT_PROPOSAL.md) (the subdomain variant) and [`AWS_TENANCY_MODELS_COMPARISON.md`](AWS_TENANCY_MODELS_COMPARISON.md).

**Contents**

- [1. The claim, stated precisely](#1-the-claim-stated-precisely)
- [2. Why the picker model is instance-bound today](#2-why-the-picker-model-is-instance-bound-today)
- [3. What is actually in the session](#3-what-is-actually-in-the-session)
- [4. The mechanism: what Spring Session changes](#4-the-mechanism-what-spring-session-changes)
- [5. Serialization — the one real design constraint](#5-serialization--the-one-real-design-constraint)
- [6. AWS topology and configuration](#6-aws-topology-and-configuration)
- [7. What changes in the picker flows](#7-what-changes-in-the-picker-flows)
- [8. What Redis does not solve](#8-what-redis-does-not-solve)
- [9. Alternative: Spring Session on JDBC](#9-alternative-spring-session-on-jdbc)
- [10. Rollout](#10-rollout)
- [11. Operations and failure modes](#11-operations-and-failure-modes)
- [12. Verification](#12-verification)

---

## 1. The claim, stated precisely

> **Externalising the session makes the tenant-picker design horizontally scalable — and on its own it does not make the platform horizontally scalable.**

Both halves matter.

The picker model keeps the active tenant and the caller's rebuilt authorities in the HTTP session (fork change S3). That is the one part of the design that is *inherently* instance-bound, and it is the only picker-specific blocker to running more than one runtime task. Spring Session on Redis removes it completely, and the picker design survives unchanged: the switch endpoint keeps its validate → store → rebuild-authorities semantics, and only *where the session physically lives* moves.

The platform, however, carries five further blockers that have nothing to do with the picker (§8). Until those are paid down, a second instance still cannot boot. **So this change is not the thing that unlocks scale-out — it is the thing that keeps the picker design from becoming the blocker once the others are cleared.**

It is worth doing anyway, immediately, at one instance: it makes sessions survive deploys and task replacement, so users stop being logged out by every release. That benefit is independent of everything else in this document.

---

## 2. Why the picker model is instance-bound today

Under the cognito profile the session policy is `SessionCreationPolicy.ALWAYS` (`CognitoSecurityConfiguration.java:86`), so every authenticated request has a session. That session lives in the Tomcat heap of whichever task created it — there is no `spring-session` on the classpath anywhere in the repository.

With one task that is invisible. With two:

```
   ┌── request A ──▶ Task 1   session {activeTenant=acme, authorities=[ROLE_manager]}  ✔
   │
ALB┤
   │
   └── request B ──▶ Task 2   same cookie, no such session in this heap                ✘
                              → unauthenticated → bounced to login → picker again
```

The user's requests land on whichever task the load balancer picks, so roughly half of them find no session. **ALB stickiness is a stopgap, not a fix**: it papers over the problem while both tasks are healthy, and loses every session pinned to a task the moment that task is replaced — which is exactly what a deploy, a scale-in event, or an AZ failure does.

Note also that `build/application/src/main/resources/application.properties` already sets `spring.session.timeout=8h` (twice — lines 15 and 18, a duplicate worth tidying). That property is **inert today** because Spring Session is not on the classpath; it becomes live the moment the dependency is added, and an 8-hour session TTL is a deliberate sizing decision (§6), not a default to inherit unexamined.

---

## 3. What is actually in the session

Before moving a session into a shared store, you must know precisely what has to survive the trip. For this codebase the surface is small and — importantly — **bounded by code, not by convention**:

| Attribute | Type | Written by | Serialization risk |
| --- | --- | --- | --- |
| `SPRING_SECURITY_CONTEXT` | `SecurityContext` → `Authentication` → the principal (membership map) + granted authorities | Spring Security, rebuilt by the switch endpoint | **The only one that needs design care** (§5) |
| `activeTenant` | `String` | The fork's switch endpoint (S3) | none |
| `invocation.count` | `Integer` | `HttpSessionFacade.getSession()` — incremented on **every** access | none, but see write amplification below |
| user-code attributes | **`String` only** | user JS/TS via `HttpSessionFacade` | none |

The last row is the load-bearing finding. Dirigible exposes the HTTP session to user code through `components/api/api-http/.../HttpSessionFacade.java`, which at first glance looks like an open door for arbitrary objects. It is not — the setter is:

```java
public static final void setAttribute(String arg0, String arg1)
```

**String keys, String values.** User code cannot put a non-serializable object into the session, so no amount of user JS/TS can break the shared store. That converts session externalisation from "audit every project ever written on this platform" into "get one principal class right."

**One caveat from the same class:** `HttpSessionFacade.getSession()` calls `request.getSession(true)` and increments `invocation.count` on *every* invocation. So any JS/TS API call that touches the session dirties it, and under Spring Session a dirtied session is written back to the store. Session-heavy user code therefore produces one store write per request — a strong argument for Redis over a relational store (§9).

---

## 4. The mechanism: what Spring Session changes

Spring Session does not ask the application to change how it uses `HttpSession`. It installs a `SessionRepositoryFilter` very early in the chain that swaps the container's session implementation for one backed by Redis. Everything downstream — Spring Security's `SecurityContextRepository`, the fork's `activeTenant` attribute, `HttpSessionFacade` — keeps calling the same servlet API.

```
request  ─▶ SessionRepositoryFilter ─── read by session id ──▶ Redis
                     │                                           │
                     │  HttpSession now backed by the store      │
                     ▼                                           │
            Security filters  (SecurityContext read from the session attribute)
                     ▼
            TenantContextInitFilter  →  reads activeTenant  →  TenantContext(acme)
                     ▼
            controller / JS engine   (may touch HttpSessionFacade)
                     ▼
            SessionRepositoryFilter ─── write back at end of request ──▶ Redis
```

Two consequences to plan for rather than discover:

- **The cookie name changes.** Spring Session issues its own `SESSION` cookie (a base64-encoded session id) instead of `JSESSIONID`. Anything asserting on `JSESSIONID` — integration tests, the stale-tab guard (S6), operational runbooks, any reverse-proxy rule — must be updated. The name is configurable if keeping `JSESSIONID` is worth more than convention.
- **Write timing is a knob.** `spring.session.redis.flush-mode` defaults to `ON_SAVE` (write once, at end of request); `IMMEDIATE` writes as soon as an attribute changes. `save-mode` defaults to `ON_SET_ATTRIBUTE` (only changed attributes). These two settings are exactly the lever for the propagation window in §7.

**A bonus worth taking:** using `RedisIndexedSessionRepository` (rather than the simpler default repository) maintains a principal-name index, which makes `findByIndexNameAndIndexValue` able to enumerate and delete *all* sessions for one user. That turns the "eject a user immediately" step — listed as a manual runbook item in both identity proposals — into an implementable operation: revoke membership, call Cognito global sign-out, and delete that user's sessions from the store in one go. Indexed mode costs extra Redis keys and needs keyspace notifications enabled for expiry events.

---

## 5. Serialization — the one real design constraint

By default Spring Session serialises attributes with **JDK serialization**, so everything reachable from a session attribute must be `Serializable`. Given §3, that reduces to one object: the `Authentication` and its principal.

**The design rule for the fork:** the membership-map principal introduced by change S2 must be a plain, self-contained value object from day one —

- fields limited to the identity key (the Cognito `sub`, a `String`), a display name, and the membership map (`Map<String, List<String>>` of tenant → role names);
- no references to Spring beans, JPA entities, repositories, `EntityManager`, or anything holding a database handle;
- an explicit `serialVersionUID`;
- immutable, with defensive copies of the map.

This is cheap if designed in and expensive if retrofitted, which is the reason to write it down before S2 is implemented rather than after.

**Rolling deploys and class evolution.** If the principal class changes shape between releases, sessions written by the old version fail to deserialize in the new one. Two honest mitigations: keep the class strictly backward-compatible (add fields, never remove or retype, keep the `serialVersionUID`), or accept that a deploy flushes the session store. The second is entirely reasonable here — the platform already deploys stop-then-start (§8), so a release is already a brief outage, and flushing sessions merely returns to today's behaviour where every deploy logs everyone out.

**JSON serialization — verify before choosing it.** The usual alternative to JDK serialization is a JSON serializer, which is version-tolerant and human-readable in the store. Spring Security ships Jackson mix-ins for its own types to make this work. **This fork is on Spring Boot 4.1.0, where the web layer uses Jackson 3 (`tools.jackson`) rather than Jackson 2 (`com.fasterxml`)** — the same split that has already bitten this codebase in controllers. Whether Spring Security's session mix-ins are available for the Jackson version in play is a compatibility question to answer by experiment, not assumption (§12). JDK serialization has no such dependency and is the safe default for a first implementation.

---

## 6. AWS topology and configuration

**Amazon ElastiCache**, in the private subnets, reachable only from the runtime tasks' security group:

```
        ┌──────────────── ECS service: dirigible-runtime ────────────────┐
        │   task 1              task 2              task N              │
        └──────┬───────────────────┬───────────────────┬────────────────┘
               │  read at start of request / write at end (TLS, AUTH)
               └───────────────────┴───────────────────┘
                                   ▼
                    ┌────────────────────────────────┐
                    │ ElastiCache (Redis / Valkey)   │
                    │ primary + replica, Multi-AZ    │
                    │ automatic failover             │
                    │ TLS in transit · encrypted     │
                    │ at rest · AUTH token in        │
                    │ Secrets Manager                │
                    │ maxmemory-policy: noeviction   │
                    └────────────────────────────────┘
```

**Sizing.** A session here is small — a principal with ~30 memberships, an active-tenant string, a counter and a few user strings is on the order of a few kilobytes. Ten thousand concurrent sessions is therefore tens of megabytes, so the smallest node class with a replica is ample; this is a latency and availability purchase, not a capacity one. Note the 8-hour timeout inherited from `application.properties` (§2) is what sets the working-set size — sessions linger for eight hours after last use, so the store holds far more than the concurrently active count.

**The setting that matters most.** `maxmemory-policy` must **not** be `allkeys-lru`. Under memory pressure that silently evicts live sessions and logs users out at random, with no error anywhere. Use `noeviction` (writes fail loudly, which is a page-able signal) or `volatile-lru` combined with TTLs, and size the node so it never gets there. **Alarm on `Evictions > 0`** — in a session store the correct value is always zero.

**Configuration sketch** (verify property names against the Spring Boot 4.1 / Spring Session versions actually resolved — §12):

```properties
spring.session.store-type=redis
spring.session.timeout=8h
spring.session.redis.namespace=dirigible:session
spring.session.redis.flush-mode=on_save
spring.session.redis.repository-type=indexed      # enables per-user session lookup (§4)

spring.data.redis.host=${DIRIGIBLE_SESSION_REDIS_HOST}
spring.data.redis.port=6379
spring.data.redis.ssl.enabled=true
spring.data.redis.password=${DIRIGIBLE_SESSION_REDIS_AUTH}   # from Secrets Manager
```

with `org.springframework.session:spring-session-data-redis` and `spring-boot-starter-data-redis` on the classpath. Per this repo's conventions the two environment variables belong in `DirigibleConfig`, not read ad hoc.

---

## 7. What changes in the picker flows

**Unchanged:** everything about the design's semantics. The picker page, `GET …/tenants/mine`, and the switch endpoint's validate → store `activeTenant` → rebuild authorities → rotate session id sequence all behave identically; `changeSessionId()` is supported by Spring Session. The tenant resolver (S1) still reads `activeTenant` from the session — it neither knows nor cares that the session is now in Redis. Membership is still enforced server-side on every switch.

**One new behaviour: a propagation window.** With `flush-mode=on_save`, the session is written back when the request completes. If a user switches tenant on instance 1 while another of their requests is already in flight on instance 2, that in-flight request finishes under the *previous* tenant and authorities.

This is bounded and benign, but should be stated rather than discovered:

- the window is the duration of one overlapping request;
- the stale request runs with authorities the user legitimately held moments earlier, in a tenant they are a member of — it is never an escalation, and never a cross-tenant leak to a tenant they don't belong to;
- the switch is followed by a full UI reload, so the browser is not issuing much concurrently;
- `flush-mode=immediate` narrows the window at the cost of an extra write per mutation.

The **stale-tab guard** (S6) is unaffected in design and becomes more useful: with a shared store, the session-version value is visible to every instance.

---

## 8. What Redis does not solve

Externalising the session clears exactly one of six items. The other five are platform-level and identical for the subdomain model:

| # | Blocker | Still blocking after Redis? |
| --- | --- | --- |
| 1 | Embedded ActiveMQ on hardcoded `vm://localhost` with a JDBC lock on SystemDB — the second task blocks at broker start and never becomes healthy | **Yes** — replace with Amazon MQ or move `.listener` work to SQS/SNS |
| 2 | Synchronizer effects are per-JVM while artefact checksums are shared — the second node sees "already current" and never materialises its own runtime state | **Yes** — needs per-node reconciliation or an unconditional boot replay |
| 3 | Boot-time DDL races (Quartz `initialize-schema=always` drops/recreates `QRTZ_*`, check-then-insert initializers) | **Yes** — schema init to `never`, initializers made concurrency-safe |
| 4 | Per-JVM caches with no invalidation channel (tenant lookup, tenant config, `.access` ACL) | **Yes** — though Redis pub/sub is then the obvious invalidation bus |
| 5 | Local filesystem repository + Lucene | **No** — already fine for *runtime-only* tasks with baked-in content; the authoring instance stays at one forever |
| 6 | **HTTP session in the Tomcat heap** | **No — this is what Redis fixes** |

Three further consequences of running N > 1 that Redis does not address:

- **WebSocket endpoints stay instance-bound.** Terminal, LSP, debug and data-transfer handlers attach to processes in one JVM; those paths still need stickiness regardless of where sessions live.
- **Connection pools multiply.** Pools are per *(instance, tenant)*, so N replicas is N× the idle and peak database connections — the configurable-pool-sizing fork fix moves from important to mandatory, and the database class must be re-sized for the product.
- **Unit routing is untouched.** The single-host model's inability to route tenants to different units from the edge (the picker proposal's §10.3) is a separate problem with separate answers.

**And Redis becomes a dependency in the authentication path.** If the store is unavailable, sessions cannot be read and every user appears logged out. Multi-AZ with automatic failover keeps that to a brief blip, but the failure mode must be an explicit decision — fail closed (users re-authenticate; correct and simple) rather than silently degrading to a tenant-less or default-tenant state, which would be a data-integrity bug of exactly the kind the picker design removes elsewhere.

---

## 9. Alternative: Spring Session on JDBC

`spring-session-jdbc` stores sessions in `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` tables in a database you already run. It deserves a fair hearing because it adds **no new infrastructure**: the deployment already has RDS PostgreSQL Multi-AZ, already backs it up, already secures and monitors it.

| | Redis (ElastiCache) | JDBC (existing RDS) |
| --- | --- | --- |
| New infrastructure | one managed cache cluster | **none** |
| Latency per request | sub-millisecond | a database round trip |
| Load added | isolated from application queries | lands on the instance already sized for per-tenant pools |
| Backup / HA | separate concern | inherits the RDS story |
| Per-user session lookup | `RedisIndexedSessionRepository` | SQL query — trivial |
| Expiry | native TTL | a cleanup job deletes expired rows |

**Recommendation: Redis**, and the deciding factor is specific to this codebase rather than generic preference. `HttpSessionFacade` dirties the session on *every* access (§3), so session-touching user code produces a store write per request. Sending that traffic to the same RDS instance that already carries every tenant's application queries — on a deployment whose documented capacity ceiling is database connections — is the wrong place to put it. Redis absorbs that pattern at a fraction of the cost and keeps the database sized for what it is for.

If a hard constraint forbids a new service, JDBC is a legitimate choice at this scale; measure the added query rate first, and revisit if session writes become visible in database load.

---

## 10. Rollout

The phases are independent, and the first delivers value on its own:

**Phase 1 — adopt at one instance (do this early).** Add the dependency and configuration; nothing else changes. Sessions immediately survive task replacement and deploys, so users stop being logged out by every release. This is a real quality-of-life improvement with no dependency on any of the five platform blockers, and it de-risks the rest by proving serialization in production at N = 1.

**Phase 2 — prove it.** Confirm the principal round-trips, measure the store write rate under real user code, size the node against the 8-hour timeout, alarm on evictions.

**Phase 3 — only then, N > 1.** Pay down blockers 1–4 (§8). Raise the replica count last, with stickiness retained on WebSocket paths and the database re-sized for multiplied pools.

Attempting Phase 3 before Phase 2 is how a session store becomes an incident; attempting it before blockers 1–4 simply does not work, because the second task never boots.

---

## 11. Operations and failure modes

- **Store unavailable** → treat as fail-closed: users re-authenticate. Never fall back to a default tenant.
- **Failover** → a short window of failed reads; the client retries or re-authenticates. Multi-AZ with automatic failover is not optional for a store in the authentication path.
- **TTL alignment** → `spring.session.timeout`, the session cookie `max-age`, and the store's own expiry must agree, or sessions die in one layer while another still advertises them. Note the existing duplicate `spring.session.timeout=8h` (§2) and decide the value deliberately.
- **Security** → TLS in transit, encryption at rest, AUTH token or Redis RBAC held in Secrets Manager, no public accessibility, security group limited to the task role's group. The store holds live authenticated sessions: compromise of it is compromise of every signed-in user.
- **Monitoring** → `Evictions` (must be zero), memory usage against `maxmemory`, replication lag, connection count, command latency, and the application-side session read/write rate.

---

## 12. Verification

1. **Two instances, one store.** Run two application instances locally against one Redis, sign in through instance 1, force the next request to instance 2, and confirm the session, the active tenant and the authorities are all intact.
2. **Kill test.** Mid-session, stop the instance that created the session; confirm the user continues uninterrupted on the survivor.
3. **Switch across instances.** Pick tenant A on instance 1, then switch to tenant B via instance 2, and assert the authorities are B's only — the same assertion the picker proposal already lists for the single-instance case.
4. **Serialization round-trip.** A test that writes the principal to the store and reads it back on a fresh context, plus a deliberate class-shape change to observe the failure mode you will get during a rolling deploy.
5. **The Jackson question** (§5): determine whether JSON session serialization is available and supported for the Jackson version resolved under Spring Boot 4.1.0, or whether JDK serialization is the only practical option. Decide before writing the principal class.
6. **Property names.** Confirm `spring.session.*` and `spring.data.redis.*` keys against the exact Spring Boot 4.1 and Spring Session versions the build resolves — these have been renamed across versions.
7. **Cookie rename fallout.** Grep the repository and the integration suite for `JSESSIONID` before switching, and decide whether to keep the name.
8. **Eviction policy.** Verify `maxmemory-policy` on the actual parameter group, not in the console description — this is the setting that fails silently.
