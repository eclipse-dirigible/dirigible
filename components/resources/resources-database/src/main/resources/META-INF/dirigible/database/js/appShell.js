/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Database shell controller. Deliberately thin, like the Monitoring shell's: two fixed pages,
 * no aggregated perspectives and no hosted iframes, so it only tracks the current route for the
 * sidebar highlight and hands the narrow-viewport drawer to Harmonia. Everything cross-page - theme,
 * user, branding, language - is a store from the shared runtime.
 */
document.addEventListener('alpine:init', () => {
  window.PineconeRouter.settings({
    basePath: (App.config && App.config.basePath) || '',
    hash: true
  });

  Alpine.data('app', () => ({
    hiddenPanels: { left: false },
    isOpen: false,
    currentPath: '/explorer',

    init() {
      // Keep the sidebar highlight in step with the URL, including deep links and back/forward.
      const applyRoute = () => {
        const hash = window.location.hash || '';
        const path = (hash.charAt(0) === '#' ? hash.slice(1) : hash) || '/';
        this.currentPath = path === '/' ? '/explorer' : path;
      };
      window.addEventListener('popstate', applyRoute);
      document.addEventListener('pinecone:end', applyRoute);
      applyRoute();

      // Below 1024px the sidebar moves into the Harmonia drawer.
      this._breakpoint = Harmonia.getBreakpointListener((isNarrow) => {
        this.hiddenPanels.left = isNarrow;
        if (isNarrow) {
          this.$refs.overlay.appendChild(this.$refs.sidebar);
        } else {
          this.$refs.sidebarPanel.appendChild(this.$refs.sidebar);
        }
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

    openSideNav() { this.isOpen = true; },
    closeSideNav() { if (window.matchMedia('(max-width: 1024px)').matches) this.isOpen = false; },
  }));
}, { once: true });
