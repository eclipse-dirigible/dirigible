/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Monitoring shell controller. Deliberately thin: the shell has a fixed set of pages (no
 * aggregated perspectives, no hosted iframes), so it only tracks the current route for the sidebar
 * highlight and hands the narrow-viewport drawer to Harmonia. Everything cross-page - theme, user,
 * branding, language - is a store from the shared runtime.
 */
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

    currentPath: '/overview',

    init() {
      // Keep the sidebar highlight in step with the URL, including deep links and back/forward.
      const applyRoute = () => {
        const hash = window.location.hash || '';
        const path = (hash.charAt(0) === '#' ? hash.slice(1) : hash) || '/';
        this.currentPath = path === '/' ? '/overview' : path;
      };
      window.addEventListener('popstate', applyRoute);
      document.addEventListener('pinecone:end', applyRoute);
      document.addEventListener('pinecone:start', () => { this.routeLoading = true; });
      document.addEventListener('pinecone:fetch-error', () => { this.routeLoading = false; });
      document.addEventListener('pinecone:end', () => { this.routeLoading = false; });
      applyRoute();

      this._breakpoint = Harmonia.getBreakpointListener((isNarrow) => {
        this.isSmallScreen = isNarrow;
        // The sidebar is ONE element, moved between its wide-screen slot and the drawer.
        const home = isNarrow ? this.$refs.sidebarSheet : this.$refs.sidebarSlot;
        home.appendChild(this.$refs.sidebar);
      }, 1024);
    },

    destroy() {
      if (this._breakpoint) this._breakpoint.remove();
    },

    navigate(route) {
      window.PineconeRouter.navigate(route);
      this.closeSideNav();
    },

    isActive(route) {
      return this.currentPath === route;
    },

    openSideNav() { this.showSidebarSheet = true; },
    closeSideNav() { this.showSidebarSheet = false; },
  }));
}, { once: true });
