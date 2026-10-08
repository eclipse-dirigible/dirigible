/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.ConfigGroup;
import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.stereotype.Service;

/**
 * The Class ConfigurationsService.
 */
@Service
public class ConfigurationsService {

    /**
     * Find all.
     *
     * @return the list
     */
    public List<List<String>> findAll() {
        return findAll(null);
    }

    /**
     * Find all the keys of the given groups, in the shape of {@link #findAll()}.
     *
     * @param groups the groups to include; {@code null} or empty means every group
     * @return the list
     */
    public List<List<String>> findAll(Collection<ConfigGroup> groups) {

        Map<String, String> runtimeVariables = Configuration.getRuntimeVariables();
        Map<String, String> environmentVariables = Configuration.getEnvironmentVariables();
        Map<String, String> deploymentVariables = Configuration.getDeploymentVariables();
        Map<String, String> moduleVariables = Configuration.getModuleVariables();

        List<List<String>> result = new ArrayList<List<String>>();
        for (String parameter : Configuration.getConfigurationParameters()) {
            if (!ConfigDescriptors.inGroups(parameter, groups)) {
                continue;
            }
            List<String> row = new ArrayList<String>();
            row.add(parameter);
            row.add(runtimeVariables.get(parameter));
            row.add(environmentVariables.get(parameter));
            row.add(deploymentVariables.get(parameter));
            row.add(moduleVariables.get(parameter));
            result.add(row);
        }

        // String customDataSourcesList = Configuration.get("DIRIGIBLE_DATABASE_CUSTOM_DATASOURCES");
        // if ((customDataSourcesList != null) && !"".equals(customDataSourcesList)) {
        // StringTokenizer tokens = new StringTokenizer(customDataSourcesList, ",");
        // while (tokens.hasMoreTokens()) {
        // String name = tokens.nextToken();
        //
        // String parameter = name + "_DRIVER";
        // List<String> row = new ArrayList<String>();
        // row.add(parameter);
        // row.add(runtimeVariables.get(parameter));
        // row.add(environmentVariables.get(parameter));
        // row.add(deploymentVariables.get(parameter));
        // row.add(moduleVariables.get(parameter));
        // result.add(row);
        //
        // parameter = name + "_URL";
        // row = new ArrayList<String>();
        // row.add(parameter);
        // row.add(runtimeVariables.get(parameter));
        // row.add(environmentVariables.get(parameter));
        // row.add(deploymentVariables.get(parameter));
        // row.add(moduleVariables.get(parameter));
        // result.add(row);
        //
        // parameter = name + "_USERNAME";
        // row = new ArrayList<String>();
        // row.add(parameter);
        // row.add(runtimeVariables.get(parameter));
        // row.add(environmentVariables.get(parameter));
        // row.add(deploymentVariables.get(parameter));
        // row.add(moduleVariables.get(parameter));
        // result.add(row);
        //
        // parameter = name + "_PASSWORD";
        // row = new ArrayList<String>();
        // row.add(parameter);
        // row.add(runtimeVariables.get(parameter));
        // row.add(environmentVariables.get(parameter));
        // row.add(deploymentVariables.get(parameter));
        // row.add(moduleVariables.get(parameter));
        // result.add(row);
        // }
        // }

        return result;
    }

    /**
     * The configuration groups with their key counts.
     *
     * @return the groups, in display order
     */
    public List<ConfigDescriptors.GroupSummary> findGroups() {
        return ConfigDescriptors.groups();
    }

    /**
     * The described configuration keys, grouped, every value masked when sensitive.
     *
     * @param groups the groups to include; {@code null} or empty means every group
     * @param query a text the key or its displayed value must contain; {@code null} matches every key
     * @param onlySet whether to keep only the keys some source sets
     * @return the groups that have matching keys
     */
    public List<ConfigDescriptors.Group> findDescriptors(Collection<ConfigGroup> groups, String query, boolean onlySet) {
        return ConfigDescriptors.describe(groups, query, onlySet);
    }

}
