/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * The Class TenantEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenants")
public class TenantEndpoint {


    /** The tenant service. */
    private final TenantService tenantService;

    /** The user service. */
    private final UserService userService;

    /**
     * Instantiates a new tenants endpoint.
     *
     * @param tenantService the tenant service
     */
    @Autowired
    public TenantEndpoint(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAll() {
        return ResponseEntity.ok(tenantService.getAll());
    }

    /**
     * Gets the tenant.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(tenantService.findById(id)
                                              .get());
    }

    /**
     * Find by subdomain.
     *
     * @param subdomain the subdomain
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<Tenant> findByName(@RequestParam("subdomain") String subdomain) {
        return ResponseEntity.ok(tenantService.findBySubdomain(subdomain)
                                              .get());
    }

    /**
     * Creates the tenant.
     *
     * @param tenantParameter the tenant parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping
    public ResponseEntity<URI> createTenant(@Valid @RequestBody TenantParameter tenantParameter) throws URISyntaxException {
        Tenant tenant = new Tenant("API_" + tenantParameter.getName(), tenantParameter.getName(), "", tenantParameter.getSubdomain(),
                TenantStatus.INITIAL);
        tenant.setId(UUID.randomUUID()
                         .toString());
        tenant.updateKey();
        tenant = tenantService.save(tenant);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenants/" + tenant.getId()))
                             .build();
    }

    /**
     * Updates the tenant.
     *
     * @param id the id of the tenant
     * @param tenantParameter the tenant parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PutMapping("{id}")
    public ResponseEntity<URI> updateTenant(@PathVariable("id") String id, @Valid @RequestBody TenantParameter tenantParameter)
            throws URISyntaxException {
        Tenant tenant = tenantService.findById(id)
                                     .get();
        tenant.setName(tenantParameter.getName());
        tenant.setSubdomain(tenantParameter.getSubdomain());
        tenant.updateKey();
        tenant = tenantService.save(tenant);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenants/" + tenant.getId()))
                             .build();
    }

    /**
     * Deletes the tenant.
     *
     * @param id the id of the tenant
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteDataSource(@PathVariable("id") String id) throws URISyntaxException {
        Tenant tenant = tenantService.findById(id)
                                     .get();
        if (TenantStatus.INITIAL.equals(tenant.getStatus())) {
            userService.findUsersByTenantId(tenant.getId())
                       .forEach(user -> userService.deleteUser(user.getId()));
            tenantService.delete(tenant);
            return ResponseEntity.noContent()
                                 .build();
        }
        return ResponseEntity.badRequest()
                             .body("Deletion of already provisioned tenants is currently not supported");
    }


}
