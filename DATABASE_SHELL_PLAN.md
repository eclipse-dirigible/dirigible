# Database shell (Harmonia) — plan, feasibility & cost

A new **Database** shell at the application layer: a Harmonia/Alpine re-imagining of the IDE's
Database perspective, scoped to **support essentials only** — browse the schema, look at data, run a
SQL fix. It gives operators and support engineers a way to touch the database without opening the
IDE, exactly the way the Monitoring shell answers "is the system healthy?" without the IDE.

Status: **in delivery.** PR 0 is the Harmonia bump (#6656), PR 1 the URL-layer gate (#6657), PR 2
this shell. Prior art: the Monitoring shell (`components/resources/resources-monitoring`,
PRs #6613–#6615) — the only existing single-purpose operations shell and the construction template
this plan clones.

### Corrections to this plan, found while building it

- **The bump is to 2.9.0, not 2.10.0.** 2.10.0 is published on npm but has no webjar on Maven
  Central, and 2.9.0 carries every breaking change that mattered (the tree rewrite included). The
  2.10.0 delta — a cross-origin iframe crash fix and an inverted `getBreakpointListener` third
  argument — is a follow-up; when its webjar lands, the six shells calling that listener must pass
  `true`.
- **The `data-disabled` sweep was a bug fix, not a rename.** `data-disabled` on a root `x-h-select`
  never did anything, even on 2.6.0: it was always a per-*option* attribute, and the whole control
  is disabled through native `disabled` on `x-h-select-input`. Preview-mode and read-only dropdowns
  in the generated apps were never actually disabled. They are now.
- **Two breaking changes the plan did not list** turned out to matter: the 2.7.0 slot-picker stopped
  collapsing its day columns on narrow screens by itself (`.responsive` restores it), and 2.7.0
  moved an option's label into a text column, which broke one IT assertion that demanded exactly one
  `<span>` per label.
- **Lazy tree loading must not own the expanded state.** A 2.9 tree item detects its own children
  and owns whether it is expanded, so a second owner in the store fights it. The shell instead
  renders one placeholder child under an unloaded node — which is what gives the row its chevron
  before anything has been fetched — and the `tree-item-click` handler only loads.

## Goal

- A standalone Harmonia shell served at `/services/web/database/`, registered on `platform-shells`
  (shell id `databaseShell`, icon `database`, order 38 — between Monitoring 37 and the IDE 40).
- Two pages: an **Explorer** (datasource → schema → tables/views/procedures → columns tree, "show
  contents" grid, DDL view) and a **SQL Console** (statement area, results grid, messages strip).
- **Zero new backend.** Every read and execute goes through endpoints that already ship in
  `components/data/*`, all method-gated `@RolesAllowed({ADMINISTRATOR, DEVELOPER, OPERATOR})`.
- Role gate: `database.access` on `/database/` for ADMINISTRATOR / OPERATOR / DEVELOPER — the same
  trio the endpoints declare, so the shell never renders a page its user cannot use.

## Verdict

**Highly feasible, low risk.** The backend surface exists 1:1; the shell skeleton, shared runtime,
page pattern, registration plumbing and IT pattern are all proven by the Monitoring shell; Harmonia
(as of 2.10.0) carries every component the UI needs (`tree`, `table`, `split`, `tabs`, `select`,
`menu`/`popover`). Estimated cost: **~2.5–3k LOC across 2–3 PRs, roughly 1–1.5 weeks** of focused
effort, of which the single real prerequisite — the Harmonia webjar bump 2.6.0 → 2.10.0 — carries
most of the risk.

## Scope

### In (support essentials)

- Datasource picker (names from `GET /services/data/metadata`), current selection in an Alpine
  store, persisted to localStorage (the IDE does the same via
  `${brand.prefix}.view-db-explorer.database`).
- Lazy metadata tree: schema → tables / views / procedures → columns. Fixed depth, so plain nested
  `x-for` — no recursive templates needed.
- "Show contents" — a generated `SELECT` executed through `/query`, rendered in an `x-h-table` with
  dynamic columns and client-side pagination.
- Object DDL via `GET /services/data/definition/{ds}/{schema}/{structure}`.
- SQL Console: monospace textarea (NOT Monaco — see decisions), Ctrl/Cmd+Enter to execute, results
  grid, update counts / errors in a messages strip, statement text persisted in localStorage.
- Explorer row actions: Show contents, Copy name, Generate SELECT into the console.
- Metadata cache refresh (`GET /services/data/metadata/invalidate-cache`).
- i18n en-US + bg-BG, following the `i18n/<locale>/<project>.json` aggregator convention.

### Out (deliberate — decided 2026-08-11)

- **Import / export** (sync + async), **anonymization**, **data transfer** (the WebSocket), **schema
  export/import processes** — not support essentials; also exactly the endpoints restricted to
  ADMINISTRATOR/OPERATOR-only, so cutting them keeps the role story uniform.
- **Datasource CRUD** (`/services/data/sources`) — the shell consumes datasources, it does not
  manage them.
- **Row-edit dialogs** (the IDE's `result-view-crud` + the server-side JS `databaseTable.js` path) —
  the SQL console covers support writes; optional later phase.
- **Monaco** — a support console does not justify embedding a multi-MB editor in an app-layer shell.
- Re-implementing anything the Workbench Database perspective does deeply (script generation beyond
  a SELECT, project exports, topology). Each page links to the IDE instead — the Monitoring shell's
  scope line, applied here.

## Backend inventory (all existing, no changes required)

Prefix from `BaseEndpoint`: `/services/data/`. Datasource/schema are **always path variables** —
no server-side "current datasource" session state; every request is self-describing.

| Need | Endpoint | Roles |
|---|---|---|
| List datasources | `GET /services/data/metadata` | ADMIN, DEV, OPERATOR |
| Datasource → schemas | `GET /services/data/metadata/{ds}` | ADMIN, DEV, OPERATOR |
| Schema → objects | `GET /services/data/metadata/{ds}/{schema}` | ADMIN, DEV, OPERATOR |
| Object metadata (columns…) | `GET /services/data/metadata/{ds}/{schema}/{structure}?kind=` — structure name **Base64-encoded** in the path | ADMIN, DEV, OPERATOR |
| Object DDL | `GET /services/data/definition/{ds}/{schema}/{structure}` | ADMIN, DEV, OPERATOR |
| Run SELECT | `POST /services/data/{ds}/query` — body raw SQL, `Content-Type: text/plain` | ADMIN, DEV, OPERATOR |
| Run UPDATE / DDL | `POST /services/data/{ds}/update` | ADMIN, DEV, OPERATOR |
| Call procedure | `POST /services/data/{ds}/procedure` | ADMIN, DEV, OPERATOR |
| Refresh metadata cache | `GET /services/data/metadata/invalidate-cache` | ADMIN, DEV, OPERATOR |

### Server contracts the new client must knowingly inherit

These are de-facto contracts of `DatabaseExecutionEndpoint` / `DatabaseExecutionService`, not bugs
to fix in this effort:

- **Statement dispatch is a prefix sniff**, not parsing (the IDE's `result.js` does the same):
  `select …` → `/query`, `call …` → `/procedure`, `query: …` → `/query` with the prefix stripped
  (NoSQL, e.g. MongoDB find), `update: …` → `/update` stripped, everything else → `/update`.
  Replicate exactly.
- **Response format branches on an *exact* `Accept` match** (`text/plain` → monospaced table,
  `text/csv` → CSV, anything else → JSON). Send a plain `Accept: application/json` and always take
  the JSON branch.
- **Results are silently capped** server-side at `DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT`
  (default 1000) with **no truncation marker in the payload**. The grid must label honestly:
  "showing N rows — the server may cap results" (at least when N looks like a round cap). The IDE
  never surfaced this; the support shell should.
- **Procedure results are double-encoded**: a JSON array whose elements are JSON *strings* —
  `JSON.parse` each element.
- The service splits multi-statement bodies on `;` (on `--` when the body contains
  `CREATE PROCEDURE`/`CREATE TRIGGER`) and streams results per statement; an error mid-stream
  surfaces as a 500 on an already-committed response — render whatever arrived plus the error.
- **Encode names consistently from day one.** Metadata/definition paths take the structure name
  Base64-encoded; anywhere else, encode per path segment. The IDE breaks on schema/table names
  containing `.` (it splits `"schema.table"` on the dot); greenfield code must not copy that.

### Optional hardening (recommended, own small PR)

`/services/data/**` has **no URL-layer gate** in `HttpSecurityURIConfigurator` — it falls through to
`AUTHENTICATED_PATTERNS` (`/services/**` → `authenticated()`), so method-level `@RolesAllowed` is
the only rejection. Mirror what the Monitoring shell did in #6613: add `/services/data/**` to the
operations `RoleGate` (ADMINISTRATOR/DEVELOPER/OPERATOR) ahead of the catch-all, and extend
`HttpSecurityURIConfiguratorTest`'s matrix. ~60 LOC of defense-in-depth. Note the endpoints are
mixed-role (export/import/anonymize are ADMIN/OPERATOR-only, stricter than the gate) — the URL gate
must use the **widest** legitimate set and leave the finer method-level checks in place.

**Independent platform gap found while mapping — report it privately, do NOT open a public issue.**
The `/websockets/data/transfer` handler (`DataTransferWebsocketHandler`, `data-transfer`) carries no
role check at all and `/websockets/**` is merely `authenticated()` — any authenticated user can
drive data transfer, unlike every REST sibling in `components/data/` (all ADMINISTRATOR + OPERATOR).
`SECURITY.md` routes undisclosed vulnerabilities to the Eclipse Security Team or a committers-only
Bugzilla entry, so this belongs there rather than in the issue tracker. It is deliberately kept out
of PR 1: that PR is a strict no-op for every existing caller, while fixing this changes who may use
a feature, and the right role set is a policy call (DEVELOPER opens the IDE's Transfer view).

## Target architecture

New module **`components/resources/resources-database`** (`dirigible-components-resources-database`),
web assets under `src/main/resources/META-INF/dirigible/database/`. A straight clone of the
`resources-monitoring` skeleton — the only live single-purpose operations shell (admin / personal /
application are *host* shells whose large `appShell.js` exists to mount extension-contributed
perspectives; not the right template).

```
pom.xml                                   (no webjar deps needed — unlike monitoring's bpmn-visualization)
META-INF/dirigible/database/
├── index.html                            sidebar + toolbar chrome, Pinecone routes into #app
├── database.access                       /database/ → ADMINISTRATOR, OPERATOR, DEVELOPER
├── configs/shell.js                      id 'databaseShell', path, label, icon 'database', order 38
├── extensions/shell.extension            platform-shells registration
├── css/app.css                           shell-local styles only
├── i18n/en-US/database.json + bg-BG/     namespace = file basename
├── js/
│   ├── config.js                         window.App.config { projectName, basePath, restBase: '' }
│   ├── appShell.js                       thin: currentPath tracking + Harmonia breakpoint drawer
│   ├── services/dbops.js                 one wrapper per endpoint + soft(); owns the dispatch sniff,
│   │                                     Base64/segment encoding, procedure double-decode
│   ├── stores/
│   │   ├── explorer.js                   datasource list + selection (localStorage), lazy tree state,
│   │   │                                 contents grid state, DDL state
│   │   └── sql.js                        statement text (localStorage), execute, results, messages
│   └── components/pages/
│       ├── explorerPage.js
│       └── sqlPage.js
└── views/
    ├── _explorer.html                    x-h-split: tree (x-h-tree, nested x-for) | contents/DDL tabs
    └── _sql.html                         textarea + run toolbar + results x-h-table + messages
```

Everything cross-cutting comes from the **shared** runtime at
`/services/web/application-core/shell/` by absolute URL (app.js, i18n/api/apiError/format/branding
services, theme/locale/currentUser/shells stores, base css) — no per-project copies. Script order in
`index.html` is load-bearing (Pinecone → shared runtime → config → local services/stores/pages →
appShell → Alpine → Harmonia bundle, which auto-starts Alpine → Lucide last).

Registration edits outside the module: `components/pom.xml` (module + dependencyManagement),
`components/group/group-ui/pom.xml` (assembly dependency), `resources-home`'s `home.js` (icon map,
description, SECONDARY list — a support tool is a secondary destination, like Monitoring).

### Page conventions (carried from Monitoring)

- Per page: an Alpine **store** (data + async actions, never touches DOM), an Alpine **page
  component** (`Alpine.data('explorerPage')` — formatting, derived getters, calls
  `$store.explorer.load()` in `init()`), and a **view fragment**.
- Every endpoint read goes through `dbops.js`; aggregate reads wrap in `soft()` so an unavailable
  datasource degrades to a marked tile, not an empty screen.
- Stable ids for the IT: sidebar buttons `database-nav-<section>`, page roots
  `database-<section>-page`.
- Alpine/Lucide trap: a bound `:data-lucide` goes on `<svg x-h-lucide>`, never `<i x-h-lucide>` —
  the plugin replaces the element and the throw silently aborts Alpine's walk (the failure mode
  `MonitoringShellIT` exists to catch).
- Tree: Harmonia ≥2.9 API (`x-h-tree-row` / `x-h-tree-label` / `x-h-tree-actions`, selection driven
  by the `tree-item-click` event — the tree keeps no selection model of its own). Lazy loading =
  fetch children on first expand, cache in the store.

## Prerequisite: Harmonia 2.6.0 → 2.10.0 (PR 0)

The root pom pins `harmonia.version` 2.6.0; the **tree was rebuilt with breaking changes in 2.9.0**
(`x-h-tree-button` → row/label/actions/indicator, new `tree-item-click` event). Building the
explorer on the 2.6.0 tree API would be instant legacy — bump first. Measured blast radius:

- Old tree API: **one file** — `template-application-ui-harmonia-java`'s
  `ui/perspective/manage/list-view.html.template` (the tree-list layout).
- Removed `data-disabled` / `data-label`: **three templates** (`admin-index.html.template`,
  `document/document-view.html.template`, `manage/form-view.html.template`). The
  `resources-karavan-libs` grep hit is a bundled third-party React app — not Harmonia markup.
- 2.9.0 also reworked **sidebar group actions** and the **`aria-disabled` keyboard behaviour** —
  verification pass over the five existing shells' `index.html` (application, monitoring, admin,
  personal, partner) plus the Harmonia templates.
- Per the established bump procedure: read the upstream CHANGELOG (2.7 → 2.10), regenerate the
  sample apps after the template migrations, full IT run.

## Phasing / PR sequence

| PR | Content | Size | Status |
|---|---|---|---|
| 0 | Harmonia bump 2.6.0 → **2.9.0**: pom pin, tree-template migration, slot-picker `.responsive`, `data-disabled` fix, one IT assertion | 7 files | **#6656 open**, 6 UI journeys green |
| 1 | `/services/data/**` URL-layer gate + `HttpSecurityURIConfiguratorTest` matrix rows | 3 files | **#6657 open**, 30 unit tests green |
| 2 | The shell: module skeleton, `dbops.js`, two store/page/view triples, registration edits, i18n, `DatabaseShellIT` | ~1,300 LOC, 18 files | in progress |

Calibration: the Monitoring shell landed as three PRs totalling +3,932 LOC over 46 files, six pages,
zero Java in the module. This shell has fewer pages (2 vs 6) but denser ones — the lazy tree and the
arbitrary-columns results grid are the two components Monitoring never needed. **Total: ~1–1.5 weeks
end to end including the bump** — materially less if the bump happens anyway for other reasons.

## Testing

- **`DatabaseShellIT`** (`@Tag("ui")`, `UserInterfaceIntegrationTest`) — the MonitoringShellIT
  pattern, ~100 LOC: (1) the Explorer renders shell chrome, the tree shows `DefaultDB` schemas, and
  a store poll resolved (catches the Alpine-bootstrap abort, which has no DOM or server symptom);
  (2) every section reachable by `database-nav-<x>` id, asserted on `database-<x>-page` with
  deployment-independent text; (3) the SQL console executes a `SELECT 1`-class statement against
  DefaultDB and the grid renders a row — the one end-to-end journey worth the browser.
- The backend needs no new tests (unchanged); PR 1's gate change extends the existing
  `HttpSecurityURIConfiguratorTest`.

## Key decisions & risks

1. **Textarea, not Monaco** (decision). Support usage doesn't justify a multi-MB editor; localStorage
   persistence and Ctrl/Cmd+Enter match the IDE's muscle memory. Revisit only on real demand
   (CodeMirror-class, lazy-loaded like Monitoring's bpmn-visualization, would be the shape).
2. **The Harmonia bump is the main risk** — a platform-wide change riding ahead of a feature.
   Mitigated by its own PR, the small measured breaking-change inventory above, and the full IT
   suite.
3. **Silent row cap UX** — inherent to the backend; label it, don't fix it here.
4. **Write access is the point, and the gate is roles, not the shell.** The shell deliberately
   allows UPDATE/DDL (a support tool that can't fix data is a dashboard). Safety comes from the
   `.access` file + the endpoints' `@RolesAllowed` (+ PR 1's URL gate), not from hiding buttons.
   No confirmation theatre beyond a destructive-statement confirm on non-`select` execution.
5. **Don't port the IDE's bugs.** Known in the AngularJS stack and out of scope to copy: a dead
   `delete` branch in `result.js` row CRUD, a `$destroy` throw on an undeclared listener, the
   dot-splitting of `schema.table`, and the never-wired `database.database.selection.changed`
   (database-kind) topic. Greenfield code replaces the whole MessageHub topic web with two Alpine
   stores.

## Explicitly out of scope

Import/export (sync/async), anonymization, data transfer, schema export/import processes,
datasource CRUD, row-edit dialogs, Monaco, procedure/script generation beyond SELECT, project
exports, topology — the Workbench Database perspective remains the deep tool; this shell links to it.
