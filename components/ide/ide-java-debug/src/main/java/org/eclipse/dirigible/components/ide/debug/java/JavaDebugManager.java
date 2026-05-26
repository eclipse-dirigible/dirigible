/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.debug.java;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsInstance;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-workspace DAP bridge instances.
 *
 * <p>
 * When a debug WebSocket connects, this manager asks the workspace's JDT.LS instance to start a DAP
 * server (via the {@code vscode.java.startDebugSession} LSP command contributed by the
 * {@code com.microsoft.java.debug.plugin}), then creates a {@link JavaDebugBridge} that bridges the
 * returned TCP port to the browser WebSocket session.
 *
 * <p>
 * If the debug plugin is not installed in the JDT.LS plugins directory, the
 * {@code workspace/executeCommand} call will fail with an "unknown command" error. In that case
 * place {@code com.microsoft.java.debug.plugin-*.jar} into the JDT.LS {@code plugins/} directory
 * (e.g. {@code ~/.dirigible/jdtls/plugins/}) and restart the application.
 */
@Component
public class JavaDebugManager implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(JavaDebugManager.class);

    /** key = "username/workspace" → active bridge */
    private final Map<String, JavaDebugBridge> bridges = new ConcurrentHashMap<>();

    @Autowired
    private JdtLsManager lspManager;

    /**
     * Returns (and lazily creates) the {@link JavaDebugBridge} for the given workspace. If no bridge
     * exists yet, this method sends {@code workspace/executeCommand → vscode.java.startDebugSession} to
     * the workspace's JDT.LS instance, connects to the returned DAP TCP port, and creates a new bridge.
     * Thread-safe; at most one bridge is ever created per workspace key.
     */
    public JavaDebugBridge getOrStart(String username, String workspace) throws Exception {
        String key = username + "/" + workspace;
        JavaDebugBridge existing = bridges.get(key);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        synchronized (this) {
            existing = bridges.get(key);
            if (existing != null && existing.isAlive()) {
                return existing;
            }
            if (existing != null) {
                existing.destroy();
                bridges.remove(key);
            }
            JavaDebugBridge fresh = startBridge(username, workspace);
            bridges.put(key, fresh);
            return fresh;
        }
    }

    /** Finds the bridge that owns the given WebSocket session ID. */
    public JavaDebugBridge findForSession(String sessionId) {
        for (JavaDebugBridge bridge : bridges.values()) {
            if (bridge.hasSession(sessionId)) {
                return bridge;
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        logger.info("[java-debug] Shutting down {} bridge(s)", bridges.size());
        bridges.values()
               .forEach(JavaDebugBridge::destroy);
        bridges.clear();
    }

    // -------------------------------------------------------------------------

    private JavaDebugBridge startBridge(String username, String workspace) throws Exception {
        JdtLsInstance lspInstance = lspManager.getOrStart(username, workspace);

        // JDT.LS queues workspace commands until the LSP initialize handshake completes. Perform it
        // from the server side when the browser editor hasn't connected yet (no-op otherwise).
        lspInstance.ensureInitialized()
                   .get(60, TimeUnit.SECONDS);

        logger.info("[java-debug] Requesting DAP server from JDT.LS for {}/{}", sanitize(username), sanitize(workspace));

        JsonNode response =
                lspInstance.sendRequest("workspace/executeCommand", "{\"command\":\"vscode.java.startDebugSession\",\"arguments\":[]}")
                           .get(30, TimeUnit.SECONDS);

        // vscode.java.startDebugSession returns the port as a direct integer: {"result": 59683}
        // not as a nested object {"result": {"port": 59683}}.
        JsonNode result = response.path("result");
        int port = result.isInt() ? result.asInt()
                : result.path("port")
                        .asInt(0);
        if (port <= 0) {
            throw new IllegalStateException(
                    "[java-debug] vscode.java.startDebugSession returned no port — is com.microsoft.java.debug.plugin installed? Response: "
                            + response);
        }
        logger.info("[java-debug] Connecting to DAP server on localhost:{}", port);
        Socket dapSocket = new Socket("localhost", port);
        return new JavaDebugBridge(dapSocket);
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }
}
