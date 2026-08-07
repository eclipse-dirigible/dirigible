/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * State behind the Processes page: the instance list (running or completed), the selected
 * instance's detail, and the two management actions this shell allows - retry and skip a stuck
 * step. Starting instances, editing variables and claiming other people's tasks stay in the IDE.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('processes', {
    /** 'active' shows running instances, 'historic' the completed ones (read-only). */
    mode: 'active',
    filter: '',
    loading: false,
    instances: [],
    error: null,

    selectedId: '',
    selected: null,
    detailLoading: false,
    variables: [],
    tasks: [],
    incidents: [],
    activities: {},
    /** Set while a retry/skip is in flight, so the buttons cannot be double-fired. */
    acting: false,
    /** The outcome of the last retry/skip, shown next to the incident it acted on. */
    actionResult: null,

    /** Load the list for the current mode, keeping the selection if it is still there. */
    async load() {
      this.loading = true;
      this.error = null;
      const ops = window.MonitoringOps;
      try {
        this.instances = await (this.mode === 'active' ? ops.processInstances() : ops.historicProcessInstances());
        if (this.selectedId && !this.instances.some((instance) => instance.id === this.selectedId)) {
          this.clearSelection();
        }
      } catch (e) {
        this.instances = [];
        this.error = e;
        console.error('monitoring: could not load the process instances', e);
      } finally {
        this.loading = false;
      }
    },

    setMode(mode) {
      if (this.mode === mode) return;
      this.mode = mode;
      this.clearSelection();
      this.load();
    },

    /** The instances matching the search box - definition, business key or instance id. */
    get visibleInstances() {
      const needle = this.filter.trim()
                         .toLowerCase();
      if (!needle) return this.instances;
      return this.instances.filter((instance) => [instance.processDefinitionName, instance.processDefinitionKey,
        instance.businessKey, instance.id].some((value) => (value || '').toLowerCase()
                                                                        .includes(needle)));
    },

    clearSelection() {
      this.selectedId = '';
      this.selected = null;
      this.variables = [];
      this.tasks = [];
      this.incidents = [];
      this.activities = {};
      this.actionResult = null;
    },

    /** Select an instance and load everything its detail pane shows. */
    async select(instance) {
      this.selectedId = instance.id;
      this.selected = instance;
      this.actionResult = null;
      await this.loadDetail();
    },

    async loadDetail() {
      if (!this.selected) return;
      this.detailLoading = true;
      const ops = window.MonitoringOps;
      const id = this.selectedId;
      const errors = {};
      try {
        // A completed instance has no live variables, tasks, dead-letter jobs or active activities -
        // only the variables it ended with.
        if (this.mode === 'historic') {
          this.variables = await ops.soft('variables', errors, () => ops.historicInstanceVariables(id), []);
          this.tasks = [];
          this.incidents = [];
          this.activities = {};
        } else {
          const [variables, tasks, incidents, activities] = await Promise.all([
            ops.soft('variables', errors, () => ops.instanceVariables(id), []),
            ops.soft('tasks', errors, () => ops.instanceTasks(id), []),
            ops.soft('incidents', errors, () => ops.deadLetterJobs(id), []),
            ops.soft('activities', errors, () => ops.instanceActivities(id), {}),
          ]);
          this.variables = variables;
          this.tasks = tasks;
          this.incidents = incidents;
          this.activities = activities;
        }
      } finally {
        this.detailLoading = false;
      }
    },

    /**
     * Retry the failed step, or skip it and carry on. Both act on the instance's dead-letter job;
     * the list is reloaded afterwards, because a successful retry usually clears the incident.
     *
     * @param {string} action RETRY or SKIP
     */
    async act(action) {
      if (!this.selectedId || this.acting) return;
      this.acting = true;
      this.actionResult = null;
      try {
        await window.MonitoringOps.instanceAction(this.selectedId, action);
        this.actionResult = { variant: 'positive',
          text: T('monitoring:processes.actionDone', 'The failed step was resubmitted.') };
        // The instance may have moved on or completed, so both the detail and the alarm count change.
        await this.loadDetail();
        Alpine.store('overview').refresh();
      } catch (e) {
        console.error('monitoring: ' + action + ' failed for instance ' + this.selectedId, e);
        this.actionResult = { variant: 'negative',
          text: (e && e.errorMessage) || T('monitoring:processes.actionFailed', 'The action failed.') };
      } finally {
        this.acting = false;
      }
    },
  });
});
