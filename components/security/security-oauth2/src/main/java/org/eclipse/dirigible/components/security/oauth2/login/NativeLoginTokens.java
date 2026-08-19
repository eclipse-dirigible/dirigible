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
 * Tokens issued by the identity provider for a completed native login. The ID token is validated
 * against the provider JWKS before any session is established; the tokens themselves never reach
 * the browser.
 *
 * @param idToken the OIDC ID token
 * @param accessToken the access token
 * @param refreshToken the refresh token, or {@code null} when the provider issued none
 * @param expiresInSeconds the access token lifetime in seconds, or {@code null} when unknown
 */
public record NativeLoginTokens(String idToken, String accessToken, String refreshToken, Long expiresInSeconds)
        implements NativeLoginResult {

    /**
     * Keeps the token material out of the record's generated string form.
     *
     * @return the string form without token material
     */
    @Override
    public String toString() {
        return "NativeLoginTokens[expiresInSeconds=" + expiresInSeconds + "]";
    }
}
