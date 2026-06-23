# Dirigible runtime on AlpineJS + Harmonia — research & plan

Research and implementation plan for a parallel, fully-embedded runtime UI stack.
Framework swap only; behaviour parity.

## Goal

Keep the **IDE** (Workbench, Monaco editors, entity/form/intent modelers) on **AngularJS + BlimpKit**.
Move the **runtime** — generated *and* custom apps, plus their **shell/dashboard**, views, forms and
BPM task forms — onto a self-contained **AlpineJS + Harmonia** stack, with SAP-icons replaced by
Harmonia/Unicons, and the whole stack **built into the fat jar** exactly as AngularJS + BlimpKit is
today. No new behaviour — same flows, same REST backend, same artifacts.

## Verdict

Feasible, and the architecture already permits it — but it is a large, multi-phase initiative, not a
re-skin. The single biggest lever is that the platform's view↔shell contract is a **framework-agnostic
message protocol** (`postMessage` hubs) plus **JSON / plain-JS registration**. Keep that protocol as
the invariant and you can swap *both ends* (shell and views) to Harmonia while the IDE keeps running on
Angular — and the two stacks can coexist during transition.

The real work is in three places: (1) reimplementing the runtime **shell/renderers** (the *receiving*
end of the hubs) in Harmonia, (2) a full **view template set** at parity, and (3) a Harmonia **forms**
runtime. Plus embedding + icons + a couple of genuine Harmonia gaps (split panes).

> **Update after studying the reference implementation.** Two assumptions above are now corrected by a
> working sample (see the new section **"Reference implementation: codbex-athena-app"** below):
> Harmonia **does** ship a split component (`x-h-split`) — the split-pane gap is closed — and a full,
> reusable **Harmonia runtime dashboard/shell already exists** in that repo (sidebar + toolbar +
> breadcrumb + responsive collapse + theming + a `fetch`-based entity client + a 422 error-mapping form
> base). That collapses the single biggest cost (Workstream 3 / Phase 1) from "build from scratch" to
> "adopt and adapt." It also reveals a **different shell architecture** than this plan assumed — a
> single-page, client-side-routed SPA served as a plain Dirigible web resource, **not** the
> iframe-per-view + `postMessage`-hub seam. Read that section before committing to the phasing.

## Reference implementation: codbex-athena-app

`github.com/codbex/codbex-athena-app` (the `codbex-athena-app/` project inside it) is a **complete,
working Alpine + Harmonia runtime application** built exactly the way this initiative needs — and per the
direction *its dashboard/shell can be adopted directly*, not re-derived. It is a Dirigible project
(`project.json` with a `guid` + git dependency on `codbex-invoices`; served as a web resource under
`/services/web/codbex-athena-app/`) and it ships, in its own `.claude/skills/`, an 88 KB Harmonia
component reference and an Alpine-architecture guide that together encode the conventions below. Treat
this repo as the canonical pattern source for Workstreams 1–5.

### What it proves / changes vs. the original plan

1. **Split panes are NOT a gap.** Harmonia ships `x-h-split` + `x-h-split-panel`, and athena uses it for
   the **shell layout itself** — the sidebar/main division is a horizontal split. Real usage from its
   `index.html`:
   ```html
   <div x-h-split class="size-full" data-orientation="horizontal" data-variant="border">
     <div x-h-split-panel :data-hidden="hiddenPanels.left" data-max="256" data-min="200"
          data-locked="true" data-gutterless="true" x-ref="sidebarPanel"> … sidebar … </div>
     <div x-h-split-panel> … main … </div>
   </div>
   ```
   Documented attributes: `data-orientation="horizontal|vertical"` (h is default), `data-default-size`
   (percentages, panels should sum to 100), `data-min` / `data-max` (px clamps), `data-locked="true"`
   (non-resizable pane), `data-gutterless="true"` (hide the drag handle), `data-variant="border"`,
   `:data-hidden` (collapse a pane reactively). This covers **both** uses the plan flagged — master-detail
   views *and* shell layout — so the `platformSplit` port is unnecessary. Decision in Phase 1 is resolved:
   use `x-h-split`.

