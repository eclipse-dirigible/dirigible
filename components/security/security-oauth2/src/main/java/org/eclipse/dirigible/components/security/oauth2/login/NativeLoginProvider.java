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

import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * First-party credential sign-in against the identity provider behind an OAuth2 login profile.
 *
 * <p>
 * An implementation authenticates the user server-side with the provider's native API and returns
 * provider-validated tokens, so the platform can establish the same session the {@code oauth2Login}
 * authorization-code callback establishes - without ever rendering the provider-hosted login page.
 * The mechanics differ per provider (Cognito needs the SRP/challenge API, Keycloak could use the
 * password grant), which is why this is a contract and the profiles opt in independently.
 */
public interface NativeLoginProvider {

    /**
     * The client registration used when a login request does not name one.
     *
     * @return the default client registration id
     */
    String getDefaultRegistrationId();

    /**
     * Authenticates the user with the identity provider.
     *
     * @param registration the client registration the login is performed against
     * @param credentials the first-party credentials
     * @return the provider-issued tokens, or the challenge the provider requires next
     * @throws NativeLoginException on authentication failure, carrying a normalized outcome
     */
    NativeLoginResult authenticate(ClientRegistration registration, NativeLoginCredentials credentials);

    /**
     * Answers a challenge the provider returned from a previous {@link #authenticate} or
     * {@link #answerChallenge} call.
     *
     * @param registration the client registration the login is performed against
     * @param answer the challenge answer
     * @return the provider-issued tokens, or the next challenge
     * @throws NativeLoginException on authentication failure, carrying a normalized outcome
     */
    NativeLoginResult answerChallenge(ClientRegistration registration, NativeLoginChallengeAnswer answer);
}
