/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.bpm;

/**
 * A listener that starts a process for a record - the shape of every generated
 * {@code <Process>Trigger}, and the surface an operator repairs a record through.
 *
 * <p>
 * A trigger stamps the instance it started on the record ({@code ProcessIds}, {@code ProcessId})
 * and reads that stamp to start each process at most once. When the stamped instance no longer
 * exists - cancelled, lost with a deployment, gone with an ephemeral database - the record has no
 * task, the stamp still says "started", and the two system-owned columns are preserved over every
 * user write. {@link #restart(String)} is the way out that needs no SQL: the platform's
 * {@code POST /services/bpm/bpm-processes/restart?process=...&id=...} locates the trigger of the
 * process by {@link #process()} and re-runs it for the record, with the same variables and the same
 * stamping the create path uses.
 */
public interface ProcessTrigger {

    /**
     * The process definition key this trigger starts.
     *
     * @return the process name
     */
    String process();

    /**
     * The entity whose records this trigger starts the process for.
     *
     * @return the entity name
     */
    String entity();

    /**
     * Starts the process for the record regardless of its stamp, and stamps the new instance on it.
     *
     * @param id the record's primary key, as text
     * @return the started process-instance id
     * @throws IllegalArgumentException when no such record exists
     * @throws IllegalStateException when the stamped instance is still running, when the trigger's
     *         condition does not hold for the record, or when the start fails
     */
    String restart(String id);
}
