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
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

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
// One Dirigible boot for the whole class: each method cleans up after itself (or is read-only), so
// the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

    /** The project published through the service (not the HTTP endpoint) by the contract test below. */
    private static final String WORKSPACE = "workspace";

    private static final String PUBLISH_PROJECT = "cleanup-race-publish-it";

    private static final String PUBLISH_SOURCE_PATH = "/Published.java";

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

    @Autowired
    private PublisherService publisherService;

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
     * The deferral above is only as good as what feeds the tracker. It used to be fed by a servlet
     * filter mapped on the publisher and workspace endpoints - so a publish performed by anything else
     * was invisible, and the most common publisher in practice is NOT that endpoint: the model-to-code
     * generation service publishes through the JS {@code lifecycle} API, from a
     * {@code /services/js/...} URL the filter never sees. A pass racing that publish deleted the
     * artefacts of the files it was about to copy back, the client-Java batch then compiled a
     * half-empty codebase to ZERO class files, and no controller was left registered (#6654).
     *
     * <p>
     * The tracker is therefore marked by {@code PublisherService} itself. This asserts that contract at
     * the only place it can be asserted without racing anything: a publish that never touches the HTTP
     * layer must still register as a completed registry mutation, because that count is exactly what
     * the pass compares to decide whether to defer.
     */
    @Test
    void a_publish_that_bypasses_the_http_layer_still_marks_the_registry_as_mutating() {
        String user = UserFacade.getName();
        String workspacePath = IRepositoryStructure.PATH_USERS + "/" + user + "/" + WORKSPACE + "/" + PUBLISH_PROJECT + PUBLISH_SOURCE_PATH;
        repository.createResource(workspacePath, SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);

        long publishesBefore = registryMutationTracker.completedMutations();
        publisherService.publish(user, WORKSPACE, PUBLISH_PROJECT, "");
        assertThat(registryMutationTracker.completedMutations()).as(
                "a publish through the service - the path the generation service uses - must mark the registry mutation")
                                                                .isGreaterThan(publishesBefore);

        long unpublishesBefore = registryMutationTracker.completedMutations();
        publisherService.unpublish(PUBLISH_PROJECT);
        assertThat(registryMutationTracker.completedMutations()).as("so must the unpublish that opens the hole")
                                                                .isGreaterThan(unpublishesBefore);
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
        for (String path : List.of(REGISTRY_PATH, PROBE_REGISTRY_PATH,
                IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PUBLISH_PROJECT + PUBLISH_SOURCE_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        // Unconditionally: the publish-bypass test unpublishes through the service without a sync, so
        // no resource is left for the loop above to notice - but the registered JavaFile artefact for
        // the vanished source still needs the cleanup pass, or every later rebuild bails and defers.
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
