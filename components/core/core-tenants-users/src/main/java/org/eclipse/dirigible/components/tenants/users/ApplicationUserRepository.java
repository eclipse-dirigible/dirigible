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
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The application users. Every lookup carries the tenant id, so a tenant can never reach another
 * tenant's users.
 */
@Repository("applicationUserRepository")
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {

    /**
     * The users of a tenant, by email.
     *
     * @param tenantId the tenant id
     * @return the users
     */
    List<ApplicationUser> findByTenantIdOrderByEmail(String tenantId);

    /**
     * One user of a tenant.
     *
     * @param tenantId the tenant id
     * @param email the email, normalized
     * @return the user
     */
    Optional<ApplicationUser> findByTenantIdAndEmail(String tenantId, String email);
}
