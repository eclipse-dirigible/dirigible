# BPM Editor — Modernization & BlimpKit Migration Context

This document is the working notebook for the BPM editor migration effort. It captures the
**functional Angular framework migration that has already shipped** on branch
`fix-bpmn-editor`, and lays out the **BlimpKit visual-alignment migration that is next**.

> Treat this file as the canonical hand-off between sessions. When something changes in
> reality, update this file in the same commit.

---

## 1. What is already done (branch `fix-bpmn-editor`)

The BPM editor on `origin/master` was broken in several user-visible ways. `fix-bpmn-editor`
restores it without deleting any decision-tables / cases / forms / apps controllers or views.

### Branch lineage

`origin/master` → cherry-picked test introducer → +9 fix commits (10 commits total).

```
14431d07f1  Replace remaining angular-strap bs-* directives
e7a12a39a0  Restore modal-header X close button visibility
feb6cd53c3  Fix stencil-palette icons white on light theme (OS vs Dirigible theme)
d0968c65dd  Fix property-panel popup lock-up: $hide/$show + prefixEvent + display:block
936d324aaa  BpmnEditorLoadsIT: assert $modal and $popover are registered
30e2e1d3d5  Migrate BPM editor from AngularJS 1.4.7 to 1.8.2
11bc5c1283  formatting
0aab20f413  test for theme change in bpmn editor and claude context for bpmn
1329f23be5  Restore --sap* CSS variables for editor theming
08995717f5  Fix BpmnEditorIT status-bar assertion and modernise BpmService return types
```

### Bugs that were diagnosed and fixed

| Symptom | Root cause | Fix |
|---|---|---|
| Blank editor iframe after opening a `.bpmn` file (on the prior `bpmn-editor-modernization` attempt with AngularJS 1.8.2) | Angular 1.6 changed the default `$locationProvider.hashPrefix` from `''` to `'!'`. The IDE iframe URL is `…/index.html#/editor`, which Angular 1.8 parsed as an empty path → fell through to `/` → redirected to `/processes` → tried to load `views/processes.html` → 404 → `ng-cloak` never released. | `$locationProvider.hashPrefix('')` in `app.js`'s `.config()`. Route key changed `'/editor/'` → `'/editor'` to match the URL exactly. `redirectTo` made a function so `editorParams` is read at route-evaluation time. |
| BPM editor (and other editors) lose all theme styling — colors fall back to browser defaults regardless of selected theme | PR #5897 ("Fully updated BlimpKit") dropped `blimpkit-{light,dark}-variables.css` from the theme `links` arrays. 9+ editors still reference `var(--sapBackgroundColor)` etc. in their CSS. The legacy `--sap*` variables were no longer defined anywhere. | Bundled the old `blimpkit-{light,dark}-variables.css` (extracted from BlimpKit 1.9.1 jar) into `theme-blimpkit/css/sap-variables-{light,dark,auto}.css`, added them back to each theme's `links` ahead of the BlimpKit CSS, bumped theme `version: 3 → 4` so saved theme records refresh. |
| Clicking property rows that open a popup (Execution listeners, Task listeners, Class fields, Expression / Class / Delegate-expression text popups, ...) opens the modal but leaves the editor grayed and locked — backdrop stays, canvas unclickable | Drop-in `$modal` factory replacing angular-strap was missing three angular-strap behaviours: (a) `$scope.$hide()` and `$scope.$show()` on the modal scope — `text-popup.html`'s `close()` handler calls `$scope.$hide()` and silently threw without it. (b) `prefixEvent` option + emission of `<prefix>.show.before/show/hide.before/hide` events up the scope tree — controllers listen for `textModalEvent.hide.before` / `modal.hide.before` to switch property rows back to read mode. (c) The Bootstrap-3 modal CSS sets `.modal { display: none; }` and `.show()` was supposed to override via `jQuery.fn.show()` but in practice ended up empty inline → backdrop visible, modal invisible. angular-strap forced `display: block` itself. | Rewrote `scripts/services/modal-service.js` to: (a) attach `$hide`/`$show` to the modal scope, (b) honour `prefixEvent`, (c) explicit `element[0].style.display = 'block'`, (d) `parentScope.$on('$destroy', …)` cleanup. Same treatment for `scripts/services/popover-service.js` (prefix defaults to `tooltip`, exposes `instance.$scope` for `state.popover.$scope.$on('tooltip.hide', …)`). |
| Stencil-palette icons (Events, Activities, Gateways, ...) appear white even on light theme — almost invisible | `editor-app/theme/style.css` invert filter was gated on `@media (prefers-color-scheme: dark)` — that's the *OS preference*, not the Dirigible theme the user picked. OS dark + Dirigible Light theme → icons inverted to white. | `setTheme()` in `index.html` now adds `dirigible-theme-{light,dark,auto}` on `<body>` from `theme.type`. CSS rule changed to use those classes. `prefers-color-scheme: dark` is kept as a fallback for `dirigible-theme-auto`. |
| Modal-header ✕ close button is zero-sized — only the Cancel button at the bottom is interactable | Bootstrap-3's `.close { font-size:21px; float:right; … }` is loaded, but its specificity is being overridden by something in the BlimpKit/CSS chain (precise selector not nailed down), leaving the button at 0 × 0. | Defensive `.modal-header .close` rule in `editor-app/theme/style.css` re-states the visible defaults with `var(--sapTextColor)` so the ✕ stays visible in both themes. |
| `<button bs-select>` in `duedate-popup.html` renders nothing without angular-strap; the due-date option dropdown is unselectable | angular-strap's `bs-select` directive turned a `<button>` into a dropdown menu; without angular-strap the button is empty. | Replaced with a plain `<select class="form-control" ng-options="…">` — native dropdown, same model. |
| `bs-tooltip` on a form-builder help link no longer shows anything | angular-strap removed. | Replaced with the native `title` attribute. |

