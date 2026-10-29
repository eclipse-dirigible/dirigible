/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.deprecation;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * Tells the IDE which published projects use server-side JavaScript or TypeScript, so its shell can
 * show the deprecation banner while there are any, and what is left to port of each.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "javascript-deprecation")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
class ServerSideScriptDeprecationEndpoint {

    private final ServerSideScriptDeprecation deprecation;

    ServerSideScriptDeprecationEndpoint(ServerSideScriptDeprecation deprecation) {
        this.deprecation = deprecation;
    }

    /**
     * The projects with a deprecation entry in the Problems view, and the migration guide.
     *
     * @return the notice, with no projects on an instance where none uses server-side scripts
     */
    @GetMapping
    ResponseEntity<Notice> getNotice() {
        return ResponseEntity.ok(new Notice(deprecation.flaggedProjects(), ServerSideScriptDeprecation.MIGRATION_GUIDE));
    }

    /**
     * What is left to port of a published project: its server-side scripts file by file, with the
     * constructs each one uses, and the browser code that calls them.
     *
     * @param project the project name
     * @return the inventory
     */
    @GetMapping("/projects/{project}")
    ResponseEntity<ServerSideScriptInventory.Inventory> getInventory(@PathVariable("project") String project) {
        if (project.contains("..") || project.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[" + project + "] is not a project name");
        }
        return deprecation.inventory(project)
                          .map(ResponseEntity::ok)
                          .orElseThrow(
                                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project [" + project + "] is not published"));
    }

    /**
     * The deprecation notice.
     *
     * @param projects the projects that use server-side JavaScript or TypeScript
     * @param guide the migration guide
     */
    record Notice(List<String> projects, String guide) {
    }
}
