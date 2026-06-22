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
 * Marks a client Java class as a WebSocket handler bound to an endpoint — the annotation style. The
 * class carries the endpoint (which has no method-level home, like Jakarta {@code @ServerEndpoint})
 * and its lifecycle callbacks are public methods annotated {@link OnOpen}, {@link OnMessage},
 * {@link OnError}, {@link OnClose}. All callbacks are optional; missing ones are skipped.
 *
 * <p>
 * {@code @Websocket} is meta-annotated with {@link Component @Component}, so the handler is a
 * managed bean and may declare injected collaborators in its constructor.
 *
 * <p>
 * The alternative, strong-interface style is a {@code @Component} bean implementing
 * {@link WebsocketHandler} (which supplies its own {@code endpoint()}). A class uses one style or
 * the other, never both — annotating a {@code WebsocketHandler} with {@code @Websocket} is
 * rejected.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Websocket(name = "chat", endpoint = "chat")
 * public class ChatHandler {
 *     {@literal @}OnMessage public String message(String message, String from) { return "echo: " + message; }
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
