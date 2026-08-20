# Multitenant Dirigible on AWS with Amazon Cognito — Setup Guide

**Status:** implementation & configuration guide. Nothing here is deployed for you; the steps are ready to run.
**Date:** 2026-07-28

**Companion documents**
- [`AWS_MULTITENANCY_RESEARCH.md`](AWS_MULTITENANCY_RESEARCH.md) — what deploys on the current release, unchanged.
- [`AWS_MULTITENANCY_TARGET_ARCHITECTURE.md`](AWS_MULTITENANCY_TARGET_ARCHITECTURE.md) — the recommended claims-driven identity model and the platform changes behind it.
- [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md) — how multitenancy works in the code today.

This guide is the **how-to**. It answers, concretely: *how many OAuth applications and with what config; one user pool or one per tenant; how a user's tenants and roles live in Cognito and reach Dirigible; and which Cognito settings to use.* A user must be able to sign into several tenants but gain access only where authorized — that requirement drives every choice below.

---

## Decisions at a glance

| Question | Answer |
| --- | --- |
| **One user pool, or one per tenant?** | **One user pool for all tenants.** Cross-tenant login means one identity per person; pool-per-tenant means one account per person per tenant, which breaks the requirement |
| **How many OAuth app clients?** | **One confidential app client per tenant**, plus one for the default/platform tenant, plus (optionally) one machine-to-machine client per tenant that needs API access |
| **Where do tenant membership and roles live?** | **In a control-plane store (DynamoDB), not in Cognito.** A pre-token-generation Lambda turns them into token claims at sign-in |
| **How do roles reach Dirigible?** | The Lambda writes the caller's roles *for this tenant* into the standard `cognito:groups` claim; Dirigible's existing mapper turns those into Spring authorities |
| **Which Cognito feature plan?** | **Essentials** (or Plus) — required for the pre-token trigger versions used here |

The rest of the document is the build order for exactly that.

### Two modes

**Mode A — configuration only.** Everything here works against the **current** Dirigible code with no changes: the per-tenant app client makes each token carry only that tenant's roles, and Dirigible's shipped `cognito:groups` → authorities mapper is then correct per tenant by construction. Mode A has real caveats (§13) — most importantly that machine-to-machine tokens skip the tenant membership check and that `@RolesAllowed` may be inert under the cognito profile.

**Mode B — fork hardening.** The small set of platform changes from the target-architecture document (§6 there) that close Mode A's caveats. Called out inline as *[Mode B]* and collected in §13.

---

## 1. Architecture overview

```
   Anna's browser
        │  https://acme.app.example.com        https://globex.app.example.com
        ▼                                        ▼
   ┌─────────────────────── ALB (wildcard *.app.example.com, ACM) ──────────────────────┐
   │  Host header → tenant subdomain (Dirigible TenantExtractor)                          │
   └───────────────────────────────────┬─────────────────────────────────────────────────┘
                                        ▼
                              ┌──────────────────────┐
                              │  Dirigible (Fargate) │  spring_profiles_active=cognito
                              │  cognito:groups →     │  MULTI_TENANT_MODE=true
                              │  ROLE_* authorities   │  ..._COGNITO_SINGLE_USER_POOL=true
                              └───────┬──────────────┘
                                      │ OIDC (per-tenant app client)
                                      ▼
   ┌──────────────────────────── ONE Cognito user pool ──────────────────────────────────┐
   │  identities: one per person (sub = global id)                                        │
   │  app clients:  acme   globex   …   default   [+ acme-m2m, globex-m2m]                 │
   │  managed-login session cookie shared across ALL app clients (1h)                     │
   │                                                                                       │
   │  Pre-token-generation Lambda  (trigger event V2_0 / V3_0)                             │
   │    clientId → tenant                                                                   │
   │    (sub, tenant) → roles          ── reads ──▶  DynamoDB membership store             │
   │    not a member → throw (deny)                    USER#<sub> / TENANT#<tenant> → roles│
   │    member → cognito:groups = roles-here                                               │
   │             dirigible:tenant, dirigible:tenants                                       │
   └───────────────────────────────────────────────────────────────────────────────────────┘
                                      ▲
                                      │ writes (grant/revoke/roles)
                              ┌───────┴──────────┐
                              │  Control plane   │  onboarding, membership admin
                              └──────────────────┘
```

