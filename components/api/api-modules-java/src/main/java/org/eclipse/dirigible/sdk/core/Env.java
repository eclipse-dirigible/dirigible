/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.core;

import org.eclipse.dirigible.components.api.core.EnvFacade;

/**
 * Read-only view of process environment variables. {@link #get(String)} returns a single value,
 * {@link #list()} returns the full map as a JSON document — handy for debug endpoints or feature-
 * flag inspection from an admin UI.
 * <p>
 * Note that for configuration knobs that <em>also</em> have a {@code DIRIGIBLE_*} configuration key
 * ({@code Configuration.get(...)} or {@link Configurations}) you should usually read the
 * configuration value, not the raw env var — the configuration layer also honours
 * {@code DIRIGIBLE_*} property files and runtime overrides.
 */
public final class Env {

    private Env() {}

    public static String get(String name) {
        return EnvFacade.get(name);
    }

    public static String list() {
        return EnvFacade.list();
    }
}
