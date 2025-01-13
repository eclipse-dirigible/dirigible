/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.StringUtils;

import java.io.File;
import java.io.IOException;

import static org.eclipse.dirigible.components.ide.git.domain.IGitConnector.*;

/**
 * A factory for creating GitConnector objects.
 */
public class GitConnectorFactory {

    /**
     * Gets org.eclipse.jgit.lib.Repository object for existing Git Repository.
     *
     * @param repositoryDirectory the path to an existing Git Repository
     * @return a newly created {@link IGitConnector} object
     * @throws GitConnectorException Git Connector Exception
     */
    public static IGitConnector getConnector(String repositoryDirectory) throws GitConnectorException {
        try {
            RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
            File current = new File(repositoryDirectory);
            repositoryBuilder.findGitDir(current);
            Repository repository = repositoryBuilder.build();
            repository.getConfig()
                      .setString(GIT_BRANCH, GIT_MASTER, GIT_MERGE, GIT_REFS_HEADS_MASTER);
            return new GitConnector(repository);
        } catch (IOException ex) {
            String errorMessage = String.format("Failed to get connector for repository [%s]", repositoryDirectory);
            throw new GitConnectorException(errorMessage, ex);
        }
    }

    /**
     * Clones secured git remote repository to the file system.
     *
     * @param repositoryDirectory where the remote repository will be cloned
     * @param repositoryUri repository's URI example: https://qwerty.com/xyz/abc.git
     * @param username the username used for authentication
     * @param password the password used for authentication
     * @param branch the branch where sources will be cloned from
     * @return a newly created {@link IGitConnector} object
     * @throws InvalidRemoteException Invalid Remote Exception
     * @throws TransportException Transport Exception
     * @throws GitAPIException Git API Exception
     */
    public static IGitConnector cloneRepository(String repositoryDirectory, String repositoryUri, String username, String password,
            String branch) throws InvalidRemoteException, TransportException, GitAPIException {
        try {
            branch = branchOrNull(branch);

            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(repositoryUri);
            if (!StringUtils.isEmptyOrNull(username) && !StringUtils.isEmptyOrNull(password)) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            }
            cloneCommand.setBranch(branch);
            cloneCommand.setDirectory(new File(repositoryDirectory));
            cloneCommand.call();

            return getConnector(repositoryDirectory);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to clone repository [%s] to dir [%s] using user [%s]", repositoryUri,
                    repositoryDirectory, username);
            throw new TransportException(errorMessage, e);
        }
    }

    /**
     * Branch or null.
     *
     * @param branch the branch
     * @return the string
     */
    private static String branchOrNull(String branch) {
        return (branch != null && !branch.isEmpty()) ? branch : null;
    }

    /**
     * Inits the repository.
     *
     * @param repositoryDirectory the repository directory
     * @param isBare the is bare
     * @throws TransportException the transport exception
     * @throws GitAPIException the git API exception
     */
    public static void initRepository(String repositoryDirectory, Boolean isBare) throws TransportException, GitAPIException {
        try {
            InitCommand initCommand = Git.init();
            if (repositoryDirectory != null) {
                initCommand.setDirectory(new File(repositoryDirectory));
            }

            initCommand.setBare(isBare);

            initCommand.call();

        } catch (Exception ex) {
            String errorMessage = String.format("Failed to init repository [%s]", repositoryDirectory);
            throw new TransportException(errorMessage, ex);
        }
    }

}
