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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs a webhook delivery: {@code v1=<hex HMAC-SHA256(secret, timestamp + "." + body)>}, the key
 * being the secret's UTF-8 bytes and the timestamp the {@code Webhook-Timestamp} header's value
 * (Unix seconds). During a rotation the header carries one {@code v1=} per secret, comma-separated,
 * so a receiver holding either secret verifies the delivery.
 */
final class WebhookSigner {

    /** The signature scheme's version prefix. */
    static final String VERSION = "v1=";

    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSigner() {}

    /**
     * The {@code Webhook-Signature} header value.
     *
     * @param secrets the active secrets, current first; never empty
     * @param timestamp the {@code Webhook-Timestamp} value
     * @param body the exact body sent
     * @return the header value
     */
    static String signatureHeader(List<String> secrets, long timestamp, String body) {
        return secrets.stream()
                      .map(secret -> VERSION + sign(secret, timestamp, body))
                      .collect(Collectors.joining(","));
    }

    /**
     * One signature.
     *
     * @param secret the secret
     * @param timestamp the {@code Webhook-Timestamp} value
     * @param body the exact body sent
     * @return the lower-case hex HMAC
     */
    static String sign(String secret, long timestamp, String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of()
                            .formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }
}
