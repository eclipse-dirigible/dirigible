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

import org.junit.jupiter.api.Test;

/** The print dialog's template languages are named in the caller's UI language (#8877). */
class PrintEndpointLanguagesTest {

    @Test
    void aBulgarianUiNamesTheLanguagesInBulgarian() throws Exception {
        assertEquals(List.of("Български", "Английски", "xx"), names(Locale.forLanguageTag("bg-BG")));
    }

    @Test
    void anEnglishUiNamesTheLanguagesInEnglish() throws Exception {
        assertEquals(List.of("Bulgarian", "English", "xx"), names(Locale.forLanguageTag("en-US")));
    }

    private static List<String> names(Locale uiLocale) throws Exception {
        PrintTemplateCatalog catalog = mock(PrintTemplateCatalog.class);
        when(catalog.languages("GoodsReceipt")).thenReturn(List.of("bg", "en", "xx"));
        PrintEndpoint endpoint = new PrintEndpoint(catalog, mock(PrintImageResolver.class));

        return endpoint.getLanguages("GoodsReceipt", uiLocale)
                       .getBody()
                       .stream()
                       .map(language -> language.get("name"))
                       .toList();
    }
}
