/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The secrets are stored sealed: never in plain text, never twice the same, and unreadable under
 * another key.
 */
class WebhookSecretCipherTest {

    private static final String SECRET = "a-signing-secret-of-enough-length";

    private static void useKey(byte fill) {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, fill);
        DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.setStringValue(Base64.getEncoder()
                                                                            .encodeToString(key));
    }

    @AfterEach
    void clearTheKey() {
        Configuration.remove(DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getKey());
    }

    @Test
    void roundTripsWithoutEverStoringThePlainSecret() {
        useKey((byte) 7);
        String first = WebhookSecretCipher.encrypt(SECRET);
        String second = WebhookSecretCipher.encrypt(SECRET);
        assertTrue(first.startsWith("v1:"));
        assertFalse(first.contains(SECRET));
        assertNotEquals(first, second, "a fresh nonce per seal");
        assertEquals(SECRET, WebhookSecretCipher.decrypt(first));
        assertEquals(SECRET, WebhookSecretCipher.decrypt(second));
    }

    @Test
    void anotherKeyCannotReadIt() {
        useKey((byte) 7);
        String sealed = WebhookSecretCipher.encrypt(SECRET);
        useKey((byte) 8);
        assertThrows(IllegalStateException.class, () -> WebhookSecretCipher.decrypt(sealed));
    }

    @Test
    void withoutAUsableKeyNothingIsSealed() {
        assertFalse(WebhookSecretCipher.isConfigured());
        assertThrows(IllegalStateException.class, () -> WebhookSecretCipher.encrypt(SECRET));
        DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.setStringValue(Base64.getEncoder()
                                                                            .encodeToString(new byte[16]));
        assertFalse(WebhookSecretCipher.isConfigured(), "AES-256 needs 32 bytes");
    }
}
