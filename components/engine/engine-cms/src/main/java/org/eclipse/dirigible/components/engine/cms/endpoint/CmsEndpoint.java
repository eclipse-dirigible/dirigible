/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class CmsEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "cms", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "cms"})
public class CmsEndpoint extends BaseEndpoint {

    /** The cms service. */
    private final CmsService cmsService;

    /**
     * Instantiates a new cms endpoint.
     *
     * @param cmsService the cms service
     */
    @Autowired
    public CmsEndpoint(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    /**
     * Gets the page.
     *
     * @param path the file path
     * @return the response
     */
    @GetMapping("/{*path}")
    public ResponseEntity get(@PathVariable("path") String path) {
        return cmsService.getResource(path);
    }

}
