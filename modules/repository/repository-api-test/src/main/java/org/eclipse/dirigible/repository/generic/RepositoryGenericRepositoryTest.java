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

import java.nio.charset.Charset;
import java.util.Arrays;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.After;
import org.junit.Test;

/**
 * The Class RepositoryGenericRepositoryTest.
 */
public class RepositoryGenericRepositoryTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test get root.
     */
    @Test
    public void testGetRoot() {
        if (repository == null) {
            return;
        }

        assertNotNull(repository.getRoot());
    }

    /**
     * Test create collection.
     */
    @Test
    public void testCreateCollection() {
        if (repository == null) {
            return;
        }

        try {
            ICollection collection = repository.createCollection("/testCollection"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test get collection.
     */
    @Test
    public void testGetCollection() {
        if (repository == null) {
            return;
        }

        try {
            repository.createCollection("/testCollection"); //$NON-NLS-1$
            ICollection collection = repository.getCollection("/testCollection"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test remove collection.
     */
    @Test
    public void testRemoveCollection() {
        if (repository == null) {
            return;
        }

        try {
            ICollection collection = repository.createCollection("/toBeRemoved"); //$NON-NLS-1$
            assertNotNull(collection);
            repository.removeCollection("/toBeRemoved"); //$NON-NLS-1$
            collection = repository.getCollection("/toBeRemoved"); //$NON-NLS-1$
            assertNotNull(collection);
            assertFalse(collection.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test has collection.
     */
    @Test
    public void testHasCollection() {
        if (repository == null) {
            return;
        }

        try {
            repository.createCollection("/testCollection"); //$NON-NLS-1$
            assertTrue(repository.hasCollection("/testCollection")); //$NON-NLS-1$
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test create resource string.
     */
    @Test
    public void testCreateResourceString() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/testCollection/testResourceEmpty.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertTrue(resource.getContent().length == 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test create resource string byte array.
     */
    @Test
    public void testCreateResourceStringByteArray() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/testCollection/testResourceContent.txt", //$NON-NLS-1$
                    "test content".getBytes()); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.getContent().length == 0);
            assertFalse(resource.isBinary());
            assertTrue(Arrays.equals(resource.getContent(), "test content".getBytes(Charset.defaultCharset()))); //$NON-NLS-1$
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test get resource.
     */
    @Test
    public void testGetResource() {
        if (repository == null) {
            return;
        }

        try {
            repository.createResource("/testCollection/testResourceEmpty.txt"); //$NON-NLS-1$
            IResource resource = repository.getResource("/testCollection/testResourceEmpty.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test remove resource.
     */
    @Test
    public void testRemoveResource() {
        if (repository == null) {
            return;
        }

        try {
            IResource resource = repository.createResource("/testCollection/toBeRemoved.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            repository.removeResource("/testCollection/toBeRemoved.txt"); //$NON-NLS-1$
            resource = repository.getResource("/testCollection/toBeRemoved.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertFalse(resource.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test has resource.
     */
    @Test
    public void testHasResource() {
        if (repository == null) {
            return;
        }

        try {
            if (repository.hasResource("/testCollection/checkExists.txt")) { //$NON-NLS-1$
                repository.removeResource("/testCollection/checkExists.txt"); //$NON-NLS-1$
            }
            IResource resource = repository.createResource("/testCollection/checkExists.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Clean up.
     */
    @After
    public void cleanUp() {
        if (repository == null) {
            return;
        }

        try {
            if (repository.hasCollection("/testCollection")) { //$NON-NLS-1$
                repository.removeCollection("/testCollection"); //$NON-NLS-1$
            }
            ICollection collection = repository.getCollection("/testCollection"); //$NON-NLS-1$
            assertNotNull(collection);
            assertFalse(collection.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test dispose.
     */
    @Test
    public void testDispose() {
        // fail("Not yet implemented");
    }

}
