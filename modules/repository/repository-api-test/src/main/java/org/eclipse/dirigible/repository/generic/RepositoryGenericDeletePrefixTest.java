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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.Test;

/**
 * The Class RepositoryGenericRepositoryTest.
 */
public class RepositoryGenericDeletePrefixTest {

    /** The repository. */
    protected IRepository repository;



    /**
     * Test remove collection.
     */
    @Test
    public void testRemoveCollection() {
        if (repository == null) {
            return;
        }

        try {
            ICollection collectionAbcd = repository.createCollection("/abcd"); //$NON-NLS-1$
            ICollection collectionAbcdEfgh = repository.createCollection("/abcd/efgh"); //$NON-NLS-1$
            ICollection collectionAbcd2 = repository.createCollection("/abcd2"); //$NON-NLS-1$
            ICollection collectionAbcd2Efgh = repository.createCollection("/abcd2/efgh"); //$NON-NLS-1$
            assertNotNull(collectionAbcd);
            assertNotNull(collectionAbcd2);
            repository.removeCollection("/abcd"); //$NON-NLS-1$
            collectionAbcd = repository.getCollection("/abcd"); //$NON-NLS-1$
            assertNotNull(collectionAbcd);
            assertFalse(collectionAbcd.exists());
            collectionAbcd2 = repository.getCollection("/abcd2"); //$NON-NLS-1$
            assertNotNull(collectionAbcd2);
            assertTrue(collectionAbcd2.exists());
            collectionAbcdEfgh = repository.getCollection("/abcd/efgh"); //$NON-NLS-1$
            assertNotNull(collectionAbcdEfgh);
            assertFalse(collectionAbcdEfgh.exists());
            collectionAbcd2Efgh = repository.getCollection("/abcd2/efgh"); //$NON-NLS-1$
            assertNotNull(collectionAbcd2Efgh);
            assertTrue(collectionAbcd2Efgh.exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
