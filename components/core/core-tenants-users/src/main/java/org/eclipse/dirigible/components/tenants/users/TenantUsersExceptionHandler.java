/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Answers the tenant users endpoints' refusals with a reason the page can translate.
 */
@RestControllerAdvice(assignableTypes = {TenantUsersEndpoint.class})
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersExceptionHandler {

    /**
     * Renders a refusal of these endpoints.
     *
     * @param ex the refusal
     * @return the body
     */
    @ExceptionHandler(TenantUsersException.class)
    ResponseEntity<TenantUsersRefusal> handleRefusal(TenantUsersException ex) {
        return ResponseEntity.status(ex.status())
                             .body(new TenantUsersRefusal(ex.status()
                                                            .value(),
                                     ex.status()
                                       .getReasonPhrase(),
                                     ex.getMessage(), ex.reason()));
    }

    /**
     * Renders a caller refused by the endpoints' {@code @RolesAllowed} - in the same shape, so a page
     * reads one reason for it.
     *
     * @param ex the refusal
     * @return the body
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<TenantUsersRefusal> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                             .body(new TenantUsersRefusal(status.value(), status.getReasonPhrase(),
                                     "Only a holder of the [" + ApplicationRoles.OWNER + "] role of this tenant may manage its users",
                                     "NOT_A_TENANT_OWNER"));
    }

    /**
     * Renders a refusal raised by the users registry.
     *
     * @param ex the refusal
     * @return the body
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<TenantUsersRefusal> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode()
                                                 .value());
        String name = status == null ? "Error" : status.getReasonPhrase();
        return ResponseEntity.status(ex.getStatusCode())
                             .body(new TenantUsersRefusal(ex.getStatusCode()
                                                            .value(),
                                     name, ex.getReason(), name.toUpperCase()
                                                               .replace(' ', '_')));
    }
}
