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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.dirigible.repository.api.IEntity;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericSearchTest.
 */
public class RepositoryGenericSearchTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test search name.
     */
    @Test
    public void testSearchName() {
        if (repository == null) {
            return;
        }

        try {
            if (repository.hasCollection("/testCollectionSearch")) {
                repository.removeCollection("/testCollectionSearch");
            }
            IResource resource = repository.createResource("/testCollectionSearch/param1.test", "param1".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/param2.test", "param2".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/param12.test", "param12".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            List<IEntity> entities = repository.searchName(".test", false); //$NON-NLS-1$
            try {
                assertNotNull(entities);
                assertEquals(3, entities.size());
            } finally {
                repository.removeResource("/testCollectionSearch/param1.test"); //$NON-NLS-1$
                repository.removeResource("/testCollectionSearch/param2.test"); //$NON-NLS-1$
                repository.removeResource("/testCollectionSearch/param12.test"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test search name under root.
     */
    @Test
    public void testSearchNameUnderRoot() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/dddd/file1.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/dddd/file2.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/dddd/file3.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            List<IEntity> entities = repository.searchName("/dddd/", ".txt", false); //$NON-NLS-1$
            try {
                assertNotNull(entities);
                assertEquals(3, entities.size());
            } finally {
                repository.removeResource("/dddd/file1.txt"); //$NON-NLS-1$
                repository.removeResource("/dddd/file2.txt"); //$NON-NLS-1$
                repository.removeResource("/dddd/file3.txt"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test search path.
     */
    @Test
    public void testSearchPath() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/testCollectionSearch/param1.txt", "param1".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/param2.txt", "param2".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/param12.txt", "param12".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            List<IEntity> entities = repository.searchPath("param1", false); //$NON-NLS-1$
            assertNotNull(entities);
            assertEquals(2, entities.size());

            entities = repository.searchPath("param", false); //$NON-NLS-1$
            assertEquals(3, entities.size());

            entities = repository.searchPath("Param", false); //$NON-NLS-1$
            assertEquals(0, entities.size());

            entities = repository.searchPath("Param", true); //$NON-NLS-1$
            assertEquals(3, entities.size());

            repository.removeResource("/testCollectionSearch/param1.txt"); //$NON-NLS-1$
            repository.removeResource("/testCollectionSearch/param2.txt"); //$NON-NLS-1$
            repository.removeResource("/testCollectionSearch/param12.txt"); //$NON-NLS-1$

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test search text.
     */
    @Test
    public void testSearchText() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/testCollectionSearch/abc1.txt", "abc def".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/abc2.txt", "ghi jkl".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/abc3.txt", "abc jkl".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());
            resource = repository.createResource("/testCollectionSearch/xxx4.txt", "xxx yyy".getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            repository.searchRefresh();

            List<IEntity> entities = repository.searchText("abc"); //$NON-NLS-1$
            assertEquals(2, entities.size());

            entities = repository.searchText("jkl"); //$NON-NLS-1$
            assertEquals(2, entities.size());

            entities = repository.searchText("Ghi"); //$NON-NLS-1$
            assertEquals(1, entities.size());

            entities = repository.searchText("abc "); //$NON-NLS-1$
            assertEquals(2, entities.size());

            repository.removeResource("/testCollectionSearch/abc1.txt"); //$NON-NLS-1$
            repository.removeResource("/testCollectionSearch/abc2.txt"); //$NON-NLS-1$
            repository.removeResource("/testCollectionSearch/abc3.txt"); //$NON-NLS-1$
            repository.removeResource("/testCollectionSearch/xxx4.txt"); //$NON-NLS-1$

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
