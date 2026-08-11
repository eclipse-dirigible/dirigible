/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Explorer page: the metadata tree on the left, the selected object's contents or DDL on the
 * right. The store owns the data and the actions; this component only formats what they hold and
 * carries the two row actions that reach outside the page (copy, and hand a SELECT to the console).
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('explorerPage', () => ({
    copied: '',

    init() {
      // Only the first visit pays for the datasource list; coming back from the console keeps it.
      if (!this.$store.explorer.datasources.length) this.$store.explorer.load();
    },

    get state() {
      return this.$store.explorer;
    },

    /** The lucide icon for a tree node kind. */
    iconFor(kind) {
      switch (kind) {
        case 'TABLE': return 'table';
        case 'VIEW': return 'eye';
        case 'PROCEDURE': return 'square-function';
        default: return 'file-text';
      }
    },

    /** Contents is a read of rows - it means nothing for a procedure. */
    canShowContents(object) {
      return object.kind === 'TABLE' || object.kind === 'VIEW';
    },

    /** A column's type with its size, the way a DDL would write it. */
    columnType(column) {
      if (!column) return '';
      const size = column.size !== undefined && column.size !== null && column.size !== 0
        ? '(' + column.size + ')' : '';
      return String(column.type || '') + size;
    },

    /** A cell as text. NULL is shown as such - an empty cell would read as an empty string. */
    cellText(value) {
      if (value === null || value === undefined) return 'NULL';
      if (typeof value === 'object') return JSON.stringify(value);
      return String(value);
    },

    /**
     * The results are capped server-side with nothing in the payload to say so, so the grid says it
     * itself rather than presenting a truncated read as the whole table.
     */
    get contentsCapped() {
      return DatabaseOps.looksCapped(this.state.rows.length);
    },

    qualifiedName(subject) {
      return subject ? subject.schema + '.' + subject.name : '';
    },

    async copyName(object) {
      const name = object.schema + '.' + object.name;
      try {
        await navigator.clipboard.writeText(name);
        this.copied = name;
        setTimeout(() => { if (this.copied === name) this.copied = ''; }, 2000);
      } catch (e) {
        console.error('database: could not copy to the clipboard', e);
      }
    },

    /** Put a SELECT for this object into the console and go there. */
    selectInConsole(object) {
      this.$store.sql.setStatement(DatabaseOps.selectAll(object.schema, object.name));
      window.PineconeRouter.navigate('/sql');
    },
  }));
});
