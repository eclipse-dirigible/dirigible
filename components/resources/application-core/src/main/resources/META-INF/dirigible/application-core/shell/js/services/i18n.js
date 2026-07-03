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
      // Bumped whenever catalogs are added AFTER init (extra namespaces) so Alpine re-evaluates
      // every T() binding that already rendered its fallback.
      version: 0,
      // Translate a fully-qualified key ('ns:path'). In the DEFAULT language the baked English
      // fallback always wins (it is the same authored label, often prettier than the raw catalog
      // value); in any other language the key resolves against THAT language's own catalog only,
      // so an untranslated key degrades to the pretty English literal - never to a raw name.
      t(key, fallback, options) {
        this.version; // reactive dependency - see above
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

  // The catalogs to request: the platform shell chrome ('application-core', shipped with the shared
  // runtime) plus the hosting app's own project namespace ('common' is always included by the
  // service). The shared application shell has no project and uses only the shell catalog; the
  // namespaces of the apps it aggregates are added on demand via AppI18nAddNamespaces below.
  const project = window.App && App.config && App.config.projectName;
  const namespaces = project ? ['application-core', project] : ['application-core'];
  const loadedNamespaces = new Set(namespaces);

  const fetchCatalogs = async (requested) => {
    const params = 'extensionPoints=platform-locales&extensionPoints=application-locales'
        + requested.map(ns => '&namespaces=' + encodeURIComponent(ns)).join('');
    const response = await fetch(LOCALES_SERVICE + '?' + params, {
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    });
    if (!response.ok) throw new Error('locales service returned ' + response.status);
    return response.json();
  };

  // Resolved after init: false when the default language rendered from the baked literals (no
  // catalogs, i18next not even loaded), true when the catalogs are live.
  let initPromise;

  const init = async () => {
    try {
      const code = (() => {
        try { return localStorage.getItem(STORAGE_KEY) || 'en'; } catch (e) { return 'en'; }
      })();
      // The default language renders entirely from the baked-in literals - no catalogs needed.
      if (DEFAULT_LOCALE.indexOf(code + '-') === 0) return false;
      const [data] = await Promise.all([fetchCatalogs(namespaces), loadScript(I18NEXT_WEBJAR)]);
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
      return true;
    } catch (e) {
      // Untranslated fallbacks keep rendering - the page stays fully usable in English.
      console.error('i18n: catalogs unavailable, using built-in labels', e);
      return false;
    }
  };

  initPromise = init();

  // Load additional catalog namespaces AFTER init - the shared application shell calls this with
  // the projects of the perspectives it aggregates (their generated entity/label keys live in each
  // project's own namespace). No-op in the default language (baked literals) and for namespaces
  // already loaded; already-rendered T() bindings re-evaluate through the store's version bump.
  window.AppI18nAddNamespaces = async (extra) => {
    try {
      const active = await initPromise;
      if (!active) return;
      const missing = (extra || []).filter(ns => ns && !loadedNamespaces.has(ns));
      if (!missing.length) return;
      missing.forEach(ns => loadedNamespaces.add(ns));
      const data = await fetchCatalogs(missing);
      const translations = data.translations || {};
      for (const lng of Object.keys(translations)) {
        for (const ns of Object.keys(translations[lng])) {
          i18next.addResourceBundle(lng, ns, translations[lng][ns], true, true);
        }
      }
      const store = window.Alpine && Alpine.store('i18n');
      if (store) store.version++;
    } catch (e) {
      console.error('i18n: additional catalogs unavailable', e);
    }
  };
})();
