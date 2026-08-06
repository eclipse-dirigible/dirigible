/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.access;

import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Grants and revokes access to CMS paths at runtime - the surface behind the "Manage access" dialog
 * in both Documents user interfaces.
 * <p>
 * Managing access is an administrative act, hence the role guard, even though reading documents is
 * not (see the Documents endpoint).
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "documents/access")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class CmsAccessEndpoint extends BaseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmsAccessEndpoint.class);

    private final CmsAccessService accessService;

    CmsAccessEndpoint(CmsAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * What decides a path: the grants declared on it and those it inherits.
     *
     * @param path the CMS path
     * @return the effective grants
     */
    @GetMapping
    public ResponseEntity<CmsAccessService.EffectiveGrants> get(@RequestParam(value = "path", required = false) String path) {
        return ResponseEntity.ok(accessService.effectiveGrants(path));
    }

    /**
     * Grants a role read or write access to a path.
     *
     * @param grant the grant
     * @param request the current request
     * @return the path's grants after the change
     */
    @PutMapping
    public ResponseEntity<CmsAccessService.EffectiveGrants> grant(@RequestBody CmsAccessGrant grant, HttpServletRequest request) {
        validate(grant);
        try {
            accessService.grant(grant.path(), grant.method(), grant.role(), request.getRemoteUser());
        } catch (SQLException e) {
            LOGGER.error("Failed to grant [{}] on [{}] to [{}]", grant.method(), grant.path(), grant.role(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The grant could not be stored");
        }
        return ResponseEntity.ok(accessService.effectiveGrants(grant.path()));
    }

    /**
     * Revokes a grant.
     *
     * @param grant the grant
     * @param request the current request
     * @return the path's grants after the change
     */
    @DeleteMapping
    public ResponseEntity<CmsAccessService.EffectiveGrants> revoke(@RequestBody CmsAccessGrant grant, HttpServletRequest request) {
        validate(grant);
        try {
            accessService.revoke(grant.path(), grant.method(), grant.role(), request.getRemoteUser());
        } catch (SQLException e) {
            LOGGER.error("Failed to revoke [{}] on [{}] from [{}]", grant.method(), grant.path(), grant.role(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The grant could not be removed");
        }
        return ResponseEntity.ok(accessService.effectiveGrants(grant.path()));
    }

    private static void validate(CmsAccessGrant grant) {
        if (grant == null || grant.path() == null || grant.path()
                                                          .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A path must be provided");
        }
        if (grant.role() == null || grant.role()
                                         .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A role must be provided");
        }
        List<String> methods = List.of(CmsAccessGrant.METHOD_READ, CmsAccessGrant.METHOD_WRITE);
        if (grant.method() == null || !methods.contains(grant.method()
                                                             .toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The method must be one of " + methods);
        }
        if (grant.path()
                 .startsWith("/__")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The internal folders are not grantable");
        }
    }
}
