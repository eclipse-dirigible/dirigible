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

import java.nio.file.Path;

import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Runs a job's handler on the engine the job declares: {@value JavaJobExecutor#ENGINE_JAVA} routes
 * to the client-Java executor, anything else to the JavaScript runner (the handler is then a
 * repository path).
 *
 * <p>
 * Shared by both ways a job can start - the scheduled fire ({@link JobExecutionService}) and the
 * manual trigger-now ({@code JobService.trigger}) - so a job runs the same way however it was
 * started. The two dispatches used to be written out separately and drifted: trigger-now stayed
 * JavaScript-only, so triggering a client-Java job from the Jobs perspective tried to run its class
 * name as a JavaScript file and failed (dirigible #6305).
 */
@Component
public class JobHandlerRunner {

    private final ObjectProvider<JavaJobExecutor> javaJobExecutor;

    JobHandlerRunner(ObjectProvider<JavaJobExecutor> javaJobExecutor) {
        this.javaJobExecutor = javaJobExecutor;
    }

    /**
     * Run the handler.
     *
     * @param handler the job's handler - a client-Java FQN (optionally {@code #method}) for the Java
     *        engine, a repository path to a JavaScript module otherwise
     * @param engine the job's engine, or {@code null} for the default JavaScript one
     * @throws Exception when the job body throws, so the caller can log the run as failed
     */
    public void run(String handler, String engine) throws Exception {
        if (JavaJobExecutor.ENGINE_JAVA.equals(engine)) {
            JavaJobExecutor executor = javaJobExecutor.getIfAvailable();
            if (executor == null) {
                throw new IllegalStateException("No JavaJobExecutor is available to run the Java job [" + handler + "]");
            }
            executor.execute(handler);
            return;
        }
        try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner()) {
            runner.run(Path.of(handler));
        }
    }
}
