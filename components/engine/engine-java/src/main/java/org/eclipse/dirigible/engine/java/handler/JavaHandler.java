/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SPI implemented by client Java sources that wish to handle a Dirigible REST endpoint.
 *
 * <p>
 * Sources placed under a project in {@code /registry/public/<project>/} that compile to a class
 * implementing this interface become accessible at
 * {@code /services/java/<project>/<package-path>/<ClassName>} once the synchronizer picks them up.
 * The class must have a public no-arg constructor.
 *
 * <p>
 * Implementations are reloaded on every source change; do not retain process-global state on the
 * instance. If you need to expose state across requests, store it externally (database, cache).
 */
public interface JavaHandler {

    /**
     * Handle a request. Write the response via {@code response}.
     *
     * @param request the incoming HTTP request
     * @param response the response to write to
     * @throws Exception any failure is mapped to a 500 by the dispatching endpoint
     */
    void handle(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
