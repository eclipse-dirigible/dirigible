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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Test;

/**
 * The Class RepositoryGenericImportZipTest.
 */
public class RepositoryGenericImportZipTest {

    /** The repository. */
    protected IRepository repository;

    /**
     * Test import zip.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testImportZip() throws IOException {
        if (repository == null) {
            return;
        }

        InputStream in = RepositoryGenericImportZipTest.class.getResourceAsStream("/testImport.zip");
        try {
            ZipInputStream zipInputStream = new ZipInputStream(in); // $NON-NLS-1$
            try {

                ICollection collection = repository.getCollection("/root1/import"); //$NON-NLS-1$
                if (collection.exists()) {
                    collection.delete();
                }

                repository.importZip(zipInputStream, "/root1/import"); //$NON-NLS-1$

                IResource resource = repository.getResource("/root1/import/folder1/text1.txt"); //$NON-NLS-1$
                String read = new String(resource.getContent(), StandardCharsets.UTF_8);
                assertEquals("text1", read); //$NON-NLS-1$

                resource = repository.getResource("/root1/import/folder1/folder2/image1.png"); //$NON-NLS-1$
                assertTrue(resource.isBinary());
                assertEquals("image/png", resource.getContentType()); //$NON-NLS-1$

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
