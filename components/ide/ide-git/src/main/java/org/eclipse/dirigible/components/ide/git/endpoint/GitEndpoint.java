/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.endpoint;

import static java.text.MessageFormat.format;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.git.domain.GitCommitInfo;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitUrlInput;
import org.eclipse.dirigible.components.ide.git.domain.GitUrlOutput;
import org.eclipse.dirigible.components.ide.git.model.BaseGitModel;
import org.eclipse.dirigible.components.ide.git.model.GitCheckoutModel;
import org.eclipse.dirigible.components.ide.git.model.GitCloneModel;
import org.eclipse.dirigible.components.ide.git.model.GitDiffModel;
import org.eclipse.dirigible.components.ide.git.model.GitProjectChangedFiles;
import org.eclipse.dirigible.components.ide.git.model.GitProjectLocalBranches;
import org.eclipse.dirigible.components.ide.git.model.GitProjectRemoteBranches;
import org.eclipse.dirigible.components.ide.git.model.GitPullModel;
import org.eclipse.dirigible.components.ide.git.model.GitPushModel;
import org.eclipse.dirigible.components.ide.git.model.GitResetModel;
import org.eclipse.dirigible.components.ide.git.model.GitShareModel;
import org.eclipse.dirigible.components.ide.git.model.GitUpdateDependenciesModel;
import org.eclipse.dirigible.components.ide.git.project.ProjectOriginUrls;
import org.eclipse.dirigible.components.ide.git.service.GitService;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

