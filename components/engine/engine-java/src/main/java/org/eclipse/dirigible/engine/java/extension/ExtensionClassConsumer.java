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
import org.eclipse.dirigible.sdk.extensions.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client classes annotated with
 * {@link org.eclipse.dirigible.sdk.extensions.Extension @Extension} as Dirigible extension
 * contributions persisted via {@link ExtensionService}.
 *
 * <p>
 * The extension-point key in the {@code DIRIGIBLE_EXTENSIONS} table is the
 * {@link org.eclipse.dirigible.sdk.extensions.Extension#target() target}'s fully qualified name.
 * The {@code module} is the impl class FQN — {@code Extensions.find(Class<T>)} loads it from the
 * active client classloader, validates {@code isAssignableFrom}, and casts to the target interface,
 * so consumers never reflect.
 *
 * <p>
 * Registration is validated at class-load time: a class declaring {@code @Extension(target = X)}
 * that does not actually implement {@code X} is logged and skipped. The target type is also
 * expected (but not required) to carry {@link ExtensionPoint @ExtensionPoint} — a missing marker is
 * logged at WARN to flag the convention drift without breaking the registration.
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
        return clazz.isAnnotationPresent(org.eclipse.dirigible.sdk.extensions.Extension.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        org.eclipse.dirigible.sdk.extensions.Extension ann = info.type()
                                                                 .getAnnotation(org.eclipse.dirigible.sdk.extensions.Extension.class);

        Class<?> target = ann.target();
        if (!target.isAssignableFrom(info.type())) {
            LOGGER.error("Skipping @Extension [{}]: class does not implement declared target [{}].", info.fqn(), target.getName());
            return;
        }
        if (!target.isAnnotationPresent(ExtensionPoint.class)) {
            LOGGER.warn("@Extension [{}] targets interface [{}] which is not annotated with @ExtensionPoint — registering anyway.",
                    info.fqn(), target.getName());
        }

        String extensionPointFqn = target.getName();
        String contributionName = ann.name()
                                     .isEmpty() ? info.fqn() : ann.name();
        String location = locationOf(info.project(), info.fqn());
        try {
            String key = Extension.ARTEFACT_TYPE + ":" + location + ":" + contributionName;
            Extension existing = extensionService.findByKey(key);
            Extension extension =
                    existing != null ? existing : new Extension(location, contributionName, null, extensionPointFqn, info.fqn(), null);
            extension.setExtensionPoint(extensionPointFqn);
            extension.setModule(info.fqn());
            extensionService.save(extension);
            LOGGER.info("Java @Extension [{}] registered for extension point [{}].", info.fqn(), extensionPointFqn);
        } catch (Exception e) {
            LOGGER.error("Failed to register @Extension [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        org.eclipse.dirigible.sdk.extensions.Extension ann = info.type()
                                                                 .getAnnotation(org.eclipse.dirigible.sdk.extensions.Extension.class);
        String contributionName = ann.name()
                                     .isEmpty() ? info.fqn() : ann.name();
        String location = locationOf(info.project(), info.fqn());
        String key = Extension.ARTEFACT_TYPE + ":" + location + ":" + contributionName;
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
