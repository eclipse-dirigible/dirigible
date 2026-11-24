/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The print dialog's template languages are named in themselves, whatever the UI or JVM language
 * (#8877).
 */
class PrintEndpointLanguagesTest {

    private final Locale defaultLocale = Locale.getDefault();

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void eachLanguageIsNamedInItselfAndAnUnknownCodeByItsCode() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        assertEquals(List.of("Български", "English", "Deutsch", "xx"), names());

        Locale.setDefault(Locale.forLanguageTag("bg"));
        assertEquals(List.of("Български", "English", "Deutsch", "xx"), names());
    }

    private static List<String> names() throws Exception {
        PrintTemplateCatalog catalog = mock(PrintTemplateCatalog.class);
        when(catalog.languages("GoodsReceipt")).thenReturn(List.of("bg", "en", "de", "xx"));
        PrintEndpoint endpoint = new PrintEndpoint(catalog, mock(PrintImageResolver.class));

        return endpoint.getLanguages("GoodsReceipt")
                       .getBody()
                       .stream()
                       .map(language -> language.get("name"))
                       .toList();
    }
}
