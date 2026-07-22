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

/**
 * SPI that runs a client-Java scheduled job identified by its handler string, invoked by
 * {@link JobExecutionService} when a job's engine is {@value #ENGINE_JAVA}. The implementation
 * lives in the Java engine (which can resolve and invoke the client bean); this interface keeps the
 * jobs engine free of a compile dependency on it (jobs -> javascript only). When no implementation
 * is on the classpath a {@value #ENGINE_JAVA} job fails loudly rather than silently no-op'ing.
 */
public interface JavaJobExecutor {

    /** The engine discriminator that routes a job to this executor (vs the default JS runner). */
    String ENGINE_JAVA = "java";

    /**
     * The synthetic {@code location} prefix under which the Java engine registers a client-Java job's
     * {@code Job} row. These rows are runtime-registered (from compiled client classes), not backed by
     * a registry artefact, so the job synchronizer must NOT reap them as orphans - it skips any job
     * whose location starts with this prefix.
     */
    String RUNTIME_LOCATION_PREFIX = "/__runtime_java_job__/";

    /**
     * Run the client-Java job.
     *
     * @param handler the job handler string - the fully-qualified class name of the client
     *        {@code JobHandler} bean, optionally suffixed {@code #method} for a {@code @Scheduled}
     *        method.
     * @throws Exception when the job body throws - surfaced by the caller as a job failure (logged to
     *         the job log like any other engine).
     */
    void execute(String handler) throws Exception;
}
