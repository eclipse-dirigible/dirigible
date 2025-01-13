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

/**
 * The Class LocalRepositoryCopyGitTest.
 */
public class LocalRepositoryCopyGitTest {

    // IRepository repositorySrc;
    // IRepository repositoryDst;
    //
    // @Before
    // public void setUp() {
    // try {
    // repositorySrc = new GitMasterRepository("testUser", "testSrcGit",
    // "https://github.com/delchev/master_repository_test", null, null,
    // "origin/master");
    // repositoryDst = new LocalRepository(null, "testDst", "testDst");
    // } catch (Exception e) {
    // e.printStackTrace();
    // fail(e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testGit() {
    // String PATH = "/db/dirigible/registry/conf/datasources/xxx.properties";
    // IResource resource = null;
    // try {
    //
    // IResource resourceBack = repositorySrc.getResource(PATH);
    // String path = resourceBack.getPath();
    //
    // assertEquals(PATH, path);
    //
    // copyRepository(repositorySrc, repositoryDst);
    //
    // resourceBack = repositoryDst.getResource(PATH);
    // path = resourceBack.getPath();
    //
    // assertTrue(resourceBack.exists());
    // assertEquals(PATH, path);
    //
    // } catch (IOException e) {
    // e.printStackTrace();
    // fail(e.getMessage());
    // } finally {
    // try {
    // if ((resource != null) && resource.exists()) {
    // repositorySrc.removeResource(PATH);
    // resource = repositorySrc.getResource(PATH);
    // assertNotNull(resource);
    // assertFalse(resource.exists());
    //
    // repositoryDst.removeResource(PATH);
    // resource = repositoryDst.getResource(PATH);
    // assertNotNull(resource);
    // assertFalse(resource.exists());
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // fail(e.getMessage());
    // }
    // }
    // }
    //
    // private void copyRepository(IRepository sourceRepository, IRepository targetRepository) throws
    // IOException {
    // ICollection root = sourceRepository.getRoot();
    // copyCollection(root, targetRepository);
    // }
    //
    // private void copyCollection(ICollection parent, IRepository targetRepository) throws IOException
    // {
    // List<IEntity> entities = parent.getChildren();
    // for (IEntity entity : entities) {
    // if (entity instanceof ICollection) {
    // ICollection collection = (ICollection) entity;
    // copyCollection(collection, targetRepository);
    // } else {
    // IResource resource = (IResource) entity;
    // try {
    // targetRepository.createResource(resource.getPath(), resource.getContent(), resource.isBinary(),
    // resource.getContentType(), true);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }
    // }

}
