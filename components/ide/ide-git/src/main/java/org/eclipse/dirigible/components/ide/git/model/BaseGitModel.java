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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.dirigible.components.ide.workspace.project.ProjectMetadataManager;


/**
 * The Base Git Model.
 */
public class BaseGitModel {

    /** The username. */
    private String username;

    /** The password. */
    private String password;

    /** The email. */
    private String email;

    /** The branch. */
    private String branch = ProjectMetadataManager.BRANCH_MASTER;

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        if (password == null) {
            return null;
        }
        return new String(Base64.getDecoder()
                                .decode(password.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email the new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the branch.
     *
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the branch.
     *
     * @param branch the branch to set
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }


}
