/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The initializations activations asked for, by tenant id.
 */
@Repository
public interface TenantInitializationRepository extends JpaRepository<TenantInitialization, String> {

    /**
     * Finds the initializations in the given states.
     *
     * @param states the states
     * @return the initializations
     */
    List<TenantInitialization> findByStateIn(Collection<TenantInitialization.State> states);
}
