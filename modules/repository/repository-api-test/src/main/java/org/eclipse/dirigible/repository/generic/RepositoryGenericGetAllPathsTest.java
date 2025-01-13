/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.generic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericGetAllPathsTest.
 */
public class RepositoryGenericGetAllPathsTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test big text.
     */
    @Test
    public void testGetAllPaths() {
        if (repository == null) {
            return;
        }

        if (repository.hasCollection("/")) { //$NON-NLS-1$
            repository.removeCollection("/"); //$NON-NLS-1$
        }

        IResource resource = null;
        try {
            resource = repository.createResource("/a/b.txt", "some text".getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            resource = repository.createResource("/a/b/c.txt", "some text".getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            resource = repository.createResource("/b/c.txt", "some text".getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            resource = repository.createResource("/e/f/g.txt", "some text".getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            resource = repository.createResource("/h/i/j/k.txt", "some text".getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$

            List<String> paths = repository.getAllResourcePaths();
            assertTrue(paths.contains("/a/b.txt"));
            assertTrue(paths.contains("/a/b/c.txt"));
            assertTrue(paths.contains("/b/c.txt"));
            assertTrue(paths.contains("/e/f/g.txt"));
            assertTrue(paths.contains("/h/i/j/k.txt"));

            assertFalse(paths.contains("/a"));
            assertFalse(paths.contains("/a/"));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    if (repository.hasCollection("/")) { //$NON-NLS-1$
                        repository.removeCollection("/"); //$NON-NLS-1$
                    }
                    resource = repository.getResource("/testCollection/toBeRemoved1.txt"); //$NON-NLS-1$
                    assertNotNull(resource);
                    assertFalse(resource.exists());
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

}