### Touched files

- **App configuration / routing**
  - `components/ui/editor-bpm/src/main/resources/META-INF/dirigible/editor-bpm/scripts/app.js`
  - `components/ui/editor-bpm/src/main/resources/META-INF/dirigible/editor-bpm/index.html`
- **Drop-in replacements for angular-strap**
  - `…/editor-bpm/scripts/services/modal-service.js` *(new)* — replaces `$modal`
  - `…/editor-bpm/scripts/services/popover-service.js` *(new)* — replaces `$popover`
- **Theming**
  - `components/resources/resources-theme-blimpkit/src/main/resources/META-INF/dirigible/theme-blimpkit/extensions/theme-{light,dark,auto}.js`
  - `components/resources/resources-theme-blimpkit/src/main/resources/META-INF/dirigible/theme-blimpkit/css/sap-variables-{light,dark,auto}.css` *(new)*
- **Editor controller**
  - `…/editor-bpm/editor-app/editor-controller.js` — `$routeParams.modelId → workspace` + `.success/.error → .then/.catch`
- **17 scripts converted from `.success/.error` → `.then/.catch`** (full list in `30e2e1d3d5` commit message and the agent run summary on this branch)
- **Theme CSS**
  - `…/editor-bpm/editor-app/theme/style.css` — body-class-based theme detection, `.modal-header .close` override
- **Templates**
  - `…/editor-bpm/editor-app/configuration/properties/duedate-popup.html` — `bs-select` → `<select>`
  - `…/editor-bpm/views/popover/formfield-edit-popover.html` — `bs-tooltip` → native `title`
- **Tests** *(all new on this branch)*
  - `tests/.../BpmnEditorLoadsIT.java` — smoke (canvas + palette + Start Events + theme variable + `$modal`/`$popover` injector contract)
  - `tests/.../BpmnEditorIT.java` — full edit / rename / save / "Published"
  - `tests/.../BpmnEditorPropertyPopupIT.java` — open Execution Listeners modal, assert `$hide` + ✕-size + close-via-Cancel + canvas still interactive
  - `tests/.../BpmnModelApiIT.java` — API-level

### Tests on this branch

```
BpmnEditorLoadsIT          25 s
BpmnEditorIT               59 s
BpmnEditorPropertyPopupIT  46 s
```

All three pass on `origin/fix-bpmn-editor`.

### Don't repeat these dead-ends

