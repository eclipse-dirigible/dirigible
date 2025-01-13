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
 * The Class GitDiffModel.
 */
public class GitDiffModel {

    /** The original. */
    private String original;

    /** The modified. */
    private String modified;

    /**
     * Instantiates a new git diff model.
     */
    public GitDiffModel() {

    }

    /**
     * Instantiates a new git diff model.
     *
     * @param original the original
     * @param modified the modified
     */
    public GitDiffModel(String original, String modified) {
        this.original = original;
        this.modified = modified;
    }

    /**
     * Gets the original.
     *
     * @return the original text
     */
    public String getOriginal() {
        return original;
    }

    /**
     * Sets the original.
     *
     * @param original the original text to set
     */
    public void setOriginal(String original) {
        this.original = original;
    }

    /**
     * Gets the modified.
     *
     * @return the modified text
     */
    public String getModified() {
        return modified;
    }

    /**
     * Sets the modified.
     *
     * @param modified the modified text to set
     */
    public void setModified(String modified) {
        this.modified = modified;
    }

}
