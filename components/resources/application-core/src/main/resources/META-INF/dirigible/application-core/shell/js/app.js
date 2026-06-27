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
window.App = {
  services: {},
  routes: {},
  utils: {
    // Derive a related entity's embedded create-page URL from its REST controller URL, so an FK
    // combobox's "Add" button can open the target's OWN generated create form in an iframe dialog -
    // even when the target lives in another project (cross-model association). Example:
    //   /services/java/customers/gen/customers/api/customer/CustomerController , "Customer"
    //   -> /services/web/customers/gen/customers/index.html?embedded=1#/Customer/create?embedded=1
    // `embedded` is set in BOTH the search (the shell hides its chrome) and the hash query (the form
    // posts a "created" message instead of navigating away).
    relatedCreateUrl(controllerUrl, entity) {
      if (!controllerUrl || !entity) return '';
      const i = controllerUrl.indexOf('/api/');
      const base = (i >= 0 ? controllerUrl.substring(0, i) : controllerUrl).replace('/services/java/', '/services/web/');
      return base + '/index.html?embedded=1#/' + entity + '/create?embedded=1';
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
