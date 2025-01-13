/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.domain;

/**
 * The Class WorkspaceFromToPair.
 */
public class WorkspaceFromToPair {

    /** The from. */
    private String from;

    /** The to. */
    private String to;

    /**
     * Instantiates a new workspace from to pair.
     *
     * @param from the from
     * @param to the to
     */
    public WorkspaceFromToPair(String from, String to) {
        super();
        this.from = from;
        this.to = to;
    }

    /**
     * Gets the from.
     *
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the from.
     *
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Gets the to.
     *
     * @return the to
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the to.
     *
     * @param to the to to set
     */
    public void setTo(String to) {
        this.to = to;
    }



}
