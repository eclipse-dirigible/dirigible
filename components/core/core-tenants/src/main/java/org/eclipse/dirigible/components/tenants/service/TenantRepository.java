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

import java.util.Optional;
import java.util.Set;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Interface TenantRepository.
 */
interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Find by subdomain.
     *
     * @param subdomain the subdomain
     * @return the optional
     */
    Optional<Tenant> findBySubdomain(String subdomain);

    /**
     * Find by status.
     *
     * @param status the status
     * @return the sets the
     */
    Set<Tenant> findByStatus(TenantStatus status);
}
