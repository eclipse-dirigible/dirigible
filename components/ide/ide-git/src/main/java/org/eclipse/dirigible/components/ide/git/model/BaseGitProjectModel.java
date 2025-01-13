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

import java.util.List;


/**
 * The Base Git Project Model.
 */
public class BaseGitProjectModel extends BaseGitModel {

    /** The projects. */
    private List<String> projects;

    /**
     * Gets the projects.
     *
     * @return the projects
     */
    public List<String> getProjects() {
        return projects;
    }

    /**
     * Sets the projects.
     *
     * @param projects the new projects
     */
    public void setProjects(List<String> projects) {
        this.projects = projects;
    }
}
