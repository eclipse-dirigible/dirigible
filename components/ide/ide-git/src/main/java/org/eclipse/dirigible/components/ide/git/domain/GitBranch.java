/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

/**
 * The Branch representation POJO.
 */
public class GitBranch {

    /** The name. */
    private String name;

    /** The remote. */
    private boolean remote;

    /** The current. */
    private boolean current;

    /** The commit object id. */
    private String commitObjectId;

    /** The commit short id. */
    private String commitShortId;

    /** The commit date. */
    private String commitDate;

    /** The commit message. */
    private String commitMessage;

    /** The commit author. */
    private String commitAuthor;

    /**
     * The constructor.
     *
     * @param name the name
     * @param remote is remote
     * @param current is current
     * @param commitObjectId the full object id
     * @param commitShortId the short object id
     * @param commitDate the commit date
     * @param commitMessage the message
     * @param commitAuthor the author
     */
    public GitBranch(String name, boolean remote, boolean current, String commitObjectId, String commitShortId, String commitDate,
            String commitMessage, String commitAuthor) {
        super();
        this.name = name;
        this.remote = remote;
        this.current = current;
        this.commitObjectId = commitObjectId;
        this.commitShortId = commitShortId;
        this.commitDate = commitDate;
        this.commitMessage = commitMessage;
        this.commitAuthor = commitAuthor;
    }

    /**
     * Getter for name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for remote.
     *
     * @return the remote
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * Setter for remote.
     *
     * @param remote the remote to set
     */
    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    /**
     * Getter for current.
     *
     * @return the current
     */
    public boolean isCurrent() {
        return current;
    }

    /**
     * Setter for current.
     *
     * @param current the current to set
     */
    public void setCurrent(boolean current) {
        this.current = current;
    }

    /**
     * Getter for full object id.
     *
     * @return the commitObjectId
     */
    public String getCommitObjectId() {
        return commitObjectId;
    }

    /**
     * Setter for full object id.
     *
     * @param commitObjectId the commitObjectId to set
     */
    public void setCommitObjectId(String commitObjectId) {
        this.commitObjectId = commitObjectId;
    }

    /**
     * Getter for short object id.
     *
     * @return the commitShortId
     */
    public String getCommitShortId() {
        return commitShortId;
    }

    /**
     * Setter for short object id.
     *
     * @param commitShortId the commitShortId to set
     */
    public void setCommitShortId(String commitShortId) {
        this.commitShortId = commitShortId;
    }

    /**
     * Getter for commit date.
     *
     * @return the commitDate
     */
    public String getCommitDate() {
        return commitDate;
    }

    /**
     * Setter for commit date.
     *
     * @param commitDate the commitDate to set
     */
    public void setCommitDate(String commitDate) {
        this.commitDate = commitDate;
    }

    /**
     * Getter for commit message.
     *
     * @return the commitMessage
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Setter for the commit message.
     *
     * @param commitMessage the commitMessage to set
     */
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    /**
     * Getter for the commit author.
     *
     * @return the commitAuthor
     */
    public String getCommitAuthor() {
        return commitAuthor;
    }

    /**
     * Setter for the commit author.
     *
     * @param commitAuthor the commitAuthor to set
     */
    public void setCommitAuthor(String commitAuthor) {
        this.commitAuthor = commitAuthor;
    }



}
