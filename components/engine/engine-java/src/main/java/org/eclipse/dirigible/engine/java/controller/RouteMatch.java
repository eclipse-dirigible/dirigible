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

import java.util.Map;

/**
 * Outcome of a successful router lookup.
 *
 * @param entry the controller the URL resolved to
 * @param route the matched route within that controller
 * @param pathParameters values extracted from the route's path placeholders, keyed by placeholder
 *        name
 */
public record RouteMatch(ControllerEntry entry, Route route, Map<String, String> pathParameters) {
}