- Don't add `.when('/editor/', { redirectTo: '/editor' })`. Angular 1.8 normalises trailing slashes and the route loops forever (`$rootScope:infdig`).
- Don't change `redirectTo` back to a string template literal — `editorParams` is undefined when `.config()` runs.
- Don't delete the casemodel / form / decision-table / apps controllers or views (the prior attempt did, the user explicitly reverted that decision).
- Don't move `modal-service.js` / `popover-service.js` *after* `editor-controller.js` in `index.html` — Angular registration order doesn't matter, but a future reader will assume it does and panic.

---

## 2. What is next — BlimpKit visual alignment

Functionally, the editor works. Visually, the property panel and pop-up modals look like
Flowable's 2014-era Bootstrap-3 UI, not BlimpKit (the rest of the IDE). The user has flagged
this — see the chat exchange where the question was *"did you adapt the properties section
with BlimpKit as the rest of the UI?"*.

The scope of "BlimpKit alignment" is **not** a framework change. It's a template + CSS
rewrite. AngularJS 1.8.2 stays. The drop-in `$modal` / `$popover` factories stay — or get
replaced by a BlimpKit-dialog-based `$modal` (see option C below).

### Status — what's done on branch `bpmn-editor-blimpkit-migration` (PR [#5963](https://github.com/eclipse-dirigible/dirigible/pull/5963))

The PR is rebased on top of `origin/master` (which now has the `fix-bpmn-editor` work
merged in as squash commit `2ecd17d802`). Commits (oldest first):

```
Step 0 — Prerequisite: wire BlimpKit into the iframe
  11e82d23d1  Document BlimpKit-wiring prerequisite in CLAUDE_BPMN.md
  8b3daa5b9b  Wire BlimpKit into the editor-bpm iframe
  67683fe71d  Re-enable Angular debug info after the blimpKit module turns it off
Step 1 — Modal-service rewrite
  fa5ac49432  Make $modal factory dual-mode for the BlimpKit popup migration
Step 2 — Popup templates (21 total)
  482eb7f0da  Migrate text-popup.html
  f158bc2a1e  Migrate execution-listeners-popup.html + update BpmnEditorPropertyPopupIT
  01e1618bb2  Migrate condition-expression, feedback, fields, duedate
  12ebf50aa3  Migrate reference + form-properties popups
  d6b8683da1  Migrate remaining popups + form-builder popover
Status update
  96c0568d97  Record popup-migration completion + remaining-step notes
Step 3 — Property-panel inline write templates
  af8d12997c  Migrate inline property-panel write templates to BlimpKit (+ test fix)
Step 4 — Glyphicon sweep + remaining editor popups
  99f358cd70  Sweep glyphicon-* → sap-icon--*, migrate remaining editor popups
Step 5 — Defensive CSS removal (partial)
  26899f1217  Drop defensive .modal-header .close CSS
```

(Commit SHAs change on rebase; use `git log --oneline bpmn-editor-blimpkit-migration ^origin/master`
for the current list.)

**Done (steps 0-5):**
- All 21 BPMN-editor popups + the form-builder popover are `<bk-dialog>`. `grep -l
  'class="modal"'` over `editor-app/configuration/properties/`, `editor-app/popups/`, and
  `views/popover/` returns empty.
- All 8 inline property-panel write templates use `<bk-input>` / `<bk-checkbox>` /
  `<bk-select>` instead of native `<input>` / `<select>`. `editor-app/editor.html`'s
  `.property-row` shell is left as-is (deeply integrated with Oryx's dynamic re-render;
  changing it would mean rewriting `properties.js`).
- Every glyphicon-* in BPMN-editor-specific HTML/JS has been swept to a `sap-icon--*`
  equivalent. The glyphicons that remain live in shared code with the other Flowable
  perspectives (form-builder / decision-table / app-definition / casemodel) — out of
  scope per the §1 "Don't delete those views" rule.
- The defensive `.modal-header .close` CSS rule is gone.

**Still open (step 6, and partials):**
- `--sap*` legacy-variable audit (step 6) — the shim from `1329f23be5` is still shipping
  via `theme-blimpkit/css/sap-variables-{light,dark,auto}.css`. Multiple editors and the
  BPMN editor's own `style.css` reference `var(--sapTextColor)` etc. Replacing each call
  with a Fundamental-Styles equivalent (`var(--sapTextColor)` → `var(--fdShellbar_TextColor)`
  / `var(--sapContent_LabelColor)` / whichever applies semantically) needs case-by-case
  judgement and a visual review per consumer.