2. **The runtime dashboard/shell already exists and is reusable.** `js/components/layout/appShell.js` is
   a single `Alpine.data('app', …)` component providing everything Workstream 3 lists: sidebar nav
   (`x-h-sidebar` + `x-for` over a `nav` array), a top `x-h-toolbar` with `x-h-breadcrumb` trail derived
   from the route, a notifications popover, an avatar `x-h-menu` with a light/dark/auto theme submenu, and
   **responsive collapse** driven by `Harmonia.getBreakpointListener((isNarrow)=>…, 1024)` that physically
   re-parents the sidebar into an `x-h-sheet` drawer below 1024 px. Adopt this file as the starting shell;
   the per-app parts that change are just the `nav`, `routeTitles`, and `sections` maps.

3. **The shell architecture is a client-routed SPA, not the iframe+hub seam.** This is the biggest
   architectural divergence from the plan. athena has **no iframes and no `postMessage` hubs**. It is one
   HTML page; **Pinecone Router** (v7.5.0, **hash mode**, `basePath: '/services/web/<project>'`) declares
   routes as `<template x-route="/customers" x-handler="App.routes.customers"
   x-template.target.app="./views/customers.html">` and renders the matched **view fragment** into a
   single `<div id="app">`. Each view's root element is an `Alpine.data(...)` page component whose `init()`
   runs on entry and `destroy()` on exit — fresh per-visit state for free. Implication: this model
   **replaces `resources-dashboard` wholesale** for Harmonia apps rather than slotting Harmonia views into
   the existing Angular shell over the hub protocol. The two models are mutually exclusive per app; choose
   deliberately (see "Reconciling the two shell models" below).

