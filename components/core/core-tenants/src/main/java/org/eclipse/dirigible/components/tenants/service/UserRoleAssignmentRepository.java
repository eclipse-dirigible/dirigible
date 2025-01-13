/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
}
