# Multitenant Dirigible on AWS — Single Host with an In-App Tenant Picker

**Status:** architecture proposal. Nothing here is deployed or implemented.
**Date:** 2026-07-29
**Companion:** [`AWS_DEPLOYMENT_PROPOSAL.md`](AWS_DEPLOYMENT_PROPOSAL.md) — the subdomain-per-tenant variant of the same one-unit deployment. Everything below the identity layer (compute, database, storage, hardening, operations) is inherited from it unchanged; this document specifies only what the changed requirements alter, and §8 weighs the two models honestly.

---

## 0. The changed requirements, and what they force

| Requirement | Architectural consequence |
| --- | --- |
| Same credentials in every tenant the user belongs to, different roles in each | One identity store (one Cognito user pool) — unchanged |
| The user logs in **once**, then picks/changes the current tenant **in the UI** | The tenant is chosen *after* authentication and can change *without* re-authentication ⇒ the token cannot be tenant-scoped at issuance; roles for **all** memberships must be available to the session, filtered when the tenant is picked |
| **No dedicated subdomain per tenant** | The `Host` header carries no tenant signal ⇒ Dirigible's host-based `TenantExtractor` model is unusable and must be replaced in the fork ⇒ the active tenant becomes **server-side session state** |

The third point is the deep one. Today the platform is *host-native multitenant*: `TenantExtractor` regex-matches the subdomain, and — crucially — the per-host `JSESSIONID` gives every tenant its own session and its own login-time authority snapshot for free. Removing subdomains removes both, so the fork must supply (a) a new per-request tenant source and (b) a way to change the session's authorities when the picker changes the tenant.

What does **not** change: `TenantContext` is a clean thread-local SPI (`components/core/core-base/.../tenant/TenantContext.java`), and everything tenant-aware — datasource-name routing, per-tenant synchronizer replay, CMS path prefixing, Quartz/JMS naming, Flowable — consumes it, not the HTTP host. Replace the *resolver* that feeds the context and the entire isolation machinery keeps working untouched.

### Decisions at a glance

| Concern | Decision | Why |
| --- | --- | --- |
| Application host | One host, e.g. `app.example.com` — single Route 53 record, single ACM cert (no wildcard) | Requirement |
| Active-tenant carrier | **HTTP session attribute**, set by a picker endpoint; `X-Dirigible-Tenant` header for API/M2M callers | The only carrier that works for browser top-level navigation; header where clients can set headers |
| No tenant selected | Redirect to the **tenant picker page** (auto-select when the user has exactly one membership); never fall through to the default tenant | Silent default-tenant fallback would read/write the wrong tenant's data |
| Identity | One Cognito user pool | Same-credentials requirement |
| OAuth app clients | **ONE confidential app client** (single callback `https://app.example.com/login/oauth2/code/cognito`) | One host = one callback; the per-tenant-client rationale from the subdomain proposal (per-tenant callbacks, tenant signal at mint time) evaporates — see §2 |
| Membership & roles | Cognito groups `t:<tenantId>:<role>` — same model as the subdomain proposal | One source of truth in the IdP; the full set arrives in the token and is filtered app-side per pick |
| Pre-token Lambda | **Not required** (optional refinement, §2.3) | With one client there is no per-tenant token to mint; prefixed groups pass through as-is — Cognito **Lite** plan may suffice |
| Roles at runtime | Authorities **rebuilt on every tenant switch** from the session's membership snapshot; one active tenant per browser session | §3 — the load-bearing fork mechanism |
| Everything below identity | Inherited unchanged from `AWS_DEPLOYMENT_PROPOSAL.md` | ECS Fargate desiredCount=1, RDS Multi-AZ schema-per-tenant, S3 CMS, baked-in content, hardening, ops |

---

## 1. Topology — what changes on AWS (almost nothing)

