/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public final class PortResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortResolver.class);

    private PortResolver() {}

    /**
     * Resolves the port to bind a native app to.
     *
     * <p>
     * If {@code preferred} is non-null and bindable on the <strong>wildcard</strong> interface
     * ({@code 0.0.0.0} / {@code ::}), it is returned. Otherwise the OS hands out a free ephemeral port.
     * Wildcard probing (rather than loopback-only) matters because typical web servers — Node's
     * Fastify, Spring Boot, Python's {@code http.server} — bind to {@code 0.0.0.0} by default. On macOS
     * in particular, a loopback-only probe ({@code 127.0.0.1:port}) can succeed even when another
     * process is already listening on {@code 0.0.0.0:port}; the child would then die with
     * {@code EADDRINUSE}. A wildcard probe matches the broadest case the spawned process is likely to
     * attempt and detects either listener type.
     *
     * <p>
     * A small race against an unrelated process grabbing the same port between probe and child start is
     * accepted — same trade-off used elsewhere in the platform; see {@code PortUtil#getFreeRandomPort}
     * for SFTP.
     */
    public static int resolve(Integer preferred) {
        if (preferred != null && preferred > 0 && isBindable(preferred)) {
            return preferred;
        }
        if (preferred != null) {
            LOGGER.info("Preferred port [{}] is not bindable on the wildcard interface; falling back to an ephemeral port.", preferred);
        }
        return allocateEphemeralPort();
    }

    private static boolean isBindable(int port) {
        // Bind to the wildcard interface (host = null) so we detect listeners on 0.0.0.0:port as
        // well as loopback-only ones. See the resolve(...) Javadoc for why.
        try (ServerSocket s = new ServerSocket(port, 0, null)) {
            s.setReuseAddress(true);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static int allocateEphemeralPort() {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not allocate an ephemeral port for the native app process.", ex);
        }
    }
}
