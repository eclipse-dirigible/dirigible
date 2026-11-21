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
 * languageCoverage store — what the embedded apps leave untranslated, per platform language, for the
 * "Incomplete translations" part of every shell's Settings (Region & Language). One implementation
 * for the four shells (#8789), in two parts (#8137):
 * - their SCREENS: whether the app ships a label catalog in that language (its i18n/<locale> files),
 *   asked of the locales service the i18n layer itself reads - one request for every app;
 * - their DATA: how many records of each multilingual entity have no row of that language in its
 *   <TABLE>_LANG table, as the entity's controller counts them (GET .../translations). The app's
 *   js/config.js lists those entities (translatableEntities). Only an app whose coverage cannot be
 *   read - generated before the list existed, or every answer failing - is judged by the languages it
 *   declares (the intent's `languages:`), as a hint. An entity the user may not read (403) is not
 *   theirs to report.
 * The configs are JS files, so they are read with targeted matches rather than executed. The scan
 * waits for Settings to open and runs once (#8346).
 */
document.addEventListener('alpine:init', () => {
  const LOCALES_URL = '/services/platform/extensions/locales';
  const JSON_HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };

  Alpine.store('languageCoverage', {
    /** One entry per scanned app: { app, provided, translatableData, entities, labelled }. */
    apps: [],
    // The perspectives the scan reads, kept until Settings first opens; null once scanned.
    pending: null,

    /** Keep the shell's perspectives for the scan Settings starts. */
    remember(perspectives) {
      this.pending = perspectives;
    },

    /** Scan the embedded apps once, the first time Settings is shown. */
    scan() {
      const perspectives = this.pending;
      if (!perspectives) return;
      this.pending = null;
      this.load(perspectives);
    },

    async load(perspectives) {
      const bases = new Map();
      const collect = (item) => {
        const path = item && item.path;
        const match = typeof path === 'string' && path.match(/^\/services\/web\/([^\/]+)\/([^#?]*\/)?index\.html/);
        if (match && !bases.has(match[1])) bases.set(match[1], '/services/web/' + match[1] + '/' + (match[2] || ''));
      };
      (perspectives || []).forEach(g => Array.isArray(g.items) ? g.items.forEach(collect) : collect(g));
      const configs = (await Promise.all([...bases].map(([app, base]) => this.readConfig(app, base)))).filter(Boolean);
      const labelled = await this.loadLabelLanguages(configs.map(c => c.app));
      this.apps = configs.map(c => Object.assign(c, { labelled: labelled ? labelled(c.app) : null }));
    },

    // Only generated apps carry a js/config.js. A missing or unreadable config means this perspective
    // is a platform surface (Inbox, Documents, ...) translated through the shared platform catalogs -
    // it must not be reported as an untranslated app. A readable config WITHOUT a languages
    // declaration is a generated app providing only the default language.
    async readConfig(app, base) {
      try {
        const res = await fetch(base + 'js/config.js', { credentials: 'same-origin' });
        if (!res.ok) return null;
        const text = await res.text();
        const match = text.match(/languages:\s*(\[[^\]]*\])/);
        let provided = ['en'];
        if (match) {
          const codes = JSON.parse(match[1].replace(/'/g, '"'));
          if (Array.isArray(codes) && codes.length) provided = codes;
        }
        const flag = text.match(/translatableData:\s*(true|false)/);
        const listed = text.match(/translatableEntities:\s*(\[.*\])/);
        const entities = listed ? await this.readEntityCoverage(JSON.parse(listed[1])) : null;
        return { app, provided, translatableData: flag ? flag[1] === 'true' : true, entities };
      } catch (e) {
        return null;
      }
    },

    // Each listed entity's coverage, { key, label, total, untranslated }. null when none could be
    // read - the app is then judged by its declaration; an entity the user may not read is left out.
    async readEntityCoverage(listed) {
      const answers = await Promise.all(listed.map(async (entity) => {
        try {
          const res = await fetch(entity.url, { credentials: 'same-origin', headers: JSON_HEADERS });
          if (res.status === 403) return { denied: true };
          if (!res.ok) return null;
          const coverage = await res.json();
          return Object.assign({}, entity, { total: coverage.total, untranslated: coverage.untranslated || {} });
        } catch (e) {
          return null;
        }
      }));
      if (listed.length && answers.every(answer => answer === null)) return null;
      return answers.filter(answer => answer && !answer.denied);
    },

    // The platform languages each app ships screen labels for, from one request to the locales
    // service; null when it cannot be read - the screens are then not judged at all, only the data.
    async loadLabelLanguages(apps) {
      if (!apps.length) return null;
      try {
        const query = 'extensionPoints=platform-locales&extensionPoints=application-locales'
            + apps.map(app => '&namespaces=' + encodeURIComponent(app)).join('');
        const res = await fetch(LOCALES_URL + '?' + query, { credentials: 'same-origin', headers: JSON_HEADERS });
        if (!res.ok) return null;
        const data = await res.json();
        const locales = (Array.isArray(data.locales) ? data.locales : []).map(l => l.id || '');
        const translations = data.translations || {};
        return (app) => Alpine.store('locale').languages().filter(code => {
          const locale = locales.find(id => id === code) || locales.find(id => id.indexOf(code + '-') === 0);
          const catalog = locale && translations[locale] && translations[locale][app];
          return !!catalog && Object.keys(catalog).length > 0;
        });
      } catch (e) {
        console.error('Failed to read the label catalogs of the applications', e);
        return null;
      }
    },

    // A language's name in the language the user reads ('bg' -> 'Bulgarian' in English, 'български'
    // in Bulgarian, which is how it reads inside a sentence); the code itself when Intl cannot tell.
    languageName(code) {
      try {
        return new Intl.DisplayNames([Alpine.store('locale').value], { type: 'language' }).of(code) || code;
      } catch (e) {
        return code;
      }
    },

    // What each app does not translate into the platform languages - the developers' to-do list, as
    // the lines of one warning per app: the languages it has no label catalog for (its pages fall back
    // to the baked English labels, which is why English itself never counts), then per multilingual
    // entity the records with no translation in a language it has labels for (or, without a readable
    // coverage, the languages it does not declare). An app with nothing to report is not listed.
    // Reactive on the scan, the platform set and the label catalogs.
    warnings() {
      const platform = Alpine.store('locale').languages();
      return this.apps
                 .map(({ app, provided, translatableData, entities, labelled }) => {
                   const screens = labelled ? platform.filter(code => code !== 'en' && !labelled.includes(code)) : [];
                   const lines = [];
                   if (screens.length > 0) {
                     lines.push(T('application-core:shell.settings.missingScreens', '{{app}} has no {{missing}} translation of its screens - its labels show in the default language.', { app, missing: screens.join(', ') }));
                   }
                   const judged = platform.filter(code => !screens.includes(code));
                   if (entities) {
                     entities.forEach(entity => judged.forEach(code => {
                       const count = entity.untranslated[code];
                       if (!(count > 0)) return;
                       lines.push(T('application-core:shell.settings.untranslatedRecords',
                         count === 1 ? '{{entity}}: {{count}} of {{total}} records has no {{language}} translation.'
                                     : '{{entity}}: {{count}} of {{total}} records have no {{language}} translation.',
                         { entity: T(entity.key, entity.label), count, total: entity.total, language: this.languageName(code) }));
                     }));
                   } else if (translatableData) {
                     const data = judged.filter(code => !provided.includes(code));
                     if (data.length > 0) {
                       lines.push(T('application-core:shell.settings.missingData', '{{app}} does not provide {{missing}} data - its lists and documents show the default-language values (for example statuses and types). Declare languages: in its intent once the data is translated.', { app, missing: data.join(', ') }));
                     }
                   }
                   return { app, lines };
                 })
                 .filter(({ lines }) => lines.length > 0);
    },
  });
}, { once: true });
