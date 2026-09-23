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
 * the list on the left, its content opens in the pane on the right. The platform preferences (the
 * language flag and the display formats) share one pane and render from the page's own markup; a
 * SETTING entity's generated manage-list view is fetched only when its row is picked, so a module
 * declaring many nomenclatures loads one of them, not all of them. That embedded list opens
 * create/edit/preview in the shared related-record dialog instead of navigating, so this pane keeps
 * its place (the list page's inlineHosted()).
 *
 * The fetch and the render are Harmonia's: the pane's host carries x-h-include, which loads the
 * fragment and initializes Alpine on it. Deliberately NOT a reactive x-html="content" binding -
 * Alpine re-runs such an effect on its own account, and a re-run replaces the whole injected
 * subtree: measured in a browser, opening the embedded list's Filter popover or its record sheet
 * rebuilt the fragment in the same tick, destroying the overlay that had just opened and loading the
 * entity a second time.
 *
 * What x-h-include does not do is report a failure - its only error path is a console.error - so the
 * page watches for the fragment:loaded event it dispatches and calls a missing fragment when that
 * event does not arrive. A silent blank pane would be the one outcome nobody can act on.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('settingsPage', () => ({
    ...basePage(),
    selected: null,     // 'regionLanguage' | the selected setting entity's name
    selectedTitle: '',  // its label - the compact toolbar names what is open, the list being hidden
    entityUrl: null,    // the selected entity's view fragment; null for a platform preference
    // Kept for pages generated before the host took over the fetch: their _settings.html binds
    // x-html="content", and this runtime is swapped under pages generated months earlier (#7427).
    content: '',
    // How long a fragment may take before the pane calls it missing (x-h-include reports nothing).
    loadTimeoutMs: 10000,
    _loadTimer: null,
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

    // The platform's supported language codes and their display names (delegates to the locale store).
    languageOptions() {
      const locale = Alpine.store('locale');
      if (!locale) return [];
      return locale.languages().map((code) => ({ value: code, text: locale.displayName(code) }));
    },

    // Open a settings item. A platform preference passes no url and renders from the page's own
    // markup; a setting entity passes its view fragment, which the host's x-h-include loads.
    select(name, url, title) {
      if (this.selected === name) return;
      this.selected = name;
      this.selectedTitle = title || '';
      this.error = null;
      this.awaitFragment(url);
      this.entityUrl = url || null;   // last: it is what x-h-include watches
    },

    // Wait for the fragment the host is about to load, or say it could not be loaded. A page
    // generated before this has no such host and fetches here instead, into the x-html binding.
    awaitFragment(url) {
      this.stopWaiting();
      if (!url) { this.loading = false; this.content = ''; return; }
      if (!this.$refs.entityView) { this.loadIntoBinding(url); return; }
      this.loading = true;
      this._loadTimer = setTimeout(() => {
        this._loadTimer = null;
        this.loading = false;
        this.error = window.T ? T('application-core:shell.settings.loadFailed', 'Could not load this setting.')
                : 'Could not load this setting.';
      }, this.loadTimeoutMs);
    },

    // x-h-include dispatches this on the host once the fragment is in the DOM and initialized.
    onFragmentLoaded() {
      this.stopWaiting();
      this.loading = false;
      this.error = null;
    },

    stopWaiting() {
      if (this._loadTimer) { clearTimeout(this._loadTimer); this._loadTimer = null; }
    },

    destroy() {
      this.stopWaiting();
      if (this._breakpoint) this._breakpoint.remove();
    },

    // A page generated before the host carried x-h-include renders from `content` through x-html.
    async loadIntoBinding(url) {
      this.loading = true;
      try {
        const r = await fetch(url, { credentials: 'same-origin' });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        this.content = await r.text();
      } catch (e) {
        console.error('settings: failed to load the view for ' + url, e);
        this.content = '';
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
      this.error = null;
      this.stopWaiting();
      this.loading = false;
      this.content = '';
    },
  }));
}, { once: true });
