/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import static org.junit.Assert.fail;

import org.eclipse.dirigible.repository.generic.RepositoryGenericCollectionCopyTest;
import org.junit.Before;

/**
 * The Class LocalRepositoryCollectionCopyTest.
 */
public class LocalRepositoryCollectionCopyTest extends RepositoryGenericCollectionCopyTest {

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        try {
            repository = new LocalRepository("target/test");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
