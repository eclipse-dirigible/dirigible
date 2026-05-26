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
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsInstance;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsManager;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the Java Debugger integration.
 *
 * <p>
 * Verifies that:
 * <ol>
 * <li>{@code installDebugPlugin()} copies the plugin JAR and registers it in every
 * {@code config_<platform>/config.ini} so that Equinox loads it.</li>
 * <li>{@code ensureInitialized()} completes the LSP initialize handshake from the server side,
 * allowing workspace commands to be processed without a browser editor session.</li>
 * <li>{@code workspace/executeCommand → vscode.java.startDebugSession} returns a TCP port on which
 * the DAP server is listening.</li>
 * <li>A TCP socket can be opened to that port (proving the DAP server is reachable).</li>
 * </ol>
 *
 * <p>
 * No JDWP target process is required — the test only verifies the DAP server starts, not that it
 * can connect to a debuggee JVM.
 */
class JavaDebugIT extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDebugIT.class);

    private static final String USERNAME = "admin";
    private static final String WORKSPACE = "workspace";

    // Minimal Java file so the workspace looks like a real Java project.
    private static final String HELLO_JAVA_REPO = "/users/" + USERNAME + "/" + WORKSPACE + "/debug-test/Hello.java";
    private static final String HELLO_JAVA = "public class Hello {}";

    @Autowired
    private JdtLsManager lspManager;

    @Autowired
    private IRepository repository;

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void debug_plugin_registers_and_dap_server_starts() throws Exception {
        // Step 1 — verify that the debug plugin is registered in config.ini.
        // This is the root cause of "No delegateCommandHandler for vscode.java.startDebugSession":
        // Equinox ignores JARs not listed in osgi.bundles even if they are in the plugins/ directory.
        assertDebugPluginInConfigIni();

        // Step 2 — start JDT.LS for the test workspace.
        repository.createResource(HELLO_JAVA_REPO, HELLO_JAVA.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        JdtLsInstance lsp = lspManager.getOrStart(USERNAME, WORKSPACE);
        assertNotNull(lsp, "JdtLsManager.getOrStart returned null — JDT.LS distribution is not bundled");
        assertTrue(lsp.isAlive(), "JDT.LS process is not alive after getOrStart");

        // Step 3 — server-side LSP initialization. Without this, workspace/executeCommand is queued
        // forever because JDT.LS waits for an initialize + initialized from a client.
        String workspaceUri = workspaceRootUri();
        LOGGER.info("[JavaDebugIT] Initializing JDT.LS for workspace URI: {}", workspaceUri);
        lsp.ensureInitialized(workspaceUri)
           .get(120, TimeUnit.SECONDS);

        // Step 4 — ask JDT.LS to start the DAP server via the debug plugin command.
        LOGGER.info("[JavaDebugIT] Sending vscode.java.startDebugSession");
        JsonNode response =
                lsp.sendRequest("workspace/executeCommand", "{\"command\":\"vscode.java.startDebugSession\",\"arguments\":[]}")
                   .get(30, TimeUnit.SECONDS);

        LOGGER.info("[JavaDebugIT] vscode.java.startDebugSession response: {}", response);
        assertFalse(response.has("error"),
                "workspace/executeCommand returned an error — debug plugin may not be loaded by Equinox: " + response);

        JsonNode result = response.path("result");
        int port = result.isInt() ? result.asInt() : result.path("port").asInt(0);
        assertTrue(port > 0, "Expected a TCP port in the startDebugSession response but got: " + response);

        // Step 5 — verify the DAP server is actually listening on that port.
        LOGGER.info("[JavaDebugIT] Connecting to DAP server on port {}", port);
        try (Socket dap = new Socket("localhost", port)) {
            assertTrue(dap.isConnected(), "Could not connect to DAP server on port " + port);
        }
        LOGGER.info("[JavaDebugIT] DAP server is reachable — Java debugger integration test passed");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts that at least one {@code config_<platform>/config.ini} under the JDT.LS install
     * directory contains an {@code osgi.bundles} entry for {@code com.microsoft.java.debug.plugin}.
     * Fails with a clear message identifying the missing registration as the root cause.
     */
    private void assertDebugPluginInConfigIni() throws Exception {
        String jdtlsHome = getJdtlsHome();
        if (jdtlsHome == null) {
            LOGGER.info("[JavaDebugIT] JDT.LS home not found — skipping config.ini check (quick-build?)");
            return;
        }
        Path jdtlsHomePath = Paths.get(jdtlsHome);
        if (!Files.isDirectory(jdtlsHomePath)) {
            LOGGER.info("[JavaDebugIT] JDT.LS home {} does not exist — skipping config.ini check", jdtlsHome);
            return;
        }
        boolean found = false;
        try (var children = Files.list(jdtlsHomePath)) {
            for (Path configDir : (Iterable<Path>) children::iterator) {
                if (!configDir.getFileName()
                              .toString()
                              .startsWith("config_") || !Files.isDirectory(configDir)) {
                    continue;
                }
                Path configIni = configDir.resolve("config.ini");
                if (!Files.exists(configIni)) {
                    continue;
                }
                String content = Files.readString(configIni, StandardCharsets.UTF_8);
                if (content.contains("com.microsoft.java.debug.plugin")) {
                    found = true;
                    LOGGER.info("[JavaDebugIT] Debug plugin registered in {}", configIni);
                    break;
                }
            }
        }
        assertTrue(found,
                "com.microsoft.java.debug.plugin is NOT registered in any config_<platform>/config.ini under "
                        + jdtlsHome + ". Equinox will not load it — this causes 'No delegateCommandHandler for "
                        + "vscode.java.startDebugSession'. Fix: installDebugPlugin() must append the plugin entry "
                        + "to the osgi.bundles list in config.ini.");
    }

    private static String getJdtlsHome() {
        String configured = System.getProperty("DIRIGIBLE_JAVA_LSP_INSTALL_DIR");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return Paths.get(System.getProperty("user.home"), ".dirigible", "jdtls")
                    .toAbsolutePath()
                    .toString();
    }

    private static String workspaceRootUri() {
        String repoRoot = org.eclipse.dirigible.commons.config.DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue();
        return Paths.get(repoRoot, "dirigible", "repository", "root", "users", USERNAME, WORKSPACE)
                    .toAbsolutePath()
                    .normalize()
                    .toUri()
                    .toString();
    }

    @AfterEach
    void cleanup() {
        if (repository.hasResource(HELLO_JAVA_REPO)) {
            repository.removeResource(HELLO_JAVA_REPO);
        }
    }
}