- The `dirigible-theme-{light,dark,auto}` body class stays — it still drives
  `.dark-mode .Oryx_button img / .stencil-item img { filter: invert(…) }` in `style.css`.
  Replacing those rules with a BlimpKit-native theming hook is its own task.
- Bootstrap-3 CSS + JS in `index.html` stay loaded. The casemodel / form-builder /
  decision-table / app-definition perspectives ship 9+ Bootstrap-3 modal templates under
  `views/popup/*.html` that go through the same `$modal` factory, and the migrated
  popups still use Bootstrap-3 grid classes (`.row`, `.col-xs-*`, `.col-md-*`) for the
  two-column listener-grid + detail-pane layout.
- The dual-mode branch in `modal-service.js` stays for the same reason — `$modal` is
  invoked from both the migrated BPMN-editor popups and the unmigrated other-perspective
  popups.

All three BPMN ITs pass on `HEAD`:
- BpmnEditorLoadsIT       44.15s
- BpmnEditorIT            40.85s
- BpmnEditorPropertyPopupIT  27.54s

The Chrome renderer can occasionally time out at `IDE.openHomePage` on macOS 13 / Chrome
148 with this PR loaded — that's environmental flake (server responds in ~120ms when
poked directly with curl), reproducible across runs both with and without my changes.
The retry-by-rerunning approach works.

### 2.1 Inventory of what to migrate

#### Pop-up templates (20 files)
All under `components/ui/editor-bpm/src/main/resources/META-INF/dirigible/editor-bpm/editor-app/configuration/properties/`:

```
assignment-popup.html               form-properties-popup.html
case-reference-popup.html           form-reference-popup.html
condition-expression-popup.html     in-parameters-popup.html
data-properties-popup.html          message-definitions-popup.html
decisiontable-reference-popup.html  out-parameters-popup.html
duedate-popup.html                  process-reference-popup.html
event-listeners-popup.html          sequenceflow-order-popup.html
execution-listeners-popup.html      signal-definitions-popup.html
feedback-popup.html                 task-listeners-popup.html
fields-popup.html                   text-popup.html
```

Plus `views/popover/formfield-edit-popover.html` (form-builder) and any other `*.html`
under `views/` that's used by the BPM editor (NOT the casemodel / forms / decision-tables
templates the user wants kept as-is).

Each popup currently follows this Bootstrap-3 shape:

```html
<div class="modal" ng-controller="…PopupCtrl">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button class="close" data-dismiss="modal" ng-click="close()">&times;</button>
        <h2>…</h2>
      </div>
      <div class="modal-body">…form fields…</div>
      <div class="modal-footer">
        <button ng-click="cancel()" class="btn btn-primary">Cancel</button>
        <button ng-click="save()"   class="btn btn-primary">Save</button>
      </div>
    </div>
  </div>
</div>
```

BlimpKit equivalent (see `components/resources/platform-core/src/main/resources/META-INF/dirigible/platform-core/ui/templates/dialogs.html`):

```html
<bk-dialog visible="true">
  <bk-dialog-header header="true" title="…">
    <bk-button glyph="sap-icon--decline" round="true" compact="true" ng-click="$hide()"></bk-button>
  </bk-dialog-header>
  <bk-dialog-body>…form fields, ideally bk-form-group / bk-form-item / bk-input…</bk-dialog-body>
  <bk-dialog-footer>
    <bk-bar-element><bk-button label="Cancel" decisive="true" ng-click="cancel()"></bk-button></bk-bar-element>
    <bk-bar-element><bk-button label="Save"   decisive="true" ng-click="save()"></bk-button></bk-bar-element>
  </bk-dialog-footer>
</bk-dialog>
```

#### Property-panel rendering
Property rows are rendered by `editor-app/configuration/properties.js` + the
`*-display-template.html` / `*-write-template.html` pairs in `…/properties/`. The shell is in
`editor-app/editor.html` near `#propertySection`. The rows currently use `.property-row` /
`span.title` / `.form-control` markup — a candidate for `<bk-list>` / `<bk-list-item>` or
`<bk-form-group>` styling, depending on the look the user wants.

