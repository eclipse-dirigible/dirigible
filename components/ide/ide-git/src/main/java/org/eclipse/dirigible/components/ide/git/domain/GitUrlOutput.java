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
 * The Class GitUrlOutput.
 */
public class GitUrlOutput {

    /** The url. */
    private String url;

    /** The status. */
    private String status;

    /**
     * Instantiates a new git url input.
     */
    public GitUrlOutput() {
        super();
    }

    /**
     * Instantiates a new git url input.
     *
     * @param url the url
     * @param status the status
     */
    public GitUrlOutput(String url, String status) {
        super();
        this.url = url;
        this.status = url;
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

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }



}
