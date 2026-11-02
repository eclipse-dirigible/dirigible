/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.springframework.stereotype.Component;

/**
 * Which roles an integration client may hold: the roles this application declares, never the
 * tenant's {@code Owner} (it manages the tenant's people) and never a role that is global to the
 * instance - the platform's own and the provisioning system's. A client works with the tenant's
 * data and nothing else.
 */
@Component
class TenantClientRoles {

    /** The roles no client is ever given. */
    static final Set<String> FORBIDDEN = Stream
                                               .concat(Stream.of(ApplicationRoles.OWNER, TenantProvisioningRoles.TENANT_PROVISIONER),
                                                       Arrays.stream(Roles.values())
                                                             .map(Roles::getRoleName))
                                               .collect(Collectors.toUnmodifiableSet());

    /** The registered roles. */
    private final RoleService roleService;

    /**
     * Instantiates the roles.
     *
     * @param roleService the registered roles
     */
    TenantClientRoles(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Whether a role may never be given to a client.
     *
     * @param role the role name
     * @return true for {@code Owner}, a platform role or the provisioning role
     */
    static boolean isForbidden(String role) {
        return FORBIDDEN.contains(role);
    }

    /**
     * The roles a client may be given: the declared roles of this application, minus the forbidden
     * ones.
     *
     * @return the role names, sorted
     */
    List<String> grantable() {
        return roleService.getAll()
                          .stream()
                          .map(Role::getName)
                          .filter(name -> name != null && !isForbidden(name))
                          .distinct()
                          .sorted()
                          .toList();
    }
}
