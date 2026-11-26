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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Test;

/**
 * The application language set is parsed in one place: trimmed, blank entries dropped, {@code en}
 * when nothing is configured.
 */
public class ApplicationLanguagesTest {

    private static final String KEY = DirigibleConfig.APPLICATION_LANGUAGES.getKey();

    @After
    public void reset() {
        Configuration.remove(KEY);
    }

    @Test
    public void defaultsToEnglish() {
        assertEquals(List.of("en"), ApplicationLanguages.get());
        assertEquals("en", ApplicationLanguages.defaultLanguage());
    }

    @Test
    public void readsTheConfiguredCodesTrimmedWithoutBlanks() {
        Configuration.set(KEY, " bg , ,en ");

        assertEquals(List.of("bg", "en"), ApplicationLanguages.get());
        assertEquals("bg", ApplicationLanguages.defaultLanguage());
    }

    @Test
    public void fallsBackToEnglishForABlankValue() {
        Configuration.set(KEY, " , ");

        assertEquals(List.of("en"), ApplicationLanguages.get());
    }
}
