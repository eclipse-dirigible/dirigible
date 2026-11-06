/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class FileUtilTest {

    /**
     * #8470: the skipped folder is the live Lucene index, and whatever happens inside it (a segment
     * file deleted by a merge while the walk lists it) must not abort the deletion of the rest. An
     * unreadable folder inside the kept subtree forces that failure deterministically: the walk fails
     * only if it opens the kept subtree at all.
     */
    @Test
    void deletesEverythingButTheSkippedFolderWithoutWalkingIntoIt(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve("dirigible");
        Path index = Files.createDirectories(base.resolve("repository/index"));
        File locked = Files.createDirectories(index.resolve("locked"))
                           .toFile();
        Path workspaceFile = Files.createDirectories(base.resolve("repository/root/users/admin/workspace/p"))
                                  .resolve("a.form.gen");
        Files.writeString(workspaceFile, "{}");

        assumeFalse(!locked.setReadable(false) || locked.canRead(), "needs a file system that enforces read permission");
        try {
            FileUtil.deleteFolder(base.toString(), index.toString());
        } finally {
            locked.setReadable(true);
        }

        assertFalse(Files.exists(base.resolve("repository/root")), "the workspace must be deleted");
        assertTrue(Files.isDirectory(locked.toPath()), "the skipped folder must be kept");
    }
}
