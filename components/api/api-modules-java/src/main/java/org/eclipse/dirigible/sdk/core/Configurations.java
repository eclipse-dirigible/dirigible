/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.core;

import org.eclipse.dirigible.commons.config.Configuration;

/**
 * Reads platform configuration values through {@link Configuration} — the same source the rest of
 * Dirigible consults for {@code DIRIGIBLE_*} env vars, {@code application*.properties} entries, and
 * runtime overrides. Use this rather than {@link System#getenv(String)} so the precedence rules
 * (env &gt; property file &gt; default) apply consistently with the rest of the platform.
 * <p>
 * For lists of supported keys see {@code DirigibleConfig} in
 * {@code modules/commons/commons-config}; that enum is the canonical inventory.
 */
public final class Configurations {

    private Configurations() {}

    public static String get(String key) {
        return Configuration.get(key);
    }

    public static String get(String key, String defaultValue) {
        return Configuration.get(key, defaultValue);
    }

    public static boolean has(String key) {
        return Configuration.get(key) != null;
    }

    public static void set(String key, String value) {
        Configuration.set(key, value);
    }

    public static void remove(String key) {
        Configuration.remove(key);
    }
}
