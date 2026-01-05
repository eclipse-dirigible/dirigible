/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.web.domain.Expose;
import org.eclipse.dirigible.components.engine.web.service.ExposeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class ExposeEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "exposes")
public class ExposeEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExposeEndpoint.class);

    /**
     * The expose service.
     */
    @Autowired
    private ExposeService exposeService;

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<Expose>> getAll() {
        return ResponseEntity.ok(exposeService.getAll());
    }

}
