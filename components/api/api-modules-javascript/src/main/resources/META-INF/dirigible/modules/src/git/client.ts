const GitFacade = Java.type("org.eclipse.dirigible.components.api.git.GitFacade");

/**
 * Describes a file within the workspace, potentially with Git status information.
 */
export interface FileDescriptor {
    name: string;
    path: string;
    contentType: string;
    status: string;
}

/**
 * Describes a folder within the workspace, potentially with Git status information.
 */
export interface FolderDescriptor {
    name: string;
    path: string;
    status: string;
    folders: FolderDescriptor[];
    files: FileDescriptor[];
}

/**
 * Describes a Git-managed project/repository in the workspace.
 */
export interface ProjectDescriptor {
    name: string;
    path: string;
    git: boolean;
    gitName: string;
    folders: FolderDescriptor[];
    files: FileDescriptor[];
}

/**
 * Describes a single Git commit.
 */
export interface GitCommitInfo {
    id: string;
    author: string;
    emailAddress: string;
    dateTime: string;
    message: string;
}


/**
 * Describes a Git branch (local or remote).
 */
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

/**
 * Describes a file that has been changed (staged or unstaged).
 */
export interface GitChangedFile {
    path: string;
    type: number;
}

/**
* The IGitConnector provides the simplified methods for communicating with a Git SCM server.
* This interface is primarily implemented by the object returned from the native Java facade.
*/
export interface GitConnector {

    /**
     * Gets the origin urls.
     *
     * @returns An object containing the fetch and push URLs.
     */
    getOriginUrls(): { fetchUrl: string; pushUrl: string; };

    /**
     * Sets the fetch url.
     *
     * @param fetchUrl The new fetch URL.
     * @throws URISyntaxException, GitAPIException
     */
    setFetchUrl(fetchUrl: string): void

    /**
     * Sets the push url.
     *
     * @param pushUrl The new push URL.
     * @throws URISyntaxException, GitAPIException
     */
    setPushUrl(pushUrl: string): void

    /**
     * Adds file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files.
     * @throws IOException, NoFilepatternException, GitAPIException
     */
    add(filePattern: string): void

    /**
     * Adds deleted file(s) to the staging index.
     *
     * @param filePattern File to add content from. Example: "." includes all files.
     * @throws IOException, NoFilepatternException, GitAPIException
     */
    addDeleted(filePattern: string): void

    /**
     * Remove from the index.
     *
     * @param path The path to be removed.
     * @throws IOException, NoFilepatternException, GitAPIException
     */
    remove(path: string): void

    /**
     * Revert to head revision.
     *
     * @param path The path to be reverted.
     * @throws IOException, NoFilepatternException, GitAPIException
     */
    revert(path: string): void


    /**
     * Adds changes to the staging index, then performs a commit.
     *
     * @param message The commit message.
     * @param name The name of the committer.
     * @param email The email of the committer.
     * @param all If true, automatically stages modified and deleted files.
     * @throws Various Git exceptions
     */
    commit(message: string, name: string, email: string, all: boolean): void;

    /**
     * Creates new branch from a particular start point.
     *
     * @param name The new branch name.
     * @param startPoint Valid tree-ish object (e.g., "master", "HEAD", commit hash).
     * @throws RefAlreadyExistsException, GitAPIException
     */
    createBranch(name: string, startPoint: string): void

    /**
     * Deletes the branch.
     *
     * @param name The branch name to delete.
     * @throws RefAlreadyExistsException, GitAPIException
     */
    deleteBranch(name: string): void

    /**
     * Renames the branch.
     *
     * @param oldName The old branch name.
     * @param newName The new branch name.
     * @throws RefAlreadyExistsException, GitAPIException
     */
    renameBranch(oldName: string, newName: string): void

    /**
     * Creates new remote branch from a particular start point.
     *
     * @param name The branch name.
     * @param startPoint Valid tree-ish object.
     * @param username Username for the remote repository.
     * @param password Password for the remote repository.
     * @throws RefAlreadyExistsException, GitAPIException
     */
    createRemoteBranch(name: string, startPoint: string, username: string, password: string): void

    /**
     * Deletes the remote branch.
     *
     * @param name The name of the remote branch to delete.
     * @param username Username for the remote repository.
     * @param password Password for the remote repository.
     * @throws RefAlreadyExistsException, GitAPIException
     */
    deleteRemoteBranch(name: string, username: string, password: string): void

    /**
     * Checkout to a valid tree-ish object (e.g., branch name, commit hash).
     *
     * @param name The tree-ish object.
     * @returns The JGit Ref object.
     * @throws RefAlreadyExistsException, GitAPIException
     */
    checkout(name: string): any

    /**
     * Hard reset the repository. Resets working directory and staging index to match the Git repository HEAD.
     *
     * @throws CheckoutConflictException, GitAPIException
     */
    hardReset(): void

    /**
     * Fetches from a remote repository and tries to merge into the current branch (Pull).
     *
     * @throws Various Git exceptions
     */
    pull(): void

