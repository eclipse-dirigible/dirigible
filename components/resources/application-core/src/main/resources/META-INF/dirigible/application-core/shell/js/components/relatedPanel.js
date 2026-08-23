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
 * relatedPanel — read-only register of the records REFERENCING the open record.
 *
 * A record page renders one of these per registered register (App.relatedFor(<entity>)), passing
 * the register definition and the open record's id. It lists the referencing entity's rows filtered
 * to that id, and each row opens the SOURCE's own record page in the shared record dialog.
 *
 * It is a window, never an owner: the listed records have their own lifecycle, pages and processes,
 * so there is no add, no edit and no delete here. That is the whole difference from detailPanel,
 * whose composition children are edited in place — the table rendering, the foreign-key label
 * resolution and the cell formatting are the SAME, and are inherited from it rather than restated.
 *
 * `def` shape (from App.registerRelated): { entity, label, tkey, apiPath, appUrl, local, fkProperty,
 *   primaryKey, columns: [{ name, label, tkey, number, float, pattern, date, lookup, multi, sensitive }] }.
 * A `multi` column is a subset (`kind: subset`): its cell is a key list resolved through `lookup` per
 * key — handled by the inherited detailPanel.cellValue, so nothing here is aware of it.
 * apiPath and appUrl are ABSOLUTE (the source may live in another project), so every call passes
 * { baseUrl: '' } and the row dialog opens the source's own application.
 */
function relatedPanel(def, recordId) {
  return {
    // The table, lookup and formatting behaviour of a detail panel; the mutating members it also
    // carries are unreachable — no register markup calls them.
    ...detailPanel(def, recordId),

    // The register's heading. A same-project source carries the catalog key of its own navigation
    // label; a cross-project one has none (its catalog belongs to its application) and shows the
    // label the model declares.
    get title() {
      return (window.T && this.def.tkey) ? T(this.def.tkey, this.def.label) : this.def.label;
    },

    // The source's own controller has no master-filter query parameter (that exists only for a
    // composition detail), so the register filters through the generic search endpoint every
    // generated controller exposes: an equality on the foreign key pointing back at this record.
    async load() {
      if (this.masterId == null || !this.def.fkProperty) { this.rows = []; this.state = 'empty'; return; }
      this.state = 'loading';
      this.error = null;
      try {
        const filter = { equals: { [this.def.fkProperty]: this.masterId } };
        this.rows = await App.services.api.post(this.def.apiPath + '/search', filter, { baseUrl: '' });
        this.state = this.rows.length === 0 ? 'empty' : 'default';
      } catch (e) {
        this.error = App.services.apiErrors.messageFor(e, 'Could not load ' + this.def.label + '.');
        this.state = 'error';
      }
      this.refreshIcons();
    },

    // Open the clicked record on its OWN page, read-only, in the shared record dialog — never a
    // main-pane navigation: this page may hold unsaved edits, and the source generally belongs to
    // another application whose routes this shell does not own. A same-project register opens this
    // very app (its pathname), a cross-project one the source app's index.html.
    openRow(row) {
      const app = this.def.local ? window.location.pathname : this.def.appUrl;
      const id = encodeURIComponent(row[this.def.primaryKey]);
      Alpine.store('related').create(
        app + '?embedded=1#/' + this.def.entity + '/' + id + '/preview?embedded=1&dialog=1', this.title);
    },
  };
}
