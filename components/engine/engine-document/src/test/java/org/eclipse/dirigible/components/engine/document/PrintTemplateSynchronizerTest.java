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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Which files the print template synchronizer seeds (#7755) - and therefore which the generic CMS
 * seed leaves alone - and the content-hash version.
 */
class PrintTemplateSynchronizerTest {

    @Test
    void aTemplateUnderTheTemplatesConventionIsShipped() {
        PrintTemplateSynchronizer.ShippedLocation location =
                PrintTemplateSynchronizer.ShippedLocation.of("/sales/doc/Templates/SalesInvoice/Print/en/standard.print")
                                                         .orElseThrow();

        assertEquals("sales", location.project());
        assertEquals("SalesInvoice", location.entity());
        assertEquals("en", location.language());
        assertEquals("standard", location.name());
        assertTrue(PrintTemplateSynchronizer.isShippedTemplate("C:\\r\\sales\\doc\\Templates\\X\\Print\\bg\\standard.print"));
    }

    @Test
    void otherDocFilesStayPlainCmsSeeds() {
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/Print/logo.png"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/SalesInvoice/Print/en/notes.txt"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/SalesInvoice/Print/standard.print"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Other/SalesInvoice/Print/en/standard.print"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/SalesInvoice.print"));
    }

    @Test
    void theContentVersionIsTheFirstEightHexDigitsOfTheSha256() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals("ba7816bf", PrintTemplateSynchronizer.contentVersion("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theSelectionKeyIsTheEntityAndLanguageUpperCased() {
        assertEquals("DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_EN", PrintTemplateSelection.key("SalesInvoice", "en"));
        assertEquals("DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_PT_BR", PrintTemplateSelection.key("SalesInvoice", "pt-BR"));
    }
}
