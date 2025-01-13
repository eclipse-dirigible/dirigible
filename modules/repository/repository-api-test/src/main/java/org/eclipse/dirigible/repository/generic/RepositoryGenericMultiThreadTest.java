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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericMultiThreadTest.
 */
public abstract class RepositoryGenericMultiThreadTest {

    /**
     * Gets the new repository.
     *
     * @param user the user
     * @return the new repository
     */
    protected abstract IRepository getNewRepository(String user);

    /**
     * Go test.
     */
    @Test
    public void goTest() {
        multi();
    }

    // @Override
    // public void run() {
    // int POOL_SIZE = 3;
    // ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
    // try {
    // for (int i = 0; i < POOL_SIZE; i++) {
    // Runnable worker = new Runnable() {
    //
    // @Override
    // public void run() {
    // IRepository repository = getNewRepository(this.hashCode() + "");
    // for (int j = 0; j < 100; j++) {
    // text(repository);
    // System.out.println(this.hashCode() + " - " + j);
    // // try {
    // // this.wait(200);
    // // } catch (InterruptedException e) {
    // // fail(e.getMessage());
    // // }
    // }
    // }
    // };
    // executorService.execute(worker);
    // }
    // } catch (Exception e) {
    // executorService.shutdown();
    // fail(e.getMessage());
    // }
    // }

    /**
     * Text.
     *
     * @param repository the repository
     */
    private void text(IRepository repository) {
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

    /**
     * Multi.
     */
    public void multi() {
        int POOL_SIZE = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                Runnable worker = new Runnable() {

                    @Override
                    public void run() {
                        IRepository repository = getNewRepository(this.hashCode() + "");
                        for (int j = 0; j < 100; j++) {
                            text(repository);
                            // System.out.println(this.hashCode() + " - " + j);
                            // try {
                            // this.wait(200);
                            // } catch (InterruptedException e) {
                            // fail(e.getMessage());
                            // }
                        }
                    }
                };
                executorService.execute(worker);
            }
        } catch (Exception e) {
            executorService.shutdown();
            fail(e.getMessage());
        }
        executorService.shutdown();
    }

}
