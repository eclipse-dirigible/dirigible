# CLAUDE_UI.md

Guidance for producing Dirigible UI views/perspectives that ship in
`components/ui/*` and render through BlimpKit (the AngularJS-on-Fundamental-Styles
component library bundled at `/webjars/blimpkit__blimpkit/dist/blimpkit.min.js`,
Angular module name **`blimpKit`**).

Three parts:

0. **Five immutable rules** - the non-negotiable house style. Read first.
1. **Recent lessons from production fixes** - what to do (and not do) when authoring a new view, distilled from the latest UI cleanups on monitoring views (commits `3fd93aa6`, `e0113335`, `68cb628e`).
2. **BlimpKit context** extracted from `CLAUDE.md` and `CLAUDE_BPMN.md` - gotchas, available components, and the iframe wiring recipe.

Read part 0 before doing anything else. Read part 1 before writing a single line of HTML/CSS in a new view. Read part 2 before reaching for `<style>`, jQuery, or a Bootstrap-3 idiom.

---

## 0. Five immutable rules

These override every other instinct. If a rule below conflicts with a pattern you see elsewhere in the codebase, the rule wins - the older code is a candidate for cleanup, not a precedent.

1. **Perspective icons have no internal padding.** The `<svg>` content area must extend to the edges of the `viewBox`. The BlimpKit-rendered icon slot draws its own spacing; padding inside the SVG makes the icon visibly smaller than its neighbours in the sidebar and produces an inconsistent rail. Trim any internal margin out of the path data when adapting a third-party icon.
2. **No em dashes.** Use the standard hyphen-minus (`-`) only. Em dashes (`—`) and en dashes (`–`) are out. We write software, not romance novels - in prose, code comments, commit messages, and Markdown alike. If you copy text from a chat or doc, sweep it for `—` before saving.
3. **Custom CSS classes only when there is no other option.** First reach: a BlimpKit utility class (`bk-text--subtitle`, `bk-font--small`, `bk-vbox`, `bk-box--gap`, `bk-padding--tiny`, etc.). Second reach: a Fundamental-Styles class (`fd-*`). Last reach: a custom class scoped to the view - and only when none of the above expresses what you need. Adding a `.metrics-toolbar-sub` rule that just sets a smaller font and a muted color is the canonical anti-pattern - that's `<span class="bk-text--subtitle bk-font--small">` and the rule was unnecessary.
4. **Never hard-code colors, and prefer BlimpKit variables over SAP-legacy ones.** Not in hex, not in rgb, not in `<path fill="…">`. The lookup order:
   1. **A BlimpKit utility class** that already maps to the right theme variable (`bk-color--negative`, `bk-color--positive`, `bk-color--critical`, `bk-color--informative`, `bk-text--subtitle`, `bk-color-bg--negative`, etc.). This is the strongly preferred form.
   2. **A BlimpKit theme variable** (semantic, short names defined in `/webjars/blimpkit__blimpkit/dist/css/themes/blimpkit-light.css` + `…-dark.css`): `var(--negative)` / `var(--positive)` / `var(--warning)` / `var(--information)` (each with `-foreground`, `-hover`, `-active` variants), `var(--background)`, `var(--foreground)`, `var(--muted)`, `var(--muted-foreground)`, `var(--accent)`, `var(--border)`, `var(--card)`, `var(--object-background)`, `var(--shellbar-background)`, `var(--sidebar)`, `var(--table-header)`, `var(--font-sans)`, `var(--font-mono)`, `var(--input-background)`, `var(--scrollbar)`. See the full list in §2.7.
   3. **A `--sap*` legacy variable** only when nothing above matches. The `--sap*` shim is still shipping for backwards compatibility but is being phased out - reaching for `var(--sapNegativeColor)` when `var(--negative)` exists is the wrong choice. Anything you write that still says `var(--sap*)` should have a comment explaining why no BlimpKit equivalent worked.

   Hard-coded colors break dark mode, theme switching, and contrast guarantees. The same rule kills inline color styles on dashboards (`color: #b00`) and inline `fill="#000000"` on perspective SVGs.
5. **Use BlimpKit components instead of building view-local equivalents.** Before writing a `<div class="my-tile">` shell with a custom CSS look, search the BlimpKit cheat sheet in §2.3 - the component you need almost certainly exists (`bk-card`, `bk-object-status`, `bk-progress-indicator`, `bk-list`, `bk-message-page`, `bk-popover`, etc.). The pattern below is what you want:

   ```html
   <!-- WRONG: invented a custom CSS class for what is just a subtitle -->
   <style>.metrics-toolbar-sub { font-size: 0.7rem; color: var(--sapContent_LabelColor, #6a6d70); }</style>
   <span class="metrics-toolbar-sub" ng-show="lastUpdated">{{lastUpdated | date:'HH:mm:ss'}}</span>

   <!-- RIGHT: BlimpKit utility classes do the same job, no custom CSS -->
   <span class="bk-text--subtitle bk-font--small">{{lastUpdated | date:'HH:mm:ss'}}</span>
   ```

6. **For dialogs, use `DialogHub` - never put `<bk-dialog>` markup in a view.** Dirigible has a platform-wide dialog system: every view holds a `new DialogHub()` instance and calls `Dialogs.showAlert(...)` / `Dialogs.showDialog(...)` / `Dialogs.showFormDialog(...)` / `Dialogs.showWindow(...)` / `Dialogs.showBusyDialog(...)`. The shell listens for these messages and renders the dialog with consistent chrome, focus management, and theming. Putting raw `<bk-dialog>` markup inside a view is what broke dialogs in the old BPMN editor - don't repeat it. Details in §2.4 below; the `<bk-dialog>` directive itself is reserved for the platform-core shell that hosts the dialog, not for view authors.

Apply these checks against your own diff before requesting review. A reviewer who finds an em dash, a custom CSS class that replicates a BlimpKit utility, a hex color in a perspective, or a `<bk-dialog>` tag in a view template should bounce the PR.

---

## 1. Recent lessons (StanZGenchev's monitoring-view cleanups)