Three parties, three jobs: **Cognito** authenticates and mints tokens; the **control plane + DynamoDB** own the tenant↔user↔roles graph; **Dirigible** consumes the resulting claims and enforces them. No identity or authorization data is stored in Dirigible's own database.

---

## 2. Step 1 — the user pool

Create **one** user pool for the whole platform.

Settings that matter:

| Setting | Value | Why |
| --- | --- | --- |
| Feature plan | **Essentials** | Pre-token trigger event versions V2_0/V3_0 (used to override groups and add claims) require Essentials or Plus. Lite only reaches V1_0/V2_0 |
| Sign-in identifier | **Email**, case-insensitive | `user-name-attribute=email` is what Dirigible's cognito config already expects |
| Self-registration | **Off** | Users are invited by the control plane during onboarding; open sign-up would let anyone create an identity with no tenant |
| MFA | Per your policy (recommend required TOTP) | One pool = one place to enforce it for every tenant |
| Deletion protection | **On** | The pool is now the identity system of record |
| Attributes | `email` (required). Add `custom:tenant` (mutable, app-writable) **only for Mode A** | Mode A's `CognitoTenantFilter` reads `custom:tenant`; Mode B replaces that check and doesn't need the attribute |

```bash
aws cognito-idp create-user-pool \
  --pool-name dirigible-platform \
  --user-pool-tier ESSENTIALS \
  --username-attributes email \
  --username-configuration CaseSensitive=false \
  --admin-create-user-config AllowAdminCreateUserOnly=true \
  --mfa-configuration OPTIONAL \
  --deletion-protection ACTIVE
# Mode A only — add the tenant-membership attribute:
#   --schema Name=tenant,AttributeDataType=String,Mutable=true
```

Note the pool id (`us-east-1_XXXXXXXXX`) and its region — Dirigible builds the issuer and JWKS URLs from them.

---

## 3. Step 2 — domain and managed login

Give the pool a custom auth domain so tokens have a stable issuer and the hosted UI can be branded once for all tenants.

```bash
aws cognito-idp create-user-pool-domain \
  --user-pool-id us-east-1_XXXXXXXXX \
  --domain auth.app.example.com \
  --custom-domain-config CertificateArn=arn:aws:acm:us-east-1:<acct>:certificate/<id>
```

The ACM certificate for a Cognito custom domain **must be in `us-east-1`** regardless of your pool's region. Point a Route 53 alias at the CloudFront distribution Cognito returns.

The managed-login session cookie is set on this domain and is valid for **1 hour, not configurable**. It is the mechanism that lets one sign-in flow into multiple tenants without re-entering credentials — see §12.

---

## 4. Step 3 — the membership store (DynamoDB)

Cognito holds *who a person is*. The control plane holds *which tenants they belong to and with what roles*. A single-table DynamoDB design is enough.

| Item kind | PK | SK | Attributes |
| --- | --- | --- | --- |
| Membership | `USER#<sub>` | `TENANT#<tenantSubdomain>` | `roles: ["ADMINISTRATOR", ...]` |
| Client → tenant map | `CLIENT#<appClientId>` | `TENANT` | `tenant: "<tenantSubdomain>"` |

Example items for Anna, who administers `acme` and manages employees in `globex`:

```json
{ "PK": "USER#a1b2-c3d4", "SK": "TENANT#acme",   "roles": ["ADMINISTRATOR"] }
{ "PK": "USER#a1b2-c3d4", "SK": "TENANT#globex", "roles": ["employee-manager"] }
{ "PK": "CLIENT#3ab...acme",   "SK": "TENANT", "tenant": "acme"   }
{ "PK": "CLIENT#7cd...globex", "SK": "TENANT", "tenant": "globex" }
```

