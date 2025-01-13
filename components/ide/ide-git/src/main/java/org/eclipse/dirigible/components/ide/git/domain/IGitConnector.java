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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.dirigible.components.ide.git.project.ProjectOriginUrls;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;

/**
 * The IGitConnector provides the simplified methods for communicating with a Git SCM server.
 */
public interface IGitConnector {

    /** The Constant GIT_REFS_HEADS_MASTER. */
    public static final String GIT_REFS_HEADS_MASTER = "refs/heads/master"; //$NON-NLS-1$

    /** The Constant GIT_MERGE. */
    public static final String GIT_MERGE = "merge"; //$NON-NLS-1$

    /** The Constant GIT_MASTER. */
    public static final String GIT_MASTER = "master"; //$NON-NLS-1$

    /** The Constant GIT_BRANCH. */
    public static final String GIT_BRANCH = "branch"; //$NON-NLS-1$

    /** The Constant GIT_ADD_ALL_FILE_PATTERN. */
    public static final String GIT_ADD_ALL_FILE_PATTERN = "."; //$NON-NLS-1$

    /**
     * Gets the origin urls.
     *
     * @return the origin urls
     */
    ProjectOriginUrls getOriginUrls();

    /**
     * Sets the fetch url.
     *
     * @param fetchUrl the new fetch url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    void setFetchUrl(String fetchUrl) throws URISyntaxException, GitAPIException;

    /**
     * Sets the push url.
     *
     * @param pushUrl the new push url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    void setPushUrl(String pushUrl) throws URISyntaxException, GitAPIException;

    /**
     * Adds file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files. If "dir/subdir/" is
     *        directory then "dir/subdir" all files from the directory recursively
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    void add(String filePattern) throws IOException, NoFilepatternException, GitAPIException;

    /**
     * Adds deleted file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files. If "dir/subdir/" is
     *        directory then "dir/subdir" all files from the directory recursively
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    void addDeleted(String filePattern) throws IOException, NoFilepatternException, GitAPIException;

    /**
     * Remove from the index.
     *
     * @param path the path to be removed
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    void remove(String path) throws IOException, NoFilepatternException, GitAPIException;

    /**
     * Revert to head revision.
     *
     * @param path the path to be removed
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    void revert(String path) throws IOException, NoFilepatternException, GitAPIException;


    /**
     * Adds changes to the staging index. Then makes commit.
     *
     * @param message the commit message
     * @param name the name of the committer used for the commit
     * @param email the email of the committer used for the commit
     * @param all if set to true, command will automatically stages files that have been modified and
     *        deleted, but new files not known by the repository are not affected. This corresponds to
     *        the parameter -a on the command line.
     * @throws NoHeadException No Head Exception
     * @throws NoMessageException No Message Exception
     * @throws UnmergedPathsException Unmerged Path Exception
     * @throws ConcurrentRefUpdateException Concurrent Ref Update Exception
     * @throws WrongRepositoryStateException Wrong Repository State Exception
     * @throws GitAPIException Git API Exception
     * @throws IOException IO Exception
     */
    void commit(String message, String name, String email, boolean all) throws NoHeadException, NoMessageException, UnmergedPathsException,
            ConcurrentRefUpdateException, WrongRepositoryStateException, GitAPIException, IOException;

    /**
     * Creates new branch from a particular start point.
     *
     * @param name the branch name
     * @param startPoint valid tree-ish object example: "5c15e8", "master", "HEAD",
     *        "21d5a96070353d01c0f30bc0559ab4de4f5e3ca0"
     * @throws RefAlreadyExistsException Already Exists Exception
     * @throws RefNotFoundException Ref Not Found Exception
     * @throws InvalidRefNameException Invalid Ref Name Exception
     * @throws GitAPIException Git API Exception
     */
    void createBranch(String name, String startPoint)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException;

    /**
     * Deletes the branch.
     *
     * @param name the name
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    void deleteBranch(String name) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException;

    /**
     * Renames the branch.
     *
     * @param oldName the oldName
     * @param newName the newName
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    void renameBranch(String oldName, String newName)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException;

    /**
     * Creates new remote branch from a particular start point.
     *
     * @param name the branch name
     * @param startPoint valid tree-ish object example: "5c15e8", "master", "HEAD",
     *        "21d5a96070353d01c0f30bc0559ab4de4f5e3ca0"
     * @param username the username
     * @param password the password
     * @throws RefAlreadyExistsException Already Exists Exception
     * @throws RefNotFoundException Ref Not Found Exception
     * @throws InvalidRefNameException Invalid Ref Name Exception
     * @throws GitAPIException Git API Exception
     */
    void createRemoteBranch(String name, String startPoint, String username, String password)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException;

    /**
     * Deletes the remote branch.
     *
     * @param name the name
     * @param username the username
     * @param password the password
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    void deleteRemoteBranch(String name, String username, String password)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException;

    /**
     * Checkout to a valid tree-ish object example: "5c15e8", "master", "HEAD",
     * "21d5a96070353d01c0f30bc0559ab4de4f5e3ca0".
     *
     * @param name the tree-ish object
     * @return {@link org.eclipse.jgit.lib.Ref} object
     * @throws RefAlreadyExistsException Ref Already Exists Exception
     * @throws RefNotFoundException Ref Not Found Exception
     * @throws InvalidRefNameException Invalid Ref Name Exception
     * @throws CheckoutConflictException Checkout Conflict Exception
     * @throws GitAPIException Git API Exception
     */
    Ref checkout(String name)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException;

