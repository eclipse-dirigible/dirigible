/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Verifies the deterministic {@code /Attachments/<Master>/<yyyy>/<MM>/<uuid>/<file>} layout and the
 * path-safety of the master and file-name segments.
 */
class AttachmentPathTest {

    private static final LocalDate JULY = LocalDate.of(2026, 7, 21);

    @Test
    void folderIsEntityYearMonthUuid() {
        assertEquals("/Attachments/Company/2026/07/abc-123", AttachmentPath.folder("Company", JULY, "abc-123"));
    }

    @Test
    void buildAppendsTheFileName() {
        assertEquals("/Attachments/Company/2026/07/abc-123/contract.pdf", AttachmentPath.build("Company", JULY, "abc-123", "contract.pdf"));
    }

    @Test
    void monthIsZeroPadded() {
        assertEquals("/Attachments/Expense/2026/03/u/x.png", AttachmentPath.build("Expense", LocalDate.of(2026, 3, 9), "u", "x.png"));
    }

    @Test
    void fileNameStripsDirectoryAndTraversal() {
        assertEquals("passwd", AttachmentPath.fileName("../../etc/passwd"));
        assertEquals("file.docx", AttachmentPath.fileName("C:\\Users\\x\\file.docx"));
        assertEquals("report 2026.pdf", AttachmentPath.fileName("report 2026.pdf"));
    }

    @Test
    void blankFileNameFallsBackToFile() {
        assertEquals("file", AttachmentPath.fileName("   "));
        assertEquals("file", AttachmentPath.fileName(null));
    }

    @Test
    void masterSegmentIsSanitized() {
        // a separator or traversal in the master name can never escape the /Attachments tree
        assertEquals("/Attachments/Sales_Invoice____x/2026/07/u/f", AttachmentPath.build("Sales Invoice/../x", JULY, "u", "f"));
        assertEquals("_", AttachmentPath.segment(" "));
    }
}
