/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.service;

/**
 * Tells whether the record a process instance runs for has been deleted (issue #8108).
 *
 * <p>
 * A process started for a record carries the record's key in its {@code __entityId} variable. The
 * engine itself cannot tell whether that record still exists - the module that started the process
 * can, and implements this. Before a task of such a process is completed, the platform asks: a task
 * whose record is gone is refused and its process retired, instead of driving the flow's steps over
 * a row that no longer exists.
 */
public interface ProcessRecordGuard {

    /**
     * Whether the record an instance of the process runs for is confirmed deleted.
     *
     * @param processDefinitionKey the key of the instance's process definition
     * @param recordId the record's key, as text
     * @return true only when the record is known to be gone; false when it exists, or when this guard
     *         does not know the process
     */
    boolean isRecordGone(String processDefinitionKey, String recordId);
}
