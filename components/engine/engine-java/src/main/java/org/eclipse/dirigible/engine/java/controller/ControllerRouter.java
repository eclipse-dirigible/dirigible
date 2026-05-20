/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Lookup table of registered controllers and their routes.
 *
 * <p>
 * Each controller is registered under a base path of the form
 * {@code <package-with-slashes>/<ClassName>}; lookups translate the incoming
 * {@code /services/java/<project>/<classPath>} URL into a base + remainder pair by greedy
 * longest-prefix match against the registered controllers for that project, then iterate the
 * controller's routes for an HTTP-method + pattern match.
 *
 * <p>
 * The table is keyed by {@code <project>::<basePath>} so two projects may legitimately define
 * controllers at the same FQN. Reads are lock-free via {@code volatile} reference; writes (a small
 * number of bursts per synchronization cycle) take a short intrinsic lock to make register /
 * unregister atomic with respect to each other.
 */
@Component
public class ControllerRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerRouter.class);

    /** Keyed by {@code <project>::<basePath>}. */
    private volatile Map<String, ControllerEntry> entries = new LinkedHashMap<>();

    private final Object writeLock = new Object();

    /** Register or replace a controller entry. */
    public void register(ControllerEntry entry) {
        synchronized (writeLock) {
            Map<String, ControllerEntry> next = new LinkedHashMap<>(entries);
            String key = key(entry.project(), entry.basePath());
            ControllerEntry sorted = sortRoutes(entry);
            ControllerEntry prior = next.put(key, sorted);
            entries = next;
            if (prior != null) {
                LOGGER.info("Replaced controller [{}::{}] ({} routes)", entry.project(), entry.fqn(), entry.routes()
                                                                                                           .size());
            } else {
                LOGGER.info("Registered controller [{}::{}] ({} routes)", entry.project(), entry.fqn(), entry.routes()
                                                                                                             .size());
            }
        }
    }

    /** Drop a controller from the table by project + FQN. */
    public void unregister(String project, String fqn) {
        String basePath = fqnToBasePath(fqn);
        synchronized (writeLock) {
            Map<String, ControllerEntry> next = new LinkedHashMap<>(entries);
            ControllerEntry removed = next.remove(key(project, basePath));
            entries = next;
            if (removed != null) {
                LOGGER.info("Unregistered controller [{}::{}]", project, fqn);
            }
        }
    }

    /**
     * Resolve an incoming request URL to a controller route.
     *
     * @param httpMethod the request method
     * @param project the {@code <project>} segment from {@code JavaEndpoint}
     * @param classPath the {@code {*classPath}} segment from {@code JavaEndpoint}, possibly leading
     *        with {@code /}
     * @return a {@link RouteMatch} if a controller is registered at a matching base path and one of its
     *         routes matches the remaining suffix and HTTP method; otherwise empty
     */
    public Optional<RouteMatch> match(HttpMethod httpMethod, String project, String classPath) {
        String normalized = classPath == null ? "" : (classPath.startsWith("/") ? classPath.substring(1) : classPath);
        Map<String, ControllerEntry> snapshot = entries;

        // Collect candidate controllers for this project ordered by descending basePath length (most
        // specific first).
        List<ControllerEntry> candidates = new ArrayList<>();
        for (Map.Entry<String, ControllerEntry> e : snapshot.entrySet()) {
            ControllerEntry entry = e.getValue();
            if (!entry.project()
                      .equals(project)) {
                continue;
            }
            String base = entry.basePath();
            if (normalized.equals(base) || normalized.startsWith(base + "/")) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.comparingInt((ControllerEntry e) -> e.basePath()
                                                                        .length())
                                  .reversed());

        for (ControllerEntry entry : candidates) {
            String suffix = normalized.substring(entry.basePath()
                                                      .length());
            for (Route route : entry.routes()) {
                if (route.httpMethod() != httpMethod) {
                    continue;
                }
                Matcher m = route.pathPattern()
                                 .matcher(suffix);
                if (m.matches()) {
                    Map<String, String> params = new HashMap<>();
                    for (String name : route.placeholders()) {
                        params.put(name, m.group(name));
                    }
                    return Optional.of(new RouteMatch(entry, route, params));
                }
            }
        }
        return Optional.empty();
    }

    /** Snapshot of the current entries — for tests / diagnostics. */
    public List<ControllerEntry> snapshot() {
        return List.copyOf(entries.values());
    }

    /** Number of registered controllers — for tests. */
    public int size() {
        return entries.size();
    }

    private static String key(String project, String basePath) {
        return project + "::" + basePath;
    }

    static String fqnToBasePath(String fqn) {
        return fqn == null ? "" : fqn.replace('.', '/');
    }

    /**
     * Defensive sort so the router's match loop tries literal routes before placeholder ones. The
     * consumer also sorts at build time, but external callers (and tests) may register out-of-order
     * routes.
     */
    private static ControllerEntry sortRoutes(ControllerEntry entry) {
        List<Route> routes = new ArrayList<>(entry.routes());
        routes.sort(Comparator.comparingInt(r -> PathPattern.specificity(r.pathTemplate(), r.placeholders()
                                                                                            .size())));
        return new ControllerEntry(entry.project(), entry.fqn(), entry.basePath(), entry.instance(), List.copyOf(routes));
    }
}
