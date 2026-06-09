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

/**
 * Basic-auth credentials for outbound auth injection. Values may be authored as {@code ${ENV_VAR}}
 * / {@code ${ENV_VAR}.{DEFAULT}} placeholders — they are resolved at parse time via
 * {@link org.eclipse.dirigible.commons.config.Configuration#configureObject(Object)
 * Configuration.configureObject}, so by the time the injector reads them they are the literal
 * username / password to send.
 */
public class BasicAuthCredentials {

    @Expose
    private String user;

    @Expose
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
