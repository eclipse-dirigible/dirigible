# Harmonia 1.24.2 -> 2.x migration plan

Migration of the Harmonia UI library consumed by the Dirigible platform from
`1.24.2` to `2.1.0`. This document is the working plan; keep it in sync as
phases land.

## Current state

- Version pin: `pom.xml` `<harmonia.version>` = `2.1.0`.
- The webjar is a dependency of exactly one module -
  `components/resources/application-core` (`org.webjars.npm:codbex__harmonia`),
  served from the classpath at `/webjars/codbex__harmonia/dist/...`. No dist
  files are vendored; the shell runtime is shared, not copied per project.
- Consumers: `components/template/template-application-ui-harmonia-java`
  (bulk of the `x-h-*` usage), `components/template/template-form-builder-harmonia`,
  `components/resources/application-core` (shared shell runtime), and
  `components/resources/resources-application` (live application shell).
- `codbex__harmonia:2.1.0` is published on Maven Central and resolves. The webjar
  keeps `dist/harmonia.min.js` + `dist/harmonia.css` (existing tags keep
  resolving) and ships the opt-in `dist/harmonia-lucide.*` and
  `dist/harmonia-i18next.*` bundles plus a `skills/` directory.

## Version history relevant to us

- **2.0.0** - the only release with breaking changes (see below).
- **2.0.1-2.0.3** - fixes; 2.0.3 was the first we adopted (Phase 1).
- **2.1.0** - NO breaking changes. Adds an opt-in i18next plugin, two app
  templates (Granite ERP, Onyx Chat), badge/avatar/sidebar enhancements, and a
  Lucide-plugin enhancement (see Phase 2). Safe to bump straight from 2.0.3.

## Harmonia 2.0.0 breaking changes (baseline for the whole migration)

1. Date/time pickers refactored to use standard model values (`YYYY-MM-DD` for
   the date picker) instead of custom formats.
2. Icon rendering changed from a modifier to the reactive `data-icon` attribute:
   `x-h-icon.home` -> `x-h-icon data-icon="home"`.
3. Utility class `absolute-fit` renamed to `position-fit`.
4. Built-in icons renamed: `info.svg` -> `circle-info.svg`,
   `warning.svg` -> `circle-warning.svg`.
5. Form validation now defers to `:user-invalid` instead of `:invalid`; opt back
   into on-load validation with `data-validate="immediate"` on an ancestor.

## What 2.x newly delivers (adopt, do not reinvent)

- Native charts: `x-h-chart-{line,bar,doughnut,pie,scatter}`.
- Lucide lifecycle directive `x-h-lucide` (opt-in bundle; drop-in for `data-lucide`).
- New components: `datetime-picker`, `file-upload`, `rating`, `slot-picker`,
  inline calendar; enhanced popover/progress/sidebar/calendar/badge/avatar.
- Locale-aware `x-h-date-format` + shared intl engine.
- Opt-in i18next plugin (`x-h-translate`, `$t`/`$i18n`) - the shell already has
  its own i18n, so this is not needed for the migration.
- New utility classes/tokens: `col-span-*`, `row-span-*`, `line-clamp-*`,
  `position-fit`/`position-center`, `text-2xs`, `self-*`, `h-mask`/`v-mask`,
  `tabular-nums`, extended sizing scales, standard palette.
- Generated, shipped agent skill under `skills/harmonia/` (package now ships it).

---

## Phase 1 - bump + mechanical sweep - DONE (committed 429f4de, then bumped to 2.1.0)

1. `<harmonia.version>` -> `2.1.0` (was bumped to 2.0.3 first, now 2.1.0).
2. Migrated the Harmonia built-in icons from `x-h-icon.<name>` to the reactive
   `x-h-icon data-icon="<name>"` form (34 sites). Icons that never existed in
   Harmonia were fixed: `upload` -> built-in `import`; `inbox`/`eye` -> Lucide.
   (`x-h-info-page-media.icon` still exists in 2.x - left as is.)
