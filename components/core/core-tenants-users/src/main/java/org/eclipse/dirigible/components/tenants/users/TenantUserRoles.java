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

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The roles a person of a tenant may be given (#8728): {@code Owner} and {@code User}, then every
 * role the application's {@code .roles} files mark {@code "assignable": true}, with the label and
 * the description those entries give it.
 *
 * <p>
 * The per-entity convention roles ({@code ...ReadOnly} / {@code ...FullAccess}) carry no such mark,
 * so an application with many entities still offers its Owner a short list; they stay grantable to
 * integration clients. A role of the instance and the provisioning role are never offered, even
 * when a {@code .roles} file marks them, and neither is a name longer than the users snapshot
 * carries.
 */
@Component
class TenantUserRoles {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserRoles.class);

    /** The roles no person is given through a tenant: the instance's own and the provisioning role. */
    static final Set<String> FORBIDDEN = Stream.concat(Stream.of(TenantProvisioningRoles.TENANT_PROVISIONER), Arrays.stream(Roles.values())
                                                                                                                    .map(Roles::getRoleName))
                                               .collect(Collectors.toUnmodifiableSet());

    /** The registered roles. */
    private final RoleService roleService;

    /** The refused names already reported, so a page load does not repeat the warning. */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * Instantiates the roles.
     *
     * @param roleService the registered roles
     */
    TenantUserRoles(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * The roles an Owner may give, in the order a page offers them: {@code Owner}, {@code User}, then
     * the application's assignable roles by name.
     *
     * @return the catalog
     */
    List<Offer> catalog() {
        Map<String, Offer> offers = new LinkedHashMap<>();
        ApplicationRoles.ALL.forEach(name -> offers.put(name, new Offer(name, null, null)));
        roleService.getAll()
                   .stream()
                   .filter(Role::isAssignable)
                   .filter(role -> role.getName() != null && !role.getName()
                                                                  .isBlank())
                   .sorted(Comparator.comparing(Role::getName))
                   .filter(this::isOfferable)
                   .forEach(role -> offers.putIfAbsent(role.getName(),
                           new Offer(role.getName(), blankToNull(role.getLabel()), blankToNull(role.getDescription()))));
        return List.copyOf(offers.values());
    }

    /**
     * The names of the roles an Owner may give.
     *
     * @return the names, in the catalog's order
     */
    List<String> names() {
        return catalog().stream()
                        .map(Offer::name)
                        .toList();
    }

    private boolean isOfferable(Role role) {
        String name = role.getName();
        if (FORBIDDEN.contains(name)) {
            warnOnce(name, "Role [{}] of [{}] is marked assignable, but a role of the instance is never given to a person of a tenant",
                    role.getLocation());
            return false;
        }
        if (name.length() > TenantUserSyncValidation.ROLE_MAX) {
            warnOnce(name, "Role [{}] of [{}] is marked assignable, but its name is longer than the " + TenantUserSyncValidation.ROLE_MAX
                    + " characters a tenant user's role may have", role.getLocation());
            return false;
        }
        return true;
    }

    private void warnOnce(String name, String message, String location) {
        if (reported.add(name)) {
            LOGGER.warn(message, name, location);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * A role an Owner may give.
     *
     * @param name the role, as the identity provider's group carries it
     * @param label how it reads; null - the name stands in
     * @param description what it lets a person do; null when the application says nothing
     */
    record Offer(String name, String label, String description) {
    }
}
