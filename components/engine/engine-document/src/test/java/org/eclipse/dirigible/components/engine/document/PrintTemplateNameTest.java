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

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The print template file-name model (#7755): {@code <name>@<version>.print} is a shipped version,
 * {@code <name>.print} a tenant template.
 */
class PrintTemplateNameTest {

    @Test
    void theFileNameTellsAShippedVersionFromATenantTemplate() {
        PrintTemplateName shipped = PrintTemplateName.fromFileName("standard@1.28.0.print")
                                                     .orElseThrow();
        PrintTemplateName tenant = PrintTemplateName.fromFileName("acme-blue.print")
                                                    .orElseThrow();

        assertTrue(shipped.isShipped());
        assertEquals("standard", shipped.name());
        assertEquals("1.28.0", shipped.version());
        assertEquals("standard@1.28.0", shipped.reference());
        assertFalse(tenant.isShipped());
        assertEquals("acme-blue.print", tenant.fileName());
    }

    @Test
    void anythingElseIsNotAPrintTemplate() {
        assertEquals(Optional.empty(), PrintTemplateName.fromFileName("logo.png"));
        assertEquals(Optional.empty(), PrintTemplateName.fromFileName(".print"));
        assertEquals(Optional.empty(), PrintTemplateName.parse("../escape"));
        assertEquals(Optional.empty(), PrintTemplateName.parse("a/b"));
        assertEquals(Optional.empty(), PrintTemplateName.parse("standard@"));
    }

    @Test
    void releaseVersionsCompareByTheirNumbers() {
        assertTrue(shipped("1.10.0").compareReleaseVersion(shipped("1.9.0")) > 0);
        assertTrue(shipped("2.0.0").compareReleaseVersion(shipped("2.0.0-rc.1")) > 0);
        assertEquals(0, shipped("1.2").compareReleaseVersion(shipped("1.2.0")));
        assertTrue(shipped("1.99999999999999999999.0").compareReleaseVersion(shipped("1.2.0")) > 0);
    }

    @Test
    void preReleasesCompareIdentifierByIdentifierAndNumericallyWhereBothAreNumbers() {
        assertTrue(shipped("1.30.0-rc.10").compareReleaseVersion(shipped("1.30.0-rc.2")) > 0);
        assertTrue(shipped("1.30.0-rc").compareReleaseVersion(shipped("1.30.0-rc.1")) < 0);
        assertTrue(shipped("1.30.0-1").compareReleaseVersion(shipped("1.30.0-alpha")) < 0);
        assertTrue(shipped("1.30.0-beta").compareReleaseVersion(shipped("1.30.0-alpha.9")) > 0);
    }

    @Test
    void aRevisionRanksAboveTheVersionItRevisesAndBelowTheNextRelease() {
        assertTrue(shipped("1.28.0_v1").compareReleaseVersion(shipped("1.28.0")) > 0);
        assertTrue(shipped("1.28.0_v10").compareReleaseVersion(shipped("1.28.0_v2")) > 0);
        assertTrue(shipped("1.28.0_v3").compareReleaseVersion(shipped("1.30.0")) < 0);
        assertEquals("1.28.0_v1", PrintTemplateName.revise("1.28.0", 1));
        assertEquals(0, PrintTemplateName.revisionOf("1.28.0", "1.28.0")
                                         .getAsInt());
        assertEquals(2, PrintTemplateName.revisionOf("1.28.0_v2", "1.28.0")
                                         .getAsInt());
        assertTrue(PrintTemplateName.revisionOf("1.28.0.1", "1.28.0")
                                    .isEmpty());
    }

    @Test
    void anyDocumentNameSanitisesToATenantTemplateName() {
        assertEquals("Invoice-template", PrintTemplateName.sanitize("Invoice template"));
        assertEquals("standard-1-", PrintTemplateName.sanitize("standard (1)"));
        assertEquals("standard-1.0", PrintTemplateName.sanitize("standard@1.0"), "never a shipped version");
        assertEquals("Facture", PrintTemplateName.sanitize("Factur\u00e9"), "an accent keeps its letter");
        assertEquals("template-" + PrintTemplateName.shortHash("\u0444\u0430\u043a\u0442\u0443\u0440\u0430"),
                PrintTemplateName.sanitize("\u0444\u0430\u043a\u0442\u0443\u0440\u0430"));
        assertTrue(PrintTemplateName.isValidName(PrintTemplateName.sanitize("x".repeat(300))));
    }

    @Test
    void theOverwriteLeftoverOnS3IsAPrintTemplateButOtherDocumentsAreNot() {
        assertEquals(Optional.of("standard"), PrintTemplateName.templateBase("standard.print-1696000000000"));
        assertEquals(Optional.of("Invoice template"), PrintTemplateName.templateBase("Invoice template.PRINT"));
        assertEquals(Optional.empty(), PrintTemplateName.templateBase("logo.png"));
        assertEquals(Optional.empty(), PrintTemplateName.templateBase("standard.print-draft"));
    }

    @Test
    void aContentHashIsNotAReleaseVersionEvenWhenItIsAllDigits() {
        assertFalse(shipped("12345678").hasReleaseVersion());
        assertFalse(shipped("a1b2c3d4").hasReleaseVersion());
        assertTrue(shipped("1.28.0").hasReleaseVersion());
    }

    private static PrintTemplateName shipped(String version) {
        return PrintTemplateName.shipped("standard", version);
    }
}
