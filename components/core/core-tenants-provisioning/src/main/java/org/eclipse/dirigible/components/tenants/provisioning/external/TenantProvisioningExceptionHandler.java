/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.util.stream.Collectors;

import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Puts the reason a call was refused into the response body.
 *
 * <p>
 * The caller of this API is a program driving a provisioning process, and every refusal it can get
 * is one it has to act on differently: a password the database rejected, a data source that has to
 * be registered first, a subdomain another tenant holds. Without the reason it sees "Bad Gateway"
 * and can only give up, and the operator reading its logs learns nothing either. The platform's
 * {@code server.error.include-message=always} was meant to cover this and does not reach a
 * {@code ResponseStatusException} raised here, so the body is written explicitly rather than left
 * to chance - it is part of what this API promises.
 *
 * <p>
 * Scoped to this component's endpoints: how the rest of the platform renders its errors is not this
 * feature's business to change.
 */
@RestControllerAdvice(assignableTypes = {TenantProvisioningEndpoint.class, TenantDataSourceProvisioningEndpoint.class})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantProvisioningExceptionHandler {

    /**
     * Renders a refusal raised by this component.
     *
     * @param ex the exception
     * @return the status it carries, with its reason in the body
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<TenantProvisioningError> handleResponseStatus(ResponseStatusException ex) {
        return body(ex.getStatusCode(), ex.getReason());
    }

    /**
     * Renders a body that failed validation, naming the fields rather than only their count.
     *
     * @param ex the exception
     * @return 400 with the offending fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<TenantProvisioningError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                           .getFieldErrors()
                           .stream()
                           .map(TenantProvisioningExceptionHandler::describe)
                           .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, message.isEmpty() ? "The request body is not valid" : message);
    }

    /**
     * Describe.
     *
     * @param error the field error
     * @return the field and what is wrong with it
     */
    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    /**
     * Body.
     *
     * @param status the status
     * @param message the message
     * @return the response
     */
    private static ResponseEntity<TenantProvisioningError> body(HttpStatusCode status, String message) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String name = resolved == null ? "Error" : resolved.getReasonPhrase();
        return ResponseEntity.status(status)
                             .body(new TenantProvisioningError(status.value(), name, message));
    }
}
