/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant.groups;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a user's identity provider groups say about this application: which tenants the user may
 * enter and with which roles, plus the roles that are not bound to any tenant.
 *
 * @param tenantRoles the roles the user has per tenant id, for this application only
 * @param globalRoles the roles that carry no tenant (staff groups such as {@code DEVELOPER})
 */
public record UserTenantAssignments(Map<String, Set<String>> tenantRoles, Set<String> globalRoles) {

    /** The empty assignments. */
    private static final UserTenantAssignments EMPTY = new UserTenantAssignments(Collections.emptyMap(), Collections.emptySet());

    /**
     * Instantiates unmodifiable assignments, copying whatever was passed in.
     *
     * @param tenantRoles the roles per tenant id; may be {@code null}
     * @param globalRoles the tenant-less roles; may be {@code null}
     */
    public UserTenantAssignments {
        tenantRoles = copyOf(tenantRoles);
        globalRoles = globalRoles == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(globalRoles));
    }

    private static Map<String, Set<String>> copyOf(Map<String, Set<String>> tenantRoles) {
        if (tenantRoles == null || tenantRoles.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        tenantRoles.forEach((tenantId, roles) -> copy.put(tenantId, Collections.unmodifiableSet(new LinkedHashSet<>(roles))));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * The assignments of a user with no groups at all.
     *
     * @return empty assignments
     */
    public static UserTenantAssignments empty() {
        return EMPTY;
    }

    /**
     * The roles the user has in a tenant.
     *
     * @param tenantId the tenant id
     * @return the roles, empty when the user is not assigned to that tenant
     */
    public Set<String> rolesFor(String tenantId) {
        return tenantRoles.getOrDefault(tenantId, Collections.emptySet());
    }

    /**
     * The tenants the user may enter, in the order their groups were read.
     *
     * @return the tenant ids
     */
    public Set<String> tenantIds() {
        return tenantRoles.keySet();
    }

    /**
     * Whether the user is assigned to no tenant of this application. Such a user is either staff
     * (global roles only) or has no access at all.
     *
     * @return true if there is no tenant assignment
     */
    public boolean hasNoTenants() {
        return tenantRoles.isEmpty();
    }
}
