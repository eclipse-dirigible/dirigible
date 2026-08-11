/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Monitoring shell's read layer: one thin wrapper per platform endpoint the shell reads, so the
 * pages never spell out a URL. Every call goes through the shared fetch client with `{ baseUrl: '' }`
 * - the URLs here are absolute platform paths, so nothing must be prepended.
 *
 * The endpoints are role-gated to ADMINISTRATOR / DEVELOPER / OPERATOR; the shell itself is gated the
 * same way (monitoring.access), so a user who reaches a page may call what it reads.
 */
window.MonitoringOps = (() => {
  const ABSOLUTE = { baseUrl: '' };
  const get = (url) => App.services.api.get(url, ABSOLUTE);

  return {
    /**
     * The build this instance runs: { productName, productVersion, productCommitId,
     * productRepository, productType, instanceName, repositoryProvider, databaseProvider, engines }.
     * The same payload the Workbench's About window reads.
     */
    version: () => get('/services/core/version'),

    /** Platform health: { status, currentStatus, jobs: { statuses: { <synchronizer>: <state> } } }. */
    health: () => get('/services/core/healthcheck'),

    /** Readiness: { status, acceptingTraffic, pendingArtefacts, failedArtefacts, since }. */
    readiness: () => get('/services/core/readiness'),

    /** Every synchronized artefact: [{ location, name, type, phase, status, error, running }]. */
    artefacts: () => get('/services/core/artefacts'),

    /** The scheduled jobs with their last execution outcome. */
    jobs: () => get('/services/jobs'),

    /** The scheduled job's execution log. A job name is a base name - it never carries a path. */
    jobLogs: (name) => get('/services/jobs/logs/' + encodeURIComponent(name)),

    /** The parameters a job declares, asked for before triggering it. */
    jobParameters: (name) => get('/services/jobs/parameters/' + encodeURIComponent(name)),

    /** Resume a disabled job's schedule. */
    enableJob: (name) => App.services.api.post('/services/jobs/enable/' + encodeURIComponent(name), undefined, ABSOLUTE),

    /** Suspend a job's schedule; it stays deployed. */
    disableJob: (name) => App.services.api.post('/services/jobs/disable/' + encodeURIComponent(name), undefined, ABSOLUTE),

    /** Run a job now with the given [{ name, value }] parameters. */
    triggerJob: (name, parameters) =>
      App.services.api.post('/services/jobs/trigger/' + encodeURIComponent(name), parameters, ABSOLUTE),

    /** Active process instances. */
    processInstances: () => get('/services/bpm/bpm-processes/instances'),

    /** Completed process instances. */
    historicProcessInstances: () => get('/services/bpm/bpm-processes/historic-instances'),

    /** The dead-letter jobs of one process instance - a non-empty list is an incident. */
    deadLetterJobs: (instanceId) =>
      get('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId) + '/jobs'),

    /** The variables of an active process instance (read-only here - editing them is IDE territory). */
    instanceVariables: (instanceId) =>
      get('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId) + '/variables'),

    /** The variables a completed instance ended with. */
    historicInstanceVariables: (instanceId) =>
      get('/services/bpm/bpm-processes/historic-instances/' + encodeURIComponent(instanceId) + '/variables'),

    /** The open user tasks of a process instance. */
    instanceTasks: (instanceId) =>
      get('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId) + '/tasks'),

    /** Per-activity { positive, negative } counters, overlaid as badges on the diagram. */
    instanceActivities: (instanceId) =>
      get('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId) + '/active'),

    /**
     * Retry or skip the failed step of a stuck instance - the two management actions this shell
     * allows. RETRY re-runs the dead-letter job; SKIP marks the step to be passed over and retries.
     *
     * @param {string} instanceId the process instance
     * @param {string} action RETRY or SKIP
     */
    instanceAction: (instanceId, action) =>
      App.services.api.post('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId), { action }, ABSOLUTE),

    /** JVM counters grouped by subject: { timestamp, groups: [{ title, items: [{ name, value, max }] }] }. */
    counts: () => get('/services/ide/monitoring/counts'),

    /** The JVM snapshot: runtime, CPU, memory (heap, non-heap, pools), threads and GC. */
    metrics: () => get('/services/ide/monitoring/metrics'),

    /** Every live thread with its state and stack. */
    threads: () => get('/services/ide/monitoring/threads'),

    /** The message broker's queues and topics with their counters. */
    brokerSummary: () => get('/services/ide/messaging-monitoring/summary'),

    /** Browse a queue's pending messages without consuming them. */
    queueMessages: (name, limit) =>
      get('/services/ide/messaging-monitoring/queues/' + encodeURIComponent(name) + '/messages?limit=' + limit),

    /** The log files on disk. */
    logFiles: () => get('/services/ide/logs/'),

    /** One log file, whole. It is never polled - the page tails what it loaded. */
    logFile: (file) => get('/services/ide/logs/' + encodeURIComponent(file)),

    /**
     * Run a read that must not take the page down with it. A monitoring screen exists to show what is
     * broken, so one unavailable source (an engine that is not enabled, a 403, a restart mid-poll)
     * degrades to a marked-unavailable tile instead of an empty screen.
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
        console.error('monitoring: could not read ' + name, e);
        return fallback;
      }
    },
  };
})();
