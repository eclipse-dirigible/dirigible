/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.extensions.modules.ExtensionModule;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModuleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aggregates the projections of extension points: named lenses over the deployed module set, each
 * selecting navigation groups (and/or individual perspectives) for the application shell's product
 * switcher.
 *
 * <p>
 * A projection carries {@code id}, {@code label}, optionally {@code description}, {@code icon} and
 * {@code order}; {@code groups} (group ids included wholesale) and {@code items} (perspective ids
 * cherry-picked), or {@code all: true} for the "everything" entry; and optionally {@code roles},
 * offering it only to a user holding at least one of them ({@link CallerRoles}).
 */
@Component
class ProjectionsAggregator {

    /** The extension point read when the request names none. */
    static final String DEFAULT_EXTENSION_POINT = "application-projections";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionsAggregator.class);

    private static final List<String> EXPORTS = List.of("getProjection");

    private final ExtensionModuleReader reader;

    /**
     * Instantiates a new projections aggregator.
     *
     * @param reader the extension module reader
     */
    ProjectionsAggregator(ExtensionModuleReader reader) {
        this.reader = reader;
    }

    /**
     * Aggregates the projections of the given extension points that the current user may see.
     *
     * @param extensionPoints the extension points
     * @return the projections, ordered
     */
    List<Map<String, Object>> aggregate(List<String> extensionPoints) {
        Set<Object> ids = new HashSet<>();
        List<Map<String, Object>> projections = new ArrayList<>();
        for (ExtensionModule module : reader.read(extensionPoints, EXPORTS)) {
            Map<String, Object> projection = module.content();
            if (!ids.add(projection.get("id"))) {
                LOGGER.warn("Projection with non-unique id [{}] is skipped.", projection.get("id"));
            } else if (CallerRoles.offered(projection)) {
                projections.add(projection);
            }
        }
        projections.sort(ExtensionOrdering.byOrderThenText("label"));
        return projections;
    }
}
