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
    /** Platform health: { status, currentStatus, jobs: { statuses: { <synchronizer>: <state> } } }. */
    health: () => get('/services/core/healthcheck'),

    /** Readiness: { status, acceptingTraffic, pendingArtefacts, failedArtefacts, since }. */
    readiness: () => get('/services/core/readiness'),

    /** Every synchronized artefact: [{ location, name, type, phase, status, error, running }]. */
    artefacts: () => get('/services/core/artefacts'),

    /** The scheduled jobs with their last execution outcome. */
    jobs: () => get('/services/jobs'),

    /** Active process instances. */
    processInstances: () => get('/services/bpm/bpm-processes/instances'),

    /** The dead-letter jobs of one process instance - a non-empty list is an incident. */
    deadLetterJobs: (instanceId) =>
      get('/services/bpm/bpm-processes/instance/' + encodeURIComponent(instanceId) + '/jobs'),

    /** JVM counters grouped by subject: { timestamp, groups: [{ title, items: [{ name, value, max }] }] }. */
    counts: () => get('/services/ide/monitoring/counts'),

    /** The message broker's queues and topics with their counters. */
    brokerSummary: () => get('/services/ide/messaging-monitoring/summary'),

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
