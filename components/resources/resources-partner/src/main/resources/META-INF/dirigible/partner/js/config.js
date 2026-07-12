/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the partner Harmonia shell. The shared shell runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. The Partner shell surfaces the external partner's perspectives and serves the
 * built-in pages itself, so it has no own REST entities.
 */
window.App = window.App || {};
App.config = {
  projectName: 'partner',
  basePath: '/services/web/partner',
  // No own entities; the built-in stores (inbox/documents) call platform services directly.
  restBase: '',
  // The Partner shell is an external-surfaces shell - it does not aggregate reports across apps.
  aggregateReports: false
};
