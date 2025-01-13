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

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.Test;

/**
 * The Class RepositoryGenericCollectionRenameTest.
 */
public class RepositoryGenericCollectionRenameTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test create.
     */
    @Test
    public void testCreate() {
        if (repository == null) {
            return;
        }

        ICollection collection = null;
        try {
            collection = repository.createCollection("/testCollectionToBeRemoved1"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());
            assertEquals(0, collection.getChildren()
                                      .size());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((collection != null) && collection.exists()) {
                    repository.removeCollection("/testCollectionToBeRemoved1"); //$NON-NLS-1$
                    collection = repository.getCollection("/testCollectionToBeRemoved1"); //$NON-NLS-1$
                    assertNotNull(collection);
                    assertFalse(collection.exists());
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Test rename.
     */
    @Test
    public void testRename() {
        if (repository == null) {
            return;
        }

        ICollection collection = null;
        try {
            collection = repository.createCollection("/a/b/c"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            collection = repository.createCollection("/a/b/c/d"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            collection = repository.createCollection("/c/b/a"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            collection = repository.getCollection("/a/b/c"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            collection.renameTo("x");

            collection = repository.getCollection("/a/b/x"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());
            assertEquals("x", collection.getName());

            collection = repository.getCollection("/a/b/x/d"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            collection = repository.getCollection("/a/b/c"); //$NON-NLS-1$
            assertNotNull(collection);
            assertFalse(collection.exists());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                repository.removeCollection("/a");
                repository.removeCollection("/c");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

}
