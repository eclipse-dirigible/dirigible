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
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.exceptions.TenantNotFoundException;
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
 * The Class UsersEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "users")
public class UsersEndpoint {


    /** The user service. */
    private final UserService userService;

    /**
     * Instantiates a new users endpoint.
     *
     * @param userService the user service
     */
    @Autowired
    public UsersEndpoint(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    /**
     * Gets the user.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.findById(id)
                                            .get());
    }

    /**
     * Find by username and tenant.
     *
     * @param username the username
     * @param tenant the tenant
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<User> findUserByUsernameAndTenant(@RequestParam("username") String username,
            @RequestParam("tenant") String tenant) {
        return ResponseEntity.ok(userService.findUserByUsernameAndTenantId(username, tenant)
                                            .get());
    }

    /**
     * Creates the user.
     *
     * @param userParameter the user parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping
    public ResponseEntity<URI> createUser(@Valid @RequestBody UserParameter userParameter) throws URISyntaxException {
        User user = userService.createNewUser(userParameter.getUsername(), userParameter.getPassword(), userParameter.getTenant());
        userService.assignUserRolesByIds(user, userParameter.getRoles());
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "users/" + user.getId()))
                             .build();
    }

    /**
     * Updates the user.
     *
     * @param id the id of the user
     * @param userParameter the user parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PutMapping("{id}")
    public ResponseEntity<URI> updateUser(@PathVariable("id") String id, @Valid @RequestBody UserParameter userParameter)
            throws URISyntaxException {
        User user = userService.updateUser(id, userParameter.getUsername(), userParameter.getPassword(), userParameter.getTenant());
        userService.assignUserRolesByIds(user, userParameter.getRoles());
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "users/" + user.getId()))
                             .build();
    }

    /**
     * Deletes the user.
     *
     * @param id the id of the user
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") String id) throws URISyntaxException {
        userService.deleteUser(id);
        return ResponseEntity.noContent()
                             .build();
    }

}
