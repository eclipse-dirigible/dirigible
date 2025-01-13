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
public enum ArtefactLifecycle {

    /** The new. */
    NEW("NEW"),
    /** The modified. */
    MODIFIED("MODIFIED"),
    /** The prepared. */
    PREPARED("PREPARED"),
    /** The created. */
    CREATED("CREATED"),
    /** The updated. */
    UPDATED("UPDATED"),
    /** The deleted. */
    DELETED("DELETED"),
    /** The started. */
    STARTED("STARTED"),
    /** The failed. */
    FAILED("FAILED"),
    /** The failed. */
    FATAL("FATAL");

    /** The status. */
    private String status;

    /**
     * Instantiates a new artefact status.
     *
     * @param status the status
     */
    ArtefactLifecycle(String status) {
        this.status = status;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return this.status;
    }

}
