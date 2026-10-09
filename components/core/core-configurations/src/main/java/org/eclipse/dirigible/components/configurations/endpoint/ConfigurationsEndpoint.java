/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.ConfigGroup;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.configurations.service.ConfigurationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * The Class ConfigurationsEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "configurations")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class ConfigurationsEndpoint extends BaseEndpoint {


    /** The configurations service. */
    private final ConfigurationsService configurationsService;

    /**
     * Instantiates a new configurations endpoint.
     *
     * @param configurationsService the configurations service
     */
    @Autowired
    public ConfigurationsEndpoint(ConfigurationsService configurationsService) {
        this.configurationsService = configurationsService;
    }

    /**
     * Find all: one {@code [key, runtime, environment, deployment, module]} row per catalogued key.
     *
     * @param group the group ids to keep (repeatable or comma-separated); all keys when absent
     * @return the response entity
     */
    @GetMapping
    public ResponseEntity<List<List<String>>> findAll(@RequestParam(name = "group", required = false) List<String> group) {
        if (group == null || group.isEmpty()) {
            return ResponseEntity.ok(configurationsService.findAll());
        }
        return ResponseEntity.ok(configurationsService.findAll(groups(group)));
    }

    /**
     * The configuration groups, in display order, with the number of keys and of set keys in each.
     *
     * @return the response entity
     */
    @GetMapping("/groups")
    public ResponseEntity<List<ConfigDescriptors.GroupSummary>> findGroups() {
        return ResponseEntity.ok(configurationsService.findGroups());
    }

    /**
     * The described configuration keys, grouped: default, effective value, source and the value of each
     * source. A sensitive value is masked in every field.
     *
     * @param group the group ids to keep (repeatable or comma-separated); all groups when absent
     * @param q a text the key or its displayed value must contain
     * @param onlySet whether to keep only the keys some source sets
     * @return the response entity
     */
    @GetMapping("/descriptors")
    public ResponseEntity<List<ConfigDescriptors.Group>> findDescriptors(@RequestParam(name = "group", required = false) List<String> group,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "onlySet", required = false, defaultValue = "false") boolean onlySet) {
        return ResponseEntity.ok(configurationsService.findDescriptors(groups(group), q, onlySet));
    }

    /**
     * Parses the requested group ids. Asking only for groups that do not exist is a client error, not a
     * request for every group.
     *
     * @param ids the requested ids, may be {@code null}
     * @return the groups, empty when none was requested
     */
    static Set<ConfigGroup> groups(List<String> ids) {
        Set<ConfigGroup> groups = ConfigDescriptors.parseGroups(ids);
        if (groups.isEmpty() && ids != null && ids.stream()
                                                  .anyMatch(id -> id != null && !id.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown configuration group: " + ids);
        }
        return groups;
    }

}
