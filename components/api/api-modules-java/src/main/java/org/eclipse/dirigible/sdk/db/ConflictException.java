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
 * Thrown when a write is refused because of the record's current state rather than its content - a
 * record its status, its master or its accounting period has fixed, a field fixed once the record
 * exists, a status only the workflow moves, a share link the record cannot have (dirigible #8775).
 * The Java controller runtime maps it to HTTP {@code 409 Conflict}. Being a
 * {@link ValidationException}, it carries the translation key, its parameters and the field the
 * refusal is about, so the error body is the one every other refusal of a generated application
 * carries.
 */
public class ConflictException extends ValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the conflict a refusal describes.
     *
     * @param refusal the refusal - its message, translation key, parameters and field
     */
    public ConflictException(ValidationException refusal) {
        super(refusal.getMessage(), refusal.getMessageKey(), refusal.getMessageParams(), refusal.getField());
    }
}
