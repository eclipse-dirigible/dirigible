/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.di.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.engine.di.service.ComponentService;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComponentRegister implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ComponentRegister.class);

    public static final Map<String, Value> COMPONENTS = new HashMap<String, Value>();

    /** The instance. */
    private static ComponentRegister INSTANCE;

    private ComponentService componentService;

    @Autowired
    public ComponentRegister(ComponentService componentService) {
        this.componentService = componentService;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the.
     *
     * @return the javascript service
     */
    public static ComponentRegister get() {
        return INSTANCE;
    }

    public ComponentService getComponentService() {
        return componentService;
    }


    public static void addComponent(String location) {
        processComponentFile(location, true);
    }

    public static void removeComponent(String location) {
        processComponentFile(location, false);
    }

    private static void processComponentFile(String location, boolean add) {
        try {
            ParsedPath path = parseFilePath(location);
            String contextId = "default"; // path.projectName; // use projectName or moduleName as context

            String filePath = path.filePath;
            if (filePath.endsWith(".ts")) {
                filePath = filePath.substring(0, filePath.length() - 3) + ".js";
            }
            Value moduleValue = ComponentRegister.get()
                                                 .getComponentService()
                                                 .executeJavaScript(path.projectName, filePath);

            Value componentValue = moduleValue.getMember(moduleValue.getMemberKeys()
                                                                    .iterator()
                                                                    .next());

            if (componentValue.hasMember("__component_name")) {
                String name = componentValue.getMember("__component_name")
                                            .asString();

                Value injections = null;
                if (componentValue.hasMember("__injections_map")) {
                    injections = componentValue.getMember("__injections_map");
                }

                ComponentContext context = ComponentContextRegistry.getContext(contextId);

                context.registerComponentMetadata(name, injections);

                if (add) {
                    Value instance = componentValue.newInstance();
                    context.registerComponent(name, instance, injections);
                    logger.info("Registered component [{}] in context [{}]", name, contextId);
                } else {
                    context.unregisterComponent(name);
                    logger.info("Unregistered component [{}] from context [{}]", name, contextId);
                }
            } else {
                logger.warn("Class does not have @Component metadata: {}", location);
            }
        } catch (PolyglotException e) {
            logger.error("Error evaluating script [{}]: {}", location, e.getMessage());
        }
    }

    // // on project undeploy:
    // ComponentContextRegistry.removeContext(projectName);
    //
    // // on server shutdown:
    // ComponentContextRegistry.clearAll();



    public record ParsedPath(String projectName, String filePath) {
    }

    public static ParsedPath parseFilePath(String path) {
        if (path == null || path.trim()
                                .isEmpty()) {
            return new ParsedPath("", "");
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.isEmpty()) {
            return new ParsedPath("", "");
        }
        String[] segments = normalizedPath.split("/", -1);
        String projectName = segments[0];
        if (segments.length == 1) {
            return new ParsedPath(projectName, "");
        }

        String[] filePathSegments = Arrays.copyOfRange(segments, 1, segments.length);
        String filePath = String.join("/", filePathSegments);

        return new ParsedPath(projectName, filePath);
    }
}
