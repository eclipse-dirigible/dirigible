/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.net;

/**
 * Optional typed contract for a {@link Websocket @Websocket} class. Implementing this interface is
 * not required — the runtime still accepts any class that exposes the lifecycle methods by name —
 * but implementations gain compile-time signature checking and only need to override the callbacks
 * they care about; the rest inherit empty default behaviour.
 *
 * <p>
 * The {@code @Websocket} annotation remains the marker that registers the class for an endpoint;
 * this interface only describes the lifecycle callback shapes.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Websocket(name = "Java Chat", endpoint = "java-chat")
 * public class ChatHandler implements WebsocketHandler {
 *     {@literal @}Override public void onMessage(String message, String from) { ... }
 *     // onOpen, onError, onClose inherit the no-op default
 * }
 * </pre>
 */
public interface WebsocketHandler {

    /** Fires once when a client opens the WebSocket. */
    default void onOpen() {
        // intentional no-op default
    }

    /**
     * Fires for every inbound text frame.
     *
     * @param message the text payload
     * @param from a stable identifier for the originating session
     */
    default void onMessage(String message, String from) {
        // intentional no-op default
    }

    /** Fires when the underlying transport reports an error. */
    default void onError(String error) {
        // intentional no-op default
    }

    /** Fires once when the client closes the WebSocket. */
    default void onClose() {
        // intentional no-op default
    }
}
