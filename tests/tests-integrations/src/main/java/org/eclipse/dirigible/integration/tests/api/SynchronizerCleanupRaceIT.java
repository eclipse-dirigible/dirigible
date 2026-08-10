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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A publish replaces a registry collection by deleting it and copying it back milliseconds later. A
 * synchronization pass that looks into that hole used to delete the artefacts of the files that
 * were about to reappear - and the client-Java batch compile that follows then rebuilt from a
 * half-empty codebase, failed as a whole, and left the instance with nothing registered.
 *
 * <p>
 * The pass must therefore defer its cleanup while the registry is being written, and still
 * reconcile a genuine deletion once the registry is quiet.
 */
class SynchronizerCleanupRaceIT extends IntegrationTest {

    private static final String LOCATION = "/cleanup-race-it/Sample.java";

    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION;

    private static final String SOURCE = """
            package cleanuprace;

            public class Sample {
            }
            """;

    /**
     * A second source published while the registry is changing. Its artefact appearing is the proof
     * that a full pass actually ran under those conditions - {@code processSynchronizers()} silently
     * skips when another run holds the slot, so without this the assertions below could pass on a pass
     * that never happened.
     */
    private static final String PROBE_LOCATION = "/cleanup-race-it/Probe.java";

    private static final String PROBE_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + PROBE_LOCATION;

    private static final String PROBE_SOURCE = """
            package cleanuprace;

            public class Probe {
            }
            """;

    /** How long to keep driving passes while the publish is in flight, waiting for one to complete. */
    private static final long PASS_TIMEOUT_SECONDS = 120;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    @Autowired
    private JavaFileService javaFileService;

    @Autowired
    private RegistryMutationTracker registryMutationTracker;

    @Test
    void an_artefact_survives_a_pass_that_races_a_publish() {
        repository.createResource(REGISTRY_PATH, SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        synchronizationProcessor.forceProcessSynchronizers();

        assertThat(javaFileService.findByLocation(LOCATION)).as("the published source is registered")
                                                            .isNotEmpty();

        // The publish hole: the source is gone for a moment, while a publish request is being served.
        repository.removeResource(REGISTRY_PATH);
        repository.createResource(PROBE_REGISTRY_PATH, PROBE_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        runPassesWhilePublishIsInFlight();

        assertThat(javaFileService.findByLocation(LOCATION)).as("an artefact whose source vanished mid-publish must not be cleaned up")
                                                            .isNotEmpty();

        // Once the publish is over, the deletion is reconciled as before - in a single forced call.
        synchronizationProcessor.forceProcessSynchronizers();

        assertThat(javaFileService.findByLocation(LOCATION)).as("a genuinely deleted source is still cleaned up")
                                                            .isEmpty();
    }

    /**
     * Drives synchronization passes while a publish is in flight - the state a real publish request is
     * in between deleting a collection and copying it back. Returns once the probe artefact proves a
     * pass has actually completed under those conditions.
     */
    private void runPassesWhilePublishIsInFlight() {
        registryMutationTracker.enter();
        try {
            Awaitility.await()
                      .atMost(PASS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                      .pollInterval(500, TimeUnit.MILLISECONDS)
                      .until(() -> {
                          synchronizationWatcher.force();
                          synchronizationProcessor.processSynchronizers();
                          return !javaFileService.findByLocation(PROBE_LOCATION)
                                                 .isEmpty();
                      });
        } finally {
            registryMutationTracker.exit();
        }
    }

    @AfterEach
    void removeSourcesFromRegistry() {
        boolean removed = false;
        for (String path : List.of(REGISTRY_PATH, PROBE_REGISTRY_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                removed = true;
            }
        }
        if (removed) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }
}
