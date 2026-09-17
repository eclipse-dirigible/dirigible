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

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A write several folders deep into the registry reaches the runtime on its own - whoever made it
 * (issue <a href="https://github.com/eclipse-dirigible/dirigible/issues/7303">#7303</a>).
 *
 * <p>
 * {@code SynchronizationWatcher}, which decides whether a synchronization pass runs at all,
 * registers the registry root and nothing below it, so only a publish (which forces a pass) and the
 * external-folder copy (which forces its own, #7299) were ever seen. {@code LocalRegistryWatcher}
 * watches the registry recursively and marks it modified, which is what makes any other writer - a
 * git clone straight into the registry, a tool copying a project in - self-healing too.
 *
 * <p>
 * Nothing here publishes, forces a pass, or mounts an external folder: the platform has to notice
 * by itself, so the assertion waits for the scheduled pass instead. On master this test fails by
 * timing out on a 404 with the source sitting on disk.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistryDeepWriteSyncIT extends IntegrationTest {

    private static final String PROJECT = "registry-deep-write-it";

    /**
     * The folder the source is written into, INSIDE the project. A file added directly under the
     * project folder moves the mtime of a direct child of the registry root, which the root watcher
     * notices on the polling watch service macOS falls back to - the test would then pass for the wrong
     * reason.
     */
    private static final String FOLDER = "rdwit";

    private static final String ENDPOINT = "/services/java/" + PROJECT + "/" + FOLDER + "/DeepWrite";

    /** The synchronization job fires on its own schedule (10s by default), then javac runs. */
    private static final long AWAIT_SECONDS = 180;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @AfterEach
    void removeTheProject() throws IOException {
        FileUtils.deleteDirectory(projectFolder().toFile());
    }

    @Test
    void a_source_written_deep_into_the_registry_is_compiled_and_served() throws IOException {
        Files.createDirectories(sourceFolder());
        // Settle first, or the assertion proves nothing: the project folder is a direct child of the
        // registry root, which the root watcher DOES see, and the pass that schedules would pick the
        // source up on its own. Once no change is pending and no pass is running, nothing else can
        // schedule one - so the write below is the only possible cause of the next pass.
        awaitAnIdlePlatform();

        Files.writeString(sourceFolder().resolve("DeepWrite.java"), handlerSource());

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("hello from a deep registry write")),
                AWAIT_SECONDS);
    }

    private void awaitAnIdlePlatform() {
        Awaitility.await()
                  .atMost(2, TimeUnit.MINUTES)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> !synchronizationWatcher.isModified() && !synchronizationProcessor.isSynchronizationRunning());
    }

    private Path sourceFolder() {
        return projectFolder().resolve(FOLDER);
    }

    private Path projectFolder() {
        return Path.of(repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC))
                   .resolve(PROJECT);
    }

    private static String handlerSource() {
        return """
                package %s;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class DeepWrite implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.setContentType("application/json");
                        response.getWriter().write("{\\"message\\": \\"hello from a deep registry write\\"}");
                    }
                }
                """.formatted(FOLDER);
    }

}
