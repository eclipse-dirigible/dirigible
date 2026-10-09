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
 * printTemplates store - the tenant's print template catalogue, for the "Print Templates" section of
 * Settings (#7755).
 *
 * A document type prints with one template per language: the shipped versions a release added
 * (`standard@1.28.0`, immutable) beside the templates the tenant authored (`acme-blue`). Which one
 * prints is the tenant configuration the server names for each language (`selectionKey`); without
 * one the newest shipped version prints, and every release switches to its own. This section lets
 * the people who may change that configuration see the catalogue of each document type and
 * language, preview a template with no data, make one the active one or go back to the default,
 * duplicate a version into a tenant template, edit or delete a tenant template, and compare two.
 * Everything goes through /services/print and /services/core/configurations/tenant; the section
 * holds no rule of its own about names, versions or defaults.
 *
 * The section is offered when the caller may change the tenant configuration (the configuration
 * endpoint answers the read; a 401/403 says they may not) and some published application declares
 * a document type with print templates. The markup is ONE fragment
 * (shell/views/_print-templates.html), mounted by the application shells' Settings and by every
 * generated application shell through x-html, like the Users section. The mounts use only
 * `visible`, `markup` and `load()`. Shells generated before this store existed never load it;
 * shared code that reaches for it must check Alpine.store('printTemplates') first.
 */
document.addEventListener('alpine:init', () => {
  const PRINT_URL = '/services/print';
  const CONFIGURATION_URL = '/services/core/configurations/tenant';
  const MARKUP_URL = '/services/web/application-core/shell/views/_print-templates.html';
  const DIFF_URL = '/webjars/diff/dist/diff.min.js';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  /** A tenant template name: the catalogue's own rule, repeated here only to refuse before the round trip. */
  const NAME = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;
  /** The choice (entity|language) the shown catalogue was read for - outside the store, so reading it tracks nothing. */
  let loadedFor = '';
  /** The choice being read right now, so the effect does not start the same read twice. */
  let loadingFor = '';
  let diffLoader = null;

  Alpine.store('printTemplates', {
    /** The document types with print templates: [{ entity }]. */
    documentTypes: [],
    /** The chosen document type. */
    entity: '',
    /** The chosen document type's languages: [{ code, name, selectionKey }]. */
    languages: [],
    /** The chosen language code. */
    language: '',
    /** The catalogue of the choice: the shipped versions newest first, then the tenant templates. */
    templates: [],
    /** Whether the tenant stored a selection for the choice - "Use default" clears it. */
    selectionStored: false,
    /** Whether the caller may change the tenant configuration; null until known. */
    canManage: null,
    loaded: false,
    loading: false,
    busy: false,
    /** The last refusal, in the user's words; empty when there is none. */
    error: '',
    /** The tenant template being edited: { name, source }; null when the editor is closed. */
    editor: null,
    /** The comparison being shown: { left, right, lines: [{ kind, text }] }; null when closed. */
    diff: null,
    /** The template the duplicate dialog copies; null when closed. */
    duplicateSource: null,
    duplicateName: '',
    /** The tenant template the delete dialog asks about; null when closed. */
    removeTarget: null,
    /** The fragment the section renders from. */
    markup: '',
    /** A load asked for before the context arrived - e.g. the page opened on the section's route. */
    loadRequested: false,

    /** The section is offered to those who may change the tenant configuration, once there is a catalogue. */
    get visible() {
      return !!this.canManage && this.documentTypes.length > 0;
    },

    /** The tenant configuration key that selects the active template of the choice - the server names it. */
    get selectionKey() {
      const language = this.languages.find((each) => each.code === this.language);
      return language ? language.selectionKey : '';
    },

    /** The templates ticked for a comparison. */
    get comparing() {
      return this.templates.filter((template) => template.compare);
    },

    init() {
      this.loadContext();
      // The choice drives the list: a change of document type re-reads its languages, a change of
      // language re-reads the catalogue. Read as an effect so the selects can simply bind x-model.
      Alpine.effect(() => {
        const choice = this.entity + '|' + this.language;
        if (this.loaded && choice !== loadedFor && choice !== loadingFor) this.choiceChanged();
      });
    },

    /** Reads what decides whether the section is offered: the document types, and whether the caller may select. */
    async loadContext() {
      try {
        const [types, configuration] = await Promise.all([
          fetch(PRINT_URL + '/document-types', { headers: HEADERS, credentials: 'same-origin' }),
          fetch(CONFIGURATION_URL, { headers: HEADERS, credentials: 'same-origin' }),
        ]);
        this.canManage = configuration.ok;
        this.documentTypes = types.ok ? await types.json() : [];
        if (this.visible) this.loadMarkup();
        if (this.visible && this.loadRequested) this.load();
      } catch (e) {
        console.error('printTemplates: could not read the document types', e);
        this.canManage = false;
      }
    },

    async loadMarkup() {
      if (this.markup) return;
      try {
        const response = await fetch(MARKUP_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('printTemplates: could not load the section', e);
      }
    },

    /** Reads the catalogue - called when the section is opened, and by Refresh. */
    async load() {
      if (this.canManage === null) {
        this.loadRequested = true;
        return;
      }
      this.loadRequested = false;
      if (!this.visible) return;
      this.loadMarkup();
      this.error = '';
      if (!this.documentTypes.some((type) => type.entity === this.entity)) {
        this.entity = this.documentTypes[0].entity;
      }
      this.loaded = true;
      loadedFor = '';
      await this.choiceChanged();
    },

    /** Re-reads the document types too - a module published meanwhile may have added one. */
    async refresh() {
      const types = await this.read(PRINT_URL + '/document-types');
      if (types) this.documentTypes = types;
      await this.load();
    },

    /** Re-reads the languages when the document type changed, then the catalogue of the choice. */
    async choiceChanged() {
      const entity = this.entity;
      loadingFor = entity + '|' + this.language;
      const [loadedEntity] = loadedFor.split('|');
      if (entity !== loadedEntity) {
        this.languages = [];
        this.templates = [];
        this.closeEditor();
        this.closeDiff();
        const languages = await this.read(PRINT_URL + '/' + encodeURIComponent(entity) + '/languages');
        if (entity !== this.entity) return;
        this.languages = languages || [];
        if (!this.languages.some((each) => each.code === this.language)) {
          this.language = this.languages.length ? this.languages[0].code : '';
        }
      }
      loadingFor = loadedFor = entity + '|' + this.language;
      await this.loadTemplates();
    },

    /** Reads the catalogue of the choice, and whether the tenant stored a selection for it. */
    async loadTemplates() {
      const choice = loadedFor;
      if (!this.entity || !this.language) {
        this.templates = [];
        this.selectionStored = false;
        return;
      }
      this.loading = true;
      try {
        const [templates, configuration] = await Promise.all([
          this.read(this.templatesUrl() + '?' + this.lang() + '&details=true'),
          this.read(CONFIGURATION_URL),
        ]);
        if (choice !== loadedFor) return;
        const kept = new Set(this.comparing.map((template) => template.name));
        this.templates = (templates || []).map((template) => ({ ...template, compare: kept.has(template.name) }));
        const key = this.selectionKey;
        this.selectionStored = (configuration || []).some((each) => each.key === key && each.value);
      } finally {
        if (choice === loadedFor) this.loading = false;
      }
    },

    // ---- the commands ----

    /** Renders the template with an empty record in a new tab, so the layout shows with its labels and no data. */
    async preview(template) {
      // The tab is opened now, while the click is still the user's gesture: opened after the
      // asynchronous render, the browser's popup blocker would swallow it.
      const tab = window.open('', '_blank');
      this.error = '';
      try {
        const url = PRINT_URL + '/' + encodeURIComponent(this.entity) + '?' + this.lang() + '&template=' + encodeURIComponent(template.name);
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Accept': 'application/pdf', 'X-Requested-With': 'XMLHttpRequest' },
          credentials: 'same-origin',
          body: JSON.stringify({ document: {}, items: [] }),
        });
        if (!response.ok) {
          if (tab) tab.close();
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return;
        }
        const pdf = URL.createObjectURL(await response.blob());
        if (tab) {
          tab.location.href = pdf;
          return;
        }
        // Popups are blocked outright: download the preview instead, which is always allowed.
        const link = document.createElement('a');
        link.href = pdf;
        link.download = template.name + '.pdf';
        document.body.appendChild(link);
        link.click();
        link.remove();
      } catch (e) {
        if (tab) tab.close();
        console.error('printTemplates: could not preview the template', e);
        this.error = this.messageFor(0, null);
      }
    },

    /** Makes the template the one that prints: the selection is a tenant configuration. */
    async setActive(template) {
      await this.command(() => fetch(CONFIGURATION_URL, {
        method: 'PUT',
        headers: { ...HEADERS, 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ key: this.selectionKey, value: template.name }),
      }), this.t('shell.printTemplates.activated', '{{name}} now prints', { name: template.name }));
    },

    /** Clears the selection: the newest shipped version prints, and every release switches to its own. */
    async useDefault() {
      await this.command(() => fetch(CONFIGURATION_URL + '?key=' + encodeURIComponent(this.selectionKey), {
        method: 'DELETE',
        headers: HEADERS,
        credentials: 'same-origin',
      }), this.t('shell.printTemplates.defaultRestored', 'The default template prints again'));
    },

    openDuplicate(template) {
      this.duplicateSource = template;
      this.duplicateName = '';
      this.error = '';
    },

    closeDuplicate() {
      this.duplicateSource = null;
    },

    /** Whether the typed name may be a tenant template name. */
    get duplicateNameValid() {
      return NAME.test(this.duplicateName.trim());
    },

    /** Copies the template into a new tenant template that records the version it derives from. */
    async duplicate() {
      const source = this.duplicateSource;
      const name = this.duplicateName.trim();
      if (!source || !this.duplicateNameValid) return;
      const url = this.templatesUrl(source.name) + '/duplicate?' + this.lang() + '&as=' + encodeURIComponent(name);
      const done = await this.command(() => fetch(url, { method: 'POST', headers: HEADERS, credentials: 'same-origin' }),
          this.t('shell.printTemplates.duplicated', '{{name}} created', { name: name }));
      if (done) this.closeDuplicate();
    },

    /** Opens a tenant template's source for editing. */
    async openEditor(template) {
      this.closeDiff();
      this.error = '';
      const source = await this.readText(this.templatesUrl(template.name) + '?' + this.lang());
      if (source === null) return;
      this.editor = { name: template.name, source: source };
    },

    closeEditor() {
      this.editor = null;
    },

    /** Writes the edited source back. */
    async save() {
      const editor = this.editor;
      if (!editor) return;
      const done = await this.command(() => fetch(this.templatesUrl(editor.name) + '?' + this.lang(), {
        method: 'PUT',
        headers: { ...HEADERS, 'Content-Type': 'text/plain' },
        credentials: 'same-origin',
        body: editor.source,
      }), this.t('shell.printTemplates.saved', '{{name}} saved', { name: editor.name }));
      if (done) this.closeEditor();
    },

    openRemove(template) {
      this.removeTarget = template;
      this.error = '';
    },

    closeRemove() {
      this.removeTarget = null;
    },

    /** Deletes a tenant template - a shipped version cannot be deleted, and the active one is refused. */
    async remove() {
      const target = this.removeTarget;
      if (!target) return;
      const done = await this.command(() => fetch(this.templatesUrl(target.name) + '?' + this.lang(), {
        method: 'DELETE',
        headers: HEADERS,
        credentials: 'same-origin',
      }), this.t('shell.printTemplates.removed', '{{name}} deleted', { name: target.name }));
      if (done) this.closeRemove();
    },

    /** Shows the line differences between the two ticked templates. */
    async compare() {
      const [left, right] = this.comparing;
      if (!left || !right) return;
      this.closeEditor();
      this.error = '';
      try {
        const Diff = await this.loadDiff();
        const [leftSource, rightSource] = await Promise.all([
          this.readText(this.templatesUrl(left.name) + '?' + this.lang()),
          this.readText(this.templatesUrl(right.name) + '?' + this.lang()),
        ]);
        if (leftSource === null || rightSource === null) return;
        const lines = [];
        for (const part of Diff.diffLines(leftSource, rightSource)) {
          const kind = part.added ? '+' : part.removed ? '-' : ' ';
          for (const line of part.value.replace(/\r?\n$/, '').split(/\r?\n/)) {
            lines.push({ kind: kind, text: line });
          }
        }
        this.diff = { left: left.name, right: right.name, lines: lines };
      } catch (e) {
        console.error('printTemplates: could not compare the templates', e);
        this.error = this.t('shell.printTemplates.compareFailed', 'The templates could not be compared.');
      }
    },

    closeDiff() {
      this.diff = null;
    },

    // ---- plumbing ----

    /** The line-diff library, loaded on the first comparison only - no other section needs it. */
    loadDiff() {
      if (window.Diff) return Promise.resolve(window.Diff);
      if (!diffLoader) {
        diffLoader = new Promise((resolve, reject) => {
          const script = document.createElement('script');
          script.src = DIFF_URL;
          script.onload = () => (window.Diff ? resolve(window.Diff) : reject(new Error('diff.min.js loaded nothing')));
          script.onerror = () => { diffLoader = null; reject(new Error('could not load ' + DIFF_URL)); };
          document.head.appendChild(script);
        });
      }
      return diffLoader;
    },

    /** Runs one change, then re-reads the catalogue; the refusal is shown in the user's words. */
    async command(request, announcement) {
      if (this.busy) return false;
      this.busy = true;
      this.error = '';
      try {
        const response = await request();
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return false;
        }
        if (announcement) this.announce(announcement);
        await this.loadTemplates();
        return true;
      } catch (e) {
        console.error('printTemplates: the change failed', e);
        this.error = this.messageFor(0, null);
        return false;
      } finally {
        this.busy = false;
      }
    },

    /** A JSON read; a refusal is shown and null returned. */
    async read(url) {
      try {
        const response = await fetch(url, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return null;
        }
        return await response.json();
      } catch (e) {
        console.error('printTemplates: could not read ' + url, e);
        this.error = this.messageFor(0, null);
        return null;
      }
    },

    /** A plain-text read (a template's source); a refusal is shown and null returned. */
    async readText(url) {
      try {
        const response = await fetch(url, { headers: { 'Accept': 'text/plain', 'X-Requested-With': 'XMLHttpRequest' }, credentials: 'same-origin' });
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return null;
        }
        return await response.text();
      } catch (e) {
        console.error('printTemplates: could not read ' + url, e);
        this.error = this.messageFor(0, null);
        return null;
      }
    },

    /** The refusal body of a failed request, or null when there is none to read. */
    async refusalOf(response) {
      try {
        return await response.json();
      } catch (e) {
        return null;
      }
    },

    /** The refusal in the user's words: the session, the permission, else what the server said. */
    messageFor(status, refusal) {
      if (status === 401) return this.t('shell.printTemplates.error.SESSION', 'Your session has expired. Reload the page to sign in again.');
      if (status === 403) return this.t('shell.printTemplates.error.FORBIDDEN', 'You do not have permission to change the print templates.');
      if (refusal && typeof refusal.message === 'string' && refusal.message) return refusal.message;
      return this.t('shell.printTemplates.error.FAILED', 'Something went wrong. Please try again.');
    },

    templatesUrl(name) {
      const base = PRINT_URL + '/' + encodeURIComponent(this.entity) + '/templates';
      return name ? base + '/' + encodeURIComponent(name) : base;
    },

    lang() {
      return 'lang=' + encodeURIComponent(this.language);
    },

    /** The kind of a catalogue entry, in words. */
    kindLabel(template) {
      return template.kind === 'shipped' ? this.t('shell.printTemplates.shipped', 'Shipped version')
        : this.t('shell.printTemplates.tenant', 'Tenant template');
    },

    /** A tenant template can be edited and, unless it is the one that prints, deleted. */
    isTenant(template) {
      return template.kind === 'tenant';
    },

    announce(message) {
      const notifications = window.Alpine && Alpine.store('notifications');
      if (notifications && typeof notifications.announce === 'function') {
        notifications.announce({ title: message, variant: 'positive' });
      }
    },

    /** Translate a shell key; the fallback where the page carries no i18n service. */
    t(key, fallback, options) {
      if (typeof window.T === 'function') return T('application-core:' + key, fallback, options);
      return options ? fallback.replace(/\{\{(\w+)\}\}/g, (match, name) => (name in options ? options[name] : match)) : fallback;
    },
  });
}, { once: true });