3. Refreshed the vendored `template-application-ui-harmonia-java/reference/harmonia/`
   skill from the 2.1.0-shipped `skills/harmonia/`. (Since removed - the skill is now
   consulted upstream at <https://github.com/codbex/harmonia/blob/main/skills/harmonia/SKILL.md>
   / <https://codbex.com/harmonia/> to avoid drift.)
4. Synced version strings in `CLAUDE.md` and `HARMONIA_RUNTIME_PLAN.md`
   (`data-size` split note re-verified - still accurate in 2.1.0).
5. Also fixed a pre-existing bug found during runtime verification: the
   standalone generated report page (`report-file/index.html.template`) loaded
   `report.js` (which formats via `window.HarmoniaFormat`) but never `format.js`,
   so `HarmoniaFormat` was undefined in the report iframe. Added the `format.js`
   include; also dropped `defer` on `format.js` in the shells.

Runtime-verified on a 2.x instance: 2.x loads, shell + generated Sales Invoice
app render, all migrated icons render as SVG, generated output carries the 2.0
`data-icon` form only.

## Phase 2 - dark-mode cleanup + Lucide

### Dark-mode

Harmonia auto-persists the color scheme to localStorage and auto-syncs it across
all same-origin iframes and other browser tabs/windows (see the Harmonia
dark-mode skill). The custom plumbing is therefore redundant.

1. Delete the custom cross-iframe `storage` listeners (generated shell template,
   plus the copies in the `report-file` and form-builder templates).
2. Keep exactly one color-scheme toggle, in the shell only (live
   `resources-application/.../application/index.html` toolbar and the generated
   `ui/shell/index.html.template` toolbar). Remove any theme toggle/logic that
   leaked into embedded perspective iframes - the shell button propagates
   automatically. The toggle's sun/moon icon is a Lucide icon that flips with
   state, so render it with the `<svg x-h-lucide>` + `x-show` pattern (see below).
3. Slim `application-core/.../shell/js/stores/theme.js` to thin wrappers over
   `Harmonia.getColorScheme()/setColorScheme()` (optionally
   `addColorSchemeListener`); drop manual DOM/class/sync code. Reconsider the
   first-run "force light" bootstrap - Harmonia defaults to `auto` and persists
   the last choice.

### Lucide (~109 `data-lucide` sites) - NOT a blind sweep

Load the opt-in `/webjars/codbex__harmonia/dist/harmonia-lucide.min.js` bundle in
every shell/standalone page, AFTER the Lucide UMD (it needs `window.lucide`).
KEEP the Lucide webjar - Harmonia does not bundle Lucide. Then remove the manual
`lucide.createIcons()` / `refreshIcons()` wiring (`x-h-lucide` renders on init
and inside dynamically loaded fragments / `x-for` / `x-if` / router views).

Migration depends on whether the icon is static or changes at runtime. Two rules
from the 2.1.0 Lucide plugin:

- **`<i x-h-lucide>` (or any non-svg tag) is REPLACED by the rendered `<svg>`.**
  Combining it with any other Alpine directive (`x-show`, `:class`, `@click`, ...)
  now THROWS a descriptive error (the only exception is `:data-lucide`, whose
  bound name is consumed at render time). So a static `<i role="img"
  data-lucide="home">` just gets ` x-h-lucide` added.
- **`<svg x-h-lucide>` is rendered IN PLACE**, so Alpine directives on it survive.
  Use this form whenever the placeholder also carries `x-show` / `:class` / etc.
- **The icon name is read ONCE.** `x-h-lucide` does NOT re-render when a bound
  `:data-lucide` value changes. Any current `:data-lucide="expr"` + `refreshIcons()`
  toggle (e.g. the report chart/table button `table-2` <-> `bar-chart-3`) must
  become either two `<svg x-h-lucide>` elements toggled with `x-show`, or an
  element re-created via `x-if`/`template` so the directive runs again. A blind
  `data-lucide` -> `x-h-lucide` sweep would FREEZE these on their first value.

So, concretely:
- Static `data-lucide` on a directive-free placeholder: add `x-h-lucide` (keep `<i>`).
- Placeholder that also has another Alpine directive, or whose name toggles: use
  `<svg x-h-lucide>` (+ `x-show` for a toggle pair), or `x-if`-recreate.
- Remove `createIcons()`/`refreshIcons()` calls and their `@click="...; refreshIcons()"` hooks.

## Phase 3 - mandatory Harmonia component adoption

Rule: if a Harmonia component exists, use it. No native-HTML or bespoke
substitutes. All picker models match the native `<input>` value they replace, so
the existing `x-model="form.X"` bindings and `format.js.toDateInput()` shapes stay
compatible (verify each on the running app).

1. Date -> `x-h-date-picker` (compound: root + `-trigger` + `-popup`; `x-model` on
   the popup). Model `YYYY-MM-DD` (== native `type="date"`).
2. Datetime -> `x-h-datetime-picker` (root + `-trigger` + `-popup`; `x-model` on
   the popup). Model ISO with `T` separator `YYYY-MM-DDTHH:mm[:ss]`
   (== native `type="datetime-local"`).
3. Time -> `x-h-time-picker` (root + `-input` + `-popup`; `x-model` on the input).
   Model `HH:MM[:SS]`, 24h (== native `type="time"`).

   Sites (native `<input x-h-input type="date|datetime-local|time">` today):
   `document-view` (form fields + inline draft rows), `manage/form-view`,
   `report-file/index` filters, form-builder `ui/index`.

4. Number: the correct pattern is `<div x-h-input-number><input type="number"></div>`
   (already used in most places). Wrap the one bare `<input type="number">` in
   `manage/form-view.html.template`.
5. Sweep for any other native/bespoke control with a Harmonia equivalent
   (e.g. file upload -> `x-h-file-upload`).

## Dropped

- Replacing `application-core/.../shell/js/services/format.js` with the 2.x intl
  engine. Reversed by #6193 (unify date/number formatting behind one
  configurable source): the custom module intentionally preserves Java
  `DecimalFormat`/`DateTimeFormatter` pattern semantics (per-field scale,
  instance-level separators, dependency-free for the BPM iframe). `x-h-date-format`
  is `Intl`-based and cannot reproduce those. At most, use `x-h-date-format` for
  pure-locale display where Java-pattern semantics are not required (marginal).

## Deferred / coordinate

- Chart.js -> native `x-h-chart-*`. The report/chart/dashboard/KPI stack was
  built very recently on Chart.js (#6137 `reports[].chart`, #6133/#6136/#6140).
  Migrating means reworking fresh code, and two gaps stand: `polarArea`/`radar`
  have no native `x-h-chart-*` equivalent, and `printChart()` canvas->PNG depends
  on a `<canvas>` the native components may not expose. The Chart.js webjar lives
  in `components/resources/resources-resources/pom.xml` (`chart.js.version` in
  root `pom.xml`); only droppable if both report surfaces fully migrate. Do this
  as a separate, coordinated PR.

## Verification

Build the platform (needs `JAVA_HOME` -> corretto-21; install the root pom first
if building modules with `-pl`, else `${harmonia.version}` resolves from the
stale installed parent), launch an instance, and drive the Harmonia app shell + a
generated app (e.g. `dirigiblelabs/sample-intent-multi-model`): shell theme
toggle (verify iframe/tab propagation), list / manage / master-detail / document
perspectives, dialogs, forms (validation timing + pickers), split panels, reports
(icons + charts + number formatting). Watch the known Selenide UI IT flakiness
and the split-panel FOUC/`data-size` behavior.
