# template-application-ui-harmonia-java

Generation template for the **Alpine.js + Harmonia** runtime UI stack — the SPA
counterpart to `template-application-ui-angular-java`. Same model, same Velocity
parameters, same reused Java REST/DAO backend (`template-application-rest-java`);
only the generated UI layer differs.

See the design rationale and phasing in the repo-root `HARMONIA_RUNTIME_PLAN.md`
("Reference implementation: codbex-athena-app" and "Phasing"). The reusable shell
assets here are adopted from `github.com/codbex/codbex-athena-app`.

## Status: functional

Registers as a `platform-templates` extension and generates a complete runnable
Harmonia SPA: the shell + all core view types (list, manage, setting, master-detail,
reports), built-in Process Inbox + Documents sections, and inline process-task
surfacing. Generation is verified against real models (`DependsOnIT`, `sales-order`,
`sample-intent-model`) with live CRUD, relationship dropdowns, master→detail filtering
and form date handling. Remaining items are refinements — see the checklist + the
repo-root `HARMONIA_RUNTIME_PLAN.md` "Implementation status" + "Follow-ups".

## Architecture (how it differs from the Angular module)

The Angular module generates **one iframe perspective per entity**, each registered
via `.extension` + `view.js` + `controller.js` and hosted by the platform dashboard
over the `postMessage` hub protocol.

This module generates a **single client-routed SPA** (no iframes, no hubs):

```
gen/{{genFolderName}}/
  index.html                         # aggregate shell: x-h-split layout, sidebar,
                                     #   breadcrumb, <template x-route> per entity,
                                     #   page-component <script> tags  (generated once)
  css/app.css                        # copy
  js/app.js                          # copy   — window.App namespace
  js/config.js                       # GENERATED — projectName, basePath, restBase
  js/services/api.js                 # copy   — fetch client + ApiError
  js/services/apiError.js            # copy   — localizable user-safe error catalog
  js/services/formValidation.js      # copy   — schema validator
  js/components/layout/appShell.js   # copy   — the reusable dashboard (Alpine.data 'app')
  js/components/pages/basePage.js     # copy
  js/components/pages/baseFormPage.js # copy   — 422 errorCauses -> per-field mapping
  js/components/pages/{persp}/{Entity}ListPage.js  # GENERATED per LIST entity
  views/{persp}/{entity}-list.html   # GENERATED per LIST entity (Harmonia fragment)
  views/_notfound.html               # copy
```

Static shared assets use `action: "copy"` (verbatim; only the path is Mustache-processed).
The one project-specific shell file is `js/config.js` (`action: "generate"`) so the
verbatim assets can read their wiring from `window.App.config` instead of being
Velocity-processed (which would collide with `$`-sigils in the JS).

## Generation wiring

- `template/template.extension` — registers on `platform-templates`.
- `template/template.js` — entry; merges `template-application-rest-java` sources
  with `template/ui/template.js`.
- `template/ui/template.js` — aggregates the per-view source collectors.
- `template/ui/shell.js` — the SPA shell + static assets (above).
- `template/ui/list.js` — LIST/PRIMARY entities -> page component + view fragment.
- `template/ui/navigation.js` — placeholder (nav currently folded into index.html).

Velocity model vars (per-entity collections, same as the Angular module):
`$name`, `$perspectiveName`, `$properties[]` (`$property.name`, `.dataName`,
`.widgetType`, `.widgetIsMajor`, `.dataAutoIncrement`), `$projectName`, `${tprefix}`,
`${primaryKeysString}`, `$hasProcess`. The aggregate `index.html` is generated with
no `collection`, so `$models` = `model.entities`.

## Parity checklist (TODO)

| View type | Angular collection | Status |
|---|---|---|
| list | `uiListModels` | ✅ skeleton (read-only list page + view) |
| manage | `uiManageModels` | ✅ CRUD list + shared create/edit form (/create, /:id/edit) on baseFormPage — relationship dropdowns, client validation, 422 field mapping, delete-confirm |
| master-list + detail | `uiListMasterModels` / `uiListDetailsModels` | ✅ master page (x-h-split: list + detail panels) + registry-driven detail panels |
| master-manage + detail | `uiManageMasterModels` / `uiManageDetailsModels` | ✅ same master page; masters reuse the manage form for create/edit, details get a routed form (FK preset) |
| main-details | (within master) | ⬜ stub |
| setting | `uiSettingModels` | ✅ reuses the manage CRUD templates, grouped under a "Settings" sidebar section |
| report / report-chart / report-table | `uiReportTableModels` / `uiReportChartModels` | ✅ table page (data table + CSV export) + chart page (chart.js bar/line/pie/doughnut/polarArea/radar) against the Java report controller; Reports sidebar group |
| navigation (generated nav data) | `uiNavigations` | ⬜ folded into index.html for now |
| dialogs (filter/window) | per view | ⬜ |
| forms + BPM task forms | (separate module) | ✅ `template-form-builder-harmonia` renders a `.form` as a Harmonia page via the neutral `formController(ctx)` contract; BPM task forms complete via `ctx.task.complete()` |
| **Process Inbox** (built-in) | — | ✅ built-in /inbox view: the user's BPM tasks (assignee+groups) via the processTasks store, claim+open the task form |
| **Documents** (built-in) | — | ✅ built-in /documents view: CMS folder browser (/services/js/documents/api) — navigate, download, create folder, upload, delete |
| process tasks | gated on `hasProcess` / `ProcessId` | ✅ processTasks Alpine store (inbox fetch + claim + bucket by processInstanceId) + inline popover in list/manage/master rows + app-wide task-form dialog |

Asset embedding (Phase 1 — DONE, verified end-to-end against a live app):
- Alpine `3.15.11` + Harmonia `1.24.1` + Lucide `1.8.0` are **webjars** bundled via
  `components/resources/application-core` (its `harmonia.version` was bumped 1.4.2 → 1.24.1 in the
  root pom), served version-less through webjars-locator at `/webjars/...` (public).
- Pinecone Router has no published webjar, so its `router.min.js` is **vendored** under
  `application-core/.../vendor/` and served at `/services/web/application-core/vendor/`
  (license-excluded in the root pom).
- The generated `index.html` references only these local URLs — no unpkg/jsdelivr (CSP/offline).

Other open items:
- **config.js `restBase`:** verified in Phase 0 — `/services/java/<project>/gen/<modelFile>/api`,
  each entity at `<restBase>/<perspective-lowercased>/<Entity>Controller` (GET list/$limit/$offset,
  GET /{id}, POST, PUT /{id}, DELETE /{id}). Page components use a relative `apiPath` so api.* prepends
  restBase; relationship dropdowns call the absolute `widgetDropdownControllerUrl` with `{ baseUrl: '' }`.
- **`platform-links` category:** optional — could add a `harmonia-view` category to inject the asset
  tags instead of hard-coding them in the shell `index.html`.
- **Generation recipe:** register the stack choice in `service-generate` / the intent recipe.
- **Parity ITs:** Selenide against the hash-routed SPA (`x-h-*` DOM, no iframe/BlimpKit selectors).
- **In-browser render:** still unverified here (no headless browser); all HTTP layers are proven.
