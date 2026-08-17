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

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of a {@link DependencyResolver#resolve(java.util.Set)} call.
 *
 * @param artifacts the resolved jar paths inside the local repository - immutable, versioned
 *        locations that are never copied out or overwritten in place
 * @param mediated the versions chosen where the dependency graph requested more than one, keyed by
 *        {@code groupId:artifactId}
 * @param failures the per-coordinate failure messages (unresolvable, bad checksum, unsupported
 *        scope, ...), keyed by the failing coordinate
 */
public record ResolutionResult(List<Path> artifacts, Map<String, String> mediated, Map<String, String> failures) {

    /**
     * Copies the collections defensively, keeping their order.
     *
     * @param artifacts the resolved jar paths
     * @param mediated the mediated versions
     * @param failures the per-coordinate failures
     */
    public ResolutionResult {
        artifacts = List.copyOf(artifacts);
        mediated = Collections.unmodifiableMap(new LinkedHashMap<>(mediated));
        failures = Collections.unmodifiableMap(new LinkedHashMap<>(failures));
    }

}
