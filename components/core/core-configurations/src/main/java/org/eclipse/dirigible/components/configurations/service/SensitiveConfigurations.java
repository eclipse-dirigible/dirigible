/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.service;

import org.eclipse.dirigible.commons.config.SensitiveConfigs;

/**
 * Classifies configuration keys as sensitive and masks their values before they leave the server.
 * Every configuration endpoint that returns values passes them through
 * {@link #mask(String, String)}, so a secret never travels in a response body, whichever source
 * (runtime, environment, deployment, module, tenant) it was set in. The classification itself is
 * {@link SensitiveConfigs}, shared with the configuration descriptors.
 */
public final class SensitiveConfigurations {

    /** The value returned in place of a set sensitive value. */
    public static final String MASK = SensitiveConfigs.MASK;

    private SensitiveConfigurations() {}

    /**
     * Whether the key holds a sensitive value.
     *
     * @param key the configuration key
     * @return true if the value must never be returned in clear
     */
    public static boolean isSensitive(String key) {
        return SensitiveConfigs.isSensitive(key);
    }

    /**
     * Masks the value of a sensitive key. An unset value stays {@code null}, so "set vs unset" is still
     * visible; a non-sensitive value is returned unchanged.
     *
     * @param key the configuration key
     * @param value the value
     * @return {@link #MASK}, the value itself, or {@code null}
     */
    public static String mask(String key, String value) {
        return SensitiveConfigs.mask(key, value);
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
        return SensitiveConfigs.isMaskEcho(key, value);
    }

}
