/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.git;

import java.io.IOException;
import java.util.List;
import org.eclipse.dirigible.components.api.git.GitFacade;
import org.eclipse.dirigible.components.ide.git.domain.GitBranch;
import org.eclipse.dirigible.components.ide.git.domain.GitChangedFile;
import org.eclipse.dirigible.components.ide.git.domain.GitCommitInfo;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.workspace.json.ProjectDescriptor;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * JGit-backed operations against repositories the IDE knows about under a user's workspace. Covers
 * the everyday flow — clone, pull, push, checkout, status, log, file content at a given revision —
 * plus branch and remote management.
 * <p>
 * Repositories are addressed by (workspace name, repository name); the URI / credentials only
 * appear on the clone and pull/push operations. Every method that touches the repository propagates
 * the underlying JGit / connector checked exceptions verbatim — handle them at the controller / job
 * boundary the way you would with {@link GitFacade} directly.
 * <p>
 * For ad-hoc Git work that does not need to live in a workspace (CI scripts, throwaway tooling),
 * the bare JGit API ({@code org.eclipse.jgit.api.Git}) is a more appropriate fit.
 */
public final class Git {

    private Git() {}

    public static void initRepository(String username, String email, String workspaceName, String projectName, String repositoryName,
            String commitMessage) throws IOException, GitAPIException, GitConnectorException {
        GitFacade.initRepository(username, email, workspaceName, projectName, repositoryName, commitMessage);
    }

    public static void commit(String username, String email, String workspaceName, String repositoryName, String commitMessage, Boolean add)
            throws GitAPIException, IOException, GitConnectorException {
        GitFacade.commit(username, email, workspaceName, repositoryName, commitMessage, add);
    }

    public static List<ProjectDescriptor> getRepositories(String workspaceName) {
        return GitFacade.getGitRepositories(workspaceName);
    }

    public static List<GitCommitInfo> getHistory(String repositoryName, String workspaceName, String path) throws GitConnectorException {
        return GitFacade.getHistory(repositoryName, workspaceName, path);
    }

    public static void deleteRepository(String workspaceName, String repositoryName) throws GitConnectorException {
        GitFacade.deleteRepository(workspaceName, repositoryName);
    }

    public static IGitConnector cloneRepository(String workspaceName, String repositoryUri, String username, String password, String branch)
            throws IOException, GitAPIException {
        return GitFacade.cloneRepository(workspaceName, repositoryUri, username, password, branch);
    }

    public static void pull(String workspaceName, String repositoryName, String username, String password)
            throws GitAPIException, IOException, GitConnectorException {
        GitFacade.pull(workspaceName, repositoryName, username, password);
    }

    public static void push(String workspaceName, String repositoryName, String username, String password)
            throws GitAPIException, IOException, GitConnectorException {
        GitFacade.push(workspaceName, repositoryName, username, password);
    }

    public static void checkout(String workspaceName, String repositoryName, String branchName)
            throws GitAPIException, IOException, GitConnectorException {
        GitFacade.checkout(workspaceName, repositoryName, branchName);
    }

    public static void hardReset(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        GitFacade.hardReset(workspaceName, repositoryName);
    }

    public static void rebase(String workspaceName, String repositoryName, String branchName)
            throws GitAPIException, IOException, GitConnectorException {
        GitFacade.rebase(workspaceName, repositoryName, branchName);
    }

    public static Status status(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.status(workspaceName, repositoryName);
    }

    public static String getBranch(String workspaceName, String repositoryName) throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getBranch(workspaceName, repositoryName);
    }

    public static List<GitBranch> getLocalBranches(String workspaceName, String repositoryName)
            throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getLocalBranches(workspaceName, repositoryName);
    }

    public static List<GitBranch> getRemoteBranches(String workspaceName, String repositoryName)
            throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getRemoteBranches(workspaceName, repositoryName);
    }

    public static List<GitChangedFile> getUnstagedChanges(String workspaceName, String repositoryName)
            throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getUnstagedChanges(workspaceName, repositoryName);
    }

    public static List<GitChangedFile> getStagedChanges(String workspaceName, String repositoryName)
            throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getStagedChanges(workspaceName, repositoryName);
    }

    public static String getFileContent(String workspaceName, String repositoryName, String filePath, String revStr)
            throws GitAPIException, IOException, GitConnectorException {
        return GitFacade.getFileContent(workspaceName, repositoryName, filePath, revStr);
    }
}
