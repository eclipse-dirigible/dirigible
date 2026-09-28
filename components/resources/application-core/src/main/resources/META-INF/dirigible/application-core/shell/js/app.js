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
 * A transient message about something the user just did ("Saved"), through the notifications store's
 * toast() - the public $notifications magic the shell attached, never Harmonia's private store.
 * Deliberately NOT announce(): that also writes an entry into the bell, which is right for the
 * outcome of an action the user launched and wrong for an ordinary save - the bell would fill up
 * with them. A missing store or overlay degrades to the console.
 */
App.services.toast = function (message, variant) {
  const store = window.Alpine && Alpine.store('notifications');
  if (store && typeof store.toast === 'function') {
    store.toast(message, variant);
    return;
  }
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

/**
 * List column widths (issue #7467).
 *
 * A generated list used the browser's automatic table layout, so every column was as wide as its
 * longest value: one long company name or address reflowed the whole table and pushed the columns
 * after it out of view. `x-list-columns` on a list's <table> switches it to the decided policy:
 *
 *   - FIXED widths. The columns are sized from their kind (a number column narrower than a text one,
 *     the trailing row-actions column narrow) and the space the list has - never from the values in
 *     it - so loading, paging, sorting or filtering never moves a column.
 *   - TRUNCATION with a tooltip. A value that does not fit ends in an ellipsis; hovering the cell shows
 *     the whole value (a native title, set only while the text is actually cut).
 *   - DRAG to resize. Each header has a grip on its right edge, a focusable separator operated like
 *     Harmonia's Split gutter: ArrowLeft / ArrowRight by 10 px, with Shift by 100 px. Widths are NOT
 *     persisted in v1: a reload starts from the defaults again.
 *
 * Until the user resizes a column, the widths are refitted to the list's width whenever it changes
 * (window resize, split-panel drag, a role-gated column appearing), with each kind's minimum as the
 * floor: at roughly 960 px or 150% zoom the list scrolls sideways instead of squeezing columns to
 * nothing. Once the user has dragged a column, the widths are theirs and stay put.
 *
 * Only the first header row sizes the table (the manage list's filter row below it is not truncated,
 * so its date pickers and dropdowns are never clipped); a cell marked `data-col-free` (the "no data"
 * message) and the row-actions cell keep their overflow.
 *
 * Deliberately NOT Harmonia's `data-fixed`: its table reference declares it incompatible with the
 * scroll container (`x-h-table-container.scroll`) most lists sit in - Harmonia's table is `w-full`,
 * so a fixed layout alone can never grow past the container and the columns would only squeeze. The
 * policy sets table-layout: fixed in css/app.css TOGETHER with an explicit table width (the sum of
 * the columns), which is what lets a fixed-layout list scroll sideways.
 *
 * The tooltip is the native `title`, not `x-h-tooltip`: Harmonia's tooltip is an absolutely
 * positioned sibling of its trigger, so inside a truncating (overflow: hidden) cell it is clipped,
 * and it has no "only while the text is cut" mode - it would need a trigger and a tooltip element
 * in every cell of every row. The full value also stays one click away in the record's own form.
 */
App.listColumns = {
  // Default and minimum widths per column kind, in rem so they follow the user's zoom/font size.
  TEXT_REM: 12,
  NUMBER_REM: 8,
  ACTIONS_REM: 3.5,
  MIN_REM: 3,
  // Keyboard steps, as Harmonia's Split gutter (3.2.0): an arrow moves 10 px, Shift + arrow 100 px.
  KEY_STEP_PX: 10,
  KEY_STEP_LARGE_PX: 100,

  rem() {
    return parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
  },

  // The header cells that size the table: the first header row's.
  heads(table) {
    const row = table.tHead && table.tHead.rows[0];
    return row ? Array.from(row.cells) : [];
  },

  // ...of which the ones on screen (a role-gated column may be hidden by x-show).
  visibleHeads(table) {
    return this.heads(table).filter((th) => getComputedStyle(th).display !== 'none');
  },

  // Assign only on change: the header is observed, and a no-op write must not schedule another pass.
  setWidth(el, px) {
    const value = px + 'px';
    if (el.style.width !== value) el.style.width = value;
  },

  kind(th) {
    if (th.hasAttribute('data-col-actions') || (!th.textContent.trim() && !th.querySelector('input,select,button'))) return 'actions';
    return th.classList.contains('text-right') ? 'number' : 'text';
  },

  baseWidth(th) {
    const kind = this.kind(th);
    const rem = kind === 'actions' ? this.ACTIONS_REM : kind === 'number' ? this.NUMBER_REM : this.TEXT_REM;
    return Math.round(rem * this.rem());
  },

  // Size every visible column: its kind's default, stretched evenly (actions column excepted) to fill
  // the container when there is room; never below the default, so a narrow list scrolls.
  fit(table, container) {
    const heads = this.visibleHeads(table);
    if (!heads.length) return;
    const bases = heads.map((th) => this.baseWidth(th));
    const flexible = heads.map((th) => this.kind(th) !== 'actions');
    const total = bases.reduce((a, b) => a + b, 0);
    const flexTotal = bases.reduce((a, b, i) => a + (flexible[i] ? b : 0), 0);
    const avail = container ? container.clientWidth : 0;
    const factor = flexTotal > 0 && avail > total ? (avail - (total - flexTotal)) / flexTotal : 1;
    let sum = 0;
    const widths = bases.map((b, i) => {
      const w = flexible[i] ? Math.floor(b * factor) : b;
      sum += w;
      return w;
    });
    // Rounding remainder to the last flexible column, so the table meets the container edge exactly.
    if (factor > 1) {
      const last = flexible.lastIndexOf(true);
      if (last >= 0) {
        widths[last] += Math.max(0, avail - sum);
        sum = avail;
      }
    }
    heads.forEach((th, i) => this.setWidth(th, widths[i]));
    this.setWidth(table, sum);
  },

  // After a user resize: the table is exactly as wide as its columns (so a drag moves only the
  // edge being dragged - Harmonia's w-full would otherwise re-spread the slack over all of them).
  sync(table) {
    const sum = this.visibleHeads(table).reduce((a, th) => a + (parseFloat(th.style.width) || th.offsetWidth), 0);
    this.setWidth(table, Math.round(sum));
  },

  addGrip(th, state) {
    if (th.querySelector(':scope > [data-col-grip]')) return;
    if (getComputedStyle(th).position === 'static') th.style.position = 'relative';
    const grip = document.createElement('span');
    grip.setAttribute('data-col-grip', '');
    grip.setAttribute('role', 'separator');
    grip.setAttribute('aria-orientation', 'vertical');
    grip.setAttribute('tabindex', '0');
    grip.setAttribute('aria-label', window.T('application-core:shell.list.resizeColumn', 'Resize column'));
    const min = () => Math.round(this.MIN_REM * this.rem());
    // A focusable separator is a widget: it announces the size it controls (here in pixels).
    const announce = () => {
      grip.setAttribute('aria-valuemin', String(min()));
      grip.setAttribute('aria-valuenow', String(Math.round(th.getBoundingClientRect().width)));
    };
    const resize = (width) => {
      state.userSized = true;
      this.setWidth(th, Math.max(min(), Math.round(width)));
      this.sync(state.table);
      announce();
    };
    grip.addEventListener('focus', announce);
    grip.addEventListener('pointerdown', (event) => {
      event.preventDefault();
      event.stopPropagation();
      const startX = event.clientX;
      const startWidth = th.getBoundingClientRect().width;
      grip.setPointerCapture(event.pointerId);
      grip.setAttribute('data-dragging', 'true');
      const move = (e) => resize(startWidth + (e.clientX - startX));
      const up = () => {
        grip.removeAttribute('data-dragging');
        grip.removeEventListener('pointermove', move);
        grip.removeEventListener('pointerup', up);
        grip.removeEventListener('pointercancel', up);
      };
      grip.addEventListener('pointermove', move);
      grip.addEventListener('pointerup', up);
      grip.addEventListener('pointercancel', up);
    });
    // The header itself sorts on click: a click that ends a drag must not reach it.
    grip.addEventListener('click', (event) => event.stopPropagation());
    grip.addEventListener('keydown', (event) => {
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
      event.preventDefault();
      event.stopPropagation();
      const width = th.getBoundingClientRect().width;
      const step = event.shiftKey ? this.KEY_STEP_LARGE_PX : this.KEY_STEP_PX;
      resize(width + (event.key === 'ArrowRight' ? step : -step));
    });
    th.appendChild(grip);
  },

  // Is the element's text wider than its content box? scrollWidth alone rounds a sub-pixel overflow
  // away, and the browser still draws the ellipsis for it - so measure the text itself.
  isCut(el) {
    if (el.scrollWidth > el.clientWidth) return true;
    const range = document.createRange();
    range.selectNodeContents(el);
    const style = getComputedStyle(el);
    const content = el.clientWidth - (parseFloat(style.paddingLeft) || 0) - (parseFloat(style.paddingRight) || 0);
    return range.getBoundingClientRect().width > content + 0.05;
  },

  // Tooltip only while the value is actually cut; the cell (or a header's label) is what truncates.
  tooltip(event) {
    const cell = event.target.closest && event.target.closest('td, th');
    if (!cell) return;
    const label = cell.tagName === 'TH' ? cell.querySelector('[data-col-label], span') : null;
    const target = label || cell;
    const cut = this.isCut(target);
    if (cut) {
      target.setAttribute('title', target.textContent.trim());
      target.setAttribute('data-col-title', '');
    } else if (target.hasAttribute('data-col-title')) {
      target.removeAttribute('title');
      target.removeAttribute('data-col-title');
    }
  },

  attach(table) {
    const container = table.closest('[data-slot="table"]:not(table)') || table.parentElement;
    const state = { table, userSized: false, scheduled: false };
    table.setAttribute('data-list-columns', '');
    const layout = () => {
      state.scheduled = false;
      this.heads(table).forEach((th) => this.addGrip(th, state));
      if (state.userSized) {
        // A column that appeared after the user resized others gets its default, not zero.
        this.heads(table).forEach((th) => { if (!th.style.width) this.setWidth(th, this.baseWidth(th)); });
        this.sync(table);
      } else {
        this.fit(table, container);
      }
    };
    const schedule = () => {
      if (state.scheduled) return;
      state.scheduled = true;
      requestAnimationFrame(layout);
    };
    // Header cells rendered by x-for (the self-service lists) or shown by a role gate arrive later.
    const observer = new MutationObserver(schedule);
    if (table.tHead) observer.observe(table.tHead, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] });
    const resizeObserver = typeof ResizeObserver === 'function' ? new ResizeObserver(schedule) : null;
    if (resizeObserver && container) resizeObserver.observe(container);
    const onOver = (event) => this.tooltip(event);
    table.addEventListener('mouseover', onOver);
    schedule();
    return () => {
      observer.disconnect();
      if (resizeObserver) resizeObserver.disconnect();
      table.removeEventListener('mouseover', onOver);
    };
  },
};

document.addEventListener('alpine:init', () => {
  Alpine.directive('list-columns', (el, directive, { cleanup }) => {
    if (el.tagName !== 'TABLE') return;
    cleanup(App.listColumns.attach(el));
  });
});