These three commits replaced hand-rolled CSS, ad-hoc pills/bars/tiles, and Bootstrap-leftover layout with BlimpKit-native equivalents. Treat them as **the canonical pattern** for new monitoring/dashboard/list views.

### 1.1 Prefer BlimpKit components over hand-rolled CSS

If a layout primitive exists as a `bk-*` directive, use it. The monitoring-view fix deleted ~350 lines of bespoke CSS - `.jvm-tile`, `.jvm-bar`, `.jvm-state-pill`, `.metrics-row`, `.jvm-threads-filters`, custom `--sap*`-keyed colors - and replaced them with their BlimpKit counterparts.

| Don't write | Use instead |
|---|---|
| `<div class="jvm-tile"><div class="jvm-tile-label">…</div><div class="jvm-tile-value">…</div></div>` | `<bk-card card-type="object"><bk-card-content class="bk-vbox bk-box--gap">…</bk-card-content></bk-card>` |
| `<div class="jvm-bar"><div class="jvm-bar-fill warn" style="width: 80%"></div></div>` | `<bk-progress-indicator current-value="80" no-label="true" state="critical" aria-label="…"></bk-progress-indicator>` |
| `<span class="jvm-state-pill jvm-state-RUNNABLE">RUNNABLE</span>` | `<span bk-object-status status="positive" text="RUNNABLE" inverted="true"></span>` |
| `<div class="metrics-row"><span>…</span><span>…</span></div>` | `<bk-list compact="true"><bk-list-item interactive="true"><bk-list-title>…</bk-list-title><bk-list-secondary>…</bk-list-secondary></bk-list-item></bk-list>` |
| `<div class="jvm-section-title">…</div>` | `<h2 class="bk-text--subtitle bk-font--small bk-padding--tiny fd-margin--none">…</h2>` |
| Custom `.jvm-threads-empty` cell for "no rows" | `<td bk-table-cell no-data="true">No threads…</td>` |
| `<bk-toolbar-label>Title</bk-toolbar-label>` | `<bk-toolbar compact="true" has-title="false"><span>Title</span>…</bk-toolbar>` |

The lone surviving CSS in the cleaned-up views is a one-rule grid that bk-card can't express on its own:

```css
.jvm-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(13.75rem, 1fr));
}
```

That's the threshold - when BlimpKit + the `bk-*` utility classes (see §1.2) cover it, drop the `<style>` block entirely.

### 1.2 Use the BlimpKit utility classes, not inline styles or per-view CSS

The cleanups standardise on these utility classes - they ship with BlimpKit and replace the hand-written `font-size: 0.75rem; color: var(--sapContent_LabelColor); …` patterns scattered across the old views.

| Class | What it does |
|---|---|
| `bk-vbox` / `bk-hbox` | Flex column / row |
| `bk-box--gap` | Standard gap between flex children |
| `bk-box--items-center` | Cross-axis center |
| `bk-box--space-between` | Justify space-between |
| `bk-flex--wrap` | Allow flex wrapping |
| `bk-flex-1` | `flex: 1` |
| `bk-full-height` | Fill parent height |
| `bk-center` | Center content (for empty states) |
| `bk-padding--tiny` | Standard small padding |
| `bk-text--subtitle` | Muted label color |
| `bk-font--small` / `bk-font--normal` / `bk-font--large` / `bk-font--h4` | Type scale |
| `bk-font--bold` | Bold weight |
| `bk-font--mono` | Monospace font (resolves to `var(--font-mono)`) |
| `bk-color--negative` / `--positive` / `--critical` / `--informative` | Semantic text colors. These wire to BlimpKit's `var(--negative)` / `var(--positive)` / `var(--warning)` / `var(--information)` theme variables - reach for the class first; only fall back to `var(--negative)` etc. when the class doesn't fit (e.g. inline SVG `fill`). Never `var(--sapNegativeColor)`. |
| `bk-color-bg--negative` / `--positive` / `--critical` / `--informative` | Same semantics, applied as `background-color` |

Rule of thumb: if you're typing `<style>` or `style="…"` for typography, color, spacing, or flex layout, you're recreating something BlimpKit already ships. Look at `components/ui/perspective-monitoring/` (post-cleanup) for the reference look.

### 1.3 Responsive filter rows - wrap each label+control as a unit

A naïve "label + select, label + select, button" row reflows by *element* on narrow viewports, so the label "State" can end up on one line and its `<bk-select>` on the next. Fix from commit `68cb628e`: wrap each label+control in its own `bk-hbox` so they wrap as a group.

```html
<div class="bk-hbox bk-flex--wrap bk-box--items-center bk-box--gap">
    <div class="bk-hbox bk-box--items-center bk-box--gap">
        <label class="bk-text--subtitle" for="i-state">State</label>
        <bk-select id="i-state" compact="true" …></bk-select>
    </div>
    <div class="bk-hbox bk-box--items-center bk-box--gap">
        <label class="bk-text--subtitle" for="i-daemon">Daemon</label>
        <bk-select id="i-daemon" compact="true" …></bk-select>
    </div>
    <bk-button compact="true" state="transparent" glyph="sap-icon--clear-filter" label="Clear" …></bk-button>
    <div style="flex: 1"></div>
    <span class="bk-text--subtitle bk-font--small">Showing {{ filtered.length }} of {{ all.length }}</span>
</div>
```

Always pair `<label for="…">` with an `id` on the BlimpKit control - keyboards/screen readers depend on it.

### 1.4 `bk-object-status` - let it size itself, just stop the text from wrapping

`<bk-object-status>` is the right component for short status labels (thread state, build status, severity). Two things bit the monitoring view:

- **Custom pill CSS is wrong.** The old `.jvm-state-pill { padding: 1px 6px; border-radius: 9999px; … }` reinvented what `bk-object-status` already draws. Replace with `<span bk-object-status status="…" text="…" inverted="true">`.
- **The text needs `white-space: nowrap` to stop wrapping inside the colored pill.** Add it inline: `<span bk-object-status status="…" style="white-space: nowrap;" text="…" inverted="true">`. Without it, multi-word statuses (`TIMED_WAITING`) break across two lines inside the pill and the chip stretches vertically.

