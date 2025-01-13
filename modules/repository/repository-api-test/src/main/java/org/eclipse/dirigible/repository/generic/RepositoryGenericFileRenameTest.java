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

import java.util.Random;

import jakarta.xml.bind.DatatypeConverter;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericFileRenameTest.
 */
public class RepositoryGenericFileRenameTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test rename by collection.
     */
    @Test
    public void testRenameByCollection() {
        if (repository == null) {
            return;
        }

        ICollection collection = null;
        IResource resource = null;
        try {
            collection = repository.createCollection("/a/b/c"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            byte[] bytes = new byte[400000];
            for (int i = 0; i < bytes.length; i++) {
                int ch = 'A' + new Random().nextInt(20);
                bytes[i] = (byte) ch;
            }

            String base64 = DatatypeConverter.printBase64Binary(bytes);

            resource = repository.createResource("/a/b/c/toBeRemoved1.txt", bytes, false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            collection.renameTo("x");

            collection = repository.getCollection("/a/b/x"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            resource = repository.getResource("/a/b/x/toBeRemoved1.txt"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            String base64back = DatatypeConverter.printBase64Binary(resource.getContent());

            assertEquals(base64, base64back);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                repository.removeCollection("/a");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Test rename by file.
     */
    @Test
    public void testRenameByFile() {
        if (repository == null) {
            return;
        }

        ICollection collection = null;
        IResource resource = null;
        try {
            collection = repository.createCollection("/a/b/c"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());

            byte[] bytes = new byte[400000];
            for (int i = 0; i < bytes.length; i++) {
                int ch = 'A' + new Random().nextInt(20);
                bytes[i] = (byte) ch;
            }

            String base64 = DatatypeConverter.printBase64Binary(bytes);

            resource = repository.createResource("/a/b/c/toBeRemoved1.txt", bytes, false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            resource.renameTo("toBeRemoved2.txt");

            resource = repository.getResource("/a/b/c/toBeRemoved2.txt"); //$NON-NLS-1$
            assertNotNull(collection);
            assertTrue(collection.exists());
            assertEquals("toBeRemoved2.txt", resource.getName());

            String base64back = DatatypeConverter.printBase64Binary(resource.getContent());

            assertEquals(base64, base64back);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                repository.removeCollection("/a");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }
}
