/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.endpoint;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.validation.Valid;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateParameters;
import org.eclipse.dirigible.components.ide.template.service.GenerationService;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Front facing REST service serving the Generation content.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "generate")
public class GenerationEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GenerationEndpoint.class);

    /** The processor. */
    @Autowired
    private GenerationService generationService;

    /** The processor. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Generate file.
     *
     * @param workspace the workspace
     * @param project tsyntax exception
     * @param path the path
     * @param parameters the parameters
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PostMapping("/file/{workspace}/{project}/{*path}")
    public ResponseEntity<URI> generateFile(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("path") String path, @Valid @RequestBody GenerationTemplateParameters parameters)
            throws URISyntaxException, IOException {

        if (!workspaceService.existsWorkspace(workspace)) {
            String error = format("Workspace {0} does not exist.", workspace);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        if (!workspaceService.existsProject(workspace, project)) {
            workspaceService.createProject(workspace, project);
        }

        File file = workspaceService.getFile(workspace, project, path);
        if (file.exists()) {
            String error = format("File {0} already exists in Project {1} in Workspace {2}.", path, project, workspace);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        generationService.generateFile(workspace, project, path, parameters);
        return ResponseEntity.created(workspaceService.getURI(workspace, project, path))
                             .build();
    }

}
