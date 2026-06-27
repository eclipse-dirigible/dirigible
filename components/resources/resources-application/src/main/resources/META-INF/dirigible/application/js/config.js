/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the Harmonia application shell. The shared shell runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. The shell hosts other projects' perspectives in an iframe and serves the
 * built-in pages (Dashboard/Inbox/Documents/Reports) itself, so it has no own REST entities.
 */
window.App = window.App || {};
App.config = {
  projectName: 'application',
  basePath: '/services/web/application',
  // No own entities; the built-in stores (inbox/documents/reports) call platform services directly.
  restBase: ''
};
