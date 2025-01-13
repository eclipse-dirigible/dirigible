/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.domain;

import jakarta.persistence.AttributeConverter;

/**
 * The Class ListenerKindConverter.
 */
public class ListenerKindConverter implements AttributeConverter<ListenerKind, Character> {

    /** The Constant QUEUE_CHAR. */
    private static final char QUEUE_CHAR = 'Q';

    /** The Constant TOPIC_CHAR. */
    private static final char TOPIC_CHAR = 'T';

    /**
     * Convert to database column.
     *
     * @param from the from
     * @return the character
     */
    @Override
    public Character convertToDatabaseColumn(ListenerKind from) {
        if (ListenerKind.QUEUE.equals(from)) {
            return QUEUE_CHAR;
        }
        if (ListenerKind.TOPIC.equals(from)) {
            return TOPIC_CHAR;
        }
        throw new IllegalArgumentException("Unsupported listener kind: " + from);
    }

    /**
     * Convert to entity attribute.
     *
     * @param to the to
     * @return the listener kind
     */
    @Override
    public ListenerKind convertToEntityAttribute(Character to) {
        if (to == null) {
            throw new IllegalArgumentException("Listener kind char cannot be null");
        }

        char uppercased = Character.toUpperCase(to);
        if (uppercased == QUEUE_CHAR) {
            return ListenerKind.QUEUE;
        }
        if (uppercased == TOPIC_CHAR) {
            return ListenerKind.TOPIC;
        }
        throw new IllegalArgumentException("Unsupported listener kind: " + to);
    }
}
