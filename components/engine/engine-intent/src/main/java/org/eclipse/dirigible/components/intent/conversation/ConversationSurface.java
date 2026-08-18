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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Where a conversation happened. Two surfaces host the assistant today, and each keeps its own
 * conversation per application.
 *
 * <p>
 * A closed set on purpose: an arbitrary surface string would let a typo create a parallel
 * conversation that is written to and never restored from, with nothing anywhere reporting it.
 */
public enum ConversationSurface {

    /** The Builder shell at {@code /services/web/builder/}. */
    BUILDER("builder"),
    /** The Intent Editor's chat pane in the Web IDE. */
    INTENT_EDITOR("intent-editor"),
    /** The Workbench's Assistant view, one conversation per hand-written Java file. */
    WORKBENCH("workbench");

    /** The wire name. */
    private final String wireName;

    /**
     * Instantiates a new conversation surface.
     *
     * @param wireName the wire name
     */
    ConversationSurface(String wireName) {
        this.wireName = wireName;
    }

    /**
     * The name clients use in the request and the value stored in the table.
     *
     * @return the wire name
     */
    public String wireName() {
        return wireName;
    }

    /**
     * Resolves a wire name.
     *
     * @param value the wire name
     * @return the surface
     * @throws IllegalArgumentException when the value names no surface
     */
    public static ConversationSurface of(String value) {
        return Arrays.stream(values())
                     .filter(surface -> surface.wireName.equalsIgnoreCase(value))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Unknown conversation surface [" + value + "]. Expected one of "
                             + Arrays.stream(values())
                                     .map(ConversationSurface::wireName)
                                     .collect(Collectors.joining(", "))));
    }
}
