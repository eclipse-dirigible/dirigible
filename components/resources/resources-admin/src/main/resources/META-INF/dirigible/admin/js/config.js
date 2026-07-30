/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the Administration Harmonia shell. The shared shell runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. The Administration shell surfaces the generated admin perspectives and serves the
 * built-in pages itself, so it has no own REST entities.
 */
window.App = window.App || {};
App.config = {
  projectName: 'admin',
  basePath: '/services/web/admin',
  // No own entities; the built-in stores (inbox/documents) call platform services directly.
  restBase: '',
  // The Administration shell surfaces low-level admin pages - it does not aggregate reports across apps.
  aggregateReports: false
};
