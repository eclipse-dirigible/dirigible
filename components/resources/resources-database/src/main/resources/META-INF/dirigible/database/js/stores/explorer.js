/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Explorer's state: which datasource is selected, the lazily-filled metadata tree, and whatever
 * the right-hand pane is currently showing (a table's contents or an object's DDL).
 *
 * The tree is loaded a level at a time, on first expand, and cached on the node - a schema with a
 * few thousand tables is not something to read up front, and the metadata endpoints are per-object
 * anyway. `loaded` distinguishes "not fetched yet" from "fetched and genuinely empty", which is why
 * an empty children array is not enough on its own.
 *
 * The store holds data and performs async work; it never touches the DOM. Formatting and derived
 * view state live on the page component (js/components/pages/explorerPage.js).
 */
document.addEventListener('alpine:init', () => {
  // Survives a reload the way the IDE's explorer does, so a support engineer returns to the
  // database they were looking at.
  const SELECTED_KEY = 'dirigible.database.datasource';

  const readSelected = () => {
    try { return localStorage.getItem(SELECTED_KEY); } catch (e) { return null; }
  };
  const writeSelected = (name) => {
    try { localStorage.setItem(SELECTED_KEY, name); } catch (e) { /* private mode - selection is per-session then */ }
  };

  Alpine.store('explorer', {
    datasources: [],
    datasource: null,
    schemas: [],
    loading: false,
    errors: {},

    // The right-hand pane: 'contents' | 'ddl' | null, plus what it is showing.
    pane: null,
    subject: null,          // { schema, name, kind }
    columns: [],
    rows: [],
    rowColumns: [],
    ddl: '',
    paneLoading: false,
    paneError: '',

    /** Load the datasource list and select one - the remembered choice when it still exists. */
    async load() {
      this.loading = true;
      this.errors = {};
      const names = await DatabaseOps.soft('datasources', this.errors, () => DatabaseOps.datasources(), []);
      this.datasources = Array.isArray(names) ? names : [];
      this.loading = false;

      if (!this.datasources.length) return;
      const remembered = readSelected();
      await this.select(this.datasources.includes(remembered) ? remembered : this.datasources[0]);
    },

    /** Switch datasource: remember it, drop the old tree, read the new one's schemas. */
    async select(name) {
      if (!name) return;
      this.datasource = name;
      await this.onDatasourceChanged();
    },

    /**
     * React to `datasource` having been set. The picker is an x-h-select whose x-model writes the
     * value straight onto the store, so the change arrives already applied - this is what runs after
     * it, and what `select()` delegates to when the change comes from code instead.
     */
    async onDatasourceChanged() {
      if (!this.datasource) return;
      writeSelected(this.datasource);
      this.closePane();
      await this.loadSchemas();
    },

    async loadSchemas() {
      this.loading = true;
      this.errors = {};
      const metadata = await DatabaseOps.soft(
        'schemas', this.errors, () => DatabaseOps.datasourceMetadata(this.datasource), null);
      this.schemas = ((metadata && metadata.schemas) || []).map((schema) => ({
        name: schema.name,
        loaded: false,
        loading: false,
        groups: []
      }));
      this.loading = false;
    },

    /**
     * Read a schema's objects, once. Called when the tree activates the schema's row.
     *
     * Expansion itself is the tree component's business, not the store's - an item detects its own
     * children and owns its expanded state, so a second owner here would only fight it. The view
     * keeps a placeholder child under an unloaded schema so the row is expandable from the start,
     * which is what makes this the only thing left to do on activation.
     *
     * The objects are grouped by kind rather than listed flat: the endpoint returns them in separate
     * fields anyway, and a schema's tables are what someone is looking for far more often than its
     * procedures.
     */
    async loadSchema(schema) {
      if (schema.loaded || schema.loading) return;

      schema.loading = true;
      const metadata = await DatabaseOps.soft(
        'schema:' + schema.name, this.errors,
        () => DatabaseOps.schemaMetadata(this.datasource, schema.name), null);

      const group = (label, kind, items) => ({
        label,
        kind,
        objects: (items || []).map((item) => ({
          name: item.name,
          kind,
          schema: schema.name,
          loaded: false,
          loading: false,
          columns: []
        }))
      });

      schema.groups = [
        group('Tables', 'TABLE', metadata && metadata.tables),
        group('Views', 'VIEW', metadata && metadata.views),
        group('Procedures', 'PROCEDURE', metadata && metadata.procedures)
      ].filter((g) => g.objects.length);

      schema.loaded = true;
      schema.loading = false;
    },

    /** Read an object's columns, once. Called when the tree activates the object's row. */
    async loadObject(object) {
      if (object.loaded || object.loading) return;

      object.loading = true;
      const metadata = await DatabaseOps.soft(
        'object:' + object.schema + '.' + object.name, this.errors,
        () => DatabaseOps.structureMetadata(this.datasource, object.schema, object.name, object.kind), null);
      object.columns = (metadata && metadata.columns) || [];
      object.loaded = true;
      object.loading = false;
    },

    /**
     * Show a table's or view's contents. The generated SELECT is capped server-side (see
     * showContents' caller for how that is surfaced), which is why the statement carries no LIMIT
     * of its own - adding one would make the shell's number disagree with the server's.
     */
    async showContents(object) {
      this.pane = 'contents';
      this.subject = { schema: object.schema, name: object.name, kind: object.kind };
      this.paneLoading = true;
      this.paneError = '';
      this.rows = [];
      this.rowColumns = [];

      try {
        const result = await DatabaseOps.execute(
          this.datasource, DatabaseOps.selectAll(object.schema, object.name));
        this.rows = result.rows || [];
        this.rowColumns = this.rows.length ? Object.keys(this.rows[0]) : [];
      } catch (e) {
        this.paneError = (e && e.errorMessage) || String(e);
      } finally {
        this.paneLoading = false;
      }
    },

    /** Show an object's DDL. */
    async showDefinition(object) {
      this.pane = 'ddl';
      this.subject = { schema: object.schema, name: object.name, kind: object.kind };
      this.paneLoading = true;
      this.paneError = '';
      this.ddl = '';

      try {
        const definition = await DatabaseOps.definition(this.datasource, object.schema, object.name);
        this.ddl = typeof definition === 'string' ? definition : JSON.stringify(definition, null, 2);
      } catch (e) {
        this.paneError = (e && e.errorMessage) || String(e);
      } finally {
        this.paneLoading = false;
      }
    },

    closePane() {
      this.pane = null;
      this.subject = null;
      this.rows = [];
      this.rowColumns = [];
      this.ddl = '';
      this.paneError = '';
    },

    /** Drop the server's metadata cache and re-read the tree, so a just-created table shows up. */
    async refresh() {
      await DatabaseOps.soft('invalidate', this.errors, () => DatabaseOps.invalidateCache(), null);
      this.closePane();
      await this.loadSchemas();
    },
  });
});
