/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The SQL console page: a statement area, a results grid and a messages strip.
 *
 * A plain textarea, not Monaco. A support console is opened to run a handful of statements, and an
 * editor of that size is not worth loading into an application-layer shell for it; Ctrl/Cmd+Enter
 * and the persisted buffer are what carries over from the IDE, not the syntax highlighting.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('sqlPage', () => ({
    confirmOpen: false,

    init() {
      // The console runs against the Explorer's datasource, so the list has to be there even when
      // the console is the first page opened (a deep link, or a reload on /sql).
      if (!this.$store.explorer.datasources.length) this.$store.explorer.load();
    },

    get state() {
      return this.$store.sql;
    },

    get datasource() {
      return this.$store.explorer.datasource;
    },

    /**
     * Run, asking first when the statement is not a read. The shell deliberately allows writes -
     * a support tool that cannot fix data is a dashboard - so the safeguard is this one prompt plus
     * the role gate, not a hidden button.
     */
    run() {
      if (this.state.isWrite) {
        this.confirmOpen = true;
        return;
      }
      this.state.execute(this.datasource);
    },

    confirmRun() {
      this.confirmOpen = false;
      this.state.execute(this.datasource);
    },

    /** Ctrl+Enter / Cmd+Enter runs, matching the IDE console. */
    onKeydown(event) {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        this.run();
      }
    },

    cellText(value) {
      if (value === null || value === undefined) return 'NULL';
      if (typeof value === 'object') return JSON.stringify(value);
      return String(value);
    },

    /** See DatabaseOps.looksCapped - the server caps a read and says nothing about it. */
    get capped() {
      return DatabaseOps.looksCapped(this.state.rows.length);
    },

    /** What the messages strip says once a statement has run. */
    get message() {
      if (this.state.error) return null;
      if (this.state.operation === null) return null;
      if (this.state.updateCount !== null) {
        return T('database:sql.rowsUpdated', 'Rows updated: {{count}}', { count: this.state.updateCount });
      }
      if (this.state.operation === 'update') {
        return T('database:sql.statementExecuted', 'The statement was executed.');
      }
      return T('database:sql.rowsReturned', 'Rows: {{count}}', { count: this.state.rows.length });
    },
  }));
});