Map domain states to BlimpKit semantic states explicitly in the controller - don't hard-code colors:

```js
$scope.getObjectState = (state) => {
    if (state === 'RUNNABLE') return 'positive';
    if (state === 'BLOCKED' || state === 'TERMINATED') return 'negative';
    if (state === 'WAITING' || state === 'TIMED_WAITING') return 'informative';
    if (state === 'NEW') return '';   // neutral
};
```

Same applies to `bk-progress-indicator` - pass `state="critical"` / `state="negative"` / `''` instead of `'warn'` / `'danger'` / `''`. The named states are tied to the theme; the custom strings just produced unstyled bars.

### 1.5 `bk-list` is `compact` via attribute, not via inline `height`

Commit `e0113335` removed `<bk-list-item style="height: 2rem">` everywhere and added `compact="true"` to the parent `<bk-list>`. The list directive renders its rows compactly through Fundamental-Styles when the attribute is set; inline `height` overrides the row's vertical padding and produces clipped text.

```html
<!-- WRONG: forces row height, clips descenders -->
<bk-list>
    <bk-list-item style="height: 2rem" interactive="true" …>…</bk-list-item>
</bk-list>

<!-- RIGHT: uses BlimpKit's compact density -->
<bk-list compact="true">
    <bk-list-item interactive="true" …>…</bk-list-item>
</bk-list>
```

### 1.6 Sortable table headers - use sap-icon caret + `interactive="true"`

For a click-to-sort column header, mark the cell `interactive="true"` and render a `sap-icon--sort` / `sap-icon--up` / `sap-icon--down` icon inside, not a Unicode caret.

```html
<th bk-table-header-cell interactive="true" ng-click="setSort('id')">
    <div class="bk-hbox bk-box--gap bk-box--space-between bk-box--items-center">
        <span>#</span>
        <span class="sap-icon sap-icon--{{sortIcon('id')}} bk-font--normal"
              role="img" aria-label="{{ sortIconLabel('id') }}"></span>
    </div>
</th>
```

```js
$scope.sortIcon = (key) => {
    if ($scope.sort.key !== key) return 'sort';
    return $scope.sort.reverse ? 'down' : 'up';
};
$scope.sortIconLabel = (key) => {
    if ($scope.sort.key !== key) return 'Sort';
    return $scope.sort.reverse ? 'Descending' : 'Ascending';
};
```

