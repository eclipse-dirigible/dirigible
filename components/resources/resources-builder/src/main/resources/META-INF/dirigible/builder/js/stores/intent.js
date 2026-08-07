/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The working model: the current app.intent buffer, its parsed model, and the workspace project the
 * two are persisted into. Project and file management are deliberately INVISIBLE to the user - the
 * project is created lazily on the first accepted proposal and every accepted proposal is saved, so
 * reloading the shell restores the app from disk rather than from browser memory.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('intent', {
    yaml: '',
    model: null,
    /** The technical project id (a slug of the app name). Empty until the first proposal lands. */
    project: '',
    /** Known builder apps (workspace projects carrying a root intent file), for the app switcher. */
    apps: [],
    busy: false,
    /** Set when the last save failed, so the shell can say so instead of silently losing work. */
    saveError: '',

    get hasIntent() { return !!this.model; },

    /** The app's display name: the intent's own `name`, falling back to the project id. */
    get appName() {
      return (this.model && this.model.name) || this.project || '';
    },

    /**
     * The canvas summary chips. Only non-empty groups are returned, so a small intent shows a small
     * strip instead of a row of zeros.
     */
    get counts() {
      const m = this.model;
      if (!m) return [];
      return [
        { key: 'entities', label: 'Entities', value: (m.entities || []).length },
        { key: 'processes', label: 'Processes', value: (m.processes || []).length },
        { key: 'forms', label: 'Forms', value: (m.forms || []).length },
        { key: 'reports', label: 'Reports', value: (m.reports || []).length },
        { key: 'roles', label: 'Roles', value: (m.permissions || []).length },
        { key: 'seeds', label: 'Seed data', value: (m.seeds || []).length },
      ].filter(c => c.value > 0);
    },

    /**
     * A workspace-project id derived from the app name. The project name is fixed at creation and
     * deliberately NOT renamed when the app is later renamed - a rename would mean delete, recreate
     * and republish, which is churn the user never asked for.
     */
    slug(name) {
      const slug = String(name || '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
      return slug || 'app';
    },

    /** Load the list of builder apps for the switcher. Never throws - the switcher just stays empty. */
    async loadApps() {
      try {
        this.apps = await App.services.intentApi.listIntentProjects();
      } catch (e) {
        console.error('builder: could not list the workspace apps', e);
        this.apps = [];
      }
    },

    /**
     * Adopt a validated proposal: it becomes the buffer and the rendered model, the project is
     * created if this is the first proposal, and the intent is saved. Returns false when persisting
     * failed - the buffer still updates, so the conversation can continue and Publish will retry.
     */
    async apply(yaml, model) {
      this.yaml = yaml;
      this.model = model;
      if (!this.project) this.project = this.slug(model && model.name);
      return await this.save();
    },

    /** Persist the buffer into the workspace project (creating the project when it is not there yet). */
    async save() {
      if (!this.project) return true;
      this.saveError = '';
      try {
        await App.services.intentApi.ensureProject(this.project);
        await App.services.intentApi.writeFile(this.project, App.config.intentFile, this.yaml);
        if (!this.apps.includes(this.project)) this.apps = [...this.apps, this.project].sort((a, b) => a.localeCompare(b));
        return true;
      } catch (e) {
        console.error('builder: could not save the intent', e);
        this.saveError = 'The app could not be saved to the workspace.';
        return false;
      }
    },

    /** Open an existing builder app: read its intent from disk, validate it and render it. */
    async open(project) {
      this.busy = true;
      try {
        const yaml = await App.services.intentApi.readFile(project, App.config.intentFile);
        if (yaml === null) return false;
        this.project = project;
        this.yaml = yaml;
        // A stored intent that no longer validates still opens - the conversation is how it gets
        // fixed, so the canvas shows what it can rather than refusing to load the app at all.
        try {
          this.model = await App.services.intentApi.parse(yaml);
        } catch (e) {
          this.model = null;
        }
        return true;
      } catch (e) {
        console.error('builder: could not open the app', e);
        return false;
      } finally {
        this.busy = false;
      }
    },

    /** Start a fresh app. Nothing is written until the first proposal is accepted. */
    reset() {
      this.yaml = '';
      this.model = null;
      this.project = '';
      this.saveError = '';
    },
  });
}, { once: true });
