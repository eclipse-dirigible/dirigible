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

import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Turns the error an identity provider answered a sign-in with into the sentence the person reads
 * on the login page. A profile whose provider wraps the sentence its operator wrote (Cognito's
 * Lambda triggers) supplies its own; the others show the provider's {@code error_description}.
 */
@FunctionalInterface
public interface OAuth2LoginFailureMessages {

    /**
     * The sentence to show for the error.
     *
     * @param error the provider's error - never one the client raised about a response it could not use
     * @return the sentence, or {@code null} when the provider gave none
     */
    String messageOf(OAuth2Error error);

    /**
     * Shows the provider's {@code error_description} as it is.
     *
     * @return the messages
     */
    static OAuth2LoginFailureMessages descriptions() {
        return OAuth2Error::getDescription;
    }
}
