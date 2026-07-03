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
and form date handling. The EDM **Depends-On** attributes (`widgetDependsOn*`) are
implemented (parity with the AngularJS stacks): `form-page.js.template` emits an Alpine
watcher + an `applyDependsOn<Name>` method per dependent property (cascading dropdown
re-filter via `POST <controller>/search` with an EQ condition — single match
auto-selects — or scalar auto-populate), covering manage forms, master-detail detail
forms and allocation panels (all reuse the FormPage); `document-page.js.template` does
the same for document headers plus a **metadata-driven** cascade in the line-item
dialog (`detail-register.js.template` emits `editColumns[].dependsOn`; filtered options
live in a separate `draftOptions` store so the items table's label resolution keeps the
full option set). The trigger's controller URL is precomputed as
`widgetDependsOnControllerUrl` by `service-generate`'s `parameterUtils.js`.
**Multi-language data** is wired through a single per-user flag: the Settings page's
**Region & Language** picker (rendered from the generated `config.js` `languages`, hidden
for a single language) writes the shared `locale` Alpine store
(`application-core/.../shell/js/stores/locale.js`, localStorage
`codbex.harmonia.language`); the shared fetch client sends the value as
`Accept-Language` on every call, which the generated multilingual Java repositories
translate by (`<TABLE>_LANG` overlay), and the document Print flow prefers the same
language when a template for it exists. The standalone **report page** offers **typed per-column filters** (date ranges,
number ranges, boolean, text contains) from generation-time column metadata, applied
**server-side** over the wrapped report query — pagination, count and CSV export all
reflect the active filters. UI **labels** remain untranslated — the Harmonia
framework itself has no i18n API (verified against 1.24.2: only breakpoint +
colour-scheme helpers), so label i18n is a documented follow-up on top of the locale
store; the generated `translations/en-US/*.json` catalogs already exist for it. Remaining items are refinements — see the checklist + the
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
| report / report-chart / report-table | `uiReportTableModels` / `uiReportChartModels` | ✅ in-SPA table page (data table + CSV export) + chart page (chart.js bar/line/pie/doughnut/polarArea/radar) against the Java report controller |
| **standalone report** (a `.report` file) | `reportModels` / `generateReportModels` | ✅ `template/template-report-file.js` ("Application Report - Table - Harmonia"): reuses the framework-neutral Java backend (reportFileEntity Repository + Controller) and generates a self-contained Harmonia page (`gen/<genFolder>/reports/<name>/`: index.html + report.js — list with `$limit`/`$offset`, count, pagination, CSV export). This is the intent recipe's `report` default |
| navigation (generated nav data) | `uiNavigations` | ⬜ folded into index.html for now |
| dialogs (filter/window) | per view | ⬜ |
| forms + BPM task forms | (separate module) | ✅ `template-form-builder-harmonia` renders a `.form` as a Harmonia page via the neutral `formController(ctx)` contract; BPM task forms complete via `ctx.task.complete()` |
| **Process Inbox** (built-in) | — | ✅ built-in /inbox view: **Outlook-style master-detail** (resizable `x-h-split`) — task list (assignee+groups) on the left, the selected task's form inline (`<iframe>`) on the right, claim-before-open, auto-refresh toggle; mirrors the dashboard redesign #6064/#6068 |
| **Documents** (built-in) | — | ✅ built-in /documents view: full **Document Storage** as an Outlook-style master-detail (file list + **File Preview** pane — CSV→table via PapaParse, other types→`<iframe>` over `/preview`). Back/forward history, breadcrumbs, search-in-folder, new folder, rename, single + multi-select delete, download (file + folder zip), copy link, file-type icons, upload (files + unpack-zip, button and drag-and-drop). Root is listed with **no `?path=`** (a `?path=/` 400s); delete sends a JSON body of absolute paths; rename is `PUT {path,name}` — matching the dashboard `js/documents.js` contract |
| process tasks | gated on `hasProcess` / `ProcessId` | ✅ processTasks Alpine store (inbox fetch + claim + bucket by processInstanceId) + inline popover in list/manage/master rows + app-wide task-form dialog |

Asset embedding (Phase 1 — DONE, verified end-to-end against a live app):
- Alpine `3.15.11` + Harmonia `1.24.2` + Lucide `1.8.0` are **webjars** bundled via
  `components/resources/application-core` (`harmonia.version` in the root pom; `1.24.2` carries the
  `x-h-select` dropdown fix), served version-less through webjars-locator at `/webjars/...` (public).
- Pinecone Router is the `org.webjars.npm:pinecone-router` **webjar** (pulled by
  `application-core`), served version-less at `/webjars/pinecone-router/dist/router.min.js`
  (the package `main`, a self-registering IIFE). It was previously vendored — there was no webjar
  until 7.5.2.
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