    /**
     * Fetches from a remote repository and tries to merge into the current branch (Pull).
     *
     * @param username Username for the remote repository.
     * @param password Password for the remote repository.
     * @throws Various Git exceptions
     */
    pull(username: string, password: string): void

    /**
     * Pushes the committed changes to the remote repository.
     *
     * @param username Username for the remote repository.
     * @param password Password for the remote repository.
     * @throws InvalidRemoteException, TransportException, GitAPIException
     */
    push(username: string, password: string): void

    /**
     * Tries to rebase the selected branch on top of the current one.
     *
     * @param name The branch to rebase.
     * @throws NoHeadException, WrongRepositoryStateException, GitAPIException
     */
    rebase(name: string): void

    /**
     * Get the current status of the Git repository.
     *
     * @returns The JGit Status object.
     * @throws NoWorkTreeException, GitAPIException
     */
    status(): any

    /**
     * Get the name of the current branch of the Git repository.
     *
     * @returns The branch name as a string.
     * @throws IOException
     */
    getBranch(): string

    /**
     * List all the local branches info.
     *
     * @returns A list of local {@link GitBranch} objects.
     * @throws GitConnectorException
     */
    getLocalBranches(): GitBranch[]

    /**
     * List all the remote branches info.
     *
     * @returns A list of remote {@link GitBranch} objects.
     * @throws GitConnectorException
     */
    getRemoteBranches(): GitBranch[]


    /**
     * Get the list of the unstaged files.
     *
     * @returns A list of {@link GitChangedFile} objects.
     * @throws GitConnectorException
     */
    getUnstagedChanges(): GitChangedFile[]

    /**
     * Get the list of the staged files.
     *
     * @returns A list of {@link GitChangedFile} objects.
     * @throws GitConnectorException
     */
    getStagedChanges(): GitChangedFile[]

    /**
     * Get file content from the HEAD.
     *
     * @param path The file path.
     * @param revStr The revision string (e.g., commit hash, branch name).
     * @returns The content of the file as a string.
     * @throws GitConnectorException
     */
    getFileContent(path: string, revStr: string): string

    /**
     * Get history of the repository or a specific file.
     *
     * @param path The file path or null to get the entire repository history.
     * @returns A list of {@link GitCommitInfo} objects.
     * @throws GitConnectorException
     */
    getHistory(path: string): GitCommitInfo[]
}

/**
 * Static client facade for workspace-level Git operations, abstracting the native GitFacade.
 */
export class Client {

    /**
     * Initializes a new Git repository for a project, performs an initial commit, and pushes.
     *
     * @param user The username of the committer.
     * @param email The email address of the committer.
     * @param workspaceName The name of the workspace.
     * @param projectName The name of the project.
     * @param repositoryName The name of the repository (where to put the git folder).
     * @param commitMessage The initial commit message.
     */
    public static initRepository(user: string, email: string, workspaceName: string, projectName: string, repositoryName: string, commitMessage: string): void {
        GitFacade.initRepository(user, email, workspaceName, projectName, repositoryName, commitMessage);
    }

    /**
     * Performs a commit in the specified repository.
     *
     * @param user The username of the committer.
     * @param email The email address of the committer.
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param commitMessage The commit message.
     * @param all If true, automatically stages modified and deleted files before committing.
     */
    public static commit(user: string, email: string, workspaceName: string, repositoryName: string, commitMessage: string, all: boolean): void {
        GitFacade.commit(user, email, workspaceName, repositoryName, commitMessage, all);
    }

    /**
     * Retrieves a list of all Git repositories (projects) within the specified workspace.
     *
     * @param workspaceName The name of the workspace.
     * @returns An array of {@link ProjectDescriptor} objects.
     */
    public static getGitRepositories(workspaceName: string): ProjectDescriptor[] {
        return GitFacade.getGitRepositories(workspaceName);
    }

    /**
     * Retrieves the commit history for the specified repository or a specific file path within it.
     *
     * @param repositoryName The name of the repository.
     * @param workspaceName The name of the workspace.
     * @param path The file path for history, or null/empty string for full repository history.
     * @returns An array of {@link GitCommitInfo} objects.
     */
    public static getHistory(repositoryName: string, workspaceName: string, path: string): GitCommitInfo[] {
        return GitFacade.getHistory(repositoryName, workspaceName, path);
    }

    /**
     * Deletes the specified Git repository.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository to delete.
     */
    public static deleteRepository(workspaceName: string, repositoryName: string): void {
        return GitFacade.deleteRepository(workspaceName, repositoryName);
    }

    /**
     * Clones a remote repository into the local workspace.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryUri The URI of the remote repository.
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @param branch The specific branch to checkout after cloning.
     * @returns A GitConnector instance for interacting directly with the cloned repository.
     */
    public static cloneRepository(workspaceName: string, repositoryUri: string, username: string, password: string, branch: string): GitConnector {
        // The native GitFacade returns a Java object implementing GitConnector
        return GitFacade.cloneRepository(workspaceName, repositoryUri, username, password, branch);
    }

    /**
     * Pulls changes from the remote repository and attempts to merge them into the current branch.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param username The username for authentication.
     * @param password The password for authentication.
     */
    public static pull(workspaceName: string, repositoryName: string, username: string, password: string): void {
        GitFacade.pull(workspaceName, repositoryName, username, password);
    }

