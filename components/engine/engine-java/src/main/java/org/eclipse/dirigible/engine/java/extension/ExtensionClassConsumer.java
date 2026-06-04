/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.extension;

import org.eclipse.dirigible.components.extensions.domain.Extension;
import org.eclipse.dirigible.components.extensions.service.ExtensionService;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client classes annotated with
 * {@link org.eclipse.dirigible.engine.java.annotations.Extension @Extension} as Dirigible extension
 * contributions persisted via {@link ExtensionService}.
 *
 * <p>
 * The stored {@link Extension} record uses the class FQN as the module path so callers that query
 * the extension point (e.g. via {@code ExtensionService.findByExtensionPoint}) can identify and
 * instantiate the Java class through {@code JavaClassRegistry} or {@code BeanProvider}.
 *
 * <p>
 * The location is set to the source {@code .java} file path (following the same convention as
 * {@code JavaControllerOpenApiPublisher}) so the synchronizer's orphan-cleanup pass does not remove
 * the artefact while the source exists.
 */
@Component
@Order(700)
public class ExtensionClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionClassConsumer.class);

    private final ExtensionService extensionService;

    @Autowired
    public ExtensionClassConsumer(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(org.eclipse.dirigible.engine.java.annotations.Extension.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        org.eclipse.dirigible.engine.java.annotations.Extension ann = info.type()
                                                                          .getAnnotation(
                                                                                  org.eclipse.dirigible.engine.java.annotations.Extension.class);

        String location = locationOf(info.project(), info.fqn());
        try {
            String key = Extension.ARTEFACT_TYPE + ":" + location + ":" + ann.name();
            Extension existing = extensionService.findByKey(key);
            Extension extension = existing != null ? existing : new Extension(location, ann.name(), null, ann.to(), info.fqn(), null);
            extension.setExtensionPoint(ann.to());
            extension.setModule(info.fqn());
            extensionService.save(extension);
            LOGGER.info("Java @Extension [{}] registered for extension point '{}'.", info.fqn(), ann.to());
        } catch (Exception e) {
            LOGGER.error("Failed to register @Extension [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        org.eclipse.dirigible.engine.java.annotations.Extension ann = info.type()
                                                                          .getAnnotation(
                                                                                  org.eclipse.dirigible.engine.java.annotations.Extension.class);
        String location = locationOf(info.project(), info.fqn());
        String key = Extension.ARTEFACT_TYPE + ":" + location + ":" + ann.name();
        try {
            Extension existing = extensionService.findByKey(key);
            if (existing != null) {
                extensionService.delete(existing);
                LOGGER.info("Java @Extension [{}] unregistered.", info.fqn());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to unregister @Extension [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    private static String locationOf(String project, String fqn) {
        return "/" + project + "/" + fqn.replace('.', '/') + ".java";
    }
}
