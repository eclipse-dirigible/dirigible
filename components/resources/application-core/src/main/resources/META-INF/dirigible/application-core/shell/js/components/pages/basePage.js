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
 * Adopted from codbex-athena-app (js/components/pages/basePage.js).
 *
 * basePage — mixin for every page component.
 *
 * Usage:
 *   Alpine.data('myPage', () => ({
 *     ...basePage(),
 *     // page-specific members
 *   }));
 */
function basePage() {
  return {
    // No-op: Lucide icons now render via the x-h-lucide directive (harmonia-lucide bundle), which
    // upgrades icons on init and inside dynamically loaded views - no manual createIcons scan needed.
    refreshIcons() {},

    /**
     * Re-read this component's data whenever a custom action finishes.
     *
     * The customActions store raises `harmonia:action-done` after every action it runs (a transition,
     * a create-from, a page action that closed) - and until #7073 nothing listened to it, so a
     * Record Reminder that had already created its row left the panel showing "No records" until the
     * user reloaded the page by hand. An action changes server-side state the open page is a view of;
     * the page has to go and look again.
     *
     * `handler` is the component's own reload. Registered once, per component instance, and removed
     * in destroy() below - a page navigated away from must not keep reloading in the background.
     */
    onActionDone(handler) {
      this._actionDoneHandlers = this._actionDoneHandlers || [];
      const listener = () => {
        try {
          const done = handler.call(this);
          if (done && typeof done.catch === 'function') done.catch((e) => console.error('[action-done] reload failed', e));
        } catch (e) {
          console.error('[action-done] reload failed', e);
        }
      };
      this._actionDoneHandlers.push(listener);
      window.addEventListener('harmonia:action-done', listener);
    },

    /**
     * Alpine calls this when the component's element goes away. A component that overrides destroy()
     * must call basePage's (or drop its own action-done listeners itself).
     */
    destroy() {
      (this._actionDoneHandlers || []).forEach((listener) => window.removeEventListener('harmonia:action-done', listener));
      this._actionDoneHandlers = [];
      // The two files are cached by the browser independently, so a page may be running this one
      // against a previous app.js - which must not turn every destroy into an exception.
      if (App.leaveGuard) App.leaveGuard.release(this);
    },

    // ===== Unsaved changes (issue #7359) =========================================================
    // A form page takes a snapshot of what it loaded (markPristine) and is DIRTY while its buffer
    // differs from it. Every exit - the page's own Back / Cancel, a sidebar entry, the browser's
    // Back button, a reload - then goes through guardExit / App.leaveGuard instead of dropping the
    // edit silently. A page that never calls markPristine (a list, a report) is never dirty and
    // everything here stays inert.
    //
    // The comparison is over the SAVE PAYLOAD, not `form`: toPayload already normalizes dates,
    // dropdown ids to strings and a multiselect to its csv, so a value that only LOOKS different
    // (3 vs "3") does not read as an edit.

    // The payload as it was last known to be saved, serialized. null = nothing to protect.
    pristine: null,
    // The guard dialog's own state. `leaveIntent` is 'leave' (an exit) or 'continue' (the user is
    // staying on the page but starting something that would save behind the dirty header).
    leaveOpen: false,
    leaveIntent: 'leave',
    leaveBusy: false,
    leaveProceed: null,

    dirtySnapshot() {
      try {
        return JSON.stringify(typeof this.toPayload === 'function' ? this.toPayload() : this.form);
      } catch (e) {
        console.error('[leaveGuard] could not snapshot the form', e);
        return null;
      }
    },

    // Declare the current buffer saved. Called after a load and after every successful save; the page
    // registers with the shared guard from here, so the guard is armed exactly while a form is open.
    markPristine() {
      this.pristine = this.dirtySnapshot();
      if (App.leaveGuard) App.leaveGuard.register(this);
    },

    // Stop guarding (the page is about to navigate itself after a successful write, or the user
    // discarded). Not a getter - see the note at the top of baseFormPage about spread and getters.
    clearPristine() {
      this.pristine = null;
      if (App.leaveGuard) App.leaveGuard.release(this);
    },

    isDirty() {
      if (this.pristine === null) return false;
      // A read-only surface cannot be edited, so it can never be dirty: preview, and a document the
      // immutability pre-check reported closed (its inputs are disabled).
      if (this.isPreview === true || this.mutable === false) return false;
      const snapshot = this.dirtySnapshot();
      // A snapshot that could not be taken says nothing about the buffer - reporting it as an edit
      // would put the dialog in front of every exit for the rest of the session.
      return snapshot !== null && snapshot !== this.pristine;
    },

    /**
     * Run `proceed` unless the form has unsaved changes - then ask first. `intent` is 'leave' (the
     * default) or 'continue' for an action that stays on the page (opening the line-item dialog,
     * whose save would otherwise commit a line under a header the server has never seen).
     */
    guardExit(proceed, intent) {
      if (!this.isDirty()) {
        proceed();
        return;
      }
      this.confirmLeave(proceed, intent);
    },

    /** Raise the guard dialog. Also the entry point the shared router guard calls. */
    confirmLeave(proceed, intent) {
      this.leaveProceed = proceed;
      this.leaveIntent = intent || 'leave';
      this.leaveOpen = true;
    },

    leaveKeepEditing() {
      this.leaveOpen = false;
      this.leaveProceed = null;
    },

    /**
     * Discard: leave without saving. On an exit there is nothing to restore - the page is going away
     * - so the snapshot is simply dropped; when the user is STAYING ('continue') the buffer is put
     * back to what the server holds, which is what makes the line save honest again.
     */
    async leaveDiscard() {
      const proceed = this.leaveProceed;
      this.leaveOpen = false;
      this.leaveProceed = null;
      if (this.leaveIntent === 'continue' && typeof this.reloadForm === 'function') {
        this.leaveBusy = true;
        try {
          await this.reloadForm();
        } catch (e) {
          console.error('[leaveGuard] could not restore the form', e);
        } finally {
          this.leaveBusy = false;
        }
      } else {
        this.clearPristine();
      }
      if (proceed) proceed();
    },

    /**
     * Save, then do what the user was trying to do. A refused save (a validation error, a 400/409 the
     * server raised) keeps the user on the page with the usual summary - the dialog closes, because
     * the message it hides is the answer to the question it asked.
     */
    async leaveSave() {
      const proceed = this.leaveProceed;
      this.leaveBusy = true;
      try {
        await this.save();
      } catch (e) {
        console.error('[leaveGuard] save failed', e);
      } finally {
        this.leaveBusy = false;
      }
      this.leaveOpen = false;
      this.leaveProceed = null;
      if (this.isDirty()) return;   // refused: the page reports why, and stays
      if (proceed) proceed();
    },

    /**
     * Open a calendar that is SCOPED BY the record in front of us (intent `calendar.scope`): the
     * calendar entity's own page, filtered to this record through the query parameter its scope
     * foreign key reads - the same URL the calendar page itself builds when it navigates to create.
     * Without this the filter was reachable only by typing the URL. Same application always: the
     * scope target's pages and the calendar's are generated from one model.
     */
    openScopedCalendar(entity, scopeProperty, id) {
      if (id === null || id === undefined || id === '') return;
      window.PineconeRouter.navigate('/' + entity + '?' + scopeProperty + '=' + encodeURIComponent(id));
    },

    /**
     * Role-scoped fields (intent `visibleTo:`) this caller may not see. The SERVER decides: it strips
     * those properties from every response and ignores them on writes, and its /restricted endpoint
     * says which ones they are for the caller in front of us. The page asks once and leaves the
     * matching columns and inputs out, so the user never sees a permanently empty control instead of
     * being told nothing about a field that is not theirs.
     *
     * Only pages that HAVE such a field call load - the endpoint is generated only for those entities.
     */
    restrictedFields: [],

    /** Whether a property may be rendered for this caller. */
    canSee(name) {
      return !this.restrictedFields.includes(name);
    },

    /**
     * Ask the entity's controller which fields it withholds from this caller. Failure leaves every
     * field visible: the values are stripped server-side either way, so the worst case is an empty
     * column the user can see is empty - never a field silently hidden from someone entitled to it.
     */
    async loadRestrictedFields(apiPath) {
      const path = apiPath || this.apiPath;
      if (!path) return;
      try {
        const answer = await App.services.api.get(path + '/restricted');
        this.restrictedFields = (answer && answer.restricted) || [];
      } catch (e) {
        console.error('[restricted] could not read the field visibility of ' + path, e);
      }
    },

    /**
     * Whether a date picker's `change` event carries a value the picker actually PARSED.
     *
     * The picker listens on its own input, so its handler has already run by the time the event
     * bubbles to a consumer's handler on the wrapper. When the typed text is not a date the picker
     * logs it, marks the input with a custom validity message and returns WITHOUT touching the
     * model - but the trusted `change` event still arrives here. A handler that acts unconditionally
     * therefore acts on the PREVIOUS model value: a half-typed date silently re-applied the filter
     * that was already in force, which reads as if the half-typed one had matched.
     *
     * A successful parse, a pick from the popup and an emptied input all clear that message, so the
     * input's own validity is the discriminator - `false` here means "the picker did not take this,
     * do nothing". The user is not left guessing: Harmonia paints a `user-invalid` picker input with
     * the negative border, so the rejected text is visibly marked where it was typed.
     */
    datePickerAccepted(event) {
      const input = event && event.target;
      return !input || !input.validity || input.validity.valid;
    },

    /**
     * Format a floating-point value for display: decimals from the field's DecimalFormat pattern, the
     * grouping/decimal separators from the instance-wide Number setting (services/format.js).
     */
    formatNumber(value, pattern) {
      return window.HarmoniaFormat.number(value, pattern);
    },

    /**
     * Translate a column header: columns carry an optional i18next key (tkey) next to the
     * design-time label, exactly like the table headers render them.
     */
    columnHeader(col) {
      return (window.T && col.tkey) ? window.T(col.tkey, col.label) : col.label;
    },

    /**
     * Download rows as a CSV file. Values come from cellText(row, col) - the SAME resolver the
     * table cells use, so FK columns carry their referenced labels and dates their formatted
     * form, never raw ids or serialized arrays. The BOM makes Excel decode UTF-8 (Cyrillic
     * included) without an import wizard.
     */
    exportRowsCsv(rows, columns, cellText, filename) {
      const esc = (v) => {
        const s = String(v == null ? '' : v);
        return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
      };
      const head = columns.map((c) => esc(this.columnHeader(c))).join(',');
      const lines = rows.map((r) => columns.map((c) => esc(cellText(r, c))).join(','));
      const blob = new Blob(['\ufeff' + [head].concat(lines).join('\r\n')], { type: 'text/csv;charset=utf-8' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    },

    /**
     * Print rows as a minimal table document in a new window (the browser dialog covers paper and
     * Save as PDF). Same data path as the CSV export: the full filtered set through cellText.
     */
    printRows(rows, columns, cellText, title) {
      const esc = (v) => String(v == null ? '' : v)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      const th = columns.map((c) =>
        '<th class="' + (c.number ? 'text-right' : '') + '">' + esc(this.columnHeader(c)) + '</th>').join('');
      const body = rows.map((r) => '<tr>' + columns.map((c) =>
        '<td class="' + (c.number ? 'text-right' : '') + '">' + esc(cellText(r, c)) + '</td>').join('') + '</tr>').join('');
      // Fixed table layout + column-count font scaling so a wide table fits the page instead of
      // overflowing and being clipped at the right edge (mirrors the server-side XslFoRenderer PDF
      // path). Long unbreakable tokens (ids, IBANs) wrap inside the fixed cell.
      const fontSize = Math.max(6, 10 - Math.max(0, columns.length - 3));
      const html = '<!doctype html><html><head><title>' + esc(title) + '</title><style>'
        + 'body{font-family:system-ui,-apple-system,sans-serif;margin:24px;color:#111}'
        + 'h1{font-size:18px;margin:0 0 4px}'
        + '.meta{font-size:11px;color:#555;margin:0 0 16px}'
        + 'table{border-collapse:collapse;width:100%;table-layout:fixed;font-size:' + fontSize + 'pt}'
        + 'th,td{border:1px solid #999;padding:2pt;text-align:left;overflow-wrap:anywhere;word-break:break-word}'
        + 'th{background:#eee}.text-right{text-align:right}'
        + '</style></head><body><h1>' + esc(title) + '</h1>'
        + '<p class="meta">' + rows.length + ' rows - ' + esc(HarmoniaFormat.value(new Date(), true)) + '</p>'
        + '<table><thead><tr>' + th + '</tr></thead><tbody>' + body + '</tbody></table></body></html>';
      const w = window.open('', '_blank');
      if (!w) return;
      w.document.write(html);
      w.document.close();
      w.focus();
      w.print();
    },
  };
}
