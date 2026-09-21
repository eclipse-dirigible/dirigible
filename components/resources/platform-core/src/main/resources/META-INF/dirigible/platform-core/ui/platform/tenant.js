/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*
 * The tenant this request runs in, and the tenants the user could switch to - what the Workbench
 * header renders as its tenant chip. The same endpoints the Harmonia shells' `tenant` store reads:
 * the state answers on every resolution strategy, the selection is the picker's own POST and
 * doubles as the switch.
 */
angular.module('platformTenant', []).factory('Tenant', ($http) => ({
    getState: () => $http({
        url: '/services/security/tenant-selection/current',
        method: 'GET',
        headers: { 'X-Requested-With': 'Fetch' }
    }),
    select: (tenantId) => $http({
        url: '/services/security/tenant-selection',
        method: 'POST',
        data: { tenantId: tenantId },
        headers: { 'X-Requested-With': 'Fetch' }
    })
}));
