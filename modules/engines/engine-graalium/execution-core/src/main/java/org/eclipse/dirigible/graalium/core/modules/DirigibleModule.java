/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.modules;

import java.util.List;
import java.util.Objects;

/**
 * The Class DirigibleModule.
 */
public class DirigibleModule {

    /** The name. */
    private final String name;

    /** The api. */
    private final String api;

    /** The versioned paths. */
    private final String[] versionedPaths;

    /** The path default. */
    private final String pathDefault;

    /** The is package description. */
    private final boolean isPackageDescription;

    /** The should be unexposed to ESM. */
    private final Boolean shouldBeUnexposedToESM;

    /** The CJS exported members to re-export in a deconstructed manner. */
    private final List<String> deconstruct;

    /**
     * Instantiates a new dirigible module.
     *
     * @param name the name
     * @param api the api
     * @param versionedPaths the versioned paths
     * @param pathDefault the path default
     * @param isPackageDescription the is package description
     * @param shouldBeUnexposedToESM the should be unexposed to ESM
     * @param deconstruct the CJS exported members to re-export in a deconstructed manner
     */
    DirigibleModule(String name, String api, String[] versionedPaths, String pathDefault, boolean isPackageDescription,
            Boolean shouldBeUnexposedToESM, List<String> deconstruct) {
        this.name = name;
        this.api = api;
        this.versionedPaths = versionedPaths;
        this.pathDefault = pathDefault;
        this.isPackageDescription = isPackageDescription;
        this.shouldBeUnexposedToESM = shouldBeUnexposedToESM;
        this.deconstruct = deconstruct;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName() {
        return name;
    }

    /**
     * Gets the api.
     *
     * @return the api
     */
    public String getApi() {
        return api;
    }

    /**
     * Gets the versioned paths.
     *
     * @return the versioned paths
     */
    public String[] getVersionedPaths() {
        return versionedPaths;
    }

    /**
     * Gets the path default.
     *
     * @return the path default
     */
    public String getPathDefault() {
        return pathDefault;
    }

    /**
     * Gets the should be unexposed to ESM.
     *
     * @return the should be unexposed to ESM
     */
    public boolean getShouldBeUnexposedToESM() {
        return Objects.requireNonNullElse(shouldBeUnexposedToESM, false);
    }

    /**
     * Checks if is package description.
     *
     * @return true, if is package description
     */
    public boolean isPackageDescription() {
        return isPackageDescription;
    }

    /**
     * Gets the CJS exported members to re-export in a deconstructed manner.
     *
     * @return names of the exported members
     */
    public List<String> getDeconstruct() {
        return deconstruct;
    }
}
