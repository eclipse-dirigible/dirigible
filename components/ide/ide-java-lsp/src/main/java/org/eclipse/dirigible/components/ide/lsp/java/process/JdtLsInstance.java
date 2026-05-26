/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.lsp.java.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a single running JDT Language Server OS process and the WebSocket sessions it serves.
 *
 * <p>
 * All stdin writes are synchronised to prevent interleaving of Content-Length frames. Stdout is
 * read on a dedicated daemon thread; each complete LSP message is forwarded to every open session
 * after translating the real filesystem URI back to the virtual URI the browser uses.
 */
public class JdtLsInstance {

    private static final Logger logger = LoggerFactory.getLogger(JdtLsInstance.class);

    private final Process process;
    private final OutputStream stdin;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** Virtual URI root sent by the browser, e.g. {@code file:///workspace/ws1/proj1/}. */
    private final String virtualRoot;
    /** Real filesystem URI root used by JDT.LS, e.g. {@code file:///home/user/.../proj1/}. */
    private final String realRoot;

    private final Map<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger serverRequestCounter = new AtomicInteger(-1);
    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Set to {@code true} once JDT.LS has completed the LSP initialize handshake (either via a browser
     * client or the server-side initialization triggered by {@link #ensureInitialized}).
     */
    private volatile boolean lspInitialized = false;

    /**
     * Cached JDT.LS capabilities JSON (the {@code result} of the {@code initialize} response). Used to
     * synthesize a fake-but-real-capabilities response for browser clients that connect after the
     * server-side initialization has already completed.
     */
    private volatile String cachedCapabilitiesJson = null;

    JdtLsInstance(Process process, String virtualRoot, String realRoot) {
        this.process = process;
        this.stdin = process.getOutputStream();
        this.virtualRoot = virtualRoot;
        this.realRoot = realRoot;
        startStdoutReader();
        startStderrDrain();
    }

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.debug("[java-lsp] Session {} attached (total: {})", session.getId(), sessions.size());
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        logger.debug("[java-lsp] Session {} detached (total: {})", sessionId, sessions.size());
    }

    boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    boolean isAlive() {
        return process.isAlive();
    }

    public synchronized void sendToProcess(String json) {
        if (!process.isAlive()) {
            logger.warn("[java-lsp] Process is dead, dropping message");
            return;
        }
        // Track browser-initiated LSP initialization: when the browser sends the `initialized`
        // notification (no id, no response expected), JDT.LS has been fully initialized by the editor.
        // Mark it here so that ensureInitialized() becomes a no-op.
        try {
            JsonNode msg = jsonMapper.readTree(json);
            String method = msg.path("method")
                               .asText("");
            if ("initialized".equals(method) && !msg.has("id")) {
                lspInitialized = true;
                logger.debug("[java-lsp] JDT.LS initialized by browser LSP client");
            }
        } catch (Exception ignored) {
        }

        // When JDT.LS was already initialized server-side, a browser that subsequently connects will
        // send an `initialize` request. JDT.LS would reject it with -32600 (server already initialized).
        // Instead, intercept it here and send back the real cached capabilities so the browser proceeds
        // normally without re-initializing JDT.LS.
        if (lspInitialized && cachedCapabilitiesJson != null) {
            try {
                JsonNode msg = jsonMapper.readTree(json);
                if ("initialize".equals(msg.path("method")
                                           .asText())
                        && msg.has("id")) {
                    int browserId = msg.get("id")
                                       .asInt();
                    String fakeResponse = "{\"jsonrpc\":\"2.0\",\"id\":" + browserId + ",\"result\":" + cachedCapabilitiesJson + "}";
                    broadcast(fakeResponse);
                    logger.debug("[java-lsp] Intercepted browser initialize (already initialized) — sent cached capabilities");
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        try {
            String translated = json.replace(virtualRoot, realRoot);
            byte[] body = translated.getBytes(StandardCharsets.UTF_8);
            String header = "Content-Length: " + body.length + "\r\n\r\n";
            stdin.write(header.getBytes(StandardCharsets.UTF_8));
            stdin.write(body);
            stdin.flush();
        } catch (IOException e) {
            logger.error("[java-lsp] Error writing to JDT.LS stdin", e);
        }
    }

    /**
     * Sends a server-initiated JSON-RPC request to the JDT.LS process and returns a future that
     * completes with the response. Uses negative IDs to avoid collision with browser-originated request
     * IDs (which are positive integers assigned by the LSP client).
     *
     * @param method LSP method name, e.g. {@code workspace/executeCommand}
     * @param paramsJson JSON object for the {@code params} field
     */
    public CompletableFuture<JsonNode> sendRequest(String method, String paramsJson) {
        int id = serverRequestCounter.getAndDecrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        sendToProcess("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + paramsJson + "}");
        return future;
    }

    /**
     * Ensures JDT.LS has completed the LSP initialize handshake before workspace commands can be sent.
     *
     * <p>
     * JDT.LS buffers (and never processes) {@code workspace/*} requests that arrive before it receives
     * the {@code initialize} + {@code initialized} pair from an LSP client. This method performs that
     * handshake from the server side so that workspace commands work even when no browser editor
     * session has connected yet. If JDT.LS was already initialized (e.g. by a browser), this is a
     * no-op.
     *
     * <p>
     * When a browser subsequently connects and sends {@code initialize}, the request is intercepted in
     * {@link #sendToProcess} and a fake-but-real-capabilities response is returned so the browser LSP
     * client continues normally without JDT.LS rejecting a duplicate initialization.
     *
     * @param workspaceUri real {@code file://} URI of the workspace root directory
     * @return a future that completes when JDT.LS is in the initialized state
     */
    public synchronized CompletableFuture<Void> ensureInitialized(String workspaceUri) {
        if (lspInitialized) {
            return CompletableFuture.completedFuture(null);
        }
        long pid = ProcessHandle.current()
                                .pid();
        String escapedUri = workspaceUri.replace("\\", "\\\\")
                                        .replace("\"", "\\\"");
        String initParams = "{\"processId\":" + pid + "," + "\"clientInfo\":{\"name\":\"dirigible-java-debug\"}," + "\"rootUri\":\""
                + escapedUri + "\"," + "\"workspaceFolders\":[{\"uri\":\"" + escapedUri + "\",\"name\":\"workspace\"}],"
                + "\"capabilities\":{\"workspace\":{\"executeCommand\":{\"dynamicRegistration\":false}}},"
                + "\"initializationOptions\":{}}";

        CompletableFuture<JsonNode> initResponse = sendRequest("initialize", initParams);
        CompletableFuture<Void> result = new CompletableFuture<>();
        initResponse.whenComplete((node, ex) -> {
            if (ex != null) {
                result.completeExceptionally(ex);
                return;
            }
            // Cache capabilities so we can replay them to browser clients that connect later.
            cachedCapabilitiesJson = node.path("result")
                                         .toString();
            // Send the 'initialized' notification — JDT.LS transitions to initialized state here.
            sendToProcess("{\"jsonrpc\":\"2.0\",\"method\":\"initialized\",\"params\":{}}");
            lspInitialized = true;
            logger.info("[java-lsp] JDT.LS initialized from server side for workspace {}", workspaceUri);
            result.complete(null);
        });
        return result;
    }

    void destroy() {
        process.destroyForcibly();
    }

    /**
     * Notifies JDT.LS that class files in the compiled output directory have changed. Sends
     * {@code workspace/didChangeWatchedFiles} for all affected class file URIs so that JDT.LS
     * re-indexes the compiled output without requiring a restart.
     *
     * @param compiledOutputDir on-disk directory where compiled {@code .class} files reside
     * @param changedFqns FQNs whose {@code .class} files were created or updated
     * @param deletedFqns FQNs whose {@code .class} files were deleted
     */
    public void notifyCompiledOutputChanged(Path compiledOutputDir, Set<String> changedFqns, Set<String> deletedFqns) {
        if (!process.isAlive()) {
            return;
        }
        StringBuilder changes = new StringBuilder();
        boolean first = true;
        for (String fqn : changedFqns) {
            if (!first) {
                changes.append(',');
            }
            first = false;
            String classFileUri = compiledOutputDir.resolve(fqn.replace('.', '/') + ".class")
                                                   .toUri()
                                                   .toString();
            changes.append("{\"uri\":\"")
                   .append(classFileUri)
                   .append("\",\"type\":1}");
        }
        for (String fqn : deletedFqns) {
            if (!first) {
                changes.append(',');
            }
            first = false;
            String classFileUri = compiledOutputDir.resolve(fqn.replace('.', '/') + ".class")
                                                   .toUri()
                                                   .toString();
            changes.append("{\"uri\":\"")
                   .append(classFileUri)
                   .append("\",\"type\":3}");
        }
        if (changes.length() == 0) {
            return;
        }
        sendToProcess(
                "{\"jsonrpc\":\"2.0\",\"method\":\"workspace/didChangeWatchedFiles\"," + "\"params\":{\"changes\":[" + changes + "]}}");
    }

    // -------------------------------------------------------------------------

    private boolean routeToServerRequest(String json) {
        try {
            JsonNode node = jsonMapper.readTree(json);
            if (!node.has("id") || node.has("method")) {
                return false;
            }
            int responseId = node.get("id")
                                 .asInt(Integer.MIN_VALUE);
            CompletableFuture<JsonNode> future = pendingRequests.remove(responseId);
            if (future == null) {
                return false;
            }
            if (node.has("error")) {
                future.completeExceptionally(new RuntimeException("[java-lsp] Request " + responseId + " failed: " + node.get("error")));
            } else {
                future.complete(node);
            }
            return true;
        } catch (Exception e) {
            logger.warn("[java-lsp] Could not route server request response", e);
            return false;
        }
    }

    private void startStdoutReader() {
        Thread t = new Thread(() -> {
            try {
                InputStream stdout = process.getInputStream();
                while (process.isAlive()) {
                    int contentLength = readContentLength(stdout);
                    if (contentLength < 0)
                        break;
                    byte[] body = stdout.readNBytes(contentLength);
                    if (body.length == 0)
                        break;
                    String json = new String(body, StandardCharsets.UTF_8);
                    String translated = json.replace(realRoot, virtualRoot);
                    if (!routeToServerRequest(translated)) {
                        broadcast(translated);
                    }
                }
            } catch (IOException e) {
                if (process.isAlive()) {
                    logger.error("[java-lsp] Error reading JDT.LS stdout", e);
                }
            }
            if (!process.isAlive()) {
                logger.warn("[java-lsp] Stdout reader exited — JDT.LS process terminated with code {}", process.exitValue());
            } else {
                logger.info("[java-lsp] Stdout reader exited (process still alive)");
            }
        }, "jdtls-stdout-reader");
        t.setDaemon(true);
        t.start();
    }

    private void startStderrDrain() {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[java-lsp] {}", line);
                }
            } catch (IOException ignored) {
            }
        }, "jdtls-stderr-drain");
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
                        logger.warn("[java-lsp] Error sending to session {}", session.getId(), e);
                    }
                });
    }

    /**
     * Reads LSP Content-Length headers from {@code in} until the blank separator line, then returns the
     * content-length value (or -1 on EOF / error).
     */
    private static int readContentLength(InputStream in) throws IOException {
        int contentLength = -1;
        StringBuilder line = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next == -1)
                    return -1;
                // next should be '\n'
                String header = line.toString()
                                    .trim();
                line.setLength(0);
                if (header.isEmpty()) {
                    // blank line signals end of headers
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
