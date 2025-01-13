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

import java.io.File;
import java.io.IOException;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * The Class InitCommandTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class InitCommandTest {

    /** The init command. */
    @Autowired
    private InitCommand initCommand;

    /**
     * Inits the repository test.
     *
     * @throws GitConnectorException the git connector exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void initRepositoryTest() throws GitConnectorException, IOException {
        String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
        if (gitEnabled != null) {
            String user = UserFacade.getName();
            if (GitFileUtils.getGitDirectory(user, "workspace1")
                            .exists()) {
                GitFileUtils.deleteGitDirectory(user, "workspace1", "workspace-repo");
            }
            File gitRepo = GitFileUtils.createGitDirectory(user, "workspace1", "workspace-repo");
            initCommand.execute(gitRepo.getCanonicalPath(), false);
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
