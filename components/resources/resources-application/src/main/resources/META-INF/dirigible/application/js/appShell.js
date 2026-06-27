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
/*
 * The Harmonia application shell (pure Java + Harmonia, no AngularJS). It aggregates the
 * `application-perspectives` extension point - the same one the generated apps contribute to - via the
 * platform-core perspectives service, renders the grouped sidebar, and hosts the selected
 * perspective's page in an iframe. Generated Harmonia apps expose their perspective paths in embedded
 * mode (?embedded), so their own sidebar is hidden and this shell provides the single chrome.
 */
const PERSPECTIVES_URL = '/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-perspectives';

document.addEventListener('alpine:init', () => {
  Alpine.data('app', () => ({
    groups: [],
    utilities: [],
    selected: null,
    loading: true,
    failed: false,
    hiddenPanels: { left: false },
    isOpen: false,

    async init() {
      try {
        const res = await fetch(PERSPECTIVES_URL, { headers: { 'Accept': 'application/json' } });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        this.groups = Array.isArray(data.perspectives) ? data.perspectives : [];
        this.utilities = Array.isArray(data.utilities) ? data.utilities : [];
      } catch (e) {
        this.failed = true;
        console.error('Failed to load application perspectives', e);
      }
      this.loading = false;

      // Restore the selection from the hash (deep-link / refresh), else the first item.
      const all = this.allItems();
      const fromHash = this.hashId();
      this.selected = (fromHash && all.find(p => p.id === fromHash)) || all[0] || null;

      // Collapse the sidebar to a drawer on narrow viewports (Harmonia breakpoint).
      this._bp = Harmonia.getBreakpointListener((isNarrow) => {
        this.hiddenPanels.left = isNarrow;
        if (isNarrow) {
          this.$refs.overlay.appendChild(this.$refs.sidebar);
        } else {
          this.$refs.sidebarPanel.appendChild(this.$refs.sidebar);
        }
      }, 1024);

      window.addEventListener('hashchange', () => {
        const id = this.hashId();
        const found = id && this.allItems().find(p => p.id === id);
        if (found) this.selected = found;
      });

      this.$nextTick(() => this.refreshIcons());
    },

    destroy() {
      if (this._bp) this._bp.remove();
    },

    /** Every selectable entry: all group items plus the utility perspectives. */
    allItems() {
      const items = [];
      for (const g of this.groups) {
        if (Array.isArray(g.items)) items.push(...g.items);
      }
      items.push(...this.utilities);
      return items;
    },

    hashId() {
      return location.hash ? decodeURIComponent(location.hash.slice(1)) : null;
    },

    select(perspective) {
      this.selected = perspective;
      location.hash = encodeURIComponent(perspective.id);
      this.closeSideNav();
    },

    isActive(perspective) {
      return !!this.selected && this.selected.id === perspective.id;
    },

    /** The URL hosted in the content iframe (blank until a perspective is chosen). */
    contentUrl() {
      return this.selected && this.selected.path ? this.selected.path : 'about:blank';
    },

    /** An icon is an image when it looks like a path/URL; otherwise it is a Lucide icon name. */
    isImageIcon(icon) {
      return !!icon && (icon.indexOf('/') !== -1 || icon.indexOf('.') !== -1 || icon.indexOf('http') === 0);
    },

    openSideNav() { this.isOpen = true; },

    closeSideNav() {
      if (window.matchMedia('(max-width: 1024px)').matches) this.isOpen = false;
    },

    refreshIcons() {
      if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
      }
    },
  }));
}, { once: true });