#### CSS using `--sap*` variables (BlimpKit alignment beyond BPM editor)
The legacy `--sap*` variables are now back (commit `1329f23be5`), so these editors visually
work. If the BlimpKit migration shifts off `--sap*` entirely, audit:

```
components/ui/editor-form-builder/.../css/editor.css
components/ui/editor-integrations/.../css/theme.css
components/ui/editor-csv/.../css/csv.css
components/ui/editor-entity/.../css/styles.css
components/ui/editor-mapping/.../css/styles.css
components/ui/editor-schema/.../css/styles.css
components/ui/editor-bpm/.../editor-app/theme/{style,stencils}.css
components/ui/perspective-processes/.../views/styles/bpm-process-viewer.css
components/resources/resources-documents/.../styles/documents.css
components/ui/view-java-debug/.../debug.html
components/ui/view-transfer/.../transfer.html
components/ui/view-console/.../console.html
components/ui/view-loggers/.../loggers.html
components/ui/editor-mapping/.../js/editor.js
```

#### `glyphicon-*` icons
Bootstrap-3 glyphicons appear throughout the popup templates (`glyphicon-arrow-up`,
`glyphicon-plus`, `glyphicon-minus`, `glyphicon-question-sign`, etc.). BlimpKit equivalent
is `sap-icon--*` via `<bk-icon glyph="sap-icon--…">` or the `glyph` attribute on
`<bk-button>`. A find-and-replace pass against a glyphicon → sap-icon table is the cleanest
way to convert (the user has done this elsewhere — look for `sap-icon--add`, `sap-icon--decline`
in existing IDE views).

#### `bs-*` directives — none left
All angular-strap `bs-select` / `bs-tooltip` / `bs-popover` usages have been removed or
replaced. Don't reintroduce them.

### 2.2 Available BlimpKit components

The BlimpKit version on `origin/master` is `2.1.6` (see `components/resources/resources-theme-blimpkit/pom.xml`).

~106 `bk-*` components are in use across the Dirigible codebase — see the list in the
chat-history search at the top of section 2. The ones most relevant to the BPM editor
migration:

| Need | BlimpKit |
|---|---|
| Modal / popup dialog | `bk-dialog`, `bk-dialog-header`, `bk-dialog-body`, `bk-dialog-footer` |
| Form fields | `bk-form-group`, `bk-form-item`, `bk-form-label`, `bk-input`, `bk-textarea`, `bk-select` + `bk-option`, `bk-checkbox`, `bk-radio`, `bk-switch`, `bk-step-input` |
| Buttons / icons | `bk-button` (with `glyph="sap-icon--…"`), `bk-segmented-button` |
| Lists / property tables | `bk-list`, `bk-list-item`, `bk-list-navigation-item`, `bk-form-list` |
| Tabs (if rebuilding the property panel) | `bk-icon-tab-bar`, `bk-icon-tab-bar-tab`, `bk-icon-tab-bar-panel` |
| Validation messages | `bk-form-input-message`, `bk-form-message` |
| Action bars | `bk-toolbar`, `bk-bar`, `bk-bar-element` |
| Popover (in-canvas hover help) | `bk-popover`, `bk-popover-control`, `bk-popover-body` |

Reference real-world uses in the codebase:

- `components/resources/platform-core/src/main/resources/META-INF/dirigible/platform-core/ui/templates/dialogs.html` — production `bk-dialog` patterns
- `components/ui/editor-monaco/...` — modern editor that's already BlimpKit-aligned
- `components/ui/perspective-processes/...` — BPM-adjacent perspective that is BlimpKit-aligned

### 2.3 Recommended migration approach

Iterate, don't big-bang. The editor must keep working through every commit. Suggested order:

