/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test for the JDT Language Server integration.
 *
 * <p>
 * Boots the full Spring application, writes {@code Hello.java} into the user workspace via
 * {@link IRepository} (simulating what the IDE does when a user saves a file), then connects to the
 * {@code /websockets/ide/java-lsp} WebSocket endpoint. {@code JdtLsManager} auto-generates
 * {@code .project} and {@code .classpath} for the project on first connection, so the test does not
 * need to create them. The test runs the LSP handshake and asserts that
 * {@code textDocument/completion} at {@code resp.getWriter().} returns at least one {@code println}
 * suggestion — confirming that JDT.LS resolves {@code PrintWriter} via the platform JARs supplied
 * by {@code ClassPathIndex}.
 *
 * <p>
 * The WebSocket connects at the workspace level (not per-project), matching the per-workspace
 * JDT.LS model introduced in Phase 1.
 */
class JavaLspIT extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaLspIT.class);

    // -------------------------------------------------------------------------
    // Project fixture
    // -------------------------------------------------------------------------

    private static final String PROJECT = "hello-java";
    private static final String WORKSPACE = "workspace";
    private static final String USERNAME = "admin";

    /** IRepository path for the Hello.java source file. */
    private static final String HELLO_JAVA_REPO = "/users/" + USERNAME + "/" + WORKSPACE + "/" + PROJECT + "/demo/Hello.java";

    /** Virtual workspace root URI used in LSP initialize and workspaceFolders. */
    private static final String VIRTUAL_WORKSPACE_ROOT = "file:///workspace/" + WORKSPACE + "/";

    /** Virtual file URI for Hello.java, scoped inside the workspace root. */
    private static final String VIRTUAL_FILE_URI = VIRTUAL_WORKSPACE_ROOT + PROJECT + "/demo/Hello.java";

    // Completion position: line 9, character 25 = start of "println" in
    // " resp.getWriter().println("Hello World!");"
    private static final int COMPLETION_LINE = 9;
    private static final int COMPLETION_CHARACTER = 25;

    private static final String HELLO_JAVA = """
            package demo;

            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletResponse;
            import org.eclipse.dirigible.engine.java.handler.JavaHandler;

            public class Hello implements JavaHandler {

                public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                    resp.getWriter().println("Hello World!");
                }
            }
            """;

    // -------------------------------------------------------------------------
    // Spring beans
    // -------------------------------------------------------------------------

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    // -------------------------------------------------------------------------
    // Per-test state for the WebSocket conversation
    // -------------------------------------------------------------------------

    private final LinkedBlockingQueue<String> inbox = new LinkedBlockingQueue<>();
    private final List<String> buffer = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger seq = new AtomicInteger(1);

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    @Test
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void completion_suggests_println_for_PrintWriter_via_resp_getWriter() throws Exception {
        writeProjectFiles();

        WebSocket ws = connectWebSocket();
        try {
            // Initialize JDT.LS
            int initId = seq.getAndIncrement();
            ws.sendText(initRequest(initId), true)
              .join();
            assertNotNull(awaitResponse(initId, 120, SECONDS),
                    "JDT.LS did not respond to 'initialize' within 120 s — check server logs for startup errors");

            ws.sendText(notification("initialized", "{}"), true)
              .join();
            ws.sendText(notification("workspace/didChangeConfiguration", "{\"settings\":" + jdtlsSettings() + "}"), true)
              .join();
            ws.sendText(didOpenNotification(), true)
              .join();

            // Wait until JDT.LS has fully indexed the classpath — signalled by a
            // publishDiagnostics notification with zero severity-1 (Error) entries.
            // Early diagnostics arrive before indexing finishes and still carry
            // "import cannot be resolved" errors; we discard those and keep waiting.
            assertTrue(awaitCleanDiagnosticsFor(VIRTUAL_FILE_URI, 240, SECONDS),
                    "JDT.LS did not publish error-free diagnostics for the open file within 240 s — "
                            + "imports may still be unresolved (check classpath index and JDT.LS logs)");

            // Request completion at resp.getWriter().| — retry a few times to absorb
            // any residual timing gap between clean diagnostics and full indexing.
            List<String> labels = List.of();
            for (int attempt = 0; attempt < 5; attempt++) {
                if (attempt > 0) {
                    LOGGER.info("[java-lsp] Completion attempt {} returned {}; retrying in 3 s", attempt, labels);
                    Thread.sleep(3_000);
                }
                int completionId = seq.getAndIncrement();
                ws.sendText(completionRequest(completionId), true)
                  .join();
                String result = awaitResponse(completionId, 30, SECONDS);
                assertNotNull(result, "JDT.LS did not respond to 'textDocument/completion' within 30 s");
                labels = completionLabels(result);
                if (labels.stream()
                          .anyMatch(l -> l.startsWith("println"))) {
                    break;
                }
            }
            assertTrue(labels.stream()
                             .anyMatch(l -> l.startsWith("println")),
                    "Expected a 'println' completion from PrintWriter but got: " + labels);
        } finally {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "test finished")
              .get(5, SECONDS);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — project setup
    // -------------------------------------------------------------------------

    private void writeProjectFiles() {
        repository.createResource(HELLO_JAVA_REPO, HELLO_JAVA.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
    }

    // -------------------------------------------------------------------------
    // Helpers — WebSocket connection
    // -------------------------------------------------------------------------

    private WebSocket connectWebSocket() throws Exception {
        String credentials = Base64.getEncoder()
                                   .encodeToString((USERNAME + ":admin").getBytes(StandardCharsets.UTF_8));
        URI uri = URI.create("ws://localhost:" + port + "/websockets/ide/java-lsp" + "?workspace=" + WORKSPACE);

        return HttpClient.newHttpClient()
                         .newWebSocketBuilder()
                         .header("Authorization", "Basic " + credentials)
                         .buildAsync(uri, new Listener())
                         .get(10, SECONDS);
    }

    private class Listener implements WebSocket.Listener {
        private final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                inbox.add(partial.toString());
                partial.setLength(0);
            }
            ws.request(1);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — LSP message routing
    // -------------------------------------------------------------------------

    /** Returns the first response with the given {@code id}, buffering all other messages. */
    private String awaitResponse(int id, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

        for (Iterator<String> it = buffer.iterator(); it.hasNext();) {
            JsonNode n = mapper.readTree(it.next());
            if (n.has("id") && n.get("id")
                                .asInt() == id) {
                it.remove();
                return n.toString();
            }
        }

        while (System.currentTimeMillis() < deadline) {
            long remaining = Math.max(100, deadline - System.currentTimeMillis());
            String msg = inbox.poll(Math.min(remaining, 2_000), MILLISECONDS);
            if (msg == null)
                continue;
            JsonNode n = mapper.readTree(msg);
            if (n.has("id") && n.get("id")
                                .asInt() == id) {
                return n.toString();
            }
            buffer.add(msg);
        }
        return null;
    }

    /**
     * Returns {@code true} once a {@code textDocument/publishDiagnostics} notification arrives for the
     * given URI that carries <em>no</em> severity-1 (Error) diagnostics. Early notifications that still
     * contain unresolved-import errors are discarded and the method keeps waiting — JDT.LS publishes
     * incremental diagnostics updates as it finishes indexing each JAR. All non-diagnostics messages
     * are buffered for subsequent {@link #awaitResponse} calls.
     */
    private boolean awaitCleanDiagnosticsFor(String uri, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

        for (Iterator<String> it = buffer.iterator(); it.hasNext();) {
            String msg = it.next();
            if (isDiagnosticsFor(msg, uri)) {
                it.remove();
                if (hasNoErrors(msg)) {
                    return true;
                }
                LOGGER.info("[java-lsp] Discarding diagnostics with errors (classpath still indexing)");
            }
        }

        while (System.currentTimeMillis() < deadline) {
            long remaining = Math.max(100, deadline - System.currentTimeMillis());
            String msg = inbox.poll(Math.min(remaining, 2_000), MILLISECONDS);
            if (msg == null)
                continue;
            if (isDiagnosticsFor(msg, uri)) {
                if (hasNoErrors(msg)) {
                    return true;
                }
                LOGGER.info("[java-lsp] Diagnostics still has errors — waiting for classpath indexing to complete");
            } else {
                buffer.add(msg);
            }
        }
        return false;
    }

    private boolean isDiagnosticsFor(String msg, String uri) throws Exception {
        JsonNode n = mapper.readTree(msg);
        return "textDocument/publishDiagnostics".equals(n.path("method")
                                                         .asText())
                && uri.equals(n.path("params")
                               .path("uri")
                               .asText());
    }

    /** Returns {@code true} if the diagnostics notification contains no severity-1 (Error) entries. */
    private boolean hasNoErrors(String diagnosticsMsg) throws Exception {
        JsonNode diagnostics = mapper.readTree(diagnosticsMsg)
                                     .path("params")
                                     .path("diagnostics");
        if (!diagnostics.isArray()) {
            return true;
        }
        for (JsonNode diag : diagnostics) {
            if (diag.path("severity")
                    .asInt() == 1) {
                return false;
            }
        }
        return true;
    }

    private List<String> completionLabels(String responseJson) throws Exception {
        JsonNode result = mapper.readTree(responseJson)
                                .path("result");
        JsonNode items = result.isArray() ? result : result.path("items");
        List<String> labels = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                labels.add(item.path("label")
                               .asText());
            }
        }
        return labels;
    }

    // -------------------------------------------------------------------------
    // Helpers — LSP message builders
    // -------------------------------------------------------------------------

    private String initRequest(int id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"initialize\",\"params\":{" + "\"processId\":null," + "\"rootUri\":\""
                + VIRTUAL_WORKSPACE_ROOT + "\"," + "\"workspaceFolders\":[{\"uri\":\"" + VIRTUAL_WORKSPACE_ROOT + "\",\"name\":\""
                + WORKSPACE + "\"}]," + "\"capabilities\":{" + "\"textDocument\":{" + "\"synchronization\":{\"dynamicRegistration\":true},"
                + "\"completion\":{\"dynamicRegistration\":true," + "\"completionItem\":{\"snippetSupport\":true},\"contextSupport\":true},"
                + "\"hover\":{\"dynamicRegistration\":true}," + "\"publishDiagnostics\":{\"relatedInformation\":true}},"
                + "\"workspace\":{\"configuration\":true," + "\"didChangeConfiguration\":{\"dynamicRegistration\":true}}}}}";
    }

    private String didOpenNotification() throws Exception {
        String escapedText = mapper.writeValueAsString(HELLO_JAVA);
        return notification("textDocument/didOpen", "{\"textDocument\":{\"uri\":\"" + VIRTUAL_FILE_URI + "\","
                + "\"languageId\":\"java\",\"version\":1,\"text\":" + escapedText + "}}");
    }

    private String completionRequest(int id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"textDocument/completion\"," + "\"params\":{\"textDocument\":{\"uri\":\""
                + VIRTUAL_FILE_URI + "\"}," + "\"position\":{\"line\":" + COMPLETION_LINE + ",\"character\":" + COMPLETION_CHARACTER + "},"
                + "\"context\":{\"triggerKind\":1}}}";
    }

    private static String notification(String method, String paramsJson) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + paramsJson + "}";
    }

    private static String jdtlsSettings() {
        return "{\"java\":{\"import\":{\"maven\":{\"enabled\":false},\"gradle\":{\"enabled\":false}}," + "\"autobuild\":{\"enabled\":true},"
                + "\"completion\":{\"overwrite\":true,\"guessMethodArguments\":false}," + "\"signatureHelp\":{\"enabled\":true}}}";
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    @AfterEach
    void removeWorkspaceFiles() {
        if (repository.hasResource(HELLO_JAVA_REPO)) {
            repository.removeResource(HELLO_JAVA_REPO);
        }
    }
}
