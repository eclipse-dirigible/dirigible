/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * Runtime configuration for the Builder shell. The shared Harmonia runtime
 * (/services/web/application-core/shell/...) reads its wiring from window.App.config, exactly like a
 * generated app does. The Builder has no generated REST entities of its own - it talks to the intent
 * engine, the workspace and the publisher - so `restBase` stays empty.
 */
window.App = window.App || {};
App.config = {
  projectName: 'builder',
  basePath: '/services/web/builder',
  restBase: '',
  // The workspace every builder app lives in. Deliberately fixed: the Builder hides project and
  // file management entirely, so there is no workspace picker to keep in sync.
  workspace: 'workspace',
  // The intent file at the root of each builder project (one intent per project - the engine's rule).
  intentFile: 'app.intent'
};