Rules:
- `sub` is Cognito's immutable per-user id — the Lambda cannot alter it, so it is the safe join key.
- **Role names must equal Dirigible role names exactly** — the three built-ins (`ADMINISTRATOR`, `DEVELOPER`, `OPERATOR`) and any application role defined by a `.roles` artefact. They become `ROLE_<name>` authorities verbatim.
- Only the control plane writes this table. Nothing in Dirigible reads or writes it directly.

Why not Cognito groups or a custom attribute? A user in many tenants with several roles each would blow past the non-adjustable **100 groups per user** limit, and a custom attribute is user-global (2 KB cap, 50 per pool) so it could not carry per-tenant role sets cleanly. Keeping the graph in DynamoDB and projecting it per-token via the Lambda has neither limit.

---

## 5. Step 4 — the pre-token-generation Lambda

This is where a user's DynamoDB memberships become tenant-scoped token claims, and where a non-member is denied a token at all.

```javascript
// Node.js 20. Trigger: Pre token generation, event version V2_0 (V3_0 if you
// also customize M2M tokens — see §6).
import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const TABLE = process.env.MEMBERSHIP_TABLE;

export const handler = async (event) => {
  const sub = event.request.userAttributes.sub;
  const clientId = event.callerContext.clientId;

  // 1. Which tenant is this app client for?
  const clientMap = await ddb.send(new GetCommand({
    TableName: TABLE, Key: { PK: `CLIENT#${clientId}`, SK: "TENANT" },
  }));
  const tenant = clientMap.Item?.tenant;
  if (!tenant) {
    throw new Error(`No tenant mapped for app client ${clientId}`); // deny — misconfig
  }

  // 2. Is this identity a member of that tenant? With which roles?
  const membership = await ddb.send(new GetCommand({
    TableName: TABLE, Key: { PK: `USER#${sub}`, SK: `TENANT#${tenant}` },
  }));
  const roles = membership.Item?.roles;
  if (!roles || roles.length === 0) {
    // Deny token issuance. Cognito surfaces this as a failed sign-in.
    throw new Error(`User ${sub} is not a member of tenant ${tenant}`);
  }

  // 3. Full membership list (for the UI tenant switcher — never an authz input).
  const all = await ddb.send(new QueryCommand({
    TableName: TABLE,
    KeyConditionExpression: "PK = :u AND begins_with(SK, :t)",
    ExpressionAttributeValues: { ":u": `USER#${sub}`, ":t": "TENANT#" },
  }));
  const tenants = (all.Items ?? []).map(i => i.SK.replace("TENANT#", ""));

  // 4. Emit tenant-scoped claims. groupsToOverride REPLACES cognito:groups
  //    in BOTH the ID and access tokens.
  event.response = {
    claimsAndScopeOverrideDetails: {
      groupOverrideDetails: { groupsToOverride: roles },
      idTokenGeneration: {
        claimsToAddOrOverride: {
          "dirigible:tenant": tenant,
          "dirigible:tenants": tenants.join(","),
        },
      },
      accessTokenGeneration: {
        claimsToAddOrOverride: { "dirigible:tenant": tenant },
      },
    },
  };
  return event;
};
```

Wire it as the pool's **Pre token generation** trigger with event version **V2_0** ("Basic features + access token customization for user identities"), or **V3_0** if you also want the same treatment for machine-to-machine tokens (§6). Grant the function `dynamodb:GetItem` + `dynamodb:Query` on the table.

Two behaviours to state as contract, not accident:

- **Role propagation is refresh-bound.** The trigger also fires on `TokenGeneration_RefreshTokens`, so a membership or role change in DynamoDB takes effect on the next token refresh — worst case ≈ the access-token lifetime (1 h) plus the Dirigible session. For an immediate revoke, use the "eject user" operation in §15.
- **The claim-changes quota.** Existing + added claims and scopes in one transaction must total ≤ 5,000 (adjustable). Not a real constraint here, but it exists.

---

## 6. Step 5 — per-tenant app clients

One **confidential** app client per tenant. The client is the thing that tells the Lambda which tenant a token is for, and its registration id in Dirigible must equal the tenant subdomain.

```bash
aws cognito-idp create-user-pool-client \
  --user-pool-id us-east-1_XXXXXXXXX \
  --client-name acme \
  --generate-secret \
  --allowed-o-auth-flows code \
  --allowed-o-auth-flows-user-pool-client \
  --allowed-o-auth-scopes openid email profile \
  --callback-urls https://acme.app.example.com/login/oauth2/code/acme \
  --logout-urls  https://acme.app.example.com/ \
  --supported-identity-providers COGNITO \
  --prevent-user-existence-errors ENABLED \
  --enable-token-revocation \
  --access-token-validity 60 --id-token-validity 60 --refresh-token-validity 30 \
  --token-validity-units AccessToken=minutes,IdToken=minutes,RefreshToken=days
