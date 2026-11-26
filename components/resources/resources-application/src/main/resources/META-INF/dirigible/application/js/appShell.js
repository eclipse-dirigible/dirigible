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
const PERSPECTIVES_URL = '/services/platform/extensions/perspectives?extensionPoints=application-perspectives';
const PROJECTIONS_URL = '/services/platform/extensions/projections?extensionPoints=application-projections';
// Last selected projection, persisted like the theme/language flags (shared localStorage convention).
const PROJECTION_KEY = 'codbex.harmonia.projection';

document.addEventListener('alpine:init', () => {
  window.PineconeRouter.settings({
    basePath: (App.config && App.config.basePath) || '',
    hash: true
  });

  Alpine.data('app', () => ({
    // Narrow viewport (below the 1024px breakpoint). The sidebar then lives in the x-h-sheet drawer
    // and the content frame drops its gutter and card chrome to use the full width.
    isSmallScreen: false,

    // The narrow-screen sidebar drawer, two-way bound to the x-h-sheet-overlay.
    showSidebarSheet: false,

    // A route template is being fetched - drives the toolbar's indefinite progress bar.
    routeLoading: false,

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
    // The inner route the hosted app shows (without its leading slash), as last mirrored from it or
    // sent to it - what an address naming the same app is compared with (#8638).
    hostedInner: '',
    // Settings: the SETTING entities from every app (the 'settings' perspective group), surfaced as a
    // single footer entry + master-detail page (list left, the selected setting hosted in an iframe).
    settingsItems: [],
    settingsMode: false,
    settingsSelected: '',
    // The iframe address of the selected setting entity (empty for a built-in section, which renders locally),
    // and the inner route of that app (without its leading slash) as last mirrored from it or sent to it.
    settingsUrl: '',
    settingsInner: '',
    // Region & Language: the platform-wide language flag (the shared locale store), offered here
    // because the shell's Settings is where users look for it. The offered codes are the PLATFORM's
    // supported set (DIRIGIBLE_APPLICATION_LANGUAGES via the locale store) - modules never define
    // what the stack supports. What the embedded apps leave untranslated is listed below it as
    // warnings (the shared languageCoverage store), so developers know where translations are still
    // needed (data falls back to the default language).
    language: 'en',
    // Tenant Configuration: a built-in settings entry showing the predefined, per-tenant-overridable
    // properties (the branding properties for now). Backed by the platform endpoint
    // /services/core/configurations/tenant; requires the tenant's Owner or ADMINISTRATOR/OPERATOR (a
    // 403 is surfaced as a read-only notice). Each entry is { key, value }; an empty value means "not overridden".
    tenantConfig: [],
    // The same entries, grouped as /services/core/configurations/tenant/descriptors answers them:
    // [{ group, label, entries: [{ key, value, defaultValue, kind }] }]; defaultValue is the platform
    // value the key falls back to.
    tenantConfigGroups: [],
    tenantConfigLoading: false,
    tenantConfigSaving: false,
    tenantConfigError: null,

    /** A localized heading for a configuration group, falling back to the server's label. */
    tenantConfigGroupLabel(group) {
      return window.T ? T('application-core:shell.settings.tenantConfigGroups.' + group.group, group.label) : group.label;
    },

    /** The input a key gets: 'boolean' (default / on / off) when its name or platform value says so, else 'text'. */
    tenantConfigKind(entry) {
      if (entry.type) return entry.type.toLowerCase() === 'boolean' ? 'boolean' : 'text';
      const platform = (entry.defaultValue || '').toLowerCase();
      return (/_ENABLED$/.test(entry.key) || platform === 'true' || platform === 'false') ? 'boolean' : 'text';
    },

    /** The "not overridden" option of a boolean key, naming the platform value it falls back to. */
    tenantConfigDefaultLabel(entry) {
      const label = window.T ? T('application-core:shell.settings.tenantConfigDefault', 'Platform default') : 'Platform default';
      return entry.defaultValue ? label + ' (' + entry.defaultValue + ')' : label;
    },

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
        const res = await fetch('/services/core/configurations/tenant/descriptors', {
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json' }
        });
        if (res.status === 403) {
          this.tenantConfig = [];
          this.tenantConfigGroups = [];
          this.tenantConfigError = 'forbidden';
          return;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        this.tenantConfigGroups = data.map((g) => ({
          group: g.group,
          label: g.label,
          entries: g.entries.map((e) => ({
            key: e.key,
            value: e.value == null ? '' : e.value,
            defaultValue: e.defaultValue,
            kind: this.tenantConfigKind(e)
          }))
        }));
        this.tenantConfig = this.tenantConfigGroups.flatMap((g) => g.entries);
      } catch (e) {
        console.error('tenant-configuration: failed to load', e);
        this.tenantConfig = [];
        this.tenantConfigGroups = [];
        this.tenantConfigError = 'load';
      } finally {
        this.tenantConfigLoading = false;
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
    // written to /services/core/numbering (the tenant's Owner or ADMINISTRATOR/OPERATOR - a 403 is
    // surfaced read-only). Each row is { series, partition, prefix, size, next } - a series declared by a published module's
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
      }
    },

    // The platform's language codes with display names for the Settings picker.
    languageOptions() {
      const locale = Alpine.store('locale');
      return locale.languages().map(code => ({ value: code, text: locale.displayName(code) }));
    },

    // ---- Act as (delegated entry) - the Applications-shell entry point -------------------
    // An entitled user (ADMINISTRATOR) arms an acting identity here and lands in the My shell
    // as that person - the manager-does-the-entry mode. Server-side session + entitlement.
    // The armed state is honoured by every surface, back-office included, so this shell shows it
    // too: arming here and walking back into the applications used to leave no trace on screen at
    // all, and a forgotten arming then hid the real user's own inbox tasks (#6694).
    actAs: { entitled: false, acting: null, expiresAt: null },
    actAsDialog: false,
    actAsInput: '',
    async loadActAs() {
      try {
        const res = await fetch('/services/core/actas', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
        if (res.ok) {
          const s = await res.json();
          this.actAs = { entitled: !!s.entitled, acting: s.actingAs || null, expiresAt: s.expiresAt || null };
        }
      } catch (e) {
        console.error('Failed to load the act-as state', e);
      }
    },
    // The arming expires on its own; show when, so the banner is a deadline and not just a state.
    actAsUntil() {
      if (!this.actAs.expiresAt) return '';
      try { return new Date(this.actAs.expiresAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
      catch (_) { return ''; }
    },
    async disarmActAs() {
      const res = await fetch('/services/core/actas', { method: 'DELETE', credentials: 'same-origin' });
      if (res.ok) window.location.reload();
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
      // This component renders the toast overlay, so it lends its $notifications magic to the
      // shared store (a store has no component scope of its own to reach the magic from).
      Alpine.store('notifications').attachToaster(this.$notifications);
      this.loadActAs(); // fire-and-forget: the menu entry appears when the state arrives
      // The arming expires server-side; re-read it when the tab comes back so the banner is not
      // still claiming an identity the platform has already dropped.
      window.addEventListener('focus', () => this.loadActAs());
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
          // A navigation group translates its label the way a custom action does: it declares
          // `translation: { key: '<project>:<path>' }` and the sidebar binds that key (#7610). Only a
          // fully-qualified key is adopted - its project is the catalog namespace loaded below.
          all.forEach(g => {
            const key = g.translation && g.translation.key;
            if (!g.tkey && typeof key === 'string' && key.indexOf(':') > 0) g.tkey = key;
          });
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
              // Each setting remembers the group it came from, so a projection that lists the group
              // keeps its settings as well as its sidebar items (#8138).
              settings.push(...g.items.filter(isSetting).map(it => Object.assign({}, it, { groupId: g.id })));
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
          const hostedProjects = [...new Set(all.flatMap(g => Array.isArray(g.items) ? [g, ...g.items] : [g])
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
          // What each embedded app leaves untranslated drives only the missing-translations
          // warnings in Settings, so the scan waits for Settings to open (#8346).
          Alpine.store('languageCoverage').remember(all);
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
          // Settings master-detail: /settings (list only) or /settings/<perspective-id>[/<inner-route>] (one
          // selected; the inner route is the embedded app's own, like /app/<id>/<inner-route>).
          this.settingsMode = true;
          Alpine.store('languageCoverage').scan();
          this.hostedId = '';
          this.hostedUrl = '';
          const settings = p.match(/^\/settings(?:\/([^/]+)(?:\/(.*))?)?$/);
          const id = settings && settings[1] ? decodeURIComponent(settings[1]) : '';
          const inner = (settings && settings[2]) || '';
          const item = id ? this.findSettingItem(id) : null;
          if (!item) {
            // The list alone (a bare /settings, or Back to it): nothing is selected, whatever was before.
            this.clearSettingSelection();
          } else if (this.settingsSelected !== id) {
            // Another setting (a typed address, or Back): its iframe is pointed in place, never reloaded by src.
            this.showSetting(item, inner);
          } else if (item.path && inner !== this.settingsInner) {
            // The same setting at another page of its app: routed in place through the app's own router.
            this.routeEmbedded(true, inner);
          }
        } else {
          this.settingsMode = false;
          this.clearSettingSelection();
          const match = p.match(/^\/app\/([^/]+)(?:\/(.*))?$/);
          if (match) {
            const id = decodeURIComponent(match[1]);
            const inner = match[2] || '';
            // Only (re)point the iframe when the app changes - while the same app is hosted, resetting
            // its document would reload it and lose its state. An address naming another page of that app
            // (typed, pasted, or the browser's Back) routes it in place instead (#8638). A frame that is
            // already loaded is pointed with location.replace, never by src: a src change adds an entry to
            // the tab's one joint session history, so a single click would take two Back presses to undo.
            if (this.hostedId !== id) {
              const item = this.findItem(id);
              if (item) {
                this.hostedId = id;
                this.hostedInner = inner;
                this.hostedUrl = this.appUrl(item, inner);
                this.pointFrame(this.$refs.hostedFrame, this.hostedUrl);
              }
            } else if (inner !== this.hostedInner) {
              this.routeEmbedded(false, inner);
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
      };
      window.addEventListener('popstate', applyRoute);
      document.addEventListener('pinecone:end', applyRoute);
      document.addEventListener('pinecone:start', () => { this.routeLoading = true; });
      document.addEventListener('pinecone:fetch-error', () => { this.routeLoading = false; });
      document.addEventListener('pinecone:end', () => { this.routeLoading = false; });
      // Resolve the initial route now that the perspectives are loaded (handles deep links / reloads).
      applyRoute();

      this._bp = Harmonia.getBreakpointListener((isNarrow) => {
        this.isSmallScreen = isNarrow;
        // The sidebar is ONE element, moved between its wide-screen slot and the drawer.
        const home = isNarrow ? this.$refs.sidebarSheet : this.$refs.sidebarSlot;
        home.appendChild(this.$refs.sidebar);
      }, 1024);
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

    /** Host a domain app (a perspective) in the iframe, once the router has taken the URL. Pinecone's
     *  navigate is asynchronous and fires pinecone:end while the old address is still current; a frame
     *  pointed before that would be pointed back by applyRoute at the app the old address names. Whichever
     *  of applyRoute and this method sees the new address first points the frame; the other finds it done.
     *  The click is one history entry - the shell's - so the frame is pointed with replace (pointFrame). */
    openApp(item) {
      // Re-clicking the already-hosted app is a no-op: leave its inner route (and the URL) untouched.
      if (this.hostedId !== item.id || this.settingsMode) {
        const route = '/app/' + encodeURIComponent(item.id);
        Promise.resolve(window.PineconeRouter.navigate(route)).then(() => {
          this.settingsMode = false;
          this.currentPath = route;
          if (this.hostedId !== item.id) {
            this.hostedId = item.id;
            this.hostedInner = '';
            this.hostedUrl = this.appUrl(item, '');
            this.pointFrame(this.$refs.hostedFrame, this.hostedUrl);
          }
        });
      } else {
        // The app is already on screen: its page re-reads instead (#8130).
        this.reenterHostedApp();
      }
      this.closeSideNav();
    },

    /** Signal the page of the hosted app that the user came back to it, so it re-reads (#8130). */
    reenterHostedApp() {
      if (this.hostedId && this.$refs.hostedFrame) App.pageReentered(this.$refs.hostedFrame.contentWindow);
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

    /** Point an iframe at `url` without adding a history entry. Setting `src` on a loaded frame pushes an
     *  entry onto the tab's one joint session history, which `location.replace` does not. A frame that is
     *  not on screen yet needs nothing: its `x-init` takes the current address as its first `src`. */
    pointFrame(frame, url) {
      if (!frame || !url) return;
      try {
        frame.contentWindow.location.replace(url);
      } catch (e) { frame.src = url; } // no usable window (cross-origin or detached)
    },

    /** The selected setting's iframe, while the Settings page is rendered. Looked up in the document, not
     *  through $refs: the frame lives in the routed _settings.html fragment, and a $refs read from the
     *  fragment's own handlers (the list's click) does not reach it. */
    settingsFrame() {
      return document.querySelector('iframe[data-settings-frame]');
    },

    /** Wire an embedded iframe - the hosted app's, or with `settings` the selected setting's - so its inner
     *  navigation is mirrored into the shell's address bar. */
    onIframeLoad(e, settings) {
      const win = e.target && e.target.contentWindow;
      if (!win) return;
      const mirror = () => this.mirrorInner(win, !!settings);
      // The embedded app routes with Pinecone (history.pushState, no hashchange) and signals every
      // navigation with a `pinecone:end` event on its own document; popstate covers in-app back/forward.
      try {
        win.document.addEventListener('pinecone:end', mirror);
        win.addEventListener('popstate', mirror);
      } catch (err) { return; } // cross-origin guard
      win.addEventListener('hashchange', mirror); // fallback for non-Pinecone embedded apps
      mirror();
    },

    /** Reflect the embedded app's current hash route into the top URL as /app/<id>/<inner-route> (or
     *  /settings/<id>/<inner-route>). The app's own navigation made its own history entry, so the shell
     *  only replaces its address - one user action, one entry. */
    mirrorInner(win, settings) {
      const id = settings ? this.settingsSelected : this.hostedId;
      if (!id || (settings && (!this.settingsMode || !this.settingsUrl))) return; // a built-in section shows no app: its hidden frame says nothing
      let hash;
      try {
        if (win.location.href === 'about:blank') return; // the frame's initial document, before its address loads
        hash = win.location.hash || '';
      } catch (e) { return; } // cross-origin guard
      const inner = hash.replace(/^#\/?/, '');
      if (settings) this.settingsInner = inner; else this.hostedInner = inner;
      const top = (settings ? '/settings/' : '/app/') + encodeURIComponent(id) + (inner ? '/' + inner : '');
      const newHash = '#' + top;
      if (window.location.hash !== newHash) {
        // replaceState updates the address bar without re-triggering the shell router (no reload loop).
        history.replaceState(history.state, '', newHash);
        this.currentPath = top;
      }
    },

    /** Reads the Users section's rows - the store may be absent on a page that predates it. */
    loadTenantUsers() {
      const store = window.Alpine && Alpine.store('tenantUsers');
      if (store) store.load();
    },

    /** Reads the Data export section's rows - the store may be absent on a page that predates it. */
    loadTenantExport() {
      const store = window.Alpine && Alpine.store('tenantExport');
      if (store) store.load();
    },

    /** Reads the Print Templates section's catalogue - the store may be absent on a page that predates it. */
    loadPrintTemplates() {
      const store = window.Alpine && Alpine.store('printTemplates');
      if (store) store.load();
    },

    /** Open the Settings master-detail (the aggregated SETTING entities from every app). */
    openSettings() {
      this.settingsMode = true;
      this.hostedId = '';
      this.hostedUrl = '';
      this.currentPath = '/settings';
      window.PineconeRouter.navigate('/settings');
      this.closeSideNav();
    },

    /** Select a setting entity: host its app at that entity's route in the settings detail iframe. The pick
     *  is one real shell entry (#/settings/<id>) and the iframe is pointed with replace (pointFrame). The
     *  route /settings/:id?/:inner* is one pattern for the list and every selection, so Pinecone does not
     *  swap the template on this navigation - the split keeps the layout it computed when first shown
     *  (that was why this used replaceState, which left no entry for Back to return to).
     *  The selection follows the router's navigation, never precedes it: Pinecone's navigate is asynchronous
     *  and fires pinecone:end while the old address is still current, and applyRoute, finding a selection
     *  that address does not name, would point the frame back at the setting it does name. */
    selectSetting(item) {
      if (this.settingsSelected !== item.id) {
        const route = '/settings/' + encodeURIComponent(item.id);
        Promise.resolve(window.PineconeRouter.navigate(route)).then(() => {
          this.currentPath = route;
          if (this.settingsSelected !== item.id) this.showSetting(item, '');
        });
      }
    },

    /** Make `item` the selected setting at the app's `inner` route and read the section it shows. */
    showSetting(item, inner) {
      this.settingsSelected = item.id;
      this.settingsInner = inner;
      this.settingsUrl = item.path ? this.appUrl(item, inner) : '';
      this.pointFrame(this.settingsFrame(), this.settingsUrl);
      if (item.id === 'tenant-configuration') this.loadTenantConfig();
      if (item.id === 'print-templates') this.loadPrintTemplates();
      if (item.id === 'document-numbering') this.loadNumbering();
      if (item.id === 'tenant-users') this.loadTenantUsers();
      if (item.id === 'tenant-export') this.loadTenantExport();
    },

    /** Nothing selected: the list alone (the frame, if any, keeps its last document out of sight until
     *  the next pick points it again). */
    clearSettingSelection() {
      this.settingsSelected = '';
      this.settingsUrl = '';
      this.settingsInner = '';
    },

    /** Compact screens: back from the detail to the list. */
    settingsBack() {
      this.clearSettingSelection();
      this.currentPath = '/settings';
      window.PineconeRouter.navigate('/settings');
    },

    isSettingActive(item) { return this.settingsMode && this.settingsSelected === item.id; },

    /** Find a setting entity (perspective) by id. 'region-language' is the built-in shell entry
     *  (the platform language preference) - it has no app path; its detail renders locally. */
    findSettingItem(id) {
      if (id === 'region-language') return { id: 'region-language' };
      if (id === 'tenant-configuration') return { id: 'tenant-configuration' };
      if (id === 'print-templates') return { id: 'print-templates' };
      if (id === 'document-numbering') return { id: 'document-numbering' };
      if (id === 'tenant-users') return { id: 'tenant-users' };
      if (id === 'tenant-export') return { id: 'tenant-export' };
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
          // A projection translates its name the way a navigation group does (#7610): it declares
          // `translation: { key: '<project>:<path>' }`, adopted as its tkey. The key's project is a
          // catalog namespace nothing else loads - a lens project contributes no perspective - so
          // the shell adds it here, or the switcher stays in English (#8133).
          this.projections.forEach(p => {
            const key = p.translation && p.translation.key;
            if (!p.tkey && typeof key === 'string' && key.indexOf(':') > 0) p.tkey = key;
          });
          const namespaces = [...new Set(this.projections.map(p => (p.tkey || '').split(':')[0])
            .filter(ns => ns && ns !== 'application-core'))];
          if (namespaces.length && window.AppI18nAddNamespaces) AppI18nAddNamespaces(namespaces);
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
      } else {
        // The hosted app stays on screen under the new projection: its page re-reads (#8130).
        this.reenterHostedApp();
      }
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
    // The KPI widgets split by the shape they render as: one grid holding both number tiles and the wide
    // top-N tables leaves holes around the tables, so the dashboard lays each kind out in its own grid.
    summaryKpis() { return this.dashKpiReports().filter(r => r.widget.kind !== 'list'); },
    listKpis() { return this.dashKpiReports().filter(r => r.widget.kind === 'list'); },
    dashPreviewReports() { return Alpine.store('reports').previewReports().filter(r => this.inProjection(r.url)); },

    /** Settings entities scoped to the projection: those of a group the projection lists (a group
     *  means everything in it - sidebar items and settings, #8138), explicitly cherry-picked ones, and
     *  those of a project with a visible sidebar item. */
    visibleSettingsItems() {
      const proj = this.activeProjection();
      if (!proj) return this.settingsItems;
      const groupIds = proj.groups || [];
      const itemIds = proj.items || [];
      return this.settingsItems.filter(it => groupIds.includes(it.groupId) || itemIds.includes(it.id) || this.inProjection(it.path));
    },

    get isDashboard() { return this.currentPath === '/' || this.currentPath === '/dashboard'; },

    /** The crumbs after the Home icon, derived from the route the way the generated application shell
     *  derives its own: a built-in page (with the selected report or setting), or a hosted app as its
     *  navigation group, the app, and the page the app shows. The hosted app runs embedded, which hides
     *  its own breadcrumb, so this trail is the only one on screen. The last crumb is the current page;
     *  a crumb with `open` links back. */
    get breadcrumbTrail() {
      if (this.isDashboard) return [{ label: this.tr('application-core:shell.nav.dashboard', 'Dashboard') }];
      if (this.currentPath.indexOf('/app/') === 0) return this.hostedAppTrail();
      const [top, rest, ...inner] = this.currentPath.split('/').filter(Boolean);
      if (top === 'settings') return this.settingsTrail(rest ? decodeURIComponent(rest) : '', inner.join('/'));
      const BUILTIN = { inbox: 'Inbox', issues: 'Issues', documents: 'Documents', reports: 'Reports' };
      if (!BUILTIN[top]) return [];
      const label = this.tr('application-core:shell.nav.' + top, BUILTIN[top]);
      const reports = Alpine.store('reports');
      const report = top === 'reports' && reports.selected;
      if (!report) return [{ label }];
      return [{ label, open: () => this.navigate('/reports') }, { label: reports.displayLabel(report) }];
    },

    /** The label of a built-in settings section (one rendered by the shell itself), or '' for any other id. */
    builtinSettingLabel(id) {
      const BUILTIN = {
        'region-language': ['regionLanguage', 'Region & Language'],
        'tenant-configuration': ['tenantConfig', 'Tenant Configuration'],
        'document-numbering': ['numbering', 'Document Numbering'],
        'tenant-users': ['tenantUsers', 'Users'],
        'print-templates': ['printTemplates', 'Print Templates'],
        'tenant-export': ['tenantExport', 'Data export'],
      };
      return BUILTIN[id] ? this.tr('application-core:shell.settings.' + BUILTIN[id][0], BUILTIN[id][1]) : '';
    },

    /** The name of the selected setting, built-in or an app's entity ('' with none selected). */
    get settingsSelectedLabel() {
      const id = this.settingsSelected;
      const item = id && !this.builtinSettingLabel(id) && this.settingsItems.find(i => i.id === id);
      return this.builtinSettingLabel(id) || (item ? this.itemLabel(item) : '');
    },

    /** Settings > the selected setting [> the page its app shows]. Settings itself does not link: its list
     *  stays beside the detail. The entity links back to its list in place, like a hosted app's landing. */
    settingsTrail(id, inner) {
      const crumbs = [{ label: this.tr('application-core:shell.nav.settings', 'Settings') }];
      const builtin = this.builtinSettingLabel(id);
      const item = !builtin && this.settingsItems.find(i => i.id === id);
      if (builtin) {
        crumbs.push({ label: builtin });
      } else if (item) {
        const page = this.hostedPageLabel(item, inner || '');
        if (!page) crumbs.push({ label: this.itemLabel(item) });
        else crumbs.push({ label: this.itemLabel(item), open: () => this.showHostedLanding(item, this.settingsFrame()) }, { label: page });
      }
      return crumbs;
    },

    /** Group > App [> page], where the page is the embedded app's route beyond its landing one. */
    hostedAppTrail() {
      const match = this.currentPath.match(/^\/app\/([^/]+)(?:\/(.*))?$/);
      const id = decodeURIComponent(match[1]);
      const group = this.groups.find(g => g.items.some(i => i.id === id));
      if (!group) return [];
      const item = group.items.find(i => i.id === id);
      const crumbs = [{ label: this.itemLabel(group) }];
      const page = this.hostedPageLabel(item, match[2] || '');
      if (!page) {
        crumbs.push({ label: this.itemLabel(item) });
        return crumbs;
      }
      crumbs.push({ label: this.itemLabel(item), open: () => this.showHostedLanding(item) }, { label: page });
      return crumbs;
    },

    /** The page an embedded app shows beyond its landing list, or '' for the list itself. A record
     *  selected on the list (/<Entity>/:id) opens in a sheet over it, so it is still the list; only
     *  /create, /:id/edit and the like are pages of their own. A route outside the landing one names nothing. */
    hostedPageLabel(item, inner) {
      const landing = this.landingRoute(item).split('/').filter(Boolean);
      const segments = inner.split('/').filter(Boolean);
      if (landing.some((s, i) => segments[i] !== s)) return '';
      const rest = segments.slice(landing.length);
      if (rest.length === 0 || (rest.length === 1 && rest[0] !== 'create')) return '';
      const last = rest[rest.length - 1];
      if (last === 'create') return this.tr('application-core:shell.nav.create', 'Create');
      if (last === 'edit') return this.tr('application-core:shell.nav.edit', 'Edit');
      return last.charAt(0).toUpperCase() + last.slice(1);
    },

    /** The route a perspective opens its app at: the hash of its path (e.g. /Customer, /my/Customer). */
    landingRoute(item) {
      const path = item.path || '';
      const hashAt = path.indexOf('#');
      return hashAt === -1 ? '/' : '/' + path.slice(hashAt + 1).replace(/^\/+/, '');
    },

    /** Route an embedded app - the hosted one, or with `settings` the selected setting's - to the inner route
     *  its address names (#8638), in place, through its own router: a dirty form's unsaved-changes guard then
     *  applies as for any navigation inside the app, and a refused one leaves the form, whose router end
     *  mirrors its own address back. An app without that router (or on another origin) has its frame pointed
     *  at the route instead. */
    routeEmbedded(settings, inner) {
      const item = settings ? this.findSettingItem(this.settingsSelected) : this.findItem(this.hostedId);
      if (!item) return;
      const frame = settings ? this.settingsFrame() : this.$refs.hostedFrame;
      if (settings) this.settingsInner = inner; else this.hostedInner = inner;
      let router = null;
      try { router = frame && frame.contentWindow && frame.contentWindow.PineconeRouter; } catch (e) { router = null; } // cross-origin guard
      const url = this.appUrl(item, inner);
      const hashAt = url.indexOf('#');
      if (router) router.navigate(hashAt === -1 ? '/' : url.slice(hashAt + 1));
      else this.pointFrame(frame, url);
    },

    /** Route an embedded app (the hosted one's frame unless `frame` is given) back to its landing page in
     *  place - reloading the iframe would lose its state. The app's own router hears it, and mirrorInner then
     *  updates the address bar and the trail. */
    showHostedLanding(item, frame) {
      const target = frame || this.$refs.hostedFrame;
      const win = target && target.contentWindow;
      if (!win) return;
      const route = this.landingRoute(item);
      if (win.PineconeRouter) win.PineconeRouter.navigate(route);
      else win.location.hash = '#' + route;
    },

    itemLabel(it) { return it.tkey ? this.tr(it.tkey, it.label) : it.label; },
    tr(key, fallback) { return window.T ? T(key, fallback) : fallback; },

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

    openSideNav() { this.showSidebarSheet = true; },
    closeSideNav() { this.showSidebarSheet = false; },
  }));
}, { once: true });