0. **DONE in `dd677b78fa`. Prerequisite — wire BlimpKit into the editor iframe.** The editor-bpm iframe is a
   self-contained Angular 1.8.2 app (`ng-app="flowableModeler"`); it does **not** inherit
   the IDE shell's script/link tags. As of `fix-bpmn-editor` it loads jQuery 2.0.3,
   AngularJS 1.8.2, angular-translate, ui-grid, Bootstrap-3 CSS and **no BlimpKit assets at
   all**. A `<bk-dialog>` tag in any popup template renders as an unknown element until
   four things are added to `editor-bpm/index.html`:
   - `<link rel="stylesheet" href="/webjars/blimpkit__blimpkit/dist/css/blimpkit.css">`
   - `<link rel="stylesheet" href="/webjars/fundamental-styles/dist/fundamental-styles.css">`
   - `<script src="/webjars/angular-aria/angular-aria.min.js">` (the `blimpKit` module
     depends on `ngAria` — see `platform-core/ui/blimpkit/blimpkit.js`)
   - `<script src="/webjars/blimpkit__blimpkit/dist/blimpkit.min.js">` (the bundled
     ~158 KB module defining every `bk-*` directive — webjar version `2.1.6`)

   Plus `'blimpKit'` added to the `flowableModeler` module-dep array in `scripts/app.js`.
   See the live recipe in `platform-links.json` (`ng-view` category) — that's what the
   IDE shell injects via `<meta name="platform-links">`, and it's the canonical list.

   **Side-effects to know about** before flipping this on:
   - The `blimpKit` module's `.config()` block calls `$compileProvider.cssClassDirectivesEnabled(false)`
     / `commentDirectivesEnabled(false)` / `debugInfoEnabled(false)` once debug is on.
     Those are app-global flags. The `cssClassDirectivesEnabled(false)` part is safe — the
     class-form-permissive directives in editor-bpm (`autoHeight`, `scrollToActive`,
     `autoScroll`, `autoFocus`, `activitiFixDropdownBug`) are all attribute-used everywhere
     I checked. But **`debugInfoEnabled(false)` is a real regression for the BPMN
     integration tests**: `BpmnEditorIT.bpmnEditor_select_rename_and_save` and
     `BpmnEditorPropertyPopupIT.executionListenersPopup_opens_and_closes_without_locking_the_editor`
     both reach into Angular via `angular.element(node).scope()` (to call `$apply` and to
     read `scope.$hide`), and Angular only attaches scope references to DOM nodes when
     debug info is on. The shipped fix is a one-line `.config()` block in `scripts/app.js`
     that re-enables all three flags. It runs after `blimpKit.config()` (dependency
     config blocks always run first), so the override sticks.
   - Bootstrap-3 CSS stays loaded (the not-yet-migrated popups still need its `.modal`
     styles). The Fundamental-Styles `.fd-*` classes don't collide by name, but z-index /
     `.modal-backdrop` interaction needs eyes-on the first time a `<bk-dialog>` opens.
   - Loading `blimpkit.min.js` BEFORE `app.js` is mandatory — `app.js`'s `angular.module(...)`
     references `'blimpKit'`, which has to be registered already.

1. **DONE in `b5bb5eb055`. Replace the modal-service.js implementation, not its API.**
   Keep the `$modal` factory signature (`$modal({ template, scope, prefixEvent }) → instance`) so the ~30 call-sites
   (`_internalCreateModal`, every `FlowableXxxCtrl`) don't have to change. Internally swap
   from `jQuery(element).modal(...)` to compiling `<bk-dialog>` markup and managing
   visibility via Angular. Preserve `$scope.$hide()`, `$scope.$show()`, `prefixEvent`, and
   the `<prefix>.{show,hide,…}` events that controllers depend on.
   *Practical hint:* the service has to keep working while only some popups have been
   migrated. The shipped version uses a structural check (`/<bk-dialog\b/i.test(html)`)
   and branches: bk-dialog mode flips a `modal.visible` flag the dialog binds to;
   legacy mode keeps the `jQuery(el).modal(...)` path. Once every popup is on
   `<bk-dialog>`, delete the legacy branch and the jQuery modal plugin can come out.