    /**
     * Hard reset the repository. Makes the working directory and staging index content to exactly match
     * the Git repository.
     *
     * @throws CheckoutConflictException Checkout Conflict Exception
     * @throws GitAPIException Git API Exception
     */
    void hardReset() throws CheckoutConflictException, GitAPIException;

    /**
     * Fetches from a remote repository and tries to merge into the current branch.
     *
     * @throws WrongRepositoryStateException Wrong Repository State Exception
     * @throws InvalidConfigurationException Invalid Configuration Exception
     * @throws DetachedHeadException Detached Head Exception
     * @throws InvalidRemoteException Invalid Remote Exception
     * @throws CanceledException Canceled Exception
     * @throws RefNotFoundException Ref Not Found Exception
     * @throws NoHeadException No Head Exception
     * @throws TransportException Transport Exception
     * @throws GitAPIException Git API Exception
     */
    void pull() throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
            CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException;

    /**
     * Fetches from a remote repository and tries to merge into the current branch.
     *
     * @param username for the remote repository
     * @param password for the remote repository
     * @throws WrongRepositoryStateException Wrong Repository State Exception
     * @throws InvalidConfigurationException Invalid Configuration Exception
     * @throws DetachedHeadException Detached Head Exception
     * @throws InvalidRemoteException Invalid Remote Exception
     * @throws CanceledException Canceled Exception
     * @throws RefNotFoundException Ref Not Found Exception
     * @throws NoHeadException No Head Exception
     * @throws TransportException Transport Exception
     * @throws GitAPIException Git API Exception
     */
    void pull(String username, String password) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
            InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException;

    /**
     * Pushes the committed changes to the remote repository.
     *
     * @param username for the remote repository
     * @param password for the remote repository
     * @throws InvalidRemoteException Invalid Remote Exception
     * @throws TransportException Transport Exception
     * @throws GitAPIException Git API Exception
     */
    void push(String username, String password) throws InvalidRemoteException, TransportException, GitAPIException;

    /**
     * Tries to rebase the selected branch on top of the current one.
     *
     * @param name the branch to rebase
     * @throws NoHeadException No Head Exception
     * @throws WrongRepositoryStateException Wrong Repository State Exception
     * @throws GitAPIException Git API Exception
     */
    void rebase(String name) throws NoHeadException, WrongRepositoryStateException, GitAPIException;

    /**
     * Get the current status of the Git repository.
     *
     * @return {@link org.eclipse.jgit.api.Status} object
     * @throws NoWorkTreeException No Work Tree Exception
     * @throws GitAPIException Git API Exception
     */
    Status status() throws NoWorkTreeException, GitAPIException;

    /**
     * Get the current branch of the Git repository.
     *
     * @return the branch
     * @throws IOException IO Exception
     */
    String getBranch() throws IOException;

    // /**
    // * Returns the SHA of the last commit on the specified branch.
    // *
    // * @param branch
    // * the name of the specified branch
    // * @return SHA example: "21d5a96070353d01c0f30bc0559ab4de4f5e3ca0"
    // * @throws RefAlreadyExistsException
    // * Ref Already Exists Exception
    // * @throws RefNotFoundException
    // * Ref Not Found Exception
    // * @throws InvalidRefNameException
    // * Invalid Ref Name Exception
    // * @throws CheckoutConflictException
    // * Checkout Conflict Exception
    // * @throws GitAPIException
    // * Git API Exception
    // */
    // String getLastSHAForBranch(String branch)
    // throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException,
    // CheckoutConflictException, GitAPIException;

    /**
     * List all the local branches info.
     *
     * @return the list of branches
     * @throws GitConnectorException in case of exception
     */
    List<GitBranch> getLocalBranches() throws GitConnectorException;

    /**
     * List all the remote branches info.
     *
     * @return the list of branches
     * @throws GitConnectorException in case of exception
     */
    List<GitBranch> getRemoteBranches() throws GitConnectorException;


    /**
     * Get the list of the unstaged files.
     *
     * @return the list
     * @throws GitConnectorException in case of exception
     */
    List<GitChangedFile> getUnstagedChanges() throws GitConnectorException;

    /**
     * Get the list of the staged files.
     *
     * @return the list
     * @throws GitConnectorException in case of exception
     */
    List<GitChangedFile> getStagedChanges() throws GitConnectorException;

    /**
     * Get file content from the HEAD.
     *
     * @param path the path
     * @param revStr the revStr
     * @return the content
     * @throws GitConnectorException in case of exception
     */
    String getFileContent(String path, String revStr) throws GitConnectorException;

    /**
     * Get history.
     *
     * @param path the file path or null
     * @return the history of a file or the whole git repo
     * @throws GitConnectorException in case of exception
     */
    List<GitCommitInfo> getHistory(String path) throws GitConnectorException;

}
