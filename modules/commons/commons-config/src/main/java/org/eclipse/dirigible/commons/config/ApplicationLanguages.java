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

import java.util.List;

/**
 * The application language set ({@code DIRIGIBLE_APPLICATION_LANGUAGES}): the languages the Region
 * &amp; Language picker offers, the first one the default. Read and parsed here only, so the
 * picker, the locales the translation catalogs load for and a server-initiated render agree on it
 * (#8774). The setting is tenant-overridable, so inside a tenant scope this is the tenant's set.
 */
public final class ApplicationLanguages {

    private static final String DEFAULT_LANGUAGE = "en";

    private ApplicationLanguages() {}

    /**
     * The configured language codes as written, trimmed and without blank entries.
     *
     * @return the codes, the default one first; {@code en} alone when none is configured
     */
    public static List<String> get() {
        List<String> languages = DirigibleConfig.APPLICATION_LANGUAGES.getListValue();
        return languages.isEmpty() ? List.of(DEFAULT_LANGUAGE) : languages;
    }

    /**
     * The default language: the first configured code.
     *
     * @return the first code, {@code en} when none is configured
     */
    public static String defaultLanguage() {
        return get().getFirst();
    }
}
