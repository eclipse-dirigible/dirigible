/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.security.domain.Scope;
import org.eclipse.dirigible.components.security.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The Class ScopeEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY)
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class ScopeEndpoint extends BaseEndpoint {

    /**
     * The scope service.
     */
    @Autowired
    private ScopeService scopeService;

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping("/scopes")
    public ResponseEntity<List<Scope>> getAll() {
        return ResponseEntity.ok(scopeService.getAll());
    }
}
