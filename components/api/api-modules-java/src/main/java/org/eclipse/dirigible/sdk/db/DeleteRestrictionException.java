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
 * Thrown to refuse a delete while another record still references it through a relation that
 * restricts it (intent {@code whenTargetDeleted: restrict}, see {@link TargetDeleteRule}). The Java
 * controller runtime maps it to HTTP {@code 409 Conflict} carrying the message, distinct from
 * {@link ValidationException}'s {@code 400} because this is not a malformed request - the request
 * is well-formed and refused only because of other rows that exist right now. A refusal built by
 * {@link DeleteRefusals} carries the translation key its message was resolved from and the
 * placeholder values, which the error body carries next to the message (dirigible #8569).
 */
public class DeleteRestrictionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The translation key the message was resolved from, or {@code null} for a literal message. */
    private final String messageKey;

    /** The values interpolated into the message's placeholders, by placeholder name. */
    private final transient Map<String, Object> messageParams;

    /**
     * Creates a delete-restriction exception.
     *
     * @param message the user-facing reason the delete was refused
     */
    public DeleteRestrictionException(String message) {
        this(message, null, null);
    }

    /**
     * Creates a delete-restriction exception whose message was resolved from a translation catalog
     * (dirigible #8569). The REST error body carries the key and the parameters next to the resolved
     * text, so a client can render the message again in another language.
     *
     * @param message the user-facing reason, already resolved for the request's language
     * @param messageKey the fully qualified translation key ({@code <project>:<catalog>.checks.<key>})
     * @param messageParams the placeholder values interpolated into the message, or {@code null}
     */
    public DeleteRestrictionException(String message, String messageKey, Map<String, Object> messageParams) {
        super(message);
        this.messageKey = messageKey;
        this.messageParams = messageParams == null || messageParams.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(messageParams));
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
