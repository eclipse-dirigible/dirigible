/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileWriter;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryCache;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.generic.RepositoryGenericCacheTest;
import org.junit.Before;
import org.junit.Test;

/**
 * The Class LocalRepositoryCacheTest.
 */
public class LocalRepositoryCacheTest extends RepositoryGenericCacheTest {

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        try {
            repository = new LocalRepository("target/test");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test cache text.
     */
    @Test
    public void testCacheDisable() {
        if (repository == null) {
            return;
        }

        IResource resource1 = null;
        IResource resource2 = null;
        RepositoryCache.enable();
        try {
            resource1 = repository.createResource("/testCollection/toBeRemoved1Cached.txt", "cached file".getBytes()); //$NON-NLS-1$
            assertNotNull(resource1);
            assertTrue(resource1.exists());
            assertFalse(resource1.isBinary());

            String workspacePath =
                    LocalWorkspaceMapper.getMappedName(((FileSystemRepository) repository), "/testCollection/toBeRemoved1Cached2.txt");

            FileSystemUtils.createFile(workspacePath);

            resource2 = repository.getResource("/testCollection/toBeRemoved1Cached2.txt"); //$NON-NLS-1$
            assertNotNull(resource2);
            assertTrue(resource2.exists());

            String workspacePath1 =
                    LocalWorkspaceMapper.getMappedName(((FileSystemRepository) repository), "/testCollection/toBeRemoved1Cached.txt");
            FileWriter output = new FileWriter(workspacePath1);
            IOUtils.write("cached file changed", output);
            output.flush();
            output.close();

            String content = new String(resource1.getContent());
            assertEquals("cached file", content);

            try {
                RepositoryCache.disable();
                resource2 = repository.getResource("/testCollection/toBeRemoved1Cached2.txt"); //$NON-NLS-1$
                assertNotNull(resource2);
                assertTrue(resource2.exists());
                resource1 = repository.getResource("/testCollection/toBeRemoved1Cached.txt"); //$NON-NLS-1$
                content = new String(resource1.getContent());
                assertEquals("cached file changed", content);

                output = new FileWriter(workspacePath1);
                IOUtils.write("cached file changed 2", output);
                output.flush();
                output.close();

                resource1 = repository.getResource("/testCollection/toBeRemoved1Cached.txt"); //$NON-NLS-1$
                content = new String(resource1.getContent());
                assertEquals("cached file changed 2", content);

            } finally {
                RepositoryCache.enable();
            }


        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource1 != null) && resource1.exists()) {
                    repository.removeResource("/testCollection/toBeRemoved1Cached.txt"); //$NON-NLS-1$
                    resource1 = repository.getResource("/testCollection/toBeRemoved1Cached.txt"); //$NON-NLS-1$
                    assertNotNull(resource1);
                    assertFalse(resource1.exists());
                    repository.removeResource("/testCollection/toBeRemoved1Cached2.txt"); //$NON-NLS-1$
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

}
