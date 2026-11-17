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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.dirigible.commons.config.DirigibleConfig;

/**
 * Encrypts the signing secrets of the webhook subscriptions at rest, with AES-256-GCM under
 * {@link DirigibleConfig#TENANT_WEBHOOKS_ENCRYPTION_KEY}. A stored value is {@code v1:} followed by
 * the Base64 of the 12-byte nonce and the ciphertext with its tag; the prefix leaves room for a
 * later scheme.
 *
 * <p>
 * The key is read on every call, so a test (or an operator, after a restart) can set it without a
 * context refresh. Without a key nothing is encrypted and a registry naming secrets is refused.
 */
final class WebhookSecretCipher {

    private static final String PREFIX = "v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int NONCE_LENGTH = 12;

    private static final int TAG_BITS = 128;

    private static final int KEY_LENGTH = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private WebhookSecretCipher() {}

    /**
     * Whether a usable key is configured.
     *
     * @return true when the key is set and is 32 bytes of Base64
     */
    static boolean isConfigured() {
        try {
            key();
            return true;
        } catch (IllegalStateException notConfigured) {
            return false;
        }
    }

    /**
     * Encrypts a secret.
     *
     * @param plain the secret
     * @return the stored form
     * @throws IllegalStateException when no usable key is configured
     */
    static String encrypt(String plain) {
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] stored = ByteBuffer.allocate(nonce.length + sealed.length)
                                      .put(nonce)
                                      .put(sealed)
                                      .array();
            return PREFIX + Base64.getEncoder()
                                  .encodeToString(stored);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt a webhook signing secret", ex);
        }
    }

    /**
     * Decrypts a stored secret.
     *
     * @param stored the stored form
     * @return the secret
     * @throws IllegalStateException when no usable key is configured, or the value was not sealed with
     *         it
     */
    static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalStateException("A stored webhook signing secret is not in a known form");
        }
        byte[] bytes = Base64.getDecoder()
                             .decode(stored.substring(PREFIX.length()));
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, bytes, 0, NONCE_LENGTH));
            byte[] plain = cipher.doFinal(bytes, NONCE_LENGTH, bytes.length - NONCE_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not decrypt a webhook signing secret - was the encryption key changed?", ex);
        }
    }

    private static SecretKeySpec key() {
        String configured = DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getStringValue();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getKey() + " is not set");
        }
        byte[] key;
        try {
            key = Base64.getDecoder()
                        .decode(configured.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getKey() + " is not Base64", ex);
        }
        if (key.length != KEY_LENGTH) {
            throw new IllegalStateException(DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.getKey() + " must be " + KEY_LENGTH + " bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
