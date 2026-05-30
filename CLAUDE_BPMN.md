# BPM Editor Modernization — Working Context

## Status: RESOLVED ✅

Both target tests pass on branch `bpmn-editor-modernization`:
- `BpmnEditorLoadsIT` — 49.66 s (canvas + stencil palette visible)
- `BpmnEditorIT` — 57.29 s (select shape, rename, save → "Published")

## Root Cause of the Blank Page

Angular 1.6 changed the default `$locationProvider.hashPrefix` from `''` to `'!'`. The IDE platform constructs the editor iframe with URL `…/index.html#/editor` (no `!`). Under Angular 1.8.2's default `!` prefix, `#/editor` is parsed as an empty path → falls through to the `/` route → redirects to `/processes` → tries to load `views/processes.html` → 404 → blank `ng-view`.

Diagnosed via a temporary in-iframe console-error overlay that surfaced the `$templateRequest:tpload` failure and the `routeChangeStart` sequence (`?` → `/` → `/processes`).

## Final Fix Set

### `scripts/app.js`

1. **`$locationProvider.hashPrefix('')`** added at the top of the `.config()` block — restores the pre-1.6 hash format so `#/editor/...` URLs route correctly.
2. **Route changed `'/editor/'` → `'/editor'`** (no trailing slash) to match the iframe's `#/editor` URL exactly.
   - A separate `.when('/editor/', { redirectTo: '/editor' })` was tried but caused `$rootScope:infdig` (Angular's trailing-slash normalization looped). Don't re-add.
3. **`redirectTo` made a function** instead of a template literal — defers `editorParams` access to route-evaluation time (post-bootstrap) and guards against `editorParams` being undefined.
4. Removed `mgcrea.ngStrap` / `mgcrea.ngStrap.helpers.dimensions` from module deps.
5. Removed `$selectProvider` injection and `$selectProvider.defaults` override.
6. Removed unused `$modal` injection from the second `.run()` block.

### `index.html`

- Swapped `libs/angular/1.4.7/*` → `/webjars/angularjs/1.8.2/*` (WebJar served by Spring Boot from the `angularjs` artifact declared in `components/resources/platform-core/pom.xml`).
- Inline shims (right after `angular.min.js`) for `angular.lowercase`/`angular.uppercase` — removed in 1.7.3, still used by `ui-grid` 3.0.0 and `angular-translate` 2.15.1 at module-load time.
- Removed `angular-strap_2.1.6` and its `.tpl.min.js`.
- Added `scripts/services/modal-service.js` script tag.

### `scripts/services/modal-service.js` *(new)*

Bootstrap-3 jQuery-based `$modal` factory replacing angular-strap's. Consumed by `EditorController` and `StencilController` (and other callers via `_internalCreateModal`).

### `editor-app/editor-controller.js`

- `$http(...).success(...).error(...)` → `.then(success, error)`.
- `$routeParams.modelId` → `$routeParams.workspace` (the route `/editor/:workspace/:project/:path*` has no `:modelId`).

### `.success()/.error()` → `.then()/.catch()` in 14 scripts

- `editor-app/configuration/properties-case-reference-controller.js`
- `editor-app/configuration/properties-decisiontable-reference-controller.js`
- `editor-app/configuration/properties-form-reference-controller.js`
- `editor-app/configuration/properties-process-reference-controller.js`
- `editor-app/configuration/toolbar-default-actions.js`
- `scripts/common/directives.js`
- `scripts/common/services/resource-service.js`
- `scripts/controllers/app-definition-builder.js`
- `scripts/controllers/app-definition.js`
- `scripts/controllers/app-definitions.js`
- `scripts/controllers/model-common-actions.js`
- `scripts/controllers/process.js`
- `scripts/controllers/processes.js`
- `scripts/services/identity-services.js`

### Deleted

- `components/resources/resources-flowable-libs/src/main/resources/META-INF/dirigible/editor-bpm/libs/angular/1.4.7/` (entire directory).

## Known Dead Code (Not Blocking)

These code paths still inject angular-strap's `$popover` (no longer available) but are unreachable from the BPM editor file-open flow, so they don't affect the two target tests:

- `scripts/editor-directives.js` — `formBuilderElement`, `storeCursorPosition` directives (not referenced in any template).
- `scripts/common/directives.js` — `selectPeoplePopover`, `selectFunctionalGroupPopover` directives (not referenced).
- `scripts/controllers/{process,app-definition,app-definition-builder}.js` — wired to `/processes/:id`, `/apps/:id`, `/app-editor/:id` routes that the IDE never navigates to from a `.bpmn` file open.
- `editor-app/configuration/properties/duedate-popup.html` uses `bs-select` (angular-strap attribute directive). Without angular-strap it degrades to a plain `<select>` — fine for the smoke + basic edit/save flow.

If future work needs the `/processes` list view etc. inside the BPM editor, these callers will need their `$popover` / `bs-select` usages replaced (e.g. with Bootstrap-3 jQuery popovers or BlimpKit equivalents).

## Build & Test Commands

```bash
# Quick rebuild after editing JS/HTML
mvn -pl components/ui/editor-bpm,build/application -am install -P quick-build

# Smoke test only
mvn -pl tests/tests-integrations install -P integration-tests \
    -Dit.test="BpmnEditorLoadsIT" -D selenide.headless=true

# Both BPM editor tests
mvn -pl tests/tests-integrations install -P integration-tests \
    -Dit.test="BpmnEditorLoadsIT,BpmnEditorIT" -D selenide.headless=true
```
