/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const GitFacade = Java.type("org.eclipse.dirigible.components.api.git.GitFacade");

export interface FileDescriptor {
    name: string;
    path: string;
    contentType: string;
    status: string;
}

export interface FolderDescriptor {
    name: string;
    path: string;
    status: string;
    folders: FolderDescriptor[];
    files: FileDescriptor[];
}

export interface ProjectDescriptor {
    name: string;
    path: string;
    git: boolean;
    gitName: string;
    folders: FolderDescriptor[];
    files: FileDescriptor[];
}

export interface GitCommitInfo {
    id: string;
    author: string;
    emailAddress: string;
    dateTime: string;
    message: string;
}


export interface GitBranch {
    name: string;
    remote: boolean;
    current: boolean;
    commitObjectId: string;
    commitShortId: string;
    commitDate: string;
    commitMessage: string;
    commitAuthor: string;
}

export interface GitChangedFile {
    path: string;
    type: number;
}

/**
* The IGitConnector provides the simplified methods for communicating with a Git SCM server.
*/
export interface GitConnector {

    /**
     * Gets the origin urls.
     *
     * @return the origin urls
     */
    getOriginUrls(): { fetchUrl: string; pushUrl: string; };

    /**
     * Sets the fetch url.
     *
     * @param fetchUrl the new fetch url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    setFetchUrl(fetchUrl: string): void

    /**
     * Sets the push url.
     *
     * @param pushUrl the new push url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    setPushUrl(pushUrl: string): void

    /**
     * Adds file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files. If "dir/subdir/" is
     *        directory then "dir/subdir" all files from the directory recursively
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    add(filePattern: string): void

    /**
     * Adds deleted file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files. If "dir/subdir/" is
     *        directory then "dir/subdir" all files from the directory recursively
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    addDeleted(filePattern: string): void

    /**
     * Remove from the index.
     *
     * @param path the path to be removed
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    remove(path: string): void

    /**
     * Revert to head revision.
     *
     * @param path the path to be removed
     * @throws IOException IO Exception
     * @throws NoFilepatternException No File Pattern Exception
     * @throws GitAPIException Git API Exception
     */
    revert(path: string): void


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
    commit(message: string, name: string, email: string, all: boolean): void;

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
    createBranch(name: string, startPoint: string): void

    /**
     * Deletes the branch.
     *
     * @param name the name
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    deleteBranch(name: string): void

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
    renameBranch(oldName: string, newName: string): void

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
    createRemoteBranch(name: string, startPoint: string, username: string, password: string): void

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
    deleteRemoteBranch(name: string, username: string, password: string): void

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
    checkout(name: string): any

    /**
     * Hard reset the repository. Makes the working directory and staging index content to exactly match
     * the Git repository.
     *
     * @throws CheckoutConflictException Checkout Conflict Exception
     * @throws GitAPIException Git API Exception
     */
    hardReset(): void

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
    pull(): void

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
    pull(username: string, password: string): void

    /**
     * Pushes the committed changes to the remote repository.
     *
     * @param username for the remote repository
     * @param password for the remote repository
     * @throws InvalidRemoteException Invalid Remote Exception
     * @throws TransportException Transport Exception
     * @throws GitAPIException Git API Exception
     */
    push(username: string, password: string): void

    /**
     * Tries to rebase the selected branch on top of the current one.
     *
     * @param name the branch to rebase
     * @throws NoHeadException No Head Exception
     * @throws WrongRepositoryStateException Wrong Repository State Exception
     * @throws GitAPIException Git API Exception
     */
    rebase(name: string): void

    /**
     * Get the current status of the Git repository.
     *
     * @return {@link org.eclipse.jgit.api.Status} object
     * @throws NoWorkTreeException No Work Tree Exception
     * @throws GitAPIException Git API Exception
     */
    status(): any

    /**
     * Get the current branch of the Git repository.
     *
     * @return the branch
     * @throws IOException IO Exception
     */
    getBranch(): string

    /**
     * List all the local branches info.
     *
     * @return the list of branches
     * @throws GitConnectorException in case of exception
     */
    getLocalBranches(): GitBranch[]

    /**
     * List all the remote branches info.
     *
     * @return the list of branches
     * @throws GitConnectorException in case of exception
     */
    getRemoteBranches(): GitBranch[]


    /**
     * Get the list of the unstaged files.
     *
     * @return the list
     * @throws GitConnectorException in case of exception
     */
    getUnstagedChanges(): GitChangedFile[]

