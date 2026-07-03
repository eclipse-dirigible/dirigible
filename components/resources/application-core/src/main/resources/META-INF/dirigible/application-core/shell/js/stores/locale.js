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
 * locale store — the PLATFORM's single language flag (the Region & Language setting). A global
 * store (like the theme) persisted per user in localStorage. The shared fetch client
 * (services/api.js) sends the value as Accept-Language on every call, so the same flag drives
 * BOTH the frontend and the backend: generated multilingual repositories overlay <TABLE>_LANG
 * values for it, and the document Print flow prefers it. The offered codes are the platform's
 * supported language set (DIRIGIBLE_APPLICATION_LANGUAGES, default en,bg) — individual modules
 * never define what the stack supports; they only provide translations, and anything a module
 * does not translate falls back to the base (first/default) language naturally.
 */
document.addEventListener('alpine:init', () => {
  const STORAGE_KEY = 'codbex.harmonia.language';

  const LANGUAGES_URL = '/services/js/platform-core/services/application-languages.js';

  Alpine.store('locale', {
    value: 'en',
    // The platform's supported set; the served value replaces this default asynchronously.
    supported: ['en', 'bg'],

    init() {
      let saved = null;
      try { saved = localStorage.getItem(STORAGE_KEY); } catch (e) { /* storage unavailable */ }
      if (saved) this.value = saved;
      // Fire-and-forget: refresh the platform set (DIRIGIBLE_APPLICATION_LANGUAGES); on failure the
      // built-in default stands. A persisted value outside the set falls back to the first entry.
      fetch(LANGUAGES_URL, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then((r) => r.ok ? r.json() : null)
        .then((codes) => {
          if (Array.isArray(codes) && codes.length) this.supported = codes;
          if (!this.supported.includes(this.value)) this.value = this.supported[0];
        })
        .catch(() => { /* platform default stands */ });
    },

    // The platform's supported data-language codes (never per-module).
    languages() {
      return this.supported;
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
