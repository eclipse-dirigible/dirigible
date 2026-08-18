/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The maven dependencies declared across all projects in the registry.
 *
 * @param dependencies the declared dependencies in registry order
 * @param errors the declaration errors that never reached resolution (bad coordinate, version
 *        range, unknown scope, ...), keyed by the declared id or the declaring project
 * @param declaredBy the declaring projects per declared coordinate - the lockfile's
 *        {@code requestedBy} attribution
 */
record DeclaredDependencies(Set<MavenDependency> dependencies, Map<String, String> errors, Map<String, Set<String>> declaredBy) {

    /**
     * A change-detection fingerprint of the declarations - stable across collection order, different
     * for any semantic change (coordinate, scope, exclusions, attribution, or a declaration error).
     *
     * @return the fingerprint
     */
    String fingerprint() {
        String declared = dependencies.stream()
                                      .map(dependency -> dependency.coordinate() + "|" + dependency.scope() + "|"
                                              + new TreeSet<>(dependency.exclusions()))
                                      .sorted()
                                      .collect(Collectors.joining(";"));
        String failed = errors.entrySet()
                              .stream()
                              .map(entry -> entry.getKey() + "=" + entry.getValue())
                              .sorted()
                              .collect(Collectors.joining(";"));
        String attribution = declaredBy.entrySet()
                                       .stream()
                                       .map(entry -> entry.getKey() + "=" + new TreeSet<>(entry.getValue()))
                                       .sorted()
                                       .collect(Collectors.joining(";"));
        return declared + "||" + failed + "||" + attribution;
    }
}
