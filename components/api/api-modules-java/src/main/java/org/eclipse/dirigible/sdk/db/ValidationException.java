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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown by client-side domain logic — a generated repository's declarative {@code checks:} gate, a
 * capacity guard, or a hand-written validation — to signal that a well-formed request violates a
 * business rule. The Java controller runtime maps it to HTTP {@code 400 Bad Request} carrying the
 * message, so a user-fixable validation surfaces as a client error instead of an opaque
 * {@code 500}.
 *
 * <p>
 * Throwing it from the repository keeps the persistence layer free of any web dependency: the
 * HTTP-status mapping lives once in the controller dispatcher, not in every controller. Raised on a
 * non-HTTP path (e.g. a BPMN service task), it simply fails that unit of work with its message, the
 * same as any other unchecked exception.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The translation key the message was resolved from, or {@code null} for a literal message. */
    private final String messageKey;

    /** The values interpolated into the message's placeholders, by placeholder name. */
    private final transient Map<String, Object> messageParams;

    /**
     * Creates a validation exception.
     *
     * @param message the user-facing reason the request was rejected
     */
    public ValidationException(String message) {
        this(message, null, null);
    }

    /**
     * Creates a validation exception whose message was resolved from a translation catalog (issue
     * #7611) - a generated {@code checks:} refusal. The REST error body carries the key and the
     * parameters next to the resolved text, so a client can render the message again in another
     * language.
     *
     * @param message the user-facing reason, already resolved for the request's language
     * @param messageKey the fully qualified translation key ({@code <project>:<catalog>.checks.<key>})
     * @param messageParams the placeholder values interpolated into the message, or {@code null}
     */
    public ValidationException(String message, String messageKey, Map<String, Object> messageParams) {
        super(message);
        this.messageKey = messageKey;
        this.messageParams = messageParams == null || messageParams.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(messageParams));
    }

    /**
     * Creates a validation exception with an underlying cause.
     *
     * @param message the user-facing reason the request was rejected
     * @param cause the underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = cause instanceof ValidationException validation ? validation.getMessageKey() : null;
        this.messageParams = cause instanceof ValidationException validation ? validation.getMessageParams() : Collections.emptyMap();
    }

    /**
     * The translation key the message was resolved from.
     *
     * @return the fully qualified key, or {@code null} for a literal message
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * The values interpolated into the message's placeholders.
     *
     * @return placeholder name to value; empty when there are none
     */
    public Map<String, Object> getMessageParams() {
        return messageParams == null ? Collections.emptyMap() : messageParams;
    }

}
