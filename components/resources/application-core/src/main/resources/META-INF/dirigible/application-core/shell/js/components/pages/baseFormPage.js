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
 * Adopted from codbex-athena-app (js/components/pages/baseFormPage.js).
 *
 * baseFormPage — mixin for form page components. Extends basePage — spread this
 * instead of basePage in form pages.
 *
 * Provides: errors, saveFailureMessage, loadFailureMessage, unmatchedCauses,
 *           fieldMap, clearError, scrollToSummary, navigateBack, refreshIcons,
 *           applyApiError, mapCauses, applyLoadError, inlineCreated,
 *           adoptCreatedOption, rememberInlineCreated, savedAlongside.
 *
 * NOTE: Do NOT put ES6 getters in this object — the spread operator invokes
 * them at spread-time and copies the result as a plain value, breaking Alpine
 * reactivity. Define getters (e.g. errorCount) directly on the page component.
 */
function baseFormPage() {
  return {
    ...basePage(),

    errors: {},
    saveFailureMessage: '',
    loadFailureMessage: '',
    // 422 causes that could not be matched to a form field. Surfaced in the
    // summary so they are never silently dropped.
    unmatchedCauses: [],
    // Per-page override: maps a backend errorCauses[].field name to this page's
    // `errors` key when they differ.
    fieldMap: {},
    // Per-page override: maps a field name to its display label, used to render a server
    // rejection that names the property ("The 'TaxRate' property is required") in the user's
    // own vocabulary. Unmapped names are shown as the server wrote them.
    fieldLabels: {},

    clearError(field) {
      if (this.errors[field]) this.errors[field] = '';
      if (this.errors.__summary) this.errors.__summary = '';
      this.unmatchedCauses = [];
    },

    /**
     * Central catch handler for save()/submit. Selects UX by errorType:
     *  - ValidationError (422) -> map causes onto field errors + summary
     *  - everything else       -> generic save-failure banner (localized)
     * Tolerates non-ApiError values.
     */
    applyApiError(err, { fallbackMessage, formState } = {}) {
      if (err && err.isApiError && err.errorType === 'ValidationError') {
        this.mapCauses(err.errorCauses);
        this.state = 'validation-error';
        this.scrollToSummary();
        return;
      }
      // A plain 400 that NAMES a field of this form is a per-field rejection the generated
      // controllers state in prose instead of structured causes. Answering it with the generic
      // banner points at nothing (and leaves whatever field happened to be marked, marked), so
      // map it onto that field and say what the server said.
      const named = App.services.apiErrors.namedProperty(err, Object.keys(this.form || {}));
      if (named) {
        const message = App.services.apiErrors.messageWithLabels(err, this.fieldLabels);
        this.errors = { [named]: message, __summary: message };
        this.state = 'validation-error';
        this.scrollToSummary();
        return;
      }
      this.saveFailureMessage = App.services.apiErrors.messageFor(err, fallbackMessage);
      this.state = formState || 'save-failure';
      this.scrollToSummary();
    },

    /** Build this.errors (+ summary) from 422 errorCauses, honoring fieldMap. */
    mapCauses(causes) {
      const apiErrors = App.services.apiErrors;
      const errors = {};
      const unmatched = [];
      for (const cause of (causes || [])) {
        let key = null;
        if (cause.field && Object.prototype.hasOwnProperty.call(this.fieldMap, cause.field)) {
          key = this.fieldMap[cause.field];                 // deliberate mapping
        } else if (cause.field && Object.prototype.hasOwnProperty.call(this.form, cause.field)) {
          key = cause.field;                                // direct 1:1 form field
        }
        if (key) {
          errors[key] = apiErrors.fieldMessageFor(cause);
        } else {
          unmatched.push(cause);
        }
      }
      this.unmatchedCauses = unmatched;
      if (unmatched.length) {
        errors.__summary = `${unmatched.length} additional ${unmatched.length === 1 ? 'error' : 'errors'} occurred.`;
      }
      this.errors = errors;
    },

    /** Catch handler for edit-page init() load failures -> load-failure page. */
    applyLoadError(err, { fallbackMessage } = {}) {
      this.loadFailureMessage = App.services.apiErrors.messageFor(err, fallbackMessage);
      this.state = 'load-failure';
    },

    scrollToSummary() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            const container = document.getElementById('app');
            if (container) container.scrollTop = 0;
          });
        });
      });
    },

    // Raw query string from the current URL. With hash routing the query lives in the
    // hash (e.g. #/Detail/create?Customer=3&returnTo=/Customer), not location.search.
    queryString() {
      if (window.location.hash.includes('?')) return window.location.hash.split('?')[1];
      if (window.location.search) return window.location.search.slice(1);
      return '';
    },

    // A single query param value, or null.
    queryParam(name) {
      try {
        return new URLSearchParams(this.queryString()).get(name);
      } catch (_) {
        return null;
      }
    },

    // A relationship control is CONTEXT-LOCKED when the URL that opened this form already names it:
    // the record is being created or edited from inside that parent (a master's detail panel, a
    // create-from flow), so the parent is implied by where the user is standing rather than chosen
    // here. The generated form then renders the referenced record's label read-only instead of a
    // dropdown that would let the record be re-pointed mid-flow. A form opened with no such param
    // (the entity's own top-level create) keeps free selection. UI only - the payload is unchanged
    // and the controller stays authoritative.
    isContextLocked(name) {
      const v = this.queryParam(name);
      return v !== null && v !== '';
    },

    // The label of a context-locked relation: the referenced record's display value out of the
    // option list the form already loaded, falling back to the raw id while the options are in
    // flight (or when the referenced row is outside the loaded set).
    contextLabel(value, options) {
      if (value === null || value === undefined || value === '') return '';
      const opt = (options || []).find(o => String(o.value) === String(value));
      return opt ? opt.text : String(value);
    },

    // Read a validated `returnTo` from the current URL (must be an in-app path).
    returnToParam() {
      const ret = this.queryParam('returnTo');
      return ret && ret.startsWith('/') ? ret : '';
    },

    navigateBack(defaultRoute) {
      window.PineconeRouter.navigate(this.returnToParam() || defaultRoute);
    },

    // Hosted without its own chrome (the shell-hosted app OR an FK "Add" dialog both pass ?embedded).
    // Controls whether this form renders its own toolbar - NOT whether save navigates.
    get isEmbedded() {
      return this.queryParam('embedded') === '1' || this.queryParam('embedded') === 'true';
    },

    // Hosted specifically inside an FK combobox's "Add" iframe dialog (opened with ?dialog=1). Only in
    // this case does a create report the new record to the opener instead of navigating to the list.
    get isDialog() {
      return this.queryParam('dialog') === '1' || this.queryParam('dialog') === 'true';
    },

    // Tell the hosting window a record was created so the opener can refresh + select it.
    emitCreated(id) {
      try {
        if (window.parent && window.parent !== window) {
          window.parent.postMessage({ type: 'harmonia.entity.created', id: id }, '*');
        }
      } catch (e) { /* cross-origin / standalone: nothing to notify */ }
    },

    // Tell the hosting dialog an EDIT saved (a detail-panel dialog's update) so it can close and
    // reload the panel - navigating this iframe to a list nobody sees would strand the dialog open.
    emitSaved(id) {
      try {
        if (window.parent && window.parent !== window) {
          window.parent.postMessage({ type: 'harmonia.entity.updated', id: id }, '*');
        }
      } catch (e) { /* cross-origin / standalone: nothing to notify */ }
    },

    // Rows the user created through a picker's inline "New <entity>" dialog since this form last
    // saved (#7725). The child commits on its own, before the parent, so a parent save that fails
    // afterwards leaves the child behind; savedAlongside() names it in the error so the user
    // corrects this form instead of creating the child a second time. A page clears it once its
    // own write lands.
    inlineCreated: [],

    // After an inline create: make sure the picker lists the new row, and return its option. The
    // reloaded list is the picker's NARROWED one (a `where:` filter, a Depends-On cascade, a
    // `pickable:` rule that hides, a leaf-only hierarchy), and a new row outside it matched no
    // option - the id was assigned but the select read as empty. The row is spliced in by id, as
    // ensureFilteredCurrent does for a stored off-filter value; the server stays the gate.
    async adoptCreatedOption(options, id, lookup) {
      if (!Array.isArray(options) || id == null) return null;
      const existing = options.find(o => String(o.value) === String(id));
      if (existing) return existing;
      if (!lookup || !lookup.url) return null;
      try {
        const row = await App.services.api.get(lookup.url + '/' + encodeURIComponent(id), { baseUrl: '' });
        if (row && row[lookup.key] != null) {
          const option = { value: row[lookup.key], text: row[lookup.text] };
          options.unshift(option);
          return option;
        }
      } catch (e) {
        console.error('[baseFormPage] failed to resolve the inline-created option ' + id, e);
      }
      return null;
    },

    // Record an inline-created row in `created` (this.inlineCreated, or a dialog's own list) under
    // the label the picker shows for it.
    rememberInlineCreated(created, entity, id, option) {
      const text = option && option.text != null && option.text !== '' ? String(option.text) : '#' + id;
      created.push({ entity: entity, text: text });
    },

    // The sentence a failed save adds to its error when inline-created rows are already saved:
    // two independent commits must not read as one failure, or the user creates the child again.
    savedAlongside(created) {
      if (!created || !created.length) return '';
      const saved = created.map(c => c.entity + " '" + c.text + "'").join(', ');
      return created.length === 1
        ? T('application-core:shell.related.savedOne', '{{saved}} was saved; this record was not. It stays selected - correct this record and save again instead of creating it anew.', { saved })
        : T('application-core:shell.related.savedMany', '{{saved}} were saved; this record was not. They stay selected - correct this record and save again instead of creating them anew.', { saved });
    },

    // Ask the hosting dialog to close (embedded Cancel) instead of navigating this iframe to a list.
    emitClose() {
      try {
        if (window.parent && window.parent !== window) {
          window.parent.postMessage({ type: 'harmonia.dialog.cancel' }, '*');
        }
      } catch (e) { /* standalone: nothing to close */ }
    },
  };
}
