/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The aggregate state behind the Overview page - the one screen that answers "is everything OK, and
 * if not, what?". It polls the platform's own endpoints (js/services/ops.js), keeps the last good
 * value of every source, and records per-source failures so an unavailable engine degrades to one
 * marked tile instead of an empty page.
 *
 * A store (not page state) because the sidebar badges read the same counts, and the poll must
 * survive a route change within the shell.
 */
document.addEventListener('alpine:init', () => {
  /** Auto-refresh cadence. There is no server-side history - every poll is a fresh snapshot. */
  const REFRESH_MS = 30000;

  /**
   * How many active process instances are probed for dead-letter jobs per refresh. There is no
   * bulk incident endpoint, so this is one request per instance; the page says so when it truncates
   * rather than silently reporting "no incidents" for an instance it never looked at.
   */
  const INCIDENT_SCAN_LIMIT = 50;

  /** The artefact lifecycles that mean "this did not synchronize". */
  const FAILED_LIFECYCLES = ['FAILED', 'FATAL'];

  Alpine.store('overview', {
    loading: true,
    refreshing: false,
    paused: false,
    lastUpdated: null,
    // Per-source read failures: { <source>: 'forbidden' | 'unavailable' }.
    errors: {},

    health: null,
    readiness: null,
    failedArtefacts: [],
    failedJobs: [],
    incidents: [],
    incidentsTruncated: false,
    pools: [],
    backlog: [],

    _timer: null,

    /** Start polling (called by the Overview page on init; idempotent). */
    start() {
      if (this._timer === null) {
        this._timer = setInterval(() => {
          if (!this.paused) this.refresh();
        }, REFRESH_MS);
      }
      this.refresh();
    },

    /** Stop polling - the shell keeps the last snapshot on screen. */
    stop() {
      if (this._timer !== null) {
        clearInterval(this._timer);
        this._timer = null;
      }
    },

    togglePause() {
      this.paused = !this.paused;
      if (!this.paused) this.refresh();
    },

    /** Re-read every source. Sources are read concurrently; one failure never fails the rest. */
    async refresh() {
      if (this.refreshing) return;
      this.refreshing = true;
      const errors = {};
      const ops = window.MonitoringOps;
      try {
        const [health, readiness, artefacts, jobs, counts, broker] = await Promise.all([
          ops.soft('health', errors, ops.health, null),
          ops.soft('readiness', errors, ops.readiness, null),
          ops.soft('artefacts', errors, ops.artefacts, []),
          ops.soft('jobs', errors, ops.jobs, []),
          ops.soft('counts', errors, ops.counts, null),
          ops.soft('messaging', errors, ops.brokerSummary, null),
        ]);

        this.health = health;
        this.readiness = readiness;
        this.failedArtefacts = (artefacts || []).filter((a) => FAILED_LIFECYCLES.includes(a.status));
        this.failedJobs = (jobs || []).filter((j) => j.status === 'FAILED');
        this.pools = this.readPools(counts);
        this.backlog = this.readBacklog(broker);
        await this.readIncidents(errors);
        this.errors = errors;
        this.lastUpdated = new Date();
      } finally {
        this.refreshing = false;
        this.loading = false;
      }
    },

    /** Database pools under pressure are the ones the page cares about; the group carries one
     *  named count per (data source, metric) pair, so recompose them per data source. */
    readPools(counts) {
      const group = ((counts && counts.groups) || []).find((g) => g.title === 'Database pools');
      if (!group) return [];
      const pools = new Map();
      for (const item of group.items || []) {
        const separator = item.name.lastIndexOf(' · ');
        if (separator < 0) continue;
        const name = item.name.slice(0, separator);
        const metric = item.name.slice(separator + 3);
        const pool = pools.get(name) || { name, active: 0, awaiting: 0, max: null };
        if (metric === 'active') {
          pool.active = item.value;
          pool.max = item.max;
        } else if (metric === 'awaiting') {
          pool.awaiting = item.value;
        }
        pools.set(name, pool);
      }
      return [...pools.values()];
    },

    /** Queues holding messages; one with no consumer attached is the interesting case. */
    readBacklog(broker) {
      return ((broker && broker.queues) || [])
        .filter((q) => q.size > 0)
        .map((q) => ({ name: q.name, size: q.size, consumerCount: q.consumerCount }));
    },

    /** A process instance with dead-letter jobs is an incident. There is no bulk endpoint, so the
     *  active instances are probed one by one, capped at INCIDENT_SCAN_LIMIT. */
    async readIncidents(errors) {
      const ops = window.MonitoringOps;
      const instances = await ops.soft('processes', errors, ops.processInstances, []);
      const scanned = (instances || []).slice(0, INCIDENT_SCAN_LIMIT);
      this.incidentsTruncated = (instances || []).length > scanned.length;
      const incidents = await Promise.all(scanned.map(async (instance) => {
        const jobs = await ops.soft('processes', errors, () => ops.deadLetterJobs(instance.id), []);
        if (!jobs || !jobs.length) return null;
        return {
          id: instance.id,
          definition: instance.processDefinitionName || instance.processDefinitionKey || '',
          businessKey: instance.businessKey || '',
          jobCount: jobs.length,
          message: jobs[0].exceptionMessage || '',
        };
      }));
      this.incidents = incidents.filter(Boolean);
    },

    /** Everything the page counts as "something is wrong right now". */
    get problemCount() {
      return this.failedArtefacts.length + this.failedJobs.length + this.incidents.length
        + this.failedSynchronizers.length;
    },

    /** Synchronizers whose last pass failed, from the health payload. */
    get failedSynchronizers() {
      const statuses = (this.health && this.health.jobs && this.health.jobs.statuses) || {};
      return Object.keys(statuses).filter((name) => statuses[name] === 'Failed');
    },

    /** The health tile's Harmonia variant: red on a failed synchronizer, amber while still running. */
    get healthVariant() {
      if (this.errors.health) return 'warning';
      if (this.failedSynchronizers.length) return 'negative';
      const status = this.health && this.health.status;
      if (status === 'Ready') return 'positive';
      return status === 'Running' ? 'information' : 'warning';
    },

    /** The readiness tile's variant: red when traffic is refused or an artefact failed. */
    get readinessVariant() {
      if (this.errors.readiness || !this.readiness) return 'warning';
      if (!this.readiness.acceptingTraffic || this.readiness.failedArtefacts > 0) return 'negative';
      return this.readiness.pendingArtefacts > 0 ? 'information' : 'positive';
    },

    /** Pools where a caller is waiting for a connection, or which are at their maximum. */
    get pressuredPools() {
      return this.pools.filter((p) => p.awaiting > 0 || (p.max && p.active >= p.max));
    },

    /** Queues holding messages with nothing consuming them. */
    get stalledQueues() {
      return this.backlog.filter((q) => !q.consumerCount);
    },
  });
});
