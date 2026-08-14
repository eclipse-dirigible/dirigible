/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.conversation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * What a stored conversation message is.
 *
 * <p>
 * {@link #USER} and {@link #ASSISTANT} are the transcript proper - the alternating dialogue the
 * model API is fed on the next turn; {@link #NOTE}, {@link #ERROR} and {@link #BOUNDARY} are
 * record-only, so a client rebuilding its upstream history from a restored conversation simply
 * keeps the first two.
 */
public enum ConversationRole {

    /** What the developer asked. */
    USER,
    /** What the assistant answered. */
    ASSISTANT,
    /** A UI note the client showed, e.g. that a proposal was applied. */
    NOTE,
    /** A failure the client showed, e.g. a proposal that did not validate. */
    ERROR,
    /**
     * A requirement the proposal could not express, reported next to it. Part of the record - it is
     * what the developer still has to build, and what tells the platform which gap a real project hit -
     * but never replayed to the model, which said it in the first place.
     */
    BOUNDARY;

    /**
     * The wire name - lower case, matching what both clients already put on their message objects.
     *
     * @return the wire name
     */
    @JsonValue
    public String wireName() {
        return name().toLowerCase();
    }

    /**
     * Resolves a wire name, case-insensitively.
     *
     * @param value the wire name
     * @return the role
     * @throws IllegalArgumentException when the value names no role
     */
    @JsonCreator
    public static ConversationRole fromWireName(String value) {
        return valueOf(String.valueOf(value)
                             .toUpperCase());
    }
}