Remember: `ng-class` on the `<th bk-table-header-cell>` element itself breaks (string-concatenates with the directive's own internal `ng-class`). Push conditional classes onto a child `<span>`. (CLAUDE.md gotcha - restated in §2.)

### 1.7 Table chrome - `outer-borders` and empty-row markup

- Use `outer-borders="horizontal"` (or `"top"`) on `<table bk-table>` so the table has top/bottom rules but no left/right rules - cleaner than `outer-borders="none"` and matches the monitoring views' final look.
- Render the empty-state row through the directive's own attribute, not a custom `class="…-empty"` CSS:

  ```html
  <tr bk-table-row ng-if="!filteredThreads.length && !loading">
      <td bk-table-cell no-data="true">
          {{ threads.length ? 'No threads match the current filters.' : 'No threads to display.' }}
      </td>
  </tr>
  ```

- Wrap a horizontally-overflowing table in `<div bk-scrollbar>` so the BlimpKit-styled scrollbar shows up.

### 1.8 Toolbars - `has-title="false"` + child span, not `bk-toolbar-label`

The current pattern (post-cleanup):

```html
<bk-toolbar compact="true" has-title="false">
    <span>View Title</span>
    <bk-toolbar-spacer></bk-toolbar-spacer>
    <span class="bk-text--subtitle bk-font--small">Last updated {{lastUpdated | date:'HH:mm:ss'}}</span>
    <bk-toolbar-separator></bk-toolbar-separator>
    <bk-button state="transparent" glyph="sap-icon--refresh" …></bk-button>
    …
</bk-toolbar>
```

`bk-toolbar-label` is gone from these views - `has-title="false"` + a child `<span>` renders the same chrome without the directive's extra wrapping.

### 1.9 No `<bk-select>` in toolbars - selects belong in forms

A `<bk-select>` is a **form input**. Toolbars are not forms. Toolbars host quick actions and settings expressed as buttons (refresh, pause, toggle, "change interval"). Dropping a `<bk-select>` into a toolbar visually clashes with the surrounding buttons and behaves like a form widget where the user expects an action.

The only inputs that are legitimate in a toolbar are `<bk-input type="text">` and `<bk-input type="search">` for inline filtering/searching - those still count as quick actions because they steer the toolbar's view.

For a settings picker like "refresh interval", the right shape is a `<bk-button>` that opens a `<bk-popover>` + `<bk-menu>`. The button surfaces the current choice ("Every 5s"); the menu lists the alternatives:

```html
<bk-popover>
    <bk-popover-control>
        <bk-button state="transparent"
                   label="{{refreshIntervals[refreshIntervalSeconds]}}"
                   is-menu="true" aria-haspopup="menu" aria-controls="refreshIntervals"></bk-button>
    </bk-popover-control>
    <bk-popover-body id="refreshIntervals" align="bottom-right">
        <bk-menu aria-label="refresh interval options">
            <bk-menu-item title="{{value}}" ng-click="setRefreshInterval(key)"
                          ng-repeat="(key, value) in refreshIntervals track by key"></bk-menu-item>
        </bk-menu>
    </bk-popover-body>
</bk-popover>
```

```js
$scope.refreshIntervalSeconds = 5;   // number, not string
$scope.refreshIntervals = { 2: 'Every 2s', 5: 'Every 5s', 10: 'Every 10s', 30: 'Every 30s' };
$scope.setRefreshInterval = (value) => { $scope.refreshIntervalSeconds = value; $scope.restartTimer(); };
```

Store numeric model values as numbers, not strings - the old `'5'` required `parseInt` on every read. Pure numbers compare cleanly and pass to `$interval` directly.

### 1.10 Use `rem` (not `px`) for inline widths/min-widths

The cleanup changed `style="min-width: 110px"` → `style="min-width: 7.5rem"`, `200px` → `12.5rem`, etc. Reasoning: BlimpKit's typography and density scale with the root font size; `px` widths break when the user (or the theme) changes the base size. If you must inline a width, divide by 16 and use `rem`.

### 1.11 SVG perspective icons - no internal padding, no hard-coded `fill`

When adding a perspective icon under `components/ui/perspective-*/src/main/resources/META-INF/dirigible/.../images/*.svg`:

- **No internal padding around the artwork** (rule 1 of §0). Path data must extend to the edges of the `viewBox` - a 24x24 viewBox uses x/y in roughly `[0..24]`, not `[2..22]`. The IDE sidebar slot draws its own spacing; an SVG that pads itself looks visibly smaller than neighbouring icons and the rail goes inconsistent. The cleanup in commit `3fd93aa6` redrew the monitoring icon at full 24x24 extent (`m20.4 0h-16.8a3.6 3.6 0 0 0-3.6 3.6v16.8a3.6 3.6 0 0 0 3.6 3.6h16.8a3.6 3.6 0 0 0 3.6-3.6v-16.8...`) and moved the body decoration to match.
- **Strip every `fill="…"` attribute** from `<path>`/`<circle>`/etc. - the CSS in `blimpkit.css` styles the SVG via `fill: var(--fdVerticalNav_Icon_Color, #303030)` and switches to `var(--sapSelectedColor)` on the active state. A hard-coded `fill="#000000"` (the default when you paste from an icon set) locks the icon to black and breaks dark-theme adaptation. (Rule 4 of §0 - never hard-code colors.)
- **Use `viewBox="0 0 24 24"`** with `width="512" height="512"` on the outer `<svg>`. (`stroke-width=".99999"` is the established style but doesn't affect rendering through this CSS.)
- **Add `xml:space="preserve"`** to the outer `<svg>` tag - that was the cleanup in commit `3fd93aa6`.
- **Don't add explicit `stroke-width` on every path** if the design doesn't need outlines - the monitoring icon cleanup removed them and the icon still renders correctly with whatever stroke the CSS provides.

### 1.12 Bump BlimpKit version in `pom.xml`, not in per-module configs

When a new BlimpKit feature is needed, bump the `<blimpkit.version>` property in the root `pom.xml` (currently `2.3.0` after commit `3fd93aa6` bumped from `2.2.2`). Don't override per-module - the version is centralised so every UI module gets the same `bk-*` directives.

---

## 2. BlimpKit context (carried over from CLAUDE.md and CLAUDE_BPMN.md)

This section consolidates everything BlimpKit-related from the existing CLAUDE.md and CLAUDE_BPMN.md notes so a UI-focused author has one place to look. The originals stay authoritative for non-UI material.

### 2.1 Where BlimpKit lives

The IDE shell and most editor perspectives render through **BlimpKit**, an AngularJS component library shipped as a webjar. BlimpKit's sources live in an external repo (<https://github.com/blimpkit/blimpkit.github.io>); inside this repo it is consumed only as the published artifact, served from `/webjars/blimpkit__blimpkit/dist/`. The Angular module name is `blimpKit` (camelCase). The version is pinned in the root `pom.xml` as `<blimpkit.version>` (currently `2.3.0`).

The five files you actually link against:

| Path | Purpose |
|---|---|
| `/webjars/blimpkit__blimpkit/dist/blimpkit.js` | Unminified BlimpKit component library (for debugging). |
| `/webjars/blimpkit__blimpkit/dist/blimpkit.min.js` | Minified BlimpKit (production link). |
| `/webjars/blimpkit__blimpkit/dist/css/blimpkit.css` | Main CSS: theme-variable mapping, `bk-*` utility classes, base component styles. |
| `/webjars/blimpkit__blimpkit/dist/css/themes/blimpkit-light.css` | Light-theme variable definitions. Sets `--background`, `--foreground`, `--negative`, `--positive`, etc. on `:root`. |
| `/webjars/blimpkit__blimpkit/dist/css/themes/blimpkit-dark.css` | Dark-theme variable definitions (same variable names, different values). |

Plus the `SAP-icons` + `72` body font `@font-face` declarations at `/services/web/platform-core/ui/styles/fonts.css`. The IDE shell pulls all of these automatically via the `platform-links` injection mechanism (§2.5); only standalone iframes have to link them by hand.

There is **no `components/ui/platform-core/.../blimpkit/`** source folder inside this repo any more - older notes still reference it, ignore them. To audit a `bk-*` component's behaviour, read `/tmp/blimpkit.css` (or grep the upstream repo on GitHub), not anything inside `components/`.

### 2.2 Gotchas - read once, save hours

These are the ones that have already burned someone.

- **`<bk-checkbox>` is invisible without `<bk-checkbox-label>`.** `bk-checkbox` compiles to a bare `<input type="checkbox" class="fd-checkbox">`. Fundamental-Styles' `.fd-checkbox` rule hides the native input (`opacity:0; position:absolute`) on the assumption that a sibling `<bk-checkbox-label>` will draw the visible square via its `.fd-checkbox__checkmark` ::before pseudo. A lone `<bk-checkbox>` is therefore a working click target with zero visible chrome - easy to ship and never catch in code review. Pair it:
  ```html
  <bk-checkbox id="x" ng-model="…"></bk-checkbox>
  <bk-checkbox-label for="x" empty="true">…</bk-checkbox-label>
  ```
  Use `empty="true"` when the surrounding markup already labels the row so the label provides just the checkmark.

- **`<bk-dialog>` is a shell-internal directive, not for view authors. Use `DialogHub` instead** (see §2.4). The two notes below apply only to maintainers working *inside* `platform-core`'s dialog shell or to the legacy BPMN/Flowable editor migration. For a new view, do not write `<bk-dialog>` at all.
  - **Isolate scope.** You can't put `ng-controller="…PopupCtrl"` on the dialog element itself - Angular throws *"Multiple directives [bkDialog, ngController] asking for new/isolated scope on: <bk-dialog>"*. Wrap with a thin `<div ng-controller="…">` and put `<bk-dialog visible="…">` inside.
  - **Toggled via the `visible` binding**, not a `.modal('show')` plugin. `<bk-dialog visible="modal.visible">` watches the expression and adds `fd-dialog--active` when true. No backdrop element is added (the dialog's own `.fd-dialog--active` overlay handles z-index + dimming). To dismiss programmatically: flip the bound flag inside an `$apply`; let the directive's digest cycle remove the `--active` class; then `$timeout` ~300ms before tearing down the scope so the close animation completes.

- **`<bk-select>` doesn't support `ng-options`.** Use `<bk-option ng-repeat>` instead - text via the `text` attribute, model value via `value`. Example:
  ```html
  <bk-option ng-repeat="opt in items" text="{{opt.name}}" value="opt.id"></bk-option>
  ```
  When the select sits in a parent with `overflow:hidden` (a dialog, a sidebar), add `dropdown-fixed="true"` so the menu floats via `position:fixed` instead of being clipped.

- **`<bk-option>`'s `text` and `value` bind differently** - `text: '@'` is **interpolation** (use `text="{{ expr }}"` or a literal), `value: '<'` is a **one-way expression** (use `value="expr"`, never `value="{{ expr }}"`). Mixing them up is the canonical bug for this directive:
  - `value="{{s}}"` makes Angular try to parse `{{s}}` as a JS expression, the directive's link silently fails, and the dropdown shows raw `{{ text }}` from the unlinked template (one ghost item per ng-repeat iteration). Fix: `value="s"`.
  - `value="user"` evaluates `$scope.user`, not the string `"user"` - every option ends up with the same `undefined` value and selection becomes a no-op. For string literals, quote inside: `value="'user'"`. For the empty default option, `value="''"`, not `value=""` (which is the undefined-expression).
  - Numeric literals (`value="2"`) and loop variables (`value="s"`) are already expressions - leave them unquoted. Numbers stay numbers, so `selectedValue === '2'` will fail; either store as numbers on the model or coerce in the controller.

- **Perspective SVG icons inherit `fill` from CSS - don't hard-code `fill` on the path.** `blimpkit.css` styles `.fd-list__navigation-item i.bk-icon--svg svg` with `fill: var(--fdVerticalNav_Icon_Color, #303030)` (and `var(--sapSelectedColor)` on the active state). Adding `fill="#000000"` locks the icon to black and breaks dark-theme adaptability. Strip the fill attribute or set `fill="currentColor"`.

- **`<bk-input>` / `<bk-textarea>` / `<bk-button>` use `replace:true`.** The attributes you write on the directive element (ng-model, ng-blur, ng-keypress, ng-disabled, custom directives like `auto-focus` / `select-text`) end up on the underlying native `<input>` / `<textarea>` / `<button>`, so existing controller code keeps working unchanged after migrating native form controls to `bk-*`. ng-model binds against the parent scope - the isolate scope `bk-input` declares only owns `compact` / `state` / `glyph`.

- **Don't put `ng-class` on a `replace:true` directive element that already has its own `ng-class`.** `bk-table-header-cell`, `bk-table-cell`, and most layout-y BlimpKit directives template as `<th ng-class="getClasses()" …>` - Angular's attribute merge **string-concatenates** duplicate `ng-class` values, producing nonsense like `ng-class="{ sorted: sort.key === 'id' } getClasses()"`. The page then throws `$parse:syntax` at compile time and the row never renders. (`class` merges cleanly - only `ng-class` is broken - so `class="no-sort"` on a `<th bk-table-header-cell>` works fine.) Fix: push the conditional class onto a child element instead of the directive root:
  ```html
  <th bk-table-header-cell ng-click="…">
    <span class="sort-caret" ng-class="{ active: sort.key === 'id' }">{{ caret() }}</span>
  </th>
  ```
  Same applies for anything else with a `replace:true` + `ng-class` template (audit `components/ui/platform-core/.../blimpkit/*.js` for the pattern before adding `ng-class` to a `bk-*` directive).

- **The `blimpKit` module's `.config()` block disables three `$compileProvider` flags.** `cssClassDirectivesEnabled(false)`, `commentDirectivesEnabled(false)`, and `debugInfoEnabled(false)` are flipped at module-load. Saves per-element scope-tracking overhead in production. The last flag breaks Selenide-style debugging that calls `angular.element(node).scope()` - Angular stops attaching scope refs to DOM nodes, so the lookup returns `undefined`. If your app or its integration tests rely on that, re-enable the flags in a `.config(['$compileProvider', …])` block of your own - module config blocks run in dependency order, so `blimpKit`'s flips happen first and your override sticks.

- **SAP-icons + the "72" body font live in platform-core's `fonts.css`.** Every BlimpKit-using page needs `<link rel="stylesheet" href="/services/web/platform-core/ui/styles/fonts.css">`. Without it `.sap-icon--*` glyphs render as tofu squares because the `@font-face { font-family: "SAP-icons"; … }` declaration is missing. The IDE shell loads this automatically via the `platform-links` injection mechanism (see §2.5); standalone iframes (editor-bpm, embedded views) have to add the link tag explicitly. Other `@font-face` rules in the same file declare the body font: `"72"` (Regular / Light / Bold), `"72-Light"`, `"72-Bold"`, `"72Mono-Regular"`, `"72Mono-Bold"`, plus `"BusinessSuiteInAppSymbols"` and `"SAP-icons-TNT"`.

- **Test-selector changes after a BlimpKit migration.** Native `<input class="form-control">` → `<input class="fd-input fd-input--compact">`. `<div class="modal in">` (Bootstrap-3 visible) → `<section class="fd-dialog fd-dialog--active">`. `body.modal-open` and `.modal-backdrop` are NOT set by `<bk-dialog>` - drop assertions on those, the active overlay handles its own dimming. When fixing Selenide tests that look at `.modal-header .close`, switch to `.fd-dialog__header .fd-button` (or scope to the dialog with `section.fd-dialog--active button.fd-button`).

### 2.3 Available BlimpKit components (cheat sheet)

~106 `bk-*` components ship in BlimpKit. The ones a new view typically needs:

| Need | BlimpKit |
|---|---|
| Modal / popup dialog | **Use `DialogHub` from a view, not the directive.** See §2.4. The `bk-dialog*` directives are shell-internal. |
| Form fields | `bk-form-group`, `bk-form-item`, `bk-form-label`, `bk-input`, `bk-textarea`, `bk-select` + `bk-option`, `bk-checkbox` + `bk-checkbox-label`, `bk-radio`, `bk-switch`, `bk-step-input`. Form inputs belong in forms - keep them out of toolbars (see §1.9). |
| Buttons / icons | `bk-button` (with `glyph="sap-icon--…"`), `bk-segmented-button` |
| Tables | `bk-table`, `bk-table-header`, `bk-table-header-cell`, `bk-table-row`, `bk-table-body`, `bk-table-cell` |
| Lists / property tables | `bk-list` (use `compact="true"`), `bk-list-item`, `bk-list-title`, `bk-list-secondary`, `bk-list-navigation-item`, `bk-form-list` |
| Cards / tiles | `bk-card`, `bk-card-content` (use `card-type="object"` for KPI tiles) |
| Status / progress | `bk-object-status` (with `status="positive"/"negative"/"critical"/"informative"`), `bk-progress-indicator` (with `state="critical"/"negative"`) |
| Tabs | `bk-icon-tab-bar`, `bk-icon-tab-bar-tab`, `bk-icon-tab-bar-panel` |
| Validation messages | `bk-form-input-message`, `bk-form-message` |
| Action bars | `bk-toolbar`, `bk-toolbar-spacer`, `bk-toolbar-separator`, `bk-bar`, `bk-bar-element` |
| Popovers / menus | `bk-popover`, `bk-popover-control`, `bk-popover-body`, `bk-menu`, `bk-menu-item` |
| Empty / error state | `bk-message-page`, `bk-message-page-title`, `bk-message-page-subtitle` |
| Scroll container | `<div bk-scrollbar>` (custom-styled scrollbar) |

Reference real-world usage:

- `components/ui/perspective-monitoring/`, `view-jvm-monitoring`, `view-jvm-threads`, `view-monitoring-metrics` - the canonical BlimpKit-native dashboard look after the §1 cleanups.
- `components/ui/view-jobs/`, `components/ui/view-security/`, `components/ui/view-repository/` - production patterns for `DialogHub` usage (`showAlert`, `showDialog`, `showFormDialog`, `showWindow`).
- `components/ui/perspective-processes/` - BPM-adjacent perspective that is BlimpKit-aligned.

(Note: `editor-monaco` is **not** a BlimpKit reference - it predates the migration and is a known cleanup target. Don't copy from it.)

### 2.4 `DialogHub` - the only way to open dialogs from a view

Dirigible's dialog system is platform-wide and message-bus-driven. A view does **not** render `<bk-dialog>` markup; it instantiates a `DialogHub` and calls one of its `show*` methods. The shell (`platform-core`) listens for the resulting platform-bus messages and renders the dialog itself with consistent chrome, focus management, theming, and z-index handling. This is what gives every view's dialog the same look without each view having to know how to draw one.

The class lives at `components/resources/platform-core/src/main/resources/META-INF/dirigible/platform-core/ui/platform/dialog-hub.js`. It is loaded automatically by the `ng-view` platform-links bundle (§2.5), so you don't link it yourself in a normal view - just `new DialogHub()`.

**Wiring it into a controller** (jobs view pattern):

```js
const jobsView = angular.module('jobs', ['blimpKit', 'platformView']);
jobsView.constant('Dialogs', new DialogHub());           // one instance, injected as 'Dialogs'
jobsView.controller('JobsController', ($scope, Dialogs) => {
    Dialogs.showAlert({
        title: 'Job state error',
        message: 'There was an error while trying to enable the job.',
        type: AlertTypes.Error,         // 'confirmation' | 'error' | 'success' | 'warning' | 'information'
        preformatted: false,
    });
});
```

**The five `show*` methods you actually need:**

| Method | When to reach for it |
|---|---|
| `showAlert({title, message, type, preformatted?, buttons?})` | A quick notification or one-shot question. Returns a promise resolving to the clicked button's id only when `buttons` is provided; default is a single "Close". `type` uses the `AlertTypes` enum. |
| `showDialog({header?, title, subheader?, message, preformatted?, buttons?, closeButton?})` | A richer dialog with header/subheader and a custom button row. Same promise contract as `showAlert`. |
| `showBusyDialog(message)` / `closeBusyDialog()` | Spinner overlay while a long action runs. Pair every `showBusyDialog` with a matching `closeBusyDialog` (try/finally). |
| `showFormDialog({header?, title, form, submitLabel?, cancelLabel?, width?, height?, …})` | Auto-rendered form (input / textarea / checkbox / radio / dropdown) with submit/cancel. `form` is an object `{ <id>: <FormItem> }`. Returns a promise resolving to `{ <id>: value }`. Use this instead of hand-writing a "user/password/confirm" dialog. |
| `showWindow({hasHeader?, header?, title?, id, params?, width?, height?, …})` / `closeWindow()` | Opens another registered view inside a windowed iframe. `id` is the view id (registered through the perspective config); `params` becomes `data-parameters` on the embedded iframe. Use this when the dialog content is itself a view (e.g. an editor in a popup). |

`FormItem` supports common properties (label / value / required / disabled / focus / errorMsg) plus per-control-type ones (`type` for input, `options` for dropdown, `rows` for textarea, `min/max/step` for numeric, `inputRules` for regex validation, `enabledOn` / `disabledOn` / `visibleOn` / `hiddenOn` for cross-field dependencies). See the JSDoc at the top of `dialog-hub.js` for the full list before writing custom checks.

**Don't:**

- Don't write `<bk-dialog>` markup in a view template. That's what broke the BPMN editor's dialogs - the editor included raw `<bk-dialog>` instead of routing through `DialogHub`, and lost the platform's dialog plumbing.
- Don't build a custom "modal" with `<div class="modal">` / Bootstrap-3 patterns. The legacy editor still has those; they are not the pattern for new code.
- Don't subscribe to platform dialog topics from a view to render the dialog yourself. The shell is the consumer. The `onAlert` / `onDialog` / `onWindow` listeners on `DialogHub` exist so the shell can render dialogs - not so views can re-implement them.

### 2.5 `<meta name="platform-links">` - auto-injects scripts + stylesheets

Looking at any non-iframe perspective HTML you'll see a single `<meta name="platform-links" category="ng-view,ng-perspective">`-style tag in the `<head>`. `HtmlPlatformLinksInjector` (in `components/engine/engine-web/.../HtmlPlatformLinksInjector.java`) reads it at request time, walks the `category` list, and replaces the meta tag with the bundle of `<link>` and `<script>` tags registered for those categories.

Categories are defined in `components/engine/engine-web/src/main/resources/platform-links.json`:

- `ng-view` - heavyweight bundle (jQuery, AngularJS, all the platform hubs, BlimpKit, Fundamental-Styles, fonts.css)
- `ng-perspective` - adds split + layout
- `ng-editor` - adds workspace + repository hubs
- … etc.

Adding new shared platform code → add it to this JSON, not to every perspective HTML.

### 2.6 Wiring BlimpKit into a non-perspective iframe (editor-style)

When building a self-contained editor or view that doesn't go through `platform-links` injection (e.g. the editor-bpm iframe), the iframe HTML has to link every script + stylesheet explicitly. Minimum set for BlimpKit to work:

```html
<link rel="stylesheet" href="/services/web/platform-core/ui/styles/fonts.css">
<link rel="stylesheet" href="/webjars/fundamental-styles/dist/fundamental-styles.css">
<link rel="stylesheet" href="/webjars/blimpkit__blimpkit/dist/css/blimpkit.css">
<!-- jQuery + AngularJS first, then: -->
<script src="/webjars/angular-aria/angular-aria.min.js"></script>
<script src="/webjars/blimpkit__blimpkit/dist/blimpkit.min.js"></script>
<!-- then your app's scripts -->
```

Plus add `'blimpKit'` to the app's module-dep array in its main `app.js`:

```js
angular.module('myApp', ['ngRoute', 'blimpKit', /* … */]);
```

**Side-effects to know about:**

- The `blimpKit` module's `.config()` block calls `$compileProvider.cssClassDirectivesEnabled(false)` / `commentDirectivesEnabled(false)` / `debugInfoEnabled(false)`. Those are app-global. **`debugInfoEnabled(false)` is a real regression for integration tests** that reach into Angular via `angular.element(node).scope()`. Re-enable the flags in a `.config(['$compileProvider', …])` block in your own `app.js` - dependency config blocks run first, so the override sticks.
- Loading `blimpkit.min.js` BEFORE your `app.js` is mandatory - `app.js`'s `angular.module(...)` references `'blimpKit'`, which has to be registered already.
- Without `fonts.css` `sap-icon--*` glyphs render as tofu squares.

### 2.7 Theming - BlimpKit variables, `--sap*` legacy, and the `dirigible-theme-*` body class

**Variable preference order** (also stated in §0 rule 4):

1. **A `bk-*` utility class** that already wires to the right variable (`bk-color--negative`, `bk-text--subtitle`, `bk-color-bg--informative`, etc.).
2. **A BlimpKit theme variable** declared in `blimpkit-light.css` / `blimpkit-dark.css`. These are the canonical theme tokens. The set:

   | Group | Variables |
   |---|---|
   | Base surface | `--background`, `--foreground`, `--contrast`, `--muted`, `--muted-foreground`, `--highlight`, `--border`, `--border-width`, `--radius`, `--radius-control` |
   | Cards / objects | `--card`, `--card-hover`, `--card-active`, `--card-border`, `--object-background`, `--object-background-alt`, `--object-background-hover`, `--object-background-active`, `--object-foreground`, `--object-header`, `--object-header-border`, `--object-header-foreground`, `--object-header-shadow` |
   | Accent | `--accent`, `--accent-foreground`, `--accent-hover`, `--accent-active` |
   | Secondary | `--secondary`, `--secondary-foreground`, `--secondary-border`, `--secondary-hover`, `--secondary-active`, `--secondary-active-border` |
   | Semantic state | `--negative`, `--negative-foreground`, `--negative-hover`, `--negative-active`; same `-foreground`/`-hover`/`-active` quartet for `--positive`, `--warning`, `--information` |
   | Inputs / buttons / tracks | `--input-background`, `--input-background-hover`, `--input-background-active`, `--input-border`, `--input-border-hover`, `--button-border`, `--button-shadow`, `--track-background`, `--track-background-hover`, `--track-handle-background`, `--track-handle-foreground` |
   | Outline / focus | `--outline`, `--outline-style`, `--outline-offset`, `--outline-width` |
   | Shell chrome | `--shellbar-background`, `--shellbar-foreground`, `--shellbar-shadow`, `--shellbar-interactive-background`, `--shellbar-interactive-foreground`, `--shellbar-interactive-hover`, `--shellbar-interactive-active`, `--shellbar-interactive-border`, `--shellbar-interactive-placeholder` |
   | Sidebar | `--sidebar`, `--sidebar-foreground`, `--sidebar-border`, `--sidebar-shadow`, `--sidebar-hover`, `--sidebar-active`, `--sidebar-active-hover`, `--sidebar-active-foreground` |
   | Tables | `--table-header`, `--table-header-foreground` |
   | Links | `--link`, `--link-hover`, `--link-visited`, `--link-subtle`, `--link-inverted` |
   | Scrollbars | `--scrollbar`, `--scrollbar-thumb`, `--scrollbar-thumb-hover` |
   | Inactive / disabled | `--inactive-background`, `--inactive-border`, `--disabled-opacity` |
   | Shadows | `--shadow-xs`, `--shadow-sm`, `--shadow-md`, `--shadow-lg` |
   | Typography | `--font-sans`, `--font-sans-bold`, `--font-sans-semibold`, `--font-mono`, `--font-mono-bold`, `--font-size` |
   | Popover | `--popover-arrow-border` |

   Definitive source: `/webjars/blimpkit__blimpkit/dist/css/themes/blimpkit-light.css` (and `-dark.css`). Both files define the same names with theme-appropriate values, so `var(--foreground)` is correct for either theme.

3. **A `--sap*` legacy variable** only when nothing above expresses the need. The shim still ships via `theme-blimpkit/css/sap-variables-{light,dark,auto}.css` for backwards compatibility (`BpmnEditorLoadsIT` asserts `--sapBackgroundColor` is non-empty inside the iframe), but most existing references are migration debt, not a pattern to copy. If you write `var(--sapXxx)`, leave a comment that explains which BlimpKit variable you tried first and why it didn't work.

**Theme-detection class.** `@media (prefers-color-scheme: dark)` is the **OS preference**, not the Dirigible theme. To target the theme the user picked, write `setTheme()` to add `body.dirigible-theme-{light,dark,auto}` and gate your CSS on that class. Reserve `prefers-color-scheme` for the `dirigible-theme-auto` fallback.

### 2.8 ui-grid override (only relevant when using ui-grid)

`ui-grid 3.0.0` hard-codes light-mode colors (`#fdfdfd` / `#f3f3f3` / `#f0f0ee` / `#c9dde1` / `#d4d4d4`). They lose contrast in dark mode. The standard fix lives in `editor-bpm/editor-app/theme/style.css` near the bottom and uses `var(--sapList_*)` (Background, AlternatingBackground, HeaderBackground, SelectionBackgroundColor, BorderColor, TextColor). If you add a new ui-grid usage, copy the override block and re-audit selector specificity - the original ui-grid CSS rules ship with `background-color:` and `border-color:` that beat unscoped overrides.

### 2.9 Don't repeat these dead-ends

- Don't write `<bk-dialog>` markup in a view. Use `DialogHub` (§2.4). The BPMN editor's broken dialogs are exactly this mistake.
- Don't put a `<bk-select>` (or any other form input besides `type="text"` / `type="search"`) in a toolbar. Toolbars hold actions and settings as buttons. (§1.9)
- Don't write `var(--sapXxx)` when `var(--xxx)` (BlimpKit) or a `bk-*` class would do. The `--sap*` shim is legacy debt, not the target. (§2.7)
- Don't reintroduce angular-strap `bs-select` / `bs-tooltip` / `bs-popover` directives - they've been ripped out and there's no migration plan back. Use native `<select>` or `<bk-select>` in forms, the `title` attribute, or `<bk-popover>`.
- Don't trust Bootstrap-3's `.modal('show')` plugin inside iframes - it's unreliable. The drop-in modal service in `editor-bpm` explicitly sets `element[0].style.display = 'block'`. For new code, route every dialog through `DialogHub`.
- Don't write per-view CSS for typography, spacing, color, or flex layout when a `bk-*` utility class exists. Audit the existing styles in `components/ui/perspective-monitoring/` for the reference look.
- Don't copy from `components/ui/editor-monaco/` for BlimpKit patterns - it predates the BlimpKit migration and is a known cleanup target.

---

## 3. Checklist before opening a UI PR

**§0 rules:**
- [ ] No em dashes (`—`) or en dashes (`–`) anywhere - prose, code, commit messages, Markdown. Plain `-` only.
- [ ] No `<bk-dialog>` markup in any view template - dialogs go through `DialogHub`.
- [ ] No hex / rgb / inline color values. No `var(--sapXxx)` when a BlimpKit class (`bk-color--*`) or variable (`var(--negative)`, etc.) exists.
- [ ] Custom CSS classes only where no BlimpKit / Fundamental class fits.
- [ ] Perspective SVG icon has no internal padding (paths extend to the `viewBox` edges) and no hard-coded `fill`. Uses `viewBox="0 0 24 24"` + `xml:space="preserve"`.

**§1 rules (recent monitoring-view cleanups):**
- [ ] No `<style>` block (or one small grid/sizing rule, max).
- [ ] No inline `style="height: …; padding: …; color: …; font-size: …"` - use BlimpKit utility classes.
- [ ] No hand-rolled "pill" / "tile" / "bar" / "section-title" CSS - use `bk-object-status`, `bk-card`, `bk-progress-indicator`, headings + `bk-text--subtitle`.
- [ ] No Unicode-caret sort indicators - use `sap-icon--sort/up/down` with `interactive="true"` on the `<th bk-table-header-cell>`.
- [ ] No `<bk-select>` (or other form input besides `type="text"` / `type="search"`) in a toolbar - use a `<bk-button>` opening a `<bk-popover>` + `<bk-menu>`.
- [ ] Every `<bk-checkbox>` is paired with a `<bk-checkbox-label>` (with `empty="true"` when redundant text).
- [ ] Every `<label for="…">` matches an `id="…"` on its BlimpKit control.
- [ ] Numeric model values are stored as numbers, not strings.
- [ ] Inline widths use `rem`, not `px`.
- [ ] Filter rows wrap label+control as a unit (`bk-hbox` per pair, `bk-flex--wrap` on the parent).
- [ ] Domain → BlimpKit status mapping is in the controller (`positive`/`negative`/`critical`/`informative`/`''`), not hard-coded colors.
- [ ] Sortable headers, table-empty cells, and scroll containers use the BlimpKit attributes (`interactive="true"`, `no-data="true"`, `<div bk-scrollbar>`).
- [ ] If the view ships in its own iframe (not a perspective), `fonts.css` is linked explicitly and the `blimpKit` debug-flag override is in `.config()`.
- [ ] If a new BlimpKit version is required, bumped centrally in root `pom.xml` (`<blimpkit.version>`).
