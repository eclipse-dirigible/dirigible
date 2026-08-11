/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the Database Harmonia shell. The shared shell runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. This shell has no entities of its own - every page reads an existing platform
 * endpoint under /services/data/ (see js/services/dbops.js), so there is no REST base to configure.
 */
window.App = window.App || {};
App.config = {
  projectName: 'database',
  basePath: '/services/web/database',
  // No own entities; the pages call the platform endpoints directly by absolute URL.
  restBase: '',
  // A support tool - it does not aggregate the applications' reports.
  aggregateReports: false
};
