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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.ide.git.project.ProjectOriginUrls;
import org.eclipse.dirigible.components.ide.git.utils.RemoteUrl;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.RenameBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
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
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GitConnector utility is used for simplified communication with Git SCM server.
 */
public class GitConnector implements IGitConnector {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GitConnector.class);

    /** The git. */
    private final Git git;

    /** The repository. */
    private Repository repository;

    /**
     * Instantiates a new git connector.
     *
     * @param repository the repository
     * @throws IOException Signals that an I/O exception has occurred.
     */
    GitConnector(Repository repository) throws IOException {
        this.repository = repository;
        this.git = new Git(repository);
    }

    /**
     * Gets the origin urls.
     *
     * @return the origin urls
     */
    @Override
    public ProjectOriginUrls getOriginUrls() {
        StoredConfig gitConfig = repository.getConfig();

        String fetchUrl = gitConfig.getString("remote", "origin", "url");

        String pushUrl;
        pushUrl = gitConfig.getString("remote", "origin", "pushurl");
        if (pushUrl == null) {
            pushUrl = fetchUrl; // fallback to fetch url if no explicit pushurl is configured
        }

        return new ProjectOriginUrls(fetchUrl, pushUrl);
    }

    /**
     * Sets the fetch url.
     *
     * @param fetchUrl the new fetch url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void setFetchUrl(String fetchUrl) throws URISyntaxException, GitAPIException {
        RemoteUrl remoteGit = new RemoteUrl(repository);
        remoteGit.setUriType(RemoteSetUrlCommand.UriType.FETCH);
        remoteGit.setRemoteName("origin");
        URIish newUrl = new URIish(fetchUrl);
        remoteGit.setRemoteUri(newUrl);
        remoteGit.call();
    }

    /**
     * Sets the push url.
     *
     * @param pushUrl the new push url
     * @throws URISyntaxException the URI syntax exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void setPushUrl(String pushUrl) throws URISyntaxException, GitAPIException {
        RemoteUrl remoteGit = new RemoteUrl(repository);
        remoteGit.setUriType(RemoteSetUrlCommand.UriType.PUSH);
        remoteGit.setRemoteName("origin");
        URIish newUrl = new URIish(pushUrl);
        remoteGit.setRemoteUri(newUrl);
        remoteGit.call();
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Gets the git.
     *
     * @return the git
     */
    public Git getGit() {
        return git;
    }

    /**
     * Adds the.
     *
     * @param filePattern the file pattern
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NoFilepatternException the no filepattern exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void add(String filePattern) throws IOException, NoFilepatternException, GitAPIException {
        AddCommand addCommand = git.add();
        addCommand.addFilepattern(filePattern);
        addCommand.call();
    }

    /**
     * Adds the deleted.
     *
     * @param filePattern the file pattern
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NoFilepatternException the no filepattern exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void addDeleted(String filePattern) throws IOException, NoFilepatternException, GitAPIException {
        RmCommand rmCommand = git.rm();
        rmCommand.addFilepattern(filePattern);
        rmCommand.call();
    }

    /**
     * Removes the.
     *
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NoFilepatternException the no filepattern exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void remove(String path) throws IOException, NoFilepatternException, GitAPIException {
        if (repository.resolve(Constants.HEAD) != null) {
            ResetCommand reset = git.reset();
            reset.setRef(Constants.HEAD);
            reset.addPath(path);
            reset.call();
        } else {
            RmCommand rmCommand = git.rm();
            rmCommand.setCached(true);
            rmCommand.addFilepattern(path);
            rmCommand.call();
        }
    }

    /**
     * Revert.
     *
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NoFilepatternException the no filepattern exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void revert(String path) throws IOException, NoFilepatternException, GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.addPath(path);
        checkoutCommand.call();
    }

    /**
     * Commit.
     *
     * @param message the message
     * @param name the name
     * @param email the email
     * @param all the all
     * @throws NoHeadException the no head exception
     * @throws NoMessageException the no message exception
     * @throws UnmergedPathsException the unmerged paths exception
     * @throws ConcurrentRefUpdateException the concurrent ref update exception
     * @throws WrongRepositoryStateException the wrong repository state exception
     * @throws GitAPIException the git API exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void commit(String message, String name, String email, boolean all) throws NoHeadException, NoMessageException,
            UnmergedPathsException, ConcurrentRefUpdateException, WrongRepositoryStateException, GitAPIException, IOException {
        CommitCommand commitCommand = git.commit();
        commitCommand.setMessage(message);
        commitCommand.setCommitter(name, email);
        commitCommand.setAuthor(name, email);
        commitCommand.setAll(all);
        commitCommand.setAllowEmpty(true);
        commitCommand.call();
    }

    /**
     * Creates the branch.
     *
     * @param name the name
     * @param startPoint the start point
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void createBranch(String name, String startPoint)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        repository.getConfig()
                  .setString(GIT_BRANCH, name, GIT_MERGE, GIT_REFS_HEADS_MASTER);
        CreateBranchCommand createBranchCommand = git.branchCreate();
        createBranchCommand.setName(name);
        if (!startPoint.equals("HEAD")) {
            createBranchCommand.setStartPoint(startPoint);
            createBranchCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
        }
        createBranchCommand.call();
    }

    /**
     * Deletes the branch.
     *
     * @param name the name
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void deleteBranch(String name) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        repository.getConfig()
                  .setString(GIT_BRANCH, name, GIT_MERGE, GIT_REFS_HEADS_MASTER);
        DeleteBranchCommand deleteBranchCommand = git.branchDelete();
        deleteBranchCommand.setBranchNames(name);
        deleteBranchCommand.call();
    }

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
    @Override
    public void renameBranch(String oldName, String newName)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        repository.getConfig()
                  .setString(GIT_BRANCH, oldName, GIT_MERGE, GIT_REFS_HEADS_MASTER);
        RenameBranchCommand renameBranchCommand = git.branchRename();
        renameBranchCommand.setOldName(oldName);
        renameBranchCommand.setNewName(newName);
        renameBranchCommand.call();
    }

    /**
     * Creates the remote branch.
     *
     * @param name the name
     * @param startPoint the start point
     * @param username the username
     * @param password the password
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void createRemoteBranch(String name, String startPoint, String username, String password)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        repository.getConfig()
                  .setString(GIT_BRANCH, name, GIT_MERGE, GIT_REFS_HEADS_MASTER);
        CreateBranchCommand createBranchCommand = git.branchCreate();
        createBranchCommand.setName(name);
        if (!startPoint.equals("HEAD")) {
            createBranchCommand.setStartPoint(startPoint);
            createBranchCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
        }
        createBranchCommand.call();

        git.push()
           .setRemote("origin")
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
           .setRefSpecs(new RefSpec(name + ":" + name))
           .call();

        git.branchDelete()
           .setBranchNames(name)
           .call();
        git.checkout()
           .setCreateBranch(true)
           .setName(name)
           .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
           .setStartPoint("origin/" + name)
           .call();
    }

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
    @Override
    public void deleteRemoteBranch(String name, String username, String password)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {

        String remoteName = "refs/heads/" + name;
        git.branchDelete()
           .setBranchNames(remoteName)
           .call();

        RefSpec refSpec = new RefSpec().setSource(null)
                                       .setDestination(remoteName);
        git.push()
           .setRefSpecs(refSpec)
           .setRemote("origin")
           .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
           .call();
    }

    /**
     * Checkout.
     *
     * @param name the name
     * @return the ref
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws CheckoutConflictException the checkout conflict exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public Ref checkout(String name)
            throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
        try {
            CheckoutCommand checkoutCommand = git.checkout();
            checkoutCommand.setName(name);
            checkoutCommand.setCreateBranch(true);
            checkoutCommand.setForce(true);
            checkoutCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
            checkoutCommand.setStartPoint("origin/" + name);
            return checkoutCommand.call();
        } catch (RefAlreadyExistsException e) {
            CheckoutCommand checkoutCommand = git.checkout();
            checkoutCommand.setName(name);
            return checkoutCommand.call();
        }
    }

    /**
     * Hard reset.
     *
     * @throws CheckoutConflictException the checkout conflict exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void hardReset() throws CheckoutConflictException, GitAPIException {
        ResetCommand resetCommand = git.reset();
        resetCommand.setMode(ResetType.HARD);
        resetCommand.call();
    }

    /**
     * Pull.
     *
     * @throws WrongRepositoryStateException the wrong repository state exception
     * @throws InvalidConfigurationException the invalid configuration exception
     * @throws DetachedHeadException the detached head exception
     * @throws InvalidRemoteException the invalid remote exception
     * @throws CanceledException the canceled exception
     * @throws RefNotFoundException the ref not found exception
     * @throws NoHeadException the no head exception
     * @throws TransportException the transport exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void pull() throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
            CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException {
        PullCommand pullCommand = git.pull();
        pullCommand.call();
    }

    /**
     * Pull.
     *
     * @param username the username
     * @param password the password
     * @throws WrongRepositoryStateException the wrong repository state exception
     * @throws InvalidConfigurationException the invalid configuration exception
     * @throws DetachedHeadException the detached head exception
     * @throws InvalidRemoteException the invalid remote exception
     * @throws CanceledException the canceled exception
     * @throws RefNotFoundException the ref not found exception
     * @throws NoHeadException the no head exception
     * @throws TransportException the transport exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void pull(String username, String password)
            throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException,
            CanceledException, RefNotFoundException, NoHeadException, TransportException, GitAPIException {
        PullCommand pullCommand = git.pull();
        pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        pullCommand.call();
    }

    /**
     * Push.
     *
     * @param username the username
     * @param password the password
     * @throws InvalidRemoteException the invalid remote exception
     * @throws TransportException the transport exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void push(String username, String password) throws InvalidRemoteException, TransportException, GitAPIException {
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        pushCommand.call();
    }

    /**
     * Rebase.
     *
     * @param name the name
     * @throws NoHeadException the no head exception
     * @throws WrongRepositoryStateException the wrong repository state exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public void rebase(String name) throws NoHeadException, WrongRepositoryStateException, GitAPIException {
        RebaseCommand rebaseCommand = git.rebase();
        rebaseCommand.setOperation(Operation.BEGIN);
        rebaseCommand.setUpstream(name);
        rebaseCommand.call();
    }

    /**
     * Status.
     *
     * @return the status
     * @throws NoWorkTreeException the no work tree exception
     * @throws GitAPIException the git API exception
     */
    @Override
    public Status status() throws NoWorkTreeException, GitAPIException {
        return git.status()
                  .call();
    }

    /**
     * Gets the branch.
     *
     * @return the branch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public String getBranch() throws IOException {
        return git.getRepository()
                  .getBranch();
    }

    /** The Constant format. */
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Gets the local branches.
     *
     * @return the local branches
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public List<GitBranch> getLocalBranches() throws GitConnectorException {
        try {
            List<GitBranch> result = new ArrayList<GitBranch>();
            List<Ref> branches = git.branchList()
                                    .call(); // .setListMode(ListMode.ALL)
            Collections.sort(branches, new Comparator<Ref>() {
                @Override
                public int compare(Ref ref1, Ref ref2) {
                    return getShortBranchName(ref1).compareTo(getShortBranchName(ref2));
                }
            });

            String currentBranch = getBranch();
            RevWalk walk = new RevWalk(git.getRepository());
            try {
                for (Ref branch : branches) {
                    RevCommit commit = walk.parseCommit(branch.getObjectId());
                    String shortLocalBranchName = getShortBranchName(branch);
                    GitBranch gitBranch = new GitBranch(shortLocalBranchName, false, currentBranch.equals(shortLocalBranchName),
                            commit.getId()
                                  .getName(),
                            commit.getId()
                                  .abbreviate(7)
                                  .name(),
                            format.format(commit.getAuthorIdent()
                                                .getWhen()),
                            commit.getShortMessage(), commit.getAuthorIdent()
                                                            .getName());
                    result.add(gitBranch);
                }
            } finally {
                walk.close();
            }
            return result;
        } catch (GitAPIException | IOException e) {
            throw new GitConnectorException(e);
        }
    }

    /**
     * Gets the remote branches.
     *
     * @return the remote branches
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public List<GitBranch> getRemoteBranches() throws GitConnectorException {
        try {
            List<GitBranch> result = new ArrayList<GitBranch>();
            Collection<Ref> remotes = Git.lsRemoteRepository()
                                         .setHeads(true)
                                         .setRemote(git.getRepository()
                                                       .getConfig()
                                                       .getString("remote", "origin", "url"))
                                         .call();

            List<Ref> branches = new ArrayList<Ref>(remotes);

            RevWalk walk = new RevWalk(git.getRepository());
            try {
                for (Ref branch : branches) {
                    try {
                        RevCommit commit = walk.parseCommit(branch.getObjectId());
                        GitBranch gitBranch = new GitBranch(getShortBranchName(branch), true, false, commit.getId()
                                                                                                           .getName(),
                                commit.getId()
                                      .abbreviate(7)
                                      .name(),
                                format.format(commit.getAuthorIdent()
                                                    .getWhen()),
                                commit.getShortMessage(), commit.getAuthorIdent()
                                                                .getName());
                        result.add(gitBranch);
                    } catch (MissingObjectException e) {
                        // pass
                    }
                }
            } finally {
                walk.close();
            }
            return result;
        } catch (GitAPIException | IOException e) {
            throw new GitConnectorException(e);
        }
    }

    /**
     * Returns the short branch name.
     *
     * @param branch the branch
     * @return the short name
     */
    private String getShortBranchName(Ref branch) {
        String name = branch.getName();
        if (name != null && name.startsWith("refs/heads/")) {
            return name.substring(11);
        }
        return name;
    }

    /**
     * Gets the unstaged changes.
     *
     * @return the unstaged changes
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public List<GitChangedFile> getUnstagedChanges() throws GitConnectorException {
        List<GitChangedFile> list = new ArrayList<GitChangedFile>();
        try {
            Status status = git.status()
                               .call();
            Set<String> missing = status.getMissing();
            for (String miss : missing) {
                GitChangedFile file = new GitChangedFile(miss, GitChangeType.Missing.ordinal());
                list.add(file);
            }
            Set<String> modified = status.getModified();
            for (String modify : modified) {
                GitChangedFile file = new GitChangedFile(modify, GitChangeType.Modified.ordinal());
                list.add(file);
            }
            Set<String> untracked = status.getUntracked();
            for (String untrack : untracked) {
                GitChangedFile file = new GitChangedFile(untrack, GitChangeType.Untracked.ordinal());
                list.add(file);
            }
        } catch (NoWorkTreeException | GitAPIException e) {
            throw new GitConnectorException(e);
        }
        return list;
    }

    /**
     * Gets the staged changes.
     *
     * @return the staged changes
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public List<GitChangedFile> getStagedChanges() throws GitConnectorException {
        List<GitChangedFile> list = new ArrayList<GitChangedFile>();
        try {
            Status status = git.status()
                               .call();
            Set<String> added = status.getAdded();
            for (String add : added) {
                GitChangedFile file = new GitChangedFile(add, GitChangeType.Added.ordinal());
                list.add(file);
            }
            Set<String> changed = status.getChanged();
            for (String change : changed) {
                GitChangedFile file = new GitChangedFile(change, GitChangeType.Changed.ordinal());
                list.add(file);
            }
            Set<String> removed = status.getRemoved();
            for (String remove : removed) {
                GitChangedFile file = new GitChangedFile(remove, GitChangeType.Removed.ordinal());
                list.add(file);
            }
        } catch (NoWorkTreeException | GitAPIException e) {
            throw new GitConnectorException(e);
        }
        return list;
    }

    /**
     * Gets the file content.
     *
     * @param path the path
     * @param revStr the rev str
     * @return the file content
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public String getFileContent(String path, String revStr) throws GitConnectorException {
        RevWalk revWalk = null;
        TreeWalk treeWalk = null;
        InputStream in = null;
        try {
            ObjectId lastCommitId = repository.resolve(revStr);
            if (lastCommitId == null) {
                // New and empty repository
                return null;
            }
            revWalk = new RevWalk(repository);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(path));
            if (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                in = loader.openStream();
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new GitConnectorException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(e.getMessage(), e);
                    } ;
                }
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
            if (revWalk != null) {
                revWalk.close();
            }
        }
        return null;
    }

    /**
     * Gets the history.
     *
     * @param path the path
     * @return the history
     * @throws GitConnectorException the git connector exception
     */
    @Override
    public List<GitCommitInfo> getHistory(String path) throws GitConnectorException {
        try {
            final List<GitCommitInfo> history = new ArrayList<GitCommitInfo>();
            if (repository.resolve(Constants.HEAD) != null) {
                LogCommand logCommand = git.log();
                if (path != null) {
                    logCommand.addPath(path);
                }
                Iterable<RevCommit> gitLog = logCommand.call();
                final SimpleDateFormat dtfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                gitLog.forEach(log -> {
                    PersonIdent personIdentity = log.getAuthorIdent();
                    dtfmt.setTimeZone(personIdentity.getTimeZone());

                    GitCommitInfo info = new GitCommitInfo();
                    info.setId(log.getId()
                                  .getName());
                    info.setAuthor(personIdentity.getName());
                    info.setEmailAddress(personIdentity.getEmailAddress());
                    info.setDateTime(dtfmt.format(personIdentity.getWhen()));
                    info.setMessage(log.getFullMessage());

                    history.add(info);
                });
            }
            return history;
        } catch (Exception e) {
            throw new GitConnectorException(e);
        }
    }

}
