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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ProjectStatus.
 */
public class ProjectStatus {

    /** The Constant DIRIGIBLE_FOLDER. */
    private static final String DIRIGIBLE_FOLDER = "/dirigible/";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ProjectStatus.class);

    /** The project. */
    private String project;

    /** The git. */
    private String git;

    /** The added. */
    private Set<String> added;

    /** The changed. */
    private Set<String> changed;

    /** The removed. */
    private Set<String> removed;

    /** The missing. */
    private Set<String> missing;

    /** The modified. */
    private Set<String> modified;

    /** The conflicting. */
    private Set<String> conflicting;

    /** The untracked. */
    private Set<String> untracked;

    /** The untracked folders. */
    private Set<String> untrackedFolders;

    /**
     * Instantiates a new project status.
     *
     * @param project the project
     * @param git the git
     * @param added the added
     * @param changed the changed
     * @param removed the removed
     * @param missing the missing
     * @param modified the modified
     * @param conflicting the conflicting
     * @param untracked the untracked
     * @param untrackedFolders the untracked folders
     */
    public ProjectStatus(String project, String git, Set<String> added, Set<String> changed, Set<String> removed, Set<String> missing,
            Set<String> modified, Set<String> conflicting, Set<String> untracked, Set<String> untrackedFolders) {
        super();
        this.project = project;
        this.git = git;
        this.added = added; // remapFilesIfNeeded(added, project);
        this.changed = changed; // remapFilesIfNeeded(changed, project);
        this.removed = removed; // remapFilesIfNeeded(removed, project);
        this.missing = missing; // remapFilesIfNeeded(missing, project);
        this.modified = modified; // remapFilesIfNeeded(modified, project);
        this.conflicting = conflicting; // remapFilesIfNeeded(conflicting, project);
        this.untracked = untracked; // remapFilesIfNeeded(untracked, project);
        this.untrackedFolders = untrackedFolders; // remapFilesIfNeeded(untrackedFolders, project);
    }

    /**
     * Gets the project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * Gets the git.
     *
     * @return the git
     */
    public String getGit() {
        return git;
    }

    /**
     * Gets the added.
     *
     * @return the added
     */
    public Set<String> getAdded() {
        return added;
    }

    /**
     * Gets the changed.
     *
     * @return the changed
     */
    public Set<String> getChanged() {
        return changed;
    }

    /**
     * Gets the removed.
     *
     * @return the removed
     */
    public Set<String> getRemoved() {
        return removed;
    }

    /**
     * Gets the missing.
     *
     * @return the missing
     */
    public Set<String> getMissing() {
        return missing;
    }

    /**
     * Gets the modified.
     *
     * @return the modified
     */
    public Set<String> getModified() {
        return modified;
    }

    /**
     * Gets the conflicting.
     *
     * @return the conflicting
     */
    public Set<String> getConflicting() {
        return conflicting;
    }

    /**
     * Gets the untracked.
     *
     * @return the untracked
     */
    public Set<String> getUntracked() {
        return untracked;
    }

    /**
     * Gets the untracked folders.
     *
     * @return the untracked folders
     */
    public Set<String> getUntrackedFolders() {
        return untrackedFolders;
    }

}
