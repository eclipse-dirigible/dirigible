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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.dirigible.components.security.oauth2.login.OAuth2LoginFailureMessages;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * The sentence a Cognito Lambda trigger refused a sign-in with, as its author wrote it (#8801).
 *
 * <p>
 * The user pool wraps it: the authorization response carries
 * {@code error_description=PreSignUp failed with error <sentence>. } (the full stop and the space
 * are the pool's), and a refusing pre token generation trigger makes the token endpoint answer
 * {@code {"error":"PreTokenGeneration failed with error <sentence> (Service: ...)"}} - the sentence
 * in the error code, with no description. Both are unwrapped; any other error shows its description
 * as it is.
 */
class CognitoTriggerMessages implements OAuth2LoginFailureMessages {

    private static final Pattern TRIGGER_REFUSAL = Pattern.compile("(?:PreSignUp|PreAuthentication|PreTokenGeneration) failed with error ");

    /** What the token endpoint appends to the trigger's sentence. */
    private static final String SERVICE_DETAILS = " (Service: ";

    /** What the user pool appends to the trigger's sentence in the authorization response. */
    private static final String POOL_SUFFIX = ". ";

    @Override
    public String messageOf(OAuth2Error error) {
        String description = error.getDescription();
        String sentence = description != null ? triggerSentence(description) : null;
        if (sentence == null) {
            sentence = triggerSentence(error.getErrorCode());
        }
        return sentence != null ? sentence : description;
    }

    /**
     * The trigger's sentence in a text the user pool wrapped it in.
     *
     * @param text the description or the code of the error
     * @return the sentence, or {@code null} when the text is not a trigger's refusal
     */
    static String triggerSentence(String text) {
        Matcher matcher = TRIGGER_REFUSAL.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String sentence = text.substring(matcher.end());
        int serviceDetails = sentence.indexOf(SERVICE_DETAILS);
        if (serviceDetails >= 0) {
            sentence = sentence.substring(0, serviceDetails);
        } else if (sentence.endsWith(POOL_SUFFIX)) {
            sentence = sentence.substring(0, sentence.length() - POOL_SUFFIX.length());
        }
        sentence = sentence.strip();
        return sentence.isEmpty() ? null : sentence;
    }
}
