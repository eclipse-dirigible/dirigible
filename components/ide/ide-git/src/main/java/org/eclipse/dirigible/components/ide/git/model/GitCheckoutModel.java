/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.model;


/**
 * The Git Checkout Model.
 */
public class GitCheckoutModel extends BaseGitModel {

    /** The project. */
    private String project;

    /** The publish. */
    private boolean publish;

    /**
     * Gets the project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the project.
     *
     * @param project the new project
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Checks if is publish.
     *
     * @return true, if is publish
     */
    public boolean isPublish() {
        return publish;
    }

    /**
     * Sets the publish.
     *
     * @param publish the new publish
     */
    public void setPublish(boolean publish) {
        this.publish = publish;
    }

}
