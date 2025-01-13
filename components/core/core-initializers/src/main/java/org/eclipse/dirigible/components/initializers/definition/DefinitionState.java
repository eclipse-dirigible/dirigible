/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.definition;

/**
 * The Enum ArtefactState.
 */
public enum DefinitionState {

    /** The file definition is brand new. */
    NEW("NEW"),

    /** The file definition is successfully parsed. */
    PARSED("PARSED"),

    /** The file definition cannot be parsed. */
    BROKEN("BROKEN"),

    /** The file definition is modified. */
    MODIFIED("MODIFIED"),

    /** The file definition is deleted. */
    DELETED("DELETED");

    /** The state. */
    private String state;

    /**
     * Instantiates a new artefact state.
     *
     * @param state the state
     */
    DefinitionState(String state) {
        this.state = state;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return this.state;
    }

}
