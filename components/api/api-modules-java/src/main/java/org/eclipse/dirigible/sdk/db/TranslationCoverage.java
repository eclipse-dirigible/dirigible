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
 * How completely a multilingual entity's data is translated, as its language table holds it (issue
 * #8789): what the Settings page of the application shells reports instead of judging by the
 * languages an application declares.
 *
 * @param total the number of base rows
 * @param untranslated per language of the application language set other than the default one, the
 *        number of base rows with no row of that language in the language table
 */
public record TranslationCoverage(long total, Map<String, Long> untranslated) {

    /**
     * Keeps the languages in their configured order, immutably.
     */
    public TranslationCoverage {
        untranslated = Collections.unmodifiableMap(new LinkedHashMap<>(untranslated));
    }
}
