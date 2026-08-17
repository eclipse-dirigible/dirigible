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

/**
 * The maven dependencies declared across all projects in the registry.
 *
 * @param dependencies the declared dependencies in registry order
 * @param errors the declaration errors that never reached resolution (bad coordinate, version
 *        range, unknown scope, ...), keyed by the declared id or the declaring project
 */
record DeclaredDependencies(Set<MavenDependency> dependencies, Map<String, String> errors) {
}
