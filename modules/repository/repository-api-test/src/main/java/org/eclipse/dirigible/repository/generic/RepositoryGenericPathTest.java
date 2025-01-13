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

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IEntity;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericPathTest.
 */
public class RepositoryGenericPathTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test path.
     */
    @Test
    public void testPath() {
        if (repository == null) {
            return;
        }

        String PATH = "/testCollectionPath/toBeRemovedTextPath1.txt";
        IResource resource = null;
        try {
            String content = "test1";

            resource = repository.createResource(PATH, content.getBytes(), false, "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            IResource resourceBack = repository.getResource(PATH);
            String path = resourceBack.getPath();

            assertEquals(PATH, path);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource(PATH);
                    resource = repository.getResource(PATH);
                    assertNotNull(resource);
                    assertFalse(resource.exists());
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Test path children.
     */
    @Test
    public void testPathChildren() {
        if (repository == null) {
            return;
        }

        String PATH_COLLECTION = "/testCollectionPath";
        String PATH = "/testCollectionPath/toBeRemovedTextPath2.txt";
        IResource resource = null;
        try {
            String content = "test1";

            resource = repository.createResource(PATH, content.getBytes(), false, "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            ICollection collection = repository.getCollection(PATH_COLLECTION);
            List<IEntity> entities = collection.getChildren();
            IResource resourceBack = (IResource) entities.get(0);
            String path = resourceBack.getPath();

            assertEquals(PATH, path);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource(PATH);
                    resource = repository.getResource(PATH);
                    assertNotNull(resource);
                    assertFalse(resource.exists());
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Test path root.
     */
    @Test
    public void testPathRoot() {
        if (repository == null) {
            return;
        }

        String PATH = "/testCollectionPath/toBeRemovedTextPath3.txt";
        IResource resource = null;
        try {
            String content = "test1";

            resource = repository.createResource(PATH, content.getBytes(), false, "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            ICollection root = repository.getRoot();
            List<IEntity> entities = root.getChildren();
            for (IEntity collection : entities) {
                if ("testCollectionPath".equals(collection.getName())) {
                    entities = ((ICollection) collection).getChildren();
                    IResource resourceBack = (IResource) entities.get(0);
                    String path = resourceBack.getPath();
                    assertEquals(PATH, path);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource(PATH);
                    resource = repository.getResource(PATH);
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
