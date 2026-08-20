/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lets a logged in user see which tenants of this application they may enter, and enter one.
 *
 * <p>
 * A {@code POST} is also how a user switches tenant - the selection and the authorities are
 * replaced together, with no re-login.
 *
 * <p>
 * Deliberately without a role gate: before a tenant is selected a user has only their global roles,
 * and a user of a single tenant has none at all - requiring a role here would lock out exactly the
 * people who need to pick. The URL is gated as authenticated, which is the requirement that
 * matters. A body is required to be JSON, which is what keeps a cross-origin form from posting a
 * selection (the security chains disable CSRF tokens).
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenant-selection")
public class TenantSelectionEndpoint {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSelectionEndpoint.class);

    private final TenantSelectionManager tenantSelectionManager;

    /**
     * Instantiates a new tenant selection endpoint.
     *
     * @param tenantSelectionManager the tenant selection manager
     */
    public TenantSelectionEndpoint(TenantSelectionManager tenantSelectionManager) {
        this.tenantSelectionManager = tenantSelectionManager;
    }

    /**
     * The tenants the user may enter, and which of them is selected.
     *
     * @param request the request
     * @return the selection state
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantSelectionState> state(HttpServletRequest request) {
        requireTokenGroupsStrategy();
        List<TenantOption> tenants = tenantSelectionManager.availableTenants(SecurityContextHolder.getContext()
                                                                                                  .getAuthentication());
        return ResponseEntity.ok(new TenantSelectionState(tenantSelectionManager.selectedTenantId(request), tenants));
    }

    /**
     * Enters a tenant, or switches to it.
     *
     * @param selection the tenant to enter
     * @param request the request
     * @param response the response
     * @return the tenant and the roles the user now has in it
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantSelectionResult> select(@RequestBody TenantSelectionRequest selection, HttpServletRequest request,
            HttpServletResponse response) {
        requireTokenGroupsStrategy();
        if (selection == null || selection.tenantId() == null || selection.tenantId()
                                                                          .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A tenant id is required");
        }
        String tenantId = selection.tenantId()
                                   .trim();
        Set<String> roles = tenantSelectionManager.selectTenant(request, response, tenantId);
        return ResponseEntity.ok(new TenantSelectionResult(tenantId, roles));
    }

    /**
     * Answers a refused selection.
     *
     * @param exception the refusal
     * @return the response
     */
    @ExceptionHandler(TenantSelectionException.class)
    public ResponseEntity<TenantSelectionRefusal> onRefusedSelection(TenantSelectionException exception) {
        LOGGER.info("Refused tenant selection [{}]: {}", exception.getTenantId(), exception.getMessage());
        HttpStatus status = switch (exception.getReason()) {
            case NOT_A_MEMBER -> HttpStatus.FORBIDDEN;
            case NOT_PROVISIONED_HERE -> HttpStatus.CONFLICT;
            case NOT_AN_INTERACTIVE_SESSION -> HttpStatus.UNAUTHORIZED;
        };
        return ResponseEntity.status(status)
                             .body(new TenantSelectionRefusal(exception.getReason()
                                                                       .name(),
                                     exception.getMessage()));
    }

    /**
     * The endpoint exists only where a tenant is something a user selects.
     */
    private void requireTokenGroupsStrategy() {
        if (TenantResolutionStrategy.fromConfiguration() != TenantResolutionStrategy.TOKEN_GROUPS) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenants are not selected in this deployment");
        }
    }

    /**
     * What the user may enter, and what they entered.
     *
     * @param selectedTenantId the selected tenant id, {@code null} when none is selected
     * @param tenants the tenants the user may enter
     */
    public record TenantSelectionState(String selectedTenantId, List<TenantOption> tenants) {
    }

    /**
     * A request to enter a tenant.
     *
     * @param tenantId the tenant id
     */
    public record TenantSelectionRequest(String tenantId) {
    }

    /**
     * The outcome of entering a tenant.
     *
     * @param tenantId the tenant entered
     * @param roles the roles the user has now
     */
    public record TenantSelectionResult(String tenantId, Set<String> roles) {
    }

    /**
     * A refused selection.
     *
     * @param reason why it was refused
     * @param message the human readable explanation
     */
    public record TenantSelectionRefusal(String reason, String message) {
    }
}
