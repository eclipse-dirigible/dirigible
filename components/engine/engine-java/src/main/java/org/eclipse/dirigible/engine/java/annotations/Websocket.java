/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as a WebSocket handler managed by the Dirigible runtime.
 *
 * <p>
 * The annotated class may expose any combination of the following public methods:
 * <ul>
 * <li>{@code onOpen()} — called when a client connects</li>
 * <li>{@code onMessage(String message, String from)} — called for each inbound message</li>
 * <li>{@code onError(String error)} — called on transport or handler error</li>
 * <li>{@code onClose()} — called when the connection is closed</li>
 * </ul>
 * All methods are optional; missing ones are silently skipped.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Websocket(name = "chat", endpoint = "chat")
 * public class ChatHandler {
 *     public void onOpen() { ... }
 *     public void onMessage(String message, String from) { ... }
 *     public void onClose() { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Websocket {

    /** Logical display name for the websocket. */
    String name();

    /**
     * URL endpoint suffix used by the client to connect, e.g. {@code "chat"} maps to
     * {@code /websockets/stomp/chat}.
     */
    String endpoint();

}
