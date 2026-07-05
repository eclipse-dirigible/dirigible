/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
// Safety net for apps generated before label i18n existed: their index.html does not load
// services/i18n.js, but the SHARED views (inbox/documents/reports) now bind labels through T().
// The stub returns the English fallback; i18n.js overwrites it with the real translator when loaded.
window.T = window.T || ((key, fallback) => fallback !== undefined ? fallback : key);

window.App = {
  services: {},
  routes: {},
  utils: {
    // Build the embedded create-page URL for a related entity, so an FK combobox's "Add" button can
    // open the target's OWN generated create form in an iframe dialog - even when the target lives in
    // another project (cross-model association). `embedded` is set in BOTH the search (the shell hides
    // its chrome) and the hash query (the form posts a "created" message instead of navigating away).
    //   appUrl "/services/web/customer-payments/gen/customer-payments/index.html", "CustomerPayment"
    //   -> "/services/web/customer-payments/gen/customer-payments/index.html?embedded=1#/CustomerPayment/create?embedded=1&dialog=1"
    // The generator passes the target app's index.html at its RAW gen folder. Legacy fallback: apps
    // generated before that arg existed pass a Java controller URL - rewrite it (this keeps the
    // sanitized gen folder, which only matters for hyphenated project names, but preserves behaviour).
    relatedCreateUrl(appUrl, entity) {
      if (!appUrl || !entity) return '';
      let base = appUrl;
      const apiIdx = appUrl.indexOf('/api/');
      if (apiIdx >= 0) {
        base = appUrl.substring(0, apiIdx).replace('/services/java/', '/services/web/') + '/index.html';
      }
      return base + '?embedded=1#/' + entity + '/create?embedded=1&dialog=1';
    },
  },
  config: {},

  // Master-detail registry. Each generated detail entity registers its metadata here
  // under its master entity's name; a master page renders one detail panel per entry,
  // so masters and details stay decoupled (no cross-entity generation). See
  // js/components/detailPanel.js and the master view.
  details: {},
  registerDetail(masterEntity, def) {
    (this.details[masterEntity] = this.details[masterEntity] || []).push(def);
  },
  detailsFor(masterEntity) {
    return this.details[masterEntity] || [];
  },
};
