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

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Init a Git repository and optionally publish it.
 */
@Component
public class InitCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(InitCommand.class);


    /**
     * Execute.
     *
     * @param gitDirectory the git directory
     * @param isBare the is bare
     * @throws GitConnectorException the git connector exception
     */
    public void execute(String gitDirectory, Boolean isBare) throws GitConnectorException {
        initRepository(gitDirectory, isBare);

    }

    /**
     * Inits the repository.
     *
     * @param gitDirectory the git directory
     * @param isBare the is bare
     * @throws GitConnectorException the git connector exception
     */
    void initRepository(String gitDirectory, Boolean isBare) throws GitConnectorException {
        try {
            GitConnectorFactory.initRepository(gitDirectory, isBare);
        } catch (GitAPIException e) {
            String errorMessage = "An error occurred while initializing repository.";
            errorMessage += " " + e.getMessage();
            if (logger.isErrorEnabled()) {
                logger.error(errorMessage);
            }
            throw new GitConnectorException(errorMessage, e);
        }
    }

}
