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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.dirigible.sdk.component.Component;

/**
 * Marks a client Java class as a WebSocket handler bound to an endpoint. The lifecycle callbacks
 * can be supplied in any of three styles:
 * <ul>
 * <li>implement {@link WebsocketHandler} (typed, compile-checked) — override only what you
 * need;</li>
 * <li>annotate methods with {@link OnOpen}, {@link OnMessage}, {@link OnError}, {@link OnClose}
 * (Jakarta-WebSocket flavour);</li>
 * <li>expose the lifecycle methods by name ({@code onOpen()}, {@code onMessage(String, String)},
 * {@code onError(String)}, {@code onClose()}) — the reflective fallback.</li>
 * </ul>
 * All callbacks are optional; missing ones are skipped.
 *
 * <p>
 * {@code @Websocket} is meta-annotated with {@link Component @Component}, so the handler is a
 * managed bean and may declare injected collaborators in its constructor.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Websocket(name = "chat", endpoint = "chat")
 * public class ChatHandler implements WebsocketHandler {
 *     {@literal @}Override public void onMessage(String message, String from) { ... }
 * }
 * </pre>
 */
@Component
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
