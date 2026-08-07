/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the Monitoring Harmonia shell. The shared shell runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. The Monitoring shell has no entities of its own - every page reads an existing
 * platform endpoint (see js/services/ops.js), so there is no REST base to configure.
 */
window.App = window.App || {};
App.config = {
  projectName: 'monitoring',
  basePath: '/services/web/monitoring',
  // No own entities; the pages call the platform endpoints directly by absolute URL.
  restBase: '',
  // An operations surface - it does not aggregate the applications' reports.
  aggregateReports: false
};
