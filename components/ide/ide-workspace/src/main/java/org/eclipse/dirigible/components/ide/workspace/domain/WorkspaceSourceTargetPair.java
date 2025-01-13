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
 * The Workspace Source Target Pair.
 */
public class WorkspaceSourceTargetPair {

    /** The source workspace. */
    private String sourceWorkspace;

    /** The sources. */
    private String[] sources;

    /** The target workspace. */
    private String targetWorkspace;

    /** The target. */
    private String target;

    /**
     * Gets the source workspace.
     *
     * @return the sourceWorkspace
     */
    public String getSourceWorkspace() {
        return sourceWorkspace;
    }

    /**
     * Sets the source.
     *
     * @param sourceWorkspace the new source workspace
     */
    public void setSourceWorkspace(String sourceWorkspace) {
        this.sourceWorkspace = sourceWorkspace;
    }

    /**
     * Gets the sources.
     *
     * @return the sources
     */
    public String[] getSources() {
        return sources;
    }

    /**
     * Sets the sources.
     *
     * @param sources the new sources
     */
    public void setSources(String[] sources) {
        this.sources = sources;
    }

    /**
     * Gets the target workspace.
     *
     * @return the targetWorkspace
     */
    public String getTargetWorkspace() {
        return targetWorkspace;
    }

    /**
     * Sets the target workspace.
     *
     * @param targetWorkspace the new target workspace
     */
    public void setTargetWorkspace(String targetWorkspace) {
        this.targetWorkspace = targetWorkspace;
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target.
     *
     * @param target the new target
     */
    public void setTarget(String target) {
        this.target = target;
    }

}
