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

import java.util.Map;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.engine.java.websocket.JavaWebsocketRegistry;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;

/**
 * Exposes the WebSocket registry state so the test can assert which endpoints were wired.
 */
@Controller
public class StatusController {

    @Get("/{endpoint}")
    public Map<String, Object> status(@PathParam("endpoint") String endpoint) {
        JavaWebsocketRegistry registry = BeanProvider.getBean(JavaWebsocketRegistry.class);
        return Map.of("registered", registry.contains(endpoint));
    }
}
