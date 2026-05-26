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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges browser WebSocket sessions to a single DAP (Debug Adapter Protocol) TCP socket.
 *
 * <p>
 * Messages from the browser arrive as raw JSON over WebSocket; this bridge prepends the
 * {@code Content-Length} header required by the DAP wire format before forwarding to the DAP
 * server. In the opposite direction, Content-Length-framed messages from the DAP server are
 * stripped of their headers and broadcast as raw JSON to all open WebSocket sessions.
 */
public class JavaDebugBridge {

    private static final Logger logger = LoggerFactory.getLogger(JavaDebugBridge.class);

    private final Socket dapSocket;
    private final OutputStream dapOut;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Virtual path prefix sent by the browser in {@code setBreakpoints} source paths, e.g.
     * {@code /workspace/} (the workspace name with surrounding slashes).
     */
    private final String virtualPathPrefix;
    /**
     * Real filesystem path prefix that replaces {@link #virtualPathPrefix} when forwarding
     * {@code setBreakpoints} to the DAP server, e.g.
     * {@code /home/user/.../repository/root/users/admin/workspace/}.
     */
    private final String realPathPrefix;

    JavaDebugBridge(Socket dapSocket, String virtualPathPrefix, String realPathPrefix) throws IOException {
        this.dapSocket = dapSocket;
        this.dapOut = dapSocket.getOutputStream();
        this.virtualPathPrefix = virtualPathPrefix;
        this.realPathPrefix = realPathPrefix;
        startDapReader();
    }

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.debug("[java-debug] Session {} attached (total: {})", session.getId(), sessions.size());
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        logger.debug("[java-debug] Session {} detached (total: {})", sessionId, sessions.size());
    }

    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public boolean hasSessions() {
        return !sessions.isEmpty();
    }

    public boolean isAlive() {
        return !dapSocket.isClosed() && dapSocket.isConnected();
    }

    public synchronized void sendToDap(String json) {
        if (!isAlive()) {
            logger.warn("[java-debug] DAP socket is closed, dropping message");
            return;
        }
        try {
            byte[] body = translateSourcePaths(json).getBytes(StandardCharsets.UTF_8);
            String header = "Content-Length: " + body.length + "\r\n\r\n";
            dapOut.write(header.getBytes(StandardCharsets.UTF_8));
            dapOut.write(body);
            dapOut.flush();
        } catch (IOException e) {
            logger.error("[java-debug] Error writing to DAP socket", e);
        }
    }

    public void destroy() {
        try {
            dapSocket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Translates the {@code source.path} field in DAP {@code setBreakpoints} requests from the virtual
     * workspace path the browser uses to the real filesystem path the DAP server needs. All other
     * messages are returned unchanged.
     */
    private String translateSourcePaths(String json) {
        if (!json.contains("setBreakpoints")) {
            return json;
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (!"setBreakpoints".equals(root.path("command")
                                             .asText())) {
                return json;
            }
            JsonNode source = root.path("arguments")
                                  .path("source");
            if (!source.isObject()) {
                return json;
            }
            String path = source.path("path")
                                .asText(null);
            if (path == null || !path.startsWith(virtualPathPrefix)) {
                return json;
            }
            String relative = path.substring(virtualPathPrefix.length());
            ((ObjectNode) source).put("path", realPathPrefix + relative);
            logger.debug("[java-debug] Translated breakpoint path {} → {}", path, realPathPrefix + relative);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            logger.warn("[java-debug] Could not translate breakpoint source path", e);
            return json;
        }
    }

    private void startDapReader() {
        Thread t = new Thread(() -> {
            try {
                InputStream in = dapSocket.getInputStream();
                while (isAlive()) {
                    int len = readContentLength(in);
                    if (len < 0) {
                        break;
                    }
                    byte[] body = in.readNBytes(len);
                    if (body.length == 0) {
                        break;
                    }
                    broadcast(new String(body, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                if (isAlive()) {
                    logger.error("[java-debug] Error reading from DAP socket", e);
                }
            }
            logger.info("[java-debug] DAP reader thread exited");
        }, "java-debug-dap-reader");
        t.setDaemon(true);
        t.start();
    }

    private void broadcast(String json) {
        TextMessage msg = new TextMessage(json);
        sessions.values()
                .forEach(session -> {
                    try {
                        if (session.isOpen()) {
                            synchronized (session) {
                                session.sendMessage(msg);
                            }
                        }
                    } catch (IOException e) {
                        logger.warn("[java-debug] Error sending to session {}", session.getId(), e);
                    }
                });
    }

    /**
     * Reads DAP {@code Content-Length} headers until the blank separator line, then returns the
     * content-length value, or {@code -1} on EOF or parse error.
     */
    private static int readContentLength(InputStream in) throws IOException {
        int contentLength = -1;
        StringBuilder line = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next == -1) {
                    return -1;
                }
                String header = line.toString()
                                    .trim();
                line.setLength(0);
                if (header.isEmpty()) {
                    return contentLength;
                }
                int colon = header.indexOf(':');
                if (colon > 0 && header.substring(0, colon)
                                       .equalsIgnoreCase("Content-Length")) {
                    contentLength = Integer.parseInt(header.substring(colon + 1)
                                                           .trim());
                }
            } else {
                line.append((char) b);
            }
        }
        return -1;
    }
}
