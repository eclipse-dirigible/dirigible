/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.configurations.domain.TenantConfiguration;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * Manages the configuration entries of the current tenant, stored in the tenant's own
 * DIRIGIBLE_CONFIGURATIONS table. Only the allow-listed keys among them are actually injected into
 * the runtime configuration for the tenant's requests; the rest are stored but inert.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "configurations/tenant")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class TenantConfigurationsEndpoint extends BaseEndpoint {

    private final TenantConfigurationService tenantConfigurationService;

    public TenantConfigurationsEndpoint(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    /**
     * Lists the current tenant's configuration entries.
     *
     * @return the entries
     */
    @GetMapping
    public ResponseEntity<List<TenantConfiguration>> findAll() {
        try {
            return ResponseEntity.ok(tenantConfigurationService.listForCurrentTenant());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read tenant configuration", ex);
        }
    }

    /**
     * Lists the predefined (overridable) configuration keys with the current tenant's value for each
     * ({@code value} is {@code null} when unset). This is the fixed set the settings UI renders.
     *
     * @return the predefined entries
     */
    @GetMapping("/predefined")
    public ResponseEntity<List<TenantConfiguration>> findPredefined() {
        try {
            return ResponseEntity.ok(tenantConfigurationService.listPredefinedForCurrentTenant());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read tenant configuration", ex);
        }
    }

    /**
     * Inserts or updates a configuration entry of the current tenant.
     *
     * @param configuration the entry to store
     * @return an empty 200 response
     */
    @PutMapping
    public ResponseEntity<Void> set(@RequestBody TenantConfiguration configuration) {
        if (configuration == null || configuration.key() == null || configuration.key()
                                                                                 .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration key must not be blank");
        }
        try {
            tenantConfigurationService.set(configuration.key(), configuration.value());
            return ResponseEntity.ok()
                                 .build();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store tenant configuration", ex);
        }
    }

    /**
     * Deletes a configuration entry of the current tenant.
     *
     * @param key the configuration key
     * @return an empty 204 response
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration key must not be blank");
        }
        try {
            tenantConfigurationService.delete(key);
            return ResponseEntity.noContent()
                                 .build();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete tenant configuration", ex);
        }
    }

}
