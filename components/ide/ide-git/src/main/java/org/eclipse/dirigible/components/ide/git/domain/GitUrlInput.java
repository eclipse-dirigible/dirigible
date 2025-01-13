/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

/**
 * The Class GitUrlInput.
 */
public class GitUrlInput {

    /** The url. */
    private String url;

    /**
     * Instantiates a new git url input.
     */
    public GitUrlInput() {
        super();
    }

    /**
     * Instantiates a new git url input.
     *
     * @param url the url
     */
    public GitUrlInput(String url) {
        super();
        this.url = url;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

}
