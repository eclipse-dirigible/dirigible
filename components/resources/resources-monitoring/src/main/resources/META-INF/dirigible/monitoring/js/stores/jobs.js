/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * State behind the Jobs page: the scheduled jobs with their last outcome, the selected job's
 * execution log, and the three management actions this shell allows - enable, disable and trigger.
 * E-mail assignment and log clearing stay in the Workbench.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('jobs', {
    loading: false,
    items: [],
    error: null,

    selectedName: '',
    logs: [],
    logsLoading: false,

    /** The trigger dialog: the selected job's declared parameters, edited before running it. */
    triggerOpen: false,
    triggerJobName: '',
    triggerParameters: [],
    triggering: false,

    /** The outcome of the last action, shown in the toolbar. */
    actionResult: null,

    async load() {
      this.loading = true;
      this.error = null;
      try {
        this.items = await window.MonitoringOps.jobs();
        if (this.selectedName && !this.items.some((job) => job.name === this.selectedName)) {
          this.selectedName = '';
          this.logs = [];
        }
      } catch (e) {
        this.items = [];
        this.error = e;
        console.error('monitoring: could not load the jobs', e);
      } finally {
        this.loading = false;
      }
    },

    /** Select a job and load its execution log. */
    async select(job) {
      this.selectedName = job.name;
      this.logsLoading = true;
      this.actionResult = null;
      try {
        this.logs = await window.MonitoringOps.jobLogs(job.name);
      } catch (e) {
        this.logs = [];
        console.error('monitoring: could not load the log of job ' + job.name, e);
      } finally {
        this.logsLoading = false;
      }
    },

    get selected() {
      return this.items.find((job) => job.name === this.selectedName) || null;
    },

    /** Suspend or resume a job's schedule. The job stays deployed either way. */
    async setEnabled(job, enabled) {
      this.actionResult = null;
      try {
        await (enabled ? window.MonitoringOps.enableJob(job.name) : window.MonitoringOps.disableJob(job.name));
        await this.load();
      } catch (e) {
        console.error('monitoring: could not ' + (enabled ? 'enable' : 'disable') + ' job ' + job.name, e);
        this.actionResult = { variant: 'negative',
          text: (e && e.errorMessage) || T('monitoring:jobs.actionFailed', 'The action failed.') };
      }
    },

    /** Open the trigger dialog, pre-filled with the job's declared parameters. */
    async openTrigger(job) {
      this.triggerJobName = job.name;
      this.triggerParameters = [];
      this.triggerOpen = true;
      this.actionResult = null;
      try {
        const parameters = await window.MonitoringOps.jobParameters(job.name);
        this.triggerParameters = (parameters || []).map((parameter) => ({
          name: parameter.name,
          description: parameter.description || '',
          value: parameter.defaultValue !== undefined && parameter.defaultValue !== null ? String(parameter.defaultValue) : '',
        }));
      } catch (e) {
        console.error('monitoring: could not read the parameters of job ' + job.name, e);
      }
    },

    closeTrigger() {
      this.triggerOpen = false;
      this.triggerJobName = '';
      this.triggerParameters = [];
    },

    /** Run the job now. The result lands in its execution log, which is reloaded. */
    async trigger() {
      if (this.triggering) return;
      this.triggering = true;
      const name = this.triggerJobName;
      try {
        await window.MonitoringOps.triggerJob(name,
          this.triggerParameters.map((parameter) => ({ name: parameter.name, value: parameter.value })));
        this.actionResult = { variant: 'positive', text: T('monitoring:jobs.triggered', 'The job was triggered.') };
        this.closeTrigger();
        await this.load();
        const job = this.items.find((candidate) => candidate.name === name);
        if (job) await this.select(job);
        Alpine.store('overview').refresh();
      } catch (e) {
        console.error('monitoring: could not trigger job ' + name, e);
        this.actionResult = { variant: 'negative',
          text: (e && e.errorMessage) || T('monitoring:jobs.actionFailed', 'The action failed.') };
      } finally {
        this.triggering = false;
      }
    },
  });
});
