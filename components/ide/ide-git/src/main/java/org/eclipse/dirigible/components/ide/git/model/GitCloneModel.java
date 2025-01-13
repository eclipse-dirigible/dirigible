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
 * The Git Clone Model.
 */
public class GitCloneModel extends BaseGitModel {

    /** The repository. */
    private String repository;

    /** The publish. */
    private boolean publish;

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the repository.
     *
     * @param repository the new repository
     */
    public void setRepository(String repository) {
        this.repository = repository;
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