```
                Route 53   app.example.com ──alias──┐        auth.example.com (Cognito managed login)
                                                    │
        ┌───────────────────────────────────────────▼────────────────────────────┐
        │  AWS WAF  →  ALB (HTTPS, ACM app.example.com)                          │
        │  health check: /services/core/healthcheck                              │
        └───────────────────────────────┬────────────────────────────────────────┘
                                        │  session cookie + activeTenant (server-side)
        ════ private / isolated ════════▼══════════════════════════════════════════
        ┌───────────────────────────┐       ┌────────────────────────────────────┐
        │ ECS Fargate               │       │ RDS PostgreSQL Multi-AZ            │
        │ dirigible-runtime         │──────▶│  SystemDB · DefaultDB              │
        │ desiredCount = 1          │       │  (schema + DB user per tenant)     │
        │ stop-then-start deploys   │       └────────────────────────────────────┘
        │ content baked in          │       ┌────────────────────────────────────┐
        └────────────┬──────────────┘──────▶│ S3 CMS  ·  Secrets Mgr · CloudWatch│
                     │ OIDC (ONE app client)└────────────────────────────────────┘
        ┌────────────▼───────────────────────────────────────────────────────────┐
        │ Amazon Cognito — one user pool (Lite plan may suffice)                 │
        │   ONE app client "dirigible" — callback app.example.com/login/…        │
        │   groups: t:acme:manager · t:acme:ADMINISTRATOR · t:globex:viewer · …  │
        │   [optional] pre-token Lambda — claim compaction / store-backed roles  │
        └─────────────────────────────────────────────────────────────────────────┘
```

Deltas from the subdomain proposal, all simplifications:

- **DNS/TLS:** one A/alias record, one plain certificate. No wildcard anything.
- **Cognito:** one app client, one secret, one callback. No per-tenant clients, no clientId→tenant DynamoDB map, and no pre-token Lambda in the baseline — which means the **Lite** feature plan may be enough (verify; the subdomain design required Essentials for trigger V2_0).
- **Dropped fork components:** the host-keyed `ClientRegistrationRepository` (fix 7 there) is unnecessary — the shipped single static registration in `application-cognito.properties` is exactly right, with its `redirect-uri` pointed at the one host. The per-tenant logout fix is unnecessary — the shipped single-client `CognitoLogoutSuccessHandler` is correct as-is. `CognitoLoginController`'s `/login/{subdomain}` entry point goes unused.
- Unchanged and still binding: the single-writer compute constraint, the Hikari pool-sizing fix, the schema-per-tenant provisioning flow, S3 CMS, all of the hardening checklist, backup/observability/cost (§9 of the companion — subtract the wildcard cert and the Lambda; the numbers barely move).

---

## 2. Identity — one client, multi-tenant tokens, roles filtered at pick time

### 2.1 Why one app client is now the right answer

The subdomain proposal argued hard for one client per tenant (its §4.2). Every leg of that argument depended on per-tenant hosts, and each one falls with them:

