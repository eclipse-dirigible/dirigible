/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Path handling of the Documents service: child paths must not double the root separator, and the
 * hidden-folder test must key on the first SEGMENT rather than a prefix, so a folder merely named
 * like the internal one stays visible.
 */
class DocumentsServicePathsTest {

    @Test
    void childOfTheRootDoesNotDoubleTheSeparator() {
        assertEquals("/notes.txt", DocumentsService.childPath("/", "notes.txt"));
    }

    @Test
    void childOfAFolderIsSeparated() {
        assertEquals("/reports/q1.pdf", DocumentsService.childPath("/reports", "q1.pdf"));
    }

    @Test
    void theInternalFolderAndItsContentAreHidden() {
        assertTrue(DocumentsService.isHidden("/__internal"));
        assertTrue(DocumentsService.isHidden("__internal"));
        assertTrue(DocumentsService.isHidden("/__internal/roles-access.json"));
    }

    @Test
    void aFolderMerelyStartingWithTheInternalNameIsVisible() {
        assertFalse(DocumentsService.isHidden("/__internalReports"));
    }

    @Test
    void ordinaryPathsAreVisible() {
        assertFalse(DocumentsService.isHidden("/"));
        assertFalse(DocumentsService.isHidden("/reports/q1.pdf"));
    }

    @Test
    void anOrdinaryPathPassesTheCharacterCheck() {
        assertEquals("/reports/q1.pdf", DocumentsService.requireCleanPath("/reports/q1.pdf"));
        assertEquals(null, DocumentsService.requireCleanPath(null));
    }

    @Test
    void aPathCarryingControlCharactersIsRejected() {
        // Such a path reaches logging and the CMIS query layer, where a newline could forge a log
        // record; no document or folder name can contain one, so rejecting is safe.
        assertThrows(DocumentInvalidPathException.class, () -> DocumentsService.requireCleanPath("/reports\nINFO forged entry"));
        assertThrows(DocumentInvalidPathException.class, () -> DocumentsService.requireCleanPath("/reports\r\nq1.pdf"));
        assertThrows(DocumentInvalidPathException.class, () -> DocumentsService.requireCleanPath("/reports" + (char) 0 + "q1.pdf"));
    }
}
