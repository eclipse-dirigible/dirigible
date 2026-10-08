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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Only a shipped print template version is protected from the Documents perspective (#7755).
 */
class ShippedPrintTemplateGuardTest {

    private final ShippedPrintTemplateGuard guard = new ShippedPrintTemplateGuard();

    @Test
    void aShippedVersionIsProtected() {
        assertTrue(guard.refusal("/Templates/SalesInvoice/Print/en/standard@1.28.0.print")
                        .isPresent());
        assertTrue(guard.refusal("Templates/SalesInvoice/Print/bg/standard@a1b2c3d4.print")
                        .isPresent());
        // The backends collapse doubled separators and dot segments, so the guard matches past them.
        assertTrue(guard.refusal("/Templates//SalesInvoice/./Print/en/standard@1.28.0.print")
                        .isPresent());
        assertTrue(guard.refusal("\\Templates\\SalesInvoice\\Print\\en\\standard@1.28.0.print")
                        .isPresent());
    }

    @Test
    void tenantTemplatesAndEverythingElseAreNot() {
        assertEquals(Optional.empty(), guard.refusal("/Templates/SalesInvoice/Print/en/acme.print"));
        assertEquals(Optional.empty(), guard.refusal("/Templates/Print/logo.png"));
        assertEquals(Optional.empty(), guard.refusal("/Other/SalesInvoice/Print/en/standard@1.28.0.print"));
        assertEquals(Optional.empty(), guard.refusal("/Templates/SalesInvoice/Print/en"));
    }
}
