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
// Safety net for apps generated before label i18n existed: their index.html does not load
// services/i18n.js, but the SHARED views (inbox/documents/reports) now bind labels through T().
// The stub returns the English fallback (with {{name}}-style placeholders interpolated, matching
// the real translator's fallback path); i18n.js overwrites it with the real translator when loaded.
window.T = window.T || ((key, fallback, options) => {
  const text = fallback !== undefined ? fallback : key;
  if (!options) return text;
  return String(text).replace(/\{\{\s*(\w+)\s*\}\}/g, (match, name) =>
    options[name] !== undefined && options[name] !== null ? options[name] : match);
});

window.App = {
  services: {},
  routes: {},
  utils: {
    // Build the embedded create-page URL for a related entity, so an FK combobox's "Add" button can
    // open the target's OWN generated create form in an iframe dialog - even when the target lives in
    // another project (cross-model association). `embedded` is set in BOTH the search (the shell hides
    // its chrome) and the hash query (the form posts a "created" message instead of navigating away).
    //   appUrl "/services/web/customer-payments/gen/customer-payments/index.html", "CustomerPayment"
    //   -> "/services/web/customer-payments/gen/customer-payments/index.html?embedded=1#/CustomerPayment/create?embedded=1&dialog=1"
    // The generator passes the target app's index.html at its RAW gen folder. Legacy fallback: apps
    // generated before that arg existed pass a Java controller URL - rewrite it (this keeps the
    // sanitized gen folder, which only matters for hyphenated project names, but preserves behaviour).
    relatedCreateUrl(appUrl, entity) {
      if (!appUrl || !entity) return '';
      let base = appUrl;
      const apiIdx = appUrl.indexOf('/api/');
      if (apiIdx >= 0) {
        base = appUrl.substring(0, apiIdx).replace('/services/java/', '/services/web/') + '/index.html';
      }
      return base + '?embedded=1#/' + entity + '/create?embedded=1&dialog=1';
    },
  },
  config: {},

  // Master-detail registry. Each generated detail entity registers its metadata here
  // under its master entity's name; a master page renders one detail panel per entry,
  // so masters and details stay decoupled (no cross-entity generation). See
  // js/components/detailPanel.js and the master view.
  details: {},
  registerDetail(masterEntity, def) {
    (this.details[masterEntity] = this.details[masterEntity] || []).push(def);
  },
  detailsFor(masterEntity) {
    return this.details[masterEntity] || [];
  },

  // Related-records registry. An entity that declares registers of the records REFERENCING it
  // registers them here under its own name; its form / document / master pages render one
  // read-only relatedPanel per entry. Unlike a detail (whose registration is contributed by the
  // CHILD), a register is contributed by the entity being referenced - the referencing entity may
  // live in another project entirely and knows nothing about this one.
  // See js/components/relatedPanel.js.
  related: {},
  registerRelated(entity, def) {
    (this.related[entity] = this.related[entity] || []).push(def);
  },
  relatedFor(entity) {
    return this.related[entity] || [];
  },
};

/**
 * A transient message about something the user just did ("Saved"), over Harmonia's own notification
 * overlay. Deliberately NOT the notifications store's announce(): that also writes an entry into the
 * bell, which is right for the outcome of an action the user launched and wrong for an ordinary save
 * - the bell would fill up with them. Missing overlay degrades to the console.
 */
App.services.toast = function (message, variant) {
  try {
    const toasts = window.Alpine && Alpine.store('_h_notifications');
    if (toasts && typeof toasts.push === 'function') {
      toasts.push(undefined, 'toast', 'top-right', 4000, { message: message, variant: variant || 'information' });
      return;
    }
  } catch (e) { /* fall through to the console */ }
  console.log('[toast] ' + message);
};

/**
 * Unsaved-changes guard (issue #7359).
 *
 * A generated form knows when its buffer differs from what the server holds; nothing else does. So
 * the open form REGISTERS itself here while it is dirty, and every way out of it - the page's own
 * Back / Cancel, a sidebar entry, the browser's Back button, a reload, closing the tab - asks this
 * one guard first. Without it an edited header was dropped silently by any of them.
 *
 * In-app navigation is vetoed through Pinecone's own global handler: a handler that THROWS aborts
 * `navigate` before the route is rendered and before the history entry is pushed (router 7.5.2), so
 * the page the user is standing on keeps its address while the dialog is up. The browser's Back
 * button has already moved the URL by then - `restoreHash` puts it back - and `bypass` is what lets
 * the guard's own "Save and leave" / "Discard" perform the navigation it just refused.
 *
 * The page supplies both halves: `isDirty()` (is there anything to protect) and `confirmLeave(proceed,
 * intent)` (raise the dialog, call `proceed` when the user says so). See basePage.
 */
App.leaveGuard = {
  page: null,
  bypass: false,
  installed: false,
  // The full hash the guarded page lives at (query string included - Pinecone's context.path drops it).
  homeHash: '',

  register(page) {
    this.page = page;
    this.homeHash = window.location.hash || '';
  },

  release(page) {
    if (this.page === page) {
      this.page = null;
      this.homeHash = '';
    }
  },

  isDirty() {
    try {
      return !!(this.page && typeof this.page.isDirty === 'function' && this.page.isDirty());
    } catch (e) {
      // A guard that throws must never be able to trap the user on a page.
      console.error('[leaveGuard] the registered page could not report its state', e);
      return false;
    }
  },

  // Put the URL back to the guarded page's own address (the browser Back button moved it before we
  // were asked). Same-document hash change, so nothing reloads.
  restoreHash() {
    try {
      if (!this.homeHash || window.location.hash === this.homeHash) return;
      window.history.pushState({ path: this.homeHash }, '', this.homeHash);
    } catch (e) {
      console.error('[leaveGuard] could not restore the address of the open form', e);
    }
  },

  // Perform a navigation the guard itself decided to allow.
  go(path) {
    this.bypass = true;
    this.page = null;
    this.homeHash = '';
    Promise.resolve(window.PineconeRouter.navigate(path)).catch((e) => {
      console.error('[leaveGuard] navigation failed', e);
    }).then(() => { this.bypass = false; });
  },

  install() {
    if (this.installed || !window.PineconeRouter || typeof window.PineconeRouter.settings !== 'function') return;
    this.installed = true;
    const guard = this;
    window.PineconeRouter.settings({
      globalHandlers: [(context) => {
        if (guard.bypass || !guard.isDirty()) return;
        // Where the user wanted to go. The browser's Back button has already put that address in the
        // bar, query string and all - Pinecone's own context.path drops the query - so prefer it when
        // it is the same route; a programmatic navigate has not touched the bar yet.
        const shown = (window.location.hash || '').replace(/^#/, '');
        const target = shown && shown.split('?')[0] === context.path ? shown : context.path;
        guard.restoreHash();
        guard.page.confirmLeave(() => guard.go(target), 'leave');
        // Pinecone aborts the navigation when a global handler throws - this is the veto.
        throw new Error('[leaveGuard] navigation stopped: the open form has unsaved changes');
      }],
    });
    window.addEventListener('beforeunload', (event) => {
      if (!guard.isDirty()) return undefined;
      // Reload / close tab: only the browser's own generic prompt is available here.
      event.preventDefault();
      event.returnValue = '';
      return '';
    });
  },
};

document.addEventListener('alpine:init', () => App.leaveGuard.install());
