/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Database shell's access layer: one thin wrapper per platform endpoint under /services/data/,
 * so no page ever spells out a URL, encodes a name, or knows how a statement is dispatched.
 *
 * Two things live here and nowhere else, because getting either wrong is silent rather than loud:
 *
 * 1. NAME ENCODING. Datasource and schema names go into the path as ordinary segments and are
 *    encoded per segment; a STRUCTURE name is additionally Base64-encoded, which is what the
 *    metadata and definition endpoints expect. Encoding the whole path in one go turns a '/' into
 *    %2F and every request 400s, and splitting a qualified name on '.' (which the IDE's explorer
 *    does) breaks any schema or table whose name contains a dot.
 *
 * 2. STATEMENT DISPATCH. The server has no single "run this" endpoint - /execute is just /query
 *    under another name - so the client decides which endpoint a statement goes to, by sniffing its
 *    first word. This mirrors the IDE's result.js exactly, so a support engineer's muscle memory
 *    (including the `query:` / `update:` prefixes, which exist for NoSQL datasources) carries over.
 *
 * Every read goes through the shared fetch client with `{ baseUrl: '' }` - these are absolute
 * platform paths, so nothing must be prepended. Execution cannot: the endpoints consume text/plain
 * and take the raw SQL as the body, while the shared client always sends JSON. postSql() below is
 * the deliberate exception, and it reproduces the client's ApiError contract so callers handle
 * failures the same way either side of that line.
 *
 * The endpoints are role-gated to ADMINISTRATOR / DEVELOPER / OPERATOR and so is the shell
 * (database.access), so a user who can reach a page can call what it reads.
 */
