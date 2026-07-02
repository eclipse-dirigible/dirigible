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
 * locale store — the app's single language flag (the Region & Language setting). A global store
 * (like the theme) persisted per user in localStorage. The shared fetch client (services/api.js)
 * sends the value as Accept-Language on every call, so the same flag drives BOTH the frontend and
 * the backend: generated multilingual repositories overlay <TABLE>_LANG values for it, and the
 * document Print flow prefers it. The available codes come from the generated App.config.languages
 * (the intent's `languages:`); a value outside that list falls back to the first entry.
 */
document.addEventListener('alpine:init', () => {
  const STORAGE_KEY = 'codbex.harmonia.language';

  Alpine.store('locale', {
    value: 'en',

    init() {
      const configured = this.languages();
      let saved = null;
      try { saved = localStorage.getItem(STORAGE_KEY); } catch (e) { /* storage unavailable */ }
      this.value = (saved && configured.includes(saved)) ? saved : (configured[0] || 'en');
    },

    // The app's offered data-language codes (from config.js; defaults to just 'en').
    languages() {
      const configured = (window.App && App.config && App.config.languages) || [];
      return Array.isArray(configured) && configured.length ? configured : ['en'];
    },

    // Human-readable name for a code ('bg' -> 'Bulgarian'), falling back to the code itself.
    displayName(code) {
      try {
        const name = new Intl.DisplayNames(['en'], { type: 'language' }).of(code);
        return name || code;
      } catch (e) { return code; }
    },

    set(lang) {
      if (!lang || lang === this.value) return;
      this.value = lang;
      try { localStorage.setItem(STORAGE_KEY, lang); } catch (e) { /* storage unavailable */ }
    },
  });
}, { once: true });
