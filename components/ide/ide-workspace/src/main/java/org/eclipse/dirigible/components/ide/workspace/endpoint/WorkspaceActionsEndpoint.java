/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.endpoint;

import static java.text.MessageFormat.format;

import java.net.URISyntaxException;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.service.ActionsService;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class WorkspaceActionsEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "workspace-actions")
public class WorkspaceActionsEndpoint {

    /** The Constant LOGGER. */
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceActionsEndpoint.class);

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The actions service. */
    @Autowired
    private ActionsService actionsService;

    /**
     * Creates the project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param action the action
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping("/{workspace}/{project}/{action}")
    public ResponseEntity<String> executeProjectAction(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("action") String action) throws URISyntaxException {
        if (!workspaceService.existsWorkspace(workspace)) {
            String error = format("Workspace {0} does not exist.", workspace);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        if (!workspaceService.existsProject(workspace, project)) {
            String error = format("Project {0} does not exist.", project);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        Project projectObject = workspaceService.getProject(workspace, project);
        File fileObject = projectObject.getFile("project.json");
        if (!fileObject.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, workspace + "/" + project + "/" + "project.json does not exist");
        }

        int result = actionsService.executeAction(workspace, project, action);
        if (result == 0) {
            logger.debug("Executed project action: " + action);
            return ResponseEntity.ok()
                                 .build();
        } else {
            logger.debug("Executed project action: " + action);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Project action execution failed with exit code: " + result);
        }


    }

    /**
     * Creates the project.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     */
    @GetMapping(value = "/{workspace}/{project}", produces = "application/json")
    public ResponseEntity<String> listProjectAction(@PathVariable("workspace") String workspace, @PathVariable("project") String project)
            throws URISyntaxException {
        if (!workspaceService.existsWorkspace(workspace)) {
            String error = format("Workspace {0} does not exist.", workspace);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        if (!workspaceService.existsProject(workspace, project)) {
            String error = format("Project {0} does not exist.", project);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        return ResponseEntity.ok(GsonHelper.toJson(actionsService.listActions(workspace, project)));
    }


}
