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
 * settingsPage — the built-in Settings section as a master-detail (mirrors the Process Inbox and the
 * entity master-detail view). The setting entities are listed on the left; selecting one loads that
 * entity's generated manage-list view fragment inline on the right via x-html — which initializes
 * Alpine on the injected markup and binds its <Entity>ManageListPage component — so a setting's CRUD
 * list opens in place instead of taking over the main pane. Create/edit still route to the entity's
 * own form page (then return to that entity's list route).
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('settingsPage', () => ({
    ...basePage(),
    selected: null,   // the selected setting entity name
    content: '',      // the selected entity's manage-list fragment HTML (rendered via x-html)
    loading: false,
    error: null,
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
    },

    // The platform's supported language codes and their display names (delegates to the locale store).
    languageOptions() {
      const locale = Alpine.store('locale');
      if (!locale) return [];
      return locale.languages().map((code) => ({ value: code, text: locale.displayName(code) }));
    },

    async select(name, url) {
      if (this.selected === name) return;
      this.selected = name;
      this.error = null;
      this.loading = true;
      this.content = '';
      try {
        const r = await fetch(url, { credentials: 'same-origin' });
        if (!r.ok) throw new Error('HTTP ' + r.status);
        this.content = await r.text();
      } catch (e) {
        console.error('settings: failed to load view for ' + name, e);
        this.error = 'Could not load ' + name + '.';
      } finally {
        this.loading = false;
        this.refreshIcons();
      }
    },
  }));
}, { once: true });
