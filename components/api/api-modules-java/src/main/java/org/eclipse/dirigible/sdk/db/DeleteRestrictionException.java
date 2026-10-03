/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

/**
 * Thrown to refuse a delete while another record still references it through a relation that
 * restricts it (intent {@code whenTargetDeleted: restrict}, see {@link TargetDeleteRule}). The Java
 * controller runtime maps it to HTTP {@code 409 Conflict} carrying the message, distinct from
 * {@link ValidationException}'s {@code 400} because this is not a malformed request - the request
 * is well-formed and refused only because of other rows that exist right now.
 */
public class DeleteRestrictionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a delete-restriction exception.
     *
     * @param message the user-facing reason the delete was refused
     */
    public DeleteRestrictionException(String message) {
        super(message);
    }

}
