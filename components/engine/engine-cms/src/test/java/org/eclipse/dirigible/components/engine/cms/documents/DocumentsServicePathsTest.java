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
}
