/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One soft finding about a write - a condition the model declared with {@code severity: warn} (a
 * second customer with the same name, a document line at price zero). Unlike a
 * {@link ValidationException} it does not refuse the write: it asks the person making it to confirm
 * first (issue #7466).
 *
 * @param code the stable identity of the warning - what a caller echoes back to confirm it
 * @param message the user-facing text, resolved for the request's language
 * @param messageKey the fully qualified translation key the message was resolved from (issue
 *        #7611), or {@code null} for a literal message
 * @param params the values interpolated into the message's placeholders, by placeholder name
 */
public record Warning(String code, String message, String messageKey, Map<String, Object> params) {

    /**
     * Normalizes the parameters to an immutable, never-null map.
     */
    public Warning {
        params = params == null || params.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /**
     * A warning carrying a literal message - no translation key.
     *
     * @param code the stable identity of the warning
     * @param message the user-facing text
     */
    public Warning(String code, String message) {
        this(code, message, null, null);
    }
}
