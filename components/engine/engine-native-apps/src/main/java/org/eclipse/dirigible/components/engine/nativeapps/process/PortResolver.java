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
import java.net.InetAddress;
import java.net.ServerSocket;

public final class PortResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortResolver.class);

    private PortResolver() {}

    /**
     * Resolves the port to bind a native app to.
     *
     * <p>
     * If {@code preferred} is non-null and bindable on the loopback interface, it is returned.
     * Otherwise the OS hands out a free ephemeral port (a small race against an unrelated process
     * grabbing the same port between probe and child start is accepted — same trade-off used elsewhere
     * in the platform; see {@code PortUtil#getFreeRandomPort} for SFTP).
     */
    public static int resolve(Integer preferred) {
        if (preferred != null && preferred > 0 && isBindable(preferred)) {
            return preferred;
        }
        if (preferred != null) {
            LOGGER.info("Preferred port [{}] not available; falling back to an ephemeral port.", preferred);
        }
        return allocateEphemeralPort();
    }

    private static boolean isBindable(int port) {
        try (ServerSocket s = new ServerSocket(port, 0, InetAddress.getLoopbackAddress())) {
            s.setReuseAddress(true);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static int allocateEphemeralPort() {
        try (ServerSocket s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not allocate an ephemeral port for the native app process.", ex);
        }
    }
}
