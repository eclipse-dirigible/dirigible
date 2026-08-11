/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The SQL console's state: the statement being written, the last result, and the messages strip.
 *
 * The statement is persisted to localStorage on every change. A support engineer who reloads the
 * page - or is bounced through a login - keeps what they were about to run; losing a carefully
 * built UPDATE to a stray refresh is the console's most annoying possible failure.
 */
document.addEventListener('alpine:init', () => {
  const STATEMENT_KEY = 'dirigible.database.statement';

  const read = () => {
    try { return localStorage.getItem(STATEMENT_KEY) || ''; } catch (e) { return ''; }
  };
  const write = (text) => {
    try { localStorage.setItem(STATEMENT_KEY, text); } catch (e) { /* private mode - no persistence */ }
  };

  Alpine.store('sql', {
    statement: read(),
    running: false,

    // The last result: rows for a query/procedure, an update count for everything else.
    columns: [],
    rows: [],
    updateCount: null,
    operation: null,
    error: '',

    /** Set the statement from code (the Explorer's "Generate SELECT"). */
    setStatement(text) {
      this.statement = text;
      this.persist();
    },

    /**
     * Write the current statement to storage. The textarea binds `statement` with x-model, so this
     * is called from its input handler rather than owning the value itself.
     */
    persist() {
      write(this.statement);
    },

    clear() {
      this.setStatement('');
      this.reset();
    },

    reset() {
      this.columns = [];
      this.rows = [];
      this.updateCount = null;
      this.operation = null;
      this.error = '';
    },

    /**
     * Whether the statement would go to /update - i.e. it is not a SELECT, a CALL, or an explicitly
     * prefixed read. The page asks before running one of these; that is the console's only
     * confirmation, and it exists because the shell deliberately allows writes.
     */
    get isWrite() {
      const statement = String(this.statement || '').trim();
      if (!statement) return false;
      return DatabaseOps.dispatch(statement).operation === 'update';
    },

    /**
     * Run the statement against the given datasource.
     *
     * @param {string} datasource the datasource to run against
     */
    async execute(datasource) {
      const statement = String(this.statement || '').trim();
      if (!statement || !datasource || this.running) return;

      this.running = true;
      this.reset();
      try {
        const result = await DatabaseOps.execute(datasource, statement);
        this.operation = result.operation;
        this.updateCount = result.updateCount;
        this.rows = result.rows || [];
        this.columns = this.rows.length ? Object.keys(this.rows[0]) : [];
      } catch (e) {
        // The service splits a multi-statement body and streams results per statement, so a failure
        // part-way through arrives as a 500 on a response that already carried rows. Whatever the
        // server managed to say is the most useful thing to show.
        this.error = (e && e.errorMessage) || String(e);
      } finally {
        this.running = false;
      }
    },
  });
});