```

Then record the client→tenant mapping the Lambda relies on:

```bash
aws dynamodb put-item --table-name dirigible-membership \
  --item '{"PK":{"S":"CLIENT#<thisClientId>"},"SK":{"S":"TENANT"},"tenant":{"S":"acme"}}'
```

The contract with Dirigible: `CognitoLoginController` exposes `GET /login/{registrationId}`, validates `{registrationId}` against the set of **provisioned tenant subdomains**, and redirects to `/oauth2/authorization/{registrationId}`. So the Dirigible client registration for this tenant must be named **`acme`**, and its callback path must be `/login/oauth2/code/acme`. The per-client callback URL is exactly why one client per tenant beats one shared client — Cognito caps callback URLs at **100 per client** and does not allow wildcards.

Also create **one app client for the default/platform tenant** the same way (name it e.g. `default` or `cognito`), because Dirigible's cognito profile has required Spring placeholders that must resolve at boot (§7).

Quota headroom: **1,000 app clients per pool** by default, raisable to **10,000** — that is your tenants-per-pool ceiling.

---

## 7. Step 6 — machine-to-machine access (optional)

For tenants whose automation calls Dirigible APIs with a token instead of a browser session:

1. Create a resource server on the pool, id `dirigible`, with **custom scopes named after roles** — e.g. `dirigible/ADMINISTRATOR`, `dirigible/data-reader`.
2. Create a `client_credentials` app client per such tenant, granted the scopes that tenant's automation should hold.
3. Set the pre-token trigger to **V3_0** — the only version that fires for `TokenGeneration_ClientCredentials`, so the only one that can tenant-scope M2M tokens.

Dirigible maps these with the shipped `ScopeRoleJwtAuthoritiesConverter`: it takes the substring **after the last `/`** of each scope (`dirigible/ADMINISTRATOR` → `ADMINISTRATOR`), optionally expands it through `.scopes` artefacts, and turns the result into `ROLE_*`. A scope with no `/` (like `openid`) is ignored, so standard scopes never become roles.

> **[Mode B]** On today's code, a Bearer/JWT request is a `JwtAuthenticationToken`, which the `CognitoTenantFilter` membership check does **not** inspect — so M2M callers bypass the tenant membership gate entirely. Until the Mode B membership filter lands (§13), scope M2M clients narrowly and treat them as trusted per tenant.

---

## 8. Step 7 — Dirigible configuration

Set these on the Dirigible ECS task (or container env). All keys are verified against `components/security/security-cognito/src/main/resources/application-cognito.properties` and `DirigibleConfig`.

**Profile + multitenancy**

| Env var | Value |
| --- | --- |
| `spring_profiles_active` | `cognito` |
| `DIRIGIBLE_MULTI_TENANT_MODE` | `true` |
| `DIRIGIBLE_MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL` | `true` |
| `DIRIGIBLE_HOST` | `https://app.example.com` (used to build the default client's redirect URI) |

**Default-tenant client (satisfies the required Spring placeholders)**

| Env var | Value |
| --- | --- |
| `DIRIGIBLE_COGNITO_CLIENT_ID` | the default app client id |
| `DIRIGIBLE_COGNITO_CLIENT_SECRET` | its secret |
| `DIRIGIBLE_COGNITO_DOMAIN` | `https://auth.app.example.com` |
| `DIRIGIBLE_COGNITO_REGION_ID` | `us-east-1` |
| `DIRIGIBLE_COGNITO_USER_POOL_ID` | `us-east-1_XXXXXXXXX` |
| `DIRIGIBLE_COGNITO_SCOPE` | `openid` (default) |
| `DIRIGIBLE_COGNITO_GRANT_TYPE` | `authorization_code` (default) |

