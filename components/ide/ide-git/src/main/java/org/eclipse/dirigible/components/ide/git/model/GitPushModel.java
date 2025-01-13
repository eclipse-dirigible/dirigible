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
 * The Git Push Model.
 */
public class GitPushModel extends BaseGitProjectModel {

    /** The commit message. */
    private String commitMessage;

    /** The auto add. */
    private boolean autoAdd;

    /** The auto commit. */
    private boolean autoCommit;



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
     * Checks if is auto add.
     *
     * @return the autoAdd
     */
    public boolean isAutoAdd() {
        return autoAdd;
    }

    /**
     * Sets the auto add.
     *
     * @param autoAdd the autoAdd to set
     */
    public void setAutoAdd(boolean autoAdd) {
        this.autoAdd = autoAdd;
    }

    /**
     * Checks if is auto commit.
     *
     * @return the autoCommit
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * Sets the auto commit.
     *
     * @param autoCommit the autoCommit to set
     */
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }



}
