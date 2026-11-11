/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.eclipse.dirigible.components.base.http.roles.Roles.RoleNames;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The export of the current tenant, for its owners: start one, see where it stands, download its
 * archive, delete it.
 *
 * <p>
 * The archive holds the application's tables of the tenant's database and every document the tenant
 * stored. <b>No route takes a data source, a schema or a path from the caller</b>: the tenant is
 * always the caller's current tenant, and what is exported is decided here. That is what sets these
 * routes apart from the administrators' export routes, which name a data source and are not for
 * owners.
 *
 * <p>
 * Protected like every other service, with {@code @RolesAllowed}: the
 * {@link ApplicationRoles#OWNER} of the tenant, a developer or an administrator. The family exists
 * only when the export is enabled, so every route answers 404 when it is not.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "tenant-export")
@Conditional(TenantExportEnabledCondition.class)
@RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
class TenantExportEndpoint extends BaseEndpoint {

    private final TenantExportService service;
    private final TenantExportArchives archives;
    private final TenantContext tenantContext;

    TenantExportEndpoint(TenantExportService service, TenantExportArchives archives, TenantContext tenantContext) {
        this.service = service;
        this.archives = archives;
        this.tenantContext = tenantContext;
    }

    /**
     * What a page needs before it offers the export; reaching it at all says the caller may export.
     *
     * @return how many archives the tenant may keep
     */
    @GetMapping(path = "/context", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> context() {
        return ResponseEntity.ok(Map.of("maxArchives", TenantExportService.maxArchives()));
    }

    /**
     * Starts an export of the current tenant.
     *
     * @return 202 with the new export; 409 while another one runs or the tenant keeps as many archives
     *         as it may
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantExportView> start() {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(service.request(tenantId(), requestedBy()));
    }

    /**
     * The exports of the current tenant.
     *
     * @return the exports, newest first
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TenantExportView>> list() {
        return ResponseEntity.ok(service.list(tenantId()));
    }

    /**
     * One export of the current tenant.
     *
     * @param id the export
     * @return the export; 404 for an unknown one or one of another tenant
     */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantExportView> get(@PathVariable("id") long id) {
        return ResponseEntity.ok(service.get(tenantId(), id));
    }

    /**
     * Streams an export's archive - from the CMS to the response, never through memory.
     *
     * @param id the export
     * @param response the response the archive is written to
     * @throws IOException when the archive cannot be read or the response cannot be written
     */
    @GetMapping(path = "/{id}/archive")
    void archive(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
        TenantExport export = service.downloadable(tenantId(), id);
        String path = export.getArchivePath();
        try (InputStream content = archives.open(path)) {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/zip");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + path.substring(path.lastIndexOf('/') + 1) + "\"");
            if (export.getSizeBytes() != null) {
                response.setContentLengthLong(export.getSizeBytes());
            }
            content.transferTo(response.getOutputStream());
        }
    }

    /**
     * Deletes an export of the current tenant, with its archive.
     *
     * @param id the export
     * @return 204; 409 while it runs
     */
    @DeleteMapping(path = "/{id}")
    ResponseEntity<Void> delete(@PathVariable("id") long id) {
        service.delete(tenantId(), id);
        return ResponseEntity.noContent()
                             .build();
    }

    private String tenantId() {
        return tenantContext.getCurrentTenant()
                            .getId();
    }

    /**
     * Who asks, as the row records it: the lower-cased email of a signed-in OAuth2/OIDC person, else
     * the principal's name. It is shown beside the export and blanked by the person's erasure - never
     * an authorization input.
     */
    private static String requestedBy() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal
                && principal.getAttribute("email") instanceof String email && !email.isBlank()) {
            return email.trim()
                        .toLowerCase(Locale.ROOT);
        }
        return authentication.getName();
    }
}
