/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

import java.net.URI;

/**
 * The coordinates of a Cognito user pool, derived from a client registration's issuer URI
 * ({@code https://cognito-idp.<region>.amazonaws.com/<poolId>}) - so the native login needs no
 * configuration beyond what the OAuth2 registration already carries.
 *
 * @param endpoint the {@code cognito-idp} API endpoint for the pool's region
 * @param poolId the user pool id (e.g. {@code eu-central-1_AbCdEfGhI})
 */
record CognitoUserPool(String endpoint, String poolId) {

    /**
     * Parses the pool coordinates from the registration's issuer URI.
     *
     * @param issuerUri the issuer URI
     * @return the user pool
     * @throws IllegalStateException when the issuer URI is not a Cognito user pool issuer
     */
    static CognitoUserPool fromIssuer(String issuerUri) {
        URI uri = issuerUri != null ? URI.create(issuerUri) : null;
        String host = uri != null ? uri.getHost() : null;
        String path = uri != null && uri.getPath() != null ? uri.getPath() : "";
        String poolId = path.startsWith("/") ? path.substring(1) : path;
        if (host == null || !host.startsWith("cognito-idp.") || !poolId.contains("_") || poolId.contains("/")) {
            throw new IllegalStateException("The issuer URI [" + issuerUri + "] is not a Cognito user pool issuer");
        }
        return new CognitoUserPool("https://" + host + "/", poolId);
    }

    /**
     * The pool name - the part of the pool id after the region prefix, used in the SRP password claim.
     *
     * @return the pool name
     */
    String poolName() {
        return poolId.substring(poolId.indexOf('_') + 1);
    }
}
