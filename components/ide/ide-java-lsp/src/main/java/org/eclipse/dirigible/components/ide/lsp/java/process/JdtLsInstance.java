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
import java.util.concurrent.ConcurrentHashMap;

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
                    broadcast(translated);
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
