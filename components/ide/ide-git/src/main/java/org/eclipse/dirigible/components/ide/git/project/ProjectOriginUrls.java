/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.project;

/**
 * The Class ProjectOriginUrls.
 */
public class ProjectOriginUrls {

    /** The fetch url. */
    private String fetchUrl;

    /** The push url. */
    private String pushUrl;

    /**
     * Instantiates a new project origin urls.
     *
     * @param fetchUrl the fetch url
     * @param pushUrl the push url
     */
    public ProjectOriginUrls(String fetchUrl, String pushUrl) {
        this.fetchUrl = fetchUrl;
        this.pushUrl = pushUrl;
    }

    /**
     * Gets the fetch url.
     *
     * @return the fetch url
     */
    public String getFetchUrl() {
        return fetchUrl;
    }

    /**
     * Gets the push url.
     *
     * @return the push url
     */
    public String getPushUrl() {
        return pushUrl;
    }

    /**
     * Sets the fetch url.
     *
     * @param fetchURL the new fetch url
     */
    public void setFetchUrl(String fetchURL) {
        this.fetchUrl = fetchURL;
    }

    /**
     * Sets the push url.
     *
     * @param pushURL the new push url
     */
    public void setPushUrl(String pushURL) {
        this.pushUrl = pushURL;
    }
}
