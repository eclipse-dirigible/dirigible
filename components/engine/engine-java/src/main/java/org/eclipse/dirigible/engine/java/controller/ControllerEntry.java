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

import java.util.List;

/**
 * A registered controller: the originating project + FQN, the controller's basePath (used by the
 * router for prefix-matching), the live instance to invoke methods on, and its routes.
 *
 * @param project owning project
 * @param fqn fully-qualified class name of the controller
 * @param basePath registry-relative base path (e.g. {@code demo/CountryController}); the matching
 *        URL prefix is {@code /services/java/<project>/<basePath>}
 * @param instance lazily-created controller instance — replaced atomically when the controller is
 *        re-registered after a hot-reload
 * @param routes the controller's routes; immutable after construction
 */
public record ControllerEntry(String project, String fqn, String basePath, Object instance, List<Route> routes) {
}
