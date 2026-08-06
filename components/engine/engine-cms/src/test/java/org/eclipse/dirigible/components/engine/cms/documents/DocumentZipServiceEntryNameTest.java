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

import org.junit.jupiter.api.Test;

/**
 * An uploaded archive is untrusted input, so an entry must never be able to name a path outside the
 * folder it is unpacked into.
 */
class DocumentZipServiceEntryNameTest {

    @Test
    void anOrdinaryEntryKeepsItsRelativePath() {
        assertEquals("reports/q1.pdf", DocumentZipService.safeEntryName("reports/q1.pdf"));
    }

    @Test
    void traversalSegmentsAreDropped() {
        assertEquals("passwd", DocumentZipService.safeEntryName("../../passwd"));
        assertEquals("reports/q1.pdf", DocumentZipService.safeEntryName("reports/../reports/q1.pdf"));
        assertEquals("etc/passwd", DocumentZipService.safeEntryName("../etc/../etc/passwd"));
    }

    @Test
    void anAbsoluteEntryBecomesRelative() {
        assertEquals("etc/passwd", DocumentZipService.safeEntryName("/etc/passwd"));
    }

    @Test
    void windowsSeparatorsAreNormalized() {
        assertEquals("reports/q1.pdf", DocumentZipService.safeEntryName("reports\\q1.pdf"));
        assertEquals("passwd", DocumentZipService.safeEntryName("..\\..\\passwd"));
    }

    @Test
    void currentDirectorySegmentsAreDropped() {
        assertEquals("reports/q1.pdf", DocumentZipService.safeEntryName("./reports/./q1.pdf"));
    }

    @Test
    void aTrailingSeparatorIsRemovedSoDirectoryEntriesResolve() {
        assertEquals("reports/2026", DocumentZipService.safeEntryName("reports/2026/"));
    }

    @Test
    void anEntryThatNamesNothingUsableIsSkipped() {
        assertEquals("", DocumentZipService.safeEntryName("../.."));
        assertEquals("", DocumentZipService.safeEntryName("/"));
        assertEquals("", DocumentZipService.safeEntryName(null));
    }
}
