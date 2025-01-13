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

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.web.service.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class WebEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "web", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "web"})
public class WebEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WebEndpoint.class.getCanonicalName());


    /** The javascript service. */
    private final WebService webService;

    /**
     * Instantiates a new web endpoint.
     *
     * @param webService the web service
     */
    @Autowired
    public WebEndpoint(WebService webService) {
        this.webService = webService;
    }

    /**
     * Gets the page.
     *
     * @param path the file path
     * @return the response
     */
    @GetMapping("/{*path}")
    public ResponseEntity get(@PathVariable("path") String path) {
        return webService.getResource(path);
    }

}
