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
 * The Git Share Model.
 */
public class GitShareModel extends BaseGitModel {

    /** The project. */
    private String project;

    /** The repository. */
    private String repository;

    /** The commit message. */
    private String commitMessage;

    /** The share in root folder. */
    private boolean shareInRootFolder;

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
     * Gets the commit message.
     *
     * @return the commit message
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Sets the commit message.
     *
     * @param commitMessage the new commit message
     */
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    /**
     * Returns whether should share the project in the root folder.
     *
     * @return true if should share the project in the root folder
     */
    public boolean isShareInRootFolder() {
        return shareInRootFolder;
    }

    /**
     * Sets whether should share the project in the root folder.
     *
     * @param shareInRootFolder the value
     */
    public void setShareInRootFolder(boolean shareInRootFolder) {
        this.shareInRootFolder = shareInRootFolder;
    }
}
