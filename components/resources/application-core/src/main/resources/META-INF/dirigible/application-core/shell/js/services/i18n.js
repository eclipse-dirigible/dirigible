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
 * i18n — UI-label translations for the Harmonia stack, REUSING the platform's existing i18next
 * machinery end-to-end: the i18next webjar (self-loaded below), the `locales.js` extension service
 * that aggregates every project's `translations/<locale>/*.json` catalogs from the registry, the
 * `platform-locales` + `application-locales` extension points that register the available locales,
 * and the very same catalogs/keys the `translate` generation action emits for the AngularJS stack.
 *
 * Generated views bind labels as `T('<project>:<tprefix>.t.<KEY>', 'English fallback')`. T() is
 * reactive through the Alpine `i18n` store: pages render the English fallback instantly and swap
 * to the translated labels the moment the catalogs finish loading. The current language is the
 * platform-wide locale flag (localStorage `codbex.harmonia.language`, a 2-letter code) resolved
 * against the registered locale ids (`en` -> `en-US`); anything a module has not translated falls
 * back through i18next's `fallbackLng` to English, so partial catalogs degrade gracefully.
 */
(() => {
  const I18NEXT_WEBJAR = '/webjars/i18next/dist/umd/i18next.min.js';
  const LOCALES_SERVICE = '/services/js/platform-core/extension-services/locales.js';
  const STORAGE_KEY = 'codbex.harmonia.language';
  const DEFAULT_LOCALE = 'en-US';

  let catalogsReady = false;

  const markReady = () => {
    catalogsReady = true;
    if (window.Alpine && Alpine.store('i18n')) Alpine.store('i18n').ready = true;
  };

  document.addEventListener('alpine:init', () => {
    Alpine.store('i18n', {
      ready: catalogsReady,
      // Translate a fully-qualified key ('ns:path'). In the DEFAULT language the baked English
      // fallback always wins (it is the same authored label, often prettier than the raw catalog
      // value); in any other language the key resolves against THAT language's own catalog only,
      // so an untranslated key degrades to the pretty English literal - never to a raw name.
      t(key, fallback, options) {
        if (!key) return fallback !== undefined ? fallback : '';
        if (this.ready && window.i18next
            && i18next.exists(key, Object.assign({ fallbackLng: false }, options))) {
          return i18next.t(key, options);
        }
        return fallback !== undefined ? fallback : key;
      },
    });
  }, { once: true });

  // The global the generated templates bind against (safe in Velocity-rendered files - no '$').
  window.T = (key, fallback, options) => {
    const store = window.Alpine && Alpine.store('i18n');
    if (store) return store.t(key, fallback, options);
    return fallback !== undefined ? fallback : key;
  };

  const loadScript = (src) => new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = src;
    script.onload = resolve;
    script.onerror = () => reject(new Error('Failed to load ' + src));
    document.head.appendChild(script);
  });

  // The catalogs to request: the hosting app's own project namespace ('common' always included by
  // the service). The shared application shell has no project - it uses only 'common' keys.
  const namespace = (window.App && App.config && App.config.projectName) || 'shell';

  const init = async () => {
    try {
      const code = (() => {
        try { return localStorage.getItem(STORAGE_KEY) || 'en'; } catch (e) { return 'en'; }
      })();
      // The default language renders entirely from the baked-in literals - no catalogs needed.
      if (DEFAULT_LOCALE.indexOf(code + '-') === 0) return;
      const params = 'extensionPoints=platform-locales&extensionPoints=application-locales&namespaces='
          + encodeURIComponent(namespace);
      const [response] = await Promise.all([
        fetch(LOCALES_SERVICE + '?' + params, {
          credentials: 'same-origin',
          headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
        }),
        loadScript(I18NEXT_WEBJAR),
      ]);
      if (!response.ok) throw new Error('locales service returned ' + response.status);
      const data = await response.json();
      const locales = Array.isArray(data.locales) ? data.locales : [];
      // Resolve the 2-letter platform code against the registered locale ids (en -> en-US).
      const resolved = locales.find(l => l.id === code) || locales.find(l => (l.id || '').indexOf(code + '-') === 0);
      const language = resolved ? resolved.id : DEFAULT_LOCALE;
      await i18next.init({
        lng: language,
        fallbackLng: DEFAULT_LOCALE,
        defaultNS: 'common',
        interpolation: { skipOnVariables: false },
        resources: data.translations || {},
      });
      markReady();
    } catch (e) {
      // Untranslated fallbacks keep rendering - the page stays fully usable in English.
      console.error('i18n: catalogs unavailable, using built-in labels', e);
    }
  };

  init();
})();
