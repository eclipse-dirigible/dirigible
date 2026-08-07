/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.endpoint;

import org.eclipse.dirigible.components.base.artefact.Artefact;

/**
 * The synchronization status of a single artefact, as exposed by {@link ArtefactStatusEndpoint}.
 *
 * @param location the registry-relative location of the source file
 * @param name the artefact name
 * @param type the artefact type, e.g. {@code job} or {@code table}
 * @param phase the last lifecycle phase applied to the artefact, e.g. {@code CREATE}
 * @param status the outcome of that phase, e.g. {@code CREATED} or {@code FAILED}
 * @param error the error message of a failed phase, or null
 * @param running whether the artefact is currently running
 */
record ArtefactStatus(String location, String name, String type, String phase, String status, String error, Boolean running) {

    /**
     * Projects an artefact onto its status.
     *
     * @param artefact the artefact
     * @return the status
     */
    static ArtefactStatus of(Artefact artefact) {
        return new ArtefactStatus(artefact.getLocation(), artefact.getName(), artefact.getType(), name(artefact.getPhase()),
                name(artefact.getLifecycle()), artefact.getError(), artefact.getRunning());
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
