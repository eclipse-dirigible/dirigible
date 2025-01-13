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

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.generic.RepositoryGenericMultiThreadTest;

/**
 * The Class LocalRepositoryMultiThreadTest.
 */
public class LocalRepositoryMultiThreadTest extends RepositoryGenericMultiThreadTest {

    /**
     * Gets the new repository.
     *
     * @param user the user
     * @return the new repository
     */
    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.dirigible.repository.generic.RepositoryGenericMultiThreadTest#getNewRepository(java.
     * lang.String)
     */
    @Override
    protected IRepository getNewRepository(String user) {
        // TODO uncomment only for manual tests
        // return new LocalRepository(user);
        return null;
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        LocalRepositoryMultiThreadTest localMultiThreadTest = new LocalRepositoryMultiThreadTest();
        localMultiThreadTest.multi();
    }

}