    /**
     * Pushes the local commits to the remote repository.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param username The username for authentication.
     * @param password The password for authentication.
     */
    public static push(workspaceName: string, repositoryName: string, username: string, password: string): void {
        GitFacade.push(workspaceName, repositoryName, username, password);
    }

    /**
     * Checks out a specific branch, commit, or tag in the repository.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The branch or tree-ish object to check out.
     */
    public static checkout(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.checkout(workspaceName, repositoryName, branch);
    }

    /**
     * Creates a new branch starting from a specified point (e.g., HEAD, a commit hash, or another branch).
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The name of the new branch to create.
     * @param startingPoint The tree-ish object to start the new branch from.
     */
    public static createBranch(workspaceName: string, repositoryName: string, branch: string, startingPoint: string): void {
        GitFacade.createBranch(workspaceName, repositoryName, branch, startingPoint);
    }

    /**
     * Deletes a local branch.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The name of the branch to delete.
     */
    public static deleteBranch(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.deleteBranch(workspaceName, repositoryName, branch);
    }

    /**
     * Renames a local branch.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param oldName The current name of the branch.
     * @param newName The new name for the branch.
     */
    public static renameBranch(workspaceName: string, repositoryName: string, oldName: string, newName: string): void {
        GitFacade.renameBranch(workspaceName, repositoryName, oldName, newName);
    }

    /**
     * Creates a new remote branch on the Git server.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The name of the remote branch to create.
     * @param startingPoint The tree-ish object to start the new remote branch from.
     * @param username The username for authentication.
     * @param password The password for authentication.
     */
    public static createRemoteBranch(workspaceName: string, repositoryName: string, branch: string, startingPoint: string, username: string, password: string): void {
        GitFacade.createRemoteBranch(workspaceName, repositoryName, branch, startingPoint, username, password);
    }

    /**
     * Deletes a remote branch on the Git server.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The name of the remote branch to delete.
     * @param username The username for authentication.
     * @param password The password for authentication.
     */
    public static deleteRemoteBranch(workspaceName: string, repositoryName: string, branch: string, username: string, password: string): void {
        GitFacade.deleteRemoteBranch(workspaceName, repositoryName, branch, username, password);
    }

    /**
     * Resets the repository, discarding all uncommitted changes in the working directory and index.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     */
    public static hardReset(workspaceName: string, repositoryName: string): void {
        GitFacade.hardReset(workspaceName, repositoryName);
    }

    /**
     * Reapplies commits from the specified branch onto the current branch.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param branch The branch to rebase.
     */
    public static rebase(workspaceName: string, repositoryName: string, branch: string): void {
        GitFacade.rebase(workspaceName, repositoryName, branch);
    }

    /**
     * Retrieves the current status of the repository (staged, unstaged, untracked files).
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns A string representation of the repository status.
     */
    public static status(workspaceName: string, repositoryName: string): string {
        return GitFacade.status(workspaceName, repositoryName);
    }

    /**
     * Retrieves the name of the currently active branch.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns The name of the current branch.
     */
    public static getBranch(workspaceName: string, repositoryName: string): string {
        return GitFacade.getBranch(workspaceName, repositoryName);
    }

    /**
     * Retrieves a list of all local branches in the repository.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns An array of {@link GitBranch} objects representing local branches.
     */
    public static getLocalBranches(workspaceName: string, repositoryName: string): GitBranch[] {
        return GitFacade.getLocalBranches(workspaceName, repositoryName);
    }

    /**
     * Retrieves a list of all remote branches configured for the repository.
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns An array of {@link GitBranch} objects representing remote branches.
     */
    public static getRemoteBranches(workspaceName: string, repositoryName: string): GitBranch[] {
        return GitFacade.getRemoteBranches(workspaceName, repositoryName);
    }

    /**
     * Retrieves a list of all unstaged files (changes not yet added to the index).
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns An array of {@link GitChangedFile} objects.
     */
    public static getUnstagedChanges(workspaceName: string, repositoryName: string): GitChangedFile[] {
        return GitFacade.getUnstagedChanges(workspaceName, repositoryName);
    }

    /**
     * Retrieves a list of all staged files (changes added to the index, ready for commit).
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @returns An array of {@link GitChangedFile} objects.
     */
    public static getStagedChanges(workspaceName: string, repositoryName: string): GitChangedFile[] {
        // Corrected return type to array of GitChangedFile
        return GitFacade.getStagedChanges(workspaceName, repositoryName);
    }

    /**
     * Retrieves the content of a file at a specific revision (commit, branch, or tag).
     *
     * @param workspaceName The name of the workspace.
     * @param repositoryName The name of the repository.
     * @param filePath The path to the file.
     * @param revStr The revision string (e.g., commit hash or branch name).
     * @returns The content of the file as a string.
     */
    public static getFileContent(workspaceName: string, repositoryName: string, filePath: string, revStr: string): string {
        return GitFacade.getFileContent(workspaceName, repositoryName, filePath, revStr);
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Client;
}