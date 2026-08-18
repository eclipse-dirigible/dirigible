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

import java.util.Map;

/**
 * The answer to a {@link NativeLoginChallenge} returned by a previous authentication step.
 *
 * @param challenge the provider's challenge name being answered
 * @param session the opaque provider session state carried over from the challenge
 * @param username the username the challenge round-trip runs for (the canonical name from the
 *        challenge parameters when the provider returned one)
 * @param responses the challenge response parameters (e.g. the MFA code or the new password) -
 *        possibly credential material, so never logged or retained
 * @param userContextData the optional provider-specific client-side collector blob
 */
public record NativeLoginChallengeAnswer(String challenge, String session, String username, Map<String, String> responses,
        String userContextData) {

    /**
     * Keeps the possibly credential-bearing responses out of the record's generated string form.
     *
     * @return the string form without credential material
     */
    @Override
    public String toString() {
        return "NativeLoginChallengeAnswer[challenge=" + challenge + ", username=" + username + "]";
    }
}
