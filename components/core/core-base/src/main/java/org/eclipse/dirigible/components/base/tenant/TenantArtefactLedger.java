/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;

/**
 * Records how a per-tenant artefact fared for each tenant it was materialized in.
 *
 * <p>
 * A per-tenant artefact is one shared row however many tenants it is materialized in, so the row
 * alone cannot say that it failed for one tenant and succeeded for another - the tenant processed
 * last used to decide what it read. The ledger keeps one outcome per artefact and tenant, which is
 * what lets the initialization status of a tenant answer for that tenant only.
 */
public interface TenantArtefactLedger {

    /**
     * Records the outcome of an artefact for a tenant, replacing the one recorded before.
     *
     * @param artefact the artefact
     * @param tenantId the tenant id
     * @param lifecycle the lifecycle the artefact reached for the tenant
     * @param error the error, when the lifecycle is a failure
     */
    void record(Artefact artefact, String tenantId, ArtefactLifecycle lifecycle, String error);

}
