/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates a bearer token outside the HTTP security chain - for a STOMP CONNECT frame, whose
 * token travels in a frame header rather than in the HTTP handshake. Implemented by the security
 * profiles that accept bearer tokens; a profile without such a bean accepts none.
 */
public interface BearerTokenAuthenticator {

    /**
     * Authenticates a bearer token by exactly the rules the HTTP chain applies to it.
     *
     * @param bearerToken the token, without the {@code Bearer } scheme prefix
     * @return the authenticated identity
     * @throws AuthenticationException when the token is not acceptable
     */
    AuthenticatedBearerToken authenticate(String bearerToken) throws AuthenticationException;
}
