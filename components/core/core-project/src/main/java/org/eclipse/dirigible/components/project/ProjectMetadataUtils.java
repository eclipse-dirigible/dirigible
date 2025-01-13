/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.project;

import com.google.gson.Gson;

/**
 * The project's metadata utility class.
 */
public class ProjectMetadataUtils {

    /** The gson. */
    private static Gson gson = new Gson();

    /**
     * Serialize to json.
     *
     * @param projectMetadata the project metadata
     * @return the string
     */
    public static String toJson(ProjectMetadata projectMetadata) {
        String json = gson.toJson(projectMetadata);
        return json;
    }

    /**
     * Load from json.
     *
     * @param json the json
     * @return the project metadata
     */
    public static ProjectMetadata fromJson(String json) {
        ProjectMetadata projectMetadata = gson.fromJson(json, ProjectMetadata.class);
        return projectMetadata;
    }

}