`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` and `DIRIGIBLE_HOST` have **no defaults** — if they are unset the context fails to start. That is the main reason the default client exists.

**Per-tenant client registrations.** Two ways; pick one.

- *Runtime (recommended, dynamic):* for each tenant, `POST /services/security/client-registrations` with a JSON body whose **`name` is the tenant subdomain**:

  ```json
  {
    "name": "acme",
    "clientId": "<acme app client id>",
    "clientSecret": "<acme secret>",
    "redirectUri": "https://acme.app.example.com/login/oauth2/code/acme",
    "authorizationGrantType": "authorization_code",
    "scope": "openid,email,profile",
    "tokenUri": "https://auth.app.example.com/oauth2/token",
    "authorizationUri": "https://auth.app.example.com/oauth2/authorize",
    "userInfoUri": "https://auth.app.example.com/oauth2/userInfo",
    "issuerUri": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXXXX",
    "jwkSetUri": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXXXXX/.well-known/jwks.json",
    "userNameAttributeName": "email"
  }
  ```

- *Boot-time (static env):* `DIRIGIBLE_OAUTH_CUSTOM_CLIENTS=acme,globex`, then for each name eleven keys prefixed with the **raw name** (no `DIRIGIBLE_`): `acme_CLIENT_ID`, `acme_CLIENT_SECRET`, `acme_REDIRECT_URI`, `acme_GRANT_TYPE`, `acme_SCOPE`, `acme_TOKEN_URI`, `acme_AUTHORIZATION_URI`, `acme_USER_INFO_URI`, `acme_ISSUER_URI`, `acme_JWK_SET_URI`, `acme_USER_NAME_ATTRIBUTE`. Any missing key throws at boot.

**Tenant row.** Each tenant also needs a Dirigible tenant record (subdomain must match): `POST /services/security/tenants` with `{ "name": "Acme", "subdomain": "acme" }`. The provisioning job then creates its schema and datasource (see the companion docs).

**Login entry point:** `https://acme.app.example.com/login/acme`.

---

## 9. End-to-end worked example

Anna (`sub = a1b2-c3d4`) administers `acme` and manages employees in `globex`. DynamoDB holds the four items from §4.

**She signs into `acme`.** App client `acme` → Lambda maps client → tenant `acme` → membership `["ADMINISTRATOR"]`. Token:

```json
{ "sub": "a1b2-c3d4", "email": "anna@example.com",
  "cognito:groups": ["ADMINISTRATOR"],
  "dirigible:tenant": "acme", "dirigible:tenants": "acme,globex" }
```

Dirigible's `userAuthoritiesMapper` reads `cognito:groups` → authority `ROLE_ADMINISTRATOR`. Anna can reach the admin surfaces in `acme`.

**She opens `https://globex.app.example.com`.** App client `globex` → tenant `globex` → membership `["employee-manager"]`. Token:

```json
{ "sub": "a1b2-c3d4", "cognito:groups": ["employee-manager"],
  "dirigible:tenant": "globex", "dirigible:tenants": "acme,globex" }
```

→ authority `ROLE_employee-manager` **only**. Same person, different roles, and her `acme` admin rights do not follow her into `globex`.

**A non-member is denied.** If Anna somehow reaches the `initech` app client, the Lambda finds no `USER#a1b2-c3d4 / TENANT#initech` item and **throws** — Cognito never issues a token, so Dirigible never sees an unauthorized session.

---

## 10. Login flows

**First login to a tenant**

```
https://acme.app.example.com/  →  unauthenticated
  → /login/acme  →  /oauth2/authorization/acme
  → Cognito managed login (client acme): credentials + MFA
  → Lambda: member? yes → groups = roles(acme)
  → callback /login/oauth2/code/acme  →  JSESSIONID for acme.app.example.com
```

**Cross-tenant hop — the requirement**

```
same browser → https://globex.app.example.com/  →  no session for this host
  → /login/globex  →  /oauth2/authorization/globex
  → Cognito: managed-login cookie still valid (pool-wide, ≤1h)  ⇒  NO credential prompt
  → Lambda: member? yes → groups = roles(globex)   ← different roles
  → separate JSESSIONID for globex.app.example.com
```

