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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class PublisherEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "publisher")
public class PublisherEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WorkspacesEndpoint.class);

    /** The publisher service. */
    @Autowired
    private PublisherService publisherService;

    /** The websocket service. */
    @Autowired
    private WorkspaceService workspaceService;


    /**
     * Publish.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping("{workspace}/{project}/{*path}")
    public ResponseEntity<?> publish(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("path") String path) throws URISyntaxException {

        if (Boolean.parseBoolean(Configuration.get(PublisherService.DIRIGIBLE_PUBLISH_DISABLED, Boolean.FALSE.toString()))) {
            return ResponseEntity.ok()
                                 .build();
        }

        if (!workspaceService.existsWorkspace(workspace)) {
            String error = format("Workspace {0} does not exist.", workspace);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        publisherService.publish(workspace, project, path);

        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Unpublish.
     *
     * @param workspace the workspace
     * @param path the path
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping("{workspace}/{*path}")
    public ResponseEntity<?> unpublish(@PathVariable("workspace") String workspace, @PathVariable("path") String path)
            throws URISyntaxException {

        if (Boolean.parseBoolean(Configuration.get(PublisherService.DIRIGIBLE_PUBLISH_DISABLED, Boolean.FALSE.toString()))) {
            return ResponseEntity.ok()
                                 .build();
        }

        if (!workspaceService.existsWorkspace(workspace)) {
            String error = format("Workspace {0} does not exist.", workspace);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        publisherService.unpublish(path);

        return ResponseEntity.ok()
                             .build();
    }

}
