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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The registry mounted from {@code DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER} reaches the runtime without
 * a publish - at boot and while the instance runs (issue
 * <a href="https://github.com/eclipse-dirigible/dirigible/issues/7192">#7192</a>).
 *
 * <p>
 * The external folder is copied straight onto the registry's file system path, and the watcher that
 * schedules synchronization passes registers the registry root only. A file written several folders
 * deep therefore produced no event at all: no pass was scheduled, nothing recompiled, and whatever
 * the last pass happened to see stayed the installed state for the life of the process - a
 * client-Java {@code @Controller} answering 404 forever on an instance whose sources are all there.
 *
 * <p>
 * Nothing here calls {@code forceProcessSynchronizers()} or publishes: the whole point is that the
 * platform notices on its own, so the assertions wait for the scheduled pass instead.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// One Dirigible boot for the whole journey: the second step adds to what the first one asserted.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExternalRegistryFolderSyncIT extends IntegrationTest {

    private static final String EXTERNAL_FOLDER_KEY = "DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER";

    private static final String PROJECT = "external-registry-it";

    /**
     * A folder INSIDE the project, populated before the boot. The follow-up source is added here rather
     * than at the project root on purpose: adding it deeper leaves the mtime of the registry root's own
     * entries untouched, which is what makes the write invisible to the root watcher on every platform
     * - a file added directly under the project folder is still noticed by the polling watch service
     * macOS falls back to, and the test would then pass for the wrong reason.
     */
    private static final String FOLDER = "extregit";

    private static final String BOOT_ENDPOINT = "/services/java/" + PROJECT + "/" + FOLDER + "/Booted";
    private static final String ADDED_ENDPOINT = "/services/java/" + PROJECT + "/" + FOLDER + "/Added";

    /**
     * Generous: the source folder is polled (up to ~10s on a platform with no native watch service) and
     * the synchronization job then fires on its own schedule (10s by default).
     */
    private static final long AWAIT_SECONDS = 180;

    @TempDir
    static Path externalFolder;

    private static String previousExternalFolder;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @BeforeAll
    static void mountAnExternalRegistryFolder() throws IOException {
        previousExternalFolder = Configuration.get(EXTERNAL_FOLDER_KEY);
        Files.createDirectories(sourceFolder());
        Files.writeString(sourceFolder().resolve("Booted.java"), handlerSource("Booted", "hello from the boot-time copy"));
        Configuration.set(EXTERNAL_FOLDER_KEY, externalFolder.toString());
    }

    @AfterAll
    static void unmountTheExternalRegistryFolder() {
        if (previousExternalFolder == null) {
            Configuration.remove(EXTERNAL_FOLDER_KEY);
        } else {
            Configuration.set(EXTERNAL_FOLDER_KEY, previousExternalFolder);
        }
    }

    @Test
    @Order(1)
    void the_folder_present_at_boot_is_synchronized() {
        assertEndpointServes(BOOT_ENDPOINT, "hello from the boot-time copy");
    }

    @Test
    @Order(2)
    void a_source_added_deep_in_the_folder_afterwards_is_compiled_too() throws IOException {
        // Settle first, or the assertion proves nothing: the boot-time copy created a direct child of
        // the registry root, which the root watcher DOES see, and the pass that schedules would pick
        // the new source up on its own. Once no change is pending and no pass is running, nothing can
        // schedule another one - so the write below is the only possible cause of the next pass.
        awaitAnIdlePlatform();

        Files.writeString(sourceFolder().resolve("Added.java"), handlerSource("Added", "hello from the live copy"));

        assertEndpointServes(ADDED_ENDPOINT, "hello from the live copy");
        // and the source that was there before is still served by the new generation
        assertEndpointServes(BOOT_ENDPOINT, "hello from the boot-time copy");
    }

    private void awaitAnIdlePlatform() {
        Awaitility.await()
                  .atMost(2, TimeUnit.MINUTES)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> !synchronizationWatcher.isModified() && !synchronizationProcessor.isSynchronizationRunning());
    }

    private void assertEndpointServes(String endpoint, String expectedBodyFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(endpoint)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedBodyFragment)),
                AWAIT_SECONDS);
    }

    private static Path sourceFolder() {
        return externalFolder.resolve(PROJECT)
                             .resolve(FOLDER);
    }

    private static String handlerSource(String className, String message) {
        return """
                package %s;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class %s implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.setContentType("application/json");
                        response.getWriter().write("{\\"message\\": \\"%s\\"}");
                    }
                }
                """.formatted(FOLDER, className, message);
    }

}
