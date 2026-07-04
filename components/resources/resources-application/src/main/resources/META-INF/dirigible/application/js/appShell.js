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
      const coverage = await Promise.all([...bases].map(async ([app, base]) => {
        let provided = ['en'];
        try {
          const res = await fetch(base + 'js/config.js', { credentials: 'same-origin' });
          if (res.ok) {
            const match = (await res.text()).match(/languages:\s*(\[[^\]]*\])/);
            if (match) {
              const codes = JSON.parse(match[1].replace(/'/g, '"'));
              if (Array.isArray(codes) && codes.length) provided = codes;
            }
          }
        } catch (e) { /* undeclared coverage counts as default-language only */ }
        return { app, provided };
      }));
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

    async init() {
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
            } else if (g.path && g.kind === 'PRIMARY') {
              // A standalone (un-grouped) app entity perspective.
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
          // Fire-and-forget: scan which languages each embedded app provides translations for
          // (drives the missing-translations warnings in Settings).
          this.loadLanguageCoverage(all);
        }
      } catch (e) {
        console.error('Failed to load application perspectives', e);
      }
      this.loading = false;

      // Mirror the shared locale store so the Settings picker has a plain bindable property;
      // persisting goes through the store (the fetch client sends it as Accept-Language).
      const locale = Alpine.store('locale');
      if (locale) {
        this.language = locale.value;
        this.$watch('language', (v) => locale.set(v));
      }

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
            if (item) { this.settingsSelected = item.id; this.settingsUrl = item.path || ''; }
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
        this.refreshIcons();
      }
    },

    isSettingActive(item) { return this.settingsMode && this.settingsSelected === item.id; },

    /** Find a setting entity (perspective) by id. 'region-language' is the built-in shell entry
     *  (the platform language preference) - it has no app path; its detail renders locally. */
    findSettingItem(id) {
      if (id === 'region-language') return { id: 'region-language' };
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

    isBuiltinActive(route) { return !this.hostedUrl && this.currentPath === route; },
    isAppActive(item) { return this.hostedId === item.id; },
    isSvgIcon(icon) { return !!icon && /\.svg(\?|#|$)/i.test(icon); },
    isImageIcon(icon) { return !!icon && !this.isSvgIcon(icon) && (icon.indexOf('/') !== -1 || icon.indexOf('.') !== -1 || icon.indexOf('http') === 0); },

    openSideNav() { this.isOpen = true; },
    closeSideNav() { if (window.matchMedia('(max-width: 1024px)').matches) this.isOpen = false; },
    refreshIcons() { if (window.lucide && typeof window.lucide.createIcons === 'function') window.lucide.createIcons(); },
  }));
}, { once: true });
