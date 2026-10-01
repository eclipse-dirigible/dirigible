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

import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningError;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Answers the users snapshot endpoint's refusals with the tenant provisioning API's error body,
 * {@code {status, error, message}}, so a caller handles every endpoint of that API the same way.
 */
@RestControllerAdvice(assignableTypes = {TenantUserSnapshotEndpoint.class})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantUserSnapshotExceptionHandler {

    /**
     * Renders a refusal.
     *
     * @param ex the exception
     * @return the status it carries, with its reason
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<TenantProvisioningError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String name = resolved == null ? "Error" : resolved.getReasonPhrase();
        return ResponseEntity.status(status)
                             .body(new TenantProvisioningError(status.value(), name, ex.getReason()));
    }
}
