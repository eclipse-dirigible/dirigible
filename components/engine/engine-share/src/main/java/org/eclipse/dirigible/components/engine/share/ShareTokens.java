/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.share;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * The secret of a share link (issue #8613): 256 random bits from a CSPRNG, URL-safe, and stored
 * only as its SHA-256. It is deliberately unrelated to anything a document carries - a record's
 * {@code uuid} is printed on the document and present in exports, so it identifies and never
 * authorizes.
 *
 * <p>
 * A token may be prefixed with the tenant it belongs to ({@code <tenantId>.<secret>}). That is the
 * only way a request without a session can name its tenant where one host serves every tenant
 * ({@code TOKEN_GROUPS}); the prefix is an address, not a secret, and the hash covers the whole
 * value.
 */
final class ShareTokens {

    /** Separates the tenant a token is addressed to from its secret; not a base64url character. */
    static final char TENANT_SEPARATOR = '.';

    /**
     * The longest token the public route looks at - anything longer is not one, and is not hashed.
     */
    static final int MAX_LENGTH = 200;

    private static final int SECRET_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ShareTokens() {}

    /**
     * Draws a new secret.
     *
     * @return 43 base64url characters carrying 256 random bits
     */
    static String newSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(bytes);
    }

    /**
     * The value a token is stored and looked up by.
     *
     * @param token the plain token
     * @return its SHA-256, as 64 hex characters
     */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                            .formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * The tenant a token is addressed to.
     *
     * @param token the plain token
     * @return the tenant id, or {@code null} when the token carries none
     */
    static String tenantOf(String token) {
        int separator = token.lastIndexOf(TENANT_SEPARATOR);
        return separator <= 0 ? null : token.substring(0, separator);
    }
}
