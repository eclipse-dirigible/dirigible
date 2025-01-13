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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.validation.Valid;

import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.json.FileDescriptor;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class WorkspaceSearchEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "workspace-search")
public class WorkspaceSearchEndpoint {

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Search.
     *
     * @param workspace the workspace
     * @param term the term
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws DecoderException the decoder exception
     */
    @PostMapping("{workspace}")
    public ResponseEntity<List<FileDescriptor>> find(@PathVariable("workspace") String workspace, @Valid @RequestBody String term)
            throws URISyntaxException, UnsupportedEncodingException, DecoderException {
        if ((term == null) || term.isEmpty()) {
            ResponseEntity.ok("No search term provided in the request body");
        }

        List<File> files = workspaceService.search(workspace, term);
        return ResponseEntity.ok(workspaceService.renderFileDescriptions(files));
    }

}