/**
 * Front facing REST service serving the Git commands.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "git/{workspace}")
public class GitEndpoint {

    /** The git service. */
    private final GitService gitService;

    /**
     * Instantiates a new git endpoint.
     *
     * @param gitService the git service
     */
    @Autowired
    public GitEndpoint(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * Gets the git service.
     *
     * @return the git service
     */
    public GitService getGitService() {
        return gitService;
    }

    /**
     * Clone repository.
     *
     * @param workspace the workspace
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/clone"}, produces = {"application/json"})
    public ResponseEntity<?> cloneRepository(@PathVariable("workspace") String workspace, @Valid @RequestBody GitCloneModel model)
            throws GitConnectorException {
        gitService.clone(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Pull projects.
     *
     * @param workspace the workspace
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/pull"}, produces = {"application/json"})
    public ResponseEntity<?> pullProjects(@PathVariable("workspace") String workspace, @Valid @RequestBody GitPullModel model)
            throws GitConnectorException {
        gitService.pull(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Pull project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/pull"}, produces = {"application/json"})
    public ResponseEntity<?> pullProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitPullModel model) throws GitConnectorException {
        model.setProjects(Arrays.asList(project));
        gitService.pull(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Push projects.
     *
     * @param workspace the workspace
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/push"}, produces = {"application/json"})
    public ResponseEntity<?> pushProjects(@PathVariable("workspace") String workspace, @Valid @RequestBody GitPushModel model)
            throws GitConnectorException {
        gitService.push(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Push project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/push"}, produces = {"application/json"})
    public ResponseEntity<?> pushProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitPushModel model) throws GitConnectorException {
        model.setProjects(Arrays.asList(project));
        gitService.push(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Reset projects.
     *
     * @param workspace the workspace
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/reset"}, produces = {"application/json"})
    public ResponseEntity<?> resetProjects(@PathVariable("workspace") String workspace, @Valid @RequestBody GitResetModel model)
            throws GitConnectorException {
        gitService.reset(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Reset project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/reset"}, produces = {"application/json"})
    public ResponseEntity<?> resetProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitResetModel model) throws GitConnectorException {
        model.setProjects(Arrays.asList(project));
        gitService.reset(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Reset project.
     *
     * @param workspace the workspace
     * @param repositoryName the project
     * @param unpublish the unpublish
     * @return the response
     * @throws GitConnectorException in case of exception
     */
    @DeleteMapping(value = {"/{repositoryName}/delete"}, produces = {"application/json"})
    public ResponseEntity<?> deleteGitRepository(@PathVariable("workspace") String workspace,
            @PathVariable("repositoryName") String repositoryName, @Nullable @RequestParam("unpublish") boolean unpublish)
            throws GitConnectorException {
        gitService.delete(workspace, repositoryName, unpublish);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Share project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/share"}, produces = {"application/json"})
    public ResponseEntity<?> shareProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitShareModel model) throws GitConnectorException {
        model.setProject(project);
        gitService.share(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Checkout project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/checkout"}, produces = {"application/json"})
    public ResponseEntity<?> checkoutBranch(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitCheckoutModel model) throws GitConnectorException {
        model.setProject(project);
        gitService.checkout(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Commit project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/commit"}, produces = {"application/json"})
    public ResponseEntity<?> commitProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitPushModel model) throws GitConnectorException {
        model.setProjects(Arrays.asList(project));
        gitService.commit(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Update projects dependencies.
     *
     * @param workspace the workspace
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/uppdate-dependencies"}, produces = {"application/json"})
    public ResponseEntity<?> updateProjectsDependencies(@PathVariable("workspace") String workspace,
            @Valid @RequestBody GitUpdateDependenciesModel model) throws GitConnectorException {
        gitService.updateDependencies(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Update project dependencies.
     *
     * @param workspace the workspace
     * @param project the project
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/uppdate-dependencies"}, produces = {"application/json"})
    public ResponseEntity<?> updateProjectDependencies(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitUpdateDependenciesModel model) throws GitConnectorException {
        model.setProjects(Arrays.asList(project));
        gitService.updateDependencies(workspace, model);
        return ResponseEntity.ok()
                             .build();
    }



    /**
     * Get local branches.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @GetMapping(value = {"/{project}/branches/local"}, produces = {"application/json"})
    public ResponseEntity<?> getProjectLocalBranches(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        GitProjectLocalBranches gitProjectBranches = gitService.getLocalBranches(workspace, project);
        return ResponseEntity.ok(GsonHelper.toJson(gitProjectBranches));
    }

    /**
     * Create a local branch.
     *
     * @param workspace the workspace
     * @param project the project
     * @param branch the branch
     * @return the response
     * @throws GitConnectorException the git connector exception
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @PostMapping(value = {"/{project}/branches/local/{branch}"}, consumes = {"application/json"})
    public ResponseEntity<?> createLocalBranch(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("branch") String branch)
            throws GitConnectorException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        gitService.createLocalBranch(workspace, project, branch);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Delete a local branch.
     *
     * @param workspace the workspace
     * @param project the project
     * @param branch the branch
     * @return the response
     * @throws GitConnectorException the git connector exception
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @DeleteMapping(value = {"/{project}/branches/local/{branch}"}, consumes = {"application/json"})
    public ResponseEntity<?> deleteLocalBranch(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("branch") String branch)
            throws GitConnectorException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        gitService.deleteLocalBranch(workspace, project, branch);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Get remote branches.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @GetMapping(value = {"/{project}/branches/remote"}, produces = {"application/json"})
    public ResponseEntity<?> getProjectRemoteBranches(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        GitProjectRemoteBranches gitProjectBranches = gitService.getRemoteBranches(workspace, project);
        return ResponseEntity.ok(GsonHelper.toJson(gitProjectBranches));
    }

    /**
     * Create a remote branch.
     *
     * @param workspace the workspace
     * @param project the project
     * @param branch the branch
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @PostMapping(value = {"/{project}/branches/remote/{branch}"}, consumes = {"application/json"})
    public ResponseEntity<?> createRemoteBranch(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("branch") String branch, @Valid @RequestBody BaseGitModel model)
            throws GitConnectorException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        gitService.createRemoteBranch(workspace, project, branch, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Delete a remote branch.
     *
     * @param workspace the workspace
     * @param project the project
     * @param branch the branch
     * @param model the model
     * @return the response
     * @throws GitConnectorException the git connector exception
     * @throws RefAlreadyExistsException the ref already exists exception
     * @throws RefNotFoundException the ref not found exception
     * @throws InvalidRefNameException the invalid ref name exception
     * @throws GitAPIException the git API exception
     */
    @DeleteMapping(value = {"/{project}/branches/remote/{branch}"}, consumes = {"application/json"})
    public ResponseEntity<?> deleteRemoteBranch(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("branch") String branch, @Valid @RequestBody BaseGitModel model)
            throws GitConnectorException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, GitAPIException {
        gitService.deleteRemoteBranch(workspace, project, branch, model);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Gets the workspace.
     *
     * @param workspace the workspace
     * @return the workspace
     */
    @GetMapping(value = {"/"}, produces = {"application/json"})
    public ResponseEntity<?> getGitRepositories(@PathVariable("workspace") String workspace) {
        String error = format("Workspace {0} does not exist.", workspace);
        if (!gitService.existsWorkspace(workspace)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        Workspace workspaceObject = gitService.getWorkspace(workspace);
        if (!workspaceObject.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }
        return ResponseEntity.ok(gitService.renderGitRepositories(UserFacade.getName(), workspace));
    }

    /**
     * Get unstaged files.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @GetMapping(value = {"/{project}"}, produces = {"application/json"})
    public ResponseEntity<?> getProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        Workspace workspaceObject = gitService.getWorkspace(workspace);
        return ResponseEntity.ok(gitService.renderWorkspaceProject(workspaceObject, project));
    }

    /**
     * Get unstaged files.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @GetMapping(value = {"/{project}/unstaged"}, produces = {"application/json"})
    public ResponseEntity<?> getProjectUnstagedFiles(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        GitProjectChangedFiles gitProjectFiles = gitService.getUnstagedFiles(workspace, project);
        return ResponseEntity.ok(GsonHelper.toJson(gitProjectFiles));
    }

    /**
     * Get staged files.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @GetMapping(value = {"/{project}/staged"}, produces = {"application/json"})
    public ResponseEntity<?> getProjectStagedFiles(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        GitProjectChangedFiles gitProjectFiles = gitService.getStagedFiles(workspace, project);
        return ResponseEntity.ok(GsonHelper.toJson(gitProjectFiles));
    }

    /**
     * Add file to index.
     *
     * @param workspace the workspace
     * @param project the project
     * @param paths the paths to be added
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/add"}, produces = {"application/json"})
    public ResponseEntity<?> addFileToIndex(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody String paths) throws GitConnectorException {
        if (paths == null || "".equals(paths)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .build();
        }
        gitService.addFileToIndex(workspace, project, paths);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Revert file to index.
     *
     * @param workspace the workspace
     * @param project the project
     * @param paths the paths to be added
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/revert"}, produces = {"application/json"})
    public ResponseEntity<?> revertToHeadRevision(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody String paths) throws GitConnectorException {
        if (paths == null || "".equals(paths)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .build();
        }
        gitService.revertToHeadRevision(workspace, project, paths);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Remove file from index.
     *
     * @param workspace the workspace
     * @param project the project
     * @param paths the paths to be added
     * @return the response
     * @throws GitConnectorException the git connector exception
     */
    @PostMapping(value = {"/{project}/remove"}, produces = {"application/json"})
    public ResponseEntity<?> removeFileFromIndex(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody String paths) throws GitConnectorException {
        if (paths == null || "".equals(paths)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .build();
        }
        gitService.removeFileFromIndex(workspace, project, paths);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Get remote origin URLs.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws GitConnectorException in case of exception
     */
    @GetMapping(value = {"/{project}/origin-urls"}, produces = {"application/json"})
    public ResponseEntity<?> getOriginUrl(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws GitConnectorException {
        ProjectOriginUrls originUrls = gitService.getOriginUrls(workspace, project);
        if (originUrls != null) {
            return ResponseEntity.ok(originUrls);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a git project");
    }

    /**
     * Update remote origin fetch URL.
     *
     * @param workspace the workspace
     * @param project the project
     * @param input the input
     * @return the response
     * @throws GitConnectorException Git Connector Exception
     * @throws GitAPIException Git API Exception
     * @throws URISyntaxException URL with wrong format provided
     */
    @PostMapping(value = {"/{project}/fetch-url"}, produces = {"application/json"})
    public ResponseEntity<?> setFetchUrl(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitUrlInput input) throws GitConnectorException, GitAPIException, URISyntaxException {
        String newurl = input.getUrl();
        gitService.setFetchUrl(workspace, project, newurl);
        GitUrlOutput res = new GitUrlOutput();
        res.setStatus("success");
        res.setUrl(newurl);
        return ResponseEntity.ok(res);
    }

    /**
     * Update remote origin push URL.
     *
     * @param workspace the workspace
     * @param project the project
     * @param input the input
     * @return the response
     * @throws GitConnectorException Git Connector Exception
     * @throws GitAPIException Git API Exception
     * @throws URISyntaxException URL with wrong format provided
     */
    @PostMapping(value = {"/{project}/push-url"}, produces = {"application/json"})
    public ResponseEntity<?> setPushUrl(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Valid @RequestBody GitUrlInput input) throws GitConnectorException, GitAPIException, URISyntaxException {
        String newurl = input.getUrl();
        gitService.setPushUrl(workspace, project, newurl);
        GitUrlOutput res = new GitUrlOutput();
        res.setStatus("success");
        res.setUrl(newurl);
        return ResponseEntity.ok(res);
    }

    /**
     * Get file diff.
     *
     * @param workspace the workspace
     * @param repositoryName the project
     * @param path the path
     * @return the response
     * @throws GitConnectorException in case of exception
     */
    @GetMapping(value = {"/{repositoryName}/diff"}, produces = {"application/json"})
    public ResponseEntity<?> getFileDiff(@PathVariable("workspace") String workspace, @PathVariable("repositoryName") String repositoryName,
            @Nullable @RequestParam("path") String path) throws GitConnectorException {
        if (path == null || "".equals(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .build();
        }
        GitDiffModel diff = gitService.getFileDiff(workspace, repositoryName, path);
        if (diff != null) {
            return ResponseEntity.ok(GsonHelper.toJson(diff));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a git project");
    }

    /**
     * Get file history.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the response
     * @throws GitConnectorException in case of exception
     */
    @GetMapping(value = {"/{project}/history"}, produces = {"application/json"})
    public ResponseEntity<?> getHistory(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @Nullable @RequestParam("path") String path) throws GitConnectorException {
        List<GitCommitInfo> history = gitService.getHistory(workspace, project, path);
        if (history != null) {
            return ResponseEntity.ok(GsonHelper.toJson(history));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a git project");
    }

    /**
     * Import projects.
     *
     * @param workspace the workspace
     * @param repository the project
     * @return the response
     * @throws GitConnectorException in case of exception
     */
    @PostMapping(value = {"/{repository}/import"}, produces = {"application/json"})
    public ResponseEntity<?> importProjects(@PathVariable("workspace") String workspace, @PathVariable("repository") String repository)
            throws GitConnectorException {
        gitService.importProjects(workspace, repository);
        return ResponseEntity.ok()
                             .build();
    }

}
