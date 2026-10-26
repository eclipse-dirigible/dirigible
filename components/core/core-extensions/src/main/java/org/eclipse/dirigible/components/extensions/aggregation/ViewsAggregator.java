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

import static org.eclipse.dirigible.components.extensions.aggregation.Contributions.truthy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.dirigible.components.base.script.ScriptEngineAvailability;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModule;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModuleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aggregates the views of extension points.
 *
 * <p>
 * A view needs an {@code id}, a {@code label}, and a target: a {@code path} to open, an
 * {@code endpoint} to call (an on-demand action button that POSTs rather than opening a page - see
 * the custom actions store), or a {@code call} - the request an intent action sends to the
 * application's own endpoint (issue #8090). The first view of an id wins. A view naming
 * {@code roles} is offered only to a caller holding one of them ({@link CallerRoles}): a button the
 * caller may not press is never shipped to the browser. A view without a region never takes the
 * focus when opened; one in the center or bottom region does unless it says otherwise, one in the
 * left or right region does not unless it says otherwise. A view declaring
 * {@value ScriptEngineAvailability#REQUIRES_SCRIPT_ENGINE} is offered only where the JavaScript
 * engine is deployed - the JavaScript debugger has nothing to attach to elsewhere.
 */
@Component
class ViewsAggregator {

    /** The extension point read when the request names none. */
    static final String DEFAULT_EXTENSION_POINT = "platform-views";

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewsAggregator.class);

    private static final List<String> EXPORTS = List.of("getView");

    private static final String AUTO_FOCUS_TAB = "autoFocusTab";

    private final ExtensionModuleReader reader;

    private final ScriptEngineAvailability scriptEngine;

    /**
     * Instantiates a new views aggregator.
     *
     * @param reader the extension module reader
     * @param scriptEngine whether the JavaScript engine is deployed, for the views that need it
     */
    ViewsAggregator(ExtensionModuleReader reader, ScriptEngineAvailability scriptEngine) {
        this.reader = reader;
        this.scriptEngine = scriptEngine;
    }

    /**
     * Aggregates the views of the given extension points.
     *
     * @param extensionPoints the extension points
     * @return the views, ordered
     */
    List<Map<String, Object>> aggregate(List<String> extensionPoints) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (ExtensionModule module : reader.read(extensionPoints, EXPORTS)) {
            Map<String, Object> view = module.content();
            if (isValid(view) && isOffered(view)) {
                Optional<Map<String, Object>> duplicate = views.stream()
                                                               .filter(existing -> Objects.equals(existing.get("id"), view.get("id")))
                                                               .findFirst();
                if (duplicate.isPresent()) {
                    LOGGER.warn("Duplication at view with id [{}] pointing to paths [{}] and [{}]: the latter is skipped.", view.get("id"),
                            duplicate.get()
                                     .get("path"),
                            view.get("path"));
                } else {
                    applyFocus(view);
                    views.add(view);
                }
            }
        }
        views.sort(ExtensionOrdering.byOrderThenText("label"));
        return views;
    }

    private static boolean isValid(Map<String, Object> view) {
        if (!truthy(view, "id")) {
            LOGGER.warn("View [{}] does not have an id and is skipped.", truthy(view, "label") ? view.get("label") : view.get("path"));
            return false;
        }
        if (!truthy(view, "label")) {
            LOGGER.warn("View [{}] does not have a label and is skipped.", view.get("id"));
            return false;
        }
        if (!truthy(view, "path") && !truthy(view, "endpoint") && !truthy(view, "call")) {
            LOGGER.warn("View [{}] does not have a path, an endpoint or a call and is skipped.", view.get("id"));
            return false;
        }
        return true;
    }

    private boolean isOffered(Map<String, Object> view) {
        return (!truthy(view, ScriptEngineAvailability.REQUIRES_SCRIPT_ENGINE) || scriptEngine.isAvailable()) && CallerRoles.offered(view);
    }

    private static void applyFocus(Map<String, Object> view) {
        Object region = view.get("region");
        if (!truthy(region)) {
            view.put(AUTO_FOCUS_TAB, Boolean.FALSE);
        } else if (!view.containsKey(AUTO_FOCUS_TAB)) {
            if ("center".equals(region) || "bottom".equals(region)) {
                view.put(AUTO_FOCUS_TAB, Boolean.TRUE);
            } else if ("left".equals(region) || "right".equals(region)) {
                view.put(AUTO_FOCUS_TAB, Boolean.FALSE);
            }
        }
    }
}
