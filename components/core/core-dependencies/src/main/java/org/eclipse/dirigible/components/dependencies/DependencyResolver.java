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

import java.util.Set;

/**
 * Resolves Maven coordinates into the instance-global local repository.
 *
 * <p>
 * The input is the union of all projects' declarations, resolved in a single collect request, so
 * Maven's standard version mediation applies globally - one flat classpath means one mediation.
 * Failures are per-coordinate and never thrown: an unresolvable dependency surfaces in the result
 * and must never prevent the platform from booting.
 *
 * <p>
 * Example:
 *
 * <pre>{@code
 * ResolutionResult r =
 *         resolver.resolve(Set.of(new MavenDependency("software.amazon.awssdk:s3:2.32.4", MavenDependency.Scope.MODULE, List.of())));
 * r.artifacts(); // jar paths inside the local repo (immutable, versioned)
 * r.mediated(); // versions chosen where the graph requested more than one
 * r.failures(); // per-coordinate failure messages (unresolvable, bad checksum, ...)
 * }</pre>
 */
public interface DependencyResolver {

    /**
     * Resolves the declared dependencies and their transitive graph into the local repository.
     *
     * @param declared the union of all declared dependencies
     * @return the resolution result - resolved jar paths, mediated versions and per-coordinate failures
     */
    ResolutionResult resolve(Set<MavenDependency> declared);

}
