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

import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;

/**
 * Stand-in for a controller shipped by an AOT-packaged {@code compiled} module (already on the
 * classpath). Registered end-to-end via {@code JavaLoader.installCompiledModules} in
 * {@link CompiledModuleControllerRegistrationTest}.
 */
@Controller
public class CompiledSampleController {

    @Get("/ping")
    public String ping() {
        return "pong";
    }
}
