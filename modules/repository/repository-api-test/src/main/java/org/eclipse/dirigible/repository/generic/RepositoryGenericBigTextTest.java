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

import java.util.Arrays;
import java.util.Random;

import jakarta.xml.bind.DatatypeConverter;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericBigTextTest.
 */
public class RepositoryGenericBigTextTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test big text.
     */
    @Test
    public void testBigText() {
        if (repository == null) {
            return;
        }

        IResource resource = null;
        try {
            byte[] bytes = new byte[400000];
            for (int i = 0; i < bytes.length; i++) {
                int ch = 'A' + new Random().nextInt(20);
                bytes[i] = (byte) ch;
            }

            String base64 = DatatypeConverter.printBase64Binary(bytes);

            resource = repository.createResource("/testCollection/toBeRemoved1.txt", bytes, false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            IResource resourceBack = repository.getResource("/testCollection/toBeRemoved1.txt"); //$NON-NLS-1$
            String base64back = DatatypeConverter.printBase64Binary(resourceBack.getContent());

            assertEquals(base64, base64back);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource("/testCollection/toBeRemoved1.txt"); //$NON-NLS-1$
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

    /**
     * Test spaces text.
     */
    @Test
    public void testSpacesText() {
        if (repository == null) {
            return;
        }

        IResource resource = null;
        try {
            byte[] bytes = new byte[400000];
            for (int i = 0; i < bytes.length; i++) {
                int ch = ' ';
                bytes[i] = (byte) ch;
            }

            resource = repository.createResource("/testCollection/toBeRemoved2.txt", bytes, false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            IResource resourceBack = repository.getResource("/testCollection/toBeRemoved2.txt"); //$NON-NLS-1$

            byte[] bytesBack = resourceBack.getContent();
            assertEquals(bytes.length, bytesBack.length);
            assertTrue(Arrays.equals(bytes, bytesBack));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource("/testCollection/toBeRemoved2.txt"); //$NON-NLS-1$
                    resource = repository.getResource("/testCollection/toBeRemoved2.txt"); //$NON-NLS-1$
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
