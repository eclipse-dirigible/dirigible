/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Where the assistant's request - and with it the API key it carries in a header - is allowed to
 * go.
 *
 * <p>
 * The base URL is a {@code Configuration} value, and on this platform that is less fixed than
 * "deployment configuration" suggests: a manually triggered job writes its parameters straight into
 * the global runtime configuration, so a caller privileged enough to trigger one can transiently
 * redefine any key. The endpoint is therefore rebuilt from a checked URL rather than concatenated
 * from whatever is configured.
 */
class ModelClientTest {

    private static final String BASE_URL_ENV = "DIRIGIBLE_INTENT_AI_BASE_URL";

    @AfterEach
    void resetBaseUrl() {
        Configuration.remove(BASE_URL_ENV);
    }

    @Test
    void anHttpsBaseUrlBecomesTheMessagesEndpoint() {
        Configuration.set(BASE_URL_ENV, "https://api.anthropic.com");

        assertEquals("https://api.anthropic.com/v1/messages", ModelClient.messagesEndpoint()
                                                                         .toString());
    }

    @Test
    void aTrailingSlashAndAGatewayPathAreKept() {
        // Pointing at a self-hosted gateway is the whole reason this key is configurable - both the
        // in-process tests and the Builder shell IT stub the upstream this way.
        Configuration.set(BASE_URL_ENV, "http://localhost:8099/anthropic/");

        assertEquals("http://localhost:8099/anthropic/v1/messages", ModelClient.messagesEndpoint()
                                                                               .toString());
    }

    @Test
    void aNonHttpSchemeIsRefused() {
        Configuration.set(BASE_URL_ENV, "file:///etc/passwd");

        assertThrows(AssistantNotConfiguredException.class, ModelClient::messagesEndpoint);
    }

    @Test
    void embeddedCredentialsAreRefused() {
        Configuration.set(BASE_URL_ENV, "https://someone:secret@evil.example.com");

        assertThrows(AssistantNotConfiguredException.class, ModelClient::messagesEndpoint);
    }

    @Test
    void aValueThatIsNotAnAbsoluteUrlIsRefused() {
        Configuration.set(BASE_URL_ENV, "api.anthropic.com");

        assertThrows(AssistantNotConfiguredException.class, ModelClient::messagesEndpoint);
    }
}
