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

import org.eclipse.dirigible.components.tenants.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Interface UserRepository.
 */
interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by username and tenant id.
     *
     * @param username the username
     * @param tenantId the tenant id
     * @return the optional
     */
    Optional<User> findUserByUsernameAndTenantId(String username, String tenantId);

    /**
     * Find users by tenant id.
     *
     * @param tenantId the tenant id
     * @return the list
     */
    List<User> findUsersByTenantId(String tenantId);
}
