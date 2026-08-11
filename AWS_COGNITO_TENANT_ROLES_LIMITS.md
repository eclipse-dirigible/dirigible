# Storing Tenant Membership and Roles in Amazon Cognito — Limits and Options

**Status:** analysis. Standalone — this document changes nothing in the existing proposals; §9 records what it would imply for them.
**Date:** 2026-07-29
**Question it answers:** *a user may belong to 20–30 tenants, and each tenant may define ~10 roles, with different roles per tenant — how should that be stored in Cognito?*
**Related (unchanged by this document):** [`AWS_DEPLOYMENT_PROPOSAL.md`](AWS_DEPLOYMENT_PROPOSAL.md) · [`AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md`](AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md) · [`AWS_TENANCY_MODELS_COMPARISON.md`](AWS_TENANCY_MODELS_COMPARISON.md)

**Contents**

- [1. The short answer](#1-the-short-answer)
- [2. What counts against what — two different quotas](#2-what-counts-against-what--two-different-quotas)
- [3. Every limit that applies](#3-every-limit-that-applies)
- [4. Arithmetic of the current model](#4-arithmetic-of-the-current-model)
- [5. Why the failure mode is the real problem](#5-why-the-failure-mode-is-the-real-problem)
- [6. The options](#6-the-options)
- [7. Options at a glance](#7-options-at-a-glance)
- [8. Recommendation](#8-recommendation)
- [9. What this would imply for the existing proposals](#9-what-this-would-imply-for-the-existing-proposals)
- [10. Verify before committing](#10-verify-before-committing)

---

## 1. The short answer

The model in the current proposals — one Cognito group per (tenant, role), named `t:<tenant>:<role>` — **does not survive these numbers.**

> **Groups to which each user can belong: 100. Not adjustable.**

A user in 30 tenants can therefore hold an average of **at most 3 roles per tenant**, and that is at the ceiling with zero headroom. Four roles each is 120 and fails. The stated worst case — 10 roles in each of 30 tenants — is 300, which fails by 3×.

Three further limits push in the same direction even below the ceiling: the `cognito:groups` claim becomes multi-kilobyte, every membership write is throttled by a **non-adjustable 25 RPS account-wide** quota shared with ordinary user updates, and Cognito cannot answer "which users are in tenant X, with what roles?" without enumerating users.

**Cognito is an excellent identity provider and a poor membership database.** The recommendation (§8) is to keep authentication in Cognito and move the tenant ↔ user ↔ roles graph to a store that is designed for it.

---

## 2. What counts against what — two different quotas

The single most common confusion here is treating "roles per tenant" as one number. Two independent Cognito quotas are loaded by two different things:

| | Loaded by | Quota | Consequence |
| --- | --- | --- | --- |
| **Groups per user pool** | **Definitions** — every (tenant, role) pair that *exists*: `tenants × roles defined per tenant` | **10,000**, not adjustable | With 10 roles per tenant, the platform tops out at **~1,000 tenants**. Comfortable now, but it is a real platform ceiling |
| **Groups per user** | **Assignments** — every (tenant, role) pair one person *holds*: `tenants that user belongs to × roles held in each` | **100**, not adjustable | With 30 tenants per user, that is **~3 held roles per tenant, maximum**. This is the wall |

A tenant *defining* 10 roles is harmless. A user *holding* many of them across many tenants is what breaks. Everything in §4 follows from that distinction.

---

## 3. Every limit that applies

All values from the [Amazon Cognito quotas page](https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html). "Adjustable" means AWS can raise it on request.

### Storage-shaping quotas

| Quota | Value | Adjustable | Constrains |
| --- | --- | --- | --- |
| **Groups to which each user can belong** | **100** | **No** | Groups as membership storage — **the binding limit** |
| Groups per user pool | 10,000 | No | Total (tenants × roles defined) |
| Custom attributes per user pool | 50 | No | Attribute-based storage (how many you can shard across) |
| **Characters per attribute** | **2,048 bytes** | **No** | Attribute-based storage — the payload ceiling |
| Characters in custom attribute name | 20 | No | Attribute naming schemes |
| Scopes per resource server | 100 | No | Scope-based (M2M) role modelling |
| Scopes per app client | 50 | No | Scope-based (M2M) role modelling |
| Resource servers per user pool | 25 | Yes → 300 | One resource server per tenant (M2M) |
| App clients per user pool | 1,000 | Yes → 10,000 | One app client per tenant (subdomain model) |
| Callback / logout URLs per app client | 100 each | No | Shared-app-client topologies |
| Identity providers per user pool | 300 | Yes → 1,000 | Per-tenant federation |
| Users per user pool | 40,000,000 | Yes | Not a constraint here |
| Identities linked to a user | 5 | No | Federated identity linking |

### Token-shaping quotas

| Quota | Value | Adjustable | Note |
| --- | --- | --- | --- |
| Combined claim + scope changes in the pre-token-generation Lambda | 5,000 | Yes | A *count*, not bytes — 300 groups is numerically fine |
| ID token / access token validity | 5 min – 1 day | — | Sets the role-change propagation delay |
| Refresh token validity | 1 hour – 3,650 days | — | |
| Hosted-UI (managed login) session cookie | 1 hour | fixed | The cross-tenant SSO window |
| Token **size** | *not published* | — | No Cognito quota; the binding limits are downstream (browser cookies ~4 KB each; proxy/gateway header limits typically 8–16 KB). In Dirigible's server-side authorization-code flow the token does not ride in browser headers, which softens this — but it does not make a 9 KB claim wise |

Note that `cognito:groups` appears in **both** the ID token and the access token, so a fat groups claim is paid twice.

### Rate quotas that bite membership administration

These are **per AWS account, per Region — shared across all user pools in that account**.

| Category | Operations | Rate | Adjustable |
| --- | --- | --- | --- |
| **UserUpdate** | `AdminAddUserToGroup`, `AdminRemoveUserFromGroup`, `AdminUpdateUserAttributes`, `UpdateUserAttributes`, `AdminDisableUser`, `AdminUserGlobalSignOut`, … | **25 RPS** | **No** |
| UserResourceRead | `AdminListGroupsForUser`, … | 50 RPS | Yes |
| UserList | `ListUsers`, `ListUsersInGroup` | 30 RPS | No |
| UserPoolResourceUpdate | `CreateGroup`, `DeleteGroup`, `AddCustomAttributes`, `CreateResourceServer`, … | 15 RPS | No |

The critical one is **UserUpdate at 25 RPS, non-adjustable**: it covers *both* group assignment and custom-attribute writes, so it constrains every "store membership in Cognito" option equally — and it is shared with routine operations like password changes, MFA setup and forced sign-outs.

---

## 4. Arithmetic of the current model

Assumptions used throughout, matching the stated scale: **20–30 tenants per user**, **~10 roles defined per tenant**, group naming `t:<tenant>:<role>`.

### 4.1 Groups per user — the wall

`groups held = tenants × roles held per tenant`, against the hard ceiling of **100**.

| Roles held per tenant ↓ / Tenants per user → | 10 | 20 | **30** | 50 |
| --- | --- | --- | --- | --- |
| 1 | 10 ✅ | 20 ✅ | **30 ✅** | 50 ✅ |
| 2 | 20 ✅ | 40 ✅ | **60 ✅** | 100 ⚠️ at limit |
| 3 | 30 ✅ | 60 ✅ | **90 ⚠️ no headroom** | 150 ❌ |
| 4 | 40 ✅ | 80 ⚠️ | **120 ❌** | 200 ❌ |
| 5 | 50 ✅ | 100 ⚠️ at limit | **150 ❌** | 250 ❌ |
| 8 | 80 ⚠️ | 160 ❌ | **240 ❌** | 400 ❌ |
| 10 (all roles) | 100 ⚠️ at limit | 200 ❌ | **300 ❌ — 3× over** | 500 ❌ |

✅ ≤ 70 (≥30 % headroom) · ⚠️ 71–100 (no meaningful headroom) · ❌ > 100 (impossible)

**Reading of the target column:** at 30 tenants the model works only while users average **2–3 roles per tenant**, and there is no headroom left for a tenant that adds roles, a user who joins a 31st tenant, or an administrator who legitimately holds several roles.

### 4.2 Groups per user pool — the platform ceiling

`groups defined = tenants × roles defined per tenant`, against **10,000**.

| Tenants | × 10 roles | vs 10,000 |
| --- | --- | --- |
| 100 | 1,000 | ✅ 10 % used |
| 500 | 5,000 | ⚠️ 50 % used |
| **1,000** | **10,000** | ❌ **at the ceiling** |

So the group model also caps the *platform* at roughly 1,000 tenants at 10 roles each — far away, but worth knowing it exists and is not adjustable.

### 4.3 Claim size

A group name `t:<tenant>:<role>` with a ~10-character tenant id and a ~12-character role name is ~25 characters; inside a JSON array, with quotes and comma, ~28–30 bytes.

| Groups held | `cognito:groups` claim | JWT payload after base64url (≈ ×1.34) | Carried in |
| --- | --- | --- | --- |
| 30 | ~0.9 KB | ~1.2 KB | ID + access token |
| 90 | ~2.7 KB | ~3.6 KB | ID + access token |
| 300 | **~9 KB** | **~12 KB** | ID + access token (≈ 24 KB combined) |

No Cognito quota is breached (the pre-token Lambda's 5,000 limit is a *count*), and Dirigible's server-side code flow means these tokens never travel in browser headers. But a 12 KB token per request-issuance, twice, is a design smell — and it is fatal for any future consumer that puts a token in a cookie (4 KB) or through a gateway with an 8–10 KB header cap.

### 4.4 Membership administration throughput

Every grant or revoke is one `AdminAddUserToGroup` / `AdminRemoveUserFromGroup` call in the **UserUpdate** category: **25 RPS, non-adjustable, account-wide**.

| Operation | API calls | Time at 25 RPS (quota fully consumed) |
| --- | --- | --- |
| Onboard one user into 30 tenants × 3 roles | 90 | ~3.6 s |
| Onboard one power user into 30 tenants × 10 roles | 300 | ~12 s |
| Roll a new tenant out to 100 users × 5 roles | 500 | ~20 s |
| Initial migration: 1,000 users × 30 memberships | 30,000 | **~20 minutes** |

During those windows the same 25 RPS is unavailable for password changes, MFA enrolment, attribute updates and forced sign-outs across *every* user pool in the account. A relational or DynamoDB write path has no such shared ceiling and completes the same migration in seconds.

### 4.5 Reverse queries — "who is in tenant X?"

Every admin UI needs this, and it is where attribute-based storage collapses entirely.

| Storage | How you answer it | Practicality |
| --- | --- | --- |
| Groups | `ListUsersInGroup` per tenant-role group (10 calls per tenant), paginated, 30 RPS non-adjustable | Workable but slow; must be merged client-side to reconstruct per-user role sets |
| Custom attribute | `ListUsers` cannot filter on arbitrary substrings of a custom attribute — you enumerate the entire pool and decode each user | **Not practical** beyond a few thousand users |
| External store | One indexed query (SQL) or a GSI lookup (DynamoDB) | Trivial |

### 4.6 Propagation delay

| Storage | A role change takes effect | Immediate-revoke path |
| --- | --- | --- |
| Groups / attributes (claims) | At the **next token refresh** — up to the access-token lifetime (60 min typical), plus the application session | `AdminUserGlobalSignOut` + invalidate the app session |
| External store, read per login/switch/request | **Immediately** | Delete the row |

The claim-based options force a "role changes land within the hour" SLA on the product. The external store removes that caveat entirely.

---

## 5. Why the failure mode is the real problem

The 100-group ceiling would be tolerable if it failed early and visibly. It does the opposite:

1. The design review passes — 30 tenants × 2 roles = 60 groups, comfortably inside the limit.
2. Production runs fine for months, for almost every user.
3. A consultant, a support engineer or a platform administrator accumulates memberships — the exact people who legitimately belong to many tenants.
4. Their **101st grant fails** at `AdminAddUserToGroup`, in production, in an onboarding flow, for one user, with a quota error that cannot be raised by a support ticket.

The blast radius is small but the fix is not: by then, membership is *in* Cognito groups, and migrating to another store means re-plumbing the token contract, the login mapper and the administration path. The limit is non-adjustable, so there is no operational escape — only a redesign.

Choosing storage that has no such ceiling costs very little now and removes the failure mode entirely.

---

## 6. The options

### Option 1 — Flat role groups: `t:<tenant>:<role>`

*The model in the current proposals.* One group per (tenant, role); membership is implied by holding at least one.

- **Hits:** groups per user **100** (❌ at the stated scale), claim size, 25 RPS writes.
- **Pros:** single source of truth in the IdP; roles arrive in the token with no lookup; non-members deniable at issuance.
- **Cons:** fails above ~3 held roles per tenant at 30 tenants; late, user-specific failure; multi-KB claims; slow bulk administration; awkward reverse queries; no place for role metadata.
- **Verdict:** ❌ **Not viable at 20–30 tenants with granular roles.** Viable only for small memberships (≤ ~10 tenants) or coarse roles.

### Option 2 — Persona / bundle groups: `t:<tenant>:<persona>`

Collapse the ~10 granular roles into 3–4 personas (e.g. `admin`, `manager`, `member`, `viewer`); a user holds one, occasionally two, per tenant. The persona → granular-roles expansion happens in the application (or in the pre-token Lambda) from a mapping held outside Cognito.

- **Arithmetic:** 30 tenants × 1 = **30 groups** ✅ (headroom to ~100 tenants). Pool: 3–4 groups × tenants → ~2,500–3,300 tenants before the 10,000 ceiling.
- **Pros:** keeps the IdP authoritative; large reduction in both group count and claim size; deny-at-issuance retained; simple to administer.
- **Cons:** **forfeits arbitrary per-user role combinations** — a user can only hold predefined bundles, which conflicts with "different roles for each tenant" if that means granular mix-and-match; changing a persona's contents silently re-permissions everyone holding it; still a hard 100-tenant ceiling per user; the persona→roles mapping is a second artifact to version.
- **Verdict:** ⚠️ **Viable if the product can live with predefined role bundles.** The cheapest fix if granularity is negotiable.

### Option 3 — Membership-only groups + roles elsewhere: `t:<tenant>`

One group per tenant, carrying membership and nothing else; roles live in an external store, resolved after authentication.

- **Arithmetic:** 30 groups per user ✅; pool: 1 group per tenant → 10,000 tenants.
- **Pros:** the token carries a cheap, accurate membership list (useful for a tenant picker); non-members still deniable at token issuance; roles unlimited and granular.
- **Cons:** two stores for one concept — membership in Cognito, roles outside — which is precisely the drift risk that got the `custom:tenant` attribute rejected in the existing proposals, in milder form; still a 100-tenant-per-user ceiling; membership writes still cost UserUpdate quota.
- **Verdict:** ⚠️ **A reasonable middle ground**, mainly when deny-at-the-IdP is worth keeping a second store for.

### Option 4 — Packed custom attribute (tenant id + role bitmask)

Encode the whole graph into one custom attribute, e.g. `custom:tenroles = "a1:3ff,b7:005,c2:180"` — a short tenant id and a hex bitmask of that tenant's roles (10 roles = 10 bits = 3 hex characters).

- **Arithmetic:** ~7–8 characters per tenant → 30 tenants ≈ **210–240 bytes**, against 2,048 bytes ⇒ fits, up to **~250–290 tenants** in a single attribute; shardable across up to 50 attributes.
- **Pros:** no group-count ceiling at all; one compact claim; no extra store or Lambda; survives the stated scale with room to spare.
- **Cons:** **opaque** — support and audit require decoding; needs a versioned registry mapping tenant → short id and role → bit position, and any role added or removed in a tenant re-numbers bits; updates are **read-modify-write with no atomicity**, so two concurrent grants silently clobber each other; **no reverse query** at all (§4.5); writes still bounded by the 25 RPS UserUpdate quota; custom attributes **cannot be deleted or renamed** once added to a pool (verify — §10).
- **Verdict:** ⚠️ Fits the numbers, but **operationally hostile.** Document it, do not ship it unless an external store is genuinely impossible.

### Option 5 — External membership store, Cognito for identity only ★

Cognito authenticates (identity, MFA, federation, `sub`). The tenant ↔ user ↔ roles graph lives in a store built for it. Two variants:

**5a — Dirigible-side membership table** (natural fit for the tenant-picker model):

```sql
DIRIGIBLE_TENANT_MEMBERSHIPS (
  SUBJECT    VARCHAR   -- the IdP 'sub' (stable, immutable)
  TENANT_ID  VARCHAR   -- FK → DIRIGIBLE_TENANTS
  ROLE_NAME  VARCHAR   -- FK → DIRIGIBLE_SECURITY_ROLES (defined by .roles artefacts)
  PRIMARY KEY (SUBJECT, TENANT_ID, ROLE_NAME)
)
-- index on (SUBJECT) for login; on (TENANT_ID) for admin screens
```

At login one indexed query by `sub` builds the membership map; a tenant switch reads the roles for `(sub, tenant)`.

**5b — DynamoDB + the pre-token-generation Lambda** (fit for the subdomain model, where the token must carry that tenant's roles): `PK USER#<sub>` / `SK TENANT#<t>` → `roles: [...]`, with a GSI on the tenant for reverse lookups. The Lambda injects only the current tenant's roles (≤ 10), keeping tokens small and tenant-scoped.

- **Hits:** no Cognito quota whatsoever.
- **Pros:** unlimited tenants and granular roles; **immediate propagation** (removes the "role changes land at the next token refresh" caveat); trivial reverse queries for admin UIs; atomic, high-throughput writes with no shared account quota; room for metadata (granted-by, granted-at, expiry) and real audit; in 5a, referential integrity with the role names that `.roles` artefacts already define, and a natural in-app administration screen with no extra AWS service.
- **Cons:** it is a store to own — schema, admin API/UI, and backups (5a rides on the existing database backups; 5b needs its own); the platform (or DynamoDB) becomes the authorization source rather than the IdP; in 5b the Lambda sits in the authentication path (latency, availability); with 5a in the subdomain model the Lambda cannot easily read the app's database, so tokens carry no roles and the application resolves them per session — which also means losing deny-at-issuance.
- **Verdict:** ★ **Recommended.** The only option with no ceiling anywhere near the stated scale.

### Option 6 — AWS Verified Permissions (or another external authorization service)

Model tenants, users and roles as Cedar policies and ask the service for decisions.

- **Pros:** purpose-built for fine-grained, multi-tenant authorization; policy-as-code; centralised audit.
- **Cons:** Dirigible enforces on **role names** (`ROLE_*` authorities from `.roles`/`.access` artefacts), so a policy engine would sit awkwardly beside the platform's own model rather than replacing it; adds a service, a policy language and a per-request call for what is fundamentally a membership lookup. AWS also notes that group identifiers are not processed by `IsAuthorizedWithToken` and need custom token-parsing code.
- **Verdict:** ❌ **Overkill here.** Revisit only if authorization needs grow beyond role names into attribute- or resource-level policy.

---

## 7. Options at a glance

| # | Option | Groups/user at 30 tenants | Granular per-user roles | Reverse query | Propagation | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Flat role groups | **300 ❌** (limit 100) | ✅ | awkward | ≤ 1 h | ❌ Not viable |
| 2 | Persona / bundle groups | 30 ✅ | ❌ bundles only | awkward | ≤ 1 h | ⚠️ If bundles acceptable |
| 3 | Membership-only groups + roles outside | 30 ✅ | ✅ | ✅ (in the store) | immediate (roles) | ⚠️ Middle ground |
| 4 | Packed custom attribute | n/a (≈240 B of 2,048) | ✅ | ❌ none | ≤ 1 h | ⚠️ Fits, hostile |
| 5 | **External store, Cognito = identity** | **n/a — no quota** | ✅ | ✅ | **immediate** | ★ **Recommended** |
| 6 | Verified Permissions | n/a | ✅ | ✅ | immediate | ❌ Overkill |

---

## 8. Recommendation

**Keep Cognito for what it is excellent at — authentication, MFA, federation, one identity per person — and move the tenant ↔ user ↔ roles graph into a store designed for a graph.**

Concretely:

1. **Cognito holds:** `sub`, email, credentials, MFA, federation, and nothing tenant-related.
2. **The membership store holds:** `(sub, tenant, role)` — Option 5a's `DIRIGIBLE_TENANT_MEMBERSHIPS` table for the tenant-picker model (it needs no Lambda at all), or Option 5b's DynamoDB table where the pre-token Lambda must mint role-bearing tenant-scoped tokens for the subdomain model.
3. **Optionally keep one membership-only group per tenant** (`t:<tenant>`, ~30 per user — comfortably inside the 100 limit) purely so the subdomain model can still refuse a token to a non-member at the IdP. This is Option 3 layered on Option 5, and it is the only reason to keep any tenant data in Cognito.
4. **If an external store is politically or operationally impossible**, take Option 2 (personas) and accept predefined role bundles — not Option 1, and not Option 4.

Sizing sanity check for the recommendation: 30 tenants × 10 roles = at most 300 rows per user; 10,000 users × 30 memberships = 300,000 rows — a trivially small table, indexed, answering both directions in one query.

---

## 9. What this would imply for the existing proposals

Recorded as implications only — no document is changed by this analysis.

**Both proposals** currently state that membership and roles live in prefixed Cognito groups and that the 100-group limit is a distant growth watchpoint. Under this analysis it is not distant: it is reached inside the stated requirements, so that decision and its "watchpoint" framing would need revisiting in both.

**`AWS_DEPLOYMENT_PROPOSAL.md` (subdomain):** the pre-token-generation Lambda stops filtering groups by prefix and instead reads the membership store for `(sub, tenant)` — the token contract (`cognito:groups` = roles for this tenant, plus `dirigible:tenant`) is **unchanged**, so nothing downstream in the platform moves. Onboarding gains a membership-store write instead of group creation.

**`AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md` (picker):** the improvement is larger. The token no longer needs to carry the membership graph at all — it carries identity, and the application builds the membership map with one query at login. Fork change **S2 gets simpler** (no group-name parsing), and the "role changes take effect at the next login" caveat in §3.5 disappears, because the store is read at login and at every switch.

**`AWS_TENANCY_MODELS_COMPARISON.md`:** its "identical in both models" section lists the groups-only membership model as shared — it would instead be "an external membership store, shared", which is still identical across the two models and remains an argument that the choice between them is purely an identity/UX decision.

---

## 10. Verify before committing

1. **Re-read the quotas page** at decision time — <https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html>. All figures here were read on 2026-07-29; the non-adjustable ones (100 groups/user, 2,048 bytes/attribute, 50 custom attributes, 25 RPS UserUpdate) are the ones that matter.
2. **Prove the ceiling in your own pool:** create a test user, add 100 groups, attempt the 101st, and record the exact error and where it surfaces in your onboarding flow.
3. **Measure a real token** with a representative membership set — decode it and check the `cognito:groups` claim size against whatever will carry it.
4. **Confirm the custom-attribute constraints** if Option 4 is under serious consideration: that attributes cannot be deleted or renamed after creation, and that `ListUsers` cannot filter on the packed value (both are the basis of that option's ❌ on reverse queries).
5. **Time a bulk grant** against the 25 RPS UserUpdate quota in a non-production account, and confirm what else in your estate shares that quota.
6. **Decide the granularity question explicitly** — whether users genuinely need arbitrary role combinations per tenant, or whether 3–4 personas per tenant would do. That single answer decides between Option 2 and Option 5.
