/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.exposure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.repository.api.IRepository;

/**
 * Web Expose Manager class .
 */
public class ExposeManager {

    /** The Constant EXPOSABLE_PROJECTS. */
    private static final Map<String, String[]> EXPOSABLE_PROJECTS = Collections.synchronizedMap(new HashMap<String, String[]>());

    /**
     * Whether the project name is known.
     *
     * @param name the project name
     * @return true if it is known
     */
    public static boolean existExposableProject(String name) {
        return EXPOSABLE_PROJECTS.keySet()
                                 .contains(name);
    }

    /**
     * Register the project as known for exposures.
     *
     * @param name the project name
     * @param paths the paths within the project to be exposed
     */
    public static void registerExposableProject(String name, String[] paths) {
        EXPOSABLE_PROJECTS.put(name, paths);
    }

    /**
     * Whether a path can be accessed.
     *
     * @param path the URI
     * @return true if it can
     */
    public static boolean isPathExposed(String path) {

        // path must not start with '/'
        if (path.startsWith(IRepository.SEPARATOR)) {
            path = path.substring(1);
        }

        // path should have at least two segments
        int index = path.indexOf(IRepository.SEPARATOR);
        if (index > 0) {

            // the project name segment
            String name = path.substring(0, index);

            if (existExposableProject(name)) {

                // project is known, so to be checked
                String[] exposedPaths = EXPOSABLE_PROJECTS.get(name);
                if (exposedPaths.length == 0) {
                    // no expose elements present
                    return true;
                }
                for (String exposedPath : exposedPaths) {

                    // normalize the exposed path value
                    if (!exposedPath.startsWith(IRepository.SEPARATOR)) {
                        exposedPath = IRepository.SEPARATOR + exposedPath;
                    }

                    // path matches of one of the exposed path values, so it is allowed
                    if (path.startsWith(name + exposedPath)) {
                        return true;
                    }
                }

                // path does not match any of the registered exposed values of a known project, so it is not allowed
                return false;
            }
        } else {
            // it is a single segment only
            return true;
        }

        // unknown project, so expose all by default
        return true;
    }

    /**
     * Getter for all the registered exposable projects.
     *
     * @return all the registered projects
     */
    public static Set<String> listRegisteredProjects() {
        return new HashSet<String>(EXPOSABLE_PROJECTS.keySet());
    }

    /**
     * Unregister a project.
     *
     * @param registeredProject the registered project
     */
    public static void unregisterProject(String registeredProject) {
        EXPOSABLE_PROJECTS.remove(registeredProject);
    }

}
