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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CognitoUserPool} - the pool coordinates derived from a registration's
 * issuer URI.
 */
class CognitoUserPoolTest {

    @Test
    void parsesThePoolCoordinatesFromTheIssuer() {
        CognitoUserPool pool = CognitoUserPool.fromIssuer("https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_AbCdEfGhI");

        assertEquals("https://cognito-idp.eu-central-1.amazonaws.com/", pool.endpoint());
        assertEquals("eu-central-1_AbCdEfGhI", pool.poolId());
        assertEquals("AbCdEfGhI", pool.poolName());
    }

    @Test
    void refusesANonCognitoIssuer() {
        assertThrows(IllegalStateException.class, () -> CognitoUserPool.fromIssuer("https://idp.example.org/realms/master"));
    }

    @Test
    void refusesAMissingIssuer() {
        assertThrows(IllegalStateException.class, () -> CognitoUserPool.fromIssuer(null));
    }
}
