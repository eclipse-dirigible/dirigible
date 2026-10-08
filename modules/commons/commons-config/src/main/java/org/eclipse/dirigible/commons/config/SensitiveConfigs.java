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

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides which configuration values are secrets and masks them before they leave the server.
 * <p>
 * The classification is a name heuristic until keys carry an explicit {@code sensitive} flag
 * (#7759): passwords, secrets, tokens, API and other keys, and the URIs that embed credentials.
 */
public final class SensitiveConfigs {

    /** What a set sensitive value is replaced with. */
    public static final String MASK = "********";

    private static final Pattern SENSITIVE = Pattern.compile("PASSWORD|SECRET|TOKEN|API_KEY|_KEY$|CLIENT_URI|BROKER_URL");

    /** Secrets the name pattern cannot tell: the basic-auth user is stored base64-encoded. */
    private static final Set<String> SENSITIVE_KEYS = Set.of("DIRIGIBLE_BASIC_USERNAME");

    private SensitiveConfigs() {}

    /**
     * Whether the value of a configuration key is a secret.
     *
     * @param key the configuration key
     * @return true when the value must never be shown in clear
     */
    public static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String name = key.toUpperCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(name) || SENSITIVE.matcher(name)
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

}
