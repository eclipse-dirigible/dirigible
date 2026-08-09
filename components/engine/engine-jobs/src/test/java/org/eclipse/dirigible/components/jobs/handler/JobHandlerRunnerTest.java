/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Dispatching a job's handler to the engine it declares. The Java branch is what the manual
 * trigger-now path was missing (#6305); the JavaScript branch runs a Graal code runner and is
 * covered end-to-end by the job integration tests instead.
 */
class JobHandlerRunnerTest {

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaJobExecutor> provider(JavaJobExecutor executor) {
        ObjectProvider<JavaJobExecutor> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(executor);
        return provider;
    }

    @Test
    void aJavaJobIsRunByTheJavaExecutor() throws Exception {
        JavaJobExecutor executor = mock(JavaJobExecutor.class);

        new JobHandlerRunner(provider(executor)).run("app.jobs.CleanupJob", JavaJobExecutor.ENGINE_JAVA);

        verify(executor).execute("app.jobs.CleanupJob");
    }

    @Test
    void aJavaJobWithoutAnExecutorFailsLoudly() {
        JobHandlerRunner runner = new JobHandlerRunner(provider(null));

        // Rather than silently doing nothing - the caller logs the run as failed, so the Jobs
        // perspective shows it instead of reporting a run that never happened.
        assertThrows(IllegalStateException.class, () -> runner.run("app.jobs.CleanupJob", JavaJobExecutor.ENGINE_JAVA));
    }

    @Test
    void aJavaScriptJobNeverReachesTheJavaExecutor() {
        JavaJobExecutor executor = mock(JavaJobExecutor.class);
        JobHandlerRunner runner = new JobHandlerRunner(provider(executor));

        // No engine (a plain .job artefact) means the JavaScript runner - which needs a repository, so
        // it throws here. What this asserts is that the Java executor was not consulted.
        assertThrows(Exception.class, () -> runner.run("project/job.mjs", null));

        verifyNoInteractions(executor);
    }
}
