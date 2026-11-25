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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A file dropped for an intent {@code source: { folder }} arrival is ingested exactly once, in its
 * own tenant (#8157).
 *
 * <p>
 * The generated folder-poll job is scheduled in every tenant, and every tenant's job used to read
 * the same declared folder - so one dropped file landed in every tenant's database, whichever
 * tenant's tick got there first moving it away. Each tenant now reads its own folder: the default
 * tenant the declared one, every other tenant {@code <folder>/tenants/<tenant id>}. The test drops
 * one file into each tenant's folder, waits until both are processed, then lets the jobs tick a few
 * more times and asserts each tenant holds its own records and none of the other's.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentInboundFolderTenantIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "inboundfoldertenantit";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String SIGNALS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/signal/SignalController";

    /** The declared drop folder, relative to the instance's working directory. */
    private static final Path DROP_FOLDER = Paths.get("target/inbox-folder-tenant-it");

    private static final long TIMEOUT_SECONDS = 120;

    /**
     * Enough ticks of the two-second cron for a cross-tenant read to have happened had it been
     * possible.
     */
    private static final long SETTLE_SECONDS = 10;

    private static final String INTENT = """
            name: inboundfoldertenantit
            description: signals dropped as files into a polled folder

            entities:
              - name: Signal
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }

            inbound:
              - { name: signalDrop, source: { folder: target/inbox-folder-tenant-it, cron: "0/2 * * * * ?" }, create: Signal }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void aDroppedFileIsIngestedOnlyInTheTenantWhoseFolderItWasDroppedInto() {
        removeDropFolder();
        DirigibleTestTenant other = new DirigibleTestTenant("inbound-folder-other");
        createTenants(other);
        waitForTenantProvisioning(other);
        securityUtil.assignAllSystemRolesToUser(other.getUsername(), other.getId());

        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        Path otherFolder = DROP_FOLDER.resolve("tenants")
                                      .resolve(other.getId());
        drop(DROP_FOLDER, "[{\"Note\":\"default one\"},{\"Note\":\"default two\"}]");
        drop(otherFolder, "[{\"Note\":\"other one\"}]");

        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> Files.exists(DROP_FOLDER.resolve("processed/signals.json"))
                          && Files.exists(otherFolder.resolve("processed/signals.json")));

        // Let both tenants' jobs tick a few more times: a job still reading another tenant's folder
        // would have had every chance to pick a file up.
        Awaitility.await()
                  .pollDelay(SETTLE_SECONDS, TimeUnit.SECONDS)
                  .atMost(2 * SETTLE_SECONDS, TimeUnit.SECONDS)
                  .until(() -> true);

        assertNotes(null, List.of("default one", "default two"));
        assertNotes(other, List.of("other one"));
        assertTrue(Files.notExists(DROP_FOLDER.resolve("failed")) && Files.notExists(otherFolder.resolve("failed")),
                "no file may fail - each one is read by its own tenant only");
    }

    private void assertNotes(DirigibleTestTenant tenant, List<String> expected) {
        Runnable read = () -> given().when()
                                     .get(SIGNALS)
                                     .then()
                                     .statusCode(200)
                                     .body("Note", containsInAnyOrder(expected.toArray()));
        if (tenant == null) {
            restAssuredExecutor.execute(read::run, TIMEOUT_SECONDS);
        } else {
            restAssuredExecutor.execute(tenant, read::run, TIMEOUT_SECONDS);
        }
    }

    private static void drop(Path folder, String content) {
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve("signals.json"), content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to drop the ingest file into " + folder.toAbsolutePath(), ex);
        }
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
        removeDropFolder();
    }

    /** The drop folder is outside the repository - clear it so a rerun starts empty. */
    private static void removeDropFolder() {
        if (!Files.isDirectory(DROP_FOLDER)) {
            return;
        }
        try (Stream<Path> tree = Files.walk(DROP_FOLDER)) {
            for (Path path : tree.sorted(Comparator.reverseOrder())
                                 .toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to clear the inbound drop folder " + DROP_FOLDER.toAbsolutePath(), ex);
        }
    }
}
