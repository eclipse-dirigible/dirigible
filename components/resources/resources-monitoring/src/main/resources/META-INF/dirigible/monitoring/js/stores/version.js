/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * version store - which build this instance actually runs.
 *
 * The same `/services/core/version` payload the Workbench's About window reads: the product and its
 * version, the commit it was built from, the instance name, the repository/database providers and
 * the engines this assembly ships. It is read ONCE - a running instance never changes its build - and
 * both the sidebar footer (visible from every page) and the System page render it from here.
 */
document.addEventListener('alpine:init', () => {
  /**
   * A build that never filtered its Maven properties reports the placeholder itself
   * ('${project.version}'). That is not a version - it reads as unknown, exactly as the Workbench's
   * About window treats it.
   *
   * @param {string} value the reported value
   * @return {string} the value, or an empty string when it is an unfiltered placeholder
   */
  const resolved = (value) => (typeof value === 'string' && value && !value.startsWith('${') ? value : '');

  Alpine.store('version', {
    /** The raw payload, or null while it has not been read (or could not be). */
    info: null,
    loaded: false,
    error: null,

    init() {
      this.load();
    },

    async load() {
      try {
        this.info = await window.MonitoringOps.version();
      } catch (e) {
        // Knowing the build is a convenience: an instance that cannot report it still monitors fine.
        this.error = 'unavailable';
        console.error('monitoring: could not read the product version', e);
      } finally {
        this.loaded = true;
      }
    },

    value(name) {
      return this.info ? resolved(this.info[name]) : '';
    },

    get productName() {
      return this.value('productName');
    },

    get productVersion() {
      return this.value('productVersion');
    },

    get instanceName() {
      return this.value('instanceName');
    },

    get engines() {
      return (this.info && this.info.engines) || [];
    },

    /** The one-line identity for the sidebar: 'BusinessIntents Suite 2.98.0'. */
    get summary() {
      return [this.productName, this.productVersion].filter(Boolean)
                                                    .join(' ');
    },

    /** What the sidebar line reveals on hover - the details that do not fit on it. */
    get tooltip() {
      const commit = this.commitShort;
      return [this.instanceName, commit ? 'commit ' + commit : ''].filter(Boolean)
                                                                 .join(' · ');
    },

    /** The commit is long; the short form is the one people compare against a release. */
    get commitShort() {
      const commit = this.value('productCommitId');
      return commit ? commit.substring(0, 7) : '';
    },

    /** The commit on the product's own repository, when the build reported both. */
    get commitUrl() {
      const repository = this.value('productRepository');
      const commit = this.value('productCommitId');
      return repository && commit ? repository + '/commit/' + commit : '';
    },
  });
});
