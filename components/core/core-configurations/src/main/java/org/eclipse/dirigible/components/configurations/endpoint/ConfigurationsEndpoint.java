/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.configurations.service.ConfigurationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class ConfigurationsEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "configurations")
public class ConfigurationsEndpoint extends BaseEndpoint {


    /** The configurations service. */
    private final ConfigurationsService configurationsService;

    /**
     * Instantiates a new configurations endpoint.
     *
     * @param configurationsService the configurations service
     */
    @Autowired
    public ConfigurationsEndpoint(ConfigurationsService configurationsService) {
        this.configurationsService = configurationsService;
    }

    /**
     * Find all.
     *
     * @return the response entity
     */
    @GetMapping
    public ResponseEntity<List<List<String>>> findAll() {
        return ResponseEntity.ok(configurationsService.findAll());
    }

}
