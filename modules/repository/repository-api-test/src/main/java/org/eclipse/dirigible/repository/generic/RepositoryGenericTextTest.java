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

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericTextTest.
 */
public class RepositoryGenericTextTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test text.
     */
    @Test
    public void testText() {
        if (repository == null) {
            return;
        }

        IResource resource = null;
        try {
            String content = "test1";

            resource = repository.createResource("/testCollection/toBeRemovedText1.txt", content.getBytes(), false, //$NON-NLS-1$
                    "text/plain"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            IResource resourceBack = repository.getResource("/testCollection/toBeRemovedText1.txt"); //$NON-NLS-1$
            String contentback = new String(resourceBack.getContent(), StandardCharsets.UTF_8);

            assertEquals(content, contentback);

            IResource resource2 = repository.getResource("/testCollection/toBeRemovedText1.txt"); //$NON-NLS-1$
            resource2.setContent("test2".getBytes());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                if ((resource != null) && resource.exists()) {
                    repository.removeResource("/testCollection/toBeRemovedText1.txt"); //$NON-NLS-1$
                    resource = repository.getResource("/testCollection/toBeRemovedText1.txt"); //$NON-NLS-1$
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
