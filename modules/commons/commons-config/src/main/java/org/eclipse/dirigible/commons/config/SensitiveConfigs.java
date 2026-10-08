/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import java.util.regex.Pattern;

/**
 * Decides which configuration values are secrets and masks them before they leave the server. It is
 * the one classification every configuration response uses: the legacy rows, the descriptors and
 * the tenant endpoints.
 */
public final class SensitiveConfigs {

    /** What a set sensitive value is replaced with. */
    public static final String MASK = "********";

    /**
     * Key names that carry a credential: passwords, secrets, API keys, tokens and signing keys, the
     * base64 basic-auth user, and client/broker URIs, which embed {@code user:password@host}.
     * {@code TOKEN} matches only as the last segment, so {@code *_MAX_TOKENS} or {@code *_TOKEN_URL}
     * stay readable.
     */
    private static final Pattern SENSITIVE_KEY =
            Pattern.compile("PASSWORD|PASSWD|SECRET|API_KEY|(^|_)TOKEN$|_KEY$|CLIENT_URI$|BROKER_URL$|^DIRIGIBLE_BASIC_USERNAME$",
                    Pattern.CASE_INSENSITIVE);

    private SensitiveConfigs() {}

    /**
     * Whether the value of a configuration key is a secret.
     *
     * @param key the configuration key
     * @return true when the value must never be shown in clear
     */
    public static boolean isSensitive(String key) {
        return key != null && SENSITIVE_KEY.matcher(key)
                                           .find();
    }

    /**
     * The value as it may be shown: {@link #MASK} for a set sensitive value, otherwise unchanged. An
     * unset value stays {@code null}, so "set" and "unset" remain distinguishable.
     *
     * @param key the configuration key
     * @param value the raw value
     * @return the displayable value
     */
    public static String mask(String key, String value) {
        return value != null && isSensitive(key) ? MASK : value;
    }

    /**
     * Whether a submitted value is the mask echoed back for a sensitive key, i.e. an unchanged edit
     * that must not overwrite the stored secret with the mask.
     *
     * @param key the configuration key
     * @param value the submitted value
     * @return true if the write should keep the stored value
     */
    public static boolean isMaskEcho(String key, String value) {
        return MASK.equals(value) && isSensitive(key);
    }

}
