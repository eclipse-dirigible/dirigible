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
`widgetDependsOnControllerUrl` by `service-generate`'s `ModelParameterProcessor`. A
dependent dropdown is never filled from the unnarrowed table: each picker's own
`loadAllOptions<Name>()` holds the full fetch, `refreshOptions<Name>()` picks narrowed or
whole by the trigger's current value, and `init()` defers that choice until the record (or
URL prefill) has put the trigger in the form (#7669).
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
store; the generated `i18n/en-US/*.json` catalogs already exist for it. Remaining items are refinements — see the checklist + the
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

The descriptors are JSON data (#8007), read without a script engine. A composite takes in
its parts with `{"include": "<descriptor>.json"}` entries, replaced in place; the template
id stays `template-application-ui-harmonia-java/template/template.js`, which resolves to the
`.json` next to it.

- `template/template.extension` — registers on `platform-templates`.
- `template/template.json` — entry; includes `template-application-schema` (REQUIRED: the
  schema creates the tables the client-Java entities only map), `template-application-rest-java`
  (which includes the DAO layer) and `template/ui/template.json`.
- `template/ui/template.json` — the translation catalog, then the per-view source lists below.
- `template/ui/shell.json` — the project-specific shell files (`index.html`, `js/config.js`,
  `_settings.html`, the dashboard); the shared runtime is served from `application-core/shell`.
- `template/ui/list.json` — LIST/PRIMARY entities -> page component + view fragment.
- `template/ui/manage.json` — MANAGE/PRIMARY entities -> a CRUD list page plus a shared
  create/edit form page (`/{name}`, `/{name}/create`, `/{name}/{id}/edit`).
- `template/ui/setting.json` — SETTING entities -> the manage views under the "Settings" perspective.
- `template/ui/masterDetail.json` — masters browse on the manage list and open on the manage form;
  details emit an `App.registerDetail` registration and a routed create/edit form.
- `template/ui/document.json` — MANAGE_DOCUMENT masters -> a header form, an inline line-items
  table and a totals footer.
- `template/ui/calendar.json`, `template/ui/slots.json` — an ADDITIONAL page, not a replacement
  (#6547): the entity keeps its own layout and every page it generates, the calendar / slot picker
  takes over the landing route.
- `template/ui/related.json` — entities declaring `relatedEntities` -> one `<Entity>.related.js`
  registration (the read-only register of the records REFERENCING them). See below.
- `template/ui/report.json` — REPORT entities -> a data table or a native `x-h-chart-*` chart over
  the generated Java report controller.
- `template/ui/navigation.json` — one shared-shell perspective per top-level entity, one pair per
  LAYOUT collection; deliberately not `uiCalendarModels` / `uiSlotsModels`, whose entities are
  already in one of them.
- `template/ui/my.json`, `template/ui/partner.json` — the personal and partner surfaces, scoped to
  `<Entity>MyController` / `<Entity>PartnerController`.
- `template/ui/admin.json` — one plain administration page per module (`adminModel`).
- `template/template-report-file.json` + `template/ui/reportFile.json` — a standalone `.report`:
  the Java report backend plus a self-contained page under `gen/<javaGenFolder>/reports/<name>/`.

Velocity model vars (per-entity collections, same as the Angular module):
`$name`, `$perspectiveName`, `$properties[]` (`$property.name`, `.dataName`,
`.widgetType`, `.widgetIsMajor`, `.dataAutoIncrement`), `$projectName`, `${tprefix}`,
`${primaryKeysString}`, `$hasProcess`. The aggregate `index.html` is generated with
no `collection`, so `$models` = `model.entities`.

**Perspectives are JSON extension modules** (#7964). Every generated `perspective.json`
(`perspective/perspective.json.template` and its `my` / `partner` / `admin` siblings) is the
perspective object itself, read by the Java aggregation behind
`/services/platform/extensions/perspectives` without starting a script engine;
the `.extension` names the `.json`. Labels go in through `menuLabelJson` / `appTitleJson`, the
JSON-escaped twins of `menuLabel` / `appTitle`.

**Perspective icons are Lucide NAMES, not unicons URLs.** The generated `perspective.json`
(both the shared-shell `perspective/perspective.json.template` and the Personal-shell
`my/my-perspective.json.template`) emits `"icon": "${iconName}"` — the bare Lucide / Harmonia
built-in icon name (the model's `iconName`), which the application shell and Personal shell render
via `x-h-lucide`. It does **NOT** emit `${perspectiveIcon}` (the `/services/web/resources/unicons/<name>.svg`
URL): that path is only meaningful to the legacy **AngularJS** perspective and 404s in the Harmonia
shells (whose `isSvgIcon`/`isImageIcon` would send a `.svg` path down the `<svg data-link>` branch to
a missing file). The `.model` still carries `perspectiveIcon` for the AngularJS stack — untouched.
So the intent `icon:` must be a valid Lucide name (bundled `org.webjars.npm:lucide` `dist/esm/icons/<name>.js`,
currently 1.8.0); an unknown name renders blank.

## Related registers (the reverse of an incoming association)

A `.model` entity may declare `relatedEntities` — read-only registers of the records that
**reference** it (a project-month's timesheet lines, a customer's invoices, an account's journal
entries). Each entity that declares any emits one `<Entity>.related.js` calling
`App.registerRelated(<entity>, def)`, and its form / document pages render one shared
`relatedPanel` per entry off `App.relatedFor(<entity>)`.

Three things about it are deliberate and easy to get wrong:

- **The REFERENCED entity contributes the registration, not the referencing one.** That is the
  opposite of a detail (`App.registerDetail`, contributed by the composition child) and it is
  forced: the referencing entity may be owned by another model in another project, generated at
  another time, with no knowledge of this one. Hence the def's `apiPath` and `appUrl` are
  **absolute** and every call passes `{ baseUrl: '' }`.
- **It filters through `POST <controller>/search`, not `?<fk>=<id>`.** The master-filter query
  parameter only exists on a `*_DETAILS` layout's controller; the generic search endpoint every
  generated controller exposes is what a register can rely on.
- **It is a window, not an owner.** No add, no edit, no delete — a row opens the source's own record
  page in the shared record dialog (`$store.related`), which is also what makes a cross-project
  source work. `relatedPanel` therefore builds on `detailPanel` (same table, same foreign-key label
  resolution, same formatting) and overrides only the load and the row action.

## Parity checklist (TODO)

| View type | Angular collection | Status |
|---|---|---|
| list | `uiListModels` | ✅ skeleton (read-only list page + view) |
| manage | `uiManageModels` | ✅ CRUD list + shared create/edit form (/create, /:id/edit) on baseFormPage — relationship dropdowns, client validation, 422 field mapping, delete-confirm |
| master-list + detail | `uiListMasterModels` / `uiListDetailsModels` | ✅ the manage list (record sheet with the master's own fields) + the manage form, which renders the registry-driven detail panels at /:id/preview and /:id/edit |
| master-manage + detail | `uiManageMasterModels` / `uiManageDetailsModels` | ✅ same pages as the master-list row; details get a routed form whose parent FK is **context-locked** — the panel names the FK in the URL on create AND on edit/preview, so the form shows the parent's label read-only instead of a dropdown that could re-point the record mid-flow (#6551); free selection survives only where nothing implies the parent (the entity's own top-level create) |
| **calendar** | `uiCalendarModels` | ✅ a PRIMARY entity with intent `view: calendar` (+ a `calendar:` block: start/end/title/color/initialView) → the entity attribute `calendarView`; full-page `x-h-calendar` (month/week/day/year) whose events are the entity's records positioned by the date/datetime field. **An ADDITIONAL page, not a layout (#6547):** the entity keeps its own `layoutType` (MANAGE / MANAGE_MASTER / MANAGE_DOCUMENT) and every page it brings; the calendar owns the landing route `/<Entity>`, that layout's browse page moves to `/<Entity>/list`, and both carry the same view switch - one icon-only single-choice `x-h-button-group` just before New, `[calendar | table]`, merged with the Tree / Table modes on a `hierarchy:` list (#7837). So **date-click → /create** and **event-click → /:id/edit** land on whatever editor the layout owns - a document master browsed on a calendar edits on its document page. Colour keyed categorically by `color`. Optional `calendar.scope: <relation>` scopes the calendar to a parent: when opened as `/<Entity>?<Scope>=<id>` it filters events (controller `/search` EQ) and presets that FK on create - so it shows/creates only one parent's records (e.g. day allocations of one timesheet); a chip names the parent record and its x drops the scope. The calendar's own chrome (Today, Day/Week/Month/Year, "+N more") is translated, events map through the shared `HarmoniaCalendar`, and a reload hands over the events only, so the calendar keeps the view the user navigated to. |
| **range** | `uiCalendarModels` | ✅ `view: range` - a span entity (`calendar.start` + `calendar.end`) on the same calendar renderer; events render as all-day multi-day bars (`calendarRange`). For vacation/booking spans. |
| **document items on a calendar** | (within document) | ✅ the document's **line-items child** declaring `view: calendar` makes the items PANE an `x-h-calendar` instead of the row grid (`documentItemsLayout: calendar`, #6482) - for a day-grained line (booked days, allocated hours). Same rows, same line dialog: event-click edits, empty-day click adds with the date preset, Delete moves into the dialog. Rendered on the power, personal and partner document surfaces; the calendar's configuration is read at runtime from the child's detail registration, never baked in. Mutually exclusive with `documentItemsLayout: chat`. |
| **slots** | `uiSlotsModels` | ✅ `view: slots` (+ a `slots:` block: start/open/close/step/disabledDays) → the entity attribute `slotsView`; a Harmonia `x-h-slot-picker` (3-day time-slot grid). Free slots are bookable; the bookings of the days in view are read on every `range-change` (a `GTE`/`LT` search on the start column, in the browser's local time) and their slots shown taken - coloured, labelled and still clickable, opening the booking; a free slot opens **the layout's own create route** prefilled with the chosen datetime (`?<Start>=`), so a booking document is created as a document. **An ADDITIONAL page like the calendar (#6547):** the picker owns the landing route `/<Entity>`, the layout browses at `/<Entity>/list`, and both carry the same view switch, `[clock | table]`. For appointment booking. |
| main-details | (within master) | ⬜ stub |
| setting | `uiSettingModels` | ✅ reuses the manage CRUD templates, grouped under a "Settings" sidebar section |
| report / report-chart / report-table | `uiReportTableModels` / `uiReportChartModels` | ✅ in-SPA table page (data table; Refresh + Updated stamp, CSV export and Print under More actions) + chart page (native Harmonia `x-h-chart-*` bar/line/pie/doughnut/polar-area/radar - no external chart library) against the Java report controller |
| **standalone report** (a `.report` file) | `reportModels` / `generateReportModels` | ✅ `template/template-report-file.js` ("Application Report - Table - Harmonia"): reuses the framework-neutral Java backend (reportFileEntity Repository + Controller) and generates a self-contained Harmonia page (`gen/<javaGenFolder>/reports/<name>/` - the same sanitized folder as its Java: index.html + report.js — list with `$limit`/`$offset`, count, pagination; one action row - title, Filters, Refresh + Updated stamp, the icon-only Chart / Table switch and Export / Print under More actions). This is the intent recipe's `report` default |
| navigation (generated nav data) | `uiNavigations` | ⬜ folded into index.html for now |
| dialogs (filter/window) | per view | ⬜ |
| forms + BPM task forms | (separate module) | ✅ `template-form-builder-harmonia` renders a `.form` as a Harmonia page via the neutral `formController(ctx)` contract; BPM task forms complete via `ctx.task.complete()` |
| **Process Inbox** (built-in) | — | ✅ built-in /inbox view: **Outlook-style master-detail** (resizable `x-h-split`) — task list (assignee+groups) on the left, the selected task's form inline (`<iframe>`) on the right, claim-before-open, auto-refresh toggle; mirrors the dashboard redesign #6064/#6068 |
| **Documents** (built-in) | — | ✅ built-in /documents view: full **Document Storage** as an Outlook-style master-detail (file list + **File Preview** pane — CSV→table via PapaParse, other types→`<iframe>` over `/preview`). Back/forward history, breadcrumbs, search-in-folder, new folder, rename, single + multi-select delete, download (file + folder zip), copy link, file-type icons, upload (files + unpack-zip, button and drag-and-drop). Root is listed with **no `?path=`** (a `?path=/` 400s); delete sends a JSON body of absolute paths; rename is `PUT {path,name}` — matching the dashboard `js/documents.js` contract |
| process tasks | gated on `hasProcess` / `ProcessId` | ✅ processTasks Alpine store (inbox fetch + claim + bucket by processInstanceId) + inline popover in list/manage/master rows + app-wide task-form dialog |

Asset embedding (Phase 1 — DONE, verified end-to-end against a live app):
- Alpine `3.17.4` + Harmonia `3.6.1` + Lucide `1.48.0` are **webjars** bundled via
  `components/resources/application-core` (`alpinejs.version` / `harmonia.version` / `lucide.version`
  in the root pom), served version-less through webjars-locator at `/webjars/...` (public). The
  Harmonia rules live in the upstream skill - see `.claude/docs/harmonia-ui.md`.
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
