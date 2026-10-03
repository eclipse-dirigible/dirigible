# Tenant by Subdomain vs Single Host with a Tenant Picker — Comparison

**Status:** decision aid. Compares the two proposed tenancy models for multitenant Dirigible on AWS.
**Date:** 2026-07-29
**Companions:**
- [`AWS_DEPLOYMENT_PROPOSAL.md`](AWS_DEPLOYMENT_PROPOSAL.md) — the full **subdomain** proposal (`tenant1.app.com`, `tenant2.app.com`, …).
- [`AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md`](AWS_SINGLE_HOST_TENANT_PICKER_PROPOSAL.md) — the full **single-host + picker** proposal (one host, tenant chosen and switched in the UI).

Both documents contain the detailed designs; this one exists to put the trade-offs side by side and support the decision.

**Contents**

- [1. The two models in one paragraph each](#1-the-two-models-in-one-paragraph-each)
- [2. What is identical in both](#2-what-is-identical-in-both)
- [3. Side-by-side comparison](#3-side-by-side-comparison)
- [4. Subdomain model — pros and cons](#4-subdomain-model--pros-and-cons)
- [5. Single host + picker — pros and cons](#5-single-host--picker--pros-and-cons)
- [6. Decision guidance](#6-decision-guidance)
- [7. The hybrid worth knowing about](#7-the-hybrid-worth-knowing-about)
- [8. Switching models later](#8-switching-models-later)

---

## 1. The two models in one paragraph each

**Subdomain:** every tenant has its own hostname (`acme.app.com`). The `Host` header *is* the tenant signal — Dirigible's shipped `TenantExtractor` resolves it per request, and per-host session cookies give every tenant an independent login session for free. Identity: one Cognito pool, **one app client per tenant**; a pre-token Lambda mints **tenant-scoped tokens** (only that tenant's roles + a `dirigible:tenant` audience claim). Moving to another tenant = opening its hostname; the pool-wide SSO cookie makes it silent.

**Single host + picker:** all tenants share one hostname (`app.com`). The host carries no tenant signal, so the **active tenant is server-side session state**: the user logs in once, the token carries their **entire membership graph** (as `t:<tenant>:<role>` groups), and a picker endpoint validates the choice, stores it in the session and **rebuilds the session's authorities** to that tenant's roles. Identity: one Cognito pool, **one app client**, no Lambda required. Moving to another tenant = a dropdown action, no re-authentication, full UI reload.

## 2. What is identical in both

- **The requirement core:** one identity per person (one Cognito user pool), same credentials in every tenant, **different roles per tenant**.
- **Membership model:** groups named `t:<tenant>:<role>`; no tenant user attribute; membership = holding ≥ 1 of the tenant's groups; the 100-groups-per-user limit as the growth watchpoint.
- **Everything below the identity layer:** ECS Fargate single-writer runtime (`desiredCount=1`, stop-then-start deploys), RDS PostgreSQL Multi-AZ with schema-per-tenant provisioning, S3 CMS with tenant-prefixed keys, content baked into the image, the hardening checklist, backup/observability/cost.
- **Horizontal-scaling blockers** (picker proposal §10.2, items 1–5): embedded ActiveMQ lock, per-node synchronizer replay, boot DDL races, per-JVM caches — the platform pins both models to one task per unit equally.
- **Shared fork fixes:** configurable Hikari pool sizing, method security under the cognito profile, trial-flag fail-fast, provisioning idempotency, admin-endpoint gating.

The decision is therefore **purely an identity/UX-layer decision** — infrastructure and data isolation do not move either way.

## 3. Side-by-side comparison

| Dimension | Subdomain per tenant | Single host + picker |
| --- | --- | --- |
| **Login UX** | Sign in per tenant host (silent after the first — pool-wide SSO cookie) | **One sign-in, switch in-app** — no second OIDC round-trip visible to the user |
| **Tenant switching** | Navigate to the other hostname | Dropdown in the shell; authorities rebuilt server-side; full UI reload |
| **Concurrent tabs in different tenants** | **Yes** — per-host cookies = independent sessions | **No** — one active tenant per browser session; a switch in one tab affects all tabs (stale-tab guard mitigates) |
| **Deep links / bookmarks** | Carry the tenant in the URL | Tenant-less — open in whatever tenant is currently active |
| **URL legibility / support** | The URL tells the user (and your support engineer, and the ALB access log) which tenant — useful in tickets, logs, screenshots | The tenant is invisible outside the session — support needs the user to say which tenant, edge logs can't attribute traffic per tenant |
| **Token scoping** | **Tenant-scoped at issuance** — token holds only that tenant's roles + `dirigible:tenant` audience claim | Multi-tenant token — the user's whole membership graph in every token |
| **Stolen-token blast radius** | One tenant (audience binding asserted per request) | **All** of the victim's tenants (no audience binding possible) |
| **Non-member denial** | **At the IdP** — the Lambda refuses to mint the token | App-side only — the picker/switch endpoint 403s |
| **Cognito surface** | N app clients + N secrets + pre-token Lambda + DynamoDB clientId→tenant map; **Essentials** plan (trigger V2_0) | **One app client, one secret, no Lambda, no map; Lite plan may suffice** |
| **DNS / TLS** | Wildcard DNS record + wildcard ACM certificate | One record, one plain certificate |
| **Tenant onboarding** | Cognito client + secret + Dirigible registration + groups | **Groups only** (plus the schema provisioning both need) |
| **Fork surface (identity layer)** | Small — per-host cookies do the heavy lifting; 7 targeted fixes | **Larger** — S1–S6: session/header tenant resolver, neutral login mapper, switch endpoint + authority rebuild, picker UI, header M2M tenancy, stale-tab guard |
| **Per-tenant branding before login** | Possible — the host identifies the tenant on the login/landing page | Not possible — the tenant is unknown until after login + pick |
| **Per-tenant enterprise federation (tenant brings own IdP)** | Clean — IdP enablement is a per-app-client setting | Needs home-realm discovery (email-domain → IdP hint at login) — doable, not free |
| **Per-tenant vanity/custom domains later** (`app.acme.com`) | Natural extension (CNAME + cert + one more callback) | Contradicts the model — would reintroduce host awareness |
| **Per-tenant edge controls** (WAF rate rules, blocking one tenant at the ALB) | By host — easy | Impossible at the edge; only app-side |
| **Scale-out to more units** | Per-tenant DNS records — clean | Edge can't see the tenant → routing cookie + CloudFront Function, gateway tier, or placement constraint (picker §10.3) |
| **Horizontal replicas someday** | Platform blockers (shared); sessions per host are already store-friendly | Same platform blockers **plus** externalized sessions (spring-session + Redis) — after which the design carries over unchanged |
| **M2M** | Per-tenant M2M client; tenant asserted from the token's audience/scopes | Per-tenant M2M client; tenant carried in `X-Dirigible-Tenant` header, asserted against scopes |
| **Logout semantics** | Per-tenant logout (fork fix); Cognito logout ends pool-wide SSO | Single-client logout — the shipped handler is correct as-is |
| **CSRF exposure of tenancy** | None — the tenant can't be changed by a forged request (it's the host) | The switch endpoint is state-changing and needs CSRF protection |

## 4. Subdomain model — pros and cons

**Pros**

1. **Strongest token security:** issuance-time least privilege, audience binding (`dirigible:tenant` == host asserted every request), IdP-side denial of non-members, stolen tokens useless outside their tenant.
2. **Concurrent multi-tenant work** — independent sessions per host; two tabs, two tenants, no interference.
3. **The URL carries the tenant** — bookmarkable deep links, legible support tickets, per-tenant edge logs/WAF rules, pre-login branding.
4. **Small fork footprint** — the platform is host-tenant native; per-host cookies solve session scoping for free.
5. **Clean growth**: per-tenant DNS routes tenants to units; vanity domains and per-tenant federation are natural extensions.

**Cons**

1. **Per-tenant Cognito machinery:** N app clients and secrets to create/rotate, a pre-token Lambda, a clientId→tenant map, Essentials-plan pricing — onboarding automation must manage all of it.
2. **Tenant switching is navigation, not an in-app control** — silent thanks to SSO, but it is a new page load on a new host, and the "which tenants do I have?" list needs the `dirigible:tenants` claim or a directory page.
3. **Wildcard DNS + wildcard certificate** to operate (modest, but real).
4. A fork-built host-keyed `ClientRegistrationRepository` is required to resolve N registrations (the shipped module was ruled out as a design input).
5. Does not satisfy a product requirement of "no per-tenant URLs" — if the product must present one address, this model is disqualified by definition.

## 5. Single host + picker — pros and cons

**Pros**

1. **The requirement's UX:** log in once, switch tenants from a dropdown, no visible re-authentication — the closest to a "single product with workspaces" feel.
2. **Radically simpler identity operations:** one app client, one secret, no Lambda, no client map; Cognito Lite may suffice; tenant onboarding = create groups.
3. **Simplest edge:** one DNS record, one plain certificate; the shipped OAuth registration and logout handler work nearly as-is.
4. **One URL for everyone** — simpler to communicate, no per-tenant links to manage in emails/docs.
5. The session-scoped design survives future horizontal scaling unchanged once sessions are externalized (spring-session + Redis) — and that addition is a cheap early win anyway (sessions survive deploys).

**Cons**

1. **Weaker token posture:** every token carries the whole membership graph; no audience binding exists to assert; denial of non-members happens only app-side; a stolen token spans all the victim's tenants.
2. **One active tenant per browser** — concurrent tabs in different tenants are structurally impossible; the stale-tab guard softens, never removes, the surprise.
3. **Bigger fork surface in the security layer** (S1–S6), including replacing tenant resolution, rebuilding authorities on a live session, and CSRF-protecting the switch endpoint — more new security-critical code to get right.
4. **The tenant is invisible outside the app:** no per-tenant edge rules, no per-tenant traffic attribution at the ALB, no pre-login branding, tenant-less deep links, and support must ask "which tenant were you in?".
5. **Scale-out friction:** routing tenants to more units needs an extra edge mechanism (routing cookie + CloudFront Function or a gateway) because nothing in the request names the tenant.
6. Per-tenant enterprise federation requires an additional home-realm-discovery step.

## 6. Decision guidance

Choose **subdomains** when:

- security posture dominates — per-tenant token scoping, IdP-side denial and small blast radius are worth per-tenant client operations;
- users (or admins/support) routinely work in **several tenants at once**;
- tenants are organizations that may later want **their own IdP or their own domain**;
- per-tenant edge behaviour matters (rate-limiting one tenant, blocking one tenant, per-tenant branding before login);
- the operator prefers **less fork code** over less Cognito configuration.

Choose the **single host + picker** when:

- the product requirement is literally "one address, switch inside the app" (which disqualifies subdomains regardless of other merits);
- users belong to many tenants and hop frequently — the dropdown beats host navigation;
- identity-operations simplicity matters more than token scoping (small trusted tenant fleet, internal deployment, one organization with several workspaces);
- nobody needs concurrent multi-tenant tabs, per-tenant URLs or per-tenant edge controls.

A useful framing: **subdomains treat tenants as separate products sharing a platform; the picker treats tenants as workspaces inside one product.** B2B SaaS with organizational customers usually wants the former; an internal or single-organization deployment usually wants the latter.

## 7. The hybrid worth knowing about

The picker *UX* does not strictly require the single-host *architecture*. On the subdomain model, a "tenant switcher" dropdown can simply link to the other tenants' hostnames (the membership list is available via the `dirigible:tenants` claim): the hop is silent thanks to the pool-wide SSO cookie, and every subdomain-model advantage (scoped tokens, per-host sessions, concurrent tabs) is retained. What the hybrid does **not** deliver is the literal requirement "no dedicated subdomain per tenant" — URLs remain per-tenant. If the requirement is about switching *convenience*, the hybrid may be the best of both; if it is about the *address itself*, only the single-host model qualifies.

## 8. Switching models later

The two models share the group naming (`t:<tenant>:<role>`), the pool, and the user base, so a later migration changes plumbing, not data:

- **Picker → subdomains:** add per-tenant app clients + the pre-token Lambda + per-tenant DNS; replace the session resolver with the (shipped) host extractor; the groups stay as they are. The main new work is the per-tenant client onboarding automation.
- **Subdomains → picker:** collapse to one client, drop the Lambda, add S1–S6. The main new work is the session/security layer.

Neither direction touches tenant schemas, provisioned data, or the membership graph — which is the strongest argument for the shared groups convention both proposals adopted.
