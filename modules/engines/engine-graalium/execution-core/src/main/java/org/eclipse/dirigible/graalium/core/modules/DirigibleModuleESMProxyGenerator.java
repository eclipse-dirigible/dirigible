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

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;

/**
 * The Class DirigibleModuleESMProxyGenerator.
 */
public class DirigibleModuleESMProxyGenerator {

    /** The Constant API_MODULES_JSON_PATH. */
    private static final String API_MODULES_JSON_PATH = "/extensions/modules.json";

    /** The Constant NAME_PLACEHOLDER. */
    private static final String NAME_PLACEHOLDER = "<name_placeholder>";

    /** The Constant PATH_PLACEHOLDER. */
    private static final String PATH_PLACEHOLDER = "<path_placeholder>";

    /** The Constant NAMES_LIST_PLACEHOLDER. */
    private static final String NAMES_LIST_PLACEHOLDER = "<names_list_placeholder>";

    /** The Constant DEFAULT_EXPORT_PATTERN. */
    private static final String DEFAULT_EXPORT_PATTERN = "export default { " + NAMES_LIST_PLACEHOLDER + " }";

    /** The Constant EXPORT_PATTERN. */
    private static final String EXPORT_PATTERN = "export const " + NAME_PLACEHOLDER + " = dirigibleRequire('" + PATH_PLACEHOLDER + "');";

    /** The Constant DECONSTRUCTED_EXPORT_PATTERN. */
    private static final String DECONSTRUCTED_EXPORT_PATTERN =
            "export const { " + NAMES_LIST_PLACEHOLDER + " } = dirigibleRequire('" + PATH_PLACEHOLDER + "');";

    /** The gson. */
    private final Gson gson = new Gson();

    /** The dirigible source provider. */
    private JavascriptSourceProvider dirigibleSourceProvider;

    /**
     * Instantiates a new dirigible module ESM proxy generator.
     *
     * @param dirigibleSourceProvider the dirigible source provider
     */
    public DirigibleModuleESMProxyGenerator(JavascriptSourceProvider dirigibleSourceProvider) {
        this.dirigibleSourceProvider = dirigibleSourceProvider;
    }

    /**
     * Gets the source provider.
     *
     * @return the source provider
     */
    public JavascriptSourceProvider getSourceProvider() {
        return dirigibleSourceProvider;
    }

    /**
     * Generate.
     *
     * @param path the path
     * @param apiVersion the api version
     * @return the string
     */
    public String generate(String path, String apiVersion) {
        DirigibleModule[] modules = readApiModuleJson(path + API_MODULES_JSON_PATH);
        StringBuilder source = new StringBuilder();
        StringBuilder moduleNames = new StringBuilder();

        for (DirigibleModule module : modules) {
            if (module.isPackageDescription() || module.getShouldBeUnexposedToESM()) {
                continue;
            }

            if (shouldDeconstructModule(module)) {
                writeDeconstructedExportedCJSModule(source, module, apiVersion);
            } else {
                writeExportedCJSModule(source, module, moduleNames, apiVersion);
            }
        }

        if (moduleNames.length() > 0) {
            moduleNames.setLength(moduleNames.length() - 1);
        }

        source.append(DEFAULT_EXPORT_PATTERN.replace(NAMES_LIST_PLACEHOLDER, moduleNames.toString()));
        source.append(System.lineSeparator());
        return source.toString();
    }

    /**
     * Should deconstruct module.
     *
     * @param module the module
     * @return true, if successful
     */
    private static boolean shouldDeconstructModule(DirigibleModule module) {
        List<String> deconstructs = module.getDeconstruct();
        return deconstructs != null && !deconstructs.isEmpty();
    }

    /**
     * Write exported CJS module.
     *
     * @param sourceBuilder the source builder
     * @param module the module
     * @param moduleNames the module names
     * @param apiVersion the api version
     */
    private static void writeExportedCJSModule(StringBuilder sourceBuilder, DirigibleModule module, StringBuilder moduleNames,
            String apiVersion) {
        String api = module.getApi();
        String dir = resolvePath(module, apiVersion);

        sourceBuilder.append(EXPORT_PATTERN.replace(NAME_PLACEHOLDER, api)
                                           .replace(PATH_PLACEHOLDER, dir));
        sourceBuilder.append(System.lineSeparator());
        moduleNames.append(api);
        moduleNames.append(',');
    }

    /**
     * Write deconstructed exported CJS module.
     *
     * @param sourceBuilder the source builder
     * @param module the module
     * @param apiVersion the api version
     */
    private static void writeDeconstructedExportedCJSModule(StringBuilder sourceBuilder, DirigibleModule module, String apiVersion) {
        List<String> deconstructs = module.getDeconstruct();
        String dir = resolvePath(module, apiVersion);

        sourceBuilder.append(DECONSTRUCTED_EXPORT_PATTERN.replace(NAMES_LIST_PLACEHOLDER, String.join(", ", deconstructs))
                                                         .replace(PATH_PLACEHOLDER, dir));
        sourceBuilder.append(System.lineSeparator());
    }

    /**
     * Read api module json.
     *
     * @param path the path
     * @return the dirigible module[]
     */
    private DirigibleModule[] readApiModuleJson(String path) {
        String apiModuleJson = getSourceProvider().getSource(path);
        return gson.fromJson(apiModuleJson, DirigibleModule[].class);
    }

    /**
     * Resolve path.
     *
     * @param module the module
     * @param apiVersion the api version
     * @return the string
     */
    private static String resolvePath(DirigibleModule module, String apiVersion) {
        if (apiVersion.isEmpty()) {
            return module.getPathDefault();
        }

        List<String> foundPaths = Arrays.stream(module.getVersionedPaths())
                                        .filter(p -> p.contains(apiVersion))
                                        .collect(Collectors.toList());

        if (foundPaths.size() != 1) {
            StringBuilder message = new StringBuilder();
            message.append("Searching for single api path containing '");
            message.append(apiVersion);
            message.append("' but found: ");
            for (String foundPath : foundPaths) {
                message.append("'");
                message.append(foundPath);
                message.append("' ");
            }
            throw new RuntimeException(message.toString());
        }

        return foundPaths.get(0);
    }
}
