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
 * Thrown when a write collided with one of the entity's business keys (a {@code unique:} key, a
 * {@code unique: true} field, or a {@code single: { onConflict: refuse }} flag another record
 * holds) - the refusal the database raised, or a key check found first, carrying the message
 * authored for that key instead of the driver's text. The Java controller runtime maps it to HTTP
 * {@code 409 Conflict}: the request is well-formed and refused only because another record holds
 * the same key right now. Being a {@link ValidationException}, it carries the translation key, its
 * parameters and the field the refusal is about, and a non-HTTP caller that skips refused rows (a
 * schedule's {@code onRefused: skip}) skips a duplicate as it skips any other refusal.
 */
public class DuplicateKeyException extends ValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the refusal of a write that collided with a business key.
     *
     * @param refusal the key's refusal - its message, translation key, parameters and field
     * @param violation the database failure that reported the collision
     */
    public DuplicateKeyException(ValidationException refusal, Throwable violation) {
        super(refusal.getMessage(), refusal.getMessageKey(), refusal.getMessageParams(), refusal.getField());
        initCause(violation);
    }

    /**
     * Creates the refusal of a write that a key check found colliding before it reached the database -
     * a case-insensitive {@code unique:} key or a refusing {@code single:} flag, which no database
     * constraint enforces.
     *
     * @param refusal the key's refusal - its message, translation key, parameters and field
     */
    public DuplicateKeyException(ValidationException refusal) {
        super(refusal.getMessage(), refusal.getMessageKey(), refusal.getMessageParams(), refusal.getField());
    }

    /**
     * Creates the refusal of a duplicate restated in other words - a generated line naming the source
     * row it was mapped from - keeping the translation key, parameters and field of the original.
     *
     * @param message the user-facing reason
     * @param duplicate the refusal it restates
     */
    public DuplicateKeyException(String message, DuplicateKeyException duplicate) {
        super(message, duplicate);
    }

}
