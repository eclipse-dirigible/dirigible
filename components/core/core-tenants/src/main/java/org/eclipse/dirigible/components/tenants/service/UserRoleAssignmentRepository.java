/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.domain.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Interface UserRoleAssignmentRepository.
 */
interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {

    /**
     * Find by user.
     *
     * @param user the user
     * @return the list
     */
    List<UserRoleAssignment> findByUser(User user);

    /**
     * Find by user and role.
     *
     * @param user the user
     * @param role the role
     * @return the list
     */
    Optional<UserRoleAssignment> findByUserAndRole(User user, Role role);

    /**
     * Bulk-delete every assignment that references the given role. Invoked when a role's {@code .roles}
     * artefact is removed from the registry, so the role's row can be deleted without tripping the
     * {@code DIRIGIBLE_USER_ROLE_ASSIGNMENTS → DIRIGIBLE_SECURITY_ROLES} FK.
     *
     * @param role the role whose assignments must be detached
     * @return the number of assignment rows deleted
     */
    @Modifying
    @Transactional
    @Query("delete from UserRoleAssignment u where u.role = :role")
    int deleteAllByRole(@Param("role") Role role);
}