4. **Icons are Lucide + Harmonia built-ins, not Unicons.** athena loads `lucide` UMD and uses
   `<i data-lucide="file-text">` for nav/content icons plus Harmonia's built-in `x-h-icon.*` set
   (`.home`, `.bell`, `.close`, `.circle-info`, `.search`, `.edit`, `.trash`, ~30 glyphs — no Lucide
   dependency for those). It re-runs `lucide.createIcons()` on `alpine:initialized` and every
   `pinecone:end` (and pages call a `refreshIcons()` mixin after DOM-mutating actions, since injected
   markup isn't auto-processed). Workstream 6 should re-scope from "→ Unicons" to "→ Lucide + Harmonia
   built-ins" unless Unicons is a hard brand requirement.

5. **The "EntityClient" and i18n/error layers already exist** (Workstream 2 is largely written):
   - `js/services/api.js` — a plain `fetch` client `App.services.api` with `get/post/put/delete`,
     `credentials: 'same-origin'`, a structured `ApiError` (`httpStatus`, `errorType`, `errorCauses[]`),
     status→errorType mapping, and **named base URLs** pointing straight at the **generated Java REST
     controllers**: `/services/java/<module>/gen/<pkg>/api/<entity>` (e.g.
     `/services/java/codbex-partners/gen/codbex_partners/api/customers`). This is exactly the REST surface
     the EDM/intent generators emit — confirming the backend is reused unchanged.
   - `js/services/apiError.js` — a **user-safe, localizable** message catalog keyed by `errorType`, plus a
     per-field catalog keyed by 422 cause type (`NotNull`, `Pattern`, `Email`, `Size`, `Duplicate`, …).
     The dev-facing `errorMessage` is logged to console only, never shown. This is the i18n seam: "swap
     the flat maps for a locale lookup in one place."
   - `js/services/formValidation.js` — a tiny schema validator (`required/minLength/min/max/email/pattern`)
     returning `{ errors, valid }`; client-side pre-validation before the server round-trip.

### Conventions to carry into the templates (Workstreams 4 & 5)

- **`window.App = { services, routes, utils }`** global namespace (CDN/no-build style); stores via
  `Alpine.store(...)`; pages via `Alpine.data(...)`; services are framework-agnostic (no Alpine inside).
  Registration uses `document.addEventListener('alpine:init', …, { once: true })`.
- **Page-base mixins** spread into every page component: `basePage()` (just `refreshIcons()`) and
  `baseFormPage()` which adds the whole server-error UX — `applyApiError(err)` routes 422
  `ValidationError` into per-field `errors` (honoring a per-page `fieldMap` for name mismatches) and
  everything else into a localized banner; `mapCauses()`, `applyLoadError()`, `scrollToSummary()`,
  `returnToParam()`/`navigateBack()` for `?returnTo=` deep-link continuation. **Caveat from the file:**
  do **not** put ES6 getters in a spread mixin — the spread invokes them once and breaks Alpine
  reactivity; define getters directly on the page component.
- **List-view pattern** (`customersPage.js`) is a self-contained client-side **state machine**
  (`loading | error | empty | no-results | default`) with search, multi-column tri-state sort
  (`asc → desc → none` via `cycleSort`), and client pagination with an ellipsis `pageNumbers` builder.
  This is the parity target for the generated `list` / `master-list` templates.
- **Toasts** are an `Alpine.store('toasts')` queue rendered through `x-h-notification-overlay` templates;
  the richer `$notifications.add({ template, data, timeout })` magic is also available.
- **Empty/error states** use `x-h-info-page` (icon/title/description/actions) — the answer to the plan's
  "message page (empty-state)" minor gap. Dashboards use `x-h-tile` + the `tile-*`/`grid-cols-1..12`
  utilities.

### Embedding facts (Workstream 1)

- **Script load order is load-bearing** and documented in athena's `index.html`: (1) Pinecone Router
  plugin, (2) app code via `defer` in document order — `app.js` namespace first, then `utils → services →
  stores → router/handlers → appShell → page components`, (3) **Alpine LAST**, then **Harmonia**
  (the IIFE bundle auto-starts Alpine — *do not call `Alpine.start()` yourself*), (4) Lucide. Cache-bust
  query strings (`?v=N`) are used per file.
- athena pulls Alpine `3.15`, `@codbex/harmonia@1.24.1` (js+css), Pinecone `7.5.0`, Lucide from **unpkg
  CDN**. For the fat jar these become **webjars or vendored `dist/`** (the plan's preferred option) — the
  CSP/offline requirement makes CDN a non-starter in-product, but the file layout is otherwise identical.
- Harmonia's CSS is a **fixed, curated Tailwind subset** (spacing 0–12, semantic colors only, `sm/md/lg/xl`
  only, no arbitrary values, no hue scales). There is **no build step**, so unknown utilities silently
  render as nothing. Generated templates must stay inside the allow-list or use inline `style=`/CSS
  variables. Theming is via CSS variables (`--primary`, `--background`, …) and
  `Harmonia.setColorScheme('light|dark|auto')` (persisted to `localStorage` under
  `codbex.harmonia.colorMode`) — this is the concrete **theming bridge** for Workstream 4/Phase 4.

### Reconciling the two shell models (key decision)

The plan's seam was "framework-agnostic `postMessage` hubs + iframe-per-view," letting a Harmonia view run
inside the Angular shell and vice-versa during transition. The reference app instead is a **self-contained
SPA** with client-side routing and direct `fetch` to the generated controllers — simpler, faster, and
already built, but it does **not** speak the platform hub protocol and does **not** embed in the Angular
dashboard. Two viable paths, to decide before Phase 1:

- **(A) Adopt the athena SPA model** as the runtime dashboard (recommended by the evidence): generated apps
  become standalone Harmonia SPAs served at `/services/web/<project>/`, replacing the Angular
  `resources-dashboard` for those apps. Fastest route to parity; loses in-Angular-shell coexistence and the
  hub protocol. Phase 0 spike changes to "stand up the athena shell against a generated module's REST API."
- **(B) Keep the hub/iframe seam** from the original plan and *borrow* athena's component patterns, entity
  client, form base, and `x-h-split` usage into hub-aware Harmonia views. Preserves coexistence and the
  conformance-tested protocol; forgoes the ready-made shell (re-implements Workstream 3 around the hubs).

Either way, athena supplies the components, the split usage, the `fetch` entity client → generated Java
controllers, the error/i18n catalog, the form-error mapping, and the list-view state machine. The choice
is only about the **shell boundary**, not the view internals.

## How the platform is wired today

Two shells, not one. The **IDE shell** (`shell-ide`) and the **runtime/app dashboard**
(`resources-dashboard` + the shared shell pieces in `platform-core`) are *both* AngularJS + BlimpKit
today. Each **view is its own HTML page loaded in an iframe**; it talks to its host shell only through
hubs and registers via plain manifests. That iframe boundary is what makes a per-stack swap possible.

**The invariant contract (keep):**
- `postMessage` hubs: Dialog / Notification / Layout / Theming / Message
- Views register via `.extension` JSON + `getView()` / `getPerspective()` returning plain objects
- Params via `data-parameters` on the iframe (`getViewParameters()`)
- `platform-links` = server-side `<meta>` → script/css bundle injection (per category)
- REST backend (`/services/java/.../<Entity>Controller`) and the EDM/intent generators

**AngularJS-bound (needs a Harmonia twin):**
- **BlimpKit** — all `bk-*` directives
- **Shell renderers** — `shell.js` / `layout.js` / `dialogs.js` (the things that *draw* dialogs,
  windows, tabs, regions)
- **EntityService** — Angular provider over `$http`
- **LocaleService** + the `| t` filter
- **platformSplit** — the `<split>` resizable panes

### Reuse map

| Platform capability | Reusable as-is? | Notes for the Harmonia stack |
|---|---|---|
| Dialog/Notification/Layout/Theming/Message hubs (*sender side*) | ✔ agnostic | Plain ES6 classes over `postMessage`; instantiate from Alpine directly. |
| View / perspective / extension registration | ✔ agnostic | `.extension` JSON + a `getView()` module; shell only reads the returned object. |
| `getViewParameters()` / iframe `data-parameters` | ✔ agnostic | Same utility works in any framework. |
| `platform-links` injector | ✔ agnostic | Add categories (`harmonia-view/-perspective/-shell`) bundling Alpine+Harmonia+hubs, not Angular. |
| EDM / intent / DAO / REST generators & Java backend | ✔ agnostic | Emit `.model`/`.form`/`.java` — UI-neutral. Only UI *template selection* changes. |
| i18n | ~ partial | `i18next` is already global & agnostic; only the Angular `LocaleService`/`| t` wrapper is replaced (call `i18next.t()` + an Alpine helper). |
| EntityService | ~ trivial port | Re-express the same REST calls as a plain `fetch` client. |
| Shell renderers, BlimpKit, split panes | → reimplement | The bulk of the effort — see workstreams 1, 3, 5. |

## Harmonia: fit & gaps

`@codbex/harmonia` (MIT, v1.24.x) — "46+ components for Alpine.js". Alpine `^3.15` peer dep; Tailwind-
built CSS shipped as `dist/harmonia.css`; dual **UMD** (`harmonia.min.js`) + **ESM** bundles; "no build
step — just drop it in" (CDN/script-tag friendly); CSS-variable theming with a theme generator; deps
`@floating-ui/dom`, `nouislider`; default icon set (Unicons-style).

| Generated UI need | BlimpKit today | Harmonia | |
|---|---|---|---|
| Data table | `bk-table` | Table | ✔ |
| Text / number / date / checkbox / select | `bk-input`, `bk-select`, `bk-checkbox` | Input, Input Number, Date Picker, Checkbox, Select | ✔ |
| Button / toolbar | `bk-button`, `bk-toolbar` | Button, Toolbar | ✔ |
| Dialog / window / popover / menu / list | `bk-dialog`, `bk-popover`, `bk-menu`, `bk-list` | Dialog, Popover, Menu, List | ✔ |
| Pagination / notifications / tabs / badge / sidebar | bk equivalents | Pagination, Notifications, Tabs, Badge, Sidebar | ✔ |
| **Split / resizable panes** (master-detail + shell layout) | `platformSplit` `<split>` | `x-h-split` + `x-h-split-panel` | ✔ (was thought a gap — see reference impl) |
| Color input | `bk-input type=color` | native `<input type=color>` | minor |
| Scrollbar styling | `bk-scrollbar` | native/CSS | minor |
| Message page (empty-state) | `bk-message-page` | compose from primitives | minor |

Component coverage is strong. The split-pane "gap" that earlier framings (and this table's first draft)
flagged is **not real**: Harmonia ships `x-h-split`/`x-h-split-panel` and the reference app uses it for
both master-detail *and* the shell layout (see "Reference implementation: codbex-athena-app"). No
`platformSplit` port is needed.

## Target architecture

- **Keep (Angular):** the IDE — `shell-ide`, Monaco editors, entity/form/intent modelers, all Workbench
  perspectives. Untouched. BlimpKit stays for the IDE.
- **Keep (shared, agnostic):** the hub *protocol*, the `.extension`/`getView()` registry, the
  `platform-links` injector, `getViewParameters`, REST endpoints, and every model-layer generator.
- **Build (Harmonia):** a runtime **Harmonia Dashboard** (shell + renderers), a runtime platform layer
  (i18n/entity/util), a generated-view **template set**, a **forms** runtime, icon migration, and the
  embedded asset bundle.

Because both shells speak the same hub protocol, a Harmonia view can run in the Angular shell (useful
for incremental bring-up) and an Angular view could run in the Harmonia shell — the swap is per-stack,
not all-or-nothing.

## Workstreams

Effort: S / M / L / XL (relative, not estimates).

| # | Workstream | Effort | What it entails |
|---|---|---|---|
| 1 | **Embed the stack** | M | Webjars (or vendored `dist/`) for `alpinejs` + `@codbex/harmonia` (js+css) + Unicons; new `platform-links` categories `harmonia-view/-perspective/-shell` bundling Alpine+Harmonia+the (reused) hub scripts and *excluding* Angular/BlimpKit. Node toolchain already a build prereq. |
| 2 | **Runtime platform layer** | M | Plain-JS `EntityClient` (fetch, same REST surface as `EntityService`); an i18n helper over the already-global `i18next` (+ an Alpine magic like `$t`); conventions for using the hubs + `getViewParameters` from Alpine. Port the new `process-tasks` directive to an Alpine component. |
| 3 | **Harmonia Dashboard (shell)** | XL | The crux. Reimplement the runtime shell as Alpine+Harmonia: perspective host, layout (regions/tabs/**split**), and the *receiving* renderers for `showAlert/showDialog/showWindow/showBusy`, notifications, theming application, and view iframe hosting — all listening on the existing hub topics so it hosts any view. Resolve the split-pane gap here. |
| 4 | **Generated view template set** | XL | New module `template-application-ui-harmonia-java` mirroring `template-application-ui-angular-java`'s `getSources()`/collections, re-skinning every view (list, manage, master-list/-manage, detail, main-details, dialogs, report/-table/-chart/-file, perspective, navigation) to Alpine+Harmonia on `EntityClient`+i18n+hubs. Same Velocity model/params — only template files + the platform-links category differ. |
| 5 | **Forms runtime** | L | A Harmonia counterpart to `template-form-builder-angularjs` so runtime forms — including **BPM task forms** — render in Harmonia from the same `.form` artifact. The form-builder *editor* stays in the IDE (Angular); only the generated runtime form changes. |
| 6 | **Icons → Lucide + Harmonia built-ins** | M | Map `sap-icon--*` usages and perspective SVGs to Harmonia's built-in `x-h-icon.*` set + Lucide (`<i data-lucide="…">`), per the reference app — *not* Unicons (revised). Embed the Lucide UMD + the (already-bundled) Harmonia glyphs; re-run `lucide.createIcons()` on `pinecone:end`/after DOM-mutating actions. Unicons remain available (`/services/web/resources/unicons/...`) if a model references them. |
| 7 | **Generation wiring** | S | Register the new template in `service-generate`; add a stack choice to the EDM/intent generation recipe (the intent `.settings` already selects templates per type). Generators are reused — they're at the model layer. |
| 8 | **Parity tests + docs** | L | Selenide ITs that drive the Harmonia runtime through the same flows as the Angular ITs (CRUD, filter, dropdowns, master-detail, dialogs, task surfacing, forms). CLAUDE.md notes for the new stack + the protocol contract. |

## Phasing

**Chosen path: (A) adopt the athena SPA shell.** The runtime becomes a self-contained Alpine + Harmonia
single-page app, served as a plain Dirigible web resource at `/services/web/<project>/`, routed
client-side by Pinecone (hash mode), talking directly over `fetch` to the generated Java controllers.
This **replaces `resources-dashboard`** for generated/custom apps; the IDE stays AngularJS + BlimpKit.
Consequences of this choice that reshape the phasing:

- **What's dropped:** the iframe-per-view model, the `postMessage` hub *receiving* renderers, and the
  hub-protocol conformance test. Coexistence is no longer "Harmonia views inside the Angular shell" — the
  two stacks coexist by living at **different URLs** (IDE shell vs. per-app SPA), not via a shared seam.
- **What gets cheaper:** the old Phase-1 crux (build shell + renderers + layout + split from scratch) is
  now "adopt and parameterize athena's `appShell.js`." Split, breadcrumb, responsive drawer, theming menu,
  entity client, error/i18n catalog and form-error mapping are **already written** — they move from
  "build" to "extract + harden."
- **What's de-risked earliest:** because no generator work is needed to stand up a real app, the spike
  validates embedding + REST parity + the full shell in one shot.

0. **Spike — stand up the athena shell against a real generated module.** Vendor Alpine `3.15` +
   `@codbex/harmonia` + Pinecone `7.5.0` + Lucide as **webjars** (no CDN — CSP/offline), with the
   load order athena documents (plugins → app code by `defer` document-order → Alpine → Harmonia → Lucide;
   no `Alpine.start()`). Copy athena's `index.html` shell, `appShell.js`, `api.js`, `apiError.js`,
   `baseFormPage`/`basePage`, `formValidation.js`; point a named base URL at an **existing generated
   module's** controllers (e.g. `/services/java/<module>/gen/<pkg>/api/<entity>`); hand-write **one list
   view + one create/edit form view** against the real REST API; serve at `/services/web/<project>/`.
   Validates webjar embedding, the `x-h-split` shell, `fetch`→generated-controller parity, theming, and
   the 422 error-mapping form base — **with zero generator/template work.** De-risks everything cheaply.
1. **Productize the runtime shell.** Turn athena's `appShell.js` into a **reusable, embeddable Harmonia
   dashboard** where the per-app parts (`nav`, `routeTitles`, `sections`, branding) are config/generated,
   not hand-edited. Finalize the **theming bridge** (platform theme switch ↔
   `Harmonia.setColorScheme('light|dark|auto')` + CSS-variable tokens), the responsive sidebar↔`x-h-sheet`
   drawer, and breadcrumb derivation. End state: a generated app boots into this shell and renders the
   Phase-0 views. (Replaces Workstream 3's from-scratch build.)
2. **Full view template set.** New module `template-application-ui-harmonia-java` mirroring
   `template-application-ui-angular-java`: generate every view type into `gen/` as **Harmonia view
   fragments + `<template x-route>` declarations + `Alpine.data` page components**, built on the
   list-view **state machine** (`loading|error|empty|no-results|default`, search, tri-state sort,
   client pagination) and `baseFormPage`. Same Velocity model/params — only the template files and the
   embedding category differ.
3. **Forms + process tasks.** Harmonia form runtime rendering from the same `.form` artifact (incl. **BPM
   task forms**, completing via `/services/inbox/tasks/{id}` and self-closing), plus the Alpine
   `entity-process-tasks` equivalent gated on the `hasProcess`/`ProcessId` flag.
4. **Icons, theming, polish.** Migrate `sap-icon--*`/perspective SVGs to **Lucide + Harmonia built-ins**
   (re-run `lucide.createIcons()` on `pinecone:end`/after DOM mutations); finalize the theme bridge;
   empty-states via `x-h-info-page`; scrollbars; and an audit that all generated markup stays inside
   Harmonia's curated Tailwind subset (no arbitrary/hue/`>12` utilities).
5. **Wire generation + parity ITs.** Register the template in `service-generate`; add the stack choice to
   the EDM/intent recipe. Build the Selenide parity suite — **note the selectors change**: a hash-routed
   SPA with no iframes (assert on `x-h-*` DOM at `/services/web/<project>/#/...`, not iframe/BlimpKit
   selectors). Docs + CLAUDE.md notes for the new stack.

## Key decisions & risks

- **Split/resizable panes** — RESOLVED: Harmonia's `x-h-split` covers master-detail *and* the shell
  (proven in the reference app). Use it directly; no port or third-party lib.
- **Shell model — SPA vs. hub/iframe seam** — the reference dashboard is a self-contained client-routed
  SPA (Pinecone, no hubs/iframes), not the hub-protocol view model this plan assumed. Pick path (A) adopt
  the athena shell or (B) keep the seam and borrow athena's patterns. Decide before Phase 1 — see
  "Reconciling the two shell models."
- **Theming bridge** — map the platform's theme switching (ThemingHub) onto Harmonia's CSS-variable
  themes so light/dark parity holds.
- **Embedding model** — webjar of prebuilt `dist/` (like the BlimpKit webjar) vs. a Tailwind build step
  in the Maven build. Webjar is simplest and keeps the IDE's BlimpKit path untouched.
- **Coexistence (under path A)** — the two stacks coexist by **URL**, not a shared seam: the IDE stays on
  the Angular shell while each generated/custom app is a standalone Harmonia SPA at
  `/services/web/<project>/`. No hub-protocol conformance test is needed; the contract that must not drift
  is instead the **REST surface** of the generated controllers the SPA's entity client calls.
- **Two stacks to maintain** — until the Angular runtime templates are retired, every runtime UI change
  risks needing both. Keep behaviour parity strict to limit divergence.
- **Forms are runtime artifacts** — easy to forget; task forms must render in Harmonia or the BPM flows
  break under the new shell.

## Explicitly out of scope

- The **IDE** itself — Workbench, Monaco editors, entity/form/intent modelers, git/database/operations
  perspectives — stays AngularJS + BlimpKit.
- The **model-layer generators** (EDM/intent/DAO/REST) and the **Java backend** — reused unchanged; this
  is a UI-layer initiative only.
- No new runtime features or behaviour — strict parity with the current generated/custom app experience.

---

Sources: code analysis of `platform-core` (hubs, locale, view/perspective, platform-links),
`resources-dashboard` (EntityService), the `template-application-ui-angular-java` templates and
`service-generate`; Harmonia from `github.com/codbex/harmonia`, the `@codbex/harmonia` package metadata,
and codbex.com/harmonia.
