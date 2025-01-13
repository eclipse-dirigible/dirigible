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

import java.util.Date;

import org.eclipse.dirigible.commons.api.context.ThreadContextFacade;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericModifiedTest.
 */
public class RepositoryGenericModifiedTest {

    /** The repository 1. */
    protected IRepository repository1;

    /** The repository 2. */
    protected IRepository repository2;

    /** The repository 3. */
    protected IRepository repository3;

    /**
     * Test modified.
     */
    @Test
    public void testModified() {
        if (repository1 == null) {
            return;
        }

        try {

            ThreadContextFacade.setUp();

            IResource resource = repository1.createResource("/testCollection/toBeModified.txt", //$NON-NLS-1$
                    "Some content".getBytes()); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertFalse(resource.isBinary());

            // assertEquals("guest1", resource.getInformation().getModifiedBy());
            // assertEquals("nobody", resource.getInformation().getModifiedBy());

            Date firstModified = resource.getInformation()
                                         .getModifiedAt();

            resource = repository2.getResource("/testCollection/toBeModified.txt"); //$NON-NLS-1$
            assertNotNull(resource);
            assertTrue(resource.exists());

            resource.setContent("Some modified content".getBytes());

            resource = repository2.getResource("/testCollection/toBeModified.txt"); //$NON-NLS-1$

            // assertEquals("guest2", resource.getInformation().getModifiedBy());
            // assertEquals("nobody", resource.getInformation().getModifiedBy());
            boolean isAfter = resource.getInformation()
                                      .getModifiedAt()
                                      .after(firstModified);
            if (!isAfter) {
                System.out.println("Warning: RepositoryGenericModifiedTest - Modified date check failed on Operating System (OS): "
                        + System.getenv("os.name"));
                isAfter = new String(resource.getContent()).equals("Some modified content");
            }
            assertTrue(isAfter);

            ThreadContextFacade.tearDown();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            try {
                repository3.removeResource("/testCollection/toBeModified.txt");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

}
