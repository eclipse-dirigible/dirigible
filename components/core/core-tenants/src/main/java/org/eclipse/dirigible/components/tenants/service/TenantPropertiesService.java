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

import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.TenantProperties;
import org.springframework.stereotype.Service;

/**
 * Reads and writes what the provisioning system decided about a tenant ({@link TenantProperties}).
 * Read on use, with no cache: the readers are a person's screen and command, not a hot path, and a
 * written value applies at once.
 */
@Service
public class TenantPropertiesService {

    /** The properties. */
    private final TenantPropertiesRepository properties;

    /**
     * Instantiates the service.
     *
     * @param properties the properties
     */
    TenantPropertiesService(TenantPropertiesRepository properties) {
        this.properties = properties;
    }

    /**
     * The properties of a tenant.
     *
     * @param tenantId the tenant id
     * @return the properties, or empty when none was ever written
     */
    public Optional<TenantProperties> find(String tenantId) {
        return properties.findById(tenantId);
    }

    /**
     * Writes the properties of a tenant, after checking every one.
     *
     * @param tenantProperties the properties
     * @return the properties as saved
     * @throws IllegalArgumentException when a property has a value it may not have
     */
    public TenantProperties save(TenantProperties tenantProperties) {
        Integer seatLimit = tenantProperties.getSeatLimit();
        if (seatLimit != null && seatLimit < 1) {
            throw new IllegalArgumentException(
                    "The seat limit of tenant [" + tenantProperties.getTenantId() + "] must be at least 1, not [" + seatLimit + "]");
        }
        return properties.save(tenantProperties);
    }

    /**
     * Deletes the properties of a tenant; finding none is success.
     *
     * @param tenantId the tenant id
     * @return whether a row was deleted
     */
    boolean delete(String tenantId) {
        Optional<TenantProperties> found = properties.findById(tenantId);
        found.ifPresent(properties::delete);
        return found.isPresent();
    }
}
