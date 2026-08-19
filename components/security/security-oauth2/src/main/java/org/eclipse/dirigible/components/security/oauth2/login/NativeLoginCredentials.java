/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

/**
 * First-party credentials for a native login. The password transits the platform only for the
 * duration of the authentication call - implementations and callers must never log or retain it.
 *
 * @param username the username as entered by the user
 * @param password the password as entered by the user
 * @param userContextData the optional provider-specific client-side collector blob (e.g. Cognito's
 *        threat-protection context), forwarded verbatim so provider-side risk scoring keeps working
 *        behind the server-side proxy
 */
public record NativeLoginCredentials(String username, String password, String userContextData) {

    /**
     * Keeps the password out of the record's generated string form.
     *
     * @return the string form without credential material
     */
    @Override
    public String toString() {
        return "NativeLoginCredentials[username=" + username + "]";
    }
}
