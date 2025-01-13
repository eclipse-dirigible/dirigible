/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * The Class GitConnectorTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class GitConnectorTest {

    /** The Constant DIRIGIBLE_TEST_GIT_ENABLED. */
    public static final String DIRIGIBLE_TEST_GIT_ENABLED = "DIRIGIBLE_TEST_GIT_ENABLED";

    /**
     * Clone repository.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InvalidRemoteException the invalid remote exception
     * @throws TransportException the transport exception
     * @throws GitAPIException the git API exception
     */
    @Test
    public void cloneRepository() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        String gitEnabled = System.getProperty(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
        if (gitEnabled != null) {
            Path path = Files.createDirectory(Paths.get("target/dirigible/git"));
            try {
                // clone repository
                IGitConnector gitConnector = GitConnectorFactory.cloneRepository(path.toString(),
                        "https://github.com/dirigiblelabs/sample_git_test.git", null, null, IGitConnector.GIT_MASTER);
                Status status = gitConnector.status();
                assertTrue(status.isClean());
                File textFile = new File(path.toString() + File.separator + "test.txt");
                assertNotNull(textFile);
                String textContent = FileUtils.readFileToString(textFile, "UTF-8");
                assertNotNull(textContent);
                assertEquals("Test Content", textContent.trim());
            } finally {
                if (path.toFile()
                        .exists()) {
                    FileUtils.deleteDirectory(path.toFile());
                }
            }
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