| Per-tenant-client argument (subdomain model) | Under a single host |
| --- | --- |
| Per-tenant callback URLs (100-URL cap, no wildcards) | There is exactly **one** callback URL — the cap is irrelevant |
| The app client is the Lambda's tenant signal, enabling tenant-scoped tokens | The user picks the tenant **after** the token is minted and switches without re-auth — no issuance-time scoping is possible in principle, client topology can't buy it back |
| Deny non-members at the IdP | Membership denial moves app-side by necessity (the IdP can't know which tenant the user will pick) |
| Per-tenant IdP federation (per-client setting) | Lost either way on one host — federation selection would need a home-realm-discovery step, not client topology (§8) |
| Per-tenant secret blast radius | One host, one app — one secret is the natural shape |

So: **one confidential app client**, the shipped static registration, standard `authorization_code` flow. Per-tenant **M2M** app clients remain available if needed (client-credentials has no callbacks and each M2M client can carry its tenant in its scopes) — see §5.

### 2.2 The token contract

| Claim | Content | Consumed by |
| --- | --- | --- |
| `sub` | Global identity of the person | identity key |
| `email` | Sign-in / display identity | `user-name-attribute` |
| `cognito:groups` | **All** memberships as prefixed groups: `["t:acme:manager", "t:acme:ADMINISTRATOR", "t:globex:viewer"]` | Parsed **once at login** into a membership map `{acme: [manager, ADMINISTRATOR], globex: [viewer]}` held on the principal; **no authorities are granted from it directly** |
| `scope` (M2M only) | `<tenantId>/<role>` per-tenant scopes | membership + roles for client-credentials callers |

The 100-groups-per-user Cognito hard limit applies to the *person's total* tenant-role pairs — same watchpoint as the subdomain design, unchanged.

### 2.3 Optional pre-token Lambda

The baseline needs none — prefixed groups flow through untouched. Add a V2_0 Lambda later only for: compacting a large group list into a JSON claim, sourcing roles from an external membership store (the research branch's DynamoDB model), or filtering what enters the token. It is an evolution, not a prerequisite — and adding it is what moves the pool from Lite to Essentials.

---

## 3. The tenant-picker mechanism (the heart of the fork change)

### 3.1 Session model

- After OIDC login the session holds an authenticated principal with the **membership map** and **zero tenant roles**. `activeTenant` is unset.
- A request with no `activeTenant` is redirected to the **picker page** (static assets and the picker/auth endpoints excepted). A user with exactly one membership is auto-selected. **No request ever silently runs against the default tenant** — with host-resolution gone, the shipped fallback-to-default behaviour becomes a data-integrity bug and must be removed for authenticated traffic.
- `POST /services/core/security/tenants/current { "tenant": "acme" }`:
  1. Validate `acme` is in the principal's membership map — 403 otherwise.
  2. Validate the tenant row exists and is `PROVISIONED` — 409 otherwise.
  3. Store `activeTenant = acme` in the session.
  4. **Rebuild the authorities**: replace the session's `Authentication` with one carrying the same principal and exactly `ROLE_<r>` for the map's `acme` roles (the mapper seam already exists: `CognitoSecurityConfiguration.userAuthoritiesMapper()`).
  5. The UI performs a **full reload** — every iframe, cached list and open perspective re-renders under the new tenant.
- `GET /services/core/security/tenants/mine` returns the membership map (+ display names) — the data source for the picker page and the shell dropdown.

### 3.2 Request → tenant resolution (replacing `TenantExtractor`)

A new resolver feeds the existing `TenantContextInitFilter`/`TenantContext`, in order:

1. **Session attribute** `activeTenant` — the browser path.
2. **`X-Dirigible-Tenant` header** — API and M2M callers (validated against the caller's memberships / M2M scopes; 400 when absent, 403 when not a member).
3. Neither ⇒ no tenant context: UI requests go to the picker; service requests are rejected.

Everything downstream — `TenantDataSourceNameManager`'s `<tenantId>_DefaultDB` routing, CMS prefixing, job/queue naming, the per-tenant synchronizer replay, `executeForEachTenant` — is untouched: it consumes `TenantContext`, not the host.

### 3.3 Login and switch, end to end

```
1  GET https://app.example.com/              → no session
2  → /oauth2/authorization/cognito (single registration) → Cognito managed login
3  token: cognito:groups = [t:acme:manager, t:globex:viewer]
4  fork mapper: membership map on principal, NO roles yet → tenant picker page
5  user picks Acme → POST /services/core/security/tenants/current
      member? yes → session activeTenant=acme, authorities=[ROLE_manager] → reload
6  every request now runs in TenantContext(acme) → ACME schema, ACME documents, …
7  shell dropdown: "Globex" → same endpoint → authorities=[ROLE_viewer] → reload
      no re-authentication, different roles            ← the requirement
8  a user who belongs to no tenant: empty map → "no tenants" page, zero authorities
```

### 3.4 The one behavioural cost to state up front

**One browser session has one active tenant at a time.** Two tabs share the `JSESSIONID`; switching the tenant in tab A silently switches tab B's context on its next request. The subdomain model gave concurrent multi-tenant tabs for free (per-host cookies); this model **cannot**, short of moving the tenant into every URL or header of the UI. Mitigations, in increasing cost:

- The shell shows the active tenant persistently (name + colour chip) and the switch reloads the whole UI — stale tabs re-render on next interaction. *(baseline)*
- A session-version cookie checked by the shell; a mismatch banner — "tenant changed in another tab" — forces reload before further writes. *(recommended)*
- True concurrent tabs require per-request tenancy in the URL (path prefix `/t/{tenant}/…`), examined and rejected in §8.

Every write API already runs server-side under the session's `activeTenant`, so the failure mode is a *confusing read* or a *write to the tenant the user actually switched to* — never a write crossing a tenant the user doesn't belong to. State it in the UX anyway.

### 3.5 Security notes specific to this model

- **The switch endpoint is state-changing: it must be CSRF-protected.** The platform disables CSRF globally (`BasicSecurityConfig`); at minimum enforce `SameSite=Lax` on the session cookie, require the custom `X-Requested-With` header on the switch call, and prefer re-enabling CSRF for this endpoint. A forced tenant switch is not privilege escalation (memberships are still enforced) but is an integrity nuisance.
- **Rotate the session id on switch** (`changeSessionId()`) — cheap fixation hygiene when authority sets change.
- Membership/role changes take effect at the **next login** in the baseline (the map is snapshotted from the login-time token). Optional freshness: the switch endpoint re-queries Cognito's `userInfo` with the stored access token and rebuilds the map — then a role change lands at the next *switch*, not the next login. The immediate-eject runbook (global sign-out + session invalidation) is unchanged.
- A stolen bearer/refresh token now represents **all** the victim's tenants (no audience binding exists to assert) — inherent to the picker requirement, not to a topology choice. Compensate with short access-token lifetime (60 min), refresh-token rotation, and WAF rate rules.

---

## 4. Fork changes

**Carried over unchanged from the subdomain proposal (§6 there):** configurable Hikari pool sizing *(must fix — capacity)*, `@EnableMethodSecurity(jsr250Enabled=true)` under the cognito profile *(must fix — verify-then-enable)*, trial-flag fail-fast, provisioning idempotency + `FAILED` status, gating the cross-tenant admin endpoints, non-root Dockerfile.

**Dropped (no longer needed):** host-keyed `ClientRegistrationRepository`; per-tenant logout handler; tenant-cache sizing (no per-request subdomain lookups — resolution is a session read); `CognitoTenantFilter`'s host-vs-claim assertion in its subdomain form.

**New — the single-host mechanism:**

| # | Change | Where | Note |
| --- | --- | --- | --- |
| S1 | **Session/header tenant resolver** replacing host extraction; no-default-fallback for authenticated traffic; picker redirect | `core-tenants` (`TenantExtractor`, `TenantContextInitFilter`) | The resolver is the only thing that changes — `TenantContext` consumers are untouched |
| S2 | **Membership-map principal + neutral login mapper** — parse `t:<t>:<r>` groups into a map; grant no authorities at login | `security-cognito` (`userAuthoritiesMapper()`) | The seam exists at `CognitoSecurityConfiguration.java:127` |
| S3 | **Picker endpoints + authority rebuild on switch** — `GET …/tenants/mine`, `POST …/tenants/current` (validate membership + PROVISIONED, swap `Authentication`, rotate session id) | new, `core-tenants` | The heart of the feature; CSRF-protect (§3.5) |
| S4 | **Tenant picker UI** — initial picker page + persistent shell dropdown with the active-tenant chip; full reload on switch | Harmonia application shell (`resources-application`) + platform shell | Data-driven from `…/tenants/mine` |
| S5 | **M2M tenancy** — `X-Dirigible-Tenant` header resolved for `JwtAuthenticationToken` callers, asserted against per-tenant scopes | `security-cognito` filter + `ScopeRoleJwtAuthoritiesConverter` surroundings | Closes the same M2M gap the subdomain proposal's fix 2 closed, in header form |
| S6 | **Stale-tab guard** — session-version cookie + shell banner | shell JS | §3.4, recommended tier |

This is **more fork surface than the subdomain model** (which got per-tenant sessions and login-time scoping free from per-host cookies). That is the engineering price of the picker requirement, and it should be named in any comparison — see §8.

---

## 5. Machine-to-machine access

One per-tenant M2M app client (client-credentials) with per-tenant resource-server scopes (`<tenantId>/<role>`), exactly as in the subdomain proposal — M2M has no callback constraints, so nothing changes on the Cognito side. The caller sends `X-Dirigible-Tenant: <tenantId>`; the fork asserts the header's tenant matches the token's scope prefix (S5). A single shared M2M client is possible but reintroduces "one secret, all tenants" for machines — keep M2M per-tenant.

---

## 6. Tenant onboarding (simpler than the subdomain runbook)

```
1. Dirigible: POST /services/core/security/tenants { name, subdomain }   → INITIAL
      (subdomain remains a stored, unique tenant identifier — it just no
       longer appears in any URL; it is the value the picker and the
       X-Dirigible-Tenant header use)
2. Wait for PROVISIONED  (provisioning interval 60 s; alarm INITIAL > 15 min)
3. Cognito: create groups t:<tenantId>:<role>; add/invite the first users
      — MANDATORY: the platform creates no user for a new tenant
4. [only if the tenant needs API access] Cognito: resource server + M2M client
5. Smoke test: log in as the invited user → tenant appears in the picker →
      switch → correct roles
```

No DNS, no certificate, no app client, no callback registration, no secret distribution per tenant. Onboarding shrinks to *provision the schema + create the groups*. Offboarding likewise loses the Cognito-client steps; delete the tenant's groups (users lose it from their picker at next login/switch) and follow the companion's data-cleanup runbook.

---

## 7. Login & switch — sequence (for the visualization)

Actors: Browser · Dirigible (app.example.com) · Cognito (one pool, one client).

1. `GET /` — no session → redirect to `/oauth2/authorization/cognito`.
2. Cognito managed login: credentials + MFA (one client, one callback).
3. Callback: token with `cognito:groups = [t:acme:manager, t:globex:viewer]` → fork mapper builds the membership map, grants **no** roles → **tenant picker** (Acme · Globex).
4. Pick **Acme** → `POST /services/core/security/tenants/current` → validate → session `activeTenant=acme`, authorities `[ROLE_manager]`, session id rotated → full reload → all requests run in `TenantContext(acme)` → `ACME` schema.
5. Later, shell dropdown → **Globex** → same endpoint → authorities `[ROLE_viewer]` → reload. **No re-authentication; different roles.**
6. Non-member tenant never appears in the picker, and a forged switch call 403s server-side.

---

## 8. Honest comparison with the subdomain model — and one rejected alternative

| Dimension | Subdomains (`AWS_DEPLOYMENT_PROPOSAL.md`) | Single host + picker (this doc) |
| --- | --- | --- |
| Login UX | One sign-in per tenant hop (silent after the first) | **One sign-in, in-app switching** — the stated requirement |
| Concurrent tabs in different tenants | **Yes** — per-host sessions | No — one active tenant per browser session (§3.4) |
| Token scoping | Tenant-scoped at issuance; audience binding; IdP-side denial | Multi-tenant token; app-side denial only; stolen token spans the victim's tenants |
| Deep links / bookmarks | Carry the tenant in the host | Tenant-less; land in whatever tenant is active |
| Cognito surface | N app clients + Lambda + client map + Essentials plan | **One client, no Lambda, Lite plan may suffice** |
| DNS/TLS | Wildcard cert + wildcard DNS | One record, one plain cert |
| Fork surface (identity layer) | Small — per-host sessions do the heavy lifting | **Larger** — S1–S6: resolver, neutral mapper, switch endpoint, picker UI, M2M header, tab guard |
| Per-tenant enterprise federation later | Per-app-client setting, clean | Needs home-realm discovery (an email-domain → IdP hint step at login) — doable, not free |
| Tenant onboarding | Cognito client + secret + registration per tenant | Groups only — the simplest onboarding of any variant |

**Rejected alternative — path-based tenancy (`/t/{tenant}/…`).** It would restore per-request tenant signal (fixing concurrent tabs and deep links) while keeping one host. Rejected because Dirigible's URL space is absolute-path-native everywhere — `/services/**`, `/public/**`, `/webjars/**`, WebSocket endpoints, generated UI assets, OAuth callback paths — so a tenant path prefix means either a rewriting reverse-proxy layer with cookie-path juggling or invasive URL surgery across the platform and every generated application. That is an order of magnitude more fork than S1–S6 for a UX refinement the picker requirement doesn't demand. Revisit only if concurrent multi-tenant tabs become a hard requirement.

---

## 9. Hypotheses to verify before go-live

Inherited from the companion (still valid here): `@RolesAllowed` inert under the cognito profile; RDS master credential can run the provisioner's DDL; boot + first-sync duration at ~100 tenants; CSRF re-enablement compatibility. New to this model:

1. **Authority rebuild on a live session** — replacing the `Authentication` in the security context + session propagates to subsequent requests in this Spring Security version (integration test: login → pick A → assert `ROLE_manager` → switch B → assert `ROLE_viewer` and *not* `ROLE_manager`).
2. **No residual host-based fallback:** with the resolver replaced, verify no code path still consults `TenantExtractor`'s regex or falls back to the default tenant for authenticated traffic (grep + an IT hitting the app with no tenant selected).
3. **Group-claim size:** a user with the maximum realistic memberships fits the ID-token/JWT header limits comfortably (arithmetic says yes; verify with a real pool).
4. **Cognito Lite vs Essentials:** confirm the no-Lambda baseline works on Lite pricing for your MAU; the Lambda upgrade path forces Essentials.
5. **Session semantics under the shipped UI:** the full-reload-on-switch leaves no stale per-tenant state in shell/iframe caches (exercise the deepest generated app view before and after a switch).
