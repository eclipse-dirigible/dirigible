/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.domain;

import com.google.gson.annotations.Expose;

public class Authentication {

    public static final String TYPE_BASIC = "basic";

    @Expose
    private String type;

    @Expose
    private BasicAuthCredentials credentials;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BasicAuthCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(BasicAuthCredentials credentials) {
        this.credentials = credentials;
    }
}