    /**
     * Get the list of the staged files.
     *
     * @return the list
     * @throws GitConnectorException in case of exception
     */
    getStagedChanges(): GitChangedFile[]

    /**
     * Get file content from the HEAD.
     *
     * @param path the path
     * @param revStr the revStr
     * @return the content
     * @throws GitConnectorException in case of exception
     */
    getFileContent(path: string, revStr: string): string

    /**
     * Get history.
     *
     * @param path the file path or null
     * @return the history of a file or the whole git repo
     * @throws GitConnectorException in case of exception
     */
    getHistory(path: string): GitCommitInfo[]
}


export class Client {

    public static initRepository(user: string, email: string, workspaceName: string, projectName: string, repositoryName: string, commitMessage: string): void {
        GitFacade.initRepository(user, email, workspaceName, projectName, repositoryName, commitMessage);
    }

    public static commit(user: string, email: string, workspaceName: string, repositoryName: string, commitMessage: string, all: boolean): void {
        GitFacade.commit(user, email, workspaceName, repositoryName, commitMessage, all);
    }

    public static getGitRepositories(workspaceName: string): ProjectDescriptor[] {
        return GitFacade.getGitRepositories(workspaceName);
    }

    public static getHistory(repositoryName: string, workspaceName: string, path: string): GitCommitInfo[] {
        return GitFacade.getHistory(repositoryName, workspaceName, path);
    }

    public static deleteRepository(workspaceName: string, repositoryName: string): void {
        return GitFacade.deleteRepository(workspaceName, repositoryName);
    }

    public static cloneRepository(workspaceName: string, repositoryUri: string, username: string, password: string, branch: string): GitConnector {
        return GitFacade.cloneRepository(workspaceName, repositoryUri, username, password, branch);
    }

    public static pull(workspaceName: string, repositoryName: string, username: string, password: string): void {
        GitFacade.pull(workspaceName, repositoryName, username, password);
    }

    public static push(workspaceName: string, repositoryName: string, username: string, password: string): void {
        GitFacade.push(workspaceName, repositoryName, username, password);
    }

    public static checkout(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.checkout(workspaceName, repositoryName, branch);
    }

    public static createBranch(workspaceName: string, repositoryName: string, branch: string, startingPoint: string): void {
        GitFacade.createBranch(workspaceName, repositoryName, branch, startingPoint);
    }

    public static deleteBranch(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.createBranch(workspaceName, repositoryName, branch);
    }

    public static renameBranch(workspaceName: string, repositoryName: string, oldName: string, newName: string): void {
        GitFacade.createBranch(workspaceName, repositoryName, oldName, newName);
    }

    public static createRemoteBranch(workspaceName: string, repositoryName: string, branch: string, startingPoint: string, username: string, password: string): void {
        GitFacade.createRemoteBranch(workspaceName, repositoryName, branch, startingPoint, username, password);
    }

    public static deleteRemoteBranch(workspaceName: string, repositoryName: string, branch: string, username: string, password: string): void {
        GitFacade.createRemoteBranch(workspaceName, repositoryName, branch, username, password);
    }

    public static hardReset(workspaceName: string, repositoryName: string): void {
        GitFacade.hardReset(workspaceName, repositoryName);
    }

    public static rebase(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.rebase(workspaceName, repositoryName, branch);
    }

    public static status(workspaceName: string, repositoryName: string): string {
        return GitFacade.status(workspaceName, repositoryName);
    }

    public static getBranch(workspaceName: string, repositoryName: string): string {
        return GitFacade.getBranch(workspaceName, repositoryName);
    }

    public static getLocalBranches(workspaceName: string, repositoryName: string): GitBranch[] {
        return GitFacade.getLocalBranches(workspaceName, repositoryName);
    }

    public static getRemoteBranches(workspaceName: string, repositoryName: string): GitBranch[] {
        return GitFacade.getRemoteBranches(workspaceName, repositoryName);
    }

    public static getUnstagedChanges(workspaceName: string, repositoryName: string): GitChangedFile[] {
        return GitFacade.getUnstagedChanges(workspaceName, repositoryName);
    }

    public static getStagedChanges(workspaceName: string, repositoryName: string): GitChangedFile {
        return GitFacade.getStagedChanges(workspaceName, repositoryName);
    }

    public static getFileContent(workspaceName: string, repositoryName: string, filePath: string, revStr: string): string {
        return GitFacade.getFileContent(workspaceName, repositoryName, filePath, revStr);
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Client;
}
