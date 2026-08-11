/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Harmonia application shell controller. It reuses the shared Harmonia runtime
 * (/services/web/application-core/shell) for the built-in pages (Dashboard/Inbox/Documents/Reports,
 * Pinecone-routed into #app) and aggregates the `application-perspectives` extension point for the
 * domain apps, which it hosts in an iframe (their generated SPA in embedded mode).
 *
 * Only NAMED perspective groups are shown (the platform's ungrouped/utility AngularJS perspectives -
 * e.g. the old Settings - are intentionally excluded; this is the pure-Harmonia application layer).
 */
const PERSPECTIVES_URL = '/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-perspectives';
const PROJECTIONS_URL = '/services/js/platform-core/extension-services/projections.js?extensionPoints=application-projections';
// Last selected projection, persisted like the theme/language flags (shared localStorage convention).
const PROJECTION_KEY = 'codbex.harmonia.projection';

document.addEventListener('alpine:init', () => {
  window.PineconeRouter.settings({
    basePath: (App.config && App.config.basePath) || '',
    hash: true
  });

  Alpine.data('app', () => ({
    hiddenPanels: { left: false },
    isOpen: false,
    currentPath: '/dashboard',
    loading: true,
    groups: [],
    // Projections: named lenses over the deployed module set (the `application-projections`
    // extension point). With 2+ the sidebar header becomes a Harmonia product switcher and the
    // selected projection filters the sidebar groups / reports / dashboard tiles / settings.
    // With 0 or 1 no switcher is shown (a single projection still filters).
    projections: [],
    projectionId: '',
    // The currently hosted domain app (a perspective). When set, the iframe is shown instead of #app.
    hostedUrl: '',
    hostedId: '',
    // Settings: the SETTING entities from every app (the 'settings' perspective group), surfaced as a
    // single footer entry + master-detail page (list left, the selected setting hosted in an iframe).
    settingsItems: [],
    settingsMode: false,
    settingsSelected: '',
    settingsUrl: '',
    // Region & Language: the platform-wide language flag (the shared locale store), offered here
    // because the shell's Settings is where users look for it. The offered codes are the PLATFORM's
    // supported set (DIRIGIBLE_APPLICATION_LANGUAGES via the locale store) - modules never define
    // what the stack supports. Each app's generated js/config.js declares which languages it
    // PROVIDES translations for; apps missing a platform language are listed as warnings so
    // developers know where translations are still needed (data falls back to the default language).
    language: 'en',
    appLanguages: [],
    // Tenant Configuration: a built-in settings entry showing the predefined, per-tenant-overridable
    // properties (the branding properties for now). Backed by the platform endpoint
    // /services/core/configurations/tenant; requires ADMINISTRATOR/OPERATOR (a 403 is surfaced as a
    // read-only notice). Each entry is { key, value }; an empty value means "not overridden".
    tenantConfig: [],
    tenantConfigLoading: false,
    tenantConfigSaving: false,
    tenantConfigError: null,

    /** A localized label for a predefined key, falling back to a title-cased form of its name. */
    tenantConfigLabel(key) {
      const derived = key.replace(/^DIRIGIBLE_BRANDING_/, '')
                .replace(/^DIRIGIBLE_/, '')
                .toLowerCase()
                .replace(/_/g, ' ')
                .replace(/\b\w/g, (c) => c.toUpperCase());
      const suffix = {
        DIRIGIBLE_BRANDING_NAME: 'name',
        DIRIGIBLE_BRANDING_SUBTITLE: 'subtitle',
        DIRIGIBLE_BRANDING_BRAND: 'brand',
        DIRIGIBLE_BRANDING_BRAND_URL: 'brandUrl',
        DIRIGIBLE_BRANDING_FAVICON: 'favicon',
        DIRIGIBLE_BRANDING_THEME: 'theme',
        DIRIGIBLE_BRANDING_PREFIX: 'prefix',
        DIRIGIBLE_BRANDING_ANALYTICS: 'analytics',
        DIRIGIBLE_APPLICATION_LANGUAGES: 'languages',
        DIRIGIBLE_CMS_ROLES_ENABLED: 'cmsRoles',
        DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED: 'msOfficeContentTypes',
      }[key];
      return (suffix && window.T)
        ? T('application-core:shell.settings.tenantConfigLabels.' + suffix, derived)
        : derived;
    },

    /** Load the predefined properties and the current tenant's value for each. */
    async loadTenantConfig() {
      this.tenantConfigLoading = true;
      this.tenantConfigError = null;
      try {
        const res = await fetch('/services/core/configurations/tenant/predefined', {
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });
        if (res.status === 403) {
          this.tenantConfig = [];
          this.tenantConfigError = 'forbidden';
          return;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        this.tenantConfig = data.map((e) => ({ key: e.key, value: e.value == null ? '' : e.value }));
      } catch (e) {
        console.error('tenant-configuration: failed to load', e);
        this.tenantConfig = [];
        this.tenantConfigError = 'load';
      } finally {
        this.tenantConfigLoading = false;
        this.refreshIcons();
      }
    },

    /** Persist the edited values: a non-empty value is stored (PUT), an emptied one is cleared (DELETE). */
    async saveTenantConfig() {
      this.tenantConfigSaving = true;
      this.tenantConfigError = null;
      try {
        for (const entry of this.tenantConfig) {
          const value = (entry.value || '').trim();
          if (value.length) {
            const res = await fetch('/services/core/configurations/tenant', {
              method: 'PUT',
              credentials: 'same-origin',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ key: entry.key, value: value })
            });
            if (!res.ok) throw new Error('PUT ' + entry.key + ' -> HTTP ' + res.status);
          } else {
            const res = await fetch('/services/core/configurations/tenant?key=' + encodeURIComponent(entry.key), {
              method: 'DELETE',
              credentials: 'same-origin'
            });
            if (!res.ok && res.status !== 404) throw new Error('DELETE ' + entry.key + ' -> HTTP ' + res.status);
          }
        }
        await this.loadTenantConfig();
      } catch (e) {
        console.error('tenant-configuration: failed to save', e);
        this.tenantConfigError = 'save';
      } finally {
        this.tenantConfigSaving = false;
      }
    },

    // Document Numbering: the current tenant's number series (engine-numbering), read from and
    // written to /services/core/numbering (ADMINISTRATOR/OPERATOR - a 403 is surfaced read-only). Each
    // row is { series, partition, prefix, size, next } - a series declared by a published module's
    // .numbers artefact, one row per partition (e.g. per company) when the intent partitions it. The
    // tenant edits the SHAPE (prefix + total width) and the NEXT value here; only what the user
    // actually changed is written, so a counter that advanced since load is never clobbered. An
    // annual restart is exactly this page: set the prefix and the next value in January.
    numbering: [],
    numberingLoading: false,
    numberingError: null,

    /**
     * A readable label for a series row: the series, plus the partition's DISPLAY LABEL when the
     * declaration names a partition source ("Sales Invoice - ACME Ltd."), falling back to the raw
     * partition value in brackets when no label resolves.
     */
    numberingLabel(row) {
      if (!row.partition) return row.series;
      if (row.partitionLabel) return row.series + ' — ' + row.partitionLabel;
      return row.series + ' [' + row.partition + ']';
    },

    /**
     * The base ("") row of a PARTITIONED series is only the shape template new partitions inherit at
     * birth - allocation always draws from a partition row - so its counter must not be offered for
     * editing (editing it looks like seeding the next number and does nothing).
     */
    numberingIsShapeTemplate(row) {
      return !row.partition && row.partitioned;
    },

    /** Whether the row's edits differ from what was loaded - drives the per-row Save. */
    numberingDirty(row) {
      return (row.prefix || '') !== row.orig.prefix
        || parseInt(row.size, 10) !== row.orig.size
        || (!this.numberingIsShapeTemplate(row) && parseInt(row.next, 10) !== row.orig.next);
    },

    /**
     * The next number as it will render with the row's CURRENT edits - prefix + sequence zero-padded
     * to the total width (mirrors the server's rendering, incl. never truncating an overflow).
     */
    numberingExample(row) {
      const prefix = row.prefix || '';
      const next = parseInt(row.next, 10);
      const size = parseInt(row.size, 10);
      if (!isFinite(next) || next < 1 || !isFinite(size)) return '';
      const digits = Math.max(1, size - prefix.length);
      return prefix + String(next).padStart(digits, '0');
    },

    /** Load the current tenant's number series. */
    async loadNumbering() {
      this.numberingLoading = true;
      this.numberingError = null;
      try {
        const res = await fetch('/services/core/numbering', {
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });
        if (res.status === 403) {
          this.numbering = [];
          this.numberingError = 'forbidden';
          return;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        this.numbering = data.map((c) => ({
          series: c.series,
          partition: c.partition || '',
          partitionLabel: c.partitionLabel || '',
          prefix: c.prefix || '',
          size: c.size,
          next: c.next,
          partitioned: !!c.partitioned,
          // A declared partition value that has never allocated: the row does not exist yet and is
          // rendered from the base shape; saving it provisions it (seed a counter before first use).
          virtual: !!c.virtual,
          saving: false,
          saved: false,
          orig: { prefix: c.prefix || '', size: c.size, next: c.next }
        }));
      } catch (e) {
        console.error('document-numbering: failed to load', e);
        this.numbering = [];
        this.numberingError = 'load';
      } finally {
        this.numberingLoading = false;
        this.refreshIcons();
      }
    },

    /**
     * Persist ONE row's edits: a changed shape via PUT /shape, a changed next via PUT. Untouched
     * values are not written - setNext rewinds the live counter, so writing an unchanged "next"
     * would silently undo allocations made since the page loaded. Per-row on purpose: a real suite
     * has dozens of series, and saving must sit next to what was edited, not below the whole list.
     * Only the saved row's baseline is refreshed, so other rows' in-progress edits survive.
     */
    async saveNumberingRow(row) {
      this.numberingError = null;
      row.saving = true;
      row.saved = false;
      try {
        const size = parseInt(row.size, 10);
        const shapeChanged = (row.prefix || '') !== row.orig.prefix || size !== row.orig.size;
        if (shapeChanged && isFinite(size)) {
          const res = await fetch('/services/core/numbering/shape', {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ series: row.series, partition: row.partition, prefix: row.prefix || '', size: size })
          });
          if (!res.ok) throw new Error('PUT shape ' + row.series + ' -> HTTP ' + res.status);
          row.orig.prefix = row.prefix || '';
          row.orig.size = size;
        }
        const next = parseInt(row.next, 10);
        if (!this.numberingIsShapeTemplate(row) && isFinite(next) && next >= 1 && next !== row.orig.next) {
          const res = await fetch('/services/core/numbering', {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ series: row.series, partition: row.partition, next: next })
          });
          if (!res.ok) throw new Error('PUT ' + row.series + ' -> HTTP ' + res.status);
          row.orig.next = next;
        }
        row.saved = true;
        setTimeout(() => { row.saved = false; }, 2500);
      } catch (e) {
        console.error('document-numbering: failed to save', e);
        this.numberingError = 'save';
      } finally {
        row.saving = false;
        this.refreshIcons();
      }
    },

    // Language coverage of the embedded apps: which languages each generated app PROVIDES
    // translations for (its js/config.js carries `languages: [...]` from the intent; the config is
    // a JS file, so the array is read with a targeted match rather than executed). One entry per
    // project; an app without a readable declaration counts as providing only the default language.
    async loadLanguageCoverage(perspectives) {
      const bases = new Map();
      const collect = (item) => {
        const path = item && item.path;
        const match = typeof path === 'string' && path.match(/^\/services\/web\/([^\/]+)\/([^#?]*\/)?index\.html/);
        if (match && !bases.has(match[1])) bases.set(match[1], '/services/web/' + match[1] + '/' + (match[2] || ''));
      };
      (perspectives || []).forEach(g => Array.isArray(g.items) ? g.items.forEach(collect) : collect(g));
      const coverage = (await Promise.all([...bases].map(async ([app, base]) => {
        // Only generated apps carry a js/config.js. A missing or unreadable config means this
        // perspective is a platform surface (Inbox, Documents, ...) translated through the shared
        // platform catalogs - it must not be reported as an untranslated app. A readable config
        // WITHOUT a languages declaration is a generated app providing only the default language.
        try {
          const res = await fetch(base + 'js/config.js', { credentials: 'same-origin' });
          if (!res.ok) return null;
          const match = (await res.text()).match(/languages:\s*(\[[^\]]*\])/);
          let provided = ['en'];
          if (match) {
            const codes = JSON.parse(match[1].replace(/'/g, '"'));
            if (Array.isArray(codes) && codes.length) provided = codes;
          }
          return { app, provided };
        } catch (e) {
          return null;
        }
      }))).filter(Boolean);
      this.appLanguages = coverage;
    },

    // Apps that do not provide every platform language - the developers' to-do list for missing
    // translation content. Reactive on both the coverage scan and the platform set.
    languageWarnings() {
      const platform = Alpine.store('locale').languages();
      return this.appLanguages
                 .map(({ app, provided }) => ({ app, missing: platform.filter(code => !provided.includes(code)) }))
                 .filter(({ missing }) => missing.length > 0);
    },

    // The platform's language codes with display names for the Settings picker.
    languageOptions() {
      const locale = Alpine.store('locale');
      return locale.languages().map(code => ({ value: code, text: locale.displayName(code) }));
    },

    // ---- Act as (delegated entry) - the Applications-shell entry point -------------------
    // An entitled user (ADMINISTRATOR) arms an acting identity here and lands in the My shell
    // as that person - the manager-does-the-entry mode. Server-side session + entitlement.
    actAs: { entitled: false, acting: null },
    actAsDialog: false,
    actAsInput: '',
    async loadActAs() {
      try {
        const res = await fetch('/services/core/actas', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        if (res.ok) {
          const s = await res.json();
          this.actAs = { entitled: !!s.entitled, acting: s.actingAs || null };
        }
      } catch (e) {
        console.error('Failed to load the act-as state', e);
      }
    },
    async armActAs() {
      const username = (this.actAsInput || '').trim();
      if (!username) return;
      const res = await fetch('/services/core/actas', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ username }),
      });
      if (res.ok) {
        window.location.href = '/services/web/personal/';
      } else {
        console.error('Failed to arm act-as', res.status);
      }
    },

    async init() {
      this.loadActAs(); // fire-and-forget: the menu entry appears when the state arrives
      const projectionsLoaded = this.loadProjections();
      try {
        const res = await fetch(PERSPECTIVES_URL, { headers: { 'Accept': 'application/json' } });
        if (res.ok) {
          const data = await res.json();
          // Domain apps live in named groups; skip the platform's 'undefined-group' and utilities
          // (those are the legacy AngularJS perspectives, which do not belong in the Harmonia shell).
          // Every app's SETTING entities are shown under the dedicated Settings footer entry, never in
          // the sidebar - whether they were declared inside a nav group or with no group at all. A
          // perspective is a setting when its kind is SETTING (from the generator); the legacy 'settings'
          // group id is a fallback for apps generated before the kind tag existed.
          const all = Array.isArray(data.perspectives) ? data.perspectives : [];
          const settings = [];
          const appGroups = [];
          // App entities declared without a navigation group come back as standalone PRIMARY
          // perspectives (no `items`); collect them into a catch-all "Other" section so they are
          // still reachable from the shared shell instead of being silently dropped. SETTING ones go to
          // the Settings footer; the shell's own built-ins (dashboard/inbox/documents - no `kind`) are
          // rendered natively and must not be re-listed here.
          const ungrouped = [];
          all.forEach(g => {
            if (Array.isArray(g.items)) {
              // A navigation group: pull its SETTING entities into Settings, keep the rest in the sidebar.
              const isSetting = (it) => it.kind === 'SETTING' || g.id === 'settings';
              settings.push(...g.items.filter(isSetting));
              const keep = g.items.filter(it => !isSetting(it));
              if (g.id !== 'undefined-group' && keep.length) {
                appGroups.push(Object.assign({}, g, { items: keep }));
              }
            } else if (g.path && g.kind === 'SETTING') {
              // A standalone setting perspective declared with no navigation group.
              settings.push(g);
            } else if (g.path && g.kind) {
              // A standalone app perspective: declared with no navigation group (PRIMARY - the
              // expected case), or one the aggregator could not place at all. Rescuing any kind, not
              // just PRIMARY, is what keeps an unplaceable perspective visible: enumerating kinds let
              // one fall off the end of this chain and vanish with no diagnostic anywhere (#6646).
              if (g.kind !== 'PRIMARY') {
                console.warn(`Perspective '${g.id}' (kind ${g.kind}, groupId '${g.groupId || ''}') matched no navigation group; showing it under "Other".`);
              }
              ungrouped.push(g);
            }
          });
          // Settings entities are listed alphabetically by label.
          settings.sort((a, b) => (a.label || '').toLowerCase().localeCompare((b.label || '').toLowerCase()));
          // Append the catch-all "Other" group last, after the named navigation groups, sorted by the
          // perspective's declared order then label so output is stable.
          if (ungrouped.length) {
            ungrouped.sort((a, b) => (a.order || 0) - (b.order || 0)
              || (a.label || '').toLowerCase().localeCompare((b.label || '').toLowerCase()));
            appGroups.push({ id: 'other', label: 'Other', tkey: 'application-core:shell.nav.other', items: ungrouped });
          }
          this.groups = appGroups;
          this.settingsItems = settings;
          // Fire-and-forget: load the contributing apps' i18n catalogs so the sidebar / dashboard /
          // Settings entries translate. Each perspective's tkey is '<project>:<path>' - the project
          // is the catalog namespace, which the shell (having no project of its own) must add.
          const hostedProjects = [...new Set(all.flatMap(g => Array.isArray(g.items) ? g.items : [g])
            .map(it => (it.tkey || '').split(':')[0])
            .filter(ns => ns && ns !== 'application-core'))];
          if (window.AppI18nAddNamespaces) {
            AppI18nAddNamespaces(hostedProjects);
          }
          // Same aggregation for the apps' contributed custom actions ('<project>-custom-action'
          // extension points): the shell's own project contributes none, so without this the
          // generate / create-from buttons exist in each app's standalone shell but not here.
          const customActions = Alpine.store('customActions');
          if (customActions && customActions.addProjects) customActions.addProjects(hostedProjects);
          // Fire-and-forget: scan which languages each embedded app provides translations for
          // (drives the missing-translations warnings in Settings).
          this.loadLanguageCoverage(all);
        }
      } catch (e) {
        console.error('Failed to load application perspectives', e);
      }
      await projectionsLoaded;
      this.loading = false;

      // Mirror the shared locale store so the Settings picker has a plain bindable property;
      // persisting goes through the store (the fetch client sends it as Accept-Language).
      const locale = Alpine.store('locale');
      if (locale) {
        this.language = locale.value;
        this.$watch('language', (v) => locale.set(v));
      }

      // Reports are discovered asynchronously (the store walks the registry). Once its items arrive,
      // re-resolve a deep-linked /reports/<name> so a refresh on that URL lands on the right report
      // instead of the "Select a report" empty state.
      this.$watch('$store.reports.loaded', () => {
        if (this.currentPath === '/reports' || this.currentPath.indexOf('/reports/') === 0) {
          this.selectReportByName(this.currentPath.indexOf('/reports/') === 0
                  ? decodeURIComponent(this.currentPath.slice('/reports/'.length))
                  : '');
        }
      });

      // Resolve shell state from the current route. Hosted domain apps are addressable as
      // /app/<perspective-id>[/<inner-route>]; everything else is a built-in page rendered into #app.
      // The inner route is the iframe app's own hash route (e.g. /SalesInvoice/42/edit), so the top
      // URL stays in sync with what the embedded app shows and is deep-linkable.
      const applyRoute = () => {
        // Read the live URL hash, not PineconeRouter.context.path: for a template-less route (our
        // /app/:id route) Pinecone fires pinecone:end BEFORE it assigns the new context, so context
        // would be stale here - but history.pushState has already updated the hash by then.
        const h = window.location.hash || '';
        let p = (h.charAt(0) === '#' ? h.slice(1) : h) || '/';
        if (p === '/') p = '/dashboard';
        this.currentPath = p;
        // Landing on the dashboard: re-pull the KPI widget values so freshly entered records show
        // without a full browser refresh (the reports store memoizes, so force a reload).
        if (p === '/dashboard') {
          const reports = Alpine.store('reports');
          if (reports) reports.loadWidgets(true);
        }
        if (p === '/settings' || p.indexOf('/settings/') === 0) {
          // Settings master-detail: /settings (list only) or /settings/<perspective-id> (one selected).
          this.settingsMode = true;
          this.hostedId = '';
          this.hostedUrl = '';
          const rest = p.indexOf('/settings/') === 0 ? p.slice('/settings/'.length) : '';
          if (rest) {
            const item = this.findSettingItem(decodeURIComponent(rest));
            if (item) {
              this.settingsSelected = item.id;
              this.settingsUrl = item.path || '';
              if (item.id === 'tenant-configuration') this.loadTenantConfig();
              if (item.id === 'document-numbering') this.loadNumbering();
            }
          }
        } else {
          this.settingsMode = false;
          const match = p.match(/^\/app\/([^/]+)(?:\/(.*))?$/);
          if (match) {
            const id = decodeURIComponent(match[1]);
            const inner = match[2] ? '/' + match[2] : '';
            // Only (re)point the iframe when the app changes - while the same app is hosted the iframe
            // owns its inner route, so we must not reset its src (that would reload it and lose state).
            if (this.hostedId !== id) {
              const item = this.findItem(id);
              if (item) { this.hostedId = id; this.hostedUrl = this.appUrl(item, inner); }
            }
          } else {
            this.hostedId = '';
            this.hostedUrl = '';
            // Reports deep-link: /reports/<name> selects that report so a browser refresh restores it.
            // The store may still be loading (its items arrive asynchronously) - the `$store.reports.loaded`
            // watcher below re-resolves once they do.
            if (p === '/reports' || p.indexOf('/reports/') === 0) {
              this.selectReportByName(p.indexOf('/reports/') === 0 ? decodeURIComponent(p.slice('/reports/'.length)) : '');
            }
          }
        }
        this.refreshIcons();
      };
      window.addEventListener('popstate', applyRoute);
      document.addEventListener('pinecone:end', applyRoute);
      // Resolve the initial route now that the perspectives are loaded (handles deep links / reloads).
      applyRoute();

      this._bp = Harmonia.getBreakpointListener((isNarrow) => {
        this.hiddenPanels.left = isNarrow;
        if (isNarrow) {
          this.$refs.overlay.appendChild(this.$refs.sidebar);
        } else {
          this.$refs.sidebarPanel.appendChild(this.$refs.sidebar);
        }
      }, 1024);

      this.$nextTick(() => this.refreshIcons());
    },

    destroy() { if (this._bp) this._bp.remove(); },

    /** Navigate to a built-in page (Pinecone route into #app); applyRoute clears any hosted app. */
    navigate(route) {
      this.settingsMode = false;
      window.PineconeRouter.navigate(route);
      this.closeSideNav();
    },

    /** Open a discovered report deep-linkably: the report name goes in the URL (/reports/<name>) so a
     *  refresh restores it. applyRoute (and the reports-loaded watcher) resolve the store selection. */
    openReport(report) {
      this.navigate('/reports/' + encodeURIComponent(report.name));
    },

    /** Select the discovered report with this name (from the shared reports store) for the Reports page.
     *  A blank name (bare /reports) clears the selection so the empty state shows; an unknown name (the
     *  store not loaded yet) leaves it null until the loaded watcher re-resolves. */
    selectReportByName(name) {
      const store = Alpine.store('reports');
      if (!store) {
        return;
      }
      store.selected = name ? (store.items || []).find(r => r.name === name) || null : null;
    },

    /** Host a domain app (a perspective) in the iframe. Swap the iframe synchronously on click (do not
     *  wait for the router's pinecone:end - for a template-less route it can fire before the context is
     *  ready, leaving the pane stale), then update the URL. applyRoute then no-ops (hostedId already set). */
    openApp(item) {
      // Re-clicking the already-hosted app is a no-op: leave its inner route (and the URL) untouched.
      if (this.hostedId !== item.id || this.settingsMode) {
        this.settingsMode = false;
        this.hostedId = item.id;
        this.hostedUrl = this.appUrl(item, '');
        this.currentPath = '/app/' + encodeURIComponent(item.id);
        window.PineconeRouter.navigate('/app/' + encodeURIComponent(item.id));
        this.refreshIcons();
      }
      this.closeSideNav();
    },

    /** Build the iframe src for a perspective, overriding its hash with `inner` (e.g. /SalesInvoice/42/edit). */
    appUrl(item, inner) {
      const path = item.path || '';
      const hashAt = path.indexOf('#');
      const base = hashAt === -1 ? path : path.slice(0, hashAt);
      const defaultHash = hashAt === -1 ? '' : path.slice(hashAt + 1);
      // Normalize to exactly one leading slash: the inner route is stored without it (mirrorInner
      // strips it), but the embedded app's hash router matches "/Entity/:id", so "#Entity/1" misses.
      const hash = (inner || defaultHash || '').replace(/^\/+/, '');
      return hash ? base + '#/' + hash : base;
    },

    /** Wire the hosted iframe so its inner navigation is mirrored into the shell's address bar. */
    onIframeLoad(e) {
      const win = e.target && e.target.contentWindow;
      if (!win) return;
      const mirror = () => this.mirrorInner(win);
      // The embedded app routes with Pinecone (history.pushState, no hashchange) and signals every
      // navigation with a `pinecone:end` event on its own document; popstate covers in-app back/forward.
      try {
        win.document.addEventListener('pinecone:end', mirror);
        win.addEventListener('popstate', mirror);
      } catch (err) { return; } // cross-origin guard
      win.addEventListener('hashchange', mirror); // fallback for non-Pinecone embedded apps
      mirror();
    },

    /** Reflect the embedded app's current hash route into the top URL as /app/<id>/<inner-route>. */
    mirrorInner(win) {
      if (!this.hostedId) return;
      let hash;
      try { hash = win.location.hash || ''; } catch (e) { return; } // cross-origin guard
      const inner = hash.replace(/^#\/?/, '');
      const top = '/app/' + encodeURIComponent(this.hostedId) + (inner ? '/' + inner : '');
      const newHash = '#' + top;
      if (window.location.hash !== newHash) {
        // replaceState updates the address bar without re-triggering the shell router (no reload loop).
        history.replaceState(history.state, '', newHash);
        this.currentPath = top;
      }
    },

    /** Open the Settings master-detail (the aggregated SETTING entities from every app). */
    openSettings() {
      this.settingsMode = true;
      this.hostedId = '';
      this.hostedUrl = '';
      this.currentPath = '/settings';
      window.PineconeRouter.navigate('/settings');
      this.refreshIcons();
      this.closeSideNav();
    },

    /** Select a setting entity: host its app at that entity's route in the settings detail iframe.
     *  Update the URL with replaceState (no Pinecone re-render) so the master-detail split keeps the
     *  layout it computed when first shown; the detail iframe just swaps its src. */
    selectSetting(item) {
      if (this.settingsSelected !== item.id) {
        this.settingsSelected = item.id;
        this.settingsUrl = item.path || '';
        this.currentPath = '/settings/' + encodeURIComponent(item.id);
        const url = '#/settings/' + encodeURIComponent(item.id);
        if (window.location.hash !== url && window.history && window.history.replaceState) {
          window.history.replaceState(window.history.state, '', url);
        }
        if (item.id === 'tenant-configuration') this.loadTenantConfig();
        if (item.id === 'document-numbering') this.loadNumbering();
        this.refreshIcons();
      }
    },

    isSettingActive(item) { return this.settingsMode && this.settingsSelected === item.id; },

    /** Find a setting entity (perspective) by id. 'region-language' is the built-in shell entry
     *  (the platform language preference) - it has no app path; its detail renders locally. */
    findSettingItem(id) {
      if (id === 'region-language') return { id: 'region-language' };
      if (id === 'tenant-configuration') return { id: 'tenant-configuration' };
      if (id === 'document-numbering') return { id: 'document-numbering' };
      return (this.settingsItems || []).find(i => i.id === id) || null;
    },

    /** Open the task behind a (task-derived) notification, and mark it read. */
    openNotification(n) {
      if (n && n.task) {
        this.$store.processTasks.openTask(n.task);
        n.unread = false;
      }
    },

    /** Find a loaded perspective by id across all groups. */
    findItem(id) {
      for (const g of this.groups) {
        const item = (g.items || []).find(i => i.id === id);
        if (item) return item;
      }
      return null;
    },

    /** Load the application-projections extension point and restore the persisted selection.
     *  Role-gated projections are filtered server-side, so what arrives here is what may be offered. */
    async loadProjections() {
      try {
        const res = await fetch(PROJECTIONS_URL, { headers: { 'Accept': 'application/json' } });
        if (res.ok) {
          const data = await res.json();
          this.projections = Array.isArray(data.projections) ? data.projections : [];
        }
      } catch (e) {
        console.error('Failed to load application projections', e);
      }
      let saved = '';
      try { saved = localStorage.getItem(PROJECTION_KEY) || ''; } catch (e) { /* no storage */ }
      this.projectionId = this.projections.some(p => p.id === saved)
        ? saved
        : (this.projections.length ? this.projections[0].id : '');
    },

    /** The currently selected projection entry (for the product-switch header), or null. */
    selectedProjection() { return this.projections.find(p => p.id === this.projectionId) || null; },

    /** The projection that FILTERS the shell: null when none is selected or the selected one is
     *  the declared "everything" entry (all: true), in which case nothing is filtered. */
    activeProjection() {
      const p = this.selectedProjection();
      return p && !p.all ? p : null;
    },

    /** Switch the product-switch selection; persist it and leave a now-hidden hosted app. */
    selectProjection(p) {
      if (this.projectionId === p.id) return;
      this.projectionId = p.id;
      try { localStorage.setItem(PROJECTION_KEY, p.id); } catch (e) { /* no storage */ }
      if (this.hostedId && !this.visibleGroups().some(g => (g.items || []).some(i => i.id === this.hostedId))) {
        this.navigate('/dashboard');
      }
      this.refreshIcons();
    },

    projectionLabel() {
      const p = this.selectedProjection();
      return p ? (p.tkey ? T(p.tkey, p.label) : p.label) : '';
    },

    /** Second line under the product name; the instance brand is the default. */
    projectionSubtitle() {
      const p = this.selectedProjection();
      return (p && p.description) || Alpine.store('branding').name || '';
    },

    /** The sidebar groups the active projection keeps: listed groups wholesale, plus any group
     *  reduced to its cherry-picked `items` (Employee-Portal-style selections keep their familiar
     *  group heading); empty groups vanish. No active projection = everything. */
    visibleGroups() {
      const proj = this.activeProjection();
      if (!proj) return this.groups;
      const groupIds = proj.groups || [];
      const itemIds = proj.items || [];
      const visible = [];
      for (const g of this.groups) {
        if (groupIds.includes(g.id)) {
          visible.push(g);
          continue;
        }
        const picked = (g.items || []).filter(i => itemIds.includes(i.id));
        if (picked.length) visible.push(Object.assign({}, g, { items: picked }));
      }
      return visible;
    },

    /** The projects contributing the visible perspectives — the projection's reach, used to scope
     *  reports, dashboard tiles and settings to the apps actually shown. */
    projectionProjects() {
      const projects = new Set();
      this.visibleGroups().forEach(g => (g.items || []).forEach(item => {
        const match = typeof item.path === 'string' && item.path.match(/^\/services\/web\/([^/]+)\//);
        if (match) projects.add(match[1]);
      }));
      return projects;
    },

    /** Whether a URL (report page, setting entity) belongs to a project inside the active
     *  projection. Unattributable URLs stay visible — filtering must not hide what it can't place. */
    inProjection(url) {
      if (!this.activeProjection()) return true;
      const match = typeof url === 'string' && url.match(/^\/services\/web\/([^/]+)\//);
      return match ? this.projectionProjects().has(match[1]) : true;
    },

    visibleReports() { return Alpine.store('reports').items.filter(r => this.inProjection(r.url)); },
    dashKpiReports() { return Alpine.store('reports').kpiReports().filter(r => this.inProjection(r.url)); },
    dashPreviewReports() { return Alpine.store('reports').previewReports().filter(r => this.inProjection(r.url)); },

    /** Settings entities scoped to the projection: explicitly cherry-picked ones always show. */
    visibleSettingsItems() {
      const proj = this.activeProjection();
      if (!proj) return this.settingsItems;
      const itemIds = proj.items || [];
      return this.settingsItems.filter(it => itemIds.includes(it.id) || this.inProjection(it.path));
    },

    isBuiltinActive(route) { return !this.hostedUrl && this.currentPath === route; },
    isAppActive(item) { return this.hostedId === item.id; },
    // A report entry is active only while a report route is showing it — keying off the current route
    // (not just $store.reports.selected) so navigating to an entity/inbox/documents clears its highlight.
    isReportActive(report) {
      const selected = Alpine.store('reports').selected;
      return !this.hostedUrl && this.currentPath.indexOf('/reports') === 0 && selected && selected.url === report.url;
    },
    isSvgIcon(icon) { return !!icon && /\.svg(\?|#|$)/i.test(icon); },
    isImageIcon(icon) { return !!icon && !this.isSvgIcon(icon) && (icon.indexOf('/') !== -1 || icon.indexOf('.') !== -1 || icon.indexOf('http') === 0); },

    openSideNav() { this.isOpen = true; },
    closeSideNav() { if (window.matchMedia('(max-width: 1024px)').matches) this.isOpen = false; },
    refreshIcons() {}, // no-op: Lucide icons render via the x-h-lucide directive (harmonia-lucide bundle)
  }));
}, { once: true });
