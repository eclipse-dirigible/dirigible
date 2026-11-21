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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Unit tests for {@link CognitoTriggerMessages} - the person reads the sentence the trigger's
 * author wrote, not the user pool's wrapping of it.
 */
class CognitoTriggerMessagesTest {

    private static final String SENTENCE = "You need an invitation to sign in this way. Ask the owner of your workspace to invite you";

    private final CognitoTriggerMessages messages = new CognitoTriggerMessages();

    @Test
    void unwrapsAPreSignUpRefusalOfTheAuthorizationResponse() {
        OAuth2Error error = new OAuth2Error("invalid_request", "PreSignUp failed with error " + SENTENCE + ". ", null);

        assertEquals(SENTENCE, messages.messageOf(error));
    }

    @Test
    void keepsTheFullStopTheTriggersAuthorWrote() {
        OAuth2Error error = new OAuth2Error("invalid_request", "PreAuthentication failed with error " + SENTENCE + ".. ", null);

        assertEquals(SENTENCE + ".", messages.messageOf(error));
    }

    @Test
    void unwrapsAPreTokenGenerationRefusalOfTheTokenEndpoint() {
        OAuth2Error error = new OAuth2Error("PreTokenGeneration failed with error " + SENTENCE
                + " (Service: AWSCognitoIdentityProviderService; Status Code: 400; Error Code: UserLambdaValidationException)");

        assertEquals(SENTENCE, messages.messageOf(error));
    }

    @Test
    void showsAnyOtherDescriptionAsItIs() {
        assertEquals("User is disabled.", messages.messageOf(new OAuth2Error("invalid_request", "User is disabled.", null)));
    }

    @Test
    void hasNothingForARefusalWithoutDescription() {
        assertNull(messages.messageOf(new OAuth2Error("access_denied")));
    }
}