One sign-in, two tenants, different roles, no re-authentication — using Cognito's documented shared-cookie behaviour rather than fighting it. Because `JSESSIONID` is host-scoped, each subdomain's Spring session holds its own tenant's authorities, so the "authorities are snapshotted at login" behaviour is correct per tenant.

**Logout.** `https://<sub>.app.example.com/logout` clears the Dirigible session and redirects to Cognito's logout endpoint.

> **[Mode A caveat]** `CognitoLogoutSuccessHandler` uses the single `DIRIGIBLE_COGNITO_CLIENT_ID` and `DIRIGIBLE_HOST`, so logout hits the *default* client and host rather than the current tenant's. It signs the user out, but the redirect can be wrong. Fixed in Mode B (§13).

---

## 11. Mode A caveats → Mode B fixes

| Mode A behaviour (works, but imperfect) | Mode B change (target-architecture §6) |
| --- | --- |
| M2M/Bearer tokens bypass the tenant membership check (the filter only inspects `OAuth2AuthenticationToken`) | One provider-agnostic `TenantMembershipFilter` that also inspects `JwtAuthenticationToken` and asserts `dirigible:tenant == host tenant` |
| `@RolesAllowed` may be inert under the cognito profile — `@EnableMethodSecurity(jsr250Enabled=true)` sits only on the basic-auth config (**hypothesis**, confirm first) | Enable JSR-250 method security in the active security config |
| `CognitoTenantFilter` isn't added inside the security chain (unlike basic/keycloak/snowflake) — runs only via servlet auto-registration | Add the membership filter explicitly in `CognitoSecurityConfiguration` |
| Logout uses the single default client/host | Per-tenant logout from the current registration |
| Client registrations are a CRUD surface with a process-global static map and plaintext-secret reads | Derive registrations from the tenant registry; stop returning secrets |
| `DIRIGIBLE_TRIAL_ENABLED` grants every role to everyone, tenant-blind | Remove or restrict to the default tenant in a non-prod profile |

None of these blocks Mode A for a trusted set of tenants; all of them matter before untrusted tenants share the pool.

---

## 12. Tenant onboarding runbook

Automate this in the control plane; the order matters (the Lambda must be able to map the client before anyone signs in):

```
1. Cognito: create app client "<sub>"  (§6)  → note clientId, secret
2. DynamoDB: put CLIENT#<clientId> → tenant "<sub>"
3. Route 53: <sub>.app.example.com → ALB
4. Dirigible: POST /services/security/tenants { name, subdomain: "<sub>" }
5. Dirigible: POST /services/security/client-registrations  (name = "<sub>")   [or env at next deploy]
6. DynamoDB: put USER#<adminSub> / TENANT#<sub> → ["ADMINISTRATOR"]   (first member)
7. wait for provisioning: schema + datasource created, tenant status PROVISIONED
```

No user row is created in Dirigible's database at any step — membership is the DynamoDB item written in step 6.

---

## 13. User & role operations

| Operation | Action |
| --- | --- |
| Invite a person | Create/confirm them in the Cognito pool once (global identity), then add a membership item |
| Add to a tenant | `put USER#<sub> / TENANT#<sub-domain> → [roles]` |
| Change roles | Overwrite the `roles` list on that item |
| Remove from a tenant | Delete that membership item |
| **Eject immediately** | Delete the membership item **and** call `AdminUserGlobalSignOut` for the user **and** invalidate their Dirigible session — otherwise the change only takes effect at next token refresh |

State the SLA plainly to tenant admins: unless you eject, a role or membership change lands within about one token lifetime (default 1 h).

---

## 14. Enterprise SSO (later, no redesign)

When a tenant wants its own IdP (Entra ID, Okta, Google Workspace):

1. Add the IdP to the **pool** (SAML or OIDC).
2. On that **tenant's app client**, set `supported-identity-providers` to include it (and drop `COGNITO` if you want to force SSO).
3. Map the IdP's subject/email so the same `sub` join key resolves; the Lambda and DynamoDB membership are unchanged.

