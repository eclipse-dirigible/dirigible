/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package demo;

import org.eclipse.dirigible.sdk.net.OnMessage;
import org.eclipse.dirigible.sdk.net.Websocket;
import org.eclipse.dirigible.sdk.net.WebsocketHandler;

/**
 * Mixes both handler styles — implements {@code WebsocketHandler} (interface) AND carries
 * {@code @Websocket} + {@code @OnMessage} (annotation). The engine must reject it (not wire the
 * endpoint).
 */
@Websocket(name = "Mixed", endpoint = "mixed-no-mixing")
public class MixedSocket implements WebsocketHandler {
    public String endpoint() {
        return "mixed-no-mixing";
    }

    @OnMessage
    public void onMessage(String message, String from) {}
}
