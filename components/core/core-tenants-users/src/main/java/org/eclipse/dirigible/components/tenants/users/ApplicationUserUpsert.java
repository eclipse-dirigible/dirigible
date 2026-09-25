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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the provisioning system records for a user: the outcome of one request.
 *
 * @param email the user's email
 * @param role the role the request was for, with the group's casing
 * @param status {@code INVITED}, {@code ASSIGNED} or {@code FAILED}
 * @param requestId the request's message id, or null
 * @param errorMessage the failure text when the status is {@code FAILED}, optional
 * @param updatedBy the person on whose behalf the change is made - never the machine client
 */
record ApplicationUserUpsert(
        @NotBlank @Size(max = 320) @Pattern(regexp = ApplicationUserUpsert.EMAIL, message = "must be an email address") String email,
        @NotBlank @Size(max = 100) String role,
        @NotBlank @Pattern(regexp = "INVITED|ASSIGNED|FAILED", message = "must be INVITED, ASSIGNED or FAILED") String status,
        @Size(max = 64) String requestId, @Size(max = 2000) String errorMessage, @NotBlank @Size(max = 320) String updatedBy) {

    /** An address with a local part, an at sign and a dotted domain. */
    static final String EMAIL = "^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$";
}
