/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

/**
 * The intent-wide settings of the properties the generator adds on its own - the ones no
 * {@code fields:} entry declares, so no field-level key can reach them (dirigible #8692).
 *
 * <pre>
 * systemProperties:
 *   processId: { visibleTo: [Support] }
 * </pre>
 */
public class SystemPropertiesIntent {

    /**
     * The {@code ProcessId} / {@code ProcessIds} pair an entity a process starts on carries. Absent,
     * the pages show them to every reader, as before.
     */
    private SystemPropertyIntent processId;

    public SystemPropertyIntent getProcessId() {
        return processId;
    }

    public void setProcessId(SystemPropertyIntent processId) {
        this.processId = processId;
    }
}
