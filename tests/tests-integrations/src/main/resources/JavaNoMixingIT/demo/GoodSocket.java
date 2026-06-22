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

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.net.WebsocketHandler;

/**
 * Clean interface-style WebSocket handler (no class annotation) — the positive control that proves
 * wiring works at all.
 */
@Component
public class GoodSocket implements WebsocketHandler {
    public String endpoint() {
        return "good-no-mixing";
    }

    public void onMessage(String message, String from) {}
}
