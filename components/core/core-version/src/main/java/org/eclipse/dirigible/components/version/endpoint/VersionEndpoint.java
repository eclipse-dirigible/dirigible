/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.version.endpoint;


import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.version.domain.Version;
import org.eclipse.dirigible.components.version.service.VersionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class VersionEndpoint.
 */

@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "version")
public class VersionEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(VersionEndpoint.class);

    /**
     * The version service.
     */
    @Autowired
    private VersionService versionService;

    /**
     * Gets the version.
     *
     * @return the version
     * @throws Exception the exception
     */
    @GetMapping
    public ResponseEntity<Version> getVersion() throws Exception {
        return ResponseEntity.ok(versionService.getVersion());
    }
}
