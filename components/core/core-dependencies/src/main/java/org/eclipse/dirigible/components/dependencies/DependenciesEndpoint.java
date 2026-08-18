/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import jakarta.annotation.security.RolesAllowed;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The maven dependency resolution surface - the current declared / resolved state and the on-demand
 * union resolution. The resolved jars take effect immediately through the swappable modules
 * classloader; no restart is required.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "dependencies")
class DependenciesEndpoint {

    /** The dependencies service. */
    private final DependenciesService dependenciesService;

    /**
     * Instantiates a new dependencies endpoint.
     *
     * @param dependenciesService the dependencies service
     */
    DependenciesEndpoint(DependenciesService dependenciesService) {
        this.dependenciesService = dependenciesService;
    }

    /**
     * The current declared / resolved state.
     *
     * @return the state
     */
    @GetMapping
    @RolesAllowed({"ADMINISTRATOR", "OPERATOR", "DEVELOPER"})
    ResponseEntity<DependenciesState> getState() {
        return ResponseEntity.ok(dependenciesService.getState());
    }

    /**
     * Runs the union resolution on demand.
     *
     * @return the resolved state
     */
    @PostMapping("resolve")
    @RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
    ResponseEntity<DependenciesState> resolve() {
        if (!dependenciesService.isDynamicEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dynamic dependency resolution is disabled - set DIRIGIBLE_DEPENDENCIES_DYNAMIC=true to enable it");
        }
        return ResponseEntity.ok(dependenciesService.resolveAndActivate());
    }

}