2. **DONE in `37e5088b35` / `68abacfe2e` / `b6fcb6ae8c` / `e5e3e1bc28` / `0b6c0e144b`. Migrate one popup template at a time**, ideally starting with `text-popup.html`
   (simplest) → `execution-listeners-popup.html` (covered by `BpmnEditorPropertyPopupIT`) →
   the rest. After each, run the three ITs.
   *Practical hint:* `<bk-dialog>` has an isolate scope, so you can't put
   `ng-controller="FlowableXxxPopupCtrl"` on the dialog element itself — Angular throws
   "Multiple directives asking for new/isolated scope". Wrap with a thin `<div ng-controller="...">`
   that hosts the controller, then place `<bk-dialog visible="modal.visible">` inside.
3. **DONE in `af8d12997c`. Convert `.property-row` markup** in `editor.html` and the property write templates to
   `bk-form-item` / `bk-input` etc. This is the biggest visual win but requires care because
   Oryx's property panel re-renders dynamically. The property panel shell lives in
   `editor-app/editor.html` near `#propertySection`; the per-property templates live in
   `editor-app/configuration/properties/*-display-template.html` and
   `*-write-template.html`. The dynamic re-render is driven by `properties.js` (the
   `flowableProperty` directive — grep for it). Walk through each `*-write-template.html`,
   convert `.form-control` inputs / selects / textareas to bk-* equivalents, then update
   the display template if it renders the value in a Bootstrap-3-specific shell.
   *Practical hint, learned from the popup migration*: bk-input / bk-textarea / bk-select
   all use `ng-model` against the parent scope (their isolate scope only owns their own
   `compact` / `state` / `glyph` bindings); the existing `selectedProperty.foo` references
   keep working unchanged.
4. **DONE in `99f358cd70` (BPMN-editor scope only). Sweep `glyphicon-*` → `sap-icon--*`.** Mechanical, low-risk. The popup-template
   migration already converted every glyphicon inside dialogs to `sap-icon--*`; the
   remaining glyphicons are in `editor-app/editor.html`, the property panel
   templates, and the form-builder shell. A `grep -rn 'glyphicon-' editor-bpm/src` is the
   inventory.
5. **PARTIAL DONE in `26899f1217`. Drop the `.modal-header .close` defensive CSS** in `editor-app/theme/style.css`,
   the `dirigible-theme-{light,dark,auto}` body class, and the Bootstrap-3 `<script>` +
   `<link>` from `index.html` once `<bk-dialog>` and BlimpKit-native filter rules are in
   for every popup. After this commit (step 2 complete), only the property panel still
   uses Bootstrap-3 styles — once step 3 lands, all of Bootstrap-3 can come out, including
   the dual-mode branch in `modal-service.js`.
6. **TODO. Audit `--sap*` usage in the editor's own CSS** (`editor-app/theme/{style,stencils}.css`). Once everything renders through BlimpKit components, the `--sap*` legacy shim from `1329f23be5` can stop shipping in the editor's links — but only after every consumer is converted. The shim is still in place (it shims for the editor-bpm CSS, the form-builder CSS, perspective-processes CSS, and the documents resources CSS) — replacing `var(--sapTextColor)` with Fundamental-Styles' `var(--sapContent_LabelColor)` / `var(--sapShell_TextColor)` / etc. requires a per-call decision on which Fundamental variable corresponds best.

### 2.4 Tests to extend

The current ITs guard the *functional* layer. For the BlimpKit migration, also add:
- A test that **opens at least one popup of each shape**: simple text popup, listener-grid popup (Execution Listeners is already covered), reference-picker popup (e.g. Form reference), structured editor popup (Data Properties). One assertion per popup that the `bk-dialog` element renders and that the Save/Cancel buttons close it.
- A test that **renders the property panel for several stencil types** (Start Event, User Task, Service Task, Gateway) and asserts a `bk-form-item` exists for each known property.

Both can extend `BpmnEditorPropertyPopupIT` or live in new sibling ITs. Keep them headless
(`-D selenide.headless=true`).

---

## 3. Build & test commands

