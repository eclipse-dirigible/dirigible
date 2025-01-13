/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The Class SecurityRoleEndpoint.
 */

@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY)
public class RoleEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RoleEndpoint.class);

    /**
     * The security role service.
     */
    @Autowired
    private RoleService roleService;

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }
}
