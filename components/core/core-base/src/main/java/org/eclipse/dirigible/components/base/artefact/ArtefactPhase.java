/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact;

/**
 * The Enum ArtefactLifecycle.
 */
public enum ArtefactPhase {

    /** The phase where the artifact should perform stateful operations for preparation. */
    PREPARE("PREPARE"),
    /** The phase where the artifact should perform stateful operations for create. */
    CREATE("CREATE"),
    /** The phase where the artifact should perform stateful operations for update. */
    UPDATE("UPDATE"),
    /** The phase where the artifact should perform stateful operations for delete. */
    DELETE("DELETE"),
    /** The phase where the artifact should perform stateless operations for start. */
    START("START"),
    /** The phase where the artifact should perform stateless operations for stop. */
    STOP("STOP");

    /** The phase. */
    private String phase;

    /**
     * Instantiates a new artefact phase.
     *
     * @param phase the phase
     */
    ArtefactPhase(String phase) {
        this.phase = phase;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return this.phase;
    }

}
