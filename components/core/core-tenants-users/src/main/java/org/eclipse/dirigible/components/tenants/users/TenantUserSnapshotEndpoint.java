/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The users snapshot endpoint of the tenant provisioning API: the external provisioning system -
 * the source of truth of who belongs to a tenant - writes its users here, either the ones that
 * changed or, with {@code complete: true}, the tenant's full list. A tenant owner then sees them in
 * the application without the application ever calling that system.
 *
 * <p>
 * It exists exactly when the tenant provisioning API does, behind the same roles, and its URL is
 * already claimed by that API's security configurator.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "tenants/{tenantId}/users")
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantUserSnapshotEndpoint extends BaseEndpoint {

    /** The replica. */
    private final TenantUserReplicaService replica;

    /**
     * Instantiates the endpoint.
     *
     * @param replica the replica
     */
    TenantUserSnapshotEndpoint(TenantUserReplicaService replica) {
        this.replica = replica;
    }

    /**
     * Applies the provisioning system's users. Answers 200 whatever the counters: a user whose revision
     * is not newer is ignored, which is normal, never an error.
     *
     * @param tenantId the tenant id
     * @param body the users
     * @return what the apply did
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantUserSync.Result> apply(@PathVariable("tenantId") String tenantId, @RequestBody TenantUserSync body) {
        return ResponseEntity.ok(replica.apply(tenantId, body));
    }

    /**
     * The users of a tenant, for diagnostics.
     *
     * @param tenantId the tenant id
     * @param includeRemoved whether the hidden tombstones are included
     * @return the users, by email
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TenantUserView>> list(@PathVariable("tenantId") String tenantId,
            @RequestParam(name = "includeRemoved", defaultValue = "false") boolean includeRemoved) {
        return ResponseEntity.ok(replica.list(tenantId, includeRemoved));
    }
}
