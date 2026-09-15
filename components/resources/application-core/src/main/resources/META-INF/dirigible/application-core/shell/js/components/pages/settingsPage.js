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
 * settingsPage — the built-in Settings section as a master-detail: every settings item is a row in
 * the list on the left, its content opens in the pane on the right. A platform preference (Region &
 * Language and the display formats) share one pane and render from the page's own markup; a SETTING
 * entity's generated manage-list view fragment is fetched only when its row is picked and rendered
 * inline via x-html — which initializes Alpine on the injected markup and binds its
 * <Entity>ManageListPage component — so a module declaring many nomenclatures loads one of them, not
 * all of them. That embedded list opens create/edit/preview in the shared related-record dialog
 * instead of navigating, so this pane keeps its place (the list page's inlineHosted()).
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('settingsPage', () => ({
    ...basePage(),
    selected: null,     // 'regionLanguage' | the selected setting entity's name
    selectedTitle: '',  // its label - the compact toolbar names what is open, the list being hidden
    entityUrl: null,    // the selected entity's view fragment; null for a platform preference
    content: '',        // the fetched fragment HTML (rendered via x-html)
    loading: false,
    error: null,
    // Below the breakpoint the list and the detail pane take turns (the split hides one of them), so
    // the detail pane shows a back control instead of leaving the list unreachable.
    isCompact: false,
    // Region & Language: the platform's single language flag, mirrored from the locale store so the
    // picker's x-model has a plain component property; changes persist through the store (and take
    // effect on the next data load - the fetch client sends the value as Accept-Language).
    language: 'en',

    init() {
      const locale = Alpine.store('locale');
      if (locale) {
        this.language = locale.value;
        this.$watch('language', (v) => locale.set(v));
      }
      this._breakpoint = Harmonia.getBreakpointListener((isNarrow) => { this.isCompact = isNarrow; }, 1024);
    },

    destroy() {
      if (this._breakpoint) this._breakpoint.remove();
    },

    // The platform's supported language codes and their display names (delegates to the locale store).
    languageOptions() {
      const locale = Alpine.store('locale');
      if (!locale) return [];
      return locale.languages().map((code) => ({ value: code, text: locale.displayName(code) }));
    },

    // Open a settings item. A platform preference passes no url and renders from the page's own
    // markup; a setting entity passes its view fragment, which is fetched here.
    async select(name, url, title) {
      if (this.selected === name) return;
      this.selected = name;
      this.selectedTitle = title || '';
      this.entityUrl = url || null;
      this.error = null;
      this.content = '';
      if (!url) return;
      this.loading = true;
      try {
        const r = await fetch(url, { credentials: 'same-origin' });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        this.content = await r.text();
      } catch (e) {
        console.error('settings: failed to load the view for ' + name, e);
        this.error = window.T ? T('application-core:shell.settings.loadFailed', 'Could not load this setting.')
                : 'Could not load this setting.';
      } finally {
        this.loading = false;
      }
    },

    // Back to the list on a narrow screen, where the split shows one panel at a time.
    back() {
      this.selected = null;
      this.selectedTitle = '';
      this.entityUrl = null;
      this.content = '';
      this.error = null;
    },
  }));
}, { once: true });