```bash
# Quick rebuild after editing JS / HTML / CSS in editor-bpm or theme-blimpkit
mvn -pl components/ui/editor-bpm,components/resources/resources-theme-blimpkit,build/application -am install -P quick-build

# Smoke test only
mvn -pl tests/tests-integrations install -P integration-tests \
    -Dit.test="BpmnEditorLoadsIT" -D selenide.headless=true

# All three BPM editor tests
mvn -pl tests/tests-integrations install -P integration-tests \
    -Dit.test="BpmnEditorLoadsIT,BpmnEditorIT,BpmnEditorPropertyPopupIT" \
    -D selenide.headless=true

# Run the app standalone to test in a real browser (admin/admin at http://localhost:8080)
java -DDIRIGIBLE_SFTP_SERVER_ENABLED=false \
    -jar build/application/target/dirigible-application-13.0.0-SNAPSHOT-executable.jar
```

---

## 4. Gotchas that bit me — read before editing

1. **AngularJS bootstrap timing.** Anything `app.js`'s `.config()` block touches at *call-site*
   time (not inside a controller / factory body) must already exist. `editorParams` is set
   by the inline script at the *bottom* of `index.html`'s body — it's defined when `.config()`
   runs *because* Angular auto-bootstrap waits for `DOMContentLoaded` and all scripts to
   complete. If you ever change to manual `angular.bootstrap(...)`, this guarantee breaks.

2. **The IDE iframe sets `data-parameters="{...JSON...}"` on its iframe element.** The
   inline script in `index.html` reads it via `window.frameElement.getAttribute(...)` and
   assigns to the global `editorParams`. The `redirectTo` function in `app.js` defers
   reading `editorParams` so it's read *after* the inline script — leave it as a function,
   don't inline.

3. **Bootstrap-3's `.modal('show')` is unreliable in this iframe.** That's why my
   modal-service explicitly sets `element[0].style.display = 'block'`. Don't trust
   `$.fn.modal('show')` alone.

4. **`$emit` vs `$broadcast`.** Modal/popover events must propagate *up* to the caller's
   scope (`$scope.$on('modal.hide', …)` or `$scope.$parent.$on('modal.hide.before', …)`).
   Use `$emit` — `$broadcast` only goes *down*.

5. **The bundled `libs/angular/1.4.7/` directory in `components/resources/resources-flowable-libs/`
   is now orphaned** (nothing references it). Removing it is safe; it was not removed on
   `fix-bpmn-editor` to keep the diff focused. The prior `bpmn-editor-modernization` branch
   removed it.

6. **`BpmnEditorLoadsIT` asserts `--sapBackgroundColor` is non-empty inside the iframe.** If
   you remove the `sap-variables-{light,dark,auto}.css` from `theme-blimpkit`'s `links`,
   this test will fail. Either keep them or replace every `var(--sapXxx)` reference in the
   editors that still use them (see CSS inventory in §2.1).

7. **`@media (prefers-color-scheme: dark)` is the OS preference, not the Dirigible theme.**
   `setTheme()` in `index.html` writes `body.dirigible-theme-{light,dark,auto}` so CSS can
   target the *picked* theme. Prefer the body class; reserve `prefers-color-scheme` for the
   `dirigible-theme-auto` fallback.

8. **`$scope.$hide()` is what the original Flowable popup controllers call.** It used to be
   added to the modal scope by angular-strap; my `modal-service.js` re-adds it. Keep adding
   it (or its equivalent) in whatever `$modal` replacement comes next.

---

## 5. Project version pinning

- AngularJS: **1.8.2** via `org.webjars:angularjs` (in `components/resources/platform-core/pom.xml`).
- jQuery: **2.0.3** (bundled in `components/ui/editor-bpm/.../scripts/jquery-2.0.3.min.js`). Upgrade is decoupled from the BlimpKit migration but the jQuery 2.x → 3.x upgrade is a candidate follow-up.
- BlimpKit: **2.1.6** via `org.webjars.npm:blimpkit__blimpkit` (in `components/resources/resources-theme-blimpkit/pom.xml`). Older 1.9.1 jars are still in `~/.m2/repository` and we extracted the legacy `*-variables.css` from 1.9.1 for the theme fix — but production should not depend on 1.9.1 jars existing in m2.
- Bootstrap (CSS only): **3.1.1** (`libs/bootstrap_3.1.1/css/bootstrap.min.css`). The BlimpKit migration may let us drop this entirely.
- Spring Boot: **4.0.6** (since commit `e959d639`, "Migration the project from Spring Boot v3.5.9 to v4.0.6 (#5918)").