Because the topology is one-client-per-tenant, this is a per-client setting — no new pool, no structural change.

> **Caveat to test:** AWS's shared-cookie / cross-app-client SSO statement is worded for *local* pool users. A user who signs in through an external IdP has their session at that IdP, so the seamless cross-tenant hop of §10 may prompt again for federated tenants. Verify before promising it.

---

## 15. Validation checklist

- [ ] Pool is on **Essentials**; the pre-token trigger shows event version **V2_0** (V3_0 if M2M).
- [ ] `create-user-pool-client` for a tenant, `put-item` its `CLIENT#…→tenant` map, then sign in — decode the ID token and confirm `cognito:groups` holds *only that tenant's roles* and `dirigible:tenant` matches the host.
- [ ] Sign in as a non-member against a tenant's client — confirm Cognito denies (Lambda throw), no Dirigible session.
- [ ] Cross-tenant hop: after signing into tenant A, open tenant B in the same browser — confirm no credential prompt and a *different* authority set.
- [ ] Dirigible boots under the cognito profile (the required `jwk-set-uri` / `DIRIGIBLE_HOST` placeholders resolve).
- [ ] `GET /login/<sub>` redirects to `/oauth2/authorization/<sub>`; an unknown subdomain 404s.
- [ ] **Isolation test worth adding to the suite:** an integration test asserting that `anna` in tenant A and a different `anna` in tenant B get distinct data and workspaces. None of the existing multitenancy ITs covers same-identity-across-tenants.
- [ ] **Confirm the `@RolesAllowed` hypothesis** (§11) before relying on method-level role checks under the cognito profile: boot with `spring_profiles_active=cognito` and call a `@RolesAllowed`-only endpoint with a token holding no matching group.

---

## 16. References

**Companion documents**
- [`AWS_MULTITENANCY_RESEARCH.md`](AWS_MULTITENANCY_RESEARCH.md), [`AWS_MULTITENANCY_TARGET_ARCHITECTURE.md`](AWS_MULTITENANCY_TARGET_ARCHITECTURE.md), [`components/core/core-tenants/CLAUDE.md`](components/core/core-tenants/CLAUDE.md)

**In-repo**
- `components/security/security-cognito/` — the cognito profile: `CognitoSecurityConfiguration` (the `cognito:groups` → authorities mapper), `CognitoTenantFilter` (`custom:tenant` membership check), `CognitoLoginController` (`/login/{registrationId}` = subdomain), `CognitoLogoutSuccessHandler`, `application-cognito.properties`.
- `components/security/security-client-registration/` — `ClientRegistrationEndpoint` (`POST /services/security/client-registrations`), `DynamicClientRegistrationRepository` (registrationId = entity `name`; the static-map and `DIRIGIBLE_OAUTH_CUSTOM_CLIENTS` seeding).
- `components/engine/engine-security/.../oauth/ScopeRoleJwtAuthoritiesConverter.java` — scope → role for M2M.
- `components/core/core-tenants/.../endpoint/TenantEndpoint.java` — `POST /services/security/tenants`.
- `components/core/core-base/.../util/AuthoritiesUtil.java` — the `ROLE_<name>` authority format.

**AWS documentation**
- [Multi-tenant application best practices](https://docs.aws.amazon.com/cognito/latest/developerguide/multi-tenant-application-best-practices.html) — shared pool + cross-app-client session cookie.
- [Pre token generation Lambda trigger](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-pre-token-generation.html) — event versions, the modifiable-claims table, `groupsToOverride`.
- [Quotas in Amazon Cognito](https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html) — app clients per pool (1,000/10,000), callback URLs per client (100), groups per user (100), claim-changes (5,000).
- [User group](https://docs.aws.amazon.com/cognito/latest/developerguide/group-based-multi-tenancy.html) / [custom attribute](https://docs.aws.amazon.com/cognito/latest/developerguide/custom-attribute-based-multi-tenancy.html) / [custom scope](https://docs.aws.amazon.com/cognito/latest/developerguide/scope-based-multi-tenancy.html) multi-tenancy models.
