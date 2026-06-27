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

    async init() {
      try {
        const res = await fetch(PERSPECTIVES_URL, { headers: { 'Accept': 'application/json' } });
        if (res.ok) {
          const data = await res.json();
          // Domain apps live in named groups; skip the platform's 'undefined-group' and utilities
          // (those are the legacy AngularJS perspectives, which do not belong in the Harmonia shell).
          this.groups = (Array.isArray(data.perspectives) ? data.perspectives : [])
            .filter(g => g.id !== 'undefined-group' && Array.isArray(g.items) && g.items.length);
        }
      } catch (e) {
        console.error('Failed to load application perspectives', e);
      }
      this.loading = false;

      const getPath = () => {
        const p = window.PineconeRouter.context?.path || '/';
        return p === '/' ? '/dashboard' : p;
      };
      this.currentPath = getPath();
      window.addEventListener('popstate', () => { this.currentPath = getPath(); this.refreshIcons(); });
      document.addEventListener('pinecone:end', () => { this.currentPath = getPath(); this.refreshIcons(); });

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

    /** Navigate to a built-in page (Pinecone route into #app); leaves any hosted app. */
    navigate(route) {
      this.hostedUrl = '';
      this.hostedId = '';
      window.PineconeRouter.navigate(route);
      this.closeSideNav();
    },

    /** Host a domain app (a perspective) in the iframe; #app is hidden while hosted. */
    openApp(item) {
      this.hostedId = item.id;
      this.hostedUrl = item.path || '';
      this.closeSideNav();
    },

    isBuiltinActive(route) { return !this.hostedUrl && this.currentPath === route; },
    isAppActive(item) { return this.hostedId === item.id; },
    isImageIcon(icon) { return !!icon && (icon.indexOf('/') !== -1 || icon.indexOf('.') !== -1 || icon.indexOf('http') === 0); },

    openSideNav() { this.isOpen = true; },
    closeSideNav() { if (window.matchMedia('(max-width: 1024px)').matches) this.isOpen = false; },
    refreshIcons() { if (window.lucide && typeof window.lucide.createIcons === 'function') window.lucide.createIcons(); },
  }));
}, { once: true });
