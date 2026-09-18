/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginChallengeAnswer;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginCredentials;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginProvider;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginResult;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginTokens;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * Test beans of {@link ExternalFrontendIT}.
 */
@TestConfiguration
class ExternalFrontendITConfig {

    /**
     * Stands in for the identity provider's native sign-in, and for that alone: it hands back the
     * tokens the IT's mock provider signs for the user. Everything after it - the ID token validation,
     * the session, the authorized client the revalidation filter governs - is the platform's own code,
     * which is what the native-login cases exercise. Only Cognito ships a real provider, and its SRP
     * handshake with the Cognito API cannot be replayed against a local mock.
     *
     * @return the provider
     */
    @Bean
    NativeLoginProvider nativeLoginProvider() {
        return new NativeLoginProvider() {

            @Override
            public String getDefaultRegistrationId() {
                return "keycloak";
            }

            @Override
            public NativeLoginResult authenticate(ClientRegistration registration, NativeLoginCredentials credentials) {
                ExternalFrontendIT.MockIdentityProvider identityProvider = ExternalFrontendIT.identityProvider;
                String username = credentials.username();
                return new NativeLoginTokens(identityProvider.idToken(username, ExternalFrontendIT.DEVELOPER_GROUPS),
                        identityProvider.accessToken("sub-" + username, "openid"), "refresh-" + username, 300L);
            }

            @Override
            public NativeLoginResult answerChallenge(ClientRegistration registration, NativeLoginChallengeAnswer answer) {
                throw new UnsupportedOperationException("The stub provider issues no challenges");
            }
        };
    }
}
