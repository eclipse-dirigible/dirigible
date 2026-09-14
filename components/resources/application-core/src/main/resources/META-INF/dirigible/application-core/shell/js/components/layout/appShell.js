/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * Adopted from codbex-athena-app (js/components/layout/appShell.js).
 *
 * The reusable Harmonia runtime dashboard: sidebar nav, route-derived breadcrumb,
 * responsive sidebar<->x-h-sheet drawer collapse, theme menu. Model-independent —
 * its only project-specific input is App.config (js/config.js).
 *
 * TODO (Phase 1/2): the `nav` / `routeTitles` / `sections` maps are currently a
 * demo placeholder. They should be generated from the model (one entry per
 * navigation node / list perspective) — see template/ui/navigation.js.
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

    currentPath: '',

    // Embedded mode: when this SPA is hosted inside the shared application shell (the platform
    // dashboard loads each perspective with ?embedded), hide our own sidebar/chrome so the shared
    // shell provides the single navigation. Standalone (no ?embedded) is unaffected.
    embedded: false,

    // Route segment -> nav label, generated from the model (window.__harmoniaNav in index.html);
    // used to label the breadcrumb's entity crumb the same way the sidebar labels it.
    navLabels: {},

    init() {
      this.navLabels = window.__harmoniaNav || {};
      // The toast overlay is rendered by this component, so it lends its $notifications magic to
      // the shared store - a store has no component scope of its own to reach the magic from.
      Alpine.store('notifications').attachToaster(this.$notifications);
      try { this.embedded = new URLSearchParams(window.location.search).has('embedded'); } catch (e) { /* no URL */ }
      const getPath = () => {
        const p = window.PineconeRouter.context?.path || '/';
        return p === '/' ? '/dashboard' : p;
      };
      this.currentPath = getPath();
      window.addEventListener('popstate', () => { this.currentPath = getPath(); });
      document.addEventListener('pinecone:start', () => { this.routeLoading = true; });
      document.addEventListener('pinecone:fetch-error', () => { this.routeLoading = false; });
      document.addEventListener('pinecone:end', () => {
        this.routeLoading = false;
        this.currentPath = getPath();
      });

      this._bp = Harmonia.getBreakpointListener((isNarrow) => {
        this.isSmallScreen = isNarrow;
        // The sidebar is ONE element, moved between its wide-screen slot and the drawer. Embedded
        // mode has no navigation of its own (the host shell owns it), so it parks in the drawer -
        // which is closed, and whose trigger is hidden - at every width.
        const home = isNarrow || this.embedded ? this.$refs.sidebarSheet : this.$refs.sidebarSlot;
        home.appendChild(this.$refs.sidebar);
      }, 1024);
    },

    destroy() {
      if (this._bp) this._bp.remove();
    },

    get pageTitle() {
      const trail = this.breadcrumbTrail;
      return trail.length ? trail[trail.length - 1].label : (window.T ? T('application-core:shell.nav.home', 'Home') : 'Home');
    },

    get isDashboard() {
      return this.currentPath === '/' || this.currentPath === '/dashboard';
    },

    // Crumbs after Home (Home is rendered separately as the icon). Derived from the path:
    // a built-in shell section, or an entity route /<Entity>[/create | /:id/edit]. The last crumb
    // is the current page (no route); earlier crumbs link back.
    get breadcrumbTrail() {
      if (this.isDashboard) return [];
      const segments = this.currentPath.split('/').filter(Boolean);
      const top = segments[0];
      const crumbs = [];

      const SHELL = { inbox: 'Inbox', documents: 'Documents', settings: 'Settings', reports: 'Reports' };
      if (SHELL[top]) {
        const selectedReport = top === 'reports' && this.$store.reports && this.$store.reports.selected;
        crumbs.push({ label: SHELL[top], route: selectedReport ? '/' + top : null });
        if (selectedReport) crumbs.push({ label: this.$store.reports.displayLabel(this.$store.reports.selected), route: null });
        return crumbs;
      }

      // Entity route: top is the entity name; label it the same way the sidebar does.
      const isList = segments.length === 1;
      const navKeys = window.__harmoniaNavKeys || {};
      crumbs.push({ label: window.T ? T(navKeys[top], this.navLabel(this.navLabels[top] || top)) : this.navLabel(this.navLabels[top] || top), route: isList ? null : '/' + top });
      if (!isList) {
        const last = segments[segments.length - 1];
        const action = last === 'create' ? (window.T ? T('application-core:shell.nav.create', 'Create') : 'Create')
                : last === 'edit' ? (window.T ? T('application-core:shell.nav.edit', 'Edit') : 'Edit')
                : this.navLabel(last);
        crumbs.push({ label: action, route: null });
      }
      return crumbs;
    },

    isActive(route) { return this.currentPath === route; },

    // Open the task behind a (task-derived) notification, and mark it read.
    openNotification(n) {
      if (n && n.task) {
        this.$store.processTasks.openTask(n.task);
        n.unread = false;
      }
    },

    // Sidebar nav label safety net: humanize a PascalCase/camelCase label so multi-word names read
    // as words (e.g. "SalesInvoice" -> "Sales Invoice"). Idempotent on already-spaced labels, so it
    // is safe over the model's menuLabel. Pluralization is the model's job (the intent generator
    // emits plural menuLabels), so this does NOT pluralize.
    navLabel(label) {
      return String(label || '')
        .replace(/[_-]+/g, ' ')
        .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
        .replace(/\s+/g, ' ')
        .trim();
    },

    navigate(route) {
      window.PineconeRouter.navigate(route);
      this.closeSideNav();
    },

    openSideNav() { this.showSidebarSheet = true; },

    closeSideNav() { this.showSidebarSheet = false; },
  }));
}, { once: true });
