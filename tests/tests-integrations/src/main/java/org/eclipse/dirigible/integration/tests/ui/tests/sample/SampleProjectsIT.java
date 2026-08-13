/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import java.util.List;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.GitPerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Base for the sample-project ITs: clones a family of {@code dirigiblelabs/sample-*} repositories
 * into one workspace, publishes them together, and lets each subclass verify one sample per
 * {@code @Test} method.
 *
 * <p>
 * The clone-and-publish journey is identical for every sample, and it is the expensive part - a
 * Dirigible boot, a Chrome session and a full publish + synchronization cycle. Running it once per
 * sample cost the {@code samples} CI shard ~18 minutes for 14 near-identical journeys, so the
 * family shares one boot ({@link DirtiesContext.ClassMode#AFTER_CLASS} overrides the per-method
 * context reset inherited from the base) and one publish.
 *
 * <p>
 * Verifications must stay independent of each other and of their order: they run against the same
 * live instance, so a method may not rely on state another method leaves behind.
 *
 * <p>
 * The families are split by what can share a runtime, not by taste:
 * {@code sample-entity-decorators} and {@code sample-java-entity-decorators} both own the
 * {@code SAMPLE_COUNTRY} table and both seed it from their own CSVIM, so they cannot be published
 * side by side.
 */
// "sample" (on top of the inherited "ui") routes the whole sample-project family into its own CI
// shard - see the integration-tests matrix in .github/workflows/build.yml.
@Tag("sample")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class SampleProjectsIT extends UserInterfaceIntegrationTest {

    /**
     * The test class whose family is already cloned and published, or {@code null}.
     *
     * <p>
     * Static because the test instance is not: JUnit's default per-method lifecycle builds a fresh
     * instance for every test, while the Spring context - and the published instance behind it - lives
     * for the whole class. Keyed by class rather than a plain flag because this field is shared by
     * every subclass, and a flag would let the second family skip its own publish and verify against
     * the first family's instance.
     */
    private static Class<?> publishedFamily;

    @Autowired
    protected RestAssuredExecutor restAssuredExecutor;

    @Autowired
    protected SynchronizationProcessor synchronizationProcessor;

    /**
     * Clones and publishes the family once, before the first verification.
     *
     * <p>
     * Deliberately a guarded {@code @BeforeEach} and NOT a {@code @BeforeAll} on a
     * {@code @TestInstance(PER_CLASS)} class: PER_CLASS creates and autowires the test instance BEFORE
     * the {@code @BeforeAll} methods run, so the Spring context - and with it the whole platform -
     * starts before {@code IntegrationTest.cleanBeforeTestClassExecution()} deletes the Dirigible
     * folder. The next synchronization pass then reaps the platform's own registry
     * ({@code Definition deleted: /shell-ide/extensions/shell.extension}) and the IDE renders with no
     * perspectives at all. The default lifecycle keeps the cleaner ahead of the boot.
     */
    @BeforeEach
    final void cloneAndPublishSamplesOnce() {
        if (getClass().equals(publishedFamily)) {
            return;
        }

        ide.openHomePage();

        GitPerspective gitPerspective = ide.openGitPerspective();
        getRepositoryUrls().forEach(gitPerspective::cloneRepository);

        Workbench workbench = ide.openWorkbench();
        workbench.publishAll(true);

        synchronizationProcessor.forceProcessSynchronizers();

        // The registry watcher may deliver trailing publish events seconds later, scheduling one
        // more sync cycle that can re-register data-store entities (rebuilding their tables) while
        // the verification is already inserting data. Proceed only after the sync stays idle.
        SynchronizationUtil.waitForStableSynchronization();

        publishedFamily = getClass();
    }

    /**
     * The sample repositories to clone and publish together, in clone order.
     *
     * @return the repository URLs
     */
    protected abstract List<String> getRepositoryUrls();

}
