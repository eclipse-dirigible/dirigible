# Harmonia 1.24.2 -> 2.x migration plan

Migration of the Harmonia UI library consumed by the Dirigible platform from
`1.24.2` to `2.0.3`. This document is the working plan; keep it in sync as
phases land.

## Current state

- Version pin: `pom.xml` `<harmonia.version>` (was `1.24.2`).
- The webjar is a dependency of exactly one module -
  `components/resources/application-core` (`org.webjars.npm:codbex__harmonia`),
  served from the classpath at `/webjars/codbex__harmonia/dist/...`. No dist
  files are vendored; the shell runtime is shared, not copied per project.
- Consumers: `components/template/template-application-ui-harmonia-java`
  (bulk of the `x-h-*` usage), `components/template/template-form-builder-harmonia`,
  `components/resources/application-core` (shared shell runtime), and
  `components/resources/resources-application` (live application shell).
- `codbex__harmonia:2.0.3` is published on Maven Central (2.0.0-2.0.2 skipped),
  so the bump is unblocked. The 2.0.3 webjar keeps `dist/harmonia.min.js` +
  `dist/harmonia.css` (existing tags keep resolving) and adds the opt-in
  `dist/harmonia-lucide.*` bundle plus a shipped `skills/` directory.

## Harmonia 2.0.0 breaking changes (from the Harmonia CHANGELOG)

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
  inline calendar; enhanced popover/progress/sidebar/calendar.
- Locale-aware `x-h-date-format` + shared intl engine.
- New utility classes/tokens: `col-span-*`, `row-span-*`, `line-clamp-*`,
  `position-fit`/`position-center`, extended sizing scales, standard palette.
- Generated, shipped agent skill under `skills/harmonia/` (package now ships it).

---

## Phase 1 - bump + mechanical sweep (low risk)

1. Bump `<harmonia.version>` to `2.0.3` in root `pom.xml`.
2. Rewrite the Harmonia built-in icon usages from the modifier form
   `x-h-icon.<name>` to the reactive `x-h-icon data-icon="<name>"` form
   (31 functional sites across 10 files). Re-verify each icon name against the
   2.x icon set (`circle-info/success/error/unknown`, `search`, `inbox`, `home`,
   `upload`, `eye`, `close`) and re-check the nested `x-h-info-page-media.icon`
   modifier.
3. Refresh the vendored `template-application-ui-harmonia-java/reference/harmonia/SKILL.md`
   from the 2.x-shipped `skills/harmonia/` (the current copy is a stale 1.x
   hand-copy - "48 components", unpkg `@1.11.0`).
4. Sync version strings in `CLAUDE.md` and `HARMONIA_RUNTIME_PLAN.md`;
   re-verify the split-panel `data-size` note against 2.x.
5. Verify the runtime contracts still hold under 2.x:
   - postMessage/localStorage keys: `codbex.harmonia.colorMode`,
     `codbex.harmonia.language`, `codbex.harmonia.format.*`,
     `harmonia.form.close`, `harmonia:action-done`.
   - validation styling: templates drive `data-invalid`/`aria-invalid` from an
     Alpine `errors` object + `application-core/.../services/formValidation.js`
     (not `:invalid` CSS), so 2.x's `:user-invalid` change should be low impact -
     confirm Harmonia's own field styling still cooperates.

## Phase 2 - dark-mode cleanup + Lucide

Harmonia auto-persists the color scheme to localStorage and auto-syncs it across
all same-origin iframes and other browser tabs/windows (see the Harmonia
dark-mode skill). The custom plumbing is therefore redundant.

1. Delete the custom cross-iframe `storage` listeners (generated shell template
   `~:489-498`, and the copies in the `report-file` and form-builder templates).
2. Keep exactly one color-scheme toggle, in the shell only (live
   `resources-application/.../application/index.html` toolbar and the generated
   `ui/shell/index.html.template` toolbar). Remove any theme toggle/logic that
   leaked into embedded perspective iframes - the shell button propagates
   automatically.
3. Slim `application-core/.../shell/js/stores/theme.js` to thin wrappers over
   `Harmonia.getColorScheme()/setColorScheme()` (optionally
   `addColorSchemeListener`); drop manual DOM/class/sync code. Reconsider the
   first-run "force light" bootstrap - Harmonia defaults to `auto` and persists
   the last choice.
4. Lucide: migrate all `data-lucide` placeholders (109 sites) to add the
   `x-h-lucide` directive. It is a drop-in - `data-lucide` takes priority over
   the expression, and it stays in sync through `x-h-include`, the router, and
   `x-for`/`x-if`, so remove any manual `lucide.createIcons()` scans/wiring.
   Load the opt-in `/webjars/codbex__harmonia/dist/harmonia-lucide.min.js`
   bundle in the shells, after the Lucide UMD (it needs `window.lucide`).
   Keep the Lucide webjar - Harmonia does not bundle Lucide.

## Phase 3 - mandatory Harmonia component adoption

Rule: if a Harmonia component exists, use it. No native-HTML or bespoke
substitutes.

1. Date/time pickers: replace `<input x-h-input type="date|datetime-local|time">`
   with `x-h-date-picker` / `x-h-datetime-picker` / `x-h-time-picker` at:
   `document-view` (`:126/128/130` form, `:379/382` inline draft),
   `manage/form-view` (`:150/153/156`), `report-file/index` filters (`:56/58`),
   form-builder `ui/index` (`:47/52`). The date picker model is `YYYY-MM-DD`
   (matches the native value `format.js.toDateInput()` produces, so date fields
   are model-compatible); verify `datetime`/`time` round-trip against
   `toDateInput`/`toInstant` before swapping.
2. Wrap the one bare number input (`manage/form-view.html.template:131`) in
   `<div x-h-input-number>` to match the pattern already used elsewhere.
3. Sweep for any other native/bespoke control with a Harmonia equivalent.

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

Build the platform, launch an instance, and drive the Harmonia app shell + a
generated app: shell theme toggle (verify iframe/tab propagation), master / list
/ manage / detail perspectives, dialogs, forms (validation timing), split panels,
report + report-file (charts), document upload. Watch the known Selenide UI IT
flakiness and the split-panel FOUC/`data-size` behavior.