window.DatabaseOps = (() => {
  const ABSOLUTE = { baseUrl: '' };
  const METADATA = '/services/data/metadata';
  const DEFINITION = '/services/data/definition';

  const get = (url) => App.services.api.get(url, ABSOLUTE);

  /** One path segment, encoded. Never call this on a whole path - it would encode the separators. */
  const segment = (value) => encodeURIComponent(value);

  /**
   * A structure name as the metadata/definition endpoints want it: Base64 of the raw name, then
   * URL-encoded because Base64's alphabet includes '+' and '/'. btoa() only accepts Latin-1, so a
   * non-ASCII identifier is UTF-8 encoded first.
   */
  const structureSegment = (name) =>
    encodeURIComponent(btoa(String.fromCharCode(...new TextEncoder().encode(name))));

  /**
   * POST raw SQL to one of the execution endpoints. The shared client cannot be used: it sets
   * Content-Type: application/json and JSON-encodes the body, while these endpoints declare
   * `consumes = "text/plain"` and read the body as the statement itself.
   *
   * Accept matters too. The endpoint branches on an EXACT header match - 'text/plain' renders a
   * monospaced table and 'text/csv' renders CSV - so anything else takes the JSON branch. We ask
   * for application/json explicitly and always parse JSON.
   *
   * @param {string} datasource the datasource name
   * @param {string} operation query | update | procedure
   * @param {string} sql the statement, already stripped of any dispatch prefix
   * @returns {Promise<*>} the parsed JSON payload (or the raw text when the body is not JSON)
   */
  async function postSql(datasource, operation, sql) {
    const url = '/services/data/' + segment(datasource) + '/' + operation;
    let response;
    try {
      response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
          'Accept': 'application/json',
          'X-Requested-With': 'XMLHttpRequest'
        },
        body: sql,
        credentials: 'same-origin'
      });
    } catch (e) {
      throw new App.services.ApiError({ httpStatus: 0, errorType: 'NetworkError', errorMessage: e.message });
    }

    const text = await response.text().catch(() => '');
    if (!response.ok) {
      let parsed = null;
      if (text) {
        try { parsed = JSON.parse(text); } catch (_) { /* the server often answers plain text here */ }
      }
      throw new App.services.ApiError({
        httpStatus: response.status,
        errorType: (parsed && parsed.errorType) || App.services.api.typeFromStatus(response.status),
        errorMessage: (parsed && (parsed.errorMessage || parsed.message)) || text || response.statusText
      });
    }
    if (!text) return null;
    try { return JSON.parse(text); } catch (_) { return text; }
  }

  return {
    /** The datasource names configured on this instance. */
    datasources: () => get(METADATA + '/'),

    /** One datasource's metadata: `{ name, kind, schemas: [{ name, tables, views, procedures }] }`. */
    datasourceMetadata: (datasource) => get(METADATA + '/' + segment(datasource)),

    /** One schema's objects, without their columns. */
    schemaMetadata: (datasource, schema) =>
      get(METADATA + '/' + segment(datasource) + '/' + segment(schema)),

    /**
     * One object's own metadata - `{ name, type, columns: [{ name, type, size, nullable, key }] }`.
     *
     * @param {string} datasource the datasource
     * @param {string} schema the schema
     * @param {string} structure the raw object name (Base64-encoded here, not by the caller)
     * @param {string} kind TABLE | VIEW | PROCEDURE - the endpoint needs it to pick its reader
     */
    structureMetadata: (datasource, schema, structure, kind) =>
      get(METADATA + '/' + segment(datasource) + '/' + segment(schema) + '/' + structureSegment(structure)
        + '?kind=' + segment(String(kind).toUpperCase())),

    /** The object's DDL, as text. */
    definition: (datasource, schema, structure) =>
      get(DEFINITION + '/' + segment(datasource) + '/' + segment(schema) + '/' + structureSegment(structure)),

    /** Drop the server-side metadata cache, so the next read re-reads the live database. */
    invalidateCache: () => get(METADATA + '/invalidate-cache'),

    /**
     * Decide which endpoint a statement belongs to, from its first word. Returns
     * `{ operation, sql }` where `sql` is the statement with any dispatch prefix removed.
     *
     * The `query:` / `update:` prefixes are how a NoSQL datasource (e.g. MongoDB) is addressed:
     * the body after the prefix is passed through verbatim rather than treated as SQL. Anything
     * that is not a SELECT, a CALL, or an explicitly prefixed statement goes to /update - which is
     * also where DDL and every other write ends up.
     *
     * @param {string} statement the raw statement text
     */
    dispatch(statement) {
      const text = String(statement || '').trim();
      const lower = text.toLowerCase();
      if (lower.startsWith('select')) return { operation: 'query', sql: text };
      if (lower.startsWith('call')) return { operation: 'procedure', sql: text };
      if (lower.startsWith('query: ')) return { operation: 'query', sql: text.substring(7).trim() };
      if (lower.startsWith('update: ')) return { operation: 'update', sql: text.substring(8).trim() };
      return { operation: 'update', sql: text };
    },

    /**
     * Run a statement against a datasource, dispatching it as `dispatch()` decides.
     *
     * A procedure's payload is DOUBLE-encoded - a JSON array whose elements are themselves JSON
     * strings - so each element is parsed again here; callers see the same row-array shape a query
     * returns, and never learn which endpoint answered.
     *
     * @param {string} datasource the datasource
     * @param {string} statement the raw statement text
     * @returns {Promise<{operation: string, rows: object[]|null, updateCount: number|null}>}
     */
    async execute(datasource, statement) {
      const { operation, sql } = this.dispatch(statement);
      const payload = await postSql(datasource, operation, sql);

      if (operation === 'procedure') {
        const results = Array.isArray(payload) ? payload : [];
        const rows = results.flatMap((result) => {
          if (typeof result !== 'string') return Array.isArray(result) ? result : [];
          try {
            const parsed = JSON.parse(result);
            return Array.isArray(parsed) ? parsed : [parsed];
          } catch (_) {
            return [];
          }
        });
        return { operation, rows, updateCount: null };
      }

      if (operation === 'query') {
        return { operation, rows: Array.isArray(payload) ? payload : [], updateCount: null };
      }

      // /update answers with the affected-row count. It arrives as a number, or as text when the
      // statement was DDL and the driver reported nothing meaningful.
      const count = typeof payload === 'number' ? payload : Number.parseInt(payload, 10);
      return { operation, rows: null, updateCount: Number.isFinite(count) ? count : null };
    },

    /**
     * Whether a result of this size looks like it hit the server's row cap.
     *
     * The server caps every read at DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT (1000 by default) and
     * puts NOTHING in the payload to say it did, so a truncated read is indistinguishable from a
     * complete one. The client cannot know the configured value either, so this is a heuristic: a
     * round hundred is what a cap looks like and an arbitrary table almost never is. It over-warns
     * on a table that genuinely holds a round number of rows, and that is the right way round -
     * presenting a truncated read as the whole table is the failure that matters.
     *
     * @param {number} count the number of rows returned
     */
    looksCapped: (count) => count >= 100 && count % 100 === 0,

    /**
     * The SELECT the Explorer's "Show contents" runs. The identifiers are double-quoted so a
     * lower-case or reserved-word name survives; a name containing a double quote would be invalid
     * in every dialect we support, so it is not something to escape around.
     *
     * @param {string} schema the schema
     * @param {string} table the table or view
     */
    selectAll: (schema, table) => 'SELECT * FROM "' + schema + '"."' + table + '"',

    /**
     * Run a read that must not take the page down with it. One unavailable datasource (a database
     * that is down, a driver that refuses a metadata call) degrades to a marked-unavailable entry
     * instead of an empty screen.
     *
     * @param {string} name the source name, used as the key under which the failure is recorded
     * @param {object} errors the accumulator receiving { <name>: <message> } for failed reads
     * @param {function} read the call to make
     * @param {*} fallback the value to return when the read fails
     */
    async soft(name, errors, read, fallback) {
      try {
        return await read();
      } catch (e) {
        errors[name] = e && e.httpStatus === 403 ? 'forbidden' : 'unavailable';
        console.error('database: could not read ' + name, e);
        return fallback;
      }
    },
  };
})();
